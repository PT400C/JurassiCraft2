package org.jurassicraft.server.message;

import io.netty.buffer.ByteBuf;
import net.ilexiconn.llibrary.server.network.AbstractMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemRecord;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jurassicraft.client.sound.EntitySound;
import org.jurassicraft.server.entity.vehicle.VehicleEntity;

public class CarEntityPlayRecord extends AbstractMessage<CarEntityPlayRecord> {

    private int entityId;
    private SoundEvent soundEvent;

    @SuppressWarnings("unused")
    public CarEntityPlayRecord(){}

    public CarEntityPlayRecord(VehicleEntity entity, ItemRecord record){
        soundEvent = record.getSound();
        entityId = entity.getEntityId();
    }


    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        ByteBufUtils.writeRegistryEntry(buf, soundEvent);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        soundEvent = ByteBufUtils.readRegistryEntry(buf, ForgeRegistries.SOUND_EVENTS);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onClientReceived(Minecraft client, CarEntityPlayRecord message, EntityPlayer player, MessageContext messageContext) {
        Entity entity = player.world.getEntityByID(message.entityId);
        if(entity instanceof VehicleEntity) {
            VehicleEntity carEntity = (VehicleEntity)entity;
            if(carEntity.sound != null) {
                carEntity.sound.setFinished();
            }
            carEntity.sound = new EntitySound<>(carEntity, message.soundEvent, SoundCategory.RECORDS, car -> car.getItem().getItem() instanceof ItemRecord && ((ItemRecord)car.getItem().getItem()).getSound() == message.soundEvent);
            client.getSoundHandler().playSound(carEntity.sound);
        }
    }

    @Override
    public void onServerReceived(MinecraftServer server, CarEntityPlayRecord message, EntityPlayer player, MessageContext messageContext) {}
}
