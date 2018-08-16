package org.jurassicraft.server.entity;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import mcp.MethodsReturnNonnullByDefault;
import net.ilexiconn.llibrary.client.model.tools.ChainBuffer;
import net.ilexiconn.llibrary.server.animation.Animation;
import net.ilexiconn.llibrary.server.animation.AnimationHandler;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.Path;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jurassicraft.JurassiCraft;
import org.jurassicraft.client.model.animation.EntityAnimation;
import org.jurassicraft.client.model.animation.PoseHandler;
import org.jurassicraft.server.api.Animatable;
import org.jurassicraft.server.block.entity.FeederBlockEntity;
import org.jurassicraft.server.block.machine.FeederBlock;
import org.jurassicraft.server.conf.JurassiCraftConfig;
import org.jurassicraft.server.damage.DinosaurDamageSource;
import org.jurassicraft.server.entity.ai.*;
import org.jurassicraft.server.entity.ai.animations.CallAnimationAI;
import org.jurassicraft.server.entity.ai.animations.HeadCockAnimationAI;
import org.jurassicraft.server.entity.ai.animations.LookAnimationAI;
import org.jurassicraft.server.entity.ai.animations.RoarAnimationAI;
import org.jurassicraft.server.entity.ai.metabolism.DrinkEntityAI;
import org.jurassicraft.server.entity.ai.metabolism.EatFoodItemEntityAI;
import org.jurassicraft.server.entity.ai.metabolism.FeederEntityAI;
import org.jurassicraft.server.entity.ai.metabolism.GrazeEntityAI;
import org.jurassicraft.server.entity.ai.navigation.DinosaurJumpHelper;
import org.jurassicraft.server.entity.ai.navigation.DinosaurMoveHelper;
import org.jurassicraft.server.entity.ai.navigation.DinosaurPathNavigate;
import org.jurassicraft.server.entity.ai.util.OnionTraverser;
import org.jurassicraft.server.entity.ai_new.AIController;
import org.jurassicraft.server.entity.item.DinosaurEggEntity;
import org.jurassicraft.server.food.FoodHelper;
import org.jurassicraft.server.food.FoodType;
import org.jurassicraft.server.genetics.GeneticsHelper;
import org.jurassicraft.server.item.FossilItem;
import org.jurassicraft.server.item.ItemHandler;
import org.jurassicraft.server.json.dinosaur.entity.objects.EntityJsonAttributes;
import org.jurassicraft.server.json.dinosaur.entity.objects.EntityJsonSounds;
import org.jurassicraft.server.json.dinosaur.objects.AdultBabyValue;
import org.jurassicraft.server.message.SetOrderMessage;
import org.jurassicraft.server.message.TrackedAddMessage;
import org.jurassicraft.server.registries.JurassicraftRegisteries;
import org.jurassicraft.server.util.GameRuleHandler;
import org.jurassicraft.server.util.LangHelper;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.vecmath.Vector3f;
import java.util.*;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DinosaurEntity extends EntityCreature implements IEntityAdditionalSpawnData, Animatable {
	private static final Logger LOGGER = LogManager.getLogger();

	private static final DataParameter<Boolean> WATCHER_IS_CARCASS = EntityDataManager.createKey(DinosaurEntity.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Integer> WATCHER_AGE = EntityDataManager.createKey(DinosaurEntity.class, DataSerializers.VARINT);
	private static final DataParameter<Boolean> WATCHER_IS_SLEEPING = EntityDataManager.createKey(DinosaurEntity.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Boolean> WATCHER_HAS_TRACKER = EntityDataManager.createKey(DinosaurEntity.class, DataSerializers.BOOLEAN);
	private static final DataParameter<String> WATCHER_OWNER = EntityDataManager.createKey(DinosaurEntity.class, DataSerializers.STRING);
	private static final DataParameter<Order> WATCHER_ORDER = EntityDataManager.createKey(DinosaurEntity.class, DinosaurSerializers.ORDER);
	private static final DataParameter<Boolean> WATCHER_IS_RUNNING = EntityDataManager.createKey(DinosaurEntity.class, DataSerializers.BOOLEAN);
	public LegSolver legSolver;
	public InventoryDinosaur inventory;
	public MetabolismContainer metabolism;
	public Dinosaur dinosaur = Dinosaur.MISSING;
	public int dinosaurAge;
	public int prevAge;
	public EntityAITasks animationTasks;
	public Order order = Order.WANDER;
	public int growthSpeedOffset;
	public boolean isCarcass;
	public int carcassHealth;
	public String genetics;
	public int geneticsQuality;
	public boolean isMale;
	public boolean hasTracker;
	@Nullable
	public UUID owner;
	public boolean isSleeping;
	public int tranquilizerTicks;
	public int stayAwakeTime;
	public boolean useInertialTweens;
	public Predicate<Entity> attackPredicate = entity -> false;
	public boolean deserializing;
	public int ticksUntilDeath;
	public int attackCooldown;
	@SideOnly(Side.CLIENT)
	public ChainBuffer tailBuffer;
	public Herd herd;
	public Family family;
	public Set<Relationship> relationships = new HashSet<>();
	public int wireTicks;
	public int disableHerdingTicks;
	public boolean isSittingNaturally;
	@SideOnly(Side.CLIENT)
	private boolean forceRender;
	private Animation animation = EntityAnimation.IDLE.get();
	private int animationTick;
	private int animationLength;
	private DinosaurLookHelper lookHelper;
	private BlockPos closestFeeder;
	private int feederSearchTick;
	private boolean inLava;
	private DinosaurAttributes attributes;
	private int breedCooldown;
	private DinosaurEntity breeding;
	private Set<DinosaurEntity> children = new HashSet<>();
	private int pregnantTime;
	private int jumpHeight;
	private boolean isSkeleton;
    private Vec3d glidingPos;
    public List<String> trackers = new ArrayList<>();

    private UUID inMouthEntity; //The entity of whose mouth this is inside
	private UUID entityInMouth; //The entity inside this entities mouth

    private AIController aiController;

	public DinosaurEntity(World world) {
		super(world);

		aiController = new AIController(this);

		this.metabolism = new MetabolismContainer(this);
		this.inventory = new InventoryDinosaur(this);

		this.moveHelper = new DinosaurMoveHelper(this);
		this.jumpHelper = new DinosaurJumpHelper(this);

		this.navigator = new DinosaurPathNavigate(this, this.world);
		((DinosaurPathNavigate) this.navigator).setCanSwim(true);
		this.lookHelper = new DinosaurLookHelper(this);
		this.legSolver = this.world == null || !this.world.isRemote ? null : this.createLegSolver();

		this.genetics = GeneticsHelper.randomGenetics(this.rand);
		this.isMale = this.rand.nextBoolean();

		this.resetAttackCooldown();

		this.animationTick = 0;

		this.setUseInertialTweens(true);

		this.animationTasks = new EntityAITasks(world.profiler);

		this.tasks.addTask(0, new AdvancedSwimEntityAI(this));

		this.tasks.addTask(0, new EscapeWireEntityAI(this));

		this.tasks.addTask(0, new DinosaurWanderAvoidWater(this, 0.8D));

		this.tasks.addTask(1, new GrazeEntityAI(this));

		this.tasks.addTask(1, new TargetCarcassEntityAI(this));

		this.tasks.addTask(1, new RespondToAttackEntityAI(this));

		this.tasks.addTask(1, new TemptNonAdultEntityAI(this, 0.6));

		this.tasks.addTask(1, new EntityAIPanic(this, 1.25D));

		this.tasks.addTask(2, new DefendOwnerEntityAI(this));
		this.tasks.addTask(2, new AssistOwnerEntityAI(this));

		this.tasks.addTask(2, new FleeEntityAI(this));

		this.tasks.addTask(2, new ProtectInfantEntityAI<>(this));

		this.tasks.addTask(3, new DinosaurWanderEntityAI(this, 0.8F, 10));
		this.tasks.addTask(3, new FollowOwnerEntityAI(this));

		this.tasks.addTask(3, this.getAttackAI());

		this.tasks.addTask(4, new EntityAILookIdle(this));
		this.tasks.addTask(4, new EntityAIWatchClosest(this, EntityLivingBase.class, 6.0F));

		this.animationTasks.addTask(0, new SleepEntityAI(this));

		this.animationTasks.addTask(1, new DrinkEntityAI(this));
		this.animationTasks.addTask(1, new MateEntityAI(this));
		this.animationTasks.addTask(1, new EatFoodItemEntityAI(this));
		this.animationTasks.addTask(1, new FeederEntityAI(this));

		this.animationTasks.addTask(3, new CallAnimationAI(this));
		this.animationTasks.addTask(3, new RoarAnimationAI(this));
		this.animationTasks.addTask(3, new LookAnimationAI(this));
		this.animationTasks.addTask(3, new HeadCockAnimationAI(this));

		if (world.isRemote) {
			this.initClient();
		}

		this.ignoreFrustumCheck = true;
		this.setSkeleton(false);

	}

    @Override
    public void travel(float strafe, float vertical, float forward) {
        float prevRot = this.rotationPitch;
        if(this.getAnimation() == EntityAnimation.GLIDING.get() && glidingPos != null) {
            this.rotationPitch = (float) -Math.toDegrees(Math.asin((this.glidingPos.y - this.posY) / glidingPos.distanceTo(this.getPositionVector())));
        }
        super.travel(strafe, vertical, forward);
        this.rotationPitch = prevRot;
    }

    public void setGlidingTo(Vec3d glidingPos) {
        this.glidingPos = glidingPos;
    }

	public void pickUpEntityInMouth(DinosaurEntity entity) {
		entity.inMouthEntity = this.getUniqueID();
		this.entityInMouth = entity.getUniqueID();
	}

	@Nullable
	protected LegSolver createLegSolver() {
		Map<String, Float> params = dinosaur.animation.legs.params;
		switch (dinosaur.animation.legs.type) {
			case BIPED:
				return new LegSolverBiped(params.get("forward"), params.get("side"), params.get("range"));
			case QUADRUPED:
				if (params.containsKey("forwardCenter")) {
					return new LegSolverQuadruped(params.get("forwardCenter"), params.get("forward"), params.get("side"), params.get("frontRange"), params.get("backRange"));
				} else {
					return new LegSolverQuadruped(params.get("forward"), params.get("side"), params.get("frontRange"), params.get("backRange"));
				}
			default:
				return null;
		}
	}
	
	public boolean addTrackingID(String id) {
		if(!this.trackers.contains(id)) {
			this.trackers.add(id);
			return true;
		}
		return false;
	}

	@Override
	protected EntityBodyHelper createBodyHelper() {
		return new SmartBodyHelper(this);
	}

	@Override
	public EntityLookHelper getLookHelper() {
		return this.lookHelper;
	}

	private void initClient() {
		this.tailBuffer = new ChainBuffer();
	}

	public boolean shouldSleep() {
		if (this.metabolism.isDehydrated() || this.metabolism.isStarving()) {
			return false;
		}
		SleepTime sleepTime = this.getDinosaur().sleepTime;
		return sleepTime.shouldSleep() && this.getDinosaurTime() > sleepTime.getAwakeTime() && !this.hasPredators() && (this.herd == null || this.herd.enemies.isEmpty());
	}

	private boolean hasPredators() {
		for (EntityLiving predator : this.world.getEntitiesWithinAABB(EntityLiving.class, new AxisAlignedBB(this.posX - 10F, this.posY - 5F, this.posZ - 10F, this.posX + 10F, this.posY + 5F, this.posZ + 10F), e -> e != DinosaurEntity.this)) {
			boolean hasDinosaurPredator = false;

			if (predator instanceof DinosaurEntity) {
				DinosaurEntity dinosaur = (DinosaurEntity) predator;

				if (!dinosaur.isCarcass() || dinosaur.isSleeping) {
					if (dinosaur.getAttackPredicate().test(this)) {
						hasDinosaurPredator = true;
					}
				}
			}

			if (this.getLastAttackedEntity() == predator || predator.getAttackTarget() == this || hasDinosaurPredator) {
				return true;
			}
		}

		return false;
	}

	public int getDinosaurTime() {
		SleepTime sleepTime = this.getDinosaur().sleepTime;

		long time = (this.world.getWorldTime() % 24000) - sleepTime.getWakeUpTime();

		if (time < 0) {
			time += 24000;
		}

		return (int) time;
	}

	@SuppressWarnings("unused")
	public boolean hasTracker() {
		return this.hasTracker;
	}

	@SuppressWarnings("unused")
	public void setHasTracker(boolean hasTracker) {
		this.hasTracker = hasTracker;
	}

	@Nullable
	public UUID getOwner() {
		return this.owner;
	}

	public void setOwner(EntityPlayer player) {
		if (this.dinosaur.isImprintable) {
			UUID prevOwner = this.owner;
			this.owner = player.getUniqueID();

			if (!this.owner.equals(prevOwner)) {
				player.sendMessage(new TextComponentString(new LangHelper("message.tame.name").withProperty("dinosaur", "entity.jurassicraft." + this.dinosaur.name.toLowerCase(Locale.ENGLISH) + ".name").build()));
			}
		}
	}

	@Override
	public boolean attackEntityAsMob(Entity entity) {
		if (entity instanceof DinosaurEntity && ((DinosaurEntity) entity).isCarcass()) {
			this.setAnimation(EntityAnimation.EATING.get());
		} else {
			this.setAnimation(EntityAnimation.ATTACKING.get());
		}

		while (entity.getRidingEntity() != null) {
			entity = entity.getRidingEntity();
		}

		float damage = (float) this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();

		if (entity.attackEntityFrom(new DinosaurDamageSource("mob", this), damage)) {
			if (entity instanceof DinosaurEntity && ((DinosaurEntity) entity).isCarcass()) {
				DinosaurEntity dinosaur = (DinosaurEntity) entity;
				if (dinosaur.herd != null && this.herd != null && dinosaur.herd.fleeing && dinosaur.herd.enemies.contains(this)) {
					this.herd.enemies.removeAll(dinosaur.herd.members);
					for (DinosaurEntity member : this.herd) {
						if (member.getAttackTarget() != null && dinosaur.herd.members.contains(member.getAttackTarget())) {
							member.setAttackTarget(null);
						}
					}
					this.herd.state = Herd.State.IDLE;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean attackEntityFrom(DamageSource damageSource, float amount) {
		boolean canHarmInCreative = damageSource.canHarmInCreative();

		Entity attacker = damageSource.getTrueSource();

		if (!this.isCarcass()) {
			if (this.getHealth() - amount <= 0.0F) {
				if (!canHarmInCreative) {
					this.playSound(this.getSoundForAnimation(EntityAnimation.DYING.get()), this.getSoundVolume(), this.getSoundPitch());
					this.setHealth(this.getMaxHealth());
					this.setCarcass(true);
					return true;
				}

				if (attacker instanceof DinosaurEntity) {
					this.getRelationship(attacker, true).onAttacked(amount);
				}

				return super.attackEntityFrom(damageSource, amount);
			} else {
				if (this.getAnimation() == EntityAnimation.RESTING.get() && !this.world.isRemote) {
					this.setAnimation(EntityAnimation.IDLE.get());
					this.isSittingNaturally = false;
				}

				if (!this.world.isRemote) {
					this.setAnimation(EntityAnimation.INJURED.get());
				}

				if (this.shouldSleep()) {
					this.disturbSleep();
				}

				if (attacker instanceof EntityLivingBase) {
					this.respondToAttack((EntityLivingBase) attacker);
				}

				return super.attackEntityFrom(damageSource, amount);
			}
		} else if (!this.world.isRemote) {
			if (canHarmInCreative) {
				return super.attackEntityFrom(damageSource, amount);
			}

			if (this.hurtResistantTime <= this.maxHurtResistantTime / 2.0F) {
				this.hurtTime = this.maxHurtTime = 10;
			}

			if (damageSource != DamageSource.DROWN) {
				if (!this.dead && this.carcassHealth >= 0 && this.world.getGameRules().getBoolean("doMobLoot")) {
					this.dropMeat(attacker);
				}

				if (this.carcassHealth <= 0) {
					this.onDeath(damageSource);
					this.setDead();
				}

				this.carcassHealth--;
			}
		}

		return false;
	}

	private Relationship getRelationship(Entity entity, boolean create) {
		for (Relationship relationship : this.relationships) {
			if (relationship.getUUID().equals(entity.getUniqueID())) {
				return relationship;
			}
		}
		if (create) {
			Relationship relationship = new Relationship(entity.getUniqueID(), (short) 0);
			this.relationships.add(relationship);
			return relationship;
		}
		return null;
	}

	private void dropMeat(Entity attacker) {
		int fortune = 0;

		if (attacker instanceof EntityLivingBase) {
			fortune = EnchantmentHelper.getLootingModifier((EntityLivingBase) attacker);
		}

		int count = this.rand.nextInt(2) + 1 + fortune;

		boolean burning = this.isBurning();

		for (int i = 0; i < count; ++i) {
			if (burning) {
				this.entityDropItem(ItemHandler.DINOSAUR_STEAK.getItemStack(this.dinosaur), 0.0F);
			} else {
				this.dropStackWithGenetics(ItemHandler.DINOSAUR_MEAT.getItemStack(this.dinosaur));
			}
		}
	}

	@Override
	public boolean canBePushed() {
		return super.canBePushed() && !this.isCarcass() && !this.isSleeping();
	}

	@Override
	public EntityItem entityDropItem(ItemStack stack, float offsetY) {
		if (stack.getCount() != 0 && stack.getItem() != null) {
			Random rand = new Random();

			EntityItem item = new EntityItem(this.world, this.posX + ((rand.nextFloat() * this.width) - this.width / 2), this.posY + (double) offsetY, this.posZ + ((rand.nextFloat() * this.width) - this.width / 2), stack);
			item.setDefaultPickupDelay();

			if (this.captureDrops) {
				this.capturedDrops.add(item);
			} else {
				this.world.spawnEntity(item);
			}

			return item;
		} else {
			return null;
		}
	}

	@Override
	public void knockBack(Entity entity, float p_70653_2_, double motionX, double motionZ) {
		if (this.rand.nextDouble() >= this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).getAttributeValue()) {
			this.isAirBorne = true;
			float distance = MathHelper.sqrt(motionX * motionX + motionZ * motionZ);
			float multiplier = 0.4F;
			this.motionX /= 2.0D;
			this.motionZ /= 2.0D;
			this.motionX -= motionX / distance * multiplier;
			this.motionZ -= motionZ / distance * multiplier;

			// TODO We should make knockback bigger and into air if dino is much smaller than attacking dino
		}
	}

	@Override
	public void onDeath(DamageSource cause) {
		super.onDeath(cause);

		if (this.herd != null) {
			if (this.herd.leader == this) {
				this.herd.updateLeader();
			}

			this.herd.members.remove(this);
		}

		if (this.family != null) {
			UUID head = this.family.getHead();
			if (head == null || head.equals(this.getUniqueID())) {
				this.family.update(this);
			}
		}

		if (cause.getTrueSource() instanceof EntityLivingBase) {
			this.respondToAttack((EntityLivingBase) cause.getTrueSource());
		}
	}
	
	public void applyTDart(EntityPlayer player, String ID) {
		boolean success = addTrackingID(ID);
			sendTrackingPacket(player, success);
	}

	@Override
	public void playLivingSound() {
		if (this.getAnimation() == EntityAnimation.IDLE.get()) {
			this.setAnimation(EntityAnimation.SPEAK.get());
			super.playLivingSound();
		}
	}

	@Override
	public void entityInit() {
		super.entityInit();

		this.dataManager.register(WATCHER_IS_CARCASS, this.isCarcass);
		this.dataManager.register(WATCHER_AGE, this.dinosaurAge);
		this.dataManager.register(WATCHER_IS_SLEEPING, this.isSleeping);
		this.dataManager.register(WATCHER_HAS_TRACKER, this.hasTracker);
		this.dataManager.register(WATCHER_OWNER, "");
		this.dataManager.register(WATCHER_ORDER, Order.WANDER);
		this.dataManager.register(WATCHER_IS_RUNNING, false);
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();

		this.attributes = DinosaurAttributes.create(this);

		this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
	}

	public void updateAttributes() {
		double prevHealth = this.getMaxHealth();
		double newHealth = Math.max(1.0F, this.interpolate(this.dinosaur.properties.statistics.health) * this.attributes.getHealthModifier());
		double speed = this.interpolate(this.dinosaur.properties.statistics.speed) * this.attributes.getSpeedModifier();
		double strength = this.getAttackDamage() * this.attributes.getDamageModifier();

		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(newHealth);
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(speed);
		this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(strength);

		GrowthStage growthStage = this.getGrowthStage();

		if(growthStage == GrowthStage.INFANT){
		    this.dinosaur.properties.statistics.health.set("baby", newHealth);
            this.dinosaur.properties.statistics.speed.set("baby", newHealth);
            this.dinosaur.properties.statistics.strength.set("baby", newHealth);
        }else{
            this.dinosaur.properties.statistics.health.set("adult", newHealth);
            this.dinosaur.properties.statistics.speed.set("adult", newHealth);
            this.dinosaur.properties.statistics.strength.set("adult", newHealth);
        }


		if (prevHealth != newHealth) {
			this.heal((float) (newHealth - prevHealth));
		}
	}

	private void updateBounds() {
		float scale = this.attributes.getScaleModifier();
		float width = MathHelper.clamp((float) this.interpolate(this.dinosaur.properties.statistics.sizeX) * scale, 0.3F, 4.0F);
		float height = MathHelper.clamp((float) this.interpolate(this.dinosaur.properties.statistics.sizeY) * scale, 0.3F, 4.0F);

		this.stepHeight = Math.max(1.0F, (float) (Math.ceil(height / 2.0F) / 2.0F));

		if (this.isCarcass) {
			this.setSize(Math.min(5.0F, height), width);
		} else {
			this.setSize(width, height);
		}
	}

	public double interpolate(AdultBabyValue adultBabyValue) {
		int dinosaurAge = this.dinosaurAge;
		int maxAge = this.dinosaur.maximumAge;
		if (dinosaurAge > maxAge) {
			dinosaurAge = maxAge;
		}
		return (adultBabyValue.get("adult") - adultBabyValue.get("baby")) / maxAge * dinosaurAge + adultBabyValue.get("baby");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isInRangeToRenderDist(double distance) {
		return true;
	}

	public void setupDisplay(boolean isMale) {
		this.setFullyGrown();
		this.setMale(isMale);
		this.ticksExisted = 4;
	}

	@Override
	public int getTalkInterval() {
		return 200;
	}

	@Override
	public float getSoundPitch() {
		return (float) this.interpolate(new AdultBabyValue(.5F, 1.0F)) + ((this.rand.nextFloat() - 0.5F) * 0.125F);
	}

	@Override
	public float getSoundVolume() {
		return (this.isCarcass() || this.isSleeping) ? 0.0F : (2.0F * ((float) this.interpolate(new AdultBabyValue(.2F, 1.0F))));
	}

	public String getGenetics() {
		return this.genetics;
	}

	public void setGenetics(String genetics) {
		this.genetics = genetics;
	}

	public boolean isEntityFreindly(Entity entity) {
		return entity instanceof DinosaurEntity && ((DinosaurEntity) entity).dinosaur == this.dinosaur;
	}

	public boolean canEatEntity(Entity entity) {
		if (entity instanceof EntityPlayer && (((EntityPlayer) entity).isCreative() || ((EntityPlayer) entity).isSpectator())) {
			return false;
		}
		return !isEntityFreindly(entity);
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (this.inMouthEntity != null) {
			List<Entity> eList = this.world.getEntities(DinosaurEntity.class, e -> this.inMouthEntity.equals(e.getUniqueID()));
			if (!eList.isEmpty()) {
				Entity e = eList.get(0);
				this.setPosition(e.posX, e.posY, e.posZ);
				this.setRotation(0, 0);
				this.setRotationYawHead(0);
				this.setAnimation(EntityAnimation.SLEEPING.get());
			}
		}
		if (this.breedCooldown > 0) {
			this.breedCooldown--;
		}

		if (!this.world.isRemote && this.dinosaur.diet.canEat(this, FoodType.MEAT) && this.getMetabolism().isHungry()) {
			world.getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().grow(10, 10, 10), this::canEatEntity).stream().findAny().ifPresent(this::setAttackTarget);
		}

		if (!this.isMale() && !this.world.isRemote) {
			if (this.isPregnant()) {
				if (--this.pregnantTime <= 0) {
					this.getNavigator().clearPath();
					this.setAnimation(this.dinosaur.birthType == Dinosaur.BirthType.LIVE_BIRTH ? EntityAnimation.GIVING_BIRTH.get() : EntityAnimation.LAYING_EGG.get());
					if (this.family != null) {
						this.family.setHome(this.getPosition(), 6000);
					}
//                    this.children.clear(); //Why was this even here?
				}
			}
			if ((this.getAnimation() == EntityAnimation.LAYING_EGG.get() || this.getAnimation() == EntityAnimation.GIVING_BIRTH.get()) && this.animationTick == this.getAnimationLength() / 2) {
				for (DinosaurEntity child : this.children) {
					Entity entity;
					if (this.dinosaur.birthType == Dinosaur.BirthType.LIVE_BIRTH) {
						entity = child;
						child.setAge(0);
						if (this.family != null) {
							this.family.addChild(entity.getUniqueID());
						}
					} else {
						entity = new DinosaurEggEntity(this.world, child, this);
					}
					entity.setPosition(this.posX + (this.rand.nextFloat() - 0.5F), this.posY + 0.5F, this.posZ + (this.rand.nextFloat() - 0.5F));
					this.world.spawnEntity(entity);
				}
			}
		}

		if (this.breeding != null) {
			if (this.ticksExisted % 10 == 0) {
				this.getNavigator().tryMoveToEntityLiving(this.breeding, 1.0);
			}
			boolean dead = this.breeding.isDead || this.breeding.isCarcass();
			if (dead || this.getEntityBoundingBox().intersects(this.breeding.getEntityBoundingBox().expand(3, 3, 3))) {
				if (!dead) {
					this.breedCooldown = this.dinosaur.breedCooldown;
					if (!this.isMale()) {
						int minClutch = this.dinosaur.minClutch;
						int maxClutch = this.dinosaur.maxClutch;
						for (int i = 0; i < (this.rand.nextInt(maxClutch - minClutch) + minClutch); i++) {
							boolean isMale = this.rand.nextBoolean();
							DinosaurEntity child = this.dinosaur.createEntity(this.world);
							child.setAge(0);
							child.setMale(isMale);
							child.setDNAQuality(Math.min(100, this.getDNAQuality() + this.breeding.getDNAQuality()));
							DinosaurAttributes attributes = DinosaurAttributes.combine(this, this.getAttributes(), this.breeding.getAttributes());
							String genetics = "";
							int maleChances = (this.isMale == isMale ? 65 : 15) + rand.nextInt(20);
							for (int c = 0; c < this.genetics.length(); c++) {
								if (c / this.genetics.length() <= maleChances) {
									genetics += this.genetics.charAt(i);
								} else {
									genetics += this.breeding.genetics.charAt(i);
								}
							}
							child.setGenetics(genetics);
							child.setAttributes(attributes);
							this.children.add(child);
						}
						this.pregnantTime = 9600;
					}
				}
				this.breeding = null;
			}
		}

		if (this.ticksExisted % 10 == 0) {
			this.inLava = this.isInLava();
		}

		if (this.isClimbing()) {
			this.prevLimbSwingAmount = this.limbSwingAmount;
			double deltaY = (this.posY - this.prevPosY) * 4.0F;
			if (deltaY > 1.0F) {
				deltaY = 1.0F;
			}
			this.limbSwingAmount += (deltaY - this.limbSwingAmount) * 0.4F;
			this.limbSwing += this.limbSwingAmount;
		}

		if (!this.isCarcass) {
			if (this.firstUpdate) {
				this.updateAttributes();
			}

			this.updateGrowth();

			if (!this.world.isRemote) {
				if (this.metabolism.isHungry()) {
					List<EntityItem> entitiesWithinAABB = this.world.getEntitiesWithinAABB(EntityItem.class, this.getEntityBoundingBox().expand(1.0, 1.0, 1.0));
					for (EntityItem itemEntity : entitiesWithinAABB) {
						Item item = itemEntity.getItem().getItem();
						if (FoodHelper.isEdible(this, this.dinosaur.diet, item)) {
							this.setAnimation(EntityAnimation.EATING.get());

							if (itemEntity.getItem().getCount() > 1) {
								itemEntity.getItem().shrink(1);
							} else {
								itemEntity.setDead();
							}

							this.getMetabolism().eat(FoodHelper.getHealAmount(item));
							FoodHelper.applyEatEffects(this, item);
							this.heal(10.0F);

							break;
						}
					}
				}

				this.metabolism.update();
			}

			if (this.ticksExisted % 62 == 0) {
				this.playSound(this.getBreathingSound(), this.getSoundVolume(), this.getSoundPitch());
			}

			if (this.dinosaur.homeType != Dinosaur.DinosaurHomeType.MARINE) {
				if (this.isInsideOfMaterial(Material.WATER) || (this.getNavigator().noPath() && this.inWater() || this.inLava())) {
					this.getJumpHelper().setJumping();
				} else {
					if (this.isSwimming()) {
						Path path = this.getNavigator().getPath();
						if (path != null) {
							AxisAlignedBB detectionBox = this.getEntityBoundingBox().expand(0.5, 0.5, 0.5);
							if (this.world.collidesWithAnyBlock(detectionBox)) {
								List<AxisAlignedBB> colliding = this.world.getCollisionBoxes(this.getAttackingEntity(), detectionBox);
								boolean swimUp = false;
								for (AxisAlignedBB bound : colliding) {
									if (bound.maxY > this.getEntityBoundingBox().minY) {
										swimUp = true;
										break;
									}
								}
								if (swimUp) {
									this.getJumpHelper().setJumping();
								}
							}
						}
					}
				}
			}

			if (this.herd == null) {
				this.herd = new Herd(this);
			}

			if (!this.world.isRemote) {
				if (this.order == Order.WANDER) {
					if (this.herd.state == Herd.State.IDLE && this.getAttackTarget() == null && !this.metabolism.isThirsty() && !this.metabolism.isHungry() && this.getNavigator().noPath()) {
						if (!this.isSleeping && this.onGround && !this.isInWater() && this.getAnimation() == EntityAnimation.IDLE.get() && this.rand.nextInt(800) == 0) {
							this.setAnimation(EntityAnimation.RESTING.get());
							this.isSittingNaturally = true;
						}
					} else if (this.getAnimation() == EntityAnimation.RESTING.get()) {
						this.setAnimation(EntityAnimation.IDLE.get());
						this.isSittingNaturally = false;
					}
				}

				if (this == this.herd.leader) {
					this.herd.update();
				}

				if (this.ticksExisted % 10 == 0) {
					if (this.family != null && (this.family.getHead() == null || this.family.getHead().equals(this.getUniqueID()))) {
						if (this.family.update(this)) {
							this.family = null;
						}
					} else if (this.family == null && this.getAttackTarget() == null) {
						if (this.relationships.size() > 0 && this.rand.nextDouble() > 0.9) {
							DinosaurEntity chosen = null;
							Relationship chosenRelationship = null;
							for (Relationship relationship : this.relationships) {
								if (relationship.getScore() > Relationship.MAX_SCORE * 0.9) {
									DinosaurEntity entity = relationship.get(this);
									if (entity != null && this.isMale != entity.isMale) {
										chosen = entity;
										chosenRelationship = relationship;
										break;
									}
								}
							}
							if (chosen != null) {
								this.family = new Family(this.getUniqueID(), chosen.getUniqueID());
								chosenRelationship.setFamily();
								this.breedCooldown = this.rand.nextInt(1000) + 1000;
								chosen.breedCooldown = this.breedCooldown;
							}
						}
					}
					if (this.herd != null) {
						for (DinosaurEntity herdMember : this.herd.members) {
							if (herdMember != this) {
								Relationship relationship = this.getRelationship(herdMember, true);
								relationship.updateHerd(this);
							}
						}
						for (EntityLivingBase enemy : this.herd.enemies) {
							if (enemy instanceof DinosaurEntity) {
								Relationship relationship = new Relationship(enemy.getUniqueID(), (short) -30);
								if (!this.relationships.contains(relationship)) {
									this.relationships.add(relationship);
								}
							}
						}
					}
					if (this.relationships.size() > 0) {
						Set<Relationship> removal = new HashSet<>();
						for (Relationship relationship : this.relationships) {
							if (relationship.update(this)) {
								removal.add(relationship);
							}
						}
						this.relationships.removeAll(removal);
					}
				}

				if (!this.getNavigator().noPath()) {
					if (this.isSittingNaturally && this.getAnimation() == EntityAnimation.RESTING.get()) {
						this.setAnimation(EntityAnimation.IDLE.get());
						this.isSittingNaturally = false;
					}
				}
			}
		}

		if (this.isServerWorld()) {
			this.lookHelper.onUpdateLook();
		}
	}

	private void updateGrowth() {
		if (!this.isDead && this.ticksExisted % 8 == 0 && !this.world.isRemote) {
			if (GameRuleHandler.DINO_GROWTH.getBoolean(this.world)) {
				this.dinosaurAge += Math.min(this.growthSpeedOffset, 960) + 1;
				this.metabolism.decreaseEnergy((int) ((Math.min(this.growthSpeedOffset, 960) + 1) * 0.1));
			}

			if (this.growthSpeedOffset > 0) {
				this.growthSpeedOffset -= 10;

				if (this.growthSpeedOffset < 0) {
					this.growthSpeedOffset = 0;
				}
			}
		}
	}

	@Override
	public void onUpdate() {
		super.onUpdate();

		if (this.ticksUntilDeath > 0) {
			if (--this.ticksUntilDeath == 0) {
				this.playSound(this.getSoundForAnimation(EntityAnimation.DYING.get()), this.getSoundVolume(), this.getSoundPitch());
				this.setHealth(this.getMaxHealth());
				this.setCarcass(true);
			}
		}

		if (this.attackCooldown > 0) {
			this.attackCooldown--;
		}

		if (!this.world.isRemote) {
			if (this.animation == EntityAnimation.LEAP.get()) {
				if (this.motionY < 0) {
					this.setAnimation(EntityAnimation.LEAP_LAND.get());
				}
			} else if (this.animation == EntityAnimation.LEAP_LAND.get() && (this.onGround || this.isSwimming())) {
				this.setAnimation(EntityAnimation.IDLE.get());
			}
		}

		if (this.animation != null && this.animation != EntityAnimation.IDLE.get()) {
			boolean shouldHold = EntityAnimation.getAnimation(this.animation).shouldHold();

			if (this.animationTick < this.animationLength) {
				this.animationTick++;
			} else if (!shouldHold) {
				this.animationTick = 0;
				if (this.animation == EntityAnimation.PREPARE_LEAP.get()) {
					this.setAnimation(EntityAnimation.LEAP.get());
				} else {
					this.setAnimation(EntityAnimation.IDLE.get());
				}
			} else {
				this.animationTick = this.animationLength - 1;
			}
		}

		if (!this.world.isRemote) {
			this.dataManager.set(WATCHER_AGE, this.dinosaurAge);
			this.dataManager.set(WATCHER_IS_SLEEPING, this.isSleeping);
			this.dataManager.set(WATCHER_IS_CARCASS, this.isCarcass);
			this.dataManager.set(WATCHER_HAS_TRACKER, this.hasTracker);
			this.dataManager.set(WATCHER_ORDER, this.order);
			this.dataManager.set(WATCHER_OWNER, this.owner != null ? this.owner.toString() : "");
			this.dataManager.set(WATCHER_IS_RUNNING, this.getAIMoveSpeed() > this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
		} else {
			this.updateTailBuffer();

			this.dinosaurAge = this.dataManager.get(WATCHER_AGE);
			this.isSleeping = this.dataManager.get(WATCHER_IS_SLEEPING);
			this.isCarcass = this.dataManager.get(WATCHER_IS_CARCASS);
			this.hasTracker = this.dataManager.get(WATCHER_HAS_TRACKER);
			this.order = this.dataManager.get(WATCHER_ORDER);

			String owner = this.dataManager.get(WATCHER_OWNER);

			if (owner.length() > 0 && (this.owner == null || !owner.equals(this.owner.toString()))) {
				this.owner = UUID.fromString(owner);
			} else if (owner.length() == 0) {
				this.owner = null;
			}
		}

		if (this.ticksExisted % 20 == 0) {
//            this.updateAttributes();
			this.updateBounds();
		}

		if (this.isCarcass) {
			this.renderYawOffset = this.rotationYaw;
			this.rotationYawHead = this.rotationYaw;
		}

		if (this.isSleeping) {
			if (this.getAnimation() != EntityAnimation.SLEEPING.get()) {
				this.setAnimation(EntityAnimation.SLEEPING.get());
			}
		} else if (this.getAnimation() == EntityAnimation.SLEEPING.get()) {
			this.setAnimation(EntityAnimation.IDLE.get());
		}

		if (!this.world.isRemote) {
			if (this.isCarcass) {
				if (this.getAnimation() != EntityAnimation.DYING.get()) {
					this.setAnimation(EntityAnimation.DYING.get());
				}

				if (this.ticksExisted % 1000 == 0) {
					this.attackEntityFrom(DamageSource.GENERIC, 1.0F);
				}
			} else {
				if (this.isSleeping) {
					if (this.ticksExisted % 20 == 0) {
						if (this.stayAwakeTime <= 0 && this.hasPredators()) {
							this.disturbSleep();
						}
					}

					if (!this.shouldSleep() && !this.world.isRemote && tranquilizerTicks-- <= 0) {
						this.isSleeping = false;
						this.tranquilizerTicks = 0;
					}
				} else if (this.getAnimation() == EntityAnimation.SLEEPING.get()) {
					this.setAnimation(EntityAnimation.IDLE.get());
				}

				if (!this.isSleeping) {
					if (this.order == Order.SIT) {
						if (this.getAnimation() != EntityAnimation.RESTING.get()) {
							this.setAnimation(EntityAnimation.RESTING.get());
						}
					} else if (!this.isSittingNaturally && this.getAnimation() == EntityAnimation.RESTING.get() && !this.world.isRemote) {
						this.setAnimation(EntityAnimation.IDLE.get());
					}
				}
			}
		}

		if (!this.shouldSleep() && !this.isSleeping) {
			this.stayAwakeTime = 0;
		}

		if (this.isServerWorld()) {
			this.animationTasks.onUpdateTasks();
		}

		if (this.stayAwakeTime > 0) {
			this.stayAwakeTime--;
		}
		if (this.wireTicks > 0) {
			this.wireTicks--;
		}
		if (this.disableHerdingTicks > 0) {
			this.disableHerdingTicks--;
		}

		if (this.legSolver != null) {
			double msc = this.dinosaur.scaleInfant / this.dinosaur.scaleAdult;
			this.legSolver.update(this, (float) this.interpolate(new AdultBabyValue(msc, 1.0)) * this.getAttributes().getScaleModifier());
		}

		this.prevAge = this.dinosaurAge;
	}

	private void updateTailBuffer() {
		this.tailBuffer.calculateChainSwingBuffer(68.0F, 3, 7.0F, this);
	}

	@Nullable
	public UUID getEntityInMouth() {
		return entityInMouth;
	}

	@Nullable
	public UUID getInMouthEntity() {
		return inMouthEntity;
	}

	@Override
	public boolean isMovementBlocked() {
		return this.isCarcass() || this.isSleeping() || (this.animation != null && EntityAnimation.getAnimation(this.animation).doesBlockMovement());
	}

	@Override
	protected float updateDistance(float angle, float distance) {
		if (!this.isMovementBlocked()) {
			return super.updateDistance(angle, distance);
		}
		return distance;
	}

	public int getDaysExisted() {
		return (int) Math.floor((this.dinosaurAge * 8.0F) / 24000.0F);
	}

	public void setFullyGrown() {
		this.setAge(this.dinosaur.maximumAge);
	}

	@Nullable
	public Dinosaur getDinosaur() {
		return this.dinosaur;
	}

	public DinosaurEntity setDinosaur(Dinosaur dinosaur) {
		this.dinosaur = dinosaur;

		this.setFullyGrown();
		this.updateAttributes();
//        this.applyEntityAttributes();

		this.metabolism = new MetabolismContainer(this);
		this.inventory = new InventoryDinosaur(this);
		this.setAnimation(EntityAnimation.IDLE.get());
		return this;
	}

	@Override
	public boolean canDespawn() {
		return false;
	}

	public int getDinosaurAge() {
		return this.dinosaurAge;
	}

	public void setAge(int age) {
		this.dinosaurAge = age;
		if (!this.world.isRemote) {
			this.dataManager.set(WATCHER_AGE, this.dinosaurAge);
		}
	}
	
	
	@Override
	public float getEyeHeight() {

		return (float) this.interpolate(new AdultBabyValue(this.dinosaur.babyEyeHeight, this.dinosaur.adultEyeHeight)) * this.attributes.getScaleModifier();

	}

	@Override
	protected void dropFewItems(boolean playerAttack, int looting) {
		for (String bone : this.dinosaur.bones) {
			if (this.rand.nextInt(10) != 0) {
				this.dropStackWithGenetics(ItemHandler.FRESH_FOSSIL.createNewStack(new FossilItem.FossilInfomation(this.dinosaur, bone)));
			}
		}
	}

	private void dropStackWithGenetics(ItemStack stack) {
		NBTTagCompound nbt = stack.getTagCompound();
		nbt.setInteger("DNAQuality", this.geneticsQuality);
		nbt.setString("Dinosaur", this.dinosaur.getRegistryName().toString());
		nbt.setString("Genetics", this.genetics);
		stack.setTagCompound(nbt);
		this.entityDropItem(stack, 0.0F);
	}

	@Override
	public boolean isCarcass() {
		return this.isCarcass;
	}

	public void setCarcass(boolean carcass) {
		this.isCarcass = carcass;
		if (!this.world.isRemote) {
			this.dataManager.set(WATCHER_IS_CARCASS, this.isCarcass);
		}
		if (carcass) {
			this.setAnimation(EntityAnimation.DYING.get());
			this.carcassHealth = Math.max(1, (int) Math.sqrt(this.width * this.height) * 2);
			this.ticksExisted = 0;
			this.inventory.dropItems(this.world, this.rand);
		}
	}

	@Override
	public boolean processInteract(EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		if (player.isSneaking() && hand == EnumHand.MAIN_HAND) {
			if (this.isOwner(player)) {
				if (this.getAgePercentage() > 75) {
					player.displayGUIChest(this.inventory);
				} else {
					if (this.world.isRemote) {
						player.sendMessage(new TextComponentTranslation("message.too_young.name"));
					}
				}
			} else {
				if (this.world.isRemote) {
					player.sendMessage(new TextComponentTranslation("message.not_owned.name"));
				}
			}
		} else {
			if (stack.isEmpty() && hand == EnumHand.MAIN_HAND && this.world.isRemote) {
				if (this.isOwner(player)) {
					JurassiCraft.PROXY.openOrderGui(this);
				} else {
					player.sendMessage(new TextComponentTranslation("message.not_owned.name"));
				}
			} else if (!stack.isEmpty() && (this.metabolism.isThirsty() || this.metabolism.isHungry())) {
				if (!this.world.isRemote) {
					Item item = stack.getItem();
					boolean fed = false;
					if (item == Items.POTIONITEM) {
						fed = true;
						this.metabolism.increaseWater(1000);
						this.setAnimation(EntityAnimation.DRINKING.get());
					} else if (FoodHelper.isEdible(this, this.dinosaur.diet, item)) {
						fed = true;
						this.metabolism.eat(FoodHelper.getHealAmount(item));
						this.setAnimation(EntityAnimation.EATING.get());
						FoodHelper.applyEatEffects(this, item);
					}
					if (fed) {
						if (!player.capabilities.isCreativeMode) {
							stack.shrink(1);
							if (item == Items.POTIONITEM) {
								player.inventory.addItemStackToInventory(new ItemStack(Items.GLASS_BOTTLE));
							}
						}
						if (!this.isOwner(player)) {
							if (this.rand.nextFloat() < 0.30) {
								if (this.dinosaur.dinosaurBehaviourType == Dinosaur.DinosaurBehaviourType.AGGRESSIVE) {
									if (this.rand.nextFloat() * 4.0F < (float) this.herd.members.size() / this.dinosaur.maxHerdSize) {
										this.herd.enemies.add(player);
									} else {
										this.attackEntityAsMob(player);
									}
								} else if (this.dinosaur.dinosaurBehaviourType == Dinosaur.DinosaurBehaviourType.SCARED) {
									this.herd.fleeing = true;
									this.herd.enemies.add(player);
								}
							}
						}
						player.swingArm(hand);
					}
				}
			}
		}

		return false;
	}

	public boolean isOwner(EntityPlayer player) {
		return player.getUniqueID().equals(this.getOwner());
	}

	@Override
	public boolean canBeLeashedTo(EntityPlayer player) {
		return !this.getLeashed() && (this.width < 1.5) && canBeLeashedToUntamed(this);
	}

	public boolean canBeLeashedToUntamed(DinosaurEntity dinosaur){
		return this.dinosaur.possibleToLeashUntamed && JurassiCraftConfig.ENTITIES.canBeLeashedUntamed;
	}

	public int getDNAQuality() {
		return this.geneticsQuality;
	}

	public void setDNAQuality(int quality) {
		this.geneticsQuality = quality;
	}

	@Override
	public Animation[] getAnimations() {
		return EntityAnimation.getAnimations();
	}

	@Override
	public Animation getAnimation() {
		return this.animation;
	}

	@Override
	public void setAnimation(Animation newAnimation) {
		if (this.isSleeping()) {
			newAnimation = EntityAnimation.SLEEPING.get();
		}

		if (this.isCarcass()) {
			newAnimation = EntityAnimation.DYING.get();
		}

		Animation oldAnimation = this.animation;

		this.animation = newAnimation;

		if (oldAnimation != newAnimation) {
			this.animationTick = 0;
			this.animationLength = (int) this.dinosaur.poseHandler.getAnimationLength(this.animation, this.getGrowthStage());

			AnimationHandler.INSTANCE.sendAnimationMessage(this, newAnimation);
		}
	}

	public LegSolver getLegSolver() {
		return legSolver;
	}

	@Override
	public int getAnimationTick() {
		return this.animationTick;
	}

	@Override
	public void setAnimationTick(int tick) {
		this.animationTick = tick;
	}

	public boolean isBusy() {
		return !this.isAlive() ||
				this.getAnimation() == EntityAnimation.IDLE.get() ||
				(this.herd != null && this.herd.isBusy()) ||
				this.getAttackTarget() != null ||
				this.isSwimming() ||
				this.shouldSleep();
	}

	public boolean isAlive() {
		return !this.isCarcass && !this.isDead;
	}

	@Override
	public SoundEvent getAmbientSound() {
		return this.getSoundForAnimation(EntityAnimation.SPEAK.get());
	}


	@Override
	public SoundEvent getHurtSound(DamageSource damageSourceIn) {
		return this.getSoundForAnimation(EntityAnimation.INJURED.get());
	}

	@Override
	public SoundEvent getDeathSound() {
		return this.getSoundForAnimation(EntityAnimation.DYING.get());
	}

	@Nullable
	public SoundEvent getSoundForAnimation(Animation animation) {
		Map<EntityAnimation, SoundEvent> sounds = this.dinosaur.sounds;
		return sounds == null ? null : sounds.get(EntityAnimation.getAnimation(animation));
	
	}

	public SoundEvent getBreathingSound() {
		return null;
	}

	public double getAttackDamage()  {
		return this.interpolate(this.dinosaur.properties.statistics.strength);
	}

	public boolean isMale() {
		return this.isMale;
	}

	public void setMale(boolean male) {
		this.isMale = male;
	}

	public boolean isPregnant() {
		return !this.isMale() && this.pregnantTime > 0;
	}

	public int getAgePercentage() {
		int age = this.getDinosaurAge();
		if (this.getDinosaur().maximumAge == 0) {
//			System.out.println(this.getDNA().getValueFloat(GeneType.LIFESPAN));
		}
		return (int) (age != 0 ? age * 100 / (this.getDinosaur().maximumAge) : 0);
	}

	@Override
	public GrowthStage getGrowthStage() {

		int percent = this.getAgePercentage();
		if (this.isSkeleton) {
			return GrowthStage.SKELETON;
		}
		return percent > 75 ? GrowthStage.ADULT : percent > 50 ? GrowthStage.ADOLESCENT : percent > 25 ? GrowthStage.JUVENILE : GrowthStage.INFANT;
	}

	public void increaseGrowthSpeed() {
		this.growthSpeedOffset += 240;
	}

	public int getBreedCooldown() {
		return this.breedCooldown;
	}

	public void breed(DinosaurEntity partner) {
		this.breeding = partner;
	}

	@Override
	public boolean isSwimming() {
		return (this.isInWater() || this.inLava()) && !this.onGround;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt = super.writeToNBT(nbt);

		nbt.setString("Dinosaur", this.dinosaur.getRegistryName().toString());
		nbt.setInteger("DinosaurAge", this.dinosaurAge);
		nbt.setBoolean("IsCarcass", this.isCarcass);
		nbt.setInteger("DNAQuality", this.geneticsQuality);
		nbt.setString("Genetics", this.genetics);
		nbt.setBoolean("IsMale", this.isMale);
		nbt.setInteger("GrowthSpeedOffset", this.growthSpeedOffset);
		nbt.setInteger("StayAwakeTime", this.stayAwakeTime);
		nbt.setBoolean("IsSleeping", this.isSleeping);
		nbt.setByte("Order", (byte) this.order.ordinal());
		nbt.setInteger("CarcassHealth", this.carcassHealth);
		nbt.setInteger("BreedCooldown", this.breedCooldown);
		nbt.setInteger("PregnantTime", this.pregnantTime);

		this.metabolism.writeToNBT(nbt);

		if (this.owner != null) {
			nbt.setString("OwnerUUID", this.owner.toString());
		}

		this.inventory.writeToNBT(nbt);

		if (this.family != null && (this.family.getHead() == null || this.family.getHead().equals(this.getUniqueID()))) {
			NBTTagCompound familyTag = new NBTTagCompound();
			this.family.writeToNBT(familyTag);
			nbt.setTag("Family", familyTag);
		}

		NBTTagList relationshipList = new NBTTagList();

		for (Relationship relationship : this.relationships) {
			NBTTagCompound compound = new NBTTagCompound();
			relationship.writeToNBT(compound);
			relationshipList.appendTag(compound);
		}

		nbt.setTag("Relationships", relationshipList);

		NBTTagCompound attributes = new NBTTagCompound();
		this.attributes.writeToNBT(attributes);
		nbt.setTag("GeneticAttributes", attributes);

		if (this.children.size() > 0) {
			NBTTagList children = new NBTTagList();
			for (DinosaurEntity child : this.children) {
				if (child != null) {
					children.appendTag(child.writeToNBT(new NBTTagCompound()));
				}
			}
			nbt.setTag("Children", children);
		}

		nbt.setInteger("TranquilizerTicks", tranquilizerTicks);
		nbt.setInteger("TicksUntilDeath", ticksUntilDeath);
		if (entityInMouth != null) {
			nbt.setUniqueId("EntityInMouth", entityInMouth);
		}
		if (inMouthEntity != null) {
			nbt.setUniqueId("InMouthEntity", inMouthEntity);
		}
		
		NBTTagList trackerList = new NBTTagList();

		for (String tracker : this.trackers) {
			NBTTagCompound compound = new NBTTagCompound();
			compound.setString("ID", tracker);
			trackerList.appendTag(compound);
		}
		nbt.setTag("Trackers", trackerList);
		return nbt;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		this.deserializing = true;

		super.readFromNBT(nbt);
		this.setDinosaur(JurassicraftRegisteries.DINOSAUR_REGISTRY.getValue(new ResourceLocation(nbt.getString("Dinosaur"))));

		this.setAge(nbt.getInteger("DinosaurAge"));
		this.setCarcass(nbt.getBoolean("IsCarcass"));
		this.geneticsQuality = nbt.getInteger("DNAQuality");
		this.genetics = nbt.getString("Genetics");
		this.isMale = nbt.getBoolean("IsMale");
		this.growthSpeedOffset = nbt.getInteger("GrowthSpeedOffset");
		this.stayAwakeTime = nbt.getInteger("StayAwakeTime");
		this.setSleeping(nbt.getBoolean("IsSleeping"));
		this.carcassHealth = nbt.getInteger("CarcassHealth");
		this.order = Order.values()[nbt.getByte("Order")];
		this.breedCooldown = nbt.getInteger("BreedCooldown");
		this.pregnantTime = nbt.getInteger("PregnantTime");

		this.metabolism.readFromNBT(nbt);

		String ownerUUID = nbt.getString("OwnerUUID");

		if (ownerUUID.length() > 0) {
			this.owner = UUID.fromString(ownerUUID);
		}

		if (nbt.hasKey("Family")) {
			NBTTagCompound familyTag = nbt.getCompoundTag("Family");
			this.family = Family.readFromNBT(familyTag);
		}

		this.inventory.readFromNBT(nbt);

		NBTTagList relationships = nbt.getTagList("Relationships", Constants.NBT.TAG_COMPOUND);

		for (int i = 0; i < relationships.tagCount(); i++) {
			NBTTagCompound compound = relationships.getCompoundTagAt(i);
			this.relationships.add(Relationship.readFromNBT(compound));
		}

		if (nbt.hasKey("GeneticAttributes")) {
			NBTTagCompound attributes = nbt.getCompoundTag("GeneticAttributes");
			this.attributes = DinosaurAttributes.from(attributes);
		}

		if (nbt.hasKey("Children")) {
			NBTTagList children = nbt.getTagList("Children", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < children.tagCount(); i++) {
				NBTTagCompound childTag = children.getCompoundTagAt(i);
				Entity entity = EntityList.createEntityFromNBT(childTag, this.world);
				if (entity instanceof DinosaurEntity) {
					this.children.add((DinosaurEntity) entity);
				}
			}
		}

		this.tranquilizerTicks = nbt.getInteger("TranquilizerTicks");
		this.ticksUntilDeath = nbt.getInteger("TicksUntilDeath");
		this.entityInMouth = nbt.hasUniqueId("EntityInMouth") ? nbt.getUniqueId("EntityInMouth") : null;
		this.inMouthEntity = nbt.hasUniqueId("InMouthEntity") ? nbt.getUniqueId("InMouthEntity") : null;

		this.updateBounds();
		
		NBTTagList trackers = nbt.getTagList("Trackers", Constants.NBT.TAG_COMPOUND);

		for (int i = 0; i < trackers.tagCount(); i++) {
			NBTTagCompound compound = trackers.getCompoundTagAt(i);
			this.trackers.add(compound.getString("ID"));
		}

		this.deserializing = false;
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		buffer.writeInt(this.dinosaurAge);
		buffer.writeBoolean(this.isCarcass);
		buffer.writeInt(this.geneticsQuality);
		buffer.writeBoolean(this.isMale);
		buffer.writeInt(this.growthSpeedOffset);
		this.attributes.write(buffer);
		buffer.writeBoolean(this.dinosaur != null);
		if (this.dinosaur != null) {
			ByteBufUtils.writeRegistryEntry(buffer, this.dinosaur);
		}
	}

	@Override
	public void readSpawnData(ByteBuf additionalData) {
		this.dinosaurAge = additionalData.readInt();
		this.isCarcass = additionalData.readBoolean();
		this.geneticsQuality = additionalData.readInt();
		this.isMale = additionalData.readBoolean();
		this.growthSpeedOffset = additionalData.readInt();
		this.attributes = DinosaurAttributes.from(additionalData);
		if (additionalData.readBoolean()) {
			this.setDinosaur(ByteBufUtils.readRegistryEntry(additionalData, JurassicraftRegisteries.DINOSAUR_REGISTRY));
		}
		if (this.isCarcass) {
			this.setAnimation(EntityAnimation.DYING.get());
		}

		this.legSolver = createLegSolver();
		this.updateBounds();
	}

	public MetabolismContainer getMetabolism() {
		return this.metabolism;
	}

	public boolean setSleepLocation(BlockPos sleepLocation, boolean moveTo) {
		return !moveTo || this.getNavigator().tryMoveToXYZ(sleepLocation.getX(), sleepLocation.getY(), sleepLocation.getZ(), 1.0);
	}

	@Override
	public boolean isSleeping() {
		return this.isSleeping;
	}

	public void setSleeping(boolean sleeping) {
		this.isSleeping = sleeping;
		if (!this.world.isRemote) {
			this.dataManager.set(WATCHER_IS_SLEEPING, this.isSleeping);
		}
	}

	public void tranquilize(int ticks) {
		tranquilizerTicks = 50 + ticks + this.rand.nextInt(50);
		setSleeping(true);
	}

	public int getStayAwakeTime() {
		return this.stayAwakeTime;
	}

	public void disturbSleep() {
		if (tranquilizerTicks == 0) {
			this.isSleeping = false;
			this.stayAwakeTime = 400;
		}
	}

	public void writeStatsToLog() {
		LOGGER.info(this);
	}

	@Override
	public String toString() {
		return "DinosaurEntity{ " +
				this.dinosaur.name +
				", id=" + this.getEntityId() +
				", remote=" + this.getEntityWorld().isRemote +
				", isDead=" + this.isDead +
				", isCarcass=" + this.isCarcass +
				", isSleeping=" + this.isSleeping +
				", stayAwakeTime=" + this.stayAwakeTime +
				"\n    " +
				", dinosaurAge=" + this.dinosaurAge +
				", prevAge=" + this.prevAge +
				", maxAge" + this.dinosaur.maximumAge +
				", ticksExisted=" + this.ticksExisted +
				", entityAge=" + this.idleTime +
				", isMale=" + this.isMale +
				", growthSpeedOffset=" + this.growthSpeedOffset +
				"\n    " +
				", food=" + this.metabolism.getEnergy() + " / " + this.metabolism.getMaxEnergy() + " (" + this.metabolism.getMaxEnergy() * 0.875 + ")" +
				", water=" + this.metabolism.getWater() + " / " + this.metabolism.getMaxWater() + " (" + this.metabolism.getMaxWater() * 0.875 + ")" +
				", digestingFood=" + this.metabolism.getDigestingFood() + " / " + MetabolismContainer.MAX_DIGESTION_AMOUNT +
				", health=" + this.getHealth() + " / " + this.getMaxHealth() +
				"\n    " +
				", pos=" + this.getPosition() +
				", eyePos=" + this.getHeadPos() +
				", eyeHeight=" + this.getEyeHeight() +
				", lookX=" + this.getLookHelper().getLookPosX() + ", lookY=" + this.getLookHelper().getLookPosY() + ", lookZ=" + this.getLookHelper().getLookPosZ() +
				"\n    " +
				", width=" + this.width +
				", bb=" + this.getEntityBoundingBox() +
//                "\n    " +
//                ", anim=" + animation + (animation != null ? ", duration" + animation.duration : "" ) +

//                "dinosaur=" + dinosaur +
//                ", genetics=" + genetics +
//                ", geneticsQuality=" + geneticsQuality +
//                ", currentAnim=" + currentAnim +
//                ", animation=" + animation +
//                ", animTick=" + animTick +
//                ", hasTracker=" + hasTracker +
//                ", tailBuffer=" + tailBuffer +
//                ", owner=" + owner +
//                ", inventory=" + inventory +
//                ", metabolism=" + metabolism +
				" }";
	}

	public Vec3d getHeadPos() {
		double scale = this.interpolate(this.dinosaur.properties.statistics.scale);

		double[] headPos = this.dinosaur.getHeadPosition(this.getGrowthStage(), ((360 - this.rotationYawHead)) % 360 - 180);

		double headX = ((headPos[0] * 0.0625F) - this.dinosaur.offsetX) * scale;
		double headY = (((24 - headPos[1]) * 0.0625F) - this.dinosaur.offsetY) * scale;
		double headZ = ((headPos[2] * 0.0625F) - this.dinosaur.offsetZ) * scale;

		return new Vec3d(this.posX + headX, this.posY + (headY), this.posZ + headZ);
	}

	public boolean areEyelidsClosed() {
		return this.dinosaur.homeType != Dinosaur.DinosaurHomeType.MARINE && ((this.isCarcass || this.isSleeping) || this.ticksExisted % 100 < 4);
	}

	@Override
	public boolean shouldUseInertia() {
		return this.useInertialTweens;
	}

	public void setUseInertialTweens(boolean parUseInertialTweens) {
		this.useInertialTweens = parUseInertialTweens;
	}

	@Override
	public ItemStack getPickedResult(RayTraceResult target) {
		return ItemHandler.SPAWN_EGG.getItemStack(this.dinosaur);
	}

	@Override
	protected float getWaterSlowDown() {
		return 0.9F;
	}

	@Override
	public void moveRelative(float strafe, float up, float forward, float friction) {
		if (this.inWater() && !this.canDinoSwim()) {
			friction *= 10;//times by 5, but as friction is divided by 2 when in water do 5 * 2 instead
		}
		super.moveRelative(strafe, up, forward, friction);
	}

	public void giveBirth() {
		pregnantTime = 1;
	}

	public void setDeathIn(int ticks) { // :(
		this.ticksUntilDeath = ticks;
		this.addPotionEffect(new PotionEffect(MobEffects.POISON, ticks));
	}

	@Override
	public void collideWithEntity(Entity entity) {
		super.collideWithEntity(entity);

		if (this.isSleeping && !this.isRidingSameEntity(entity)) {
			if (!entity.noClip && !this.noClip) {
				if (!(entity instanceof DinosaurEntity) || ((DinosaurEntity) entity).dinosaur != this.dinosaur) {
					this.disturbSleep();
				}
			}
		}
	}

	@Override
	protected void setSize(float width, float height) {
		if (width != this.width || height != this.height) {
			float prevWidth = this.width;
			this.width = width;
			this.height = height;
			if (!this.deserializing) {
				AxisAlignedBB bounds = this.getEntityBoundingBox();
				AxisAlignedBB newBounds = new AxisAlignedBB(bounds.minX, bounds.minY, bounds.minZ, bounds.minX + this.width, bounds.minY + this.height, bounds.minZ + this.width);
				if (!this.world.collidesWithAnyBlock(newBounds)) {
					this.setEntityBoundingBox(newBounds);
					if (this.width > prevWidth && !this.firstUpdate && !this.world.isRemote) {
						this.move(MoverType.SELF, prevWidth - this.width, 0.0F, prevWidth - this.width);
					}
				}
			} else {
				float halfWidth = this.width / 2.0F;
				this.setEntityBoundingBox(new AxisAlignedBB(this.posX - halfWidth, this.posY, this.posZ - halfWidth, this.posX + halfWidth, this.posY + this.height, this.posZ + halfWidth));
			}
		}
	}

	public Order getOrder() {
		return this.order;
	}

	public void setOrder(Order order) {
		this.order = order;

		if (this.world.isRemote) {
			if (this.owner != null) {
				EntityPlayer player = this.world.getPlayerEntityByUUID(this.owner);

				if (player != null) {
					player.sendMessage(new TextComponentString(new LangHelper("message.set_order.name").withProperty("order", "order." + order.name().toLowerCase(Locale.ENGLISH) + ".name").build()));
				}
			}

			JurassiCraft.NETWORK_WRAPPER.sendToServer(new SetOrderMessage(this));
		}
	}

	@SafeVarargs
	public final void target(Predicate<Entity>... predicates) {
		this.attackPredicate = entity -> {
			for (Predicate<Entity> predicate : predicates) {
				if (predicate.test(entity)) {
					return true;
				}
			}
			return false;
		};
		this.targetTasks.addTask(1, new SelectTargetEntityAI(this, this.attackPredicate));

	}

	public EntityAIBase getAttackAI() {//this.dinosaur.getAttackSpeed()
		return new DinosaurAttackMeleeEntityAI(this, 1f, false);
	}

	@SideOnly(Side.CLIENT)
	public void forceRender() {
		this.forceRender = true;
	}

	@SideOnly(Side.CLIENT)
	public boolean shouldForceRender() {
		if (this.forceRender) {
			this.forceRender = false;
			return true;
		}
		return false;
	}

	public Predicate<Entity> getAttackPredicate() {
		return attackPredicate;
	}

	@Override
	public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, IEntityLivingData data) {
		this.metabolism.setEnergy(this.metabolism.getMaxEnergy());
		this.metabolism.setWater(this.metabolism.getMaxWater());
		this.genetics = GeneticsHelper.randomGenetics(this.rand);
		this.setFullyGrown();
		this.setMale(this.rand.nextBoolean());
		this.setDNAQuality(100);
		return data;
	}

	public int getAttackCooldown() {
		return this.attackCooldown;
	}

	public void resetAttackCooldown() {
		this.attackCooldown = 50 + this.getRNG().nextInt(20);
	}

	public void respondToAttack(EntityLivingBase attacker) {
		if (attacker != null && !attacker.isDead && !(attacker instanceof EntityPlayer && ((EntityPlayer) attacker).capabilities.isCreativeMode)) {
			List<EntityLivingBase> enemies = new LinkedList<>();

			if (attacker instanceof DinosaurEntity) {
				DinosaurEntity enemyDinosaur = (DinosaurEntity) attacker;

				if (enemyDinosaur.herd != null) {
					enemies.addAll(enemyDinosaur.herd.members);
				}
			} else {
				enemies.add(attacker);
			}

			if (enemies.size() > 0) {
				Herd herd = this.herd;

				if (herd != null) {
					herd.fleeing = !herd.shouldDefend(enemies) || this.dinosaur.flee;

					for (EntityLivingBase entity : enemies) {
						if (!herd.enemies.contains(entity)) {
							herd.enemies.add(entity);
						}
					}
				} else {
					this.setAttackTarget(enemies.get(this.getRNG().nextInt(enemies.size())));
				}
			}
		}
	}

	public int getAnimationLength() {
		return this.animationLength;
	}

	@Override
	public boolean isRunning() {
		return this.dataManager.get(WATCHER_IS_RUNNING);
	}

	@Override
	public boolean getCanSpawnHere() {
		return this.dimension == 0;
	}

	public boolean shouldEscapeWaterFast() {
		return true;
	}

	public BlockPos getClosestFeeder() {
		if (this.ticksExisted - this.feederSearchTick > 200) {
			this.feederSearchTick = this.ticksExisted;
			OnionTraverser traverser = new OnionTraverser(this.getPosition(), 32);
			for (BlockPos pos : traverser) {
				IBlockState state = this.world.getBlockState(pos);
				if (state.getBlock() instanceof FeederBlock) {
					TileEntity tile = this.world.getTileEntity(pos);
					if (tile instanceof FeederBlockEntity) {
						FeederBlockEntity feeder = (FeederBlockEntity) tile;
						if (feeder.canFeedDinosaur(this) && feeder.getFeeding() == null && feeder.openAnimation == 0) {
							Path path = this.getNavigator().getPathToPos(pos);
							if (path != null && path.getCurrentPathLength() != 0) {
								return this.closestFeeder = pos;
							}
						}
					}
				}
			}
		}
		return this.closestFeeder;
	}

	@Override
	public boolean isClimbing() {
		return false;
	}

	@Override
	public boolean isMoving() {
		float deltaX = (float) (this.posX - this.prevPosX);
		float deltaZ = (float) (this.posZ - this.prevPosZ);
		return deltaX * deltaX + deltaZ * deltaZ > 0.001F;
	}

	@Override
	public boolean canUseGrowthStage(GrowthStage growthStage) {
		return this.dinosaur.doesSupportGrowthStage(growthStage);
	}

	@Override
	public boolean isMarineCreature() {
		return this.dinosaur.homeType == Dinosaur.DinosaurHomeType.MARINE;
	}

	@Override
	public <ENTITY extends EntityLivingBase & Animatable> PoseHandler<ENTITY> getPoseHandler() {
		return (PoseHandler<ENTITY>) this.dinosaur.poseHandler;
	}

	@Override
	public boolean inWater() {
		return this.isInWater();
	}

	@Override
	public boolean inLava() {
		return this.inLava;
	}

	public DinosaurAttributes getAttributes() {
		return this.attributes;
	}

	public void setAttributes(DinosaurAttributes attributes) {
		this.attributes = attributes;
	}

	public boolean isBreeding() {
		return this.breeding != null;
	}

	public void setJumpHeight(int jumpHeight) {
		this.jumpHeight = jumpHeight;
	}

	@Override
	protected float getJumpUpwardsMotion() {
		return (float) Math.sqrt((this.jumpHeight + 0.2) * 0.27);
	}

	public boolean isSkeleton() {
		return this.getGrowthStage() == GrowthStage.SKELETON;
	}

	public void setSkeleton(boolean isSkeleton) {
		this.isSkeleton = isSkeleton;
	}

	public boolean canDinoSwim() {
		return this.getGrowthStage() != GrowthStage.INFANT && this.getGrowthStage() != GrowthStage.JUVENILE;
	}

	public Vector3f getDinosaurCultivatorRotation() {
		return new Vector3f();
	}

	@Override
	protected void handleJumpWater() {
		if (this.canDinoSwim()) {
			super.handleJumpWater();
		}
	}
	
    public void sendTrackingPacket(EntityPlayer player, boolean success) {
		
		if(player != null && !player.world.isRemote)
		JurassiCraft.NETWORK_WRAPPER.sendTo(new TrackedAddMessage(this, success), (EntityPlayerMP) player);  
	}

	public enum Order {
		WANDER, FOLLOW, SIT
	}

	public static class FieldGuideInfo {
		public int hunger;
		public int thirst;
		public boolean flocking;
		public boolean scared;
		public boolean hungry;
		public boolean thirsty;
		public boolean poisoned;

		public static FieldGuideInfo deserialize(ByteBuf buf) {
			FieldGuideInfo info = new FieldGuideInfo();
			info.flocking = buf.readBoolean();
			info.scared = buf.readBoolean();
			info.hunger = buf.readInt();
			info.thirst = buf.readInt();
			info.hungry = buf.readBoolean();
			info.thirsty = buf.readBoolean();
			info.poisoned = buf.readBoolean();
			return info;
		}

		public static FieldGuideInfo serialize(ByteBuf buf, DinosaurEntity entity) {
			MetabolismContainer metabolism = entity.getMetabolism();
			Herd herd = entity.herd;
			FieldGuideInfo info = new FieldGuideInfo();
			info.flocking = herd != null && herd.members.size() > 1 && herd.state == Herd.State.MOVING;
			info.scared = herd != null && herd.fleeing;
			info.hunger = metabolism.getEnergy();
			info.thirst = metabolism.getWater();
			info.hungry = metabolism.isHungry();
			info.thirsty = metabolism.isThirsty();
			info.poisoned = entity.isPotionActive(MobEffects.POISON);
			buf.writeBoolean(info.flocking);
			buf.writeBoolean(info.scared);
			buf.writeInt(info.hunger);
			buf.writeInt(info.thirst);
			buf.writeBoolean(info.hungry);
			buf.writeBoolean(info.thirsty);
			buf.writeBoolean(info.poisoned);
			return info;
		}
	}

	public class DinosaurClassAttackPredicate implements Predicate<Entity> {
		private final List<Class<? extends Entity>> list;

		@SafeVarargs
		public DinosaurClassAttackPredicate(Class<? extends Entity>... list) {
			this.list = Lists.newArrayList(list);
		}

		@Override
		public boolean test(Entity entity) {
			for (Class<? extends Entity> aClass : list) {
				if (aClass.isAssignableFrom(entity.getClass())) {
					return true;
				}
			}
			return false;
		}
	}

	public class DinosaurAttackPredicate implements Predicate<Entity> {
		private final List<Dinosaur> list;

		public DinosaurAttackPredicate(Dinosaur... list) {
			this.list = Lists.newArrayList(list);
		}

		@Override
		public boolean test(Entity entity) {
			return entity instanceof DinosaurEntity && list.contains(((DinosaurEntity) entity).dinosaur);
		}
	}
	
	
}
