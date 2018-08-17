package org.jurassicraft.server.item;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jurassicraft.server.entity.DinosaurEntity;
import org.jurassicraft.server.util.TriConsumer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class Dart extends ItemTrackable {
	
	@Nonnull
    private final BiConsumer<DinosaurEntity, ItemStack> consumer;
    @Nonnull
    private final TriConsumer<DinosaurEntity, EntityPlayer, String> data;
    
    private final int dartColor;
    
    public Dart(BiConsumer<DinosaurEntity, ItemStack> consumer, int dartColor) {
        this(consumer, -1, null);
    }

    public Dart(BiConsumer<DinosaurEntity, ItemStack> consumer, TriConsumer<DinosaurEntity, EntityPlayer, String> data) {
        this(consumer, -1, data);
    }

    public Dart(BiConsumer<DinosaurEntity, ItemStack> consumer, int dartColor, TriConsumer<DinosaurEntity, EntityPlayer, String> data) {
	    this.consumer = consumer;
	    this.dartColor = dartColor;
	    this.data = data;
    }

    public BiConsumer<DinosaurEntity, ItemStack> getConsumer() {
        return consumer;
    }
    

	public int getDartColor(ItemStack stack) {
        return dartColor;
    }
    
    public TriConsumer<DinosaurEntity, EntityPlayer, String> getData() {
        return data;
    }

	@Override
	public void setID(ItemStack stack, String ID) {
	}
}