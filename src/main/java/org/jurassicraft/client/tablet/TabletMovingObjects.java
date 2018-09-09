package org.jurassicraft.client.tablet;
import java.util.List;

import org.jurassicraft.JurassiCraft;
import org.jurassicraft.client.proxy.ClientProxy;
import org.jurassicraft.server.item.TrackingTablet;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
public class TabletMovingObjects {
	
	public double getPlayerOffX(EntityPlayer p, ItemStack stack) {
		if(((TrackingTablet) stack.getItem()).isLocked(stack)) {
			ClientProxy.getHandlerInstance().currentOffset.x = 0;
			return ((TrackingTablet) stack.getItem()).getCoords(stack, p).x;
		}
		return p.posX + ClientProxy.getHandlerInstance().currentOffset.x;
	}
	public double getPlayerOffZ(EntityPlayer p, ItemStack stack) {
		if(((TrackingTablet) stack.getItem()).isLocked(stack)) {
			ClientProxy.getHandlerInstance().currentOffset.y = 0;
			return ((TrackingTablet) stack.getItem()).getCoords(stack, p).y;
		}
		return p.posZ + ClientProxy.getHandlerInstance().currentOffset.y;
	}
	public void renderDinosaurIcon(RenderDinosaurInfo e, EntityPlayer p, double X, double Z) {
		
		double offX = e.x - X;
		double offZ =  -(e.z - Z); 
		GL11.glPushMatrix();
		GlStateManager.color(1f, 1f, 1f, 1f);
		GlStateManager.translate((double) (offX * ClientProxy.getHandlerInstance().getZoomFactor()), (double) (offZ * ClientProxy.getHandlerInstance().getZoomFactor()), (double) 0.0);
		GlStateManager.translate((float) 14.5f, (float) 14.5f, (float) 0.0f);
		GL11.glScalef(3f, 3f, 3f);
		Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("jurassicraft:textures/gui/mapicons/" + e.info.type + "_" + (e.info.male ? "1" : "0") + ".png"));
		Minecraft.getMinecraft().ingameGUI.drawModalRectWithCustomSizedTexture(0, 0, 8, 8, -8, -8, 8, -8);
		GL11.glPopMatrix();
	}
	public void renderPlayerArrow(EntityPlayer p, ItemStack stack) {
		
		double offX = p.posX - this.getPlayerOffX(p, stack);
		double offZ = -(p.posZ - this.getPlayerOffZ(p, stack));
		GL11.glPushMatrix();
		GlStateManager.translate((double) (offX * ClientProxy.getHandlerInstance().getZoomFactor()), (double) (offZ * ClientProxy.getHandlerInstance().getZoomFactor()), (double) 0.0);
		GlStateManager.translate((float) -0.5f, (float) -0.5f, (float) 0.0f);
		GL11.glColor4f((float) 0f, (float) 1f, (float) 0f, (float) 1.0f);
		float playerAngle = -MathHelper.wrapDegrees(p.rotationYaw);
		GlStateManager.rotate(playerAngle, 0, 0, 1);
		GL11.glScalef(1f / ClientProxy.getHandlerInstance().getMapScale(), 1f / ClientProxy.getHandlerInstance().getMapScale(), (float) 1.0f);
		Minecraft.getMinecraft().ingameGUI.drawModalRectWithCustomSizedTexture(-8, -8, 17, 0, 16, 16, 48, 48);
		GL11.glPopMatrix();
	}
}