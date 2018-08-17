package org.jurassicraft.server.message;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import net.ilexiconn.llibrary.server.network.AbstractMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.jurassicraft.client.gui.TrackingTabletGui;
import org.jurassicraft.server.item.ItemTrackable;
import org.jurassicraft.server.item.TrackingDart;
import org.jurassicraft.server.item.TrackingTablet;

import java.util.List;
import java.util.Timer;

public class UpdateChannelMessage extends AbstractMessage<UpdateChannelMessage> {

	private String id;
	private byte slot;

	public UpdateChannelMessage() {
	}

	public UpdateChannelMessage(String id, byte slot) {
		this.id = id;
		this.slot = slot;
	}

	@Override
	public void onClientReceived(Minecraft client, UpdateChannelMessage message, EntityPlayer player, MessageContext messageContext) {

	}

	@Override
	public void onServerReceived(MinecraftServer server, UpdateChannelMessage message, EntityPlayer player, MessageContext messageContext) {

		ItemStack stack = player.getHeldItem(EnumHand.values()[message.slot]);
        ((ItemTrackable) stack.getItem()).setID(stack, message.id);

	}

	@Override
	public void fromBytes(ByteBuf buf) {

		this.id = ByteBufUtils.readUTF8String(buf);
		this.slot = buf.readByte();

	}

	@Override
	public void toBytes(ByteBuf buf) {

		ByteBufUtils.writeUTF8String(buf, this.id);
		buf.writeByte(this.slot);

	}
}