package org.jurassicraft.client.model.obj;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.jurassicraft.client.proxy.ClientProxy;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;

public class TextureQuilt {
	public int sourceWidth = 0;
	public int sourceHeight = 0;
	public final int textureID;
	public final ResourceLocation texture;
	private int[] pixels;

	/**
     * Process the texture of an OBJ model
     * @param texture The texture location of the model
     */
	public TextureQuilt(ResourceLocation texture) {
		this.textureID = GL11.glGenTextures();
		this.texture = texture;
		BufferedImage image = null;
		try (InputStream input = ClientProxy.getResourceStream(texture)){
			image = TextureUtil.readBufferedImage(input);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		this.sourceWidth = image.getWidth();
		this.sourceHeight = image.getHeight();	
		this.pixels = image.getRGB(0, 0, this.sourceWidth, this.sourceHeight, null, 0, this.sourceWidth);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textureID);
		TextureUtil.allocateTexture(this.textureID, this.sourceWidth, this.sourceHeight);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		uploadToGPU(textureID);
	}
	
	/**
     * Upload a texture to the GPU
     * @param textureID OpenGL texture identifier
     */
	public void uploadToGPU(int textureID) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(this.sourceWidth * this.sourceHeight * 4);
        for(int y = 0; y < this.sourceHeight; y++){
            for(int x = 0; x < this.sourceWidth; x++){
                int pixel = pixels[x + y * this.sourceWidth];
                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) ((pixel >> 0)& 0xFF));
                buffer.put((byte) ((pixel >> 24) & 0xFF));
            }
        }
        buffer.flip();
		GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, this.sourceWidth, this.sourceHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
	}
	
	/**
     * Remove the associated texture from the graphical RAM
     */
	public void purgeTexture() {
		GL11.glDeleteTextures(textureID);
	}
}
