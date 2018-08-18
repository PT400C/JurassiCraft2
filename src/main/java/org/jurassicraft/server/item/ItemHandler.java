package org.jurassicraft.server.item;

import com.google.common.collect.Maps;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.oredict.OreDictionary;
import org.jurassicraft.client.sound.SoundHandler;
import org.jurassicraft.server.block.BlockHandler;
import org.jurassicraft.server.block.NestFossilBlock;
import org.jurassicraft.server.block.tree.TreeType;
import org.jurassicraft.server.entity.DinosaurEntity;
import org.jurassicraft.server.entity.item.AttractionSignEntity;
import org.jurassicraft.server.tab.TabHandler;
import org.jurassicraft.server.util.RegistryHandler;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ItemHandler {

    private static CreativeTabs itemTab = TabHandler.ITEMS;
    private static CreativeTabs foodTab = TabHandler.FOODS;
    private static CreativeTabs dnaTab = TabHandler.DNA;
    private static CreativeTabs decoTab = TabHandler.DECORATIONS;
    private static CreativeTabs fossilTab = TabHandler.FOSSILS;
    private static CreativeTabs plantTab = TabHandler.PLANTS;

    public static final Map<TreeType, ItemDoor> ANCIENT_DOORS = new HashMap<>();

    public static final PlasterAndBandageItem PLASTER_AND_BANDAGE = new PlasterAndBandageItem();
    public static final DinosaurSpawnEggItem SPAWN_EGG = new DinosaurSpawnEggItem();

    public static final DNAItem DNA = new DNAItem();
    public static final DinosaurEggItem EGG = new DinosaurEggItem();
    public static final HatchedEggItem HATCHED_EGG = new HatchedEggItem();
    public static final SoftTissueItem SOFT_TISSUE = new SoftTissueItem();
    public static final PlantSoftTissueItem PLANT_SOFT_TISSUE = new PlantSoftTissueItem();

    public static final DinosaurMeatItem DINOSAUR_MEAT = new DinosaurMeatItem();
    public static final DinosaurSteakItem DINOSAUR_STEAK = new DinosaurSteakItem();

    public static final PaddockSignItem PADDOCK_SIGN = new PaddockSignItem();
    public static final Map<AttractionSignEntity.AttractionSignType, AttractionSignItem> ATTRACTION_SIGN = Maps.newHashMap();

    public static final Map<AmberItem.AmberStorageType, AmberItem> AMBER = Maps.newHashMap();
    public static final Item PETRI_DISH = new Item();
    public static final Item PETRI_DISH_AGAR = new Item();
    public static final Item EMPTY_TEST_TUBE = new Item();

    public static final SyringeItem SYRINGE = new SyringeItem();
    public static final EmptySyringeItem EMPTY_SYRINGE = new EmptySyringeItem();

    public static final StorageDiscItem STORAGE_DISC = new StorageDiscItem();
    public static final Item DNA_NUCLEOTIDES = new Item();

    public static final PlantDNAItem PLANT_DNA = new PlantDNAItem();

    public static final Item SEA_LAMPREY = new Item();

    public static final Item IRON_BLADES = new Item();
    public static final Item IRON_ROD = new Item();
    public static final Item DISC_DRIVE = new Item();
    public static final Item LASER = new Item();

    public static final Item GROWTH_SERUM = new EntityRightClickItem(interaction -> {
	if (interaction.getTarget() instanceof DinosaurEntity) {
            DinosaurEntity dinosaur = (DinosaurEntity) interaction.getTarget();
            if (!dinosaur.isCarcass()) {
                dinosaur.setFullyGrown();
                interaction.getStack().shrink(1);
                if (!interaction.getPlayer().capabilities.isCreativeMode) {
                    interaction.getPlayer().inventory.addItemStackToInventory(new ItemStack(ItemHandler.EMPTY_SYRINGE));
                }
                return true;
            }
        }
	return false;
    }).setCreativeTab(TabHandler.ITEMS);
    
    public static final Item BREEDING_WAND = new EntityRightClickItem(interaction -> {
        ItemStack stack = interaction.getPlayer().getHeldItem(interaction.getHand());
        NBTTagCompound nbt = stack.getOrCreateSubCompound("wand_info");
        if(nbt.hasKey("dino_id", 99)) {
            Entity entity = interaction.getPlayer().world.getEntityByID(nbt.getInteger("dino_id"));
            if(entity instanceof DinosaurEntity && ((DinosaurEntity)entity).isMale() != ((DinosaurEntity)interaction.getTarget()).isMale()) {
            ((DinosaurEntity)entity).breed((DinosaurEntity)interaction.getTarget());
            ((DinosaurEntity)interaction.getTarget()).breed((DinosaurEntity)entity);
            } else if(entity != interaction.getTarget()) {
            nbt.removeTag("dino_id");
            }
            return true;
        } else if(interaction.getTarget() instanceof DinosaurEntity) {
            nbt.setInteger("dino_id", interaction.getTarget().getEntityId());
            return true;
        }
        return false;
    }).setCreativeTab(TabHandler.ITEMS);
    
    public static final Item BIRTHING_WAND = new EntityRightClickItem(interaction -> {
        if(interaction.getTarget() instanceof DinosaurEntity) {
            ((DinosaurEntity)interaction.getTarget()).giveBirth();
            return true;
        }
        return false;
    });

    public static final Item PREGNANCY_TEST = new EntityRightClickItem(interaction -> {
        if(interaction.getTarget() instanceof DinosaurEntity && !interaction.getPlayer().world.isRemote) {
            DinosaurEntity dino = ((DinosaurEntity)interaction.getTarget());
            interaction.getPlayer().sendStatusMessage(new TextComponentTranslation("dinosaur.pregnancytest." + (dino.isMale() ? "male" : dino.isPregnant() ? "pregnant" : "not_pregnant")), false);
            return true;
        }
        return false;
    });

    public static final Item PLANT_CELLS = new Item();
    public static final PlantCallusItem PLANT_CALLUS = new PlantCallusItem();
    public static final Item PLANT_CELLS_PETRI_DISH = new Item();

    public static final Item TRACKER = new Item();

    public static final ItemRecord JURASSICRAFT_THEME_DISC = new AncientRecordItem("jurassicraft_theme", SoundHandler.JURASSICRAFT_THEME);
    public static final ItemRecord TROODONS_AND_RAPTORS_DISC = new AncientRecordItem("troodons_and_raptors", SoundHandler.TROODONS_AND_RAPTORS);
    public static final ItemRecord DONT_MOVE_A_MUSCLE_DISC = new AncientRecordItem("dont_move_a_muscle", SoundHandler.DONT_MOVE_A_MUSCLE);

    public static final DisplayBlockItem DISPLAY_BLOCK = new DisplayBlockItem();

    public static final Item AMBER_KEYCHAIN = new Item();
    public static final Item AMBER_CANE = new Item();
    public static final Item MR_DNA_KEYCHAIN = new Item();

    public static final Item BASIC_CIRCUIT = new Item();
    public static final Item ADVANCED_CIRCUIT = new Item();

    public static final Item IRON_NUGGET = new Item();
    public static final Item DIAMOND_NUGGET = new Item();

    public static final Item AJUGINUCULA_SMITHII_SEEDS = new ItemSeeds(BlockHandler.AJUGINUCULA_SMITHII, Blocks.FARMLAND).setUnlocalizedName("ajuginucula_smithii_seeds");
    public static final Item AJUGINUCULA_SMITHII_LEAVES = new ItemFood(1, 0.5F, false).setUnlocalizedName("ajuginucula_smithii_leaves");
    public static final Item AJUGINUCULA_SMITHII_OIL = new Item();

    public static final Item WILD_ONION = new ItemSeedFood(3, 0.3F, BlockHandler.WILD_ONION, Blocks.FARMLAND).setUnlocalizedName("wild_onion");
    public static final ItemSeeds WILD_POTATO_SEEDS = new ItemSeeds(BlockHandler.WILD_POTATO_PLANT, Blocks.FARMLAND);
    public static final ItemSeeds RHAMNUS_SEEDS = new ItemSeeds(BlockHandler.RHAMNUS_SALICIFOLIUS_PLANT, Blocks.FARMLAND);
    public static final Item WILD_POTATO = new ItemFood(1, 0.1F, false).setCreativeTab(TabHandler.FOODS);
    public static final Item WILD_POTATO_COOKED = new ItemFood(6, 0.6F, false).setCreativeTab(TabHandler.FOODS);
    public static final RhamnusBerriesItem RHAMNUS_BERRIES = new RhamnusBerriesItem(5, 0.6F, false);
    public static final Item WEST_INDIAN_LILAC_BERRIES = new WestIndianLilacBerriesItem(1, 0.1F, false);

    public static final GracilariaItem GRACILARIA = (GracilariaItem) new GracilariaItem(BlockHandler.GRACILARIA);
    public static final Item LIQUID_AGAR = new Item();

    public static final DinoScannerItem DINO_SCANNER = new DinoScannerItem();

    public static final PlantFossilItem PLANT_FOSSIL = new PlantFossilItem();
    public static final TwigFossilItem TWIG_FOSSIL = new TwigFossilItem();

    public static final FossilItem FOSSIL = new FossilItem(false);
    public static final FossilItem FRESH_FOSSIL = new FossilItem(true);

    public static final Map<NestFossilBlock.Variant, FossilizedEggItem> FOSSILIZED_EGG = Maps.newHashMap();

    public static final Item GYPSUM_POWDER = new Item();

    public static final Item COMPUTER_SCREEN = new Item();
    public static final Item KEYBOARD = new Item();

    public static final Item DNA_ANALYZER = new Item();

    public static final ItemFood CHILEAN_SEA_BASS = new ItemFood(10, 1.0F, false);
    public static final ItemFood FUN_FRIES = new ItemFood(4, 2.0F, false);
    public static final ItemFood OILED_POTATO_STRIPS = new ItemFood(1, 0.0F, false);

    public static final FieldGuideItem FIELD_GUIDE = new FieldGuideItem();

    public static final Item LUNCH_BOX = new Item();
    public static final Item STAMP_SET = new Item();

    public static final Item CAR_CHASSIS = new Item();
    public static final Item CAR_ENGINE_SYSTEM = new Item();
    public static final Item CAR_SEATS = new Item();
    public static final Item CAR_TIRE = new Item();
    public static final Item CAR_WINDSCREEN = new Item();
    public static final Item UNFINISHED_CAR = new Item();

    public static final Map<JournalItem.JournalType, JournalItem> INGEN_JOURNAL = Maps.newHashMap();

    public static final JeepWranglerItem JEEP_WRANGLER = new JeepWranglerItem();
    public static final FordExplorerItem FORD_EXPLORER = new FordExplorerItem();

    public static final MuralItem MURAL = new MuralItem();

    public static final SaplingSeedItem PHOENIX_SEEDS = (SaplingSeedItem) new SaplingSeedItem(BlockHandler.ANCIENT_SAPLINGS.get(TreeType.PHOENIX));
    public static final SeededFruitItem PHOENIX_FRUIT = (SeededFruitItem) new SeededFruitItem(PHOENIX_SEEDS, 4, 0.4F);

    public static final BugItem CRICKETS = new BugItem(stack -> {
        Item item = stack.getItem();
        Block block = Block.getBlockFromItem(item);
        if (item == Items.WHEAT_SEEDS) {
            return 1;
        } else if (block == Blocks.TALLGRASS) {
            return 2;
        } else if (item == Items.WHEAT) {
            return 3;
        } else if (block == Blocks.LEAVES || block == Blocks.LEAVES2) {
            return 7;
        } else if (block == Blocks.HAY_BLOCK) {
            return 27;
        }
        return 0;
    });

    public static final BugItem COCKROACHES = new BugItem(stack -> {
        Item item = stack.getItem();
        Block block = Block.getBlockFromItem(item);
        if (item == Items.WHEAT_SEEDS || item == Items.MELON_SEEDS) {
            return 1;
        } else if (item == Items.WHEAT || item == Items.PUMPKIN_SEEDS) {
            return 2;
        } else if (item == Items.MELON || item == Items.POTATO) {
            return 3;
        } else if (item == Items.CARROT) {
            return 4;
        } else if (item == Items.BREAD || item == Items.FISH) {
            return 6;
        } else if (item == Items.CHICKEN || item == Items.COOKED_CHICKEN) {
            return 7;
        } else if (item == Items.PORKCHOP || item == Items.COOKED_PORKCHOP) {
            return 8;
        } else if (item == Items.BEEF || item == Items.COOKED_BEEF) {
            return 10;
        } else if (item == ItemHandler.DINOSAUR_MEAT || item == ItemHandler.DINOSAUR_STEAK) {
            return 12;
        } else if (block == Blocks.HAY_BLOCK || block == Blocks.PUMPKIN) {
            return 16;
        } else if (block == Blocks.MELON_BLOCK) {
            return 27;
        }
        return 0;
    });

    public static final BugItem MEALWORM_BEETLES = new BugItem(stack -> {
        Item item = stack.getItem();
        Block block = Block.getBlockFromItem(item);
        if (item == Items.WHEAT_SEEDS || item == Items.MELON_SEEDS) {
            return 1;
        } else if (item == Items.PUMPKIN_SEEDS || item == Items.WHEAT) {
            return 2;
        } else if (item == Items.POTATO) {
            return 3;
        } else if (block == Blocks.CARROTS) {
            return 4;
        } else if (item == Items.BREAD) {
            return 6;
        } else if (block == Blocks.HAY_BLOCK) {
            return 16;
        }
        return 0;
    });

    public static final FineNetItem FINE_NET = new FineNetItem();
    public static final SwarmItem PLANKTON = new SwarmItem(BlockHandler.PLANKTON_SWARM::getDefaultState);
    public static final SwarmItem KRILL = new SwarmItem(BlockHandler.KRILL_SWARM::getDefaultState);

    public static final ItemFood GOAT_RAW = new ItemFood(3, 0.3F, true);
    public static final ItemFood GOAT_COOKED = new ItemFood(6, 1.0F, true);
    
    public static final DartGun DART_GUN = new DartGun();
    public static final Dart DART_TRANQUILIZER = new Dart((entity, stack) -> entity.tranquilize(2000), 0xFFFFFF);
    public static final Dart DART_POISON_CYCASIN = new Dart((entity, stack) -> entity.addPotionEffect(new PotionEffect(MobEffects.POISON, 2000)), 0xE2E1B8);
    public static final Dart DART_POISON_EXECUTIONER_CONCOCTION = new Dart((entity, stack) -> entity.setDeathIn(200), 0x000000);
    public static final Dart DART_TIPPED_POTION = new PotionDart();
    public static final Dart TRACKING_DART = new TrackingDart();

    public static final TrackingTablet TRACKING_TABLET = new TrackingTablet(3);
    public static final TrackingTablet TRACKING_TABLET_ADVANCED = new TrackingTablet(2);
    public static final TrackingTablet TRACKING_TABLET_HIGHEND = new TrackingTablet(1);
    public static final TrackingTablet TRACKING_TABLET_FREQUENCY = new FrequencyTrackingTablet();
    public static final Item FREQUENCY_MODULE = new Item();

    static {
        for(AttractionSignEntity.AttractionSignType type : AttractionSignEntity.AttractionSignType.values()) {
            ATTRACTION_SIGN.put(type, new AttractionSignItem(type));
        }
        for(AmberItem.AmberStorageType type : AmberItem.AmberStorageType.values()) {
            AMBER.put(type, new AmberItem(type));
        }
        for (JournalItem.JournalType journalType : JournalItem.JournalType.values()) {
            INGEN_JOURNAL.put(journalType, new JournalItem(journalType));
        }
        for (NestFossilBlock.Variant variant : NestFossilBlock.Variant.values()) {
            FOSSILIZED_EGG.put(variant, new FossilizedEggItem(variant));
        }
    }

    public static void init() {

        for (NestFossilBlock.Variant variant : NestFossilBlock.Variant.values()) {
            registerItem(FOSSILIZED_EGG.get(variant), "Fossilized Egg " + variant.getName(), fossilTab);
        }

        //Fossils
        registerItem(FOSSIL, "Fossil", fossilTab);
        registerItem(FRESH_FOSSIL, "Fresh Fossil", fossilTab);

        //Decorations
        registerItem(PADDOCK_SIGN, "Paddock Sign", decoTab);
        registerItem(MURAL, "Mural", decoTab);
        registerItem(AMBER_CANE, "Amber Cane", decoTab);
        registerItem(AMBER_KEYCHAIN, "Amber Keychain", decoTab);
        registerItem(MR_DNA_KEYCHAIN, "Mr DNA Keychain", decoTab);
        registerItem(DISPLAY_BLOCK, "Display Block Item", decoTab);
        for (AttractionSignEntity.AttractionSignType type : AttractionSignEntity.AttractionSignType.values()) {
            registerItem(ATTRACTION_SIGN.get(type), "Attraction Sign " + type.name(), decoTab);
        }

        //Food
        registerItem(DINOSAUR_MEAT, "Dinosaur Meat", foodTab);
        registerItem(DINOSAUR_STEAK, "Dinosaur Steak", foodTab);
        registerItem(WILD_POTATO_COOKED, "Wild Potato Cooked", foodTab);
        registerItem(RHAMNUS_BERRIES, "Rhamnus Salicifolius Berries", foodTab);
        registerItem(CHILEAN_SEA_BASS, "Chilean Sea Bass", foodTab);
        registerItem(PHOENIX_FRUIT, "Phoenix Fruit", foodTab);
        registerItem(GOAT_RAW, "Goat Raw", foodTab);
        registerItem(GOAT_COOKED, "Goat Cooked", foodTab);
        registerItem(FUN_FRIES, "Fun Fries", foodTab);
        registerItem(OILED_POTATO_STRIPS, "Oiled Potato Strips", foodTab);
        registerItem(WEST_INDIAN_LILAC_BERRIES, "West Indian Lilac Berries", foodTab);

        //DNA
        registerItem(PLANT_SOFT_TISSUE, "Plant Soft Tissue", null);

        registerItem(DNA, "DNA", dnaTab);
        registerItem(SOFT_TISSUE, "Soft Tissue", dnaTab);
        registerItem(SYRINGE, "Syringe", dnaTab);
        registerItem(EGG, "Dino Egg", dnaTab);
        registerItem(HATCHED_EGG, "Hatched Egg", dnaTab);

        //Plants
        registerItem(PLANT_CALLUS, "Plant Callus", plantTab);
        registerItem(PLANT_DNA, "Plant DNA", plantTab);
        registerItem(PLANT_FOSSIL, "Plant Fossil", plantTab);
        registerItem(TWIG_FOSSIL, "Twig Fossil", plantTab);
        registerItem(AJUGINUCULA_SMITHII_SEEDS, "Ajuginucula Smithii Seeds", plantTab);
        registerItem(AJUGINUCULA_SMITHII_LEAVES, "Ajuginucula Smithii Leaves", plantTab);
        registerItem(AJUGINUCULA_SMITHII_OIL, "Ajuginucula Smithii Oil", plantTab);
        registerItem(WILD_ONION, "Wild Onion", plantTab);
        registerItem(WILD_POTATO_SEEDS, "Wild Potato Seeds", plantTab);
        registerItem(WILD_POTATO, "Wild Potato", plantTab);
        registerItem(RHAMNUS_SEEDS, "Rhamnus Salicifolius Seeds", plantTab);
        registerItem(GRACILARIA, "Gracilaria", plantTab);
        registerItem(LIQUID_AGAR, "Liquid Agar", plantTab);
        registerItem(PHOENIX_SEEDS, "Phoenix Seeds", plantTab);

        //General Items
        registerItem(SPAWN_EGG, "Dino Spawn Egg", itemTab);
        registerItem(FIELD_GUIDE, "Field Guide", itemTab);
        registerItem(SEA_LAMPREY, "Sea Lamprey", itemTab);
        registerItem(PLASTER_AND_BANDAGE, "Plaster And Bandage", itemTab);
        registerItem(EMPTY_TEST_TUBE, "Empty Test Tube", itemTab);
        registerItem(EMPTY_SYRINGE, "Empty Syringe", itemTab);
        registerItem(GROWTH_SERUM, "Growth Serum", itemTab);
        registerItem(BREEDING_WAND, "Breeding Wand", itemTab);
        registerItem(BIRTHING_WAND, "Birthing_Wand", itemTab);
        registerItem(PREGNANCY_TEST, "Pregnancy Test", itemTab);
        registerItem(STORAGE_DISC, "Storage Disc", itemTab);
        registerItem(DISC_DRIVE, "Disc Reader", itemTab);
        registerItem(LASER, "Laser", itemTab);
        registerItem(DNA_NUCLEOTIDES, "DNA Base Material", itemTab);
        registerItem(PETRI_DISH, "Petri Dish", itemTab);
        registerItem(PETRI_DISH_AGAR, "Petri Dish Agar", itemTab);
        registerItem(PLANT_CELLS_PETRI_DISH, "Plant Cells Petri Dish", itemTab);
        registerItem(IRON_BLADES, "Iron Blades", itemTab);
        registerItem(IRON_ROD, "Iron Rod", itemTab);
        registerItem(PLANT_CELLS, "Plant Cells", itemTab);
        registerItem(TRACKER, "Tracker", itemTab);
        registerItem(BASIC_CIRCUIT, "Basic Circuit", itemTab);
        registerItem(ADVANCED_CIRCUIT, "Advanced Circuit", itemTab);
        registerItem(IRON_NUGGET, "Iron Nugget", itemTab);
        registerItem(DIAMOND_NUGGET, "Diamond Nugget", itemTab);
        registerItem(COMPUTER_SCREEN, "Computer Screen", itemTab);
        registerItem(KEYBOARD, "Keyboard", itemTab);
        registerItem(DNA_ANALYZER, "DNA Analyzer", itemTab);
        registerItem(DINO_SCANNER, "Dino Scanner", itemTab);
        registerItem(GYPSUM_POWDER, "Gypsum Powder", itemTab);
        registerItem(FINE_NET, "Fine Net", itemTab);
        registerItem(PLANKTON, "Plankton", itemTab);
        registerItem(KRILL, "Krill", itemTab);
        registerItem(CRICKETS, "Crickets", itemTab);
        registerItem(COCKROACHES, "Cockroaches", itemTab);
        registerItem(MEALWORM_BEETLES, "Mealworm Beetles", itemTab);
        registerItem(CAR_CHASSIS, "Car Chassis", itemTab);
        registerItem(CAR_ENGINE_SYSTEM, "Car Engine System", itemTab);
        registerItem(CAR_SEATS, "Car Seats", itemTab);
        registerItem(CAR_TIRE, "Car Tire", itemTab);
        registerItem(CAR_WINDSCREEN, "Car Windscreen", itemTab);
        registerItem(UNFINISHED_CAR, "Unfinished Car", itemTab);
        registerItem(JEEP_WRANGLER, "Jeep Wrangler", itemTab);
        registerItem(FORD_EXPLORER, "Ford Explorer", itemTab);
        registerItem(JURASSICRAFT_THEME_DISC, "Disc JurassiCraft Theme", itemTab);
        registerItem(TROODONS_AND_RAPTORS_DISC, "Disc Troodons And Raptors", itemTab);
        registerItem(DONT_MOVE_A_MUSCLE_DISC, "Disc Don't Move A Muscle", itemTab);
        registerItem(LUNCH_BOX, "Lunch Box", itemTab);
        registerItem(STAMP_SET, "Stamp Set", itemTab);
        registerItem(DART_GUN, "Dart Gun", itemTab);
        registerItem(DART_TRANQUILIZER, "Dart Tranquilizer", itemTab);
        registerItem(DART_POISON_CYCASIN, "Dart Poison Cycasin", itemTab);
        registerItem(DART_POISON_EXECUTIONER_CONCOCTION, "Dart Poison Executioner Concoction", itemTab);
        registerItem(DART_TIPPED_POTION, "Dart Tipped Potion", itemTab);
        registerItem(TRACKING_DART, "Tracking Dart", itemTab);
        registerItem(TRACKING_TABLET, "Tracking Tablet", itemTab);
        registerItem(TRACKING_TABLET_ADVANCED, "Tracking Tablet Advanced", null);
        registerItem(TRACKING_TABLET_HIGHEND, "Tracking Tablet Highend", null);
        registerItem(TRACKING_TABLET_FREQUENCY, "Tracking Tablet Frequency", itemTab);
        registerItem(FREQUENCY_MODULE, "Frequency Module", itemTab);
        for (JournalItem.JournalType journalType : JournalItem.JournalType.values()) {
            registerItem(INGEN_JOURNAL.get(journalType), "InGen Journal " + journalType.name(), itemTab);

        }
        for (AmberItem.AmberStorageType type : AmberItem.AmberStorageType.values()) {
            registerItem(AMBER.get(type), "Amber " + type.getName(), itemTab);
        }
        for (TreeType type : TreeType.values()) {
            registerTreeType(type);
        }
    }

    public static void registerOres() {
        OreDictionary.registerOre("nuggetDiamond", DIAMOND_NUGGET);
        OreDictionary.registerOre("nuggetIron", IRON_NUGGET);
    }



    private static void registerTreeType(TreeType type) {
        String typeName = type.name();
        ItemDoor door = new ItemDoor(BlockHandler.ANCIENT_DOORS.get(type));
        ANCIENT_DOORS.put(type, door);
        registerItem(door, typeName + " Door Item", plantTab);
    }

    private static void registerItem(Item item, String name, CreativeTabs creativeTab) {
        String formattedName = name.toLowerCase(Locale.ENGLISH).replaceAll(" ", "_").replaceAll("'", "");
        item.setUnlocalizedName(formattedName);
        RegistryHandler.registerItem(item, formattedName);
        item.setCreativeTab(creativeTab);
    }
}
