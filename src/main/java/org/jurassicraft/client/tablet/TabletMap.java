package org.jurassicraft.client.tablet;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.logging.log4j.core.config.plugins.util.ResolverUtil.Test;
import org.jurassicraft.client.proxy.ClientProxy;
import org.jurassicraft.server.item.TrackingTablet;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockDeadBush;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockDropper;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockOre;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

public class TabletMap {
	public TabletMovingObjects movingObjects;
	public List<Integer> wipingTextures = new ArrayList();
	private Minecraft mc = Minecraft.getMinecraft();
	public List<RenderDinosaurInfo> dinosaurs = new ArrayList();
	boolean illuminated = false;
	private int loadingMapChunkX = 0;
	private int loadingMapChunkZ = 0;
	private int loadedMapChunkX = 0;
	private int loadedMapChunkZ = 0;
	private int blockColor = 0;
	private int blockY = 0;
	public boolean finished = false;
	public boolean receivingUpload = false;
	public MapChunk[][] currentBlocks = new MapChunk[16][16];
	private MapChunk[][] loadingBlocks = new MapChunk[16][16];
	private boolean forcedRefresh = false;
	public volatile int[][] blockStates = new int[750][750];
	public volatile int[][] blockHeights = new int[750][750];
	public volatile int[][] biomes = new int[750][750];
	private MutableBlockPos mbp = new MutableBlockPos();
	private int refreshChunkX = 0;
	private int refreshChunkZ = 0;
	private int containedSegmentX = 0;
	private int containedSegmentZ = 0;
	private boolean initiated = false;
	private MapChunk oldChunk = null;
	private HashMap<String, Integer> textureColors = new HashMap();
	private HashMap<Integer, Integer> blockColors = new HashMap();
	public boolean refreshColors = false;
	public int[][] previousBlockY = new int[4][16];
	private int red;
	private int green;
	private int blue;
	private float liveBrightness;
	private float postBrightness;
	private double shadowBlock;
	boolean toResetImage = true;
	public boolean FBOActive = false;
	public Framebuffer mapFrameBuffer;
	public Framebuffer overlayFrameBuffer;
	public Integer chunkX, chunkZ = null;
	public byte handshake = 0;


	public TabletMap() {
		//I'm waiting for a recode to be a dynamic thread! :(
		Thread t = new Thread(new Runnable() {
			public void run() {

				while (true) {
					if (ClientProxy.getHandlerInstance() != null) {
						if (ClientProxy.getHandlerInstance().getActive()) {
							int maxRunTime = 860;
							int idle = 100;
							while (maxRunTime > 0) {
								try {
									EntityPlayer player = mc.player;
									World world = mc.world;
									if ((ClientProxy.getHandlerInstance() == null) || (player == null)
											|| (world == null)) {
										idle = 400;
										break;
									}
									synchronized (world) {
										
										ClientProxy.getHandlerInstance().getMap().refreshChunks(player, world);
										maxRunTime--;
									}
									if ((containedSegmentX == 0) && (containedSegmentZ == 0) && (refreshChunkX == 0)
											&& (refreshChunkZ == 0)) {

										idle = 900;
										break;
									}
								} catch (ConcurrentModificationException ex) {
									ex.printStackTrace();
								}
							}
							try {
								Thread.sleep(idle);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}

						}
					
					}
				}
			}
		});
		t.start();
		t.setDaemon(true);
		this.movingObjects = new TabletMovingObjects();
	}

	public void resetChunks() {
		this.currentBlocks = new MapChunk[16][16];
	}
	
	public void resetPersistentChunks() {
		this.chunkX = this.chunkZ = null;
	}
	
