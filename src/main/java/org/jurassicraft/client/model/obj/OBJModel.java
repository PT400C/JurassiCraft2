package org.jurassicraft.client.model.obj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.jurassicraft.client.proxy.ClientProxy;
import org.jurassicraft.server.util.Vec2f;
import org.jurassicraft.server.util.Vec3f;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class OBJModel {
	
	public HashMap<String, int[]> groups = new HashMap<String, int[]>();
	public float[] vertices;
	public float[] vertexNormals;
	public float[] vertexTextures;
	public int[] faceVerts;

	/**
     * Parses a new model
     * @param location Location of the model
     */
	public OBJModel(ResourceLocation location) throws IOException {
		InputStream input = ClientProxy.getResourceStream(location);
		String groupName = "";
		ArrayList<Integer> prevGroups = new ArrayList<Integer>();
		ArrayList<Integer> faceVerts = new ArrayList<Integer>();
		ArrayList<Float> vertices = new ArrayList<Float>();
		ArrayList<Float> vertexNormals = new ArrayList<Float>();
		ArrayList<Float> vertexTextures = new ArrayList<Float>();
		int faces = 0;
		String line;
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(input))){
			while ((line = reader.readLine()) != null) {
				
				if (line.startsWith("#")) {
					continue;
				}
				if (line.length() == 0) {
					continue;
				}
				String[] args = line.split(" ");
				String command = args[0];
				switch (command) {
				case "g":
					if (prevGroups.size() > 0) {
						this.groups.put(groupName, ArrayUtils.toPrimitive(prevGroups.toArray(new Integer[0])));
					}
					groupName = args[1].intern();
					prevGroups = new ArrayList<Integer>();
					break;
				case "v":
					vertices.add(Float.parseFloat(args[1]));
					vertices.add(Float.parseFloat(args[2]));
					vertices.add(Float.parseFloat(args[3]));
					break;
				case "vn":
					vertexNormals.add(Float.parseFloat(args[1]));
					vertexNormals.add(Float.parseFloat(args[2]));
					vertexNormals.add(Float.parseFloat(args[3]));
					break;
				case "vt":
					vertexTextures.add(Float.parseFloat(args[1]));
					vertexTextures.add(Float.parseFloat(args[2]));
					break;
				case "f":
					if (args.length == 4) {
						for (int i = 0; i < 3; i++)
							for (int j : readVertex(args[i + 1].intern()))
								faceVerts.add(j - 1);

						prevGroups.add(faces);
						faces++;
					}
					break;
				default:
					break;
				}
			}
		}
		input.close();
		
		this.vertices = ArrayUtils.toPrimitive(vertices.toArray(new Float[0]));
		this.vertexNormals = ArrayUtils.toPrimitive(vertexNormals.toArray(new Float[0]));
		this.vertexTextures = ArrayUtils.toPrimitive(vertexTextures.toArray(new Float[0]));
		this.groups.put(groupName, ArrayUtils.toPrimitive(prevGroups.toArray(new Integer[0])));
		this.faceVerts = ArrayUtils.toPrimitive(faceVerts.toArray(new Integer[0]));
	}
	
	/**
     * Retrieve face vertices
     * @param vertex Vertex identifier
     */
	public int[][] getFaceVertices(int vertex) {
		int[][] points = new int[3][];
		for (int i = 0; i < 3; i ++) 
			points[i] = new int[] {faceVerts[i * 3 + vertex * 9], faceVerts[1 + i * 3 + vertex * 9], faceVerts[2 + i * 3 + vertex * 9]};
		
		return points;
	}
	
	/**
     * Parses a vertex from the OBJ model file
     * @param line String to process
     */
	private static int[] readVertex(String line) {
		String[] vecs = line.intern().split("/");
		int[] point = new int[3];
		for (int i = 0; i < 3; i++) 
			try {
				if (!vecs[i].equals("")) 
					point[i] = Integer.parseInt(vecs[i]);
				
			} catch (NumberFormatException ex) {
				ex.printStackTrace();
			}
			
		return point;
	}
	
	/**
     * Retrieve Vertices, associated to a vertex
     * @param vertex Vertex identifier
     */
	public Vec3f getVertices(int vertex) {
		return new Vec3f(vertices[vertex * 3], vertices[1 + vertex * 3], vertices[2 + vertex * 3]);
	}
	
	/**
     * Retrieve vertex Textures, associated to a vertex
     * @param vertex Vertex identifier
     */
	public Vec2f getVertexTex(int vertex) {
		return new Vec2f(vertexTextures[vertex * 2], vertexTextures[1 + vertex * 2]);
	}
	
	/**
     * Retrieve vertex Normals, associated to a vertex
     * @param vertex Vertex identifier
     */
	public Vec3f getVertexNormals(int vertex) {
		return new Vec3f(vertexNormals[vertex * 3], vertexNormals[1 + vertex * 3], vertexNormals[2 + vertex * 3]);
	}
	
}
