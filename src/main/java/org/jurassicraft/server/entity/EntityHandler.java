package org.jurassicraft.server.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jurassicraft.JurassiCraft;
import org.jurassicraft.server.api.Hybrid;
import org.jurassicraft.server.conf.JurassiCraftConfig;
import org.jurassicraft.server.dinosaur.BrachiosaurusDinosaur;
import org.jurassicraft.server.dinosaur.CoelacanthDinosaur;
import org.jurassicraft.server.dinosaur.DilophosaurusDinosaur;
import org.jurassicraft.server.dinosaur.Dinosaur;
import org.jurassicraft.server.dinosaur.GallimimusDinosaur;
import org.jurassicraft.server.dinosaur.MicroraptorDinosaur;
import org.jurassicraft.server.dinosaur.MussaurusDinosaur;
import org.jurassicraft.server.dinosaur.ParasaurolophusDinosaur;
import org.jurassicraft.server.dinosaur.TriceratopsDinosaur;
import org.jurassicraft.server.dinosaur.TyrannosaurusDinosaur;
import org.jurassicraft.server.dinosaur.VelociraptorDinosaur;
import org.jurassicraft.server.entity.item.AttractionSignEntity;
import org.jurassicraft.server.entity.item.DinosaurEggEntity;
import org.jurassicraft.server.entity.item.MuralEntity;
import org.jurassicraft.server.entity.item.PaddockSignEntity;
import org.jurassicraft.server.entity.vehicle.FordExplorerEntity;
import org.jurassicraft.server.entity.vehicle.HelicopterEntity;
import org.jurassicraft.server.entity.vehicle.JeepWranglerEntity;
import org.jurassicraft.server.period.TimePeriod;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.registry.EntityRegistry;

public class EntityHandler {
    public static final Dinosaur BRACHIOSAURUS = new BrachiosaurusDinosaur();
    public static final Dinosaur COELACANTH = new CoelacanthDinosaur();
    public static final Dinosaur DILOPHOSAURUS = new DilophosaurusDinosaur();
    public static final Dinosaur GALLIMIMUS = new GallimimusDinosaur();
    public static final Dinosaur PARASAUROLOPHUS = new ParasaurolophusDinosaur();
    public static final Dinosaur MICRORAPTOR = new MicroraptorDinosaur();
    public static final Dinosaur MUSSAURUS = new MussaurusDinosaur();
    public static final Dinosaur TRICERATOPS = new TriceratopsDinosaur();
    public static final Dinosaur TYRANNOSAURUS = new TyrannosaurusDinosaur();
    public static final Dinosaur VELOCIRAPTOR = new VelociraptorDinosaur();
//    public static final Dinosaur ALLIGATORGAR = new AlligatorGarDinosaur();
    //public static final Dinosaur STEGeOSAURUS = new StegosaurusDinosaur();

    private static final Map<Integer, Dinosaur> DINOSAURS = new HashMap<>();
    private static final Map<Dinosaur, Integer> DINOSAUR_IDS = new HashMap<>();
    private static final HashMap<TimePeriod, List<Dinosaur>> DINOSAUR_PERIODS = new HashMap<>();

    private static int entityId;

    private static ProgressManager.ProgressBar dinosaurProgress;
    private static int highestID;

    public static List<Dinosaur> getMarineCreatures() {
        List<Dinosaur> marineDinosaurs = new ArrayList<>();
        for (Dinosaur dino : getRegisteredDinosaurs()) {
            if (dino.isMarineCreature() && !(dino instanceof Hybrid)) {
                marineDinosaurs.add(dino);
            }
        }
        return marineDinosaurs;
    }