	private void refreshChunks(EntityPlayer player, World world){
		MapChunk mchunk;
		if (this.containedSegmentX == 0 && this.containedSegmentZ == 0) {
			if (this.refreshChunkX == 0 && this.refreshChunkZ == 0) {
				if (this.refreshColors) {
					this.refreshColors = false;
					if (!this.blockColors.isEmpty()) {
						this.blockColors.clear();
						this.textureColors.clear();
					}
				}
				this.loadingMapChunkX = this.getMapCoord(player.posX);
				this.loadingMapChunkZ = this.getMapCoord(player.posZ);
				this.loadingBlocks = new MapChunk[16][16];
				if (this.toResetImage) {
					this.forcedRefresh = true;
					this.toResetImage = false;
				}
			}
			this.oldChunk = null;
			if (this.currentBlocks != null) {
				int refreshChunkXOld = this.loadingMapChunkX + this.refreshChunkX - this.loadedMapChunkX;
				int refreshChunkZOld = this.loadingMapChunkZ + this.refreshChunkZ - this.loadedMapChunkZ;
				if (refreshChunkXOld > -1 && refreshChunkXOld < this.currentBlocks.length && refreshChunkZOld > -1
						&& refreshChunkZOld < this.currentBlocks.length) {
					this.oldChunk = this.currentBlocks[refreshChunkXOld][refreshChunkZOld];
				}
			}
		}

		if (this.loadingBlocks[this.refreshChunkX][this.refreshChunkZ] == null) {
		
			this.loadingBlocks[this.refreshChunkX][this.refreshChunkZ] = new MapChunk(
					this.loadingMapChunkX + this.refreshChunkX, this.loadingMapChunkZ + this.refreshChunkZ);

		}
		mchunk = this.loadingBlocks[this.refreshChunkX][this.refreshChunkZ];
		
		this.refreshSegment(player, world, mchunk, this.oldChunk, this.refreshChunkX, this.refreshChunkZ,
				this.containedSegmentX, this.containedSegmentZ);

		this.containedSegmentZ++;
		if (this.containedSegmentZ >= 4) {
			this.containedSegmentZ = 0;
			this.containedSegmentX++;
			if (this.containedSegmentX >= 4) {
				this.containedSegmentX = 0;
				mchunk = this.loadingBlocks[this.refreshChunkX][this.refreshChunkZ];
				if (mchunk != null && mchunk.updateRequested) {
					mchunk.updateBuffers();
					mchunk.updateRequested = false;
				}
				if (this.refreshChunkX == 15
						&& this.refreshChunkZ == 15) {
					for (int i = 0; i < this.currentBlocks.length; i++) {
						if (i == this.currentBlocks.length - 1) {
							if(this.handshake == TrackingTablet.HandShake.NMAP.ordinal()) {
							this.finished = true;
							}
						}
						for (int j = 0; j < this.currentBlocks.length; j++) {
							MapChunk m = this.currentBlocks[i][j];
							MapChunk lm = null;
							int loadingX = this.loadedMapChunkX + i - this.loadingMapChunkX;
							int loadingZ = this.loadedMapChunkZ + j - this.loadingMapChunkZ;
							if (loadingX > -1 && loadingZ > -1 && loadingX < 16
									&& loadingZ < 16) {
								lm = this.loadingBlocks[loadingX][loadingZ];
							}

							if (m == null)
								continue;
							boolean shouldTransfer = lm != null;


								if (m.glTexture != 0) {
									if (shouldTransfer) {
										lm.glTexture = m.glTexture;

										continue;
									}
									this.wipingTextures.add(m.glTexture);
									continue;
								}
								if (!shouldTransfer || lm.refreshRequested || !m.refreshRequested)
									continue;
								lm.chunkBuffer = m.chunkBuffer;
								lm.refreshRequested = true;

							

						}

					}
					this.currentBlocks = this.loadingBlocks;
					this.loadedMapChunkX = this.loadingMapChunkX;
					this.loadedMapChunkZ = this.loadingMapChunkZ;

					// HERE'S THE POINT TO CHECK TWICE IF FINISHED!!!
					this.forcedRefresh = false;
				}
				if (this.refreshChunkZ + 1 >= 16) {
					this.refreshChunkZ = 0;
					this.previousBlockY = new int[4][16];
					if (this.refreshChunkX + 1 == 16) {
						this.refreshChunkX = 0;
					} else {
						this.refreshChunkX++;
					}
				} else {
					this.refreshChunkZ++;
				}
			}
		}

	}
	
