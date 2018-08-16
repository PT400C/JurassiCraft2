package org.jurassicraft.client.gui;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Biomes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.jurassicraft.JurassiCraft;
import org.jurassicraft.client.proxy.ClientProxy;
import org.jurassicraft.client.tablet.DinosaurInfo;
import org.jurassicraft.client.tablet.RenderDinosaurInfo;
import org.jurassicraft.server.item.TrackingTablet;
import org.jurassicraft.server.message.TabletStartListener;
import org.jurassicraft.server.message.TabletStopListener;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

@SideOnly(Side.CLIENT)
public class TrackingTabletGui extends GuiScreen {

	private final Minecraft mc = Minecraft.getMinecraft();

	private final EnumHand hand;
	private final List<RenderDinosaurInfo> renderList = Lists.newArrayList();
	private int ySize = 252;
	private int xSize = 226;
	private final int initZoom = ClientProxy.getHandlerInstance().getZoomFactor();
	private static final ResourceLocation TEXTURE_GUI = new ResourceLocation("jurassicraft:textures/gui/tablet.png");

	private Point lastMouseUpPos = new Point(0, 0);
	private Vec2d lastMouseClicked = new Vec2d(0, 0);
	
	private int[] allowedOff = new int[] { 0, 95, 130, 145 };
	private final Timer packetSend;
	private EntityPlayer player;
	private AnimatedTexture loadingTexture;

	public TrackingTabletGui(EnumHand hand, EntityPlayer player) {
		this.hand = hand;
		this.player = player;
		TrackingTablet.dinosaurList.clear();
		refreshDinosaurs();
		packetSend = new Timer();
		packetSend.scheduleAtFixedRate(new PacketSend(this), 0, 2000);

	}

	public static class PacketSend extends TimerTask {

		private TrackingTabletGui gui;

		public PacketSend(TrackingTabletGui gui) {
			this.gui = gui;
		}

		public void run() {

			JurassiCraft.NETWORK_WRAPPER.sendToServer(new TabletStartListener(TrackingTablet.getArea(), TrackingTablet.getID(this.gui.player.getHeldItem(EnumHand.values()[this.gui.hand.ordinal()]), Minecraft.getMinecraft().player.getName())));
		}
	}

