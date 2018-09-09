package org.jurassicraft.server.item;

import com.google.common.collect.Lists;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jurassicraft.server.entity.TranquilizerDartEntity;
import org.jurassicraft.server.tab.TabHandler;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DartGun extends Item {

    public DartGun() {
        this.setCreativeTab(TabHandler.ITEMS);
        this.setMaxStackSize(1);
    }

    private static final int MAX_CARRY_SIZE = 12; //TODO config ?
    
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack itemstack = playerIn.getHeldItem(handIn);

        SoundEvent event = null;
        
        ItemStack dartItem = getDartItem(itemstack);
       
        if(dartItem.isEmpty()) {
        	
                this.setReloading(worldIn, itemstack, true);
        }else if(!dartItem.isEmpty() && dartItem.getCount() != 12 && playerIn.isSneaking()) {
            this.setReloading(worldIn, itemstack, true); 	
        }else if (!worldIn.isRemote) {
        	if(!this.isReloading(itemstack) && this.canShoot(itemstack)){
        	TranquilizerDartEntity dartEntity = null;
        	dartEntity = new TranquilizerDartEntity(worldIn, playerIn, dartItem, null);
    	    if(dartItem.getItem() instanceof TrackingDart)
    	    	dartEntity = new TranquilizerDartEntity(worldIn, playerIn, dartItem, dartID(dartItem, playerIn));
    	    dartEntity.shoot(playerIn, playerIn.rotationPitch, playerIn.rotationYaw, 0.0F, 2.5F, 1.0F);
            worldIn.spawnEntity(dartEntity);
            dartItem.shrink(1);
            fillUpSink(itemstack, dartItem);
            event = SoundEvents.ENTITY_SNOWBALL_THROW;
            this.setTimers(itemstack, 15, false);
        	}
        }
        
        if(event != null) {
            worldIn.playSound(null, playerIn.posX, playerIn.posY, playerIn.posZ, event, SoundCategory.NEUTRAL, 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
        }


        playerIn.addStat(StatList.getObjectUseStats(this));
        return new ActionResult(EnumActionResult.SUCCESS, itemstack);
    }
    
    private String dartID(ItemStack dartItem, EntityPlayer player) {
    	return ((TrackingDart)dartItem.getItem()).getID(dartItem, null).equals("") ? player.getName() : ((TrackingDart)dartItem.getItem()).getID(dartItem, null);
    }
    
    
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
    	return false;
    }
    
    public static ItemStack getDartItem(ItemStack dartGun) {
        NBTTagCompound nbt = dartGun.getOrCreateSubCompound("dart_gun");
        ItemStack stack = new ItemStack(nbt.getCompoundTag("itemstack"));
        stack.setCount(Math.min(stack.getCount(), MAX_CARRY_SIZE));
        return stack;
    }
    
    private boolean fillUpSink(ItemStack dartGun, ItemStack dartItem) {
        boolean hadItem = !dartItem.isEmpty();
        ItemStack dartItem2 = dartItem.splitStack(MAX_CARRY_SIZE);
        dartGun.getOrCreateSubCompound("dart_gun").setTag("itemstack", dartItem2.serializeNBT());
        return hadItem;
    }
    
    private boolean drainItem(ItemStack dartGun, ItemStack dartItem, int count) {
        ItemStack dartItem2 = dartItem.splitStack(1);
        if(dartItem2.getCount() == 1) {
        dartItem2.setCount(count + dartItem2.getCount());
        dartGun.getOrCreateSubCompound("dart_gun").setTag("itemstack", dartItem2.serializeNBT());
        return true;
        }
        return false;
    }
    
	private boolean isReloading(ItemStack stack) {
	
		if (stack.hasTagCompound() && stack.getTagCompound().hasKey("reloading"))
			return stack.getTagCompound().getBoolean("reloading");
		return false;

	}
	
	
	private boolean canShoot(ItemStack stack) {
		if(this.getTicker(stack, false) == 0)
			return true;
		return false;
	}
	
	private int getTicker(ItemStack stack, boolean ticks) {
			return fromInt(getTimerValue(stack), ticks);


	}
	
	private int getTimerValue(ItemStack stack) {
		if (stack.hasTagCompound() && stack.getTagCompound().hasKey("timers"))
			return stack.getTagCompound().getInteger("timers");
		return 0;

	}
	
	private void setTimers(ItemStack stack, int value, boolean ticks) {
    	NBTTagCompound nbt = stack.getTagCompound();
    	if(nbt == null) {
			nbt = new NBTTagCompound();
			stack.setTagCompound(nbt);
    	}
		nbt.setInteger("timers", toInt(getTimerValue(stack), ticks, value));
		
    }
	
	private int toInt(int current, boolean ticks, int value) {
		current = ticks ? (current & ~(0b1111 << 4) | value << 4) : (current & ~0b1111 | value);
		return current;
	}
	
    private int fromInt(int input, boolean ticks) {
    	return ticks ? input >> 4 : input & 0b1111;
		
	}
    
	private void setReloading(World world, ItemStack stack, boolean reloading) {
		if(!world.isRemote) {
    	NBTTagCompound nbt = stack.getTagCompound();
    	if(nbt == null) {
			nbt = new NBTTagCompound();
			stack.setTagCompound(nbt);
    	}
		nbt.setBoolean("reloading", reloading);
		}
		
    }
	
	@Override
	public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
		super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected);
		if(this.isReloading(stack) && this.getTicker(stack, true) > 6 && !worldIn.isRemote) {
			
			this.setTimers(stack, 0, true);
		        ItemStack dartItem = getDartItem(stack);
		        if(entityIn instanceof EntityPlayer) {
		        EntityPlayer playerIn = (EntityPlayer) entityIn;
		        
		        byte slot = (byte) itemSlot;
		    	int dartSlot = -1;
		    	int distance = 10;
		        for(int id = 0; id < 9; id++) {
		        
		        	if(playerIn.inventory.getStackInSlot(id).getItem() instanceof Dart && distance > Math.abs(id - slot)) {
		        
		        		if(!(dartItem.getItem() instanceof Dart) ? true : ((Dart) playerIn.inventory.getStackInSlot(id).getItem()).getType() == ((Dart) dartItem.getItem()).getType()) {
		        			distance = Math.abs(id - slot);
			        		dartSlot = id;
		        		}
		        	
		        		
		        	}
		        }
		        ItemStack dart = ItemStack.EMPTY;
		        if(dartSlot == -1 && playerIn.getHeldItemOffhand().getItem() instanceof Dart && (!(dartItem.getItem() instanceof Dart) ? true : ((Dart) playerIn.getHeldItemOffhand().getItem()).getType() == ((Dart) dartItem.getItem()).getType())) {
		        	dart = playerIn.getHeldItemOffhand();
		        }else if(dartSlot == -1){
		        	 List<Slot> list = Lists.newArrayList(playerIn.inventoryContainer.inventorySlots);
		        	 Collections.reverse(list);
		        	 dart = list.stream().map(Slot::getStack).filter(stackD -> stackD.getItem() instanceof Dart && (!(dartItem.getItem() instanceof Dart) ? true : ((Dart) stackD.getItem()).getType() == ((Dart) dartItem.getItem()).getType())).findFirst().orElse(ItemStack.EMPTY);
		        }else {
		        	dart = playerIn.inventory.getStackInSlot(dartSlot);
		        }
		
			if(dartItem.getCount() != 12) {
	        	
	        
		            if(drainItem(stack, dart, dartItem.getCount())) {
		            worldIn.playSound(null, playerIn.posX, playerIn.posY, playerIn.posZ, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.NEUTRAL, 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
		            }else {
		            	this.setReloading(worldIn, stack, false);
		            }
	        	    
		        }else {
		        	 this.setReloading(worldIn, stack, false);
		        }
		}
		}
		if(!worldIn.isRemote) {
			if(this.isReloading(stack)) 
				this.setTimers(stack, this.getTicker(stack, true) + 1, true);
			if(this.getTicker(stack, false) > 0)
				this.setTimers(stack, this.getTicker(stack, false) - 1, false);
		}
	}
}