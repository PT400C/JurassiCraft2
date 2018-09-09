package org.jurassicraft.server.item;

import java.util.function.BiConsumer;
import javax.annotation.Nonnull;

import org.jurassicraft.common.util.TriConsumer;
import org.jurassicraft.server.entity.DinosaurEntity;
import net.minecraft.item.Item;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

import org.jurassicraft.server.tab.TabHandler;

public class Dart extends ItemTrackable {
    private final BiConsumer<EntityLivingBase, ItemStack> consumer;
    private byte id;
    private String name;
    @Nonnull
    private final TriConsumer<DinosaurEntity, EntityPlayer, String> data;
    private final int dartColor;
    
    public Dart(BiConsumer<EntityLivingBase, ItemStack> consumer, int dartColor, byte id, String name) {
        this(consumer, dartColor, null, id, name);
    }

    public Dart(BiConsumer<EntityLivingBase, ItemStack> consumer, TriConsumer<DinosaurEntity, EntityPlayer, String> data, byte id, String name) {
        this(consumer, -1, data, id, name);
    }

    public Dart(BiConsumer<EntityLivingBase, ItemStack> consumer, int dartColor, TriConsumer<DinosaurEntity, EntityPlayer, String> data, byte id, String name) {
	    this.consumer = consumer;
	    this.dartColor = dartColor;
	    this.setCreativeTab(TabHandler.ITEMS);
	    this.data = data;
	    this.id = id;
	    this.name = name;
}

    public int getDartColor(ItemStack stack) {
        return dartColor;
    }

    public BiConsumer<EntityLivingBase, ItemStack> getConsumer() {
        return consumer;
    }
    
    public TriConsumer<DinosaurEntity, EntityPlayer, String> getData() {
        return this.data;
    }
    
    public boolean isUniversal() {
    	boolean universal = this.id != (byte) TYPES.TRANQ.ordinal() && this.id != (byte) TYPES.TRACKING.ordinal();
        return universal;
    }

	@Override
	public void setID(ItemStack stack, String ID) {
    }

	public String getName() {
		return this.name;
	}

	@Override
	public String getID(ItemStack stack, String name) {
		return null;
	}
	
	public byte getType() {
		return this.id;
	}
	
	public enum TYPES{
		TRANQ, POISON, POTION, TRACKING
	}
}