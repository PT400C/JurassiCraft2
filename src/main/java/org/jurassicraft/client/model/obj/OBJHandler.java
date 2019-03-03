package org.jurassicraft.client.model.obj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import org.jurassicraft.JurassiCraft;
import org.jurassicraft.client.proxy.ClientProxy;
import org.jurassicraft.server.util.Vec3f;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class OBJHandler {
	
	public static final MutableBlockPos mbp = new MutableBlockPos();
	
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
		if (!OBJRender.testMode) {	
			if (prevState != state)
				GL11.glDisable(GL11.GL_BLEND);
		}
        List<TileEntity> entities = new ArrayList<TileEntity>(Minecraft.getMinecraft().player.getEntityWorld().loadedTileEntityList);
        for (TileEntity te : entities) {
        	if (te instanceof IOBJTile) {
        		IOBJTile entity = ((IOBJTile) te);
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
	        	        if(OBJRender.testMode) {
	        	        	OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
	        	        }else {
	        	        	OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) j, (float) k);
	        	        }

						Vec3f pos = new Vec3f(mbp.getX() + 0.5F, mbp.getY() + 0.75F, mbp.getZ() + 0.5F).subtract(cameraPos);
						GL11.glTranslated(pos.x, pos.y, pos.z);
		        		renderTile(entity, 0, entity.getVariantID(), mbp.toLong());
	        		}
	        		GL11.glPopMatrix();
	        	}	
        	}
        }
		if (!OBJRender.testMode) {
			if (prevState != state)
				GL11.glEnable(GL11.GL_BLEND);
		}
        Minecraft.getMinecraft().mcProfiler.endSection();
	}
	
	public static final DisplayListCache displayLists = new DisplayListCache();
	
	/**
     * Render tiles with custom models
     * @param te The specified TileEntity
     * @param identifier The unique identifier for the DisplayList
     */
	public static void renderTile(IOBJTile te, int tileID, int variant, long identifier) {
		Integer displayList = displayLists.get(identifier);
		final OBJRender render = RenderRegistry.renderers.get(tileID).getBakedRenderer(variant);
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