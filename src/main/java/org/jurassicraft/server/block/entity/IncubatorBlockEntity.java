package org.jurassicraft.server.block.entity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
import org.jurassicraft.JurassiCraft;
import org.jurassicraft.server.api.GrindableItem;
import org.jurassicraft.server.api.IncubatorEnvironmentItem;
import org.jurassicraft.server.container.IncubatorContainer;
import org.jurassicraft.server.item.DinosaurEggItem;
import org.jurassicraft.server.item.ItemHandler;

import com.google.common.primitives.Ints;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

public class IncubatorBlockEntity extends MachineBaseBlockEntity implements TemperatureControl {
    private static final int[] INPUTS = new int[] { 0, 1, 2, 3, 4 };
    private static final int[] ENVIRONMENT = new int[] { 5 };

    private int[] temperature = new int[5];

    private NonNullList<ItemStack> slots = NonNullList.withSize(6, ItemStack.EMPTY);

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        for (int i = 0; i < this.getProcessCount(); i++) {
            this.temperature[i] = compound.getShort("Temperature" + i);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound = super.writeToNBT(compound);

        for (int i = 0; i < this.getProcessCount(); i++) {
            compound.setShort("Temperature" + i, (short) this.temperature[i]);
        }

        return compound;
    }

    @Override
    protected int getProcess(int slot) {
        if (slot == 5) {
            return -1;
        } else {
            return slot;
        }
    }

    @Override
    protected boolean canProcess(int process) {
        ItemStack environment = this.slots.get(5);
        boolean hasEnvironment = false;

        if (!environment.isEmpty()) {
            Item item = environment.getItem();

            if (item instanceof IncubatorEnvironmentItem || Block.getBlockFromItem(item) instanceof IncubatorEnvironmentItem) {
                hasEnvironment = true;
            }
        }

        return hasEnvironment && !this.slots.get(process).isEmpty() && this.slots.get(process).getCount() > 0 && this.slots.get(process).getItem() instanceof DinosaurEggItem;
    }

    @Override
    protected void processItem(int process) {
        if (this.canProcess(process) && !this.world.isRemote) {
            ItemStack egg = this.slots.get(process);

            ItemStack incubatedEgg = new ItemStack(ItemHandler.HATCHED_EGG, 1, egg.getItemDamage());
            NBTTagCompound compound = new NBTTagCompound();
            compound.setBoolean("Gender", this.temperature[process] > 50);

            if (egg.getTagCompound() != null) {
                compound.setString("Genetics", egg.getTagCompound().getString("Genetics"));
                compound.setInteger("DNAQuality", egg.getTagCompound().getInteger("DNAQuality"));
            }

            incubatedEgg.setTagCompound(compound);

            this.decreaseStackSize(5);

            this.slots.set(process, incubatedEgg);
        }
    }

    @Override
    protected int getMainOutput(int process) {
        return 0;
    }

    @Override
    protected int getStackProcessTime(ItemStack stack) {
        return 8000;
    }

    @Override
    protected int getProcessCount() {
        return 5;
    }

    @Override
    protected int[] getInputs() {
        return INPUTS;
    }

    @Override
    protected int[] getInputs(int process) {
        return new int[] { process };
    }

    @Override
    protected int[] getOutputs() {
        return ENVIRONMENT;
    }

    @Override
    protected NonNullList<ItemStack> getSlots() {
        return this.slots;
    }

    @Override
    protected void setSlots(NonNullList<ItemStack> slots) {
        this.slots = slots;
    }

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
        return new IncubatorContainer(playerInventory, this);
    }

    @Override
    public String getGuiID() {
        return JurassiCraft.MODID + ":incubator";
    }

    @Override
    public String getName() {
        return this.hasCustomName() ? this.customName : "container.incubator";
    }

    @Override
    public int getField(int id) {
        if (id < 5) {
            return this.processTime[id];
        } else if (id < 10) {
            return this.totalProcessTime[id - 5];
        } else if (id < 15) {
            return this.temperature[id - 10];
        }

        return 0;
    }

    @Override
    public void setField(int id, int value) {
        if (id < 5) {
            this.processTime[id] = value;
        } else if (id < 10) {
            this.totalProcessTime[id - 5] = value;
        } else if (id < 15) {
            this.temperature[id - 10] = value;
        }
    }
    
    @Override
	public int[] getSlotsForFace(EnumFacing side) {
		
		return ArrayUtils.addAll(INPUTS, ENVIRONMENT);
	}
    
    @Override
	public boolean canExtractItem(int slotID, ItemStack itemstack, EnumFacing side) {
		return true;
	}

	@Override
	public boolean isItemValidForSlot(int slotID, ItemStack itemstack) {
		if (Ints.asList(INPUTS).contains(slotID)) {
			if (itemstack != null && itemstack.getItem() == ItemHandler.EGG) {
				return true;
			}
		}else if(Ints.asList(ENVIRONMENT).contains(slotID)) {
			if (itemstack != null && itemstack.getItem() instanceof IncubatorEnvironmentItem || Block.getBlockFromItem(itemstack.getItem()) instanceof IncubatorEnvironmentItem) {
				return true;
			}
		}

		return false;
	}
	
	@Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing)
    {
        if (facing != null && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
                return (T) handlerPull;
        return super.getCapability(capability, facing);
    }

    @Override
    protected boolean shouldResetProgress() {
        return false;
    }

    @Override
    public void setTemperature(int index, int value) {
        this.temperature[index] = value;
    }

    @Override
    public int getTemperature(int index) {
        return this.temperature[index];
    }

    @Override
    public int getTemperatureCount() {
        return this.temperature.length;
    }

	@Override
	public boolean isEmpty() {
		return false;
	}
	
	IItemHandler handlerPull = new SidedInvWrapper(this, null) {

		@Override
		@Nonnull
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (Ints.asList(INPUTS).contains(slot)) {
				ItemStack stackInSlot = inv.getStackInSlot(slot);
				if (stackInSlot != null && stackInSlot.getItem() == ItemHandler.HATCHED_EGG) {
					return super.extractItem(slot, amount, simulate);
				}

			}
			return ItemStack.EMPTY;

		}
	};
}