	@Override
	public void initGui() {
		this.loadingTexture = new AnimatedTexture(
				new ResourceLocation(JurassiCraft.MODID, "textures/gui/map_loading_" + new Random().nextInt(2) + ".png"), this.width / 2 - 50, this.height / 2 - 50, 100, 100, 100D);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	public void refreshDinosaurs() {
		this.renderList.clear();
		ItemStack stack = mc.player.getHeldItem(hand);
		for (DinosaurInfo dinosaurInfo : TrackingTablet.dinosaurList) {
			RenderDinosaurInfo s = new RenderDinosaurInfo(mc.player, dinosaurInfo);
			renderList.add(s);
		}
	}

	@Override
	public void onGuiClosed() {
		ClientProxy.getHandlerInstance().setActive(false);
		renderList.clear();
		lastMouseUpPos = new Point(0, 0);
		lastMouseClicked = new Vec2d(0, 0);
		ClientProxy.getHandlerInstance().currentOffset = new Vec2d(0, 0);
		TrackingTablet.dinosaurList.clear();
		packetSend.cancel();
		JurassiCraft.NETWORK_WRAPPER.sendToServer(new TabletStopListener());
		ClientProxy.getHandlerInstance().getMap().finished = false;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {

		this.drawDefaultBackground();
		drawGuiContainerBackgroundLayer2(mouseX, mouseY);
		ClientProxy.getHandlerInstance().starty = (height - ySize) / 2;
		ClientProxy.getHandlerInstance().startx = (width - xSize) / 2;

		Minecraft.getMinecraft().entityRenderer.setupOverlayRendering();

		refreshDinosaurs();

		ClientProxy.getHandlerInstance().getMap().dinosaurs = renderList;

		if (ClientProxy.getHandlerInstance().getMap().finished) {

			ClientProxy.getHandlerInstance().handleDraft(false);

			float relX = mouseX - width / 2;

			float relY = mouseY - height / 2 + 15;

			int xPos = (int) (MathHelper.clamp(relX / ClientProxy.getHandlerInstance().getZoomFactor(), -127F, 127F)
					/ 127F * 256F + ClientProxy.getHandlerInstance().currentOffset.x);
			int zPos = (int) (MathHelper.clamp(relY / ClientProxy.getHandlerInstance().getZoomFactor(), -127F, 127F)
					/ 127F * 256F + ClientProxy.getHandlerInstance().currentOffset.y);

			if (Mouse.isButtonDown(0)) {
				xPos = lastMouseUpPos.x;
				zPos = lastMouseUpPos.y;
			} else {
				lastMouseUpPos = new Point(xPos, zPos);
			}

			int cordX = (int) (xPos + this.player.posX);

			int cordZ = (int) (zPos + this.player.posZ);
			String xOut;
			String zOut;
			if ((width / 2 - 110) + 8 <= mouseX && (width / 2 - 110) + 210 >= mouseX && (height / 2 - 123) + 8 <= mouseY
					&& (height / 2 - 123) + 210 >= mouseY) {
				xOut = "X: " + cordX;
				zOut = "Z: " + cordZ;
			} else {
				xOut = "X: -";
				zOut = "Z: -";
			}
			mc.fontRenderer.drawString(xOut, (width / 2 + 45 - mc.fontRenderer.getStringWidth(xOut) / 2),
					(height / 2 - 123) + 220, 0xFFFFFF);
			mc.fontRenderer.drawString(zOut, (width / 2 + 45 - mc.fontRenderer.getStringWidth(zOut) / 2),
					(height / 2 - 123) + 230, 0xFFFFFF);

			RenderDinosaurInfo closest = null;
			for (RenderDinosaurInfo render : this.renderList) {
				if (render.x - (14 / ClientProxy.getHandlerInstance().getZoomFactor()) <= cordX
						&& render.x + (14 / ClientProxy.getHandlerInstance().getZoomFactor()) >= cordX
						&& render.z - (8 / ClientProxy.getHandlerInstance().getZoomFactor()) <= cordZ
						&& render.z + (14 / ClientProxy.getHandlerInstance().getZoomFactor()) >= cordZ) {

					if (closest == null || Vec2d.distance(render.x, render.z, relX, relY) < Vec2d.distance(closest.x,
							closest.z, relX, relY)) {
						closest = render;
					}
				}
			}
			if (closest != null && !Mouse.isButtonDown(0)) {
				List<String> lines = Lists.newArrayList();
				DinosaurInfo info = closest.info;
				String regName = info.type;
				lines.add("Dinosaur: " + I18n.format("entity." + JurassiCraft.MODID + "." + regName + ".name"));
				if (!info.name.equals(""))
					lines.add("Name: " + info.name);
				lines.add("Gender: " + I18n.format("gender." + (info.male ? "male" : "female") + ".name"));
				BlockPos pos = BlockPos.fromLong(info.pos);
				lines.add("At: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
				lines.add("Days Existed: " + info.existed);
				this.drawHoveringText(lines, mouseX, mouseY);

			}

		} else {
			this.loadingTexture.render();
			ClientProxy.getHandlerInstance().handleDraft(true);
		}
		super.drawScreen(mouseX, mouseY, partialTicks);

	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		if (mouseButton == 0) {

			if ((width / 2 - 110) + 8 <= mouseX && (width / 2 - 110) + 210 >= mouseX && (height / 2 - 123) + 8 <= mouseY
					&& (height / 2 - 123) + 210 >= mouseY)
				this.lastMouseClicked = new Vec2d(mouseX, mouseY);

		}
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		if (clickedMouseButton == 0) {

			if ((width / 2 - 110) + 8 <= mouseX && (width / 2 - 110) + 210 >= mouseX && (height / 2 - 123) + 8 <= mouseY
					&& (height / 2 - 123) + 210 >= mouseY) {
				Vec2d newVector = new Vec2d(
						ClientProxy.getHandlerInstance().currentOffset.x
								+ (lastMouseClicked.x - mouseX) / ClientProxy.getHandlerInstance().getZoomFactor(),
						ClientProxy.getHandlerInstance().currentOffset.y
								+ (lastMouseClicked.y - mouseY) / ClientProxy.getHandlerInstance().getZoomFactor());
				if (-(allowedOff[ClientProxy.getHandlerInstance().getZoomFactor() - 1]) <= newVector.y
						&& allowedOff[ClientProxy.getHandlerInstance().getZoomFactor() - 1] >= newVector.y
						&& -(allowedOff[ClientProxy.getHandlerInstance().getZoomFactor() - 1]) <= newVector.x
						&& allowedOff[ClientProxy.getHandlerInstance().getZoomFactor() - 1] >= newVector.x) {
					ClientProxy.getHandlerInstance().currentOffset = newVector;
				}
				this.lastMouseClicked = new Vec2d(mouseX, mouseY);

			}

		}
		super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		int wheel = Mouse.getDWheel();
		if (wheel > 0 && ClientProxy.getHandlerInstance().zoom < 4) {

			float relX = lastMouseUpPos.x;
			float relY = lastMouseUpPos.y;

			if (ClientProxy.getHandlerInstance().getZoomFactor() == initZoom) {

				ClientProxy.getHandlerInstance().currentOffset.x = (float) MathHelper.clamp(
						ClientProxy.getHandlerInstance().currentOffset.x + relX * 0.75,
						-allowedOff[ClientProxy.getHandlerInstance().getZoomFactor()],
						allowedOff[ClientProxy.getHandlerInstance().getZoomFactor()]);
				ClientProxy.getHandlerInstance().currentOffset.y = (float) MathHelper.clamp(
						ClientProxy.getHandlerInstance().currentOffset.y + relY * 0.75,
						-allowedOff[ClientProxy.getHandlerInstance().getZoomFactor()],
						allowedOff[ClientProxy.getHandlerInstance().getZoomFactor()]);

			}

			ClientProxy.getHandlerInstance().zoom = ClientProxy.getHandlerInstance().zoom + 1;

		} else if (wheel < 0 && ClientProxy.getHandlerInstance().zoom > 1) {
			if (!(-(allowedOff[ClientProxy.getHandlerInstance().getZoomFactor() - 2]) <= ClientProxy
					.getHandlerInstance().currentOffset.y
					&& allowedOff[ClientProxy.getHandlerInstance().getZoomFactor()
							- 2] >= ClientProxy.getHandlerInstance().currentOffset.y)) {

				if (ClientProxy.getHandlerInstance().currentOffset.y > 0) {
					ClientProxy.getHandlerInstance().currentOffset.y = allowedOff[ClientProxy.getHandlerInstance()
							.getZoomFactor() - 2];
				} else {
					ClientProxy.getHandlerInstance().currentOffset.y = -allowedOff[ClientProxy.getHandlerInstance()
							.getZoomFactor() - 2];
				}

			}

			if (!(-(allowedOff[ClientProxy.getHandlerInstance().getZoomFactor() - 2]) <= ClientProxy
					.getHandlerInstance().currentOffset.x
					&& allowedOff[ClientProxy.getHandlerInstance().getZoomFactor()
							- 2] >= ClientProxy.getHandlerInstance().currentOffset.x)) {

				if (ClientProxy.getHandlerInstance().currentOffset.x > 0) {
					ClientProxy.getHandlerInstance().currentOffset.x = allowedOff[ClientProxy.getHandlerInstance().getZoomFactor() - 2];
				} else {
					ClientProxy.getHandlerInstance().currentOffset.x = -allowedOff[ClientProxy.getHandlerInstance().getZoomFactor() - 2];
				}

			}
			if (ClientProxy.getHandlerInstance().zoom - 1 >= this.initZoom)
				ClientProxy.getHandlerInstance().zoom = ClientProxy.getHandlerInstance().zoom - 1;

		}
	}

	protected void drawGuiContainerBackgroundLayer2(int mouseX, int mouseY) {
		mc.renderEngine.bindTexture(TEXTURE_GUI);
		drawTexturedModalRect((width - xSize) / 2, (height - ySize) / 2, 0, 0, xSize, ySize);
	}

}
