package nemocraft.hangul;

import java.lang.reflect.Field;

import net.minecraft.block.Block;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 표지판 내용 수정 GUI(net.minecraft.client.gui.inventory.GuiEditSign) 대체 클래스
 *
 * @author mojang, nemocraft
 */
@SideOnly(Side.CLIENT)
public class MCGuiEditSign extends GuiEditSign
{
	/** Reference to the sign object. */
	private TileEntitySign tileSign;

	private Field editLineField;

	/** input method */
	private final InputMethod im;
	private String preedit = "";
	private String current = "";

	public MCGuiEditSign(TileEntitySign par1TileEntitySign)
	{
		super(par1TileEntitySign);

		this.tileSign = par1TileEntitySign;

		im = new Hangul();
		preedit = "";
		current = "";

		try
		{
			this.editLineField = GuiEditSign.class.getDeclaredField("field_146851_h");
		}
		catch (Exception e)
		{
			this.editLineField = null;
		}

		if (this.editLineField == null)
		{
			try
			{
				this.editLineField = GuiEditSign.class.getDeclaredField("editLine");
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}

		this.editLineField.setAccessible(true);
	}

	private int getEditLine()
	{
		try
		{
			return this.editLineField.getInt(this);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Adds the buttons (and other controls) to the screen in question.
	 */
	@Override
	public void initGui()
	{
		super.initGui();

		if (im.reset())
		{
			im.getCommited();
			preedit = "";
		}

		current = this.tileSign.signText[getEditLine()];
	}

	/**
	 * Fired when a key is typed. This is the equivalent of KeyListener.keyTyped(KeyEvent e).
	 */
	@Override
	protected void keyTyped(char par1, int par2)
	{
		int editLine = getEditLine();

		if (par2 == Keyboard.KEY_UP)
		{
			if (im.reset())
			{
				current = current + im.getCommited();
				preedit = "";
			}
			this.tileSign.signText[editLine] = current;
			super.keyTyped(par1, par2);
			current = this.tileSign.signText[getEditLine()];
		}

		if (par2 == Keyboard.KEY_DOWN || par2 == Keyboard.KEY_RETURN || par2 == Keyboard.KEY_NUMPADENTER)
		{
			if (im.reset())
			{
				current = current + im.getCommited();
				preedit = "";
			}
			this.tileSign.signText[editLine] = current;
			super.keyTyped(par1, par2);
			current = this.tileSign.signText[getEditLine()];
		}

		if (par2 == Keyboard.KEY_BACK && this.tileSign.signText[editLine].length() > 0)
		{
			if (im.delete())
				preedit = im.getPreedit();
			else
				current = current.substring(0, current.length() - 1);

			this.tileSign.signText[editLine] = current + preedit;
		}

		if (ChatAllowedCharacters.isAllowedCharacter(par1) && this.tileSign.signText[editLine].length() < 15)
		{
			if (im.input(par1, isShiftKeyDown()))
				current = current + im.getCommited();
			preedit = im.getPreedit();
			this.tileSign.signText[editLine] = current + preedit;
		}

		if (par2 == Keyboard.KEY_ESCAPE)
		{
			if (im.reset())
			{
				current = current + im.getCommited();
				preedit = "";
			}
			this.tileSign.signText[editLine] = current;
		}

		if (par2 == im.getToggleKey())
		{
			im.toggleMode();
			super.keyTyped(par1, par2);
		}
	}

	/**
	 * Draws the screen and all the components in it.
	 */
	@Override
	public void drawScreen(int par1, int par2, float par3)
	{
		super.drawScreen(par1, par2, par3);

		String modeText = im.getMode() ? "\ud55c" : "\uc601";
		Block block = this.tileSign.getBlockType();
		int modeYPos = (block == Blocks.standing_sign) ? 58 : 87;
		this.fontRendererObj.drawString(modeText, this.width / 2 - 48, modeYPos, 0xFFFFFF);
	}
}
