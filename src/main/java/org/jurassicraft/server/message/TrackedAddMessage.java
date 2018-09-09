package org.jurassicraft.server.message;

import io.netty.buffer.ByteBuf;
import net.ilexiconn.llibrary.server.network.AbstractMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.jurassicraft.server.entity.DinosaurEntity;

public class TrackedAddMessage extends AbstractMessage<TrackedAddMessage> {
	private int entityId;
	private boolean success;

	public TrackedAddMessage() {
	}

	public TrackedAddMessage(DinosaurEntity entity, boolean success) {
		this.entityId = entity.getEntityId();
		this.success = success;
	}

	@Override
	public void onClientReceived(Minecraft minecraft, TrackedAddMessage message, EntityPlayer player, MessageContext messageContext) {

		if (message.success) {
			player.sendStatusMessage(new TextComponentString(TextFormatting.GREEN + "Tracking that dinosaur now!"),
					true);
			return;
		}
		player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "This dinosaur is already tracked!"),
				true);

	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.entityId = buf.readInt();
		this.success = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(this.entityId);
		buf.writeBoolean(this.success);
	}

	@Override
	public void onServerReceived(MinecraftServer server, TrackedAddMessage message, EntityPlayer player, MessageContext messageContext) {

	}
}