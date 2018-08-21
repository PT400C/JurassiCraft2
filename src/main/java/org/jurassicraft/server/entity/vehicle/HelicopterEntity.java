package org.jurassicraft.server.entity.vehicle;

import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.model.BlockStateLoader;
import net.minecraftforge.client.model.b3d.B3DModel;
import net.minecraftforge.server.permission.context.ContextKeys;
import org.lwjgl.input.Keyboard;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jurassicraft.JurassiCraft;
import org.jurassicraft.client.proxy.ClientProxy;
import org.jurassicraft.server.entity.ai.util.InterpValue;
import org.jurassicraft.server.entity.vehicle.CarEntity.Speed;
import org.jurassicraft.server.item.ItemHandler;
import org.jurassicraft.server.message.UpdateVehicleControlMessage;
import org.jurassicraft.server.util.MutableVec3;

import javax.annotation.Nonnull;

public class HelicopterEntity extends CarEntity {

    private static final BlockPos INACTIVE = new BlockPos(-1, -1, -1);

    private static final byte UPWARD   = 0b010000;
    private static final byte DOWNWARD = 0b100000;
    
    private BlockPos prevPos = INACTIVE;

    private boolean lastDirBackwards;
    private final float MAX_POWER = 80.0F;
    private final float REQUIRED_POWER = MAX_POWER / 2.0F;
    private float enginePower;
    public float gearLift;
    public boolean shouldGearLift = true;
    private final InterpValue rotationYawInterp = new InterpValue(this, 4f);
    private final float SPEEDMODIFIER = 2.5f;
    public boolean isFlying;
    public float rotorRotationAmount;
    public final InterpValue interpRotationPitch = new InterpValue(this, 0.25D);
    public final InterpValue interpRotationRoll = new InterpValue(this, 0.25D);
    private MutableVec3 direction;
    public float rotationAmount;
    public float sideRotationAmount;
    private final float MAXMOVEMENTROTATION = 15f;
    private boolean shouldFallDamage;
    public double rotAmount = 0D;
    private Vec3d prevInAirPos;
    private float damageAmount;
    /* =================================== CAR START ===========================================*/

    public HelicopterEntity(World worldIn) {
        super(worldIn);
        double w = 5f; // width in blocks
        double h = 3.5f; // height in blocks
        double d = 8f; // depth in blocks
        this.setEntityBoundingBox(new AxisAlignedBB( 0, 0, 0, w, h, d));
        this.setSize(5f, 3.5f);
        this.speedModifier = 1.5f;
        this.isFlying = false;
        this.direction = new MutableVec3(0,1,0);
    }
    
    public boolean upward() {
        return this.getStateBit(UPWARD);
    }
    
    public boolean downward() {
        return this.getStateBit(DOWNWARD);
    }
    
    
    public void upward(boolean upward) {
        this.setStateBit(UPWARD, upward);
       
    }

    public void downward(boolean downward) {
        this.setStateBit(DOWNWARD, downward);
    }

    @Override
    public void dropItems() {
        this.dropItem(ItemHandler.HELICOPTER, 1);
    }

    @Override
    protected Seat[] createSeats() {
        Seat middle = new Seat(0F, -0.23F, 1.2F, 0.5F, 0.25F);
        Seat frontLeft = new Seat(-0.55F, -0.34F, 0.1F, 0.5F, 0.25F);
        Seat frontRight = new Seat(0.55F, -0.34F, 0.1F, 0.5F, 0.25F);
        Seat backLeft = new Seat( 0.4F, 0.25F, -1F, 0.5F, 0.25F);
        Seat backReft = new Seat( -0.4F, 0.25F, -1F, 0.5F, 0.25F);
        return new Seat[] { middle, frontLeft, frontRight, backLeft, backReft};
    }

    @Override
    protected boolean shouldStopUpdates() {
        return false;
    }

    @Override
    public void onUpdate() {
        BlockPos startPos = this.getPosition();
     //   this.setPosition(this.posX, this.posY, this.posZ); //Make sure that the car is in the right position. Can cause issues when changing size of car
       //Why is that needed?
        if(!startPos.equals(this.getPosition())) {
            prevPos = this.getPosition();
        }
        super.onUpdate();
    }

    @Override
    protected void doBlockCollisions() {
        super.doBlockCollisions();

    }

    
    @Override
    protected void handleControl() {
    	
    	this.upward(ClientProxy.getKeyHandler().HELICOPTER_UP.isKeyDown());
        this.downward(ClientProxy.getKeyHandler().HELICOPTER_DOWN.isKeyDown());
        super.handleControl();
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        for (Seat seat : this.seats) {
            if (passenger.equals(seat.getOccupant())) {
                passenger.noClip = false;
                break;
            }
        }
    }

    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();
        //this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, this.posX-0.65f, this.posY+2f, this.posZ+ -2.9, 0.0f, 0.0f, 0.0f, new int[0]);
        //this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, this.posX+0.65f, this.posY+2f, this.posZ+ -2.9, 0.0f, 0.0f, 0.0f, new int[0]);
        if(!world.isRemote) {
        for (Seat seat : this.seats) {
        	if(seat.getOccupant() != null)
            seat.getOccupant().fallDistance = 0;
        }
        }
        if (this.forward() && this.isFlying) {
            this.rotationAmount += 1f;
        } else if (this.backward() && this.isFlying) {
            this.rotationAmount -= 1f;
        }else{
            if(this.rotationAmount < 0f){
                this.rotationAmount += 1f;
            }else if(this.rotationAmount >0f){
                this.rotationAmount -= 1f;
            }
        }
        if (this.left() && this.isFlying) {
            this.sideRotationAmount += 1f;
        } else if (this.right() && this.isFlying) {
            this.sideRotationAmount -= 1f;
        }else{
            if(this.sideRotationAmount < 0f){
                this.sideRotationAmount += 1f;
            }else if(this.sideRotationAmount > 0f){
                this.sideRotationAmount -= 1f;
            }
        }
        if(this.rotationAmount >= MAXMOVEMENTROTATION){
            this.rotationAmount = MAXMOVEMENTROTATION;
        }
        if(this.rotationAmount <= -MAXMOVEMENTROTATION){
            this.rotationAmount = -MAXMOVEMENTROTATION;
        }
        if(this.sideRotationAmount >= MAXMOVEMENTROTATION){
            this.sideRotationAmount = MAXMOVEMENTROTATION;
        }
        if(this.sideRotationAmount <= -MAXMOVEMENTROTATION){
            this.sideRotationAmount = -MAXMOVEMENTROTATION;
        }

