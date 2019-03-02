package org.jurassicraft.server.block.entity;

import net.ilexiconn.llibrary.client.model.tabula.TabulaModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jurassicraft.client.model.FixedTabulaModel;
import org.jurassicraft.client.model.animation.EntityAnimation;
import org.jurassicraft.client.model.obj.IOBJTile;
import org.jurassicraft.server.block.SkullDisplay;
import org.jurassicraft.server.dinosaur.Dinosaur;
import org.jurassicraft.server.entity.DinosaurEntity;
import org.jurassicraft.server.entity.EntityHandler;

import com.mojang.authlib.GameProfile;

public class SkullDisplayEntity extends TileEntity implements IOBJTile {
	
	private static final Map<IDClass, Integer> compareList = new HashMap<IDClass, Integer>();
	
	static {
		if(FMLCommonHandler.instance().getSide() == Side.CLIENT) {
			compareList.put(new IDClass(false, (byte) 0), 0);
			compareList.put(new IDClass(true, (byte) 0), 1);
			compareList.put(new IDClass(false, (byte) 1), 2);
			compareList.put(new IDClass(true, (byte) 1), 3);
			compareList.put(new IDClass(false, (byte) 2), 4);
			compareList.put(new IDClass(true, (byte) 2), 5);
		}
	}
	
	private byte state = 0;
	private short angle = 0;
	private int dinosaur = -1;
    private boolean isFossilized;
    
    @SideOnly(Side.CLIENT)
    public int renderID;
    
	public ResourceLocation texture = null;
	public TabulaModel model = null;
	
	public void setModel(int dinosaurID, boolean isFossilized, boolean hasStand) {
        this.dinosaur = dinosaurID;
        this.isFossilized = isFossilized;
        this.state = (byte) (this.getWorld().getBlockState(this.getPos()).getValue(SkullDisplay.FACING).getAxis() == EnumFacing.Axis.Y ? hasStand ? 0 : 2 : 1);
        this.markDirty();
    }
	
	@SideOnly(Side.CLIENT)
    public short getAngle()
    {
        return this.angle;
    }
	
    public byte getState()
    {
		return (byte) this.state;
    }
	
	public Dinosaur getDinosaur() {
		return EntityHandler.getDinosaurById(this.dinosaur);
	}
	
	@SideOnly(Side.CLIENT)
	public boolean hasData() {
		return this.dinosaur != -1 ? true : false;
	}
	
	public boolean isFossilized() {
		return this.isFossilized;
	}
	
	public void setAngle(short angle)
    {
        this.angle = angle;
    }
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        compound = super.writeToNBT(compound);
        compound.setInteger("dinosaur", this.dinosaur);
        compound.setBoolean("isFossilized", this.isFossilized);
        compound.setShort("angle", this.angle);
        compound.setByte("state", this.state);
        return compound;
    }
	
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(this.pos, 0, this.getUpdateTag());
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return this.writeToNBT(new NBTTagCompound());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void onDataPacket(NetworkManager networkManager, SPacketUpdateTileEntity packet) {
		this.readFromNBT(packet.getNbtCompound());
	
	}
	
	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return super.getRenderBoundingBox().grow(2);
	}

	@Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.dinosaur = compound.getInteger("dinosaur");
        this.isFossilized = compound.getBoolean("isFossilized");
        this.angle = compound.getShort("angle");
        this.state = compound.getByte("state");
        if(FMLCommonHandler.instance().getSide() == Side.CLIENT)
        	this.renderID = compareList.get(new IDClass(this.isFossilized(), this.getState()));
    }

	@SideOnly(Side.CLIENT)
	@Override
	public int getVariantID() {
		return this.renderID;
	}
	
	public static class IDClass {
		
		public final boolean fossilized;
		public final byte state;
		
		public IDClass(boolean fossilized, byte state) {
			this.fossilized = fossilized;
			this.state = state;
		}
		
		@Override
		public boolean equals(Object obj) {
			return obj instanceof IDClass && ((IDClass) obj).fossilized == this.fossilized && ((IDClass) obj).state == this.state;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.state) * Objects.hash(this.fossilized);
		}
}

}
