package org.jurassicraft.client.sound;

import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jurassicraft.server.entity.vehicle.CarEntity;
import org.jurassicraft.server.entity.vehicle.HelicopterEntity;

@SideOnly(Side.CLIENT)
public class VehicleSound extends EntitySound<CarEntity> {
    public VehicleSound(CarEntity entity, SoundEvent vehicleMove) {
        super(entity, vehicleMove, SoundCategory.NEUTRAL);
    }

    @Override
    public boolean canRepeat() {
        return true;
    }

    @Override
    public float getVolume() {
    	
    	return ((CarEntity) this.entity).getSoundVolume();
    }

    @Override
    public float getPitch() {
        return Math.min(1.0F, this.getVolume()) * 0.5F + 0.7F;
    }
}
