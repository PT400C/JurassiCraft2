package org.jurassicraft.server.api;

import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.registries.IForgeRegistry;
import org.jurassicraft.server.entity.Dinosaur;
import org.jurassicraft.server.registries.JurassicraftRegisteries;

import javax.annotation.Nonnull;

public interface TabletProvider extends RegistryStackNBTProvider<Dinosaur> {

    @Override
    default IForgeRegistry<Dinosaur> getRegistry() {
        return JurassicraftRegisteries.DINOSAUR_REGISTRY;
    }

    @Override
    default String getKey() {
        return "dinosaur";
    }

    @Nonnull
    static TabletProvider getFromStack(ItemStack stack) {
        Item item = stack.getItem();
        if(item instanceof TabletProvider) {
            return (TabletProvider)item;
        } else if((item instanceof ItemBlock && ((ItemBlock)item).getBlock() instanceof TabletProvider)) {
            return (TabletProvider)((ItemBlock)item).getBlock();
        } else {
            return MISSING_PROVIDER;
        }
    }

    default boolean isMissing() {
        return this == MISSING_PROVIDER;
    }

    TabletProvider MISSING_PROVIDER = new TabletProvider() {
        @Override
        public ItemStack getItemStack(Dinosaur dinosaur) {
            return ItemStack.EMPTY;
        }

        @Override
        public Dinosaur getValue(ItemStack stack) {
            return Dinosaur.MISSING;
        }
    };
}
