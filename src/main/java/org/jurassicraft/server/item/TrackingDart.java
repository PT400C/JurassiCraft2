package org.jurassicraft.server.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

import org.jurassicraft.client.gui.TuneTabletGui;

import java.util.List;

public class TrackingDart extends Dart {

    TrackingDart() {
        super((entity, stack) -> {}, (entity, player, id) -> entity.applyTDart(player, id), (byte) Dart.TYPES.TRACKING.ordinal(), "Tracking Dart");
    }

    @Override
    @SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
	
    	if(!getID(stack, null).equals(""))
			tooltip.add("ID: \u00A7b" + getID(stack, null));
		
	}

    @Override
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		ItemStack stack = player.getHeldItem(hand);
		if(player.isSneaking() && worldIn.isRemote) {
		
				this.openTuneScreen(hand, stack, player);
			
		}
		return EnumActionResult.SUCCESS;
	}
    
    @SideOnly(Side.CLIENT)
	public void openTuneScreen(EnumHand hand, ItemStack stack, EntityPlayer player) {
		Minecraft.getMinecraft().displayGuiScreen(new TuneTabletGui(player, this.getID(stack, null), (byte)hand.ordinal()));
	}
    
    public boolean hasID(ItemStack stack) {
    	if(stack.hasTagCompound())
    		return stack.getTagCompound().hasKey("ID");
		return false;
    	
    }
    @Override
    public String getID(ItemStack stack, String name) {
		if(stack.hasTagCompound())
		return stack.getTagCompound().hasKey("ID") ? String.valueOf(stack.getTagCompound().getString("ID")) : "";
		return "";
		
	}
    
    @Override
    public void setID(ItemStack stack, String ID) {
    	NBTTagCompound nbt = stack.getTagCompound();
    	
    	if(nbt == null) {
			nbt = new NBTTagCompound();
			stack.setTagCompound(nbt);
    	}
			nbt.setString("ID", ID);
    }

}