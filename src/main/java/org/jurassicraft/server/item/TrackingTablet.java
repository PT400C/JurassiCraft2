package org.jurassicraft.server.item;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.jurassicraft.JurassiCraft;
import org.jurassicraft.client.gui.TrackingTabletGui;
import org.jurassicraft.client.gui.TrackingTabletGui.PacketSend;
import org.jurassicraft.client.gui.TuneTabletGui;
import org.jurassicraft.client.proxy.ClientProxy;
import org.jurassicraft.client.tablet.DinosaurInfo;
import org.jurassicraft.server.api.DinosaurProvider;
import org.jurassicraft.server.api.StackNBTProvider;
import org.jurassicraft.server.entity.Dinosaur;
import org.jurassicraft.server.entity.DinosaurEntity;
import org.jurassicraft.server.message.TabletSendData;
import org.jurassicraft.server.registries.JurassicraftRegisteries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TrackingTablet extends ItemTrackable{
	
	private final int maxZoom;
	
	public TrackingTablet(int maxZoom) {
		this.maxZoom = maxZoom;
	}

	public static HashMap<EntityPlayer, QueryData> dataSet = new HashMap<>();
	public static List<DinosaurInfo> dinosaurList = new ArrayList<>();
	
	@Override
	public int getItemStackLimit() {
		return 1;
	}
	public static class DataSender extends TimerTask {

		private EntityPlayer player;
		
		public DataSender(EntityPlayer player) {
			this.player = player;
		}
		
		public void run() {
			
			if((long)(((QueryData)dataSet.get(player)).getTime() + (long) 5000) < System.currentTimeMillis()) {
				dataSet.remove(this.player);
				this.cancel();
			}else {
				
                int area = ((QueryData)dataSet.get(player)).getArea();
                String id = ((QueryData)dataSet.get(player)).getID();
				List<DinosaurInfo> infos = player.world.getEntitiesWithinAABB(DinosaurEntity.class, new AxisAlignedBB(this.player.getPosition().add(area, area, area), this.player.getPosition().add(-area, -area, -area))).stream().filter(e -> e.isAlive() && e.trackers.contains(id)).map(e -> DinosaurInfo.fromEntity(e)).collect(Collectors.toList());
				JurassiCraft.NETWORK_WRAPPER.sendTo(new TabletSendData(infos), (EntityPlayerMP) this.player);
			}

			

		}
	}
	
	public Integer getMaxZoom() {
		return this.maxZoom;
	}
	
	public boolean hasFrequencyModule() {
		return false;
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
		
		if (worldIn.isRemote && !playerIn.isSneaking()) {
			 ItemStack tablet = playerIn.getHeldItem(EnumHand.values()[handIn.ordinal()]);
			ClientProxy.getHandlerInstance().zoom = ((TrackingTablet) tablet.getItem()).getMaxZoom();
			ClientProxy.getHandlerInstance().setActive(true);
			
			this.openGui(handIn, playerIn);
			
		} else {
		
			if(playerIn.isSneaking()) {
				if(hasFrequencyModule()) {
				 byte handID = (byte) handIn.ordinal();
				 byte handIDDart = (byte) (handID ^ 1);
				 ItemStack tablet = playerIn.getHeldItem(EnumHand.values()[handID]);
				 if(playerIn.getHeldItem(EnumHand.values()[handIDDart]).getItem() instanceof TrackingDart){
					 
					 String tabletID = ((TrackingTablet) tablet.getItem()).getID(tablet, playerIn.getName());
					 ItemStack dart = playerIn.getHeldItem(EnumHand.values()[handIDDart]);
					 if(((TrackingDart) dart.getItem()).getID(dart).equals(tabletID)) {
						 playerIn.sendStatusMessage(new TextComponentString(TextFormatting.YELLOW + "The dart has the same ID as the tablet"), true);
					 }else {
						 ((TrackingDart) dart.getItem()).setID(dart, tabletID);
						 playerIn.sendStatusMessage(new TextComponentString(TextFormatting.GREEN + "Synchronized the dart's ID"), true);
					 }
					 
					 
					 
				 }else {
					 playerIn.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Put a tracking dart in your selected slot or off-hand"), true);
				 }
				}else {
					playerIn.sendStatusMessage(new TextComponentString(TextFormatting.RED + "The tablet isn't capable of using frequencies"), true);
				}
			}else {
				
				dataSet.put(playerIn, new QueryData(System.currentTimeMillis(), 0));
			
			}
		}
		return super.onItemRightClick(worldIn, playerIn, handIn);
	}

	
	
	public static String getID(ItemStack stack, String name) {
		if(stack.hasTagCompound())
		return stack.getTagCompound().hasKey("ID") ? stack.getTagCompound().getString("ID") : name;
		return cap(name);
		
	}
	
	@Override
	public void setID(ItemStack stack, String id) {
		if(stack.hasTagCompound())
			stack.getTagCompound().setString("ID", id);
		
	}
	
	private static String cap(String id) {
		if(id.length() >= 13) {
			return id.substring(0, 13) + "...";
		}
		return id;
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		ItemStack stack = player.getHeldItem(hand);
		
		if(!player.isSneaking() && worldIn.isRemote) {
		
				this.openTuneScreen(hand, stack, player);
			
		}
		return EnumActionResult.SUCCESS;
	}

	@SideOnly(Side.CLIENT)
	public void openGui(EnumHand hand, EntityPlayer player) {
		Minecraft.getMinecraft().displayGuiScreen(new TrackingTabletGui(hand, player));
	}
	
	@SideOnly(Side.CLIENT)
	public void openTuneScreen(EnumHand hand, ItemStack stack, EntityPlayer player) {
		Minecraft.getMinecraft().displayGuiScreen(new TuneTabletGui(player, this.getID(stack, player.getName()), (byte)hand.ordinal()));
	}

	@Override
	public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {

		if(!worldIn.isRemote) {
			
			NBTTagCompound nbt = stack.getTagCompound();
	    	
	    	if(nbt == null) {
				nbt = new NBTTagCompound();
				stack.setTagCompound(nbt);
	    	}
		
		if(!nbt.hasKey("ID")) {
			nbt.setString("ID", cap(entityIn.getName()));
		}
		}

  
		super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected);
	}
	
	public static int getArea() {
		return 200 / ClientProxy.getHandlerInstance().getZoomFactor();
	}
	
	public List<DinosaurInfo> getDinosaurInfos() {
		return dinosaurList;
	}


}
