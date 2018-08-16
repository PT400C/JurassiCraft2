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
import org.jurassicraft.JurassiCraft;
import org.jurassicraft.server.item.TrackingTablet;
import org.jurassicraft.server.message.StopMapSyncMessage;
import org.jurassicraft.server.util.TrackingMapIterator;
import org.jurassicraft.server.util.Vec2d;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class TrackingTabletGui extends GuiScreen {

    private final Minecraft mc = Minecraft.getMinecraft();

    private final DynamicTexture texture;
    private final Biome[] biomeList;
    private final EnumHand hand;
    private final List<RenderDinosaurInfo> renderList = Lists.newArrayList();
    private final int distance;
    private final BlockPos pos;

    private ResourceLocation location;

    private Vec2d lastMouseClicked = new Vec2d(0, 0);
    private Vec2d currentOffset = new Vec2d(0, 0);
    private Point lastMouseUpPos = new Point(0, 0);
    private float zoomedScale = 1f;

    private boolean startRecieved;
    private AnimatedTexture loadingTexture;

    public TrackingTabletGui(BlockPos pos, EnumHand hand, int distance) {
        this.pos = pos;
        this.hand = hand;
        this.distance = distance;
        this.biomeList = new Biome[this.distance * this.distance * 4];
        this.location = mc.getTextureManager().getDynamicTextureLocation("tracking_tablet", texture = new DynamicTexture(this.distance * 2, this.distance * 2));
        refreshDinosaurs();
    }

    @Override
    public void initGui() {
        this.loadingTexture = new AnimatedTexture(new ResourceLocation(JurassiCraft.MODID, "textures/gui/map_loading_" + new Random().nextInt(2) + ".png"), this.width / 2 - 50, this.height / 2 - 50, 100, 100, 100D);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public void refreshDinosaurs() {
        this.renderList.clear();
        ItemStack stack = mc.player.getHeldItem(hand);
        TrackingTablet.TrackingInfo info = new TrackingTablet.TrackingInfo(stack);
        for (TrackingTablet.DinosaurInfo dinosaurInfo : info.getDinosaurInfos()) {
            renderList.add(new RenderDinosaurInfo(mc.player, dinosaurInfo));
        }
    }

    @Override
    public void onGuiClosed() {
        mc.getTextureManager().deleteTexture(this.location);
        JurassiCraft.NETWORK_WRAPPER.sendToServer(new StopMapSyncMessage());
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        refreshDinosaurs();
        this.drawDefaultBackground();
        if(startRecieved) {
            mc.getTextureManager().bindTexture(this.location);
            int i = (int)(256 * this.zoomedScale);
            double xOffset = -currentOffset.x;
            double zOffset = -currentOffset.y;

            double left = this.width / 2 + (xOffset - 128) * this.zoomedScale;
            double top = this.height / 2 + (zOffset - 128) * this.zoomedScale;

            GlStateManager.enableBlend();
            drawModalRectWithCustomSizedTexture(left, top, 0, 0, i, i, i, i);
            GlStateManager.disableBlend();
            drawBorder(left, top, left + i, top + i, 0xFF000000, 2F * this.zoomedScale);
            for (RenderDinosaurInfo r : renderList) {
                ResourceLocation location = r.info.dinosaur.getRegistryName();
                mc.getTextureManager().bindTexture(new ResourceLocation(location.getResourceDomain(), "textures/gui/mapicons/" + location.getResourcePath() + ".png"));
                drawModalRectWithCustomSizedTexture(this.width / 2 + (xOffset + r.x) * this.zoomedScale - 4, this.height / 2 + (zOffset + r.z) * this.zoomedScale - 4, 0, 0, 8, 8, 8, 8);
            }

            double playerX = this.width / 2 + xOffset * this.zoomedScale;
            double playerZ = this.height / 2 + zOffset * this.zoomedScale;
            float playerAngle = 180 + MathHelper.wrapDegrees(mc.player.rotationYaw);
            GlStateManager.translate(playerX, playerZ, 0);
            GlStateManager.rotate(playerAngle, 0, 0, 1);
            mc.getTextureManager().bindTexture(new ResourceLocation(JurassiCraft.MODID, "textures/gui/mapicons/player.png"));
            drawModalRectWithCustomSizedTexture(- 4, - 4, 0, 0, 8, 8, 8, 8);
            GlStateManager.rotate(playerAngle, 0, 0, -1);
            GlStateManager.translate(-playerX, -playerZ, 0);

            int relX = mouseX - width / 2;
            int relY = mouseY - height / 2;
            RenderDinosaurInfo closest = null;
            for (RenderDinosaurInfo render : this.renderList) {
                if(Math.abs(relX - xOffset * this.zoomedScale - render.x * this.zoomedScale + 4) <= 10 && Math.abs(relY - zOffset * this.zoomedScale - render.z * this.zoomedScale + 4) <= 10) {
                    if(closest == null || Vec2d.distance(render.x, render.z, relX, relY) < Vec2d.distance(closest.x, closest.z, relX, relY)) {
                        closest = render;
                    }
                }
            }
            if(closest != null) {
                List<String> lines = Lists.newArrayList();
                TrackingTablet.DinosaurInfo info = closest.info;
                ResourceLocation regName = info.dinosaur.getRegistryName();
                lines.add("Dinosaur: " + I18n.format("entity." + regName.getResourceDomain() + "." + regName.getResourcePath() + ".name"));
                lines.add("Gender: " + I18n.format("gender." + (info.male ? "male" : "female") + ".name"));
                BlockPos pos = info.pos;
                lines.add("At: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
                lines.add("Days existed: " + Math.floor((info.growthPercentage * 8.0F) / 24000.0F));
                this.drawHoveringText(lines, mouseX, mouseY);
            }


            GlStateManager.color(1F, 1F, 1F, 1F);

            int xPos = (int)(MathHelper.clamp(relX / zoomedScale - xOffset, -127F, 127F) / 128F * distance);
            int zPos = (int)(MathHelper.clamp(relY / zoomedScale - zOffset, -127F, 127F) / 128F * distance);

            //When moving the map around, the position can jitter, to prevent this, just default to the last position where the mouse wasnt down, when the mouse is down
            if(Mouse.isButtonDown(0)) {
                xPos = lastMouseUpPos.x;
                zPos = lastMouseUpPos.y;
            } else {
                lastMouseUpPos = new Point(xPos, zPos);
            }

            Biome biomeUnderMouse = this.biomeList[xPos + distance + (zPos + distance) * distance * 2];
            if(biomeUnderMouse != null) {
                String biomeText = biomeUnderMouse.getBiomeName();
                mc.fontRenderer.drawString(biomeText, this.width - 10 - mc.fontRenderer.getStringWidth(biomeText), this.height - 10, 0xFFFFFFFF);
            }

            String zOut = "Z: " + (zPos + pos.getZ());
            String xOut = "X: " + (xPos + pos.getX());
            mc.fontRenderer.drawString(zOut, this.width - 10 - mc.fontRenderer.getStringWidth(zOut), this.height - 20, 0xFFFFFFFF);
            mc.fontRenderer.drawString(xOut, this.width - 10 - mc.fontRenderer.getStringWidth(xOut), this.height - 30, 0xFFFFFFFF);

        } else {
            this.loadingTexture.render();
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public static void drawModalRectWithCustomSizedTexture(double x, double y, float u, float v, int width, int height, float textureWidth, float textureHeight)
    {
        float f = 1.0F / textureWidth;
        float f1 = 1.0F / textureHeight;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x, y + height, 0.0D).tex(u * f,(v + height) * f1).endVertex();
        bufferbuilder.pos(x + width, y + height, 0.0D).tex((u + width) * f,(v + height) * f1).endVertex();
        bufferbuilder.pos(x + width, y, 0.0D).tex((u + width) * f, v * f1).endVertex();
        bufferbuilder.pos(x, y, 0.0D).tex(u * f, v * f1).endVertex();
        tessellator.draw();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if(mouseButton == 0) {
            this.lastMouseClicked = new Vec2d(mouseX, mouseY);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if(clickedMouseButton == 0) {
            currentOffset = new Vec2d(currentOffset.x + (lastMouseClicked.x - mouseX) / this.zoomedScale, currentOffset.y + (lastMouseClicked.y - mouseY) / this.zoomedScale);
            this.lastMouseClicked = new Vec2d(mouseX, mouseY);
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getDWheel();
        if(wheel > 0) {
            this.zoomedScale += 0.2F * this.zoomedScale;
        } else if(wheel < 0) {
            this.zoomedScale -= 0.2F * this.zoomedScale;
        }
    }

    public void upload(List<Integer> list, int offset) {
        for (int i = 0; i < list.size(); i++) {
            Biome biome = Biome.getBiome(list.get(i), Biomes.PLAINS);
            int color;
            if (biome.getBaseHeight() >= 0) {
                color = 0xFF000000 | biome.topBlock.getMapColor(mc.world, BlockPos.ORIGIN).getMapColor(1);
            } else {
                int biomeColor = biome.getWaterColor();
                color = 0xFF000000 | ((biomeColor >> 16 & 255) * 53 / 255) << 16 | ((biomeColor >> 8 & 255) * 78 / 255) << 8 | ((biomeColor & 255) * 244 / 255);
            }
            this.texture.getTextureData()[offset + i] = color;
            this.biomeList[offset + i] = biome;
        }
        int length = this.distance * 2;
        int height = Math.min(Math.floorDiv(list.size(), length) + 1, length);
        int area = length * height;
        int aint[] = new int[area];
        for(int i = 0; i < area; i++) {
            int pos = i + Math.floorDiv(offset, length) * length;
            if(pos < this.texture.getTextureData().length) { //TODO: remove?
                aint[i] = this.texture.getTextureData()[pos];
            }
        }
        GlStateManager.bindTexture(this.texture.getGlTextureId());
        TextureUtil.uploadTextureMipmap(new int[][]{aint}, length, height, 0, Math.floorDiv(offset, length), false, false);
        this.startRecieved = true;
    }

    public static void drawBorder(double left, double top, double right, double bottom, int color, double thickness) {
        thickness /= 2;
        drawRect(left - thickness, top - thickness, right + thickness, top + thickness, color);
        drawRect(left - thickness, bottom - thickness, right + thickness, bottom + thickness, color);
        drawRect(left - thickness, top + thickness, left + thickness, bottom - thickness, color);
        drawRect(right + thickness, top + thickness, right - thickness, bottom - thickness, color);
    }

    public static void drawRect(double left, double top, double right, double bottom, int color) {
        if (left < right) {
            double i = left;
            left = right;
            right = i;
        }

        if (top < bottom) {
            double j = top;
            top = bottom;
            bottom = j;
        }

        float f3 = (float)(color >> 24 & 255) / 255.0F;
        float f = (float)(color >> 16 & 255) / 255.0F;
        float f1 = (float)(color >> 8 & 255) / 255.0F;
        float f2 = (float)(color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(f, f1, f2, f3);
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
        bufferbuilder.pos(left, bottom, 0.0D).endVertex();
        bufferbuilder.pos(right, bottom, 0.0D).endVertex();
        bufferbuilder.pos(right, top, 0.0D).endVertex();
        bufferbuilder.pos(left, top, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private class RenderDinosaurInfo {
        private final TrackingTablet.DinosaurInfo info;
        private final int x;
        private final int z;

        private RenderDinosaurInfo(EntityPlayer player, TrackingTablet.DinosaurInfo info) {
            this.info = info;
            this.x = (info.pos.getX() - player.getPosition().getX()) / (TrackingTabletGui.this.distance / 128);
            this.z = (info.pos.getZ() - player.getPosition().getZ()) / (TrackingTabletGui.this.distance / 128);
        }
    }
}