	public void setInitiated() {
		
		this.initiated = true;
		
	}

    public boolean isInitiated() {
		
		return this.initiated;
		
	}
	
	private void refreshSegment(EntityPlayer player, World world, MapChunk mchunk, MapChunk oldChunk, int canvasX,
			int canvasZ, int chunkX, int chunkZ) {
		Chunk bchunk;
		int tileX = mchunk.XCord * 4 + chunkX;
		
		int tileZ = mchunk.ZCord * 4 + chunkZ;
		int tileFromCenterX = canvasX - 8;
		int tileFromCenterZ = canvasZ - 8;
		MapSegment oldTile = null;
		if (oldChunk != null) {
			oldTile = oldChunk.segments[chunkX][chunkZ];
		}
		bchunk = world.getChunkFromChunkCoords(tileX, tileZ);
		if (!(bchunk = world.getChunkFromChunkCoords(tileX, tileZ)).isLoaded() && oldTile != null) {
		
			
				mchunk.segments[chunkX][chunkZ] = oldTile;
				for (int j = 0; j < 16; j++) {
					this.previousBlockY[chunkX][j] = mchunk.lastHeights[chunkX][j];
				}
				if (this.forcedRefresh) {
					mchunk.updateRequested = true;
				}
			
	}
		
		int x1 = tileX * 16;
		int z1 = tileZ * 16;
		for (int blockX = x1; blockX < x1 + 16; blockX++) {
			for (int blockZ = z1; blockZ < z1 + 16; blockZ++) {
				
				this.getBlockColor(player, world, blockX, blockZ, bchunk, canvasX, canvasZ, tileX, tileZ, chunkX,
						chunkZ, oldTile);
				
				if ((blockZ & 15) != 15)
					continue;
				mchunk.lastHeights[chunkX][blockX & 15] = this.previousBlockY[chunkX][blockX & 15];
			}
		}
	}

