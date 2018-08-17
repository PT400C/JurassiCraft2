package org.jurassicraft.server.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public abstract class ItemTrackable extends Item {
	
	public abstract void setID(ItemStack stack, String ID);

}
