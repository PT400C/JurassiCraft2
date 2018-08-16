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
import org.jurassicraft.client.gui.TrackingTabletGui;
import org.jurassicraft.server.item.QueryData;
import org.jurassicraft.server.item.TrackingTablet;
import org.jurassicraft.server.item.TrackingTablet.*;

import java.util.List;
import java.util.Timer;

public class TabletStartListener extends AbstractMessage<TabletStartListener> {

	private int area;
	private String ID;

	public TabletStartListener() {
	}

	public TabletStartListener(int area, String ID) {
		this.area = area;
		this.ID = ID;
	}

	@Override
	public void onClientReceived(Minecraft client, TabletStartListener message, EntityPlayer player, MessageContext messageContext) {

	}

	@Override
	public void onServerReceived(MinecraftServer server, TabletStartListener message, EntityPlayer player, MessageContext messageContext) {

		if (TrackingTablet.dataSet.containsKey(player)) {
			QueryData dataset = TrackingTablet.dataSet.get(player);
			dataset.setTime(System.currentTimeMillis());
			dataset.setArea(message.area);
			dataset.setID(message.ID);
			if (dataset.getTimer() == null) {
				Timer timer = new Timer();
				timer.scheduleAtFixedRate(new DataSender(player), 0, 5000);
				((QueryData) TrackingTablet.dataSet.get(player)).setTimer(timer);
			}
		}

	}

	@Override
	public void fromBytes(ByteBuf buf) {

		this.area = buf.readInt();
		this.ID = ByteBufUtils.readUTF8String(buf);

	}

	@Override
	public void toBytes(ByteBuf buf) {

		buf.writeInt(this.area);
		ByteBufUtils.writeUTF8String(buf, this.ID);

	}
}