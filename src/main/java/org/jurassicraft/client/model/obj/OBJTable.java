package org.jurassicraft.client.model.obj;

import org.jurassicraft.client.model.obj.SkullRenderer.Variants;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Registry for every OBJ-rendered TE
 */
@SideOnly(Side.CLIENT)
public enum OBJTable {
	
	SKULL(0);
	
	private int id;
	public static final OBJTable[] ids = OBJTable.values();
	
	private OBJTable(int id) {
		this.id = id;
	}
	
	public int getID() {
		return this.id;
	}

}
