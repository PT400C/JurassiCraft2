package org.jurassicraft.client.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.world.World;

public class DummyCameraEntity extends EntityPlayerSP {

	public static DummyCameraEntity instance;

	public DummyCameraEntity(Minecraft mc, World world) {
		super(mc, world, Minecraft.getMinecraft().player.connection,
				Minecraft.getMinecraft().player.getStatFileWriter(), Minecraft.getMinecraft().player.getRecipeBook());
		this.movementInput = Minecraft.getMinecraft().player.movementInput;
		if (instance == null) {
			instance = this;
		}
	}

	public DummyCameraEntity(World world) {
		this(Minecraft.getMinecraft(), world);
	}

	@Override
	public float getFovModifier() {
		return super.getFovModifier() * 2f;
	}

	public static void onUnloadWorld() {
		if (instance != null) {
			instance.setDead();
			instance = null;
		}
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		System.out.println("POS: " + this.posX + ";" + this.posY + ";" + this.posZ);
	}
}