	private int getBlockColorFromTexture(World world, IBlockState extended, BlockPos pos, Integer tempColor) {
		Integer color = this.blockColors.get(Block.getStateId((IBlockState) extended));
		int red = 0;
		int green = 0;
		int blue = 0;
		if (color == null) {
			String name = null;
			try {
				TextureAtlasSprite texture;
				Integer cachedColor;
				List upQuads = null;
				BlockModelShapes bms = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes();
				IBakedModel model = bms.getModelForState(extended);
					upQuads = model.getQuads(extended, EnumFacing.UP, 0);
				if (upQuads == null || upQuads.isEmpty()) {
					texture = bms.getTexture(extended);
				} else {
					texture = ((BakedQuad) upQuads.get(0)).getSprite();
				}
				name = texture.getIconName() + ".png";
				String[] args = name.split(":");
				if (args.length < 2) {
					args = new String[] { "minecraft", args[0] };
				}
				if ((cachedColor = this.textureColors.get(name)) == null) {
					ResourceLocation location = new ResourceLocation(args[0], "textures/" + args[1]);
					IResource resource = null;
					try {
					resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
					}catch(FileNotFoundException e) {
							texture = bms.getTexture(extended);
						name = texture.getIconName() + ".png";
				        args = name.split(":");
						if (args.length < 2) {
							args = new String[] { "minecraft", args[0] };
						}
						location = new ResourceLocation(args[0], "textures/" + args[1]);
						resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
					}
					InputStream input = resource.getInputStream();
					BufferedImage img = TextureUtil.readBufferedImage((InputStream) input);
					red = 0;
					green = 0;
					blue = 0;
					int total = 64;
					int tw = img.getWidth();
					int diff = tw / 8;
					for (int i = 0; i < 8; i++) {
						for (int j = 0; j < 8; j++) {
							int rgb = img.getRGB(i * diff, j * diff);
							if (rgb == 0) {
								--total;
								continue;
							}
							red += rgb >> 16 & 255;
							green += rgb >> 8 & 255;
							blue += rgb & 255;
						}
					}
					input.close();
					if (total == 0) {
						total = 1;
					}
					color = -16777215 | (red /= total) << 16 | (green /= total) << 8 | (blue /= total);
					if(color != 0)
						this.textureColors.put(name, color);
				} else {
					color = cachedColor;
				}
			} catch (Exception ex) {
				color = extended.getMapColor((IBlockAccess) world, (BlockPos) pos).colorValue;
				if (name != null && color != extended.getMapColor((IBlockAccess) world, (BlockPos) pos).colorValue) {
					this.textureColors.put(name, color);
				}
			}
			if (color != null) {
				this.blockColors.put(Block.getStateId((IBlockState) extended), color);
			}
		}
		if(color == 0 || color == -16777215) {
			color = extended.getMapColor((IBlockAccess) world, (BlockPos) pos).colorValue;
		}
		int grassColor = 16777215;
		try {
		
			if(tempColor != null) {
			if(extended.getBlock() instanceof BlockLeaves) {
			grassColor =  Biome.getBiome(tempColor).getFoliageColorAtPos(pos);
			}else if(extended.getBlock() instanceof BlockGrass){
				grassColor =  Biome.getBiome(tempColor).getGrassColorAtPos(pos);
			}else if(extended.getBlock() instanceof BlockLiquid) {
				grassColor = Biome.getBiome(tempColor).getWaterColorMultiplier();
			}else {
				grassColor = Minecraft.getMinecraft().getBlockColors().colorMultiplier(extended, (IBlockAccess) world, pos, 0);
			}
			}else {
				if(extended.getBlock() instanceof BlockLeaves) {
					grassColor = world.getBiome(pos).getFoliageColorAtPos(pos);
				}else if(extended.getBlock() instanceof BlockGrass){
					grassColor = world.getBiome(pos).getGrassColorAtPos(pos);
				}else if(extended.getBlock() instanceof BlockLiquid) {
					grassColor = world.getBiome(pos).getWaterColorMultiplier();
				}else {
					grassColor = Minecraft.getMinecraft().getBlockColors().colorMultiplier(extended, (IBlockAccess) world, pos, 0);
				}
				
					

			}
		} catch (IllegalArgumentException | NullPointerException e) {
			e.printStackTrace();
		}
		if (grassColor != 16777215) {
			float rMultiplier = (float) (color >> 16 & 255) / 255.0f;
			float gMultiplier = (float) (color >> 8 & 255) / 255.0f;
			float bMultiplier = (float) (color & 255) / 255.0f;
			red = (int) ((float) (grassColor >> 16 & 255) * rMultiplier);
			green = (int) ((float) (grassColor >> 8 & 255) * gMultiplier);
			blue = (int) ((float) (grassColor & 255) * bMultiplier);
			color = -16777215 | red << 16 | green << 8 | blue;
		}
		return color;
	}

