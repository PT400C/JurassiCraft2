package net.timeless.jurassicraft.client.model.animation;

import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.timeless.animationapi.client.Animator;
import net.timeless.jurassicraft.client.model.ModelDinosaur;
import net.timeless.unilib.client.model.json.IModelAnimator;
import net.timeless.unilib.client.model.json.ModelJson;
import net.timeless.jurassicraft.common.entity.EntityLeaellynasaura;

@SideOnly(Side.CLIENT)
public class AnimationLeaellynasaura implements IModelAnimator
{
    @Override
    public void setRotationAngles(ModelJson modelJson, float f, float f1, float rotation, float rotationYaw, float rotationPitch, float partialTicks, Entity e)
    {
        ModelDinosaur model = (ModelDinosaur) modelJson;
		EntityLeaellynasaura entity = (EntityLeaellynasaura) e;
        Animator animator = model.animator;
    }
}
