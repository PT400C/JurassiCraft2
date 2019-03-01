package org.jurassicraft.client.model.obj;

import java.util.Map;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IOBJRenderer {

	public Map<Integer, String> getSubRenderers();
	public OBJRender getBakedRenderer(int id);
	public void init();
	
}