    public static void init() {
        registerDinosaur(0, VELOCIRAPTOR);
        registerDinosaur(1, COELACANTH);
        registerDinosaur(2, MICRORAPTOR);
        registerDinosaur(3, BRACHIOSAURUS);
        registerDinosaur(4, MUSSAURUS);
        registerDinosaur(7, DILOPHOSAURUS);
        registerDinosaur(9, GALLIMIMUS);
        registerDinosaur(13, PARASAUROLOPHUS);
        registerDinosaur(19, TRICERATOPS);
        registerDinosaur(20, TYRANNOSAURUS);
//        registerDinosaur(22, ALLIGATORGAR);
        //registerDinosaur(21, STEGOSAURUS);

        dinosaurProgress = ProgressManager.push("Loading dinosaurs", DINOSAURS.size());

        initDinosaurs();

        ProgressManager.pop(dinosaurProgress);

        registerEntity(AttractionSignEntity.class, "Attraction Sign");
        registerEntity(PaddockSignEntity.class, "Paddock Sign");
        registerEntity(MuralEntity.class, "Mural");
        registerEntity(VenomEntity.class, "Venom");

        registerEntity(JeepWranglerEntity.class, "Jeep Wrangler");
        registerEntity(FordExplorerEntity.class, "Ford Explorer");

        registerEntity(GoatEntity.class, "Goat", 0xEFEDE7, 0x7B3E20);

        registerEntity(TranquilizerDartEntity.class, "Tranquilizer Dart");
        
        ArrayList<Biome> biomeHills = new ArrayList<Biome>();
        	biomeHills.addAll(BiomeDictionary.getBiomes(BiomeDictionary.Type.HILLS));
        ArrayList<Biome> biomePlains = new ArrayList<Biome>();
        	biomePlains.addAll(BiomeDictionary.getBiomes(BiomeDictionary.Type.PLAINS));
        ArrayList<Biome> biomeForest = new ArrayList<Biome>();
        	biomeForest.addAll(BiomeDictionary.getBiomes(BiomeDictionary.Type.FOREST));
        
//        EntityRegistry.addSpawn(GoatEntity.class, 10, 1, 3, EnumCreatureType.CREATURE, biomeHills.toArray(new Biome[biomeHills.size()]));
//        EntityRegistry.addSpawn(GoatEntity.class, 15, 1, 3, EnumCreatureType.CREATURE, biomeHills.toArray(new Biome[biomePlains.size()]));
//        EntityRegistry.addSpawn(GoatEntity.class, 15, 1, 3, EnumCreatureType.CREATURE, biomeHills.toArray(new Biome[biomeForest.size()]));

        registerEntity(DinosaurEggEntity.class, "Dinosaur Egg");
        registerEntity(HelicopterEntity.class, "Helicopter base");
    }

    private static void initDinosaurs() {
        for (Map.Entry<Integer, Dinosaur> entry : DINOSAURS.entrySet()) {
            Dinosaur dinosaur = entry.getValue();

            dinosaurProgress.step(dinosaur.getName());

            dinosaur.init();

            boolean canSpawn = !(dinosaur instanceof Hybrid) && dinosaur.shouldRegister();

            if (canSpawn) {
                TimePeriod period = dinosaur.getPeriod();
                List<Dinosaur> periods = DINOSAUR_PERIODS.get(period);
                if (periods == null) {
                    periods = new LinkedList<>();
                    DINOSAUR_PERIODS.put(period, periods);
                }
                periods.add(dinosaur);
            }

            Class<? extends DinosaurEntity> clazz = dinosaur.getDinosaurClass();

            registerEntity(clazz, dinosaur.getName());

            addSpawn(canSpawn, clazz, dinosaur);
        }
    }
    
    private static void addSpawn(boolean canSpawn, Class clazz, Dinosaur dinosaur) {
    	if (canSpawn && JurassiCraftConfig.ENTITIES.naturalSpawning_D) {
            EntityRegistry.addSpawn(clazz, dinosaur.getSpawnChance(), 1, Math.min(6, dinosaur.getMaxHerdSize() / 2), dinosaur.isMarineCreature() ? EnumCreatureType.WATER_CREATURE : EnumCreatureType.CREATURE, dinosaur.getSpawnBiomes());
        }
    }
    