	public Block findBlock(World world, Chunk bchunk, int chunkX, int chunkZ, int maxY, int x, int z, EntityPlayer player) {
		
		if(this.chunkX != null && this.chunkZ != null) {
			
		int diffX = (x >> 4) - this.chunkX;
		int diffZ = (z >> 4) - this.chunkZ;
		
		
		if((diffX > -1 && diffX < 29) && (diffZ > -1 && diffZ < 29)) {
			if(diffX < 29 &&  diffZ < 29 && this.handshake == TrackingTablet.HandShake.SMAP.ordinal()) {
				this.handshake = (byte) TrackingTablet.HandShake.NMAP.ordinal();
				this.finished = true;
			}
			if(!world.getChunkFromChunkCoords(x >> 4, z >> 4).isLoaded()){
			
			int originX = (x >> 4) * 16;
			int originZ = (z >> 4) * 16;
			
			int indexX = x - originX;
			int indexZ = z - originZ;
			//This try'n catch was once important! It's currently there to prevent a crash when closing the game in the world!
			try {
				
			IBlockState state = Block.getStateById(this.blockStates[indexX + 16 * diffX][indexZ + 16 * diffZ]);
			this.blockY = this.blockHeights[indexX + 16 * diffX][indexZ + 16 * diffZ];
			this.mbp.setPos(this.worldBlockPos((x >> 4), (z >> 4), chunkX, this.blockY, chunkZ));

			this.blockColor = this.getBlockColorFromTexture(world, state, this.mbp, this.biomes[indexX + 16 * diffX][indexZ + 16 * diffZ]);

			this.illuminated = this.isIlluminated(state, world, this.mbp);
			return state.getBlock();
			}catch(ArrayIndexOutOfBoundsException e) {}
			
			
			}
			
		}
		
		}


		for (int i = maxY; i >= 0; --i) {
			IBlockState state = bchunk.getBlockState(chunkX, i, chunkZ);
			if (state == null)
				continue;
			Block block = state.getBlock();
			if (!(block instanceof BlockAir)) {
				if (state.getRenderType() == EnumBlockRenderType.INVISIBLE || block == Blocks.TORCH
						|| block == Blocks.TALLGRASS || block == Blocks.DOUBLE_PLANT
						|| (block instanceof BlockFlower || block instanceof BlockDoublePlant
								|| block instanceof BlockDeadBush)
						|| (block == Blocks.REDSTONE_TORCH || block == Blocks.REDSTONE_WIRE
								|| block instanceof BlockRedstoneRepeater || block instanceof BlockRedstoneComparator))
					continue;
				this.blockY = i;
				BlockPos worldPos = this.worldBlockPos(bchunk.x, bchunk.z, chunkX, this.blockY, chunkZ);
				
				IBlockState extended = block.getExtendedState(state, (IBlockAccess) world, worldPos);

				this.blockColor = this.getBlockColorFromTexture(world, extended, worldPos, null);

				this.illuminated = this.isIlluminated(state, bchunk.getWorld(), worldPos);
				return block;
			}

		}
	
		return null;
	}
	
	static int[] getMaxColorValue(int r, int g, int b) {
		int biggestValue = Math.max(r, Math.max(g, b));
		int[] color = new int[3];
		color[0] = 255 * r / biggestValue;
		color[1] = 255 * g / biggestValue;
		color[2] = 255 * b / biggestValue;

		return color;
	}

	public int getMapCoord(double coord) {
		return ((int) Math.floor(coord) >> 6) - 8;
	}

	public BlockPos worldBlockPos(int chunkX, int chunkZ, int x, int y, int z) {
		return new BlockPos(chunkX * 16 + x, y, chunkZ * 16 + z);
	}

	public boolean isIlluminated(IBlockState state, World world, BlockPos pos) {
		return state.getLightValue((IBlockAccess) world, pos) > 0;
	}

