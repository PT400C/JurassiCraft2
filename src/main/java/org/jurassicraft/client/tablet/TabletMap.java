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
import org.jurassicraft.client.proxy.ClientProxy;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockDeadBush;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockOre;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneRepeater;
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
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class TabletMap {
	public TabletMovingObjects movingObjects;
	private MapSegmentData mapsegment;
	public List<Integer> wipingTextures = new ArrayList();
	private Minecraft mc = Minecraft.getMinecraft();
	public boolean FBOActive = false;
	public boolean finished = false;
	private boolean forcedRefresh = false;
	public List<RenderDinosaurInfo> dinosaurs = new ArrayList();
	boolean illuminated = false;
	private HashMap<String, Integer> textureColors = new HashMap();
	private HashMap<Integer, Integer> blockColors = new HashMap();
	public boolean refreshColors = false;
	public int[][] previousBlockY = new int[4][16];
	private int blockY = 0;
	private int blockColor = 0;
	private double shadowBlock;
	boolean toResetImage = true;
	public Framebuffer mapFrameBuffer;
	public Framebuffer overlayFrameBuffer;

	public TabletMap() {
		(new Thread(new Runnable() {
			public void run() {

				while (true) {

					if (ClientProxy.getHandlerInstance() != null) {

						if (ClientProxy.getHandlerInstance().getActive()) {

							int maxRunTime = 860;
							long startTimestamp = System.currentTimeMillis();
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
									if ((mapsegment.containerX == 0) && (mapsegment.containerZ == 0)
											&& (mapsegment.updateChunkX == 0) && (mapsegment.updateChunkZ == 0)) {

										idle = 900;
										break;
									}
								} catch (ConcurrentModificationException ex) {
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
		})).start();
		this.movingObjects = new TabletMovingObjects();
		this.mapsegment = new MapSegmentData();
	}

	private void refreshChunks(EntityPlayer player, World world) throws ConcurrentModificationException {
		if (this.mapsegment.containerX == 0 && this.mapsegment.containerZ == 0) {
			if (this.mapsegment.updateChunkX == 0 && this.mapsegment.updateChunkZ == 0) {
				if (this.refreshColors) {
					this.refreshColors = false;
					if (!this.blockColors.isEmpty()) {
						this.blockColors.clear();
						this.textureColors.clear();
					}
				}
				this.mapsegment.loadingMapChunkX = this.getMapCoord(player.posX);
				this.mapsegment.loadingMapChunkZ = this.getMapCoord(player.posZ);
				this.mapsegment.loadingBlocks = new MapChunk[16][16];
				if (this.toResetImage) {
					this.forcedRefresh = true;
					this.toResetImage = false;
				}
			}
			this.mapsegment.previousChunk = null;
			if (this.mapsegment.currentBlocks != null) {
				int updateChunkXOld = this.mapsegment.loadingMapChunkX + this.mapsegment.updateChunkX
						- this.mapsegment.loadedMapChunkX;
				int updateChunkZOld = this.mapsegment.loadingMapChunkZ + this.mapsegment.updateChunkZ
						- this.mapsegment.loadedMapChunkZ;
				if (updateChunkXOld > -1 && updateChunkXOld < this.mapsegment.currentBlocks.length
						&& updateChunkZOld > -1 && updateChunkZOld < this.mapsegment.currentBlocks.length) {
					this.mapsegment.previousChunk = this.mapsegment.currentBlocks[updateChunkXOld][updateChunkZOld];
				}
			}
		}
		if (this.mapsegment.loadingBlocks[this.mapsegment.updateChunkX][this.mapsegment.updateChunkZ] == null) {
			this.mapsegment.loadingBlocks[this.mapsegment.updateChunkX][this.mapsegment.updateChunkZ] = new MapChunk(
					this.mapsegment.loadingMapChunkX + this.mapsegment.updateChunkX,
					this.mapsegment.loadingMapChunkZ + this.mapsegment.updateChunkZ);

		}
		MapChunk reloadChunk = this.mapsegment.loadingBlocks[this.mapsegment.updateChunkX][this.mapsegment.updateChunkZ];
		this.refreshSegment(player, world, reloadChunk, this.mapsegment.previousChunk, this.mapsegment.updateChunkX,
				this.mapsegment.updateChunkZ, this.mapsegment.containerX, this.mapsegment.containerZ);
		++this.mapsegment.containerZ;
		if (this.mapsegment.containerZ >= 4) {
			this.mapsegment.containerZ = 0;
			++this.mapsegment.containerX;
			if (this.mapsegment.containerX >= 4) {
				this.mapsegment.containerX = 0;

				if (this.mapsegment.updateChunkX == 15 && this.mapsegment.updateChunkZ == 15) {
					for (int i = 0; i < this.mapsegment.currentBlocks.length; ++i) {
						if (i == this.mapsegment.currentBlocks.length - 1) {
							this.finished = true;
						}
						for (int j = 0; j < this.mapsegment.currentBlocks.length; ++j) {
							MapChunk m = this.mapsegment.currentBlocks[i][j];
							MapChunk lm = null;
							int loadingX = this.mapsegment.loadedMapChunkX + i - this.mapsegment.loadingMapChunkX;
							int loadingZ = this.mapsegment.loadedMapChunkZ + j - this.mapsegment.loadingMapChunkZ;
							if (loadingX > -1 && loadingZ > -1 && loadingX < 16 && loadingZ < 16) {
								lm = this.mapsegment.loadingBlocks[loadingX][loadingZ];
							}

							if (m == null)
								continue;
							boolean shouldTransfer = lm != null;

							if (m.glTexture[0] != 0) {
								if (shouldTransfer) {
									lm.glTexture[0] = m.glTexture[0];

									continue;
								}
								this.wipingTextures.add(m.glTexture[0]);
								continue;
							}
							if (!shouldTransfer || !m.refreshRequested[0] || lm.refreshRequested[0])
								continue;
							lm.chunkBuffer[0] = m.chunkBuffer[0];
							lm.refreshRequested[0] = true;

						}

					}
					this.mapsegment.currentBlocks = this.mapsegment.loadingBlocks;
					this.mapsegment.loadedMapChunkX = this.mapsegment.loadingMapChunkX;
					this.mapsegment.loadedMapChunkZ = this.mapsegment.loadingMapChunkZ;

					// HERE'S THE POINT TO CHECK TWICE IF FINISHED LEVEL OVERLAY!!!

					this.forcedRefresh = false;
				}
				reloadChunk = this.mapsegment.loadingBlocks[this.mapsegment.updateChunkX][this.mapsegment.updateChunkZ];
				if (reloadChunk != null && reloadChunk.updateRequested) {
					reloadChunk.updateBuffers();
					reloadChunk.updateRequested = false;
				}
				if (this.mapsegment.updateChunkZ + 1 >= 16) {
					this.mapsegment.updateChunkZ = 0;
					this.previousBlockY = new int[4][16];
					if (this.mapsegment.updateChunkX + 1 == 16) {
						this.mapsegment.updateChunkX = 0;
					} else {
						this.mapsegment.updateChunkX++;
					}
				} else {
					++this.mapsegment.updateChunkZ;
				}
			}
		}

	}

	private void refreshSegment(EntityPlayer player, World world, MapChunk reloadChunk, MapChunk overrideChunk,
			int mapX, int mapZ, int containerX, int containerZ) {

		int tileX = reloadChunk.XCord * 4 + containerX;
		int tileZ = reloadChunk.ZCord * 4 + containerZ;
		int tileFromCenterX = mapX - 8;
		int tileFromCenterZ = mapZ - 8;
		MapSegment prevSegment = null;
		if (this.mapsegment.previousChunk != null) {
			prevSegment = this.mapsegment.previousChunk.tiles[containerX][containerZ];
		}
		Chunk chunk = world.getChunkFromChunkCoords(tileX, tileZ);
		if (!chunk.isLoaded()
				|| (tileFromCenterX > 16 || tileFromCenterZ > 16 || tileFromCenterX < -16 || tileFromCenterZ < -16)
						&& prevSegment != null) {
			if (prevSegment != null) {
				reloadChunk.tiles[containerX][containerZ] = prevSegment;
				for (int j = 0; j < 16; ++j) {
					this.previousBlockY[containerX][j] = reloadChunk.lastHeights[containerX][j];
				}
				if (this.forcedRefresh) {
					reloadChunk.updateRequested = true;
				}
			} else {
				this.previousBlockY = new int[4][16];
			}
			return;
		}
		int x1 = tileX * 16;
		int z1 = tileZ * 16;
		for (int blockX = x1; blockX < x1 + 16; ++blockX) {
			for (int blockZ = z1; blockZ < z1 + 16; ++blockZ) {
				this.getBlockColor(player, world, blockX, blockZ, chunk, mapX, mapZ, tileX, tileZ, containerX, containerZ, prevSegment);
				if ((blockZ & 15) != 15)
					continue;
				reloadChunk.lastHeights[containerX][blockX & 15] = this.previousBlockY[containerX][blockX & 15];
			}
		}
	}

	private int getBlockColourFromTexture(World world, IBlockState state, IBlockState extended, Block b, BlockPos pos) {
		Integer color = this.blockColors.get(Block.getStateId((IBlockState) state));
		int red = 0;
		int green = 0;
		int blue = 0;
		
		if (color == null) {
			String name = null;
			try {
				TextureAtlasSprite texture = null;
				BlockModelShapes bms = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes();
				IBakedModel model = bms.getModelForState(state);
				Integer cachedColour;
				List textureQuads = model.getQuads(extended, EnumFacing.UP, 0);
				if (!(textureQuads == null || textureQuads.isEmpty()))
					texture = ((BakedQuad) textureQuads.get(0)).getSprite();
				name = texture.getIconName() + ".png";
				String[] args = name.split(":");
				if ((cachedColour = this.textureColors.get(name)) == null) {
					ResourceLocation location = new ResourceLocation(args[0], "textures/" + args[1]);
					IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
					InputStream input = resource.getInputStream();
					BufferedImage img = TextureUtil.readBufferedImage((InputStream) input);
					int total = 64;
					int tw = img.getWidth();
					int diff = tw / 8;
					for (int i = 0; i < 8; ++i) {
						for (int j = 0; j < 8; ++j) {
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
					color = -16777216 | (red /= total) << 16 | (green /= total) << 8 | (blue /= total);
					this.textureColors.put(name, color);
				} else {
					color = cachedColour;
				}
			} catch (Exception ex) {
				
				color = state.getMapColor((IBlockAccess) world, (BlockPos) pos).colorValue;
				if (name != null) {
					this.textureColors.put(name, color);
				}
			}
			
		}else if (color != null) {
			this.blockColors.put(Block.getStateId((IBlockState) state), color);
		}
		int grassColor = 16777215;
		try {
			grassColor = Minecraft.getMinecraft().getBlockColors().colorMultiplier(state, (IBlockAccess) world, pos, 0);
		} catch (IllegalArgumentException e) {

		}
		if (grassColor != 16777215) {
			float rMultiplier = (float) (color >> 16 & 255) / 255.0f;
			float gMultiplier = (float) (color >> 8 & 255) / 255.0f;
			float bMultiplier = (float) (color & 255) / 255.0f;
			red = (int) ((float) (grassColor >> 16 & 255) * rMultiplier);
			green = (int) ((float) (grassColor >> 8 & 255) * gMultiplier);
			blue = (int) ((float) (grassColor & 255) * bMultiplier);
			color = -16777216 | red << 16 | green << 8 | blue;
		}
		return color;
	}

	private Block findBlock(World world, Chunk oldChunk, int insideX, int insideZ, int highY, int lowY) {
		for (int i = highY; i >= lowY; --i) {
			IBlockState state = oldChunk.getBlockState(insideX, i, insideZ);
			if (state == null)
				continue;
			Block block = state.getBlock();
			if (!(block instanceof BlockAir)) {
				if (state.getRenderType() == EnumBlockRenderType.INVISIBLE || block == Blocks.TORCH
						|| block == Blocks.TALLGRASS || block == Blocks.DOUBLE_PLANT
						|| (block instanceof BlockDeadBush || block instanceof BlockFlower || block instanceof BlockDoublePlant)
						|| (block == Blocks.REDSTONE_TORCH || block instanceof BlockRedstoneDiode || block == Blocks.REDSTONE_WIRE))
					continue;
				this.blockY = i;
				BlockPos pos = new BlockPos(insideX, this.blockY, insideZ);
				BlockPos worldPos = this.worldBlockPos(oldChunk.x, oldChunk.z, insideX, this.blockY, insideZ);
				IBlockState extended = block.getExtendedState(state, (IBlockAccess) world, worldPos);

				this.blockColor = this.getBlockColourFromTexture(world, state, extended, block, worldPos);

				this.illuminated = this.isIlluminated(state, oldChunk.getWorld(), worldPos);
				return block;
			}
			if (!(block instanceof BlockAir))
				continue;

		}
		return null;
	}

	private void getMaxColorValue(int r, int g, int b, int[] color) {
		int biggestValue = Math.max(r, Math.max(g, b));
		color[0] = 255 * r / biggestValue;
		color[1] = 255 * g / biggestValue;
		color[2] = 255 * b / biggestValue;
	}

	private int getMapCoord(double worldCoord) {
		return ((int) Math.floor(worldCoord) >> 6) - 8;
	}

	private BlockPos worldBlockPos(int chunkX, int chunkZ, int x, int y, int z) {
		return new BlockPos(chunkX * 16 + x, y, chunkZ * 16 + z);
	}

	private boolean isIlluminated(IBlockState state, World world, BlockPos pos) {
		return state.getLightValue((IBlockAccess) world, pos) > 0;
	}

	private void getBlockColor(EntityPlayer player, World world, int blockX, int blockZ, Chunk oldChunk, int mapX,
			int mapZ, int tileX, int tileZ, int containerX, int containerZ, MapSegment prevSegment) {
		int lowY;
		int containedX = blockX & 15;
		int containedZ = blockZ & 15;
		int playerY = (int) player.posY;
		int height = oldChunk.getHeightValue(containedX, containedZ);
		int highY = height + 3;
		int n = lowY = 0;
		if (lowY < 0) {
			lowY = 0;
		}
		this.blockY = 0;
		this.blockColor = 0;
		this.illuminated = false;
		this.shadowBlock = 1.0;
		Block block = this.findBlock(world, oldChunk, containedX, containedZ, highY, lowY);
		this.illuminated = block != null && this.illuminated && !(block instanceof BlockOre);
		if (this.previousBlockY[containerX][containedX] <= 0) {
			this.previousBlockY[containerX][containedX] = this.blockY;
			try {
				Chunk prevChunk = world.getChunkFromChunkCoords(tileX, tileZ - 1);
				if (prevChunk != null && prevChunk.isLoaded()) {
					this.previousBlockY[containerX][containedX] = prevChunk.getHeightValue(containedX, 15) - 1;
				}
			} catch (IllegalStateException e) {

			}
		}
		if (!this.illuminated) {
			BlockPos pos = new BlockPos(containedX, Math.min(this.blockY + 1, 255), containedZ);
			this.mapsegment.liveBrightness[0] = 1.0f;
			this.mapsegment.postBrightness[0] = 1.0f;
			if (this.blockY < this.previousBlockY[containerX][containedX]) {
				this.shadowBlock *= 0.90;
			}
			if (this.blockY > this.previousBlockY[containerX][containedX]) {
				this.shadowBlock *= 1.10;
			}

		}
		int red = this.blockColor >> 16 & 255;
		int green = this.blockColor >> 8 & 255;
		int blue = this.blockColor & 255;
		int[] color = new int[3];
		this.previousBlockY[containerX][containedX] = this.blockY;
		if (this.illuminated) {
			getMaxColorValue(red, green, blue, color);
		}
		float b;
		if (this.illuminated) {
			this.mapsegment.red[0] = color[0];
			this.mapsegment.green[0] = color[1];
			this.mapsegment.blue[0] = color[2];
			b = 1.0f;
		} else {
			this.mapsegment.red[0] = this.blockColor >> 16 & 255;
			this.mapsegment.green[0] = this.blockColor >> 8 & 255;
			this.mapsegment.blue[0] = this.blockColor & 255;
			b = this.mapsegment.liveBrightness[0];
		}
		this.mapsegment.red[0] = (int) (((double) ((float) this.mapsegment.red[0] * b) * this.shadowBlock)
				* (double) this.mapsegment.postBrightness[0]);
		if (this.mapsegment.red[0] > 255) {
			this.mapsegment.red[0] = 255;
		}
		this.mapsegment.green[0] = (int) (((double) ((float) this.mapsegment.green[0] * b) * this.shadowBlock)
				* (double) this.mapsegment.postBrightness[0]);
		if (this.mapsegment.green[0] > 255) {
			this.mapsegment.green[0] = 255;
		}
		this.mapsegment.blue[0] = (int) (((double) ((float) this.mapsegment.blue[0] * b) * this.shadowBlock)
				* (double) this.mapsegment.postBrightness[0]);
		if (!(this.mapsegment.blue[0] <= 255))
			this.mapsegment.blue[0] = 255;

		if (mapX < 0 || mapX >= 16 || mapZ < 0 || mapZ >= 16) {
			return;
		}
		MapChunk chunk = this.mapsegment.loadingBlocks[mapX][mapZ];
		MapSegment segment = chunk.tiles[containerX][containerZ];
		if (segment == null) {
			chunk.tiles[containerX][containerZ] = segment = new MapSegment();
		}
		if ((this.mapsegment.red[0] != 0 || this.mapsegment.green[0] != 0 || this.mapsegment.blue[0] != 0)
				&& (prevSegment == null || prevSegment.red[0][containedX][containedZ] != this.mapsegment.red[0]
						|| prevSegment.green[0][containedX][containedZ] != this.mapsegment.green[0]
						|| prevSegment.blue[0][containedX][containedZ] != this.mapsegment.blue[0])) {
			chunk.updateRequested = true;
		}
		segment.red[0][containedX][containedZ] = this.mapsegment.red[0];
		segment.green[0][containedX][containedZ] = this.mapsegment.green[0];
		segment.blue[0][containedX][containedZ] = this.mapsegment.blue[0];

	}

	public void markForReset() {
		this.toResetImage = true;
	}

	void createFrameBuffers(float scale) {

		if (!Minecraft.getMinecraft().gameSettings.fboEnable) {
			Minecraft.getMinecraft().gameSettings.setOptionValue(GameSettings.Options.FBO_ENABLE, 0);
		}
		this.overlayFrameBuffer = new Framebuffer((int) (512 * scale), (int) (512 * scale), false);
		this.mapFrameBuffer = new Framebuffer((int) (512 * scale), (int) (512 * scale), false);
		this.FBOActive = this.mapFrameBuffer.framebufferObject != -1 && this.overlayFrameBuffer.framebufferObject != -1;

	}

	void renderChunks(EntityPlayer player, int bufferSize, int viewW, boolean retryIfError) {
		double zInsidePixel;
		int zoom = ClientProxy.getHandlerInstance().getZoomFactor();
		int radius = (int) ((double) viewW / Math.sqrt(2.0) / 2.0 / zoom) / 64 + 1;
		double playerX = this.movingObjects.getPlayerOffX((EntityPlayer) player);
		double playerZ = this.movingObjects.getPlayerOffZ((EntityPlayer) player);
		int xFloored = (int) Math.floor(playerX);
		int zFloored = (int) Math.floor(playerZ);
		int playerChunkX = xFloored >> 6;
		int playerChunkZ = zFloored >> 6;
		int offsetX = xFloored & 63;
		int offsetZ = zFloored & 63;
		this.mapFrameBuffer.bindFramebuffer(true);
		GL11.glClear(16640);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		RenderHelper.disableStandardItemLighting();
		GlStateManager.clear(256);
		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		GL11.glPushMatrix();
		GlStateManager.loadIdentity();
		GlStateManager.ortho((double) 0.0, (double) 512.0, (double) 512.0, (double) 0.0, (double) 1000.0,
				(double) 3000.0);
		GlStateManager.matrixMode(GL11.GL_MODELVIEW);
		GL11.glPushMatrix();
		GlStateManager.loadIdentity();
		double xInsidePixel = this.movingObjects.getPlayerOffX((EntityPlayer) player) - (double) xFloored;
		if (xInsidePixel < 0.0) {
			xInsidePixel += 1.0;
		}
		if ((zInsidePixel = this.movingObjects.getPlayerOffZ((EntityPlayer) player) - (double) zFloored) < 0.0) {
			zInsidePixel += 1.0;
		}
		zInsidePixel = 1.0 - zInsidePixel;
		float halfWView = viewW / 2.0f;
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
			int canvasX = X - this.mapsegment.loadedMapChunkX;
			if (canvasX < 0 || canvasX >= this.mapsegment.currentBlocks.length)
				continue;
			for (int Z = minZ; Z <= maxZ; Z++) {
				MapChunk mchunk;
				int canvasZ = Z - this.mapsegment.loadedMapChunkZ;
				if (canvasZ < 0 || canvasZ >= this.mapsegment.currentBlocks.length
						|| (mchunk = this.mapsegment.currentBlocks[canvasX][canvasZ]) == null)
					continue;
				mchunk.bindTexture(0);
				if (mchunk.glTexture[0] == 0)
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

		GlStateManager.translate((float) (halfWView + 0.5f), (float) (511.5f - halfWView), (float) -2000.0f);
		GL11.glPushMatrix();
		GlStateManager.translate((double) ((-xInsidePixel) * zoom), (double) ((-zInsidePixel) * zoom), (double) 0.0);
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
		this.movingObjects.renderPlayerArrow(p);
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

	private void renderDinosaurs(EntityPlayer p, List<RenderDinosaurInfo> loadedDinosaurs, double X, double Z) {

		for (RenderDinosaurInfo dinosaur : loadedDinosaurs) {
			this.movingObjects.renderDinosaurIcon(dinosaur, p, X, Z);
		}

	}

}

class MapChunk {

	public int XCord;
	public int ZCord;
	public int[][] lastHeights = new int[4][16];
	public int[] glTexture = new int[5];
	public ByteBuffer[] chunkBuffer = null;
	public MapSegment[][] tiles = new MapSegment[4][4];
	public boolean[] refreshRequested = new boolean[5];
	private boolean refreshed = false;
	public boolean updateRequested = false;

	public MapChunk(int X, int Z) {
		this.XCord = X;
		this.ZCord = Z;
		this.chunkBuffer = new ByteBuffer[5];
	}

	public void bindTexture(int level) {
		int levelToRefresh = this.getLevelToRefresh(Math.min(level, 0));
		if (levelToRefresh != -1) {
			boolean result = false;
			if (this.glTexture[levelToRefresh] == 0) {
				this.glTexture[levelToRefresh] = GL11.glGenTextures();
				result = true;
			}
			GlStateManager.bindTexture(this.glTexture[levelToRefresh]);
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
					GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) this.chunkBuffer[levelToRefresh]);
			this.refreshRequested[levelToRefresh] = false;
			this.chunkBuffer[levelToRefresh].clear();
		}
		if (this.glTexture[level] != 0) {
			GlStateManager.bindTexture(this.glTexture[level]);
		}
	}

	public int getLevelToRefresh(int currentLevel) {
		if (this.refreshed) {
			return -1;
		}
		if (this.refreshRequested[currentLevel]) {
			return currentLevel;
		}
		this.refreshed = true;
		return -1;
	}

	public void updateBuffers() {
		this.refreshed = true;

		this.refreshRequested[0] = false;
		if (this.chunkBuffer[0] == null)
			this.chunkBuffer[0] = BufferUtils.createByteBuffer((this.tiles.length * this.tiles.length * 850));

		byte[][] bytes = new byte[1][this.tiles.length * this.tiles.length * 850];
		for (int o = 0; o < this.tiles.length; ++o) {
			int offX = o * 16;
			for (int p = 0; p < this.tiles.length; ++p) {
				MapSegment tile = this.tiles[o][p];
				if (tile == null)
					continue;
				int offZ = p * 16;
				for (int z = 0; z < 16; ++z) {
					for (int x = 0; x < 16; ++x) {

						this.storeColour(offX + x, offZ + z, tile.red[0][x][z], tile.green[0][x][z], tile.blue[0][x][z],
								bytes[0], 64);

					}
				}
			}
		}

		this.chunkBuffer[0].put(bytes[0]);
		this.chunkBuffer[0].flip();
		this.refreshRequested[0] = true;

		this.refreshed = false;
	}

	public void storeColour(int x, int y, int red, int green, int blue, byte[] texture, int size) {
		int pos = (y * size + x) * 3;
		texture[pos] = (byte) red;
		texture[++pos] = (byte) green;
		texture[++pos] = (byte) blue;
	}
}

class MapSegmentData {

	int loadingMapChunkX = 0;
	int loadingMapChunkZ = 0;
	int loadedMapChunkX = 0;
	int loadedMapChunkZ = 0;
	int updateChunkX = 0;
	int updateChunkZ = 0;
	int containerX = 0;
	int containerZ = 0;
	int[] red = new int[5];
	int[] green = new int[5];
	int[] blue = new int[5];
	float[] liveBrightness = new float[5];
	float[] postBrightness = new float[5];
	MapChunk previousChunk = null;
	MapChunk[][] currentBlocks = new MapChunk[16][16];
	MapChunk[][] loadingBlocks = new MapChunk[16][16];
}

class MapSegment {

	public int[][][] red = new int[5][16][16];
	public int[][][] green = new int[5][16][16];
	public int[][][] blue = new int[5][16][16];

}