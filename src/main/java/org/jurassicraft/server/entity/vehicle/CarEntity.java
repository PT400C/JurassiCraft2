package org.jurassicraft.server.entity.vehicle;

import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumHand;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jurassicraft.JurassiCraft;
import org.jurassicraft.client.proxy.ClientProxy;
import org.jurassicraft.client.render.entity.TyretrackRenderer;
import org.jurassicraft.client.sound.EntitySound;
import org.jurassicraft.server.damage.DamageSources;
import org.jurassicraft.server.entity.ai.util.InterpValue;
import org.jurassicraft.server.entity.vehicle.CarEntity.Seat;
import org.jurassicraft.server.entity.vehicle.util.CarWheel;
import org.jurassicraft.server.entity.vehicle.util.WheelParticleData;
import org.jurassicraft.server.message.CarEntityPlayRecord;
import org.jurassicraft.server.message.UpdateVehicleControlMessage;
import org.lwjgl.input.Keyboard;
import org.omg.CORBA.DoubleHolder;

import javax.annotation.Nullable;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector4d;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Predicate;

public abstract class CarEntity extends Entity implements MultiSeatedEntity {
    public static final DataParameter<Byte> WATCHER_STATE = EntityDataManager.createKey(CarEntity.class, DataSerializers.BYTE);
    public static final DataParameter<Float> WATCHER_HEALTH = EntityDataManager.createKey(CarEntity.class, DataSerializers.FLOAT);
    public static final DataParameter<Integer> WATCHER_SPEED = EntityDataManager.createKey(CarEntity.class, DataSerializers.VARINT);
    public static final DataParameter<ItemStack> RECORD_ITEM = EntityDataManager.createKey(CarEntity.class, DataSerializers.ITEM_STACK);

    public static final float MAX_HEALTH = 40;
    private static final byte LEFT     = 0b000001;
    private static final byte RIGHT    = 0b000010;
    private static final byte FORWARD  = 0b000100;
    private static final byte BACKWARD = 0b001000;

    //TODO Recode that system
    protected final Seat[] seats = createSeats();
    protected final WheelData wheeldata = createWheels();
    
    public float wheelRotation;
    public float wheelRotateAmount;
    public float prevWheelRotateAmount;

    protected float rotationDelta;

    public int interpProgress;
    double interpTargetX;
    public double interpTargetY;
    public double interpTargetZ;
    public double interpTargetYaw;
    public float speedModifier = 0f;
    private static final double INTERP_AMOUNT = 0.15D; //TODO config ?
    public boolean wasOnGroundLastTick;
    private Vec3d prevUnairbornPos;

    
    public final InterpValue backValue = new InterpValue(this, INTERP_AMOUNT);
    public final InterpValue frontValue = new InterpValue(this, INTERP_AMOUNT);
    public final InterpValue leftValue = new InterpValue(this, INTERP_AMOUNT);
    public final InterpValue rightValue = new InterpValue(this, INTERP_AMOUNT);

    public final CarWheel backLeftWheel = new CarWheel(0, wheeldata.bl); 
    public final CarWheel backRightWheel = new CarWheel(1, wheeldata.br);
    public final CarWheel frontLeftWheel = new CarWheel(2, wheeldata.fl);
    public final CarWheel frontRightWheel = new CarWheel(3, wheeldata.fr);

    @SuppressWarnings("unchecked")
    public final List<WheelParticleData>[] wheelDataList = new List[4];
    
    public List<CarWheel> allWheels = Lists.newArrayList(backLeftWheel, frontLeftWheel, backRightWheel, frontRightWheel);

    @SideOnly(Side.CLIENT)
    public EntitySound<CarEntity> sound;
    @SideOnly(Side.CLIENT)
    public InterpValue steerAmount;

    private float healAmount;
    private int healCooldown = 40;
    
    private Vec3d previousPosition = null; //Used for speed calculations
    private long prevWorldTime = -1;//Also used for speed calculations
    
    public double estimatedSpeed = 0D;
    
	private byte prevState = 0;
    
    public CarEntity(World world) {
        super(world);
        this.setSize(3.0F, 2.5F);
        this.stepHeight = 1.5F;
        if (world.isRemote) {
            this.startSound();
            this.steerAmount = new InterpValue(this,0.1D);
        }
        for(int i = 0; i < 4; i++) {
            this.wheelDataList[i] = Lists.newArrayList();
        }
        
        backLeftWheel.setPair(backRightWheel);
        frontLeftWheel.setPair(frontRightWheel);

    }

