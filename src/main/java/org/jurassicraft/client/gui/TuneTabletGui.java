package org.jurassicraft.client.gui;

import java.io.IOException;

import org.jurassicraft.JurassiCraft;
import org.jurassicraft.server.message.UpdateChannelMessage;
import org.lwjgl.input.Keyboard;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TuneTabletGui extends GuiScreen
{
	private final EntityPlayer player;
	private GuiTextField guiIDField;
	private GuiButton doneButton;
	private final String id;
	private final byte slot;

	public TuneTabletGui(EntityPlayer player, String id, byte slot)
	{
		super();
		this.player = player;
		this.id = id;
		this.slot = slot;
	}

	@Override
	public void updateScreen()
	{
		super.updateScreen();

		try
		{
			guiIDField.updateCursorCounter();

			if (guiIDField.getText().isEmpty() || guiIDField.getText().equals(this.id))
			{
				doneButton.enabled = false;
			}

			else
			{
				doneButton.enabled = true;
			}
		}

		catch (final NullPointerException e)
		{

		}
	}

	@Override
	public void initGui()
	{
		Keyboard.enableRepeatEvents(true);

		buttonList.clear();
		doneButton = new GuiButton(1, width / 2 - 40, height / 2, 80, 20, "Done");
		doneButton.enabled = false;
		buttonList.add(doneButton);
		guiIDField = new GuiTextField(3, fontRenderer, width / 2 - 100, height / 2 - 50, 200, 20);
		guiIDField.setText(this.id);
		guiIDField.setMaxStringLength(16);
	}

	@Override
	public void onGuiClosed()
	{
		Keyboard.enableRepeatEvents(false);
	}

	@Override
	public boolean doesGuiPauseGame() 
	{
		return false;
	}

	@Override
	protected void actionPerformed(GuiButton guibutton)
	{
		if (guibutton.enabled == false)
		{
			return;
		}

		else if (guibutton == doneButton)
		{

		//	MCA.getPacketHandler().sendPacketToServer(new PacketBabyName(babyNameTextField.getText().trim(), slot));
			JurassiCraft.NETWORK_WRAPPER.sendToServer(new UpdateChannelMessage(this.guiIDField.getText().trim(), this.slot));
			mc.displayGuiScreen(null);
		}

	}

	@Override
	protected void keyTyped(char c, int i)
	{
		
		if (i == 1)
        {
            this.mc.displayGuiScreen((GuiScreen)null);

            if (this.mc.currentScreen == null)
            {
                this.mc.setIngameFocus();
            }
            
         
        }else if (i == 28 || i == 156)
        {
            this.actionPerformed(this.buttonList.get(0));
        }else {
        	if(c != 45)
        	guiIDField.textboxKeyTyped(c, i);
        }
		
		
	}

	@Override
	protected void mouseClicked(int clickX, int clickY, int clicked) throws IOException
	{
		super.mouseClicked(clickX, clickY, clicked);
		guiIDField.mouseClicked(clickX, clickY, clicked);
	}

	@Override
	public void drawScreen(int sizeX, int sizeY, float offset)
	{
		drawDefaultBackground();

			drawCenteredString(fontRenderer, "Adjust tablet channel", width / 2, height / 2 - 80, 0xffffff);
		

		drawString(fontRenderer, "Tablet Channel", width / 2 - 100, height / 2 - 60, 0xa0a0a0);

		guiIDField.drawTextBox();
		super.drawScreen(sizeX, sizeY, offset);
	}
}