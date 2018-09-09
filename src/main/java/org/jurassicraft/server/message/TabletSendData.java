package org.jurassicraft.server.message;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import net.ilexiconn.llibrary.server.network.AbstractMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.apache.commons.lang3.tuple.Pair;
import org.jurassicraft.client.gui.TrackingTabletGui;
import org.jurassicraft.server.entity.DinosaurEntity;
import org.jurassicraft.server.item.TrackingTablet;
import org.jurassicraft.server.util.DinosaurInfo;

import java.util.ArrayList;
import java.util.List;

public class TabletSendData extends AbstractMessage<TabletSendData> {

	public List<DinosaurInfo> info;

	public TabletSendData() {
	}

	public TabletSendData(List<DinosaurInfo> info) {

		this.info = info;

	}

	@Override
	public void onClientReceived(Minecraft client, TabletSendData message, EntityPlayer player, MessageContext messageContext) {
		TrackingTablet.dinosaurList = message.info;
	}

	@Override
	public void onServerReceived(MinecraftServer server, TabletSendData message, EntityPlayer player, MessageContext messageContext) {

	}

	@Override
	public void fromBytes(ByteBuf buf) {

		int size = buf.readInt();
		List<DinosaurInfo> list = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			list.add(DinosaurInfo.fromData(buf.readLong(), ByteBufUtils.readUTF8String(buf), buf.readBoolean(), buf.readInt(), ByteBufUtils.readUTF8String(buf)));
		}
		this.info = list;

	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(this.info.size());
		for (DinosaurInfo info : this.info) {

			buf.writeLong(info.pos);
			ByteBufUtils.writeUTF8String(buf, info.type);
			buf.writeBoolean(info.male);
			buf.writeInt(info.existed);
			ByteBufUtils.writeUTF8String(buf, info.name);

		}

	}
}