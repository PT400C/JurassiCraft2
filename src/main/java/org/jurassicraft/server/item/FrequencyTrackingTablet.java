package org.jurassicraft.server.item;

import java.util.List;

import org.jurassicraft.server.tab.TabHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class FrequencyTrackingTablet extends TrackingTablet {
    
	public FrequencyTrackingTablet() {
		super(1);
		this.setCreativeTab(TabHandler.ITEMS);
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {

			tooltip.add("ID: \u00A7b" + getID(stack, Minecraft.getMinecraft().player != null ? Minecraft.getMinecraft().player.getName() : ""));
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		return getID(stack, Minecraft.getMinecraft().player != null ? Minecraft.getMinecraft().player.getName() : "").equals(Minecraft.getMinecraft().player != null ? Minecraft.getMinecraft().player.getName() : "") ? super.getItemStackDisplayName(stack) : TextFormatting.AQUA + getID(stack, Minecraft.getMinecraft().player != null ? Minecraft.getMinecraft().player.getName() : "");
	}
	
	@Override
	public boolean hasFrequencyModule() {
		return true;
	}
}