package org.jurassicraft.client.gui;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
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
import net.minecraft.init.SoundEvents;
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
import org.jurassicraft.client.tablet.RenderDinosaurInfo;
import org.jurassicraft.common.util.Vec2d;
import org.jurassicraft.server.item.TrackingTablet;
import org.jurassicraft.server.message.TabletGuiMessage;
import org.jurassicraft.server.message.TabletStatusMessage;
import org.jurassicraft.server.util.DinosaurInfo;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
	private ArrayList<ButtonContainer> buttons = new ArrayList<>();
	private final Timer packetSend;
	private EntityPlayer player;
	private AnimatedTexture loadingTexture;
	private ItemStack tabletItem;

	public TrackingTabletGui(EnumHand hand, EntityPlayer player) {
		
		this.hand = hand;
		this.player = player;
		TrackingTablet.dinosaurList.clear();
		refreshDinosaurs();
		packetSend = new Timer();
		packetSend.scheduleAtFixedRate(new PacketSend(this), 0, 2000);
		tabletItem = player.getHeldItem(hand);

	}

	public static class PacketSend extends TimerTask {

		private TrackingTabletGui gui;

		public PacketSend(TrackingTabletGui gui) {
			this.gui = gui;
		}

		public void run() {

			JurassiCraft.NETWORK_WRAPPER.sendToServer(new TabletStatusMessage(TrackingTablet.getArea(), TrackingTablet.getPacketID(this.gui.player.getHeldItem(EnumHand.values()[this.gui.hand.ordinal()]), Minecraft.getMinecraft().player.getName()), (byte)this.gui.hand.ordinal()));
		}
	}

	@Override
	public void initGui() {
		buttons.clear();
		// - BUTTON
		buttons.add(new ButtonContainer((width / 2 - 110) + 210 - 24, (height / 2 - 123) + 210 - 24, 24, 24, (byte) 0));
		// + BUTTON
		buttons.add(new ButtonContainer((width / 2 - 110) + 210 - 24, (height / 2 - 123) + 184 - 24, 24, 24, (byte) 1));
		// LOCK BUTTON
		buttons.add(new ButtonContainer((width / 2 - 110) + 210 - 24, (height / 2 - 123) + 158 - 24, 24, 24, (byte) 2));
		Keyboard.enableRepeatEvents(true);
		this.loadingTexture = new AnimatedTexture(
				new ResourceLocation(JurassiCraft.MODID, "textures/gui/map_loading_" + new Random().nextInt(2) + ".png"), this.width / 2 - 50, this.height / 2 - 50, 100, 100, 100D);
	}
	
	private ButtonContainer isButton(int mouseX, int mouseY) {
		for(ButtonContainer container : this.buttons) {
			if(container.selected(mouseX, mouseY))
				return container;
		}
		return null;
		
	}
	
	private boolean inRange(int allowed, double x, double y) {
		if (-(allowed) <= y && allowed >= y && -(allowed) <= x && allowed >= x) 
			return true;
		
		return false;
		
	}
	
	private boolean isInMap(int mouseX, int mouseY) {
		
		if((width / 2 - 110) + 8 <= mouseX && (width / 2 - 110) + 210 >= mouseX && (height / 2 - 123) + 8 <= mouseY
				&& (height / 2 - 123) + 210 >= mouseY)
			return true;
		return false;
		
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
		Keyboard.enableRepeatEvents(false);
		renderList.clear();
		lastMouseUpPos = new Point(0, 0);
		lastMouseClicked = new Vec2d(0, 0);
		ClientProxy.getHandlerInstance().currentOffset = new Vec2d(0, 0);
		TrackingTablet.dinosaurList.clear();
		packetSend.cancel();
		JurassiCraft.NETWORK_WRAPPER.sendToServer(new TabletStatusMessage(true));
		ClientProxy.getHandlerInstance().getMap().finished = false;
	}
	
	private int getRange(byte current) {
		return ClientProxy.getHandlerInstance().getZoomFactor() == current ? 0 : 95 + 35 * (ClientProxy.getHandlerInstance().getZoomFactor() - current - 1);
	}
	
	@Override
	protected void keyTyped(char c, int i)
	{
		if (i == 1)
        {
            this.mc.displayGuiScreen((GuiScreen)null);

            if (this.mc.currentScreen == null)
            	this.mc.setIngameFocus();
            return;
            
        }
		if(!((TrackingTablet) this.tabletItem.getItem()).isLocked(this.tabletItem)) {
		int x = 0;
		int y = 0;
		
		if(i == Keyboard.KEY_UP || i == Keyboard.KEY_W)
			y = -1;
		if(i == Keyboard.KEY_LEFT || i == Keyboard.KEY_A)
			x = -1;
		if(i == Keyboard.KEY_DOWN || i == Keyboard.KEY_S)
			y = 1;
		if(i == Keyboard.KEY_RIGHT || i == Keyboard.KEY_D)
			x = 1;
		
			Vec2d newVector = new Vec2d(ClientProxy.getHandlerInstance().currentOffset.x + 10 * x / ClientProxy.getHandlerInstance().getZoomFactor(), ClientProxy.getHandlerInstance().currentOffset.y + 10 * y / ClientProxy.getHandlerInstance().getZoomFactor());
			if (inRange(getRange((byte) 1), newVector.x, newVector.y)) {
				ClientProxy.getHandlerInstance().currentOffset = newVector;
			}
		}
		
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

			ClientProxy.getHandlerInstance().handleDraft(false, tabletItem);

			float relX = mouseX - width / 2;

			float relY = mouseY - height / 2 + 15;

			int xPos = (int) (MathHelper.clamp(relX / ClientProxy.getHandlerInstance().getZoomFactor(), -127F, 127F) / 127F * 256F + ClientProxy.getHandlerInstance().currentOffset.x);
			int zPos = (int) (MathHelper.clamp(relY / ClientProxy.getHandlerInstance().getZoomFactor(), -127F, 127F) / 127F * 256F + ClientProxy.getHandlerInstance().currentOffset.y);

			if (Mouse.isButtonDown(0)) {
				xPos = lastMouseUpPos.x;
				zPos = lastMouseUpPos.y;
			} else {
				lastMouseUpPos = new Point(xPos, zPos);
			}

			int cordX = (int) (xPos + (((TrackingTablet) this.tabletItem.getItem()).isLocked(this.tabletItem) ? ((TrackingTablet) this.tabletItem.getItem()).getCoords(this.tabletItem, player).x : this.player.posX));

			int cordZ = (int) (zPos + (((TrackingTablet) this.tabletItem.getItem()).isLocked(this.tabletItem) ? ((TrackingTablet) this.tabletItem.getItem()).getCoords(this.tabletItem, player).y : this.player.posZ));
			String xOut;
			String zOut;
			if (isInMap(mouseX, mouseY)) {
				xOut = "X: " + cordX;
				zOut = "Z: " + cordZ;
			} else {
				xOut = "X: -";
				zOut = "Z: -";
			}
			
		    int wid = mc.fontRenderer.getStringWidth(xOut) > mc.fontRenderer.getStringWidth(zOut) ? mc.fontRenderer.getStringWidth(xOut) + 5 : mc.fontRenderer.getStringWidth(zOut) + 5;
			this.drawGradientRect((width / 2 - 110) + 210 - wid, (height / 2 - 123) + 9, (width / 2 - 110) + 210, ((height / 2 - 123) + 9) + 45, -804253680, -804253680);
			Minecraft.getMinecraft().getTextureManager().bindTexture(ClientProxy.getHandlerInstance().guiTextures);
			Minecraft.getMinecraft().ingameGUI.drawModalRectWithCustomSizedTexture((width / 2 - 110) + 210 - wid / 2 - 7, (height / 2 - 123) + 9 + 3, 33, 0, 15, 15, 48, 48);
			mc.fontRenderer.drawString(xOut, (width / 2 - 110) + 210 - wid / 2 - mc.fontRenderer.getStringWidth(xOut) / 2, (height / 2 - 123) + 9 + 25, 0xFFFFFF);
			mc.fontRenderer.drawString(zOut, (width / 2 - 110) + 210 - wid / 2 - mc.fontRenderer.getStringWidth(zOut) / 2, (height / 2 - 123) + 9 + 35, 0xFFFFFF);
		
			for(ButtonContainer container : buttons) {
				int color = -804253680;
				if(container.selected(mouseX, mouseY)) {
					color = 2139062143;
				}
				if(container.type == (byte) 2 && ((TrackingTablet) this.tabletItem.getItem()).isLocked(this.tabletItem))
					color = 2012912143;
				this.drawGradientRect(container.one.x, container.one.y, container.one.x + container.sizeX, container.one.y + container.sizeZ, color, color);
				int type = container.type;
				if(type == 0) {
						mc.fontRenderer.drawString("-", container.one.x + container.sizeX / 2 - (mc.fontRenderer.getStringWidth("-") - 1) / 2, container.one.y + container.sizeZ / 2 - mc.fontRenderer.getStringWidth("-") / 2, 0xFFFFFF);
				}else if(type == 1) {
					
					mc.fontRenderer.drawString("+", container.one.x + container.sizeX / 2 - (mc.fontRenderer.getStringWidth("+") - 1) / 2, container.one.y + container.sizeZ / 2 - mc.fontRenderer.getStringWidth("+") / 2, 0xFFFFFF);
				}else if(type == 2) {
					Minecraft.getMinecraft().getTextureManager().bindTexture(ClientProxy.getHandlerInstance().guiTextures);
					Minecraft.getMinecraft().ingameGUI.drawModalRectWithCustomSizedTexture(container.one.x + container.sizeX / 2 - 4, container.one.y + container.sizeZ / 2 - 6, 33, 32, 9, 13, 48, 48);
				}
			
				
			}
			
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
				String name = info.type;
				lines.add("Dinosaur: " + name);
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
			ClientProxy.getHandlerInstance().handleDraft(true, tabletItem);
		}
		super.drawScreen(mouseX, mouseY, partialTicks);

	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		if (mouseButton == 0) {

			if (isInMap(mouseX, mouseY)) {
				this.lastMouseClicked = new Vec2d(mouseX, mouseY);
				if(isButton(mouseX, mouseY) != null) {
					Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				if (isButton(mouseX, mouseY).type == 1 && ClientProxy.getHandlerInstance().zoom < 4) {

					ClientProxy.getHandlerInstance().zoom = ClientProxy.getHandlerInstance().zoom + 1;
				}else if(isButton(mouseX, mouseY).type == 0 && ClientProxy.getHandlerInstance().zoom > 1) {
					if (!(-getRange((byte) 2) <= ClientProxy
							.getHandlerInstance().currentOffset.y
							&& getRange((byte) 2) >= ClientProxy.getHandlerInstance().currentOffset.y)) {

						if (ClientProxy.getHandlerInstance().currentOffset.y > 0) {
							ClientProxy.getHandlerInstance().currentOffset.y = getRange((byte) 2);
						} else {
							ClientProxy.getHandlerInstance().currentOffset.y = -getRange((byte) 2);
						}

					}

					if (!(-getRange((byte) 2) <= ClientProxy
							.getHandlerInstance().currentOffset.x
							&& getRange((byte) 2) >= ClientProxy.getHandlerInstance().currentOffset.x)) {

						if (ClientProxy.getHandlerInstance().currentOffset.x > 0) {
							ClientProxy.getHandlerInstance().currentOffset.x = getRange((byte) 2);
						} else {
							ClientProxy.getHandlerInstance().currentOffset.x = -getRange((byte) 2);
						}

					}
					if (ClientProxy.getHandlerInstance().zoom - 1 >= this.initZoom)
						ClientProxy.getHandlerInstance().zoom = ClientProxy.getHandlerInstance().zoom - 1;
				
				}else if(isButton(mouseX, mouseY).type == 2) {
					((TrackingTablet) this.tabletItem.getItem()).setLocked(this.tabletItem, Boolean.logicalXor(((TrackingTablet) this.tabletItem.getItem()).isLocked(this.tabletItem), true), ClientProxy.getHandlerInstance().currentOffset.x + this.player.posX, ClientProxy.getHandlerInstance().currentOffset.y + this.player.posZ);
					JurassiCraft.NETWORK_WRAPPER.sendToServer(new TabletGuiMessage((byte) 0, (byte)this.hand.ordinal(), ClientProxy.getHandlerInstance().currentOffset.x + this.player.posX, ClientProxy.getHandlerInstance().currentOffset.y + this.player.posZ));
						
				}
			}
			}
		}
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		if(!((TrackingTablet) this.tabletItem.getItem()).isLocked(this.tabletItem)) {
		if (clickedMouseButton == 0) {

			if (isInMap(mouseX, mouseY) && isButton(mouseX, mouseY) == null) {
				Vec2d newVector = new Vec2d(
						ClientProxy.getHandlerInstance().currentOffset.x
								+ (lastMouseClicked.x - mouseX) / ClientProxy.getHandlerInstance().getZoomFactor(),
						ClientProxy.getHandlerInstance().currentOffset.y
								+ (lastMouseClicked.y - mouseY) / ClientProxy.getHandlerInstance().getZoomFactor());
				if (inRange(getRange((byte) 1), newVector.x, newVector.y)) {
					ClientProxy.getHandlerInstance().currentOffset = newVector;
				}
				this.lastMouseClicked = new Vec2d(mouseX, mouseY);

			}

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

			if (ClientProxy.getHandlerInstance().getZoomFactor() == initZoom && !((TrackingTablet) this.tabletItem.getItem()).isLocked(this.tabletItem)) {
				ClientProxy.getHandlerInstance().currentOffset.x = (float) MathHelper.clamp(
						ClientProxy.getHandlerInstance().currentOffset.x + relX * 0.75,
						-getRange((byte) 0),
						getRange((byte) 0));
				ClientProxy.getHandlerInstance().currentOffset.y = (float) MathHelper.clamp(
						ClientProxy.getHandlerInstance().currentOffset.y + relY * 0.75,
						-getRange((byte) 0),
						getRange((byte) 0));

			}

			ClientProxy.getHandlerInstance().zoom = ClientProxy.getHandlerInstance().zoom + 1;

		} else if (wheel < 0 && ClientProxy.getHandlerInstance().zoom > 1) {
			if(!((TrackingTablet) this.tabletItem.getItem()).isLocked(this.tabletItem)) {
			if (!(-getRange((byte) 2) <= ClientProxy.getHandlerInstance().currentOffset.y && getRange((byte) 2) >= ClientProxy.getHandlerInstance().currentOffset.y)) {

				if (ClientProxy.getHandlerInstance().currentOffset.y > 0) {
					ClientProxy.getHandlerInstance().currentOffset.y = getRange((byte) 2);
				} else {
					ClientProxy.getHandlerInstance().currentOffset.y = -getRange((byte) 2);
				}

			}

			if (!(-getRange((byte) 2) <= ClientProxy
					.getHandlerInstance().currentOffset.x
					&& getRange((byte) 2) >= ClientProxy.getHandlerInstance().currentOffset.x)) {

				if (ClientProxy.getHandlerInstance().currentOffset.x > 0) {
					ClientProxy.getHandlerInstance().currentOffset.x = getRange((byte) 2);
				} else {
					ClientProxy.getHandlerInstance().currentOffset.x = -getRange((byte) 2);
				}

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

class ButtonContainer {
	
	public final Vec2i one;
	public final int sizeX;
	public final int sizeZ;
	public final byte type;
	
	public ButtonContainer(int x1, int y1, int sizeX, int sizeZ, byte type) {
		this.one = new Vec2i(x1, y1);
		this.sizeX = sizeX;
		this.sizeZ = sizeZ;
		this.type = type;
	}

	public boolean selected(int mouseX, int mouseY) {
		return this.one.x <= mouseX && this.one.x + this.sizeX >= mouseX && this.one.y <= mouseY && this.one.y + sizeZ >= mouseY;
	}

}

class Vec2i {
	
	public int x;
    public int y;
    public Vec2i(int xIn, int yIn)
    {
        this.x = xIn;
        this.y = yIn;
    }
	public static double distance(int x1, int z1, int x2, int z2) {
		return Math.sqrt((z1 - z2) * (z1 - z2) + (x1 - x2) * (x1 - x2));
	}
}