    @Override
    protected void entityInit() {
        this.dataManager.register(WATCHER_STATE, (byte) 0);
        this.dataManager.register(WATCHER_HEALTH, MAX_HEALTH);
        this.dataManager.register(WATCHER_SPEED, 1);
        this.dataManager.register(RECORD_ITEM, ItemStack.EMPTY);
    }

    public boolean left() {
        return this.getStateBit(LEFT);
    }

    public boolean right() {
        return this.getStateBit(RIGHT);
    }

    public boolean forward() {
        return this.getStateBit(FORWARD);
    }

    public boolean backward() {
        return this.getStateBit(BACKWARD);
    }
    
    public void left(boolean left) {
        this.setStateBit(LEFT, left);
    }

    public void right(boolean right) {
        this.setStateBit(RIGHT, right);
    }

    public void forward(boolean forward) {
        this.setStateBit(FORWARD, forward);
    }

    public void backward(boolean backward) {
        this.setStateBit(BACKWARD, backward);
    }
    
    protected boolean getStateBit(byte mask) {
        return (this.getControlState() & mask) != 0;
    }

    protected void setStateBit(byte mask, boolean newState) {
    	byte state = this.getControlState();
        this.setControlState((byte) (newState ? state | mask : state & ~mask));
    }

    public byte getControlState() {

        return this.dataManager.get(WATCHER_STATE);
        
    }
    
    public byte getPreviousState() {

        return this.prevState;
        
    }

    public void setControlState(byte state) {
    	
        this.dataManager.set(WATCHER_STATE, state);
    }
    
    public void setPreviousState(byte state) {
        this.prevState = state;
    }
    
    public void setSpeed(Speed speed) {
	this.dataManager.set(WATCHER_SPEED, speed.ordinal());
    }
    
    public Speed getSpeed() {
	    return Speed.values()[this.dataManager.get(WATCHER_SPEED)];
    }

    public void setHealth(float health) {
        this.dataManager.set(WATCHER_HEALTH, health);
    }

    public float getHealth() {
        return this.dataManager.get(WATCHER_HEALTH);
    }

    public ItemStack getItem() {
        return this.dataManager.get(RECORD_ITEM);
    }

    @Override
    public boolean isInRangeToRenderDist(double dist) {
        return true;
    }

    @Override
    protected boolean canFitPassenger(Entity passenger) {
        return this.getPassengers().size() < this.seats.length;
    }

    @Override
    public Entity getControllingPassenger() {
        return this.getPassengers().isEmpty() ? null : this.getPassengers().get(0);
    }
    
