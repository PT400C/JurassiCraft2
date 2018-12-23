package org.jurassicraft.server.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jurassicraft.server.block.entity.DisplayBlockEntity;
import org.jurassicraft.server.dinosaur.Dinosaur;
import org.jurassicraft.server.dinosaur.DinosaurMetadata;
import org.jurassicraft.server.entity.EntityHandler;
import org.jurassicraft.server.item.DisplayBlockItem;
import org.jurassicraft.server.item.ItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DisplayBlock extends BlockContainer {
    public DisplayBlock() {
        super(Material.WOOD);
        this.setSoundType(SoundType.WOOD);
        this.setTickRandomly(false);
        this.setHardness(0.0F);
        this.setResistance(0.0F);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess blockAccess, BlockPos pos) {
        return this.getBounds(blockAccess, pos);
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos) {
        return this.getBounds(world, pos).offset(pos);
    }

    private AxisAlignedBB getBounds(IBlockAccess world, BlockPos pos) {
        TileEntity entity = world.getTileEntity(pos);
        if (entity instanceof DisplayBlockEntity) {
            DisplayBlockEntity displayEntity = (DisplayBlockEntity) entity;
            Dinosaur dinosaur = displayEntity.getEntity().getDinosaur();
            if (dinosaur != null && !displayEntity.isSkeleton()) {
                DinosaurMetadata metadata = dinosaur.getMetadata();
                float width = MathHelper.clamp(metadata.getAdultSizeX() * 0.25F, 0.1F, 1.0F);
                float height = MathHelper.clamp(metadata.getAdultSizeY() * 0.25F, 0.1F, 1.0F);
                float halfWidth = width / 2.0F;
                return new AxisAlignedBB(0.5 - halfWidth, 0, 0.5 - halfWidth, halfWidth + 0.5, height, halfWidth + 0.5);
            }
        }
        return new AxisAlignedBB(0, 0, 0, 1, 1, 1);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && this.canBlockStay(world, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        super.neighborChanged(state, world, pos, block, fromPos);
        this.checkAndDropBlock(world, pos, world.getBlockState(pos));
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        this.checkAndDropBlock(world, pos, state);
    }

    protected void checkAndDropBlock(World world, BlockPos pos, IBlockState state) {
        if (!this.canBlockStay(world, pos)) {
            this.dropBlockAsItem(world, pos, state, 0);
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        }
    }

    public boolean canBlockStay(World world, BlockPos pos) {
        return world.getBlockState(pos.down()).isOpaqueCube();
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return ItemHandler.DISPLAY_BLOCK_ITEM;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return this.getItemFromTile(this.getTile(world, pos));
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new DisplayBlockEntity();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return true;
    }

    protected DisplayBlockEntity getTile(IBlockAccess world, BlockPos pos) {
        return (DisplayBlockEntity) world.getTileEntity(pos);
    }

    @Override
    public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        if (!player.capabilities.isCreativeMode) {
            this.dropBlockAsItem(world, pos, state, 0);
        }

        super.onBlockHarvested(world, pos, state, player);
    }

    public ItemStack getItemFromTile(DisplayBlockEntity tile) {
        int metadata = DisplayBlockItem.getMetadata(EntityHandler.getDinosaurId(tile.getEntity().getDinosaur()), tile.getVariant(), tile.isMale() ? 1 : 2, tile.isSkeleton());
        return new ItemStack(ItemHandler.DISPLAY_BLOCK_ITEM, 1, metadata);
    }

    @Override
    public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        List<ItemStack> drops = new ArrayList<>(1);

        DisplayBlockEntity tile = this.getTile(world, pos);

        if (tile != null) {
            drops.add(this.getItemFromTile(tile));
        }

        return drops;
    }
}