    public static void reinitSpawns() {
        for (Map.Entry<Integer, Dinosaur> entry : DINOSAURS.entrySet()) {
            Dinosaur dinosaur = entry.getValue();
            Class<? extends DinosaurEntity> clazz = dinosaur.getDinosaurClass();
            EntityRegistry.removeSpawn(clazz, dinosaur.isMarineCreature() ? EnumCreatureType.WATER_CREATURE : EnumCreatureType.CREATURE, dinosaur.getSpawnBiomes());
            boolean canSpawn = !(dinosaur instanceof Hybrid) && dinosaur.shouldRegister();
            addSpawn(canSpawn, clazz, dinosaur);
            
        }
    }
    
    private static void registerEntity(Class<? extends Entity> entity, String name) {
        String formattedName = name.toLowerCase(Locale.ENGLISH).replaceAll(" ", "_");
        ResourceLocation registryName = new ResourceLocation("jurassicraft:entities." + formattedName);
        EntityRegistry.registerModEntity(registryName, entity, "jurassicraft." + formattedName, entityId++, JurassiCraft.INSTANCE, 1024, 1, true);
    }

    private static void registerEntity(Class<? extends Entity> entity, String name, int primary, int secondary) {
        String formattedName = name.toLowerCase(Locale.ENGLISH).replaceAll(" ", "_");
        ResourceLocation registryName = new ResourceLocation("jurassicraft:entities." + formattedName);
        EntityRegistry.registerModEntity(registryName, entity, "jurassicraft." + formattedName, entityId++, JurassiCraft.INSTANCE, 1024, 1, true, primary, secondary);
    }

    public static void registerDinosaur(int id, Dinosaur dinosaur) {
        if (id > highestID) {
            highestID = id;
        }

        DINOSAURS.put(id, dinosaur);
        DINOSAUR_IDS.put(dinosaur, id);
    }

    public static Dinosaur getDinosaurById(int id) {
        Dinosaur dinosaur = DINOSAURS.get(id);
        return dinosaur != null ? dinosaur : getDinosaurById(0);
    }

    public static int getDinosaurId(Dinosaur dinosaur) {
        return DINOSAUR_IDS.get(dinosaur);
    }

    public static List<Dinosaur> getDinosaursFromAmber() {
        List<Dinosaur> dinosaurs = new LinkedList<>();
        for (Dinosaur dinosaur : getRegisteredDinosaurs()) {
            if (!dinosaur.isMarineCreature() && !(dinosaur instanceof Hybrid)) {
                dinosaurs.add(dinosaur);
            }
        }
        return dinosaurs;
    }

    public static Map<Integer, Dinosaur> getDinosaurs() {
        return DINOSAURS;
    }

    public static List<Dinosaur> getRegisteredDinosaurs() {
        List<Dinosaur> dinosaurs = new LinkedList<>();

        for (Map.Entry<Integer, Dinosaur> entry : EntityHandler.DINOSAURS.entrySet()) {
            Dinosaur dinosaur = entry.getValue();

            if (dinosaur.shouldRegister()) {
                dinosaurs.add(dinosaur);
            }
        }

        return dinosaurs;
    }

    public static List<Dinosaur> getPrehistoricDinosaurs() {
        List<Dinosaur> dinosaurs = new LinkedList<>();
        for (Map.Entry<Integer, Dinosaur> entry : EntityHandler.DINOSAURS.entrySet()) {
            Dinosaur dinosaur = entry.getValue();
            if (dinosaur.shouldRegister() && !(dinosaur instanceof Hybrid)) {
                dinosaurs.add(dinosaur);
            }
        }
        return dinosaurs;
    }

    public static List<Dinosaur> getDinosaursFromPeriod(TimePeriod period) {
        return DINOSAUR_PERIODS.get(period);
    }

    public static Dinosaur getDinosaurByClass(Class<? extends DinosaurEntity> clazz) {
        for (Map.Entry<Integer, Dinosaur> entry : EntityHandler.DINOSAURS.entrySet()) {
            Dinosaur dinosaur = entry.getValue();

            if (dinosaur.getDinosaurClass().equals(clazz)) {
                return dinosaur;
            }
        }

        return null;
    }

    public static int getHighestID() {
        return highestID;
    }
}
