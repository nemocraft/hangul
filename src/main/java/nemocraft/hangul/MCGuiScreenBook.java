package nemocraft.hangul;

import java.lang.reflect.Field;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.EnumChatFormatting;

/**
 * 책 작성 GUI(net.minecraft.client.gui.GuiScreenBook) 대체 클래스
 *
 * @author mojang, nemocraft
 */
@SideOnly(Side.CLIENT)
public class MCGuiScreenBook extends GuiScreenBook
{
	/** Whether the book is signed or can still be edited */
	private final boolean bookIsUnsigned;
	private boolean editingTitle;
	private int bookImageWidth = 192;
	private int currPage;
	private NBTTagList bookPages;

	Field currPageField;
	Field bookTitleField;
	Field bookModifiedField;

	/** input method */
	private final InputMethod im;
	private String preedit = "";
	private String current = "";

	public MCGuiScreenBook(EntityPlayer par1EntityPlayer, ItemStack par2ItemStack, boolean par3)
	{
		super(par1EntityPlayer, par2ItemStack, par3);

		this.bookIsUnsigned = par3;

		im = new Hangul();
		preedit = "";
		current = "";

		try
		{
			Field field = GuiScreenBook.class.getDeclaredField("field_74177_s");
			field.setAccessible(true);
			this.bookPages = (NBTTagList) field.get(this);
			field.setAccessible(false);

			currPageField = GuiScreenBook.class.getDeclaredField("field_74178_r");
			bookTitleField = GuiScreenBook.class.getDeclaredField("field_74176_t");
			bookModifiedField = GuiScreenBook.class.getDeclaredField("field_74166_d");
		}
		catch (Exception e)
		{
			this.bookPages = null;
		}

		if (this.bookPages == null)
		{
			try
			{
				Field field = GuiScreenBook.class.getDeclaredField("bookPages");
				field.setAccessible(true);
				this.bookPages = (NBTTagList) field.get(this);
				field.setAccessible(false);

				currPageField = GuiScreenBook.class.getDeclaredField("currPage");
				bookTitleField = GuiScreenBook.class.getDeclaredField("bookTitle");
				bookModifiedField = GuiScreenBook.class.getDeclaredField("bookModified");
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}

		currPageField.setAccessible(true);
		bookTitleField.setAccessible(true);
		bookModifiedField.setAccessible(true);
	}

	@Override
	public void initGui()
	{
		super.initGui();

		if (im.reset())
		{
			im.getCommited();
			preedit = "";
		}

		if (this.bookIsUnsigned)
		{
			try
			{
				this.currPage = this.currPageField.getInt(this);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}

			current = this.getText();
		}
	}

	@Override
	protected void actionPerformed(GuiButton p_146284_1_)
	{
		if (p_146284_1_.enabled)
		{
			if (p_146284_1_.id == 3 && this.bookIsUnsigned)
			{
				this.editingTitle = true;
				current = getBookTitle();
			}
			else if (p_146284_1_.id == 4 && this.editingTitle)
			{
				this.editingTitle = false;
			}

			if (this.bookIsUnsigned && im.reset())
			{
				im.getCommited();
				preedit = "";
			}

			super.actionPerformed(p_146284_1_);

			if (((p_146284_1_.id == 1) || (p_146284_1_.id == 2) || (p_146284_1_.id == 4)) && this.bookIsUnsigned && !this.editingTitle)
			{
				try
				{
					this.currPage = this.currPageField.getInt(this);
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}

				this.current = this.getText();
			}
		}
	}

	private String getText()
	{
		return this.bookPages != null && this.currPage >= 0 && this.currPage < this.bookPages.tagCount() ? this.bookPages.tagAt(this.currPage).toString() : "";
	}

	private void setText(String par1Str)
	{
		if (this.bookPages != null && this.currPage >= 0 && this.currPage < this.bookPages.tagCount())
		{
			NBTTagString nbttagstring = (NBTTagString) this.bookPages.tagAt(this.currPage);
			nbttagstring.data = par1Str;

			try
			{
				this.bookModifiedField.setBoolean(this, true);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
	}

	private String getBookTitle()
	{
		try
		{
			return (String) this.bookTitleField.get(this);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	private void setBookTitle(String title)
	{
		try
		{
			this.bookTitleField.set(this, title);
			this.bookModifiedField.setBoolean(this, true);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Fired when a key is typed. This is the equivalent of KeyListener.keyTyped(KeyEvent e).
	 */
	@Override
	protected void keyTyped(char par1, int par2)
	{
		if (!this.bookIsUnsigned)
		{
			super.keyTyped(par1, par2);
			return;
		}

		// from GuiScreen class
		if (par2 == 1)
		{
			this.mc.displayGuiScreen((GuiScreen)null);
			this.mc.setIngameFocus();
			return;
		}

		if (par1 != 22 && par2 == im.getToggleKey())
		{
			im.toggleMode();
		}
		else if (this.editingTitle)
		{
			if (keyTypedInTitle(par1, par2))
			{
				switch (par2)
				{
				case Keyboard.KEY_BACK:
					super.keyTyped(par1, par2);
					current = getBookTitle();
					break;
				case Keyboard.KEY_RETURN:
				case Keyboard.KEY_NUMPADENTER:
					super.keyTyped(par1, par2);
					break;
				default:
					this.setBookTitle(current + preedit + " ");
					super.keyTyped(par1, Keyboard.KEY_BACK);
				}
			}
		}
		else
		{
			if (keyTypedInBook(par1, par2))
			{
				super.keyTyped(par1, par2);
			}
		}
	}

	/**
	 * Processes keystrokes when editing the text of a book
	 */
	private boolean keyTypedInBook(char par1, int par2)
	{
		switch (par1)
		{
		case 22: // Ctrl + V
			if (im.reset())
			{
				current = current + im.getCommited();
				preedit = "";
				this.setText(current);
			}
			return true;
		default:
			switch (par2)
			{
			case Keyboard.KEY_BACK:
				if (im.delete())
				{
					preedit = im.getPreedit();
					this.setText(current + preedit);
				}
				else
				{
					if (current.length() > 0)
					{
						current = current.substring(0, current.length() - 1);
						this.setText(current);
					}
				}

				break;
			case Keyboard.KEY_RETURN:
			case Keyboard.KEY_NUMPADENTER:
				if (im.reset())
				{
					current = current + im.getCommited();
					preedit = "";
				}
				if (current.length() < 256)
				{
					current = current + "\n";
				}
				this.setText(current);
				break;
			default:
				if (ChatAllowedCharacters.isAllowedCharacter(par1))
				{
					if (im.input(par1, isShiftKeyDown()))
					{
						current = current + im.getCommited();
					}
					int i = this.fontRenderer.splitStringWidth(current + "" + EnumChatFormatting.BLACK + "_", 118);

					if (i <= 118 && current.length() < 256)
					{
						preedit = im.getPreedit();
					}
					else
					{
						if (im.reset())
						{
							im.getCommited();
						}
						preedit = "";
					}
					this.setText(current + preedit);
				}
			}
		}

		return false;
	}

	private boolean keyTypedInTitle(char par1, int par2)
	{
		switch (par2)
		{
		case Keyboard.KEY_BACK:
			if (im.delete())
			{
				preedit = im.getPreedit();
				setBookTitle(current + preedit);
			}
			else
			{
				return true;
			}

			break;
		case Keyboard.KEY_RETURN:
		case Keyboard.KEY_NUMPADENTER:
			if (im.reset())
			{
				current = current + im.getCommited();
				preedit = "";
				this.setBookTitle(current);
			}
			return true;
		default:
			if (current.length() < 16 && ChatAllowedCharacters.isAllowedCharacter(par1))
			{
				if (im.input(par1, isShiftKeyDown()))
				{
					current = current + im.getCommited();
				}

				if (current.length() < 16)
				{
					preedit = im.getPreedit();
				}
				else
				{
					if (im.reset())
					{
						im.getCommited();
					}
					preedit = "";
				}

				this.setBookTitle(current + preedit);
				return true;
			}
		}

		return false;
	}

	/**
	 * Draws the screen and all the components in it.
	 */
	public void drawScreen(int par1, int par2, float par3)
	{
		super.drawScreen(par1, par2, par3);

		if (this.bookIsUnsigned)
		{
			int k = (this.width - this.bookImageWidth) / 2;
			byte b0 = 2;

			String modeText = im.getMode() ? "\ud55c" : "\uc601";
			this.fontRenderer.drawString(modeText, k + 36, b0 + 16, 0);
		}
	}
}
