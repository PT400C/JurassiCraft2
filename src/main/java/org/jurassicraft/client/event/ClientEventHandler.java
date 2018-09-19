package org.jurassicraft.client.event;

import net.ilexiconn.llibrary.LLibrary;
import net.ilexiconn.llibrary.client.util.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.jurassicraft.JurassiCraft;
import org.jurassicraft.client.proxy.ClientProxy;
import org.jurassicraft.server.entity.vehicle.MultiSeatedEntity;
import org.jurassicraft.server.item.DartGun;
import org.jurassicraft.server.item.ItemHandler;
import org.jurassicraft.server.message.AttemptMoveToSeatMessage;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class ClientEventHandler {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final ResourceLocation PATREON_BADGE = new ResourceLocation(JurassiCraft.MODID, "textures/items/patreon_badge.png");

    private boolean isGUI;

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        JurassiCraft.timerTicks++;
    }

    @SubscribeEvent
    public void onGUIRender(GuiScreenEvent.DrawScreenEvent.Pre event) {
        this.isGUI = true;
    }
    
    @SubscribeEvent
   	public void joinWorld(FMLNetworkEvent.ClientConnectedToServerEvent event) {
    	
    	if(ClientProxy.getHandlerInstance().getMap().getThread() == null) {
    		ClientProxy.getHandlerInstance().getMap().createThread();
    	}
    	
   	}

    @SubscribeEvent
	public void leaveWorld(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {

    	if(ClientProxy.getHandlerInstance().getMap().getThread() != null) {
    		ClientProxy.getHandlerInstance().getMap().eliminateThread();
    	}
    	
	}
    
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
        	if (!ClientProxy.getHandlerInstance().getMap().wipingTextures.isEmpty()) {
				int texture = ClientProxy.getHandlerInstance().getMap().wipingTextures.get(0);
				GL11.glDeleteTextures(texture);
				ClientProxy.getHandlerInstance().getMap().wipingTextures.remove(0);
			}
            this.isGUI = false;
        }
    }
    
    @SubscribeEvent
	public void textureStitching(TextureStitchEvent.Post event) {
		if (!(ClientProxy.getHandlerInstance() == null) && !(ClientProxy.getHandlerInstance().getMap() == null))
			ClientProxy.getHandlerInstance().getMap().refreshColors = true;
	}
    
	@SubscribeEvent
	public void setupMap(PlayerSetSpawnEvent event) {
		if (!(ClientProxy.getHandlerInstance() == null) && !(ClientProxy.getHandlerInstance().getMap() == null) && ClientProxy.getHandlerInstance().getMap().isInitiated()) {
			ClientProxy.getHandlerInstance().getMap().refreshColors = true;
			ClientProxy.getHandlerInstance().getMap().markForReset();
			ClientProxy.getHandlerInstance().getMap().mapFrameBuffer.deleteFramebuffer();
			ClientProxy.getHandlerInstance().getMap().overlayFrameBuffer.deleteFramebuffer();
			ClientProxy.getHandlerInstance().getMap().createFrameBuffers(ClientProxy.getHandlerInstance().getMapScale());
			ClientProxy.getHandlerInstance().getMap().resetChunks();
		}
		ClientProxy.getHandlerInstance().initDraft();
		ClientProxy.getHandlerInstance().getMap().resetPersistentChunks();
	}

    @SubscribeEvent
    public void onGameOverlay(RenderGameOverlayEvent.Text event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;

        for(EnumHand hand : EnumHand.values()) {
            ItemStack stack = player.getHeldItem(hand);
            if(stack.getItem() == ItemHandler.DART_GUN) {
                ItemStack dartItem = DartGun.getDartItem(stack);
                if(!dartItem.isEmpty()) {
                    RenderItem renderItem = mc.getRenderItem();
                    FontRenderer fontRenderer = mc.fontRenderer;
                    ScaledResolution scaledResolution = new ScaledResolution(mc);

                    int xPosition = scaledResolution.getScaledWidth() / 2 + 88;
                    int yPosition = scaledResolution.getScaledHeight() - 18;
                    GL11.glEnable(GL11.GL_DEPTH_TEST); 
                    renderItem.renderItemAndEffectIntoGUI(dartItem, xPosition, yPosition);
                    GL11.glDisable(GL11.GL_LIGHTING); 
                    String s = String.valueOf(dartItem.getCount());
                    GlStateManager.disableDepth();
                    fontRenderer.drawStringWithShadow(s, xPosition + 17 - fontRenderer.getStringWidth(s), yPosition + 9, 0xFFFFFFFF);
                    GlStateManager.enableDepth();
                }
                break;
            }
        }
    }

    @SubscribeEvent
    public void keyInputEvent(InputEvent.KeyInputEvent event) {
        int i = 0;
        for(KeyBinding binding : ClientProxy.getKeyHandler().VEHICLE_KEY_BINDINGS) {
            if(binding.isPressed()) {
                EntityPlayer player = Minecraft.getMinecraft().player;
                Entity entity = player.getRidingEntity();
                if(entity instanceof MultiSeatedEntity) {
                    int fromSeat = ((MultiSeatedEntity)entity).getSeatForEntity(player);
                    if(fromSeat != -1) {
                        JurassiCraft.NETWORK_WRAPPER.sendToServer(new AttemptMoveToSeatMessage(entity, fromSeat, i));
                    }
                }
                break;
            }
            ++i;
        }
    }


    @SubscribeEvent
    public void onPlayerRender(RenderPlayerEvent.Post event) {
        EntityPlayer player = event.getEntityPlayer();

        if (!player.isPlayerSleeping() && player.deathTime <= 0 && !player.isInvisible() && !player.isInvisibleToPlayer(MC.player) && ClientProxy.PATRONS.contains(player.getUniqueID())) {
            GlStateManager.pushMatrix();

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            if (this.isGUI) {
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            }

            RenderPlayer renderer = event.getRenderer();

            GlStateManager.translate(event.getX(), event.getY(), event.getZ());

            GlStateManager.rotate(-ClientUtils.interpolate(this.isGUI ? player.renderYawOffset : player.prevRenderYawOffset, player.renderYawOffset, LLibrary.PROXY.getPartialTicks()), 0.0F, 1.0F, 0.0F);

            if (player.isSneaking()) {
                GlStateManager.translate(0.0F, -0.2F, 0.0F);
                GlStateManager.rotate((float) Math.toDegrees(-renderer.getMainModel().bipedBody.rotateAngleY), 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate((float) Math.toDegrees(0.5F), 1.0F, 0.0F, 0.0F);
                GlStateManager.translate(0.0F, -0.15F, -0.68F);
            } else {
                renderer.getMainModel().bipedBody.postRender(0.0625F);
                GlStateManager.rotate((float) Math.toDegrees(-renderer.getMainModel().bipedBody.rotateAngleY) * 2.0F, 0.0F, 1.0F, 0.0F);
            }

            GlStateManager.translate(-0.1F, 1.4F, 0.14F);

            float scale = 0.35F;

            GlStateManager.scale(scale, -scale, scale);

            GlStateManager.disableCull();

            MC.getTextureManager().bindTexture(PATREON_BADGE);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();

            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(0.0, 0.0, 0.0).tex(0.0, 0.0).endVertex();
            buffer.pos(1.0, 0.0, 0.0).tex(1.0, 0.0).endVertex();
            buffer.pos(1.0, 1.0, 0.0).tex(1.0, 1.0).endVertex();
            buffer.pos(0.0, 1.0, 0.0).tex(0.0, 1.0).endVertex();
            tessellator.draw();

            GlStateManager.popMatrix();

            if (this.isGUI) {
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, OpenGlHelper.lastBrightnessX, OpenGlHelper.lastBrightnessY);
            }
        }
    }
}
