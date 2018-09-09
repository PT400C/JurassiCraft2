/*package org.jurassicraft.server.message;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import net.ilexiconn.llibrary.server.network.AbstractMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.jurassicraft.client.gui.TrackingTabletGui;
import org.jurassicraft.server.item.TrackingTablet;

import java.util.List;
import java.util.Timer;

public class TabletStopListener extends AbstractMessage<TabletStopListener> {

	public TabletStopListener() {

	}

	@Override
	public void onClientReceived(Minecraft client, TabletStopListener message, EntityPlayer player, MessageContext messageContext) {

	}

	@Override
	public void onServerReceived(MinecraftServer server, TabletStopListener message, EntityPlayer player, MessageContext messageContext) {

		((Timer) TrackingTablet.dataSet.get(player).getTimer()).cancel();
		TrackingTablet.dataSet.remove(player);

	}

	@Override
	public void fromBytes(ByteBuf buf) {

	}

	@Override
	public void toBytes(ByteBuf buf) {

	}
}*/