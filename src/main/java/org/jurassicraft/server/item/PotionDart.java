package org.jurassicraft.server.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PotionDart extends Dart {
    public PotionDart() {
        super((entity, stack) -> PotionUtils.getEffectsFromStack(stack).forEach(entity::addPotionEffect), -1, (byte) Dart.TYPES.POTION.ordinal(), "Potion Dart");
    }

    @Override
    public int getDartColor(ItemStack stack) {
    	if(PotionUtils.getColor(stack) != 0xF800F8)
        return PotionUtils.getColor(stack);
    	return 0xC4C933;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        PotionUtils.addPotionTooltip(stack, tooltip, 1.0F);
    }
}