        rotationYawInterp.reset(this.rotationYaw - 180D);
        if (forward()) {
            lastDirBackwards = false;
        } else if (backward()) {
            lastDirBackwards = true;
        }
        this.interpRotationPitch.setTarget(this.direction.zCoord * -30D);
        this.interpRotationRoll.setTarget(this.direction.xCoord * 20D);
        if (this.seats[0].getOccupant() != null) {
            if (this.upward()) {
                this.motionY += 0.2f;
                if (this.motionY >= 4f) {
                    this.motionY = 4f;
                }

                this.isFlying = true;
                this.shouldFallDamage = true;
                this.rotorRotationAmount += 0.1f;
                this.prevInAirPos = this.getPositionVector();
                this.setNoGravity(true);
                for(Seat seat : this.seats){
                    if(seat.getOccupant() != null) {
                        seat.getOccupant().setNoGravity(false);
                    }
                }

            } else if (this.downward()) {
                this.motionY -= 0.3f;
                if (this.motionY <= -4f) {
                    this.motionY = -4f;
                }
                this.shouldFallDamage = false;


            } else {
                if(!this.isFlying){
                    this.setNoGravity(false);
                    for(Seat seat : this.seats){
                        if(seat.getOccupant() != null) {
                            seat.getOccupant().setNoGravity(false);
                        }
                    }

                }else{
                    this.rotorRotationAmount += 5f;
                }
            }
        }

        if(!this.isFlying){
            this.speedModifier = -0.75f;
        }else{
            this.speedModifier = 1.5f;


        }
        if(this.onGround == true) {
            this.isFlying = false;
            this.rotorRotationAmount -= 0.2f;
        }else{
            this.rotorRotationAmount += 5f;
        }
        if(!this.shouldGearLift) {
            this.gearLift += 0.02f;
        }else{
            this.gearLift -= 0.02f;
        }
        if(world.getBlockState(new BlockPos.MutableBlockPos((int)Math.floor( this.posX), (int)Math.floor( this.posY - 10f), (int)Math.floor( this.posZ))).getBlock() != Blocks.AIR){
            this.shouldGearLift = false;
        }else{
            this.shouldGearLift = true;
        }
        if(this.seats[0].getOccupant() == null){
            this.setNoGravity(false);
        }
        if(this.onGround && this.shouldFallDamage){
            this.damageAmount = (float)this.prevInAirPos.y - (float)this.getPositionVector().y;
            this.setHealth(this.getHealth() - (float)Math.floor((double)(this.damageAmount / 3)));
            this.shouldFallDamage = false;
        }
        if(this.rotorRotationAmount < 0f){
            this.rotorRotationAmount = 0f;
        }
        if(this.rotorRotationAmount > 1.5f){
            this.rotorRotationAmount = 1.5f;
        }
        if(this.gearLift < -0.5f){
            this.gearLift = -0.5f;
        }
        if(this.gearLift > 0){
            this.gearLift = 0f;
        }
        this.rotAmount += this.rotorRotationAmount / 2d;
       
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setLong("PrevBlockPosition", this.prevPos.toLong());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.prevPos = BlockPos.fromLong(compound.getLong("PrevBlockPosition"));
    }

    @Override
    public float getSoundVolume() {
        return this.getControllingPassenger() != null ? this.getSpeed().modifier / 2f : 0f;
    }



    @Nonnull
    @Override
    public EnumFacing getAdjustedHorizontalFacing() {
        return super.getAdjustedHorizontalFacing();
    }

    @Override
    protected WheelData createWheels() {
        return new WheelData(1, 2, -1, -2.2);
    }

    @Override
    protected boolean shouldTyresRender() {
        return false;
    }
    @Override
    public void updatePassenger(Entity passenger) {
        if (this.isPassenger(passenger)) {
            Seat seat = null;
            for (Seat s : this.seats) {
                if (passenger.equals(s.getOccupant())) {
                    seat = s;
                    break;
                }
            }
            Vec3d pos;
            if (seat == null) {
                pos = new Vec3d(this.posX, this.posY + this.height, this.posZ);
            } else {
                pos = seat.getPos();
            }
            passenger.setPosition(pos.x, pos.y + this.interpRotationPitch.getCurrent() / 75D, pos.z);
            passenger.rotationYaw += this.rotationDelta;
            passenger.setRotationYawHead(passenger.getRotationYawHead() + this.rotationDelta);
            if (passenger instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) passenger;
                living.renderYawOffset += (living.rotationYaw - living.renderYawOffset) * 0.6F;
            }
        }
    }


    @Override
    public float getCollisionBorderSize() {
        return 2.25f;
    }
}
