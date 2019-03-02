package org.jurassicraft.client.model.obj;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import org.jurassicraft.server.util.Vec2f;
import org.jurassicraft.server.util.Vec3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class OBJRender {

	private static final boolean testMode = false;
	public final OBJModel model;
	public TextureQuilt texture;
	private int prevTexture = -1;
	
	public OBJRender(OBJModel model, TextureQuilt texture) {
		this.model = model;
		this.texture = texture;
	}
	
	/**
     * Binds and unbinds textures to the OBJ renderer
     * @param unbind true to unbind/reset the texture
     */
	public void setTexture(boolean unbind) {
		int currentTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		if (!unbind && currentTexture != texture.textureID) {
			prevTexture = currentTexture;
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.textureID);
			
		}else if(unbind && prevTexture != -1) {
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTexture);
			prevTexture = -1;
		}
	}
	
	/**
     * Draw the model with the default scale (1)
     */
	public void drawModel() {
		this.drawModel(1);
	}
	
	/**
     * Draw the model with a specific scale
     * @param renderScale Specify the renderScale
     */
	public void drawModel(float renderScale) {
		ArrayList<Integer> faces = new ArrayList<Integer>();
		for (int[] faceArray : this.model.groups.values()) {
			for (int face : faceArray) {
				faces.add(face);
			}
		}
		FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(faces.size() * 3 * 3);
		FloatBuffer normalBuffer = BufferUtils.createFloatBuffer(faces.size() * 3 * 3);
		FloatBuffer texBuffer = BufferUtils.createFloatBuffer(faces.size() * 3 * 2);

		for (int face : faces) {
			for (int[] vertexArray : this.model.getFaceVertices(face)) {
				Vec3f vertex = this.model.getVertices(vertexArray[0]);
				Vec3f vertexNormal = vertexArray[2] != -1 ? this.model.getVertexNormals(vertexArray[2]) : null;
				Vec2f vertexTex = vertexArray[1] != -1 ? this.model.getVertexTex(vertexArray[1]) : null;

				vertexBuffer.put((float) (vertex.x * renderScale));
				vertexBuffer.put((float) (vertex.y * renderScale));
				vertexBuffer.put((float) (vertex.z * renderScale));

				normalBuffer.put((float) (vertexNormal.x));
				normalBuffer.put((float) (vertexNormal.y));
				normalBuffer.put((float) (vertexNormal.z));
				texBuffer.put(vertexTex.x);
				texBuffer.put(-vertexTex.y);
			}
		}
		if(testMode) {
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glColor4f(0.6f, 0.6f, 1, 0.7F);
		}
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		vertexBuffer.flip();
		normalBuffer.flip();
		texBuffer.flip();
		GL11.glTexCoordPointer(2, 0, texBuffer);
		GL11.glNormalPointer(0, normalBuffer);
		GL11.glVertexPointer(3, 0, vertexBuffer);
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, faces.size() * 3);
		GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
		if(testMode)
			GL11.glDisable(GL11.GL_BLEND);
		
		GL11.glColor4f(1, 1, 1, 1);
	}
	
	/**
     * Remove the associated texture from the graphical RAM
     */
	public void purgeTextures() {
		this.texture.purgeTexture();
	}
}
