package org.jurassicraft.client.tablet;

import org.jurassicraft.common.util.Vec2d;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class HandlerInstance {
	
	private TabletMap map;
	public volatile boolean active = false;
	private Minecraft mc = Minecraft.getMinecraft();
	double mapSizeBefore = 0;
	public static final ResourceLocation guiTextures = new ResourceLocation("jurassicraft", "textures/gui/tablet_interface.png");
	public ScaledResolution scaledresolution;
	public int x = 0;
	public int y = 0;
	public int startx = 0;
	public int starty = 0;
	public Vec2d currentOffset = new Vec2d(0, 0);
	public int zoom = 3;

	public int getZoomFactor() {
		return this.zoom;
	}

	public HandlerInstance() {
		
		this.map = new TabletMap();
	}

	public synchronized boolean getActive() {
		return this.active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public TabletMap getMap() {
		return this.map;
	}

	public void handleDraft(boolean loading, ItemStack stack) {
		
		this.scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
		int width = this.scaledresolution.getScaledWidth();
		int height = this.scaledresolution.getScaledHeight();
		x = width / 2 - 110;
		y = height / 2 - 123;
		int scale = this.scaledresolution.getScaleFactor();
		drawInterface(width, height, scale, loading, stack);

	}

	public float getMapScale() {
		
		return this.scaledresolution.getScaleFactor() / 2F;
	}

	public void initDraft() {
		
		this.scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
		int width = this.scaledresolution.getScaledWidth();
		int height = this.scaledresolution.getScaledHeight();
		x = width / 2 - 110;
		y = height / 2 - 123;
		int scale = this.scaledresolution.getScaleFactor();
		initInterface(width, height, scale);

	}

	public void initInterface(int width, int height, int scale) {
		
		if (!this.map.FBOActive) {
			this.map.createFrameBuffers(getMapScale());
		}
		if (this.mapSizeBefore != getMapScale()) {
			this.mapSizeBefore = getMapScale();
			this.map.markForReset();
		}
	}

	public void drawInterface(int width, int height, int scale, boolean loading, ItemStack stack) {
		int bufferDimensions = (int)(512 * getMapScale());
		int textureDimensions = 48;
		int mapDimensions = 400;
		if(!loading) {
		if (!this.map.FBOActive) {
			this.map.createFrameBuffers(getMapScale());	
		}
		if (this.mapSizeBefore != getMapScale()) {
			this.scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
			this.mapSizeBefore = getMapScale();
			this.map.markForReset();
			this.map.mapFrameBuffer.deleteFramebuffer();
			this.map.overlayFrameBuffer.deleteFramebuffer();
			this.map.createFrameBuffers(getMapScale());
		}
	    }
		float mapScale = (float) scale / 2.0f / getMapScale();
		int mapHeight,mapWidth;
		mapHeight = mapWidth = mapDimensions;
		RenderHelper.disableStandardItemLighting();
		if(!loading) {
		GL11.glColor4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
		this.map.renderChunks((EntityPlayer) this.mc.player, mapWidth, stack);
		Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
		GlStateManager.viewport((int) 0, (int) 0, (int) Minecraft.getMinecraft().getFramebuffer().framebufferWidth,
				(int) Minecraft.getMinecraft().getFramebuffer().framebufferHeight);
		this.map.overlayFrameBuffer.bindFramebufferTexture();
	    }
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GlStateManager.enableAlpha();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glScalef((float) (1.0f / mapScale), (float) (1.0f / mapScale), (float) 1.0f);
		int scaledX = (int) ((float) this.x * mapScale);
		int scaledY = (int) ((float) this.y * mapScale);
		if(!loading) {
		this.mc.ingameGUI.drawTexturedModalRect((int) ((float) (scaledX + 9)),
				(int) ((float) (scaledY + 9)), 0, 0, (int) ((float) (mapWidth / 2 + 1)),
				(int) ((float) (mapHeight / 2 + 1)));
		}
		this.mc.getTextureManager().bindTexture(HandlerInstance.guiTextures);

		int horLineLength = (mapWidth / 2 - 16) / 16;
		for (int i = 0; i < horLineLength; ++i) {
			this.mc.ingameGUI.drawModalRectWithCustomSizedTexture(scaledX + i * 16 + 22, scaledY + 5, 17, 32, 16, 4, textureDimensions, textureDimensions);
			this.mc.ingameGUI.drawModalRectWithCustomSizedTexture(scaledX + i * 16 + 22, scaledY + mapHeight / 2 + 10, 17, 36, 16, 4, textureDimensions, textureDimensions);
		}
		int vertLineLength = (mapHeight / 2 - 16) / 15;
		for (int i = 0; i < vertLineLength; ++i) {
			this.mc.ingameGUI.drawModalRectWithCustomSizedTexture(scaledX + 5, scaledY + i * 16 + 20, 17, 16, 4, 16, textureDimensions, textureDimensions);
			this.mc.ingameGUI.drawModalRectWithCustomSizedTexture(scaledX + mapWidth / 2 + 10, scaledY + i * 16 + 20, 21, 16, 4, 16, textureDimensions, textureDimensions);
		}
		
		this.mc.ingameGUI.drawModalRectWithCustomSizedTexture(scaledX + mapWidth / 2 - 3, scaledY + 5, 0, 15, 17, 15, textureDimensions, textureDimensions);
		this.mc.ingameGUI.drawModalRectWithCustomSizedTexture(scaledX + 5, scaledY + 5, 0, 30, 17, 15, textureDimensions, textureDimensions);
		this.mc.ingameGUI.drawModalRectWithCustomSizedTexture(scaledX + mapWidth / 2 - 3, scaledY + mapHeight / 2 - 1, 25, 17, 17, 15, textureDimensions, textureDimensions);
		this.mc.ingameGUI.drawModalRectWithCustomSizedTexture(scaledX + 5, scaledY + mapHeight / 2 - 1, 0, 0, 17, 15, textureDimensions, textureDimensions);
		GL11.glPushMatrix();
		GlStateManager.scale((float) 0.5f, (float) 0.5f, (float) 1.0f);
		GlStateManager.translate((float) (2 * scaledX + mapWidth / 2 + 18), (float) (2 * scaledY + mapWidth / 2 + 18), (float) 0.0f);
		GL11.glPopMatrix();
		GL11.glScalef((float) mapScale, (float) mapScale, (float) 1.0f);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	}

}