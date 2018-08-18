package org.jurassicraft.server.item;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
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

public class FrequencyTrackingTablet extends TrackingTablet {

	public FrequencyTrackingTablet() {
		super(1);
	}
	
	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {

			tooltip.add("ID: \u00A7b" + getID(stack, Minecraft.getMinecraft().player != null ? Minecraft.getMinecraft().player.getName() : "None"));

	}
	
	@Override
	public boolean hasFrequencyModule() {
		return true;
	}
}
