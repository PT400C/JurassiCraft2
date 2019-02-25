package org.jurassicraft.server.entity;

import org.jurassicraft.client.proxy.ClientProxy;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Deprecated 
public class DummyEntity extends Entity {
	
	private static final AxisAlignedBB entityAABB = new AxisAlignedBB(0, 0, 0, 0, 0, 0);
	private static final AxisAlignedBB renderAABB = Block.FULL_BLOCK_AABB.grow(4);
	private static Vec3d eyePos = Vec3d.ZERO;

	public DummyEntity(World worldIn) {
		super(worldIn);
		this.forceSpawn = true;
	}
	
	/**
     * Update dummy entities position to match the player's position
     * @deprecated We need a better way of solving this problem
     */
	public void update(float pT) {
		final EntityPlayer player = ClientProxy.MC.player;
		if (player == null)
			return;
		eyePos = player.getPositionEyes(pT).add(player.getLookVec().scale(4));
		this.setPosition(eyePos.x, eyePos.y, eyePos.z);
	}
	
	@Override
	public boolean isInRangeToRender3d(double x, double y, double z) {
		return true;
	}
	
	@Override
	protected void readEntityFromNBT(NBTTagCompound compound) {};

	@Override
	protected void writeEntityToNBT(NBTTagCompound compound) {};

	@Override
	public AxisAlignedBB getEntityBoundingBox() {
		return entityAABB;
	}
	
	@Override
	public AxisAlignedBB getCollisionBoundingBox() {
		return null;
	}
	
	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return renderAABB;
	}

	@Override
	protected void entityInit() {
		this.setEntityId(-70983);
	}

}
