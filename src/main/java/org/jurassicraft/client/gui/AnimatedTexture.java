package org.jurassicraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class AnimatedTexture {

    private final ResourceLocation location;
    private final int frames;

    private final int x;
    private final int z;
    private final int height;
    private final int width;
    private final double frameTime;

    public AnimatedTexture(ResourceLocation location, int x, int z, int height, int width, double frameTime) {
        this.location = location;
        this.x = x;
        this.z = z;
        this.height = height;
        this.width = width;
        this.frameTime = frameTime;
        Minecraft.getMinecraft().getTextureManager().bindTexture(location);
        float imgWidth = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        float imgHeight = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        float frames = imgHeight / imgWidth;
        if((frames * 10) % 10 != 0) {
            throw new IllegalArgumentException("Uneven frames. w:" + imgWidth + " h:" + imgHeight);
        }
        this.frames = (int)frames;
    }

    public void render() {
        Minecraft.getMinecraft().getTextureManager().bindTexture(this.location);
        int frame = (int)((System.currentTimeMillis() / this.frameTime) % this.frames);
        Gui.drawModalRectWithCustomSizedTexture(this.x, this.z - 27, 0, this.height * frame, this.width, this.height, this.width, this.height * this.frames);
        
    }
}
