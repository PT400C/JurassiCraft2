package org.jurassicraft.client.render.entity;

import org.jurassicraft.client.model.obj.OBJHandler;
import org.jurassicraft.client.proxy.ClientProxy;
import org.jurassicraft.server.entity.DummyEntity;
import org.jurassicraft.server.util.Vec3f;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Deprecated
public class DummyEntityRender extends Render<DummyEntity> {
	
	private static ICamera camera = new Frustum();
	private static Vec3f cameraPos = Vec3f.ZERO;

	public DummyEntityRender(RenderManager renderManager) {
		super(renderManager);
	}
	
	@Override
	public boolean shouldRender(DummyEntity entity, ICamera camera, double camX, double camY, double camZ) {
		return true;
	}

	@Override
	public void doRender(DummyEntity particle, double x, double y, double z, float entityYaw, float partialTicks) {
		final Entity cameraEntity = ClientProxy.MC.getRenderViewEntity();
		final float viewX = (float) (cameraEntity.lastTickPosX + (cameraEntity.posX - cameraEntity.lastTickPosX) * partialTicks);
		final float viewY = (float) (cameraEntity.lastTickPosY + (cameraEntity.posY - cameraEntity.lastTickPosY) * partialTicks);
		final float viewZ = (float) (cameraEntity.lastTickPosZ + (cameraEntity.posZ - cameraEntity.lastTickPosZ) * partialTicks);
		camera.setPosition(viewX, viewY, viewZ);
		cameraPos.setVec(viewX, viewY, viewZ);
		OBJHandler.renderTiles(partialTicks, cameraPos, camera);
	}

	@Override
	protected ResourceLocation getEntityTexture(DummyEntity entity) {
		return null;
	}
}