    public Entity getSeatController() {
        return this.seats[0].getOccupant();
    }
    
    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }
    
    public Vector4d getCarDimensions() {
	return this.wheeldata.carVector;
    }
    
    public Vector2d getBackWheelRotationPoint() {
	return new Vector2d(-0.5, 1.4);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int duration, boolean teleport) {
        this.interpTargetX = x;
        this.interpTargetY = y;
        this.interpTargetZ = z;
        this.interpTargetYaw = yaw;
        this.interpProgress = duration;
    }

    private void resetFlyTicks(EntityPlayerMP entity) {
    	entity.fallDistance = 0F;
		 Field field = null;
		 try {
			 if((Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")){
			 field = NetHandlerPlayServer.class.getDeclaredField("vehicleFloatingTickCount");
		 }else{
			 field = NetHandlerPlayServer.class.getDeclaredField("field_184346_E");
		 }

            field.setAccessible(true);
            field.set(entity.connection, -1);
		 }catch(NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			 e.printStackTrace();
		 }
    }
    
	@Override
	public void fall(float distance, float damageMultiplier) {
		
		if (!world.isRemote) {
			float damage = MathHelper.ceil((distance - 3F) * damageMultiplier);
			
			if (damage > 0) {
				this.setHealth(this.getHealth() - (float) (damage * 1.25F));
				
				if (this.getHealth() < 0) {
					this.setDead();
					
					if (this.world.getGameRules().getBoolean("doEntityDrops")) {
						
						this.dropItems();
						ItemStack recordItem = this.dataManager.get(RECORD_ITEM);
						if (!recordItem.isEmpty()) {
							this.entityDropItem(recordItem, 0);
						}
					}
				}
			}
		}
	}
    
    @Override
    public void onEntityUpdate() {
    	if (!world.isRemote) {
    	 if (this.getHealth() < 0 && !this.isDead) {
             this.setDead();
             if (this.world.getGameRules().getBoolean("doEntityDrops")) {
                 this.dropItems();
                 ItemStack recordItem = this.dataManager.get(RECORD_ITEM);
                 if(!recordItem.isEmpty()) {
                     this.entityDropItem(recordItem, 0);
                 }
             }
         }
    	
        	 for(Seat seat : this.seats){
        		 if(seat.getOccupant() != null && seat.getOccupant() instanceof EntityPlayerMP) {
        			 resetFlyTicks((EntityPlayerMP) seat.getOccupant());
        		 }
        	 }
			
        }
        if(this.getSpeed() == Speed.FAST) {
            this.allWheels.forEach(wheel -> this.createWheelParticles(wheel, true));
        }
        this.allWheels.forEach(this::createWheelParticles);

        if(shouldStopUpdates()) {
            super.onEntityUpdate();
            return;
        }
        if(!world.isRemote) {
            if(prevWorldTime != -1) {
                world.getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().grow(0.1f), this::canRunoverEntity).forEach(this::runOverEntity);
            }
        }
        if(this.isInWater()){
            for (Seat seat : seats) {
                if(seat.getOccupant() != null) {
                    seat.getOccupant().attackEntityFrom(DamageSource.DROWN, 0.5f);
                }
            }
            this.setHealth(this.getHealth() - 0.25F);
        }
        if(previousPosition == null) {
            previousPosition = this.getPositionVector();
        }
        estimatedSpeed = this.getPositionVector().distanceTo(previousPosition) / (world.getTotalWorldTime() - prevWorldTime);
        previousPosition = this.getPositionVector();
        prevWorldTime = world.getTotalWorldTime();
        
        for(int i = 0; i < 4; i++) {
            List<WheelParticleData> markedRemoved = Lists.newArrayList();
            wheelDataList[i].forEach(wheel -> wheel.onUpdate(markedRemoved));
            markedRemoved.forEach(wheelDataList[i]::remove);
        }

        super.onEntityUpdate();

        this.allWheels.forEach(this::processWheel);
        
        Vector4d vec = wheeldata.carVector;
        this.backValue.setTarget(this.calculateWheelHeight(vec.y, false));
        this.frontValue.setTarget(this.calculateWheelHeight(vec.w, false));
        this.leftValue.setTarget(this.calculateWheelHeight(vec.z, true));
        this.rightValue.setTarget(this.calculateWheelHeight(vec.x, true));

        if (!this.world.isRemote) {
            if (this.healCooldown > 0) {
                this.healCooldown--;
            } else if (this.healAmount > 0) {
                this.setHealth(this.getHealth() + 1);
                this.healAmount--;
                if (this.getHealth() > MAX_HEALTH) {
                    this.setHealth(MAX_HEALTH);
                    this.healAmount = 0;
                }
            }
        }

        this.tickInterp();
        this.updateMotion();
        if (this.getPassengers().isEmpty() || !(this.getPassengers().get(0) instanceof EntityPlayer)) {
            this.setControlState((byte) 0);
        }
        if (this.world.isRemote) {
            this.handleControl();
        }
        this.applyMovement();
        this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
    }
    
    protected boolean canRunoverEntity(Entity entity) {
	    return EntitySelectors.NOT_SPECTATING.apply(entity) && !this.getPassengers().contains(entity);
    }
    
    protected void runOverEntity(Entity entity) {
        if(this.estimatedSpeed > 0.1D) {
            entity.attackEntityFrom(DamageSources.CAR, (float) (this.estimatedSpeed));
        }
    }
    
    protected void createWheelParticles(CarWheel wheel) {
    	this.createWheelParticles(wheel, false);
    }
    
    protected void createWheelParticles(CarWheel wheel, boolean runBetween) {
        Vec3d pos;
        Vec3d opposite;
        if(runBetween) {
            Vec3d vec = wheel.getCurrentWheelPos();
            Vec3d oldVec = wheel.getCurrentWheelPos();

            Vec3d vec1 = wheel.getOppositeWheel().getCurrentWheelPos();
            Vec3d oldVec1 = wheel.getOppositeWheel().getCurrentWheelPos();

            pos =  new Vec3d((vec.x + oldVec.x) / 2D, (vec.y + oldVec.y) / 2D, (vec.z + oldVec.z) / 2D);
            opposite = new Vec3d((vec1.x + oldVec1.x) / 2D, (vec1.y + oldVec1.y) / 2D, (vec1.z + oldVec1.z) / 2D);
        } else {
            pos = wheel.getCurrentWheelPos();
            opposite = wheel.getOppositeWheel().getCurrentWheelPos();
        }
        if(wheel.getCurrentWheelPos().distanceTo(wheel.getPrevCurrentWheelPos()) >= 0.05D) {
            this.wheelDataList[wheel.getID()].add(new WheelParticleData(pos, opposite, world.getTotalWorldTime()).setShouldRender(shouldTyresRender()));
        }
    }
    
    protected void processWheel(CarWheel wheel) {
	    float localYaw = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw);
	    Vector2d relPos = wheel.getRelativeWheelPosition();
	    double xRot = Math.sin(Math.toRadians(localYaw)) * relPos.y - Math.cos(Math.toRadians(localYaw)) * relPos.x;
        double zRot = - Math.cos(Math.toRadians(localYaw)) * relPos.y - Math.sin(Math.toRadians(localYaw)) * relPos.x;
        Vec3d vec = new Vec3d(posX + xRot, this.posY, posZ + zRot);
        wheel.setCurrentWheelPos(vec);

    }
    
	protected boolean shouldTyresRender() {
		return this.getSpeed() != Speed.SLOW;
	}

    @Override
    public void onUpdate() {
        super.onUpdate();
        if(shouldStopUpdates()) {
            return;
        }
        AxisAlignedBB aabb = this.getEntityBoundingBox();
        for(BlockPos pos : BlockPos.getAllInBoxMutable(new BlockPos(Math.floor(aabb.minX), Math.floor(aabb.minY), Math.floor(aabb.minZ)), new BlockPos(Math.ceil(aabb.maxX), Math.ceil(aabb.maxY), Math.ceil(aabb.maxZ)))) {
            IBlockState state = world.getBlockState(pos);
            if(state.getMaterial() == Material.VINE) {
                if(world.isRemote) {
                    world.playEvent(2001, pos, Block.getStateId(state));
                } else {
                    state.getBlock().dropBlockAsItem(world, pos, state, 0);
                }
                world.setBlockToAir(pos);
            }else if(state.getMaterial() == Material.LEAVES){
                world.setBlockToAir(pos);
            }

        }
        this.prevWheelRotateAmount = this.wheelRotateAmount;
        double deltaX = this.posX - this.prevPosX;
        double deltaZ = this.posZ - this.prevPosZ;
        float delta = Math.min(MathHelper.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 4, 1);
        this.wheelRotateAmount += (delta - this.wheelRotateAmount) * 0.4F;
        this.wheelRotation += this.wheelRotateAmount;
        while (this.rotationYaw - this.prevRotationYaw < -180) {
            this.prevRotationYaw -= 360;
        }
        this.doBlockCollisions();
    }

	private void updateMotion() {
		final double resist = 0.8F;
		this.motionX *= resist;
		this.motionY *= resist;
		this.motionZ *= resist;
		this.rotationDelta *= resist;
		if (!this.hasNoGravity())
			this.motionY -= 0.15F;

	}

	protected void handleControl() {
		
		Entity driver = this.getSeatController();
		if (driver != null)
			driver = this.getControllingPassenger();
		if (!(driver instanceof EntityPlayer) || !((EntityPlayer) driver).isUser()) {
			return;
		}
		EntityPlayerSP player = (EntityPlayerSP) driver;
		MovementInput movementInput = player.movementInput;
		if (this.isInWater()) {
			this.setControlState((byte) 0);
		} else {

			this.left(movementInput.leftKeyDown);
			this.right(movementInput.rightKeyDown);
			this.forward(movementInput.forwardKeyDown);
			this.backward(movementInput.backKeyDown);
		}
		if (this.getControlState() != this.getPreviousState()) {
			JurassiCraft.NETWORK_WRAPPER.sendToServer(new UpdateVehicleControlMessage(this));
		}
		this.setPreviousState(this.getControlState());

	}

	protected void applyMovement() {
	    Speed speed = this.getSpeed();
	    float moveAmount = 0.0f;
        if ((this.left() || this.right()) && !(this.forward() || this.backward())) {
            moveAmount += 0.05F;
        }
        if (this.forward()) {
            moveAmount += 0.1F;
        } else if (this.backward()) {
            moveAmount -= 0.05F;
        }
        moveAmount *= (speed.modifier + this.speedModifier);
        if(this.isInWater()) {
            moveAmount -= 0.1f;
            if(moveAmount < 0f)
                moveAmount = 0f;
        }
        if (this.left()) {
            this.rotationDelta -= 20.0F * moveAmount;
        } else if (this.right()) {
            this.rotationDelta += 20.0F * moveAmount;
        }
        
        this.rotationDelta = MathHelper.clamp(this.rotationDelta, -30 * 0.1F, 30 * 0.1F);
        this.rotationYaw += this.rotationDelta;
        this.motionX += MathHelper.sin(-this.rotationYaw * 0.017453292F) * moveAmount;
        this.motionZ += MathHelper.cos(this.rotationYaw * 0.017453292F) * moveAmount;
    }

    private void tickInterp() {
        if (this.interpProgress > 0 && !this.canPassengerSteer()) {
            double interpolatedX = this.posX + (this.interpTargetX - this.posX) / (double) this.interpProgress;
            double interpolatedY = this.posY + (this.interpTargetY - this.posY) / (double) this.interpProgress;
            double interpolatedZ = this.posZ + (this.interpTargetZ - this.posZ) / (double) this.interpProgress;
            double deltaYaw = MathHelper.wrapDegrees(this.interpTargetYaw - (double) this.rotationYaw);
            this.rotationYaw = (float) ((double) this.rotationYaw + deltaYaw / (double) this.interpProgress);
            this.interpProgress--;
            this.setPosition(interpolatedX, interpolatedY, interpolatedZ);
            this.setRotation(this.rotationYaw, this.rotationPitch);
        }
    }
    
    protected final double calculateWheelHeight(double distance, boolean rotate90) {
        float localYaw = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw);
        double ret = Integer.MIN_VALUE;

        Vector4d carVec = wheeldata.carVector;
        double sideLength = Math.abs(rotate90 ? carVec.x - carVec.z : carVec.z - carVec.w);
        for(double d = -sideLength ; d <= sideLength; d += 0.25D/*TODO: config this ?*/) {
            double xRot = Math.sin(Math.toRadians(localYaw)) * (rotate90 ? d : distance) - Math.cos(Math.toRadians(localYaw)) * (rotate90 ? distance : d); 
            double zRot = - Math.cos(Math.toRadians(localYaw)) * (rotate90 ? d : distance) - Math.sin(Math.toRadians(localYaw)) * (rotate90 ? distance : d);
            Vec3d vec = new Vec3d(posX + xRot, this.posY, posZ + zRot);
            BlockPos pos = new BlockPos(vec);
            
            //world.spawnParticle(EnumParticleTypes.CRIT, vec.x, vec.y + 5, vec.z, 0, 0, 0);
            
            boolean found = false;
            List<AxisAlignedBB> aabbList = Lists.newArrayList();
            while(!found) {
                if(pos.getY() < 0) {
                    break;
                }
                aabbList.clear();
                world.getBlockState(pos).addCollisionBoxToList(world, pos, new AxisAlignedBB(pos), aabbList, this, false);
                if(world.isAirBlock(pos) || aabbList.isEmpty()) {
                    pos = pos.down();
                } else {
                    found = true;
                }
            }
            if(!found) {
                ret = posY;
            }
            if(found && !world.isAirBlock(pos.up()) && !world.isAirBlock(pos.up(2))) {
                List<AxisAlignedBB> list = Lists.newArrayList();
                world.getBlockState(pos.up()).addCollisionBoxToList(world, pos.up(), new AxisAlignedBB(pos.up()), list, this, false);
                world.getBlockState(pos.up(2)).addCollisionBoxToList(world, pos.up(2), new AxisAlignedBB(pos.up(2)), list, this, false);
                if(!list.isEmpty()) {
                    ret = posY;
                }
            }
            if(aabbList.isEmpty()) {
                ret = pos.getY() + 1;
            }
            DoubleHolder holder = new DoubleHolder(Integer.MIN_VALUE);
            aabbList.forEach(aabb -> holder.value = Math.max(aabb.maxY, holder.value));
            if(holder.value > ret) {
                ret = holder.value;
            }
        }
        return ret;
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if(!world.isRemote) {
            if(player.getRidingEntity() == this) {
                ItemStack currentStack = this.dataManager.get(RECORD_ITEM);
                ItemStack stack = player.getHeldItem(hand);
                if(stack.getItem() instanceof ItemRecord || stack.isEmpty()) {
                    this.dataManager.set(RECORD_ITEM, stack);
                    player.setHeldItem(hand, currentStack);
                    if(!stack.isEmpty()) {
                        JurassiCraft.NETWORK_WRAPPER.sendToAll(new CarEntityPlayRecord(this, (ItemRecord)stack.getItem()));
                    }
                }
            } else if (!player.isSneaking()) {
                player.startRiding(this);
            }
        }
        return true;
    }
    
	@Override
	protected void addPassenger(Entity passenger) {
		super.addPassenger(passenger);
		if (passenger instanceof EntityPlayerMP) {
			resetFlyTicks((EntityPlayerMP) passenger);
		}
		this.usherPassenger(passenger, 0);
	}
    
    private void usherPassenger(Entity passenger, int start) {
        for (int i = start; i < this.seats.length; i++) {
            if(this.tryPutInSeat(passenger, i)) {
                return;
            }
        }
    }

	@Override
	public boolean tryPutInSeat(Entity passenger, int seatID) {
		if (this.seats[0].occupant == passenger) {
			this.setControlState((byte) 0);
		}
		if (seatID < this.seats.length && seatID >= 0) {
			Seat seat = this.seats[seatID];
			if (seat.occupant == null && seat.predicate.test(passenger)) {
				for (Seat seat1 : this.seats) {
					if (seat1.occupant == passenger) {
						seat1.occupant = null;
					}
				}

				seat.occupant = passenger;

				return true;
			}
		}
		return false;
	}

    @Nullable
    @Override
    public Entity getEntityInSeat(int seatID) {
        return getSeat(seatID).getOccupant();
    }

    @Override
    public int getSeatForEntity(Entity entity) {
        for(int i = 0; i < this.seats.length; i++) {
            if(this.seats[i].getOccupant() == entity) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        for (Seat seat : this.seats) {
            if (passenger.equals(seat.occupant)) {
                seat.occupant = null;
                break;
            }
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (this.isEntityInvulnerable(source)) {
            return false;
        }
        if (!this.world.isRemote) {
            if (source.getTrueSource() instanceof EntityPlayer) {
                amount *= 10;
                this.healAmount += amount;
                this.healCooldown = 40;
            }
            this.setHealth(this.getHealth() - amount);
        }
        return true;
    }

    @Override
    public void updatePassenger(Entity passenger) {
        if (this.isPassenger(passenger)) {
            Seat seat = null;
            for (Seat s : this.seats) {
                if (passenger.equals(s.occupant)) {
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
            passenger.setPosition(pos.x, pos.y, pos.z);
            passenger.rotationYaw += this.rotationDelta;
            passenger.setRotationYawHead(passenger.getRotationYawHead() + this.rotationDelta);
            if (passenger instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) passenger;
                living.renderYawOffset += (living.rotationYaw - living.renderYawOffset) * 0.6F;
            }
        }
    }
    
    public Seat getSeat(int id) {
        if(id < seats.length) {
            return seats[id];
        }
        return null;
    }
    
    protected boolean shouldStopUpdates() {
	    return false;
    }
    
    @Override
    public void setDead() {
        super.setDead();
        TyretrackRenderer.uploadList(this);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        this.setHealth(compound.getFloat("Health"));
        this.healAmount = compound.getFloat("HealAmount");
        this.setSpeed(Speed.values()[compound.getInteger("Speed")]); 
        NBTTagCompound tag = compound.getCompoundTag("InterpValues");
        
        this.backValue.deserializeNBT(tag.getCompoundTag("Back"));
        this.frontValue.deserializeNBT(tag.getCompoundTag("Front"));
        this.leftValue.deserializeNBT(tag.getCompoundTag("Left"));
        this.rightValue.deserializeNBT(tag.getCompoundTag("Right"));
        this.dataManager.set(RECORD_ITEM, new ItemStack(compound.getCompoundTag("RecordItem")));

    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setFloat("Health", this.getHealth());
        compound.setFloat("HealAmount", this.healAmount);
        compound.setInteger("Speed", this.getSpeed().ordinal());
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Back", this.backValue.serializeNBT());
        tag.setTag("Front", this.frontValue.serializeNBT());
        tag.setTag("Left", this.leftValue.serializeNBT());
        tag.setTag("Right", this.rightValue.serializeNBT());        
        compound.setTag("InterpValues", tag);
        compound.setTag("RecordItem", this.dataManager.get(RECORD_ITEM).serializeNBT());
    }

    public void startSound() {
        ClientProxy.playCarSound(this);
    }

    public final class Seat {

    	 private final InterpValue interpValue = new InterpValue(CarEntity.this, 0.1D);

         private float offsetX;
         private float offsetY;
         private float offsetZ;

         private final float radius;
         private final float height;
         private final Predicate<Entity> predicate;
         
         private Entity occupant;

         public Seat(float offsetX, float offsetY, float offsetZ, float radius, float height) {
             this(offsetX, offsetY, offsetZ, radius, height, entity -> true);
         }
         
         public Seat(float offsetX, float offsetY, float offsetZ, float radius, float height, Predicate<Entity> predicate) {
             this.offsetX = offsetX;
             this.offsetY = offsetY;
             this.offsetZ = offsetZ;
             this.radius = radius;
             this.height = height;
             this.predicate = predicate;
         }

         protected void so(float x, float y, float z) {
             this.offsetX = x;
             this.offsetY = y;
             this.offsetZ = z;
         }

         public Entity getOccupant() {
             return this.occupant;
         }

         public Vec3d getPos() {
             double theta = Math.toRadians(CarEntity.this.rotationYaw);
             double sideX = Math.cos(theta);
             double sideZ = Math.sin(theta);
             double forwardTheta = theta + Math.PI / 2;
             double forwardX = Math.cos(forwardTheta);
             double forwardZ = Math.sin(forwardTheta);
             double x = CarEntity.this.posX + sideX * this.offsetX + forwardX * this.offsetZ;
             double y = CarEntity.this.posY + this.offsetY;
             double z = CarEntity.this.posZ + sideZ * this.offsetX + forwardZ * this.offsetZ;
             return new Vec3d(x, y, z);
         }

         public AxisAlignedBB getBounds() {
             Vec3d pos = this.getPos();
             double x = pos.x;
             double y = pos.y;
             double z = pos.z;
             return new AxisAlignedBB(x - this.radius, y, z - this.radius, x + this.radius, y + this.offsetY + this.height, z + this.radius);
         }

         public InterpValue getInterpValue() {
             return interpValue;
         }
    }
    
    protected final class WheelData {
        public final Vector2d bl;
        public final Vector2d br;
        public final Vector2d fl;
        public final Vector2d fr;

        public final Vector4d carVector;

        public WheelData(double backLeftX, double backLeftZ, double frontRightX, double frontRightZ) {
            bl = new Vector2d(backLeftX, backLeftZ);
            br = new Vector2d(frontRightX, backLeftZ);
            fl = new Vector2d(backLeftX, frontRightZ);
            fr = new Vector2d(frontRightX, frontRightZ);

            carVector = new Vector4d(backLeftX, backLeftZ, frontRightX, frontRightZ);
        }
    }
    
    public enum Speed {
        //The modifiers ARE hardcoded. If you want to change them, please talk to me first. The tyre mark code relies on the modifiers being how they are
        SLOW(0.5f),
        MEDIUM(1f),
        FAST(2f);

        public final float modifier;

        Speed(float modifier) {
            this.modifier = modifier;
        }
    }

    protected abstract Seat[] createSeats();

    protected abstract WheelData createWheels();
    
    public abstract void dropItems();

    public float getSoundVolume() {
    	return (Math.abs(this.wheelRotateAmount) + 0.001F) / (this.sound == null || this.sound.isDonePlaying() ? 2f : 4f);
    }
}
