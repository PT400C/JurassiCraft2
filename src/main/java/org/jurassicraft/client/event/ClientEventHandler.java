package org.jurassicraft.client.event;

import net.ilexiconn.llibrary.LLibrary;
import net.ilexiconn.llibrary.client.util.ClientUtils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.model.ModelChest;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.List;

import org.jurassicraft.JurassiCraft;
import org.jurassicraft.client.proxy.ClientProxy;
import org.jurassicraft.server.block.SkullDisplay;
import org.jurassicraft.server.block.entity.SkullDisplayEntity;
import org.jurassicraft.server.entity.DinosaurEntity;
import org.jurassicraft.server.entity.vehicle.MultiSeatedEntity;
import org.jurassicraft.server.item.DartGun;
import org.jurassicraft.server.item.ItemHandler;
import org.jurassicraft.server.message.AttemptMoveToSeatMessage;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class ClientEventHandler {
	
    public static final Minecraft MC = Minecraft.getMinecraft();
    private static final ResourceLocation PATREON_BADGE = new ResourceLocation(JurassiCraft.MODID, "textures/items/patreon_badge.png");

    private static boolean isGUI;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        JurassiCraft.timerTicks++;
    }

    @SubscribeEvent
    public static void onGUIRender(GuiScreenEvent.DrawScreenEvent.Pre event) {
        isGUI = true;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            isGUI = false;
        }
    }
    
    @SubscribeEvent
    public void hightlightEvent(final DrawBlockHighlightEvent e) {
    	if (e.getTarget().typeOfHit == RayTraceResult.Type.BLOCK)
        {
    		final BlockPos blockpos = e.getTarget().getBlockPos();
            final IBlockState iblockstate = e.getPlayer().world.getBlockState(blockpos);
            if(iblockstate.getBlock() instanceof SkullDisplay) {
    		e.setCanceled(true);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.glLineWidth(2.0F);
            GlStateManager.disableTexture2D();
            GlStateManager.depthMask(false);
            

            if (iblockstate.getMaterial() != Material.AIR && e.getPlayer().world.getWorldBorder().contains(blockpos))
            {
            	final double x = e.getPlayer().lastTickPosX + (e.getPlayer().posX - e.getPlayer().lastTickPosX) * (double) e.getPartialTicks();
            	final double y = e.getPlayer().lastTickPosY + (e.getPlayer().posY - e.getPlayer().lastTickPosY) * (double) e.getPartialTicks();
            	final double z = e.getPlayer().lastTickPosZ + (e.getPlayer().posZ - e.getPlayer().lastTickPosZ) * (double) e.getPartialTicks();

                GL11.glPushMatrix();
                GlStateManager.translate(-x, -y, -z);
                GlStateManager.translate(blockpos.getX() + 0.5, blockpos.getY(), blockpos.getZ() + 0.5);
                final TileEntity tile = e.getPlayer().world.getTileEntity(blockpos);

                if(tile != null && tile instanceof SkullDisplayEntity && ((SkullDisplayEntity) tile).hasData()) {
                	GlStateManager.rotate(((SkullDisplayEntity) tile).getAngle(), 0.0F, 1.0F, 0.0F);
                }

                
                RenderGlobal.drawSelectionBoundingBox(iblockstate.getCollisionBoundingBox(e.getPlayer().world, blockpos).offset(-0.5, 0, -0.5).grow(0.0020000000949949026D), 0.0f, 0.0f, 0.0f, 0.4f);
                GL11.glPopMatrix();
                
           
            }

            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
      
            }
        }
    }

    @SubscribeEvent
    public static void onGameOverlay(RenderGameOverlayEvent.Post event) {
    	final Minecraft mc = Minecraft.getMinecraft();
    	final EntityPlayer player = mc.player;

        for(final EnumHand hand : EnumHand.values()) {
        	final ItemStack stack = player.getHeldItem(hand);
            if(stack.getItem() == ItemHandler.DART_GUN) {
                ItemStack dartItem = DartGun.getDartItem(stack);
                if(!dartItem.isEmpty()) {
                    RenderItem renderItem = mc.getRenderItem();
                    FontRenderer fontRenderer = mc.fontRenderer;
                    ScaledResolution scaledResolution = new ScaledResolution(mc);

                    int xPosition = scaledResolution.getScaledWidth() - 18;
                    int yPosition = scaledResolution.getScaledHeight() - 18;

                    renderItem.renderItemAndEffectIntoGUI(dartItem, xPosition, yPosition);
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
    public static void keyInputEvent(InputEvent.KeyInputEvent event) {
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
    public static void onPlayerRender(RenderPlayerEvent.Post event) {
    	final EntityPlayer player = event.getEntityPlayer();

        if (ClientProxy.PATRONS.contains(player.getUniqueID()) && !player.isPlayerSleeping() && player.deathTime <= 0 && !player.isInvisible() && !player.isInvisibleToPlayer(MC.player)) {
            GlStateManager.pushMatrix();

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            if (isGUI) {
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            }

            RenderPlayer renderer = event.getRenderer();

            GlStateManager.translate(event.getX(), event.getY(), event.getZ());

            GlStateManager.rotate(-ClientUtils.interpolate(isGUI ? player.renderYawOffset : player.prevRenderYawOffset, player.renderYawOffset, LLibrary.PROXY.getPartialTicks()), 0.0F, 1.0F, 0.0F);

            if (player.isSneaking()) {
                GlStateManager.translate(0.0F, -0.3F, 0.0F);
                GlStateManager.rotate((float) Math.toDegrees(-renderer.getMainModel().bipedBody.rotateAngleY), 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate((float) Math.toDegrees(0.5F), 1.0F, 0.0F, 0.0F);
                GlStateManager.translate(0.0F, -0.15F, -0.665F);
            } else {
                renderer.getMainModel().bipedBody.postRender(0.0625F);
                GlStateManager.rotate((float) Math.toDegrees(-renderer.getMainModel().bipedBody.rotateAngleY) * 2.0F, 0.0F, 1.0F, 0.0F);
            }

            GlStateManager.translate(-0.1F, 1.4F, 0.14F);

            final float scale = 0.35F;

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

            if (isGUI) {
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, OpenGlHelper.lastBrightnessX, OpenGlHelper.lastBrightnessY);
            }
        }
    }
    
	@SubscribeEvent
	public static void onRenderWorldLast(RenderWorldLastEvent event) {

		final Minecraft mc = Minecraft.getMinecraft();
		if (!Minecraft.isGuiEnabled())
			return;
		
		final Entity cameraEntity = mc.getRenderViewEntity();
		Frustum frustrum = new Frustum();
		final double viewX = cameraEntity.lastTickPosX + (cameraEntity.posX - cameraEntity.lastTickPosX) * event.getPartialTicks();
		final double viewY = cameraEntity.lastTickPosY + (cameraEntity.posY - cameraEntity.lastTickPosY) * event.getPartialTicks();
		final double viewZ = cameraEntity.lastTickPosZ + (cameraEntity.posZ - cameraEntity.lastTickPosZ) * event.getPartialTicks();
		frustrum.setPosition(viewX, viewY, viewZ);

		final List<Entity> loadedEntities = mc.world.getLoadedEntityList();
		for (final Entity entity : loadedEntities) {
			if (entity != null && entity instanceof DinosaurEntity) {
				if (entity.isInRangeToRender3d(cameraEntity.getPosition().getX(), cameraEntity.getPosition().getY(), cameraEntity.getPosition().getZ()) && (frustrum.isBoundingBoxInFrustum(entity.getRenderBoundingBox().grow(0.5D))) && entity.isEntityAlive()) {
					((DinosaurEntity) entity).isRendered = true;
				} else {
					((DinosaurEntity) entity).isRendered = false;
				}
			}
		}
	}
}
