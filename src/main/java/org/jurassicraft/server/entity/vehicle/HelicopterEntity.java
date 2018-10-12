package org.jurassicraft.server.entity.vehicle;

import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.server.permission.context.ContextKeys;
import org.lwjgl.input.Keyboard;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jurassicraft.JurassiCraft;
import org.jurassicraft.client.proxy.ClientProxy;
import org.jurassicraft.client.sound.EntitySound;
import org.jurassicraft.server.entity.ai.util.InterpValue;
import org.jurassicraft.server.entity.vehicle.CarEntity.Speed;
import org.jurassicraft.server.item.ItemHandler;
import org.jurassicraft.server.message.CarEntityPlayRecord;
import org.jurassicraft.server.message.UpdateVehicleControlMessage;
import org.jurassicraft.server.util.MutableVec3;

import javax.annotation.Nonnull;

public class HelicopterEntity extends CarEntity {

    private static final byte UPWARD   = 0b010000;
    private static final byte DOWNWARD = 0b100000;
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
    private MutableBlockPos mb = new MutableBlockPos();
    /* =================================== CAR START ===========================================*/

    public HelicopterEntity(World worldIn) {
        super(worldIn);
        double w = 5f; // width in blocks
        double h = 3.5f; // height in blocks
        double d = 8f; // depth in blocks
        this.setEntityBoundingBox(new AxisAlignedBB(0, 0, 0, w, h, d));
        this.setSize(5f, 3.5f);
        this.speedModifier = 1.5f;
        this.isFlying = false;
        this.direction = new MutableVec3(0, 1, 0);
    }
    
    public boolean upward() {
        return this.getStateBit(UPWARD);
    }
    
    public boolean downward() {
        return this.getStateBit(DOWNWARD);
    }
    
    @Override
    public void startSound() {
        ClientProxy.playHelicopterSound(this);
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
        Seat backReft = new Seat(-0.4F, 0.25F, -1F, 0.5F, 0.25F);
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
      
        
        //  if(!startPos.equals(this.getPosition())) {
       //     prevPos = this.getPosition();
     //   }
        super.onUpdate();
    }

    @Override
    protected void doBlockCollisions() {
        super.doBlockCollisions();

    }

    private boolean isController(EntityPlayer e) {
    	if((this.getSeatController() != null && this.getSeatController().getUniqueID() == e.getUniqueID()) && (this.getControllingPassenger() != null && this.getControllingPassenger().getUniqueID() == e.getUniqueID())) {
    		return true;
    	}
    	return false;
    }
    
    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if(!world.isRemote) {
            if (!player.isSneaking() && !(player.getRidingEntity() == this)) {
                player.startRiding(this);
            }
        }
        return true;
    }
    
	@SideOnly(Side.CLIENT)
	@Override
	protected void handleControl() {

		if (isController(Minecraft.getMinecraft().player)) {
			if (this.isInWater()) {
				this.upward(false);
				this.downward(false);
			} else {
				this.upward(ClientProxy.getKeyHandler().HELICOPTER_UP.isKeyDown());
				this.downward(ClientProxy.getKeyHandler().HELICOPTER_DOWN.isKeyDown());
			}
			super.handleControl();
		}
	}
    
    @Override
    public void fall(float distance, float damageMultiplier) {
    	
    	if(!world.isRemote && !isFlying) {
    		float damage = MathHelper.ceil((distance - 3F) * damageMultiplier);
    		if (damage > 0){
    			
    			this.setHealth(this.getHealth() - (float)(damage * 1.25F));
    			
    			if (this.getHealth() < 0) {
    				this.setDead();
    				if (this.world.getGameRules().getBoolean("doEntityDrops")) {
    					this.dropItems();
    				}
    			}
    		}
        }

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
    	  if(world.isRemote) {
          	this.isFlying = this.hasNoGravity();
          }
    	   super.onEntityUpdate();
      
        if(!this.isInWater()){
        	
        //this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, this.posX-0.65f, this.posY+2f, this.posZ+ -2.9, 0.0f, 0.0f, 0.0f, new int[0]);
        //this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, this.posX+0.65f, this.posY+2f, this.posZ+ -2.9, 0.0f, 0.0f, 0.0f, new int[0]);
        if(!world.isRemote) {
        for (Seat seat : this.seats) {
        	if(seat.getOccupant() != null)
            seat.getOccupant().fallDistance = 0;
        }
        }
        if (forward() && this.isFlying) {
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
        if ((this.getSeatController() != null || this.getControllingPassenger() != null)) {
            if (this.upward()) {
            	this.motionY += 0.005f;
                if(this.motionY > 0)
                	this.motionY *= 1.35f;
                if (this.motionY >= 1.5f) {
                    this.motionY = 1.5f;
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

            } else if (this.downward() && this.isFlying) {
            	this.motionY -= 0.02f;
            	
            	if(this.motionY < 0)
            		this.motionY *= 1.3f;
                if (this.motionY <= -1.3f) {
                    this.motionY = -1.3f;
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
                    this.rotorRotationAmount += 0.01f;
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
            this.rotorRotationAmount -= 0.05f;
        }else{
            this.rotorRotationAmount += 0.0001f;
        }
        if(world.isRemote) {
        if(!this.shouldGearLift) {
            this.gearLift += 0.02f;
        }else{
            this.gearLift -= 0.02f;
        }
        
        boolean found = false;
        float dist = -1;
        mb.setPos(this.getPosition());
        while(!found) {
            if(this.posY < 0) {
                break;
            }
            if(world.isAirBlock(mb)) {
                mb = mb.setPos(mb.getX(), mb.getY() - 1, mb.getZ());
            } else {
                found = true;
                dist = (float)(this.posY - mb.getY() - 1);
            }
        }
       // if(this.getPosition().getY() - 10 < world.getChunkFromBlockCoords(this.getPosition()).getPrecipitationHeight(this.getPosition()).getY()){
        if(dist < 10){
        	this.shouldGearLift = false;
        }else{
            this.shouldGearLift = true;
        }
        }
        if(this.getSeatController() == null && this.getControllingPassenger() == null){
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
        if(world.isRemote) {
        if(this.gearLift < -0.5f){
            this.gearLift = -0.5f;
        }
        if(this.gearLift > 0){
            this.gearLift = 0f;
        }
        }
        this.rotAmount += this.rotorRotationAmount / 2d;
    }else {
    	this.setNoGravity(false);
    	this.wheelRotateAmount = 0;
    	this.rotorRotationAmount = 0;
    }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        //compound.setLong("PrevBlockPosition", this.prevPos.toLong());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        //this.prevPos = BlockPos.fromLong(compound.getLong("PrevBlockPosition"));
    }

    @Override
    public float getSoundVolume() {
        return this.rotorRotationAmount > 0 ? (Math.abs(this.rotorRotationAmount) + 0.001F) / (this.sound == null || this.sound.isDonePlaying() ? 2f : 4f) : (Math.abs(this.wheelRotateAmount) + 0.001F) / (this.sound == null || this.sound.isDonePlaying() ? 2f : 4f);
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