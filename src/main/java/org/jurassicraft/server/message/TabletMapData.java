package org.jurassicraft.server.message;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import net.ilexiconn.llibrary.server.network.AbstractMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import org.apache.commons.lang3.tuple.Pair;
import org.jurassicraft.client.gui.TrackingTabletGui;
import org.jurassicraft.client.proxy.ClientProxy;
import org.jurassicraft.server.entity.DinosaurEntity;
import org.jurassicraft.server.item.TrackingTablet;

import java.util.ArrayList;
import java.util.List;

public class TabletMapData extends AbstractMessage<TabletMapData> {

	private byte handshake;
	private long[] blockpos;
	private int[] stateIDs;
	private int[] biomes;
	private int chunkX;
	private int chunkZ;

	public TabletMapData() {
	}

	public TabletMapData(long[] blockpos, int[] stateIDs, int[] biomes, int chunkX, int chunkZ) {

		this.blockpos = blockpos;
		this.stateIDs = stateIDs;
		this.biomes = biomes;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;	

	}
	
	public TabletMapData(byte handshake) {

		this.handshake = handshake;

	}

	@Override
	public void onClientReceived(Minecraft client, TabletMapData message, EntityPlayer player, MessageContext messageContext) {
		
		if(message.handshake != TrackingTablet.HandShake.IDLE.ordinal()) {
			
			ClientProxy.getHandlerInstance().getMap().receivedHandshake(message.handshake);

		}else {
		
		for (int i = 0; i < message.blockpos.length; i++) {
				ClientProxy.getHandlerInstance().getMap().blockStates[Math.abs(BlockPos.fromLong(message.blockpos[i]).getX() - BlockPos.fromLong(message.blockpos[0]).getX())][Math.abs(BlockPos.fromLong(message.blockpos[i]).getZ() - BlockPos.fromLong(message.blockpos[0]).getZ())] = message.stateIDs[i];
				ClientProxy.getHandlerInstance().getMap().blockHeights[Math.abs(BlockPos.fromLong(message.blockpos[i]).getX() - BlockPos.fromLong(message.blockpos[0]).getX())][Math.abs(BlockPos.fromLong(message.blockpos[i]).getZ() - BlockPos.fromLong(message.blockpos[0]).getZ())] = BlockPos.fromLong(message.blockpos[i]).getY();
				ClientProxy.getHandlerInstance().getMap().biomes[Math.abs(BlockPos.fromLong(message.blockpos[i]).getX() - BlockPos.fromLong(message.blockpos[0]).getX())][Math.abs(BlockPos.fromLong(message.blockpos[i]).getZ() - BlockPos.fromLong(message.blockpos[0]).getZ())] = message.biomes[i];
			
		}
		ClientProxy.getHandlerInstance().getMap().chunkX = message.chunkX;
		ClientProxy.getHandlerInstance().getMap().chunkZ = message.chunkZ;
		}
	}

	@Override
	public void onServerReceived(MinecraftServer server, TabletMapData message, EntityPlayer player, MessageContext messageContext) {

	}

	@Override
	public void fromBytes(ByteBuf buf) {
		
		this.handshake = buf.readByte();
        if(this.handshake != TrackingTablet.HandShake.NMAP.ordinal() && this.handshake != TrackingTablet.HandShake.SMAP.ordinal()) {
		this.chunkX = buf.readInt();
		this.chunkZ = buf.readInt();
		int sizePos = buf.readInt();
		long[] blockpos = new long[sizePos];
		for (int i = 0; i < sizePos; i++) {
			blockpos[i] = buf.readLong();
		}
		this.blockpos = blockpos;
		int sizeID = buf.readInt();
		int[] stateIds = new int[sizeID];
		for (int i = 0; i < sizeID; i++) {
			stateIds[i] = buf.readInt();
		}
		this.stateIDs = stateIds;
		
		int sizeBiomes = buf.readInt();
		int[] biomes = new int[sizeBiomes];
		for (int i = 0; i < sizeBiomes; i++) {
			biomes[i] = buf.readInt();
		}
		this.biomes = biomes;
        }

	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeByte(this.handshake);
		if(this.handshake != TrackingTablet.HandShake.NMAP.ordinal() && this.handshake != TrackingTablet.HandShake.SMAP.ordinal()) {
		buf.writeInt(this.chunkX);
		buf.writeInt(this.chunkZ);
		buf.writeInt(blockpos.length);
		for (long pos : blockpos) {

			buf.writeLong(pos);

		}
		buf.writeInt(stateIDs.length);
		for (int id : stateIDs) {

			buf.writeInt(id);

		}
		buf.writeInt(biomes.length);
		for (int biome : biomes) {

			buf.writeInt(biome);

		}
		}

	}
}