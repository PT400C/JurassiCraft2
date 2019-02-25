package org.jurassicraft.client.model.obj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import org.jurassicraft.JurassiCraft;
import org.jurassicraft.client.proxy.ClientProxy;
import org.jurassicraft.server.block.entity.SkullDisplayEntity;
import org.jurassicraft.server.util.Vec3f;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.fml.common.ProgressManager;

public class OBJHandler {
	
	//TODO: Create better modular registry
	@Deprecated
	public static final HashMap<String, String> modelRequests = new HashMap<String, String>() {{
		put("T. Rex - Standing ", "skull_display/tyrannosaurus_horizontal");
		put("T. Rex - Handing ", "skull_display/tyrannosaurus_vertical");
		put("T. Rex - Placed ", "skull_display/tyrannosaurus_placed");
	}};
	
	@Deprecated
	public static final HashMap<String, String> textureRequests = new HashMap<String, String>() {{
		put("Fr", "skull_display/tyrannosaurus_fresh");
		put("Fo", "skull_display/tyrannosaurus_fossilized");
	}};
	
	public static HashMap<String, OBJRender> renderer = new HashMap<String, OBJRender>();
	public static HashMap<String, TextureQuilt> textures = new HashMap<String, TextureQuilt>();
	public static final MutableBlockPos mbp = new MutableBlockPos();
	
	/**
     * Init the OBJHandler
     * @param bar ProgressBar to be updated
     */
	public static void init(@Nullable ProgressManager.ProgressBar bar) {
		
		for(Entry<String, String> temp : textureRequests.entrySet()) {
			try {
				JurassiCraft.getLogger().debug("Cache texture " + temp.getKey());
				ResourceLocation model = new ResourceLocation(JurassiCraft.MODID, "textures/blocks/" + temp.getValue() + ".png");
				textures.put(temp.getKey(), new TextureQuilt(model));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		for(Entry<String, String> temp : modelRequests.entrySet()) {
			try {
				JurassiCraft.getLogger().debug("Cache model " + temp.getKey());
				if(bar != null)
					bar.step(temp.getKey());
				ResourceLocation model = new ResourceLocation(JurassiCraft.MODID, "models/block/" + temp.getValue() + ".obj");
				for(Entry<String, TextureQuilt> tq : textures.entrySet())
					renderer.put(temp.getKey() + tq.getKey(), new OBJRender(new OBJModel(model), tq.getValue()));
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
     * Render all visible TileEntities which should be custom-rendered
     * @param partialTicks Particial ticks from the renderer
     * @param cameraPos The camera position of the spectator
     * @param camera The camera object itself
     */
	public static void renderTiles(float partialTicks, Vec3f cameraPos, ICamera camera) {
		Minecraft.getMinecraft().mcProfiler.startSection("JC-TE");
		boolean prevState = GL11.glGetBoolean(GL11.GL_BLEND);
		boolean state = false;
		if(prevState != state)
			GL11.glDisable(GL11.GL_BLEND);

		if (MinecraftForgeClient.getRenderPass() != 0) 
        	return;
		
        List<TileEntity> entities = new ArrayList<TileEntity>(Minecraft.getMinecraft().player.getEntityWorld().loadedTileEntityList);
        for (TileEntity te : entities) {
        	if (te instanceof SkullDisplayEntity) {
        		SkullDisplayEntity entity = ((SkullDisplayEntity) te);
        		if (!entity.hasData()) {
        			continue;
        		}
        		mbp.setPos(te.getPos());
	        	if (camera.isBoundingBoxInFrustum(te.getRenderBoundingBox()) && ClientProxy.MC.player.getPositionVector().distanceTo(new Vec3d(mbp)) < te.getMaxRenderDistanceSquared()) {

	        		GL11.glPushMatrix();
	        		{
	        	        int i = te.getWorld().getCombinedLight(mbp, 0);
	        	        int j = i % 65536;
	        	        int k = i / 65536;
	        	        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) j, (float) k);

						Vec3f pos = new Vec3f(mbp.getX() + 0.5F, mbp.getY() + 0.75F, mbp.getZ() + 0.5F).subtract(cameraPos);
						GL11.glTranslated(pos.x, pos.y, pos.z);

		        		renderTile((SkullDisplayEntity) te, "T. Rex - Standing " + (entity.isFossilized() ? "Fo" : "Fr"), locationIdentifier(mbp));
	        		}
	        		GL11.glPopMatrix();
	        	}	
        	}
        }
        if(prevState != state) 
			GL11.glEnable(GL11.GL_BLEND);
        Minecraft.getMinecraft().mcProfiler.endSection();
	}
	
	/**
     * Transform BlockPos into String to identify DisplayLists
     * @param pos BlockPos for the identifier
     */
	public static String locationIdentifier(BlockPos pos) {
		return pos.getX() + " " + pos.getY() + " " + pos.getZ();
	}
	
	public static final DisplayListCache displayLists = new DisplayListCache();
	
	/**
     * Render tiles with custom models
     * @param te The specified TileEntity
     * @param identifier The unique identifier for the DisplayList
     */
	public static void renderTile(SkullDisplayEntity te, String key, String identifier) {

		Integer displayList = displayLists.get(identifier);
		final OBJRender render = renderer.get(key);
		if (displayList == null) {

			if (!displayLists.isAvailable()) {
				return;
			}		

			displayList = displayLists.newRenderer(() -> {
				GL11.glRotatef(te.getAngle(), 0, 1, 0);
				render.drawModel();

			});
			displayLists.put(identifier, displayList);
		}
		
		render.setTexture(false);
		GL11.glCallList(displayList);
		render.setTexture(true);
	}
}