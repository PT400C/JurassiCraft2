package org.jurassicraft.server.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionUtils;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class TrackingDart extends Dart {
    TrackingDart() {
        super((entity, stack) -> {}, (entity, player, id) -> entity.applyTDart(player, id));
    }

    @Override
    @SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
	
			tooltip.add("ID: \u00A7b" + getID(stack));
		
	}
    
    public boolean hasID(ItemStack stack) {
    	if(stack.hasTagCompound())
    		return stack.getTagCompound().hasKey("ID");
		return false;
    	
    }
    
    public String getID(ItemStack stack) {
		if(stack.hasTagCompound())
		return stack.getTagCompound().hasKey("ID") ? String.valueOf(stack.getTagCompound().getString("ID")) : "None";
		return "None";
		
	}
    
    public void setID(ItemStack stack, String ID) {
    	NBTTagCompound nbt = stack.getTagCompound();
    	
    	if(nbt == null) {
			nbt = new NBTTagCompound();
			stack.setTagCompound(nbt);
    	}
			nbt.setString("ID", ID);
		
		
    }

}
