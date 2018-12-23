package org.jurassicraft.client.model.animation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.ilexiconn.llibrary.client.model.tools.AdvancedModelRenderer;
import net.ilexiconn.llibrary.server.animation.Animation;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jurassicraft.JurassiCraft;
import org.jurassicraft.client.model.AnimatableModel;
import org.jurassicraft.client.model.animation.dto.AnimatableRenderDefDTO;
import org.jurassicraft.client.model.animation.dto.AnimationsDTO;
import org.jurassicraft.client.model.animation.dto.PoseDTO;
import org.jurassicraft.client.model.animation.dto.VariantDTO;
import org.jurassicraft.server.api.Animatable;
import org.jurassicraft.server.dinosaur.Dinosaur;
import org.jurassicraft.server.entity.GrowthStage;
import org.jurassicraft.server.tabula.TabulaModelHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public class PoseHandler<ENTITY extends EntityLivingBase & Animatable> {
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(AnimatableRenderDefDTO.class, new AnimatableRenderDefDTO.AnimatableDeserializer()).create();
    private Map<GrowthStage, ModelData> modelData = new EnumMap<>(GrowthStage.class);

    public PoseHandler(Dinosaur dinosaur) {
        this(dinosaur.getIdentifier(), dinosaur.getSupportedStages());
    }

    public PoseHandler(ResourceLocation identifier, List<GrowthStage> supported) {
    	
        this.modelData = new EnumMap<>(GrowthStage.class);
        ResourceLocation entityResource = new ResourceLocation(identifier.getResourceDomain(), "models/entities/" + identifier.getResourcePath());
        for (GrowthStage growth : GrowthStage.values()) {
            try {
                GrowthStage actualGrowth = growth;
                if (!supported.contains(actualGrowth)) {
                    actualGrowth = GrowthStage.ADULT;
                }
                if (this.modelData.containsKey(actualGrowth)) {
                    this.modelData.put(growth, this.modelData.get(actualGrowth));
                } else {
                    ModelData loaded = this.loadModelData(identifier, entityResource, actualGrowth);
                    this.modelData.put(growth, loaded);
                    if (actualGrowth != growth) {
                        this.modelData.put(actualGrowth, loaded);
                    }
                }
            } catch (Exception e) {
                JurassiCraft.INSTANCE.getLogger().fatal("Failed to parse growth stage " + growth + " for dinosaur " + identifier, e);
                this.modelData.put(growth, new ModelData());
            }
        }
    }

    private ModelData loadModelData(ResourceLocation identifier, ResourceLocation origin, GrowthStage growth) {
        String namespace = origin.getResourceDomain();
        String name = identifier.getResourcePath();
        ResourceLocation stageOrigin = new ResourceLocation(namespace, origin.getResourcePath() + "/" + growth.getKey());
        ResourceLocation definition = new ResourceLocation(namespace, stageOrigin.getResourcePath() + "/" + name + "_" + growth.getKey() + ".json");
        try (InputStream modelStream = TabulaModelHelper.class.getResourceAsStream("/assets/" + definition.getResourceDomain() + "/" + definition.getResourcePath())) {
            if (modelStream == null) {
                throw new IllegalArgumentException("No model definition for the dino " + identifier + " with grow-state " + growth + " exists. Expected at " + definition);
            }
            Reader reader = new InputStreamReader(modelStream);
            AnimationsDTO rawAnimations = GSON.fromJson(reader, AnimationsDTO.class);
            ModelData data = this.loadModelData(stageOrigin, rawAnimations);
            JurassiCraft.INSTANCE.getLogger().debug("Successfully loaded " + identifier + "(" + growth + ") from " + definition);
            reader.close();
            return data;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
	private ModelData loadModelData(ResourceLocation origin, AnimationsDTO animationsDefinition) {

		if (animationsDefinition == null || animationsDefinition.poses == null
				|| animationsDefinition.poses.get(EntityAnimation.IDLE.name()) == null
				|| animationsDefinition.poses.get(EntityAnimation.IDLE.name()).variants == null
				|| animationsDefinition.poses.get(EntityAnimation.IDLE.name()).variants.size() == 0
				|| animationsDefinition.poses.get(EntityAnimation.IDLE.name()).variants.get("0") == null
				|| animationsDefinition.poses.get(EntityAnimation.IDLE.name()).variants.get("0").length == 0) {

			throw new IllegalArgumentException(
					"Animation files must define at least one variant and pose for the IDLE animation");
		}

		List<ResourceLocation> posedModelResources = new ArrayList<>();
		for (Map.Entry<String, VariantDTO> variants : animationsDefinition.poses.entrySet()) {
			if (variants == null) {
				continue;
			}
			for (Map.Entry<String, PoseDTO[]> poses : variants.getValue().variants.entrySet()) {
				
				if (poses == null) {
					continue;
				}

				for (PoseDTO pose : poses.getValue()) {
					if (pose == null) {
						continue;
					}
					if (pose.pose == null) {
						throw new IllegalArgumentException("Every pose must define a pose file");
					}
					
					ResourceLocation resolvedRes = new ResourceLocation(origin.getResourceDomain(), origin.getResourcePath() + "/" + pose.pose);
					int index = posedModelResources.indexOf(resolvedRes);
					if (index == -1) {
						pose.index = posedModelResources.size();
						posedModelResources.add(resolvedRes);
					} else {
						pose.index = index;
					}

				}
				
			}
				
			
		}

		Map<Animation, float[][][]> animations = new HashMap<>();
		Map<Animation, byte[]> poseCount = new HashMap<>();

		for (Map.Entry<String, VariantDTO> variants : animationsDefinition.poses.entrySet()) {
			
			int length = 0;
			for (Map.Entry<String, PoseDTO[]> posesList : variants.getValue().variants.entrySet()) {
				PoseDTO[] poses = posesList.getValue();
				if(length < poses.length)
					length = poses.length;
			}

			for (Map.Entry<String, PoseDTO[]> posesList : variants.getValue().variants.entrySet()) {

				Animation animation = EntityAnimation.valueOf(variants.getKey()).get();
				PoseDTO[] poses = posesList.getValue();
				float[][][] poseSequence = animations.get(animation);
				byte[] counts = poseCount.get(animation);
				if(poseSequence == null) {
					poseSequence = new float[variants.getValue().variants.size()][length][2];
					counts = new byte[variants.getValue().variants.size()];
				}
				
				
				counts[Integer.parseInt(posesList.getKey())] = (byte) poses.length;
				poseCount.put(animation, counts);
				for (int i = 0; i < poses.length; i++) {
					poseSequence[Integer.parseInt(posesList.getKey())][i][0] = poses[i].index;
					poseSequence[Integer.parseInt(posesList.getKey())][i][1] = poses[i].time;
				
				}
				animations.put(animation, poseSequence);
				
			}
		}
		if (FMLCommonHandler.instance().getSide().isClient()) {
			return this.loadModelDataClient(posedModelResources, animations, poseCount);
		}

		return new ModelData(animations, poseCount);
	}

    @SideOnly(Side.CLIENT)
    private ModelData loadModelDataClient(List<ResourceLocation> posedModelResources, Map<Animation, float[][][]> animations, Map<Animation, byte[]> poseCount) {
        PosedCuboid[][] posedCuboids = new PosedCuboid[posedModelResources.size()][];
        AnimatableModel mainModel = JabelarAnimationHandler.loadModel(posedModelResources.get(0));
       
        if (mainModel == null) {
            throw new IllegalArgumentException("Couldn't load the model from " + posedModelResources.get(0));
        }
        String[] identifiers = mainModel.getCubeIdentifierArray();
        int partCount = identifiers.length;
        	
        for (int i = 0; i < posedModelResources.size(); i++) {
        	ResourceLocation resource = posedModelResources.get(i);
            AnimatableModel model = JabelarAnimationHandler.loadModel(resource);
            if (model == null) {
                throw new IllegalArgumentException("Couldn't load the model from " + resource);
            }
            PosedCuboid[] pose = new PosedCuboid[partCount];
            for (int partIndex = 0; partIndex < partCount; partIndex++) {
                String identifier = identifiers[partIndex];
                AdvancedModelRenderer cube = model.getCubeByIdentifier(identifier);
                if (cube == null) {
                    AdvancedModelRenderer mainCube = mainModel.getCubeByIdentifier(identifier);
                    
                    //TODO: Recreate T-Rex running model
                    JurassiCraft.getLogger().error("Could not retrieve cube " + identifier + " (" + mainCube.boxName + ", " + partIndex + ") from the model " + resource +" (We're aware of this problem!)");
                    pose[partIndex] = new PosedCuboid(mainCube);
                } else {
                    pose[partIndex] = new PosedCuboid(cube);
                }
            }
            posedCuboids[i] = pose;
        }
    
        return new ModelData(posedCuboids, animations, poseCount);
    }

    @SideOnly(Side.CLIENT)
    public JabelarAnimationHandler<ENTITY> createAnimationHandler(ENTITY entity, AnimatableModel model, GrowthStage growthStage, boolean useInertialTweens) {
        ModelData growthModel = this.modelData.get(growthStage);
        
        if (!entity.canUseGrowthStage(growthStage)) {
            growthModel = this.modelData.get(growthStage);
        }
        return new JabelarAnimationHandler<>(entity, model, growthModel.poses, growthModel.animations, growthModel.poseCount, useInertialTweens);
    }

    public Map<Animation, float[][][]> getAnimations(GrowthStage growthStage) {
        return this.modelData.get(growthStage).animations;
    }

	public float getAnimationLength(Animation animation, GrowthStage growthStage, byte variant) {
		Map<Animation, float[][][]> animations = this.getAnimations(growthStage);
		float duration = 0;
		if (animation != null && animations.get(animation) != null) {

			if (animations.get(animation)[variant] != null) {
				for (float[] pose : animations.get(animation)[variant]) {
					duration += pose[1];
				}

			}
		}
		return duration;
	}

    public boolean hasAnimation(Animation animation, GrowthStage growthStage) {
        return this.modelData.get(growthStage).animations.get(animation) != null;
    }

    private class ModelData {
        @SideOnly(Side.CLIENT)
        PosedCuboid[][] poses;

        Map<Animation, float[][][]> animations;
        Map<Animation, byte[]> poseCount;

        public ModelData() {
            this(null, null);
        }

        @SideOnly(Side.CLIENT)
        public ModelData(PosedCuboid[][] cuboids, Map<Animation, float[][][]> animations, Map<Animation, byte[]> poseCount) {
            this(animations, poseCount);

            if (cuboids == null) {
                cuboids = new PosedCuboid[0][];
            }

            this.poses = cuboids;
        }

        public ModelData(Map<Animation, float[][][]> animations, Map<Animation, byte[]> poseCount) {
            if (animations == null) {
                animations = new LinkedHashMap<>();
            }
            if (poseCount == null) {
            	poseCount = new LinkedHashMap<>();
            }
            
            this.animations = animations;
            this.poseCount = poseCount;
        }
    }
    
}