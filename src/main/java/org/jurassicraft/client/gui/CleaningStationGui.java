package org.jurassicraft.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jurassicraft.server.block.entity.CleaningStationBlockEntity;
import org.jurassicraft.server.container.CleaningStationContainer;

@SideOnly(Side.CLIENT)
public class CleaningStationGui extends GuiContainer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("jurassicraft:textures/gui/cleaning_station.png");

    private final InventoryPlayer playerInventory;
    private CleaningStationBlockEntity tileEntity;

    public CleaningStationGui(InventoryPlayer playerInv, CleaningStationBlockEntity tileEntity) {
        super(new CleaningStationContainer(playerInv, tileEntity));
        this.playerInventory = playerInv;
        this.tileEntity = tileEntity;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String name = I18n.format("container.cleaning_station");
        this.fontRenderer.drawString(name, this.xSize / 2 - this.fontRenderer.getStringWidth(name) / 2, 6, 4210752);
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getUnformattedText(), 8, this.ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);
        int progress;

        if (this.tileEntity.getWaterLevel() > 0) {
            progress = this.getWaterLevel(51);
            this.drawTexturedModalRect(x + 46, y + 18 + 51 - progress, 176, 81 - progress + 1, 14, progress + 1);
        }

        progress = this.getProgress(24);
        this.drawTexturedModalRect(x + 79, y + 34, 176, 14, progress + 1, 16);
    }

    private int getProgress(int scale) {
        int j = this.tileEntity.getField(0);
        int k = this.tileEntity.getField(1);
        return k != 0 && j != 0 ? j * scale / k : 0;
    }

    private int getWaterLevel(int scale) {
        return (int)((this.tileEntity.getWaterLevel() / 200F) * scale);
    }
}