	public void getBlockColor(EntityPlayer player, World world, int par1, int par2, Chunk bchunk, int canvasX,
			int canvasZ, int tileX, int tileZ, int containedSegmentX, int containedSegmentZ, MapSegment oldTile) {
		MapSegment tile;
		int playerY = (int) player.posY;
		int inChunkX = par1 & 15;
		int inChunkZ = par2 & 15;
		
		//That heightmap part does ignore lava 
		//TODO: Further investigation needed
		//UNCOMMENT THAT LINE AND REMOVE THE OTHER ONE TO GET A BETTER PERFORMANCE!

	    //int maxY = bchunk.getHeightValue(inChunkX, inChunkZ) + 3;
		int maxY = bchunk.getPrecipitationHeight(new BlockPos(par1, 0 , par2)).getY();
		this.blockY = 0;
		this.blockColor = 0;
		this.illuminated = false;
		this.shadowBlock = 1.0;
		Block block = this.findBlock(world, bchunk, inChunkX, inChunkZ, maxY, par1, par2, player);
		this.illuminated = block != null && !(block instanceof BlockOre) && this.illuminated;
		boolean success = true;
		if (this.previousBlockY[containedSegmentX][inChunkX] <= 0) {
			this.previousBlockY[containedSegmentX][inChunkX] = this.blockY;
			try {
				Chunk prevChunk = world.getChunkFromChunkCoords(tileX, tileZ - 1);
				if (prevChunk != null && prevChunk.isLoaded()) {
				    //this.previousBlockY[containedSegmentX][inChunkX] = prevChunk.getHeightValue(inChunkX, 15) - 1;
					this.previousBlockY[containedSegmentX][inChunkX] = prevChunk.getPrecipitationHeight(new BlockPos(par1, 0 , (par2 >> 4 + 16) * 16)).getY() - 1;
				} else {
					success = false;
				}
			} catch (IllegalStateException e) {
				success = false;
			}
		}
		if (!this.illuminated) {
			BlockPos pos = new BlockPos(inChunkX, Math.min(this.blockY + 1, 255), inChunkZ);

				this.liveBrightness = 1.0f;

				this.postBrightness = 1.0f;
			
		    if (this.blockY > this.previousBlockY[containedSegmentX][inChunkX]) {
				this.shadowBlock *= 1.10;
			}
			if (this.blockY < this.previousBlockY[containedSegmentX][inChunkX]) {
				this.shadowBlock *= 0.90;
			}
			

		}
		this.previousBlockY[containedSegmentX][inChunkX] = this.blockY;
		int[] color = new int[3];
		if (this.illuminated) {
			color = getMaxColorValue(this.blockColor >> 16 & 255, this.blockColor >> 8 & 255, this.blockColor & 255);
		}

			float b;
			if (this.illuminated) {
				this.red = color[0];
				this.green = color[1];
				this.blue = color[2];
				b = 1.0f;
			} else {
				this.red = this.blockColor >> 16 & 255;
				this.green = this.blockColor >> 8 & 255;
				this.blue = this.blockColor & 255;
				b = this.liveBrightness;
			}
			this.red = (int) (((double) ((float) this.red * b) * this.shadowBlock)
					* (double) this.postBrightness);
			if (this.red > 255) {
				this.red = 255;
			}
			this.green = (int) (((double) ((float) this.green * b) * this.shadowBlock)
					* (double) this.postBrightness);
			if (this.green > 255) {
				this.green = 255;
			}
			this.blue = (int) (((double) ((float) this.blue * b) * this.shadowBlock)
					* (double) this.postBrightness);
			if (this.blue > 255)
			this.blue = 255;
		
		if (canvasX < 0 || canvasX >= 16 || canvasZ < 0 || canvasZ >= 16) {
			return;
		}
		MapChunk chunk = this.loadingBlocks[canvasX][canvasZ];
		if ((tile = chunk.segments[containedSegmentX][containedSegmentZ]) == null) {
			chunk.segments[containedSegmentX][containedSegmentZ] = tile = new MapSegment();
		}
		if ((this.red != 0 || this.green != 0 || this.blue != 0) && (oldTile == null || oldTile.red[inChunkX][inChunkZ] != this.red || oldTile.green[inChunkX][inChunkZ] != this.green || oldTile.blue[inChunkX][inChunkZ] != this.blue)) {
			chunk.updateRequested = true;
		}
	
			tile.red[inChunkX][inChunkZ] = this.red;
			tile.green[inChunkX][inChunkZ] = this.green;
			tile.blue[inChunkX][inChunkZ] = this.blue;
		
	}

	public void markForReset() {
		this.toResetImage = true;
	}

	public void createFrameBuffers(float scale) {

		if (!Minecraft.getMinecraft().gameSettings.fboEnable) {
			Minecraft.getMinecraft().gameSettings.setOptionValue(GameSettings.Options.FBO_ENABLE, 0);
		}
		this.overlayFrameBuffer = new Framebuffer((int) (512 * scale), (int) (512 * scale), false);
		this.mapFrameBuffer = new Framebuffer((int) (512 * scale), (int) (512 * scale), false);
		this.FBOActive = this.mapFrameBuffer.framebufferObject != -1 && this.overlayFrameBuffer.framebufferObject != -1;

	}

