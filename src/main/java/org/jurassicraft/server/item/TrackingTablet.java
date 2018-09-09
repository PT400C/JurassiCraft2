package org.jurassicraft.server.item;

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

import org.jurassicraft.JurassiCraft;
import org.jurassicraft.client.gui.TrackingTabletGui;
import org.jurassicraft.client.gui.TuneTabletGui;
import org.jurassicraft.client.proxy.ClientProxy;
import org.jurassicraft.common.util.Vec2d;
import org.jurassicraft.server.conf.JurassiCraftConfig;
import org.jurassicraft.server.entity.DinosaurEntity;
import org.jurassicraft.server.message.TabletMapData;
import org.jurassicraft.server.message.TabletSendData;
import org.jurassicraft.server.tab.TabHandler;
import org.jurassicraft.server.util.DinosaurInfo;
import org.jurassicraft.server.util.QueryData;

import mezz.jei.util.MathUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockDeadBush;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TrackingTablet extends ItemTrackable {

	private final int maxZoom;

	public TrackingTablet(int maxZoom) {
		this.maxZoom = maxZoom;
		if (this.maxZoom == 3) {
			this.setCreativeTab(TabHandler.ITEMS);
			return;
		}
		this.setCreativeTab(null);

	}

	public static HashMap<EntityPlayer, QueryData> dataSet = new HashMap<>();
	public static List<DinosaurInfo> dinosaurList = new ArrayList<>();

	@Override
	public int getItemStackLimit() {
		return 1;
	}

	public static class DataSender extends TimerTask {

		private EntityPlayer player;
		private final byte hand;

		public DataSender(EntityPlayer player, byte hand) {
			this.player = player;
			this.hand = hand;
		}

		public void run() {

			if ((long) (((QueryData) dataSet.get(player)).getTime() + (long) 5000) < System.currentTimeMillis()) {
				dataSet.remove(this.player);
				this.cancel();
			} else {
				int area = ((QueryData) dataSet.get(player)).getArea();
				String id = ((QueryData) dataSet.get(player)).getID();
				MutableBlockPos bp = new MutableBlockPos((int)this.player.posX, 0, (int)this.player.posZ);
				if(((TrackingTablet) player.getHeldItem(EnumHand.values()[this.hand]).getItem()).isLocked(player.getHeldItem(EnumHand.values()[this.hand]))){			
					bp.setPos((int)((TrackingTablet) player.getHeldItem(EnumHand.values()[this.hand]).getItem()).getCoords(player.getHeldItem(EnumHand.values()[this.hand]), player).x, 0 , (int)((TrackingTablet) player.getHeldItem(EnumHand.values()[this.hand]).getItem()).getCoords(player.getHeldItem(EnumHand.values()[this.hand]), player).y);
				}
				List<DinosaurInfo> infos = player.world
						.getEntitiesWithinAABB(DinosaurEntity.class,
								new AxisAlignedBB(bp.add(area, 0, area), bp.add(-area, 255, -area)))
						.stream().filter(e -> e.isAlive() && e.trackers.contains(id) && Vec2d.distance((int)e.posX, (int)e.posZ, (float)player.posX, (float)player.posZ) < 600)
						.map(e -> DinosaurInfo.fromEntity(e)).collect(Collectors.toList());
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
	
	public static void sendChunks(EntityPlayerMP player, ItemStack stack) {
		if(FMLCommonHandler.instance().getMinecraftServerInstance().isSinglePlayer() || JurassiCraftConfig.GENERAL.sendMap) {
			int chunkX = (int) player.posX >> 4;
			int chunkZ = (int) player.posZ >> 4;
			if(((TrackingTablet) stack.getItem()).isLocked(stack)){
				chunkX = (int)((TrackingTablet) stack.getItem()).getCoords(stack, player).x >> 4;
				chunkZ = (int)((TrackingTablet) stack.getItem()).getCoords(stack, player).y >> 4;
			}
		long[] blockpos = new long[200704];
		int[] stateIDs = new int[200704];
		int[] biomes = new int[200704];
		int id = 0;
		MutableBlockPos mb = new MutableBlockPos();
				for(int x = (chunkX - 14); x < (chunkX + 14); x++) {
					for(int z = (chunkZ - 14); z < (chunkZ + 14); z++) {
				Chunk chunk = player.getServerWorld().getChunkProvider().loadChunk(x, z);
				for(int inChunkX = 0; inChunkX < 16; inChunkX++) {
					for(int inChunkZ = 0; inChunkZ < 16; inChunkZ++) {
						if(player.getServerWorld().getChunkProvider().isChunkGeneratedAt(x, z) && chunk != null) {
							//int chunkValue = chunk.getHeightValue(inChunkX, inChunkZ) + 3;
							int chunkValue = chunk.getPrecipitationHeight(new BlockPos(x * 16 + inChunkX, 0 , z * 16 + inChunkZ)).getY();
                        
						for (int i = chunkValue; i >= 0; --i) {
							IBlockState state = chunk.getBlockState(inChunkX, i, inChunkZ);
							if (state == null) {
								continue;
							}
							Block block = state.getBlock();
							if (!(block instanceof BlockAir)) {
								if (state.getRenderType() == EnumBlockRenderType.INVISIBLE || block == Blocks.TORCH
										|| block == Blocks.TALLGRASS || block == Blocks.DOUBLE_PLANT
										|| (block instanceof BlockFlower || block instanceof BlockDoublePlant
												|| block instanceof BlockDeadBush)
										|| (block == Blocks.REDSTONE_TORCH || block == Blocks.REDSTONE_WIRE
												|| block instanceof BlockRedstoneRepeater || block instanceof BlockRedstoneComparator))
									continue;
								mb.setPos(chunk.x * 16 + inChunkX, i, chunk.z * 16 + inChunkZ);
									int blockId = Block.getStateId(block.getExtendedState(state, (IBlockAccess) player.getEntityWorld(), mb));
									biomes[id] = Biome.getIdForBiome(player.getServerWorld().getBiomeProvider().getBiome(mb));
									blockpos[id] = mb.toLong();
									stateIDs[id] = blockId;
							id++;
							break;
							}
							
						
						}
					}else {
						mb.setPos(x * 16 + inChunkX, 0, z * 16 + inChunkZ);
						biomes[id] = 0;
						blockpos[id] = mb.toLong();
						stateIDs[id] = -1;
						id++;
					}
					}
				}
			
				}
	}
				
		JurassiCraft.NETWORK_WRAPPER.sendTo(new TabletMapData(blockpos, stateIDs, biomes, chunkX - 14, chunkZ - 14), (EntityPlayerMP) player);
				
	}
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return false;
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {

		if (worldIn.isRemote && !playerIn.isSneaking()) {
			if(handIn == EnumHand.MAIN_HAND || (handIn == EnumHand.OFF_HAND && !(playerIn.getHeldItem(EnumHand.MAIN_HAND).getItem() instanceof TrackingTablet))) {
			ItemStack tablet = playerIn.getHeldItem(EnumHand.values()[handIn.ordinal()]);
			ClientProxy.getHandlerInstance().zoom = ((TrackingTablet) tablet.getItem()).getMaxZoom();
			ClientProxy.getHandlerInstance().setActive(true);
			ClientProxy.getHandlerInstance().getMap().setInitiated();
			this.openGui(handIn, playerIn);
			}

		} else {
			byte handID = (byte) handIn.ordinal();
			byte handIDDart = (byte) (handID ^ 1);
			if (playerIn.isSneaking()) {
				if (hasFrequencyModule()) {
					
					ItemStack tablet = playerIn.getHeldItem(EnumHand.values()[handID]);
					if (playerIn.getHeldItem(EnumHand.values()[handIDDart]).getItem() instanceof ItemTrackable) {
						if(handIn == EnumHand.MAIN_HAND || (handIn == EnumHand.OFF_HAND && !(playerIn.getHeldItem(EnumHand.MAIN_HAND).getItem() instanceof ItemTrackable))) {
						String tabletID = ((ItemTrackable) tablet.getItem()).getID(tablet, playerIn.getName());
						ItemStack otherTablet = playerIn.getHeldItem(EnumHand.values()[handIDDart]);
						if (((ItemTrackable) otherTablet.getItem()).getID(otherTablet, null).equals(tabletID)) {
							playerIn.sendStatusMessage(new TextComponentString(
									TextFormatting.YELLOW + "Both devices have the same ID"), true);
						} else {
							((ItemTrackable) otherTablet.getItem()).setID(otherTablet, tabletID);
							playerIn.sendStatusMessage(
									new TextComponentString(TextFormatting.GREEN + "Synchronized the IDs"), true);
						}
					}
					} else {
						playerIn.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Put any Tracking Device in your selected slot or off-hand"),true);
					}
				} else {
					playerIn.sendStatusMessage(new TextComponentString(TextFormatting.RED + "The tablet isn't capable of using frequencies"), true);
				}
			} else {
				if(handIn == EnumHand.MAIN_HAND || (handIn == EnumHand.OFF_HAND && !(playerIn.getHeldItem(EnumHand.MAIN_HAND).getItem() instanceof TrackingTablet))) {
			
					JurassiCraft.NETWORK_WRAPPER.sendTo(new TabletMapData((FMLCommonHandler.instance().getMinecraftServerInstance().isSinglePlayer() || JurassiCraftConfig.GENERAL.sendMap ? (byte) TrackingTablet.HandShake.SMAP.ordinal() : (byte) TrackingTablet.HandShake.NMAP.ordinal())), (EntityPlayerMP) playerIn);
					
					dataSet.put(playerIn, new QueryData(System.currentTimeMillis(), 0));
	                sendChunks((EntityPlayerMP) playerIn, playerIn.getHeldItem(handIn));
				}
			
			}
		}
		return super.onItemRightClick(worldIn, playerIn, handIn);
	}
    @Override
	public String getID(ItemStack stack, String name) {
		if (stack.hasTagCompound())
			return stack.getTagCompound().hasKey("ID") ? stack.getTagCompound().getString("ID") : name;
		return cap(name);

	}
    
    public boolean isLocked(ItemStack stack) {
		if (stack.hasTagCompound() && stack.getTagCompound().hasKey("locked"))
			return stack.getTagCompound().getBoolean("locked");
		return false;

	}
    
    public Vec2d getCoords(ItemStack stack, EntityPlayer player) {
		if (stack.hasTagCompound() && stack.getTagCompound().hasKey("x") && stack.getTagCompound().hasKey("z"))
			return new Vec2d(stack.getTagCompound().getDouble("x"), stack.getTagCompound().getDouble("z"));
		return new Vec2d(player.posX, player.posZ);

	}
    
    public void setLocked(ItemStack stack, boolean locked, double x, double z) {
    	NBTTagCompound nbt = stack.getTagCompound();
    	
    	if(nbt == null) {
			nbt = new NBTTagCompound();
			stack.setTagCompound(nbt);
    	}
    	nbt.setBoolean("locked", locked);
    	if(locked) {
    		nbt.setDouble("x", x);
    		nbt.setDouble("z", z);
    	}
			
    }
    
    public static String getPacketID(ItemStack stack, String name) {
    	if (stack.hasTagCompound())
			return stack.getTagCompound().hasKey("ID") ? stack.getTagCompound().getString("ID") : name;
		return cap(name);
    }

	@Override
	public void setID(ItemStack stack, String id) {
		if (stack.hasTagCompound())
			stack.getTagCompound().setString("ID", id);

	}

	private static String cap(String id) {
		if (id.length() >= 13) {
			return id.substring(0, 13) + "...";
		}
		return id;
	}
	
	

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand,
			EnumFacing facing, float hitX, float hitY, float hitZ) {
		ItemStack stack = player.getHeldItem(hand);

		if (!player.isSneaking() && worldIn.isRemote) {
			if (hasFrequencyModule())
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
		Minecraft.getMinecraft().displayGuiScreen(
				new TuneTabletGui(player, this.getID(stack, player.getName()), (byte) hand.ordinal()));
	}

	@Override
	public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {

		if (!worldIn.isRemote) {

			NBTTagCompound nbt = stack.getTagCompound();

			if (nbt == null) {
				nbt = new NBTTagCompound();
				stack.setTagCompound(nbt);
			}

			if (!nbt.hasKey("ID")) {
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

	public enum HandShake {
		IDLE, SMAP, NMAP
	}
	
}