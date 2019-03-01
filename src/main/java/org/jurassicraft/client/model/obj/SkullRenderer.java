package org.jurassicraft.client.model.obj;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jurassicraft.JurassiCraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SkullRenderer implements IOBJRenderer {
	
	public static final Map<Integer, String> renderer_requests = new HashMap<Integer, String>();
	public static Map<Integer, OBJRender> renderers = new HashMap<Integer, OBJRender>();
	
	static {
		final String hori = "skull_display/tyrannosaurus_horizontal";
		final String verti = "skull_display/tyrannosaurus_vertical";
		final String sit = "skull_display/tyrannosaurus_placed";
		renderer_requests.put(Variants.STANDING.getIDs()[0], hori);
		renderer_requests.put(Variants.STANDING.getIDs()[1], hori);
		renderer_requests.put(Variants.HANGING.getIDs()[0], verti);
		renderer_requests.put(Variants.HANGING.getIDs()[1], verti);
		renderer_requests.put(Variants.SITTING.getIDs()[0], sit);
		renderer_requests.put(Variants.SITTING.getIDs()[1], sit);
	}
	
	public void init() {
		for (Entry<Integer, String> renderer : renderer_requests.entrySet()) {
			ResourceLocation model = new ResourceLocation(JurassiCraft.MODID, "models/block/" + renderer.getValue() + ".obj");
			try {
				final int id = renderer.getKey();
				renderers.put(id, new OBJRender(new OBJModel(model), new TextureQuilt(new ResourceLocation(JurassiCraft.MODID, (id % 2 == 0 ? "textures/blocks/skull_display/tyrannosaurus_fossilized.png" : "textures/blocks/skull_display/tyrannosaurus_fresh.png")))));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public Map<Integer, String> getSubRenderers() {
		return renderer_requests;
	}
	
	@SideOnly(Side.CLIENT)
	public static enum Variants {
		
		STANDING(0, 1),
		HANGING(2, 3),
		SITTING(4, 5);
		
		private int[] id;
		
		private Variants(int... id) {
			this.id = id;
		}
		
		public int[] getIDs() {
			return this.id;
		}
		
	}
	
	public static final Variants[] values = Variants.values();

	@Override
	public OBJRender getBakedRenderer(int id) {
		return renderers.get(id);
	}

}