	public void renderChunks(EntityPlayer player, int mapWidth, ItemStack stack) {
		
		int zoom = ClientProxy.getHandlerInstance().getZoomFactor();
		int radius = (int) ((double) mapWidth / Math.sqrt(2.0) / 2.0 / zoom) / 64 + 1;
		double playerX = this.movingObjects.getPlayerOffX((EntityPlayer) player, stack);
		double playerZ = this.movingObjects.getPlayerOffZ((EntityPlayer) player, stack);
		int playerChunkX = (int) Math.floor(playerX) >> 6;
		int playerChunkZ = (int) Math.floor(playerZ) >> 6;
		int offsetX = (int) Math.floor(playerX) & 63;
		int offsetZ = (int) Math.floor(playerZ) & 63;
		this.mapFrameBuffer.bindFramebuffer(true);
		GL11.glClear(16640);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		RenderHelper.disableStandardItemLighting();
		GlStateManager.clear(256);
		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		GL11.glPushMatrix();
		GlStateManager.loadIdentity();
		GlStateManager.ortho((double) 0.0, (double) 512.0, (double) 512.0,
				(double) 0.0, (double) 1000.0, (double) 3000.0);
		GlStateManager.matrixMode(GL11.GL_MODELVIEW);
		GL11.glPushMatrix();
		GlStateManager.loadIdentity();
		double zChunkPixel = this.movingObjects.getPlayerOffZ((EntityPlayer) player, stack) - Math.floor(playerZ);
		double xChunkPixel = this.movingObjects.getPlayerOffX((EntityPlayer) player, stack) - Math.floor(playerX);
		if (xChunkPixel < 0.0) {
			xChunkPixel += 1;
		}
		if (zChunkPixel < 0.0) {
			zChunkPixel += 1;
		}
		zChunkPixel = 1 - zChunkPixel;
		GlStateManager.enableBlend();
		GlStateManager.translate(256.0f, 256.0f, -2000.0f);
		GlStateManager.scale((double) zoom, (double) zoom, (double) 1.0);
		Gui.drawRect(-256, -256, 256, 256, new Color(0, 0, 0, 255).hashCode());
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		int minX = playerChunkX - radius - 1;
		int minZ = playerChunkZ - radius - 1;
		int maxX = playerChunkX + radius + 1;
		int maxZ = playerChunkZ + radius + 1;
		for (int X = minX; X <= maxX; X++) {
			int canvasX = X - this.loadedMapChunkX;
			if (canvasX < 0 || canvasX >= this.currentBlocks.length)
				continue;
			for (int Z = minZ; Z <= maxZ; Z++) {
				MapChunk mchunk;
				int canvasZ = Z - this.loadedMapChunkZ;
				if (canvasZ < 0 || canvasZ >= this.currentBlocks.length
						|| (mchunk = this.currentBlocks[canvasX][canvasZ]) == null)
					continue;
				mchunk.bindTexture();
				if (mchunk.glTexture == 0)
					continue;

				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

				int drawX = (mchunk.XCord - playerChunkX) * 64 - offsetX;
				int drawZ = (mchunk.ZCord - playerChunkZ) * 64 - offsetZ - 1;
				Gui.drawModalRectWithCustomSizedTexture(drawX, drawZ, 0, 0, 64, 64, 64.0f, 64.0f);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
				GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1,
						GL11.GL_ONE_MINUS_SRC_ALPHA);
				int r = 0;
				int g = 0;
				int b = 0;

				GlStateManager.color((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);

			}

		}

		this.mapFrameBuffer.unbindFramebuffer();
		this.overlayFrameBuffer.bindFramebuffer(false);
		GL11.glClear(16640);
		this.mapFrameBuffer.bindFramebufferTexture();
		GlStateManager.loadIdentity();
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);

