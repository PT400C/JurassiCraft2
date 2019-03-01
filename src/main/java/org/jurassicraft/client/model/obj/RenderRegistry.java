package org.jurassicraft.client.model.obj;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Registry for all OBJ renderers
 */
@SideOnly(Side.CLIENT)
public class RenderRegistry {
	
	public static final Map<Integer, IOBJRenderer> renderers = new HashMap<Integer, IOBJRenderer>();
	
	public static void init(){
		renderers.put(OBJTable.SKULL.getID(), new SkullRenderer());
		
		renderers.forEach((id, render) -> render.init());
	}
	
	public static void reinit() {
		renderers.forEach((id, render) -> render.init());
	}

}
