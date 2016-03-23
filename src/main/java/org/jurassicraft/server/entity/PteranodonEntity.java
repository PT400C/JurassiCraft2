package org.jurassicraft.server.entity;

import net.minecraft.world.World;
import org.jurassicraft.client.animation.Animations;
import org.jurassicraft.server.entity.ai.animations.JCNonAutoAnimBase;
import org.jurassicraft.server.entity.ai.animations.JCNonAutoAnimSoundBase;
import org.jurassicraft.server.entity.base.AggressiveFlyingDinosaurEntity;

public class PteranodonEntity extends AggressiveFlyingDinosaurEntity // implements IEntityAIFlyingCreature, ICarnivore
{
    public PteranodonEntity(World world)
    {
        super(world);

        tasks.addTask(2, new JCNonAutoAnimBase(this, 25, Animations.LOOKING_RIGHT.get(), 100)); // Head twitch right
        tasks.addTask(2, new JCNonAutoAnimBase(this, 25, Animations.LOOKING_LEFT.get(), 100)); // Head twitch left
        tasks.addTask(2, new JCNonAutoAnimSoundBase(this, 34, Animations.CALLING.get(), 100, getSoundForAnimation(Animations.CALLING.get()), 2.5F)); // Call
    }

    @Override
    public int getTailBoxCount()
    {
        return 0;
    }
}