		GlStateManager.translate((float) (mapWidth / 2.0f + 0.5f), (float) (511.5f - mapWidth / 2.0f), (float) -2000.0f);
		GL11.glPushMatrix();
		GlStateManager.translate((double) ((-xChunkPixel) * zoom), (double) ((-zChunkPixel) * zoom), (double) 0.0);
		GlStateManager.disableBlend();
		GL11.glColor4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
		Gui.drawModalRectWithCustomSizedTexture(-256, -256, 0, 0, 512, 512, 512.0f, 512.0f);
		GL11.glColor4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
		GL11.glPopMatrix();
		this.mc.getTextureManager().bindTexture(HandlerInstance.guiTextures);
		GlStateManager.enableBlend();
		GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, GL11.GL_ONE_MINUS_SRC_ALPHA);
		EntityPlayer p = player;
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		this.renderDinosaurs(p, this.dinosaurs, playerX, playerZ);
		this.mc.getTextureManager().bindTexture(HandlerInstance.guiTextures);
		this.movingObjects.renderPlayerArrow(p, stack);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		this.overlayFrameBuffer.unbindFramebuffer();
		GL11.glColor4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
		GlStateManager.disableBlend();
		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		GL11.glPopMatrix();
		GlStateManager.matrixMode(GL11.GL_MODELVIEW);
		GL11.glPopMatrix();
	}

	public void renderDinosaurs(EntityPlayer p, List<RenderDinosaurInfo> loadedDinosaurs, double X, double Z) {

		for (RenderDinosaurInfo dinosaur : loadedDinosaurs) {
			this.movingObjects.renderDinosaurIcon(dinosaur, p, X, Z);
		}

	}

	public void receivedHandshake(byte status) {
		this.handshake = status;
		
	}

}

class MapChunk {

	public int XCord;
	public int ZCord;
	public MapSegment[][] segments = new MapSegment[4][4];
	public int glTexture;
	public boolean refreshRequested;
	private boolean refreshed = false;
	public ByteBuffer chunkBuffer;
	public int[][] lastHeights = new int[4][16];
	public boolean updateRequested = false;

	public MapChunk(int X, int Z) {
		this.XCord = X;
		this.ZCord = Z;
	}

	public boolean wantsRefresh() {
		if (this.refreshed) {
			return false;
		}
		if (this.refreshRequested) {
			return true;
		}
		this.refreshed = true;
		return false;
}

	public void bindTexture() {
		if (wantsRefresh()) {
			boolean result = false;
			if (this.glTexture == 0) {
				this.glTexture = GL11.glGenTextures();
				result = true;
			}
			GlStateManager.bindTexture(this.glTexture);
			if (result) {
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 33071);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 33071);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, 33085, 0);
				GL11.glTexParameterf(GL11.GL_TEXTURE_2D, 33082, 0.0f);
				GL11.glTexParameterf(GL11.GL_TEXTURE_2D, 33083, 0.0f);
				GL11.glTexParameterf(GL11.GL_TEXTURE_2D, 34049, 0.0f);
			}
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, GL11.GL_LIGHTING_BIT, GL11.GL_LIGHTING_BIT, 0,
					GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) this.chunkBuffer);
			this.chunkBuffer.clear();
			this.refreshRequested = false;
		}
		if (this.glTexture != 0) {
			GlStateManager.bindTexture(this.glTexture);
		}
	}

	public void updateBuffers() {
		this.refreshed = true;

			this.refreshRequested = false;
			if (this.chunkBuffer == null)
		
			this.chunkBuffer = BufferUtils.createByteBuffer((this.segments.length * this.segments.length * 800));
		
		byte[] bytes = new byte[this.segments.length * this.segments.length * 800];
		for (int i = 0; i < this.segments.length; i++) {
			int offX = i * 16;
			for (int q = 0; q < this.segments.length; q++) {
				MapSegment tile = this.segments[i][q];
				if (tile == null)
					continue;
				int offZ = q * 16;
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
							this.storeColor(offX + x, offZ + z, (byte)tile.red[x][z], (byte) tile.green[x][z], (byte)tile.blue[x][z], bytes);
						
					}
				}
			}
		}
		
			this.chunkBuffer.put(bytes);
			this.chunkBuffer.flip();
			this.refreshRequested = true;
		
		this.refreshed = false;
	}

	public void storeColor(int x, int y, byte red, byte green, byte blue, byte[] texture) {
		int pos = (x + y * 64 ) * 3;
		texture[pos] = red;
		texture[++pos] = green;
		texture[++pos] = blue;
	}
}

class MapSegment {

	public int[][] red = new int[16][16];
	public int[][] green = new int[16][16];
	public int[][] blue = new int[16][16];

}