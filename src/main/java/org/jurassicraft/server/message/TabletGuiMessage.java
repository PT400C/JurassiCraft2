package org.jurassicraft.server.message;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import net.ilexiconn.llibrary.server.network.AbstractMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.apache.commons.lang3.tuple.Pair;
import org.jurassicraft.JurassiCraft;
import org.jurassicraft.client.gui.TrackingTabletGui;
import org.jurassicraft.client.proxy.ClientProxy;
import org.jurassicraft.server.entity.DinosaurEntity;
import org.jurassicraft.server.item.TrackingTablet;

import java.util.ArrayList;
import java.util.List;

public class TabletGuiMessage extends AbstractMessage<TabletGuiMessage> {

	private byte id;
	private byte hand;
	private double x;
	private double z;

	public TabletGuiMessage() {
	}

	public TabletGuiMessage(byte id, byte hand, double x, double z) {

		this.id = id;
		this.hand = hand;
		this.x = x;
		this.z = z;

	}

	@Override
	public void onClientReceived(Minecraft client, TabletGuiMessage message, EntityPlayer player, MessageContext messageContext) {
		
	
	}

	@Override
	public void onServerReceived(MinecraftServer server, TabletGuiMessage message, EntityPlayer player, MessageContext messageContext) {
		if(player.getHeldItem(EnumHand.values()[message.hand]).getItem() instanceof TrackingTablet) {
			ItemStack tabletItem = player.getHeldItem(EnumHand.values()[message.hand]);
			boolean state = Boolean.logicalXor(((TrackingTablet) tabletItem.getItem()).isLocked(tabletItem), true);
			((TrackingTablet) tabletItem.getItem()).setLocked(tabletItem, state, message.x, message.z);
		}

	}

	@Override
	public void fromBytes(ByteBuf buf) {
		
		this.id = buf.readByte();
		this.hand = buf.readByte();
		this.x = buf.readDouble();
		this.z = buf.readDouble();

	}

	@Override
	public void toBytes(ByteBuf buf) {
		
		buf.writeByte(this.id);
		buf.writeByte(this.hand);
		buf.writeDouble(this.x);
		buf.writeDouble(this.z);
		

	}
}