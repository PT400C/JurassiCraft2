package org.jurassicraft.server.entity;

import org.jurassicraft.JurassiCraft;
import org.jurassicraft.server.item.Dart;
import org.jurassicraft.server.item.TrackingDart;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TranquilizerDartEntity extends EntityThrowable implements IEntityAdditionalSpawnData {

	private ItemStack stack;
	private EntityPlayer shooter;
	private String id;

	public TranquilizerDartEntity(World worldIn) {
		super(worldIn);
	}

	public TranquilizerDartEntity(World worldIn, EntityLivingBase throwerIn, ItemStack stack, String id) {
		super(worldIn, throwerIn);
		this.stack = stack.copy();
		if (throwerIn instanceof EntityPlayer)
			this.shooter = (EntityPlayer) throwerIn;
		this.id = id;
	}

	@Override
	public void onUpdate() {
		if (world.isRemote) {
			spawnParticles();
		}
		super.onUpdate();
	}

	@SideOnly(Side.CLIENT)
	private void spawnParticles() {
		Minecraft.getMinecraft().effectRenderer.spawnEffectParticle(EnumParticleTypes.CLOUD.getParticleID(),
				this.posX + this.motionX / 4.0D, this.posY + this.motionY / 4.0D, this.posZ + this.motionZ / 4.0D,
				-this.motionX / 20.0D, -this.motionY / 20.0D + 0.2D, -this.motionZ / 20.0D);
	}

	@Override
	protected void onImpact(RayTraceResult result) {
		if (stack == null)
			return;
		Item item = stack.getItem();
		if (item != null && item instanceof Dart) {
			if (result.entityHit instanceof DinosaurEntity) {

				((Dart) item).getConsumer().accept((DinosaurEntity) result.entityHit, stack);
				if (item instanceof TrackingDart && this.shooter != null) {
					((Dart) item).getData().accept((DinosaurEntity) result.entityHit, this.shooter, this.id);
				}
				
			} else if (result.entityHit instanceof EntityLivingBase && (((Dart) item).isUniversal())) {
				((Dart) item).getConsumer().accept((EntityLivingBase) result.entityHit, stack);
			}else if (result.entityHit instanceof EntityLivingBase && !(((Dart) item).isUniversal()) && this.shooter != null) {
				this.shooter.sendStatusMessage(new TextComponentString(TextFormatting.YELLOW + "The "+((Dart) item).getName()+" doesn't seem effective on this creature"), true);
			}
		} else {
			JurassiCraft.getLogger().error("Expected Dart Item, got {} ", item.getRegistryName());
		}
		if (!this.world.isRemote) {
			this.world.setEntityState(this, (byte) 3);
			this.setDead();
		}
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		if (stack != null)
			ByteBufUtils.writeItemStack(buffer, stack);
	}

	@Override
	public void readSpawnData(ByteBuf additionalData) {
		if (stack != null)
			stack = ByteBufUtils.readItemStack(additionalData);
	}

}