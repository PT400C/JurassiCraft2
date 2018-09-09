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
import org.jurassicraft.server.item.TrackingTablet;
import org.jurassicraft.server.item.TrackingTablet.*;
import org.jurassicraft.server.util.QueryData;

import java.util.List;
import java.util.Timer;

public class TabletStatusMessage extends AbstractMessage<TabletStatusMessage> {

	private boolean stop = false;
	private int area;
	private String ID;
	private byte hand;

	public TabletStatusMessage() {
	}
	
	public TabletStatusMessage(boolean stop) {
		this.stop = stop;
	}

	public TabletStatusMessage(int area, String ID, byte hand) {
		this.area = area;
		this.ID = ID;
		this.hand = hand;
	}

	@Override
	public void onClientReceived(Minecraft client, TabletStatusMessage message, EntityPlayer player, MessageContext messageContext) {

	}

	@Override
	public void onServerReceived(MinecraftServer server, TabletStatusMessage message, EntityPlayer player, MessageContext messageContext) {

		if(message.stop) {
			
			((Timer) TrackingTablet.dataSet.get(player).getTimer()).cancel();
			TrackingTablet.dataSet.remove(player);
			
		}else {
		
		if (TrackingTablet.dataSet.containsKey(player)) {
			QueryData dataset = TrackingTablet.dataSet.get(player);
			dataset.setTime(System.currentTimeMillis());
			dataset.setArea(message.area);
			dataset.setID(message.ID);
			if (dataset.getTimer() == null) {
				Timer timer = new Timer();
				timer.scheduleAtFixedRate(new DataSender(player, message.hand), 0, 5000);
				((QueryData) TrackingTablet.dataSet.get(player)).setTimer(timer);
			}
		}
		}
	}

	@Override
	public void fromBytes(ByteBuf buf) {
        this.stop = buf.readBoolean();
		if(!this.stop) {
		this.hand = buf.readByte();
		this.area = buf.readInt();
		this.ID = ByteBufUtils.readUTF8String(buf);
		}

	}

	@Override
	public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.stop);
        if(!this.stop) {
		buf.writeByte(this.hand);
		buf.writeInt(this.area);
		ByteBufUtils.writeUTF8String(buf, this.ID);
        }

	}
}