package nemocraft.hangul;

import java.lang.reflect.Field;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ChatAllowedCharacters;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 마인크래프트 기본 GUI의 TextField 대체 클래스
 *
 * @author mojang, nemocraft
 */
@SideOnly(Side.CLIENT)
public class MCGuiTextField extends GuiTextField
{
	/**
	 * Have the font renderer from GuiScreen to render the textbox text into the screen.
	 */
	private final FontRenderer fontRenderer;
	private final int xPos;
	private final int yPos;

	/** The width of this text field. */
	private final int width;
	private final int height;

	private int cursorCounter;

	/**
	 * If this value is true along with isEnabled, keyTyped will process the keys.
	 */
	private boolean isFocused;
	/**
	 * If this value is true along with isFocused, keyTyped will process the keys.
	 */
	private boolean isEnabled = true;
	/**
	 * The current character index that should be used as start of the rendered text.
	 */
	private int lineScrollOffset;
	private Field lineScrollOffsetField;

	private int enabledColor = 14737632;
	private int disabledColor = 7368816;

	/** input method */
	private final InputMethod im;
	private String preedit = "";

	public MCGuiTextField(FontRenderer par1FontRenderer, int par2, int par3, int par4, int par5)
	{
		super(par1FontRenderer, par2, par3, par4, par5);

		this.fontRenderer = par1FontRenderer;
		this.xPos = par2;
		this.yPos = par3;
		this.width = par4;
		this.height = par5;

		im = new Hangul();
		preedit = "";

		try
		{
			this.lineScrollOffsetField = GuiTextField.class.getDeclaredField("field_146225_q");
		}
		catch (Exception e)
		{
			this.lineScrollOffsetField = null;
		}

		if (this.lineScrollOffsetField == null)
		{
			try
			{
				this.lineScrollOffsetField = GuiTextField.class.getDeclaredField("lineScrollOffset");
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}

		this.lineScrollOffsetField.setAccessible(true);
	}

	/**
	 * Increments the cursor counter
	 */
	public void updateCursorCounter()
	{
		++this.cursorCounter;
	}

	/**
	 * Sets the text of the textbox.
	 */
	@Override
	public void setText(String par1Str)
	{
		if (im.reset())
		{
			im.getCommited();
			preedit = "";
		}

		super.setText(par1Str);
	}

	/**
	 * Returns the text beign edited on the textbox.
	 */
	@Override
	public String getText()
	{
		String fullText = super.getText();
		if (preedit.length() > 0)
		{
			int cursorPosition = getCursorPosition();
			fullText = fullText.substring(0, cursorPosition) + preedit + fullText.substring(cursorPosition);
		}
		return fullText;
	}

	/**
	 * Deletes the specified number of words starting at the cursor position. Negative numbers will delete words left of
	 * the cursor.
	 */
	@Override
	public void deleteWords(int par1)
	{
		if (im.reset())
		{
			this.writeText(im.getCommited());
			preedit = "";
		}

		super.deleteWords(par1);
	}

	/**
	 * delete the selected text, otherwsie deletes characters from either side of the cursor. params: delete num
	 */
	@Override
	public void deleteFromCursor(int par1)
	{
		if (((par1 == 1) || (par1 == -1)) && im.delete())
		{
			preedit = im.getPreedit();
		}
		else
		{
			if (im.reset())
			{
				this.writeText(im.getCommited());
				preedit = "";
			}

			super.deleteFromCursor(par1);
		}
	}

	/**
	 * Call this method from you GuiScreen to process the keys into textbox.
	 */
	@Override
	public boolean textboxKeyTyped(char par1, int par2)
	{
		if (this.isEnabled && this.isFocused)
		{
			switch (par1)
			{
			case 1: // Ctrl + A
				if (im.reset())
				{
					this.writeText(im.getCommited());
					preedit = "";
				}
				this.setCursorPositionEnd();
				this.setSelectionPos(0);
				return true;
			case 3: // Ctrl + C
				GuiScreen.setClipboardString(this.getSelectedText());
				return true;
			case 22: // Ctrl + V
				this.writeText(GuiScreen.getClipboardString());
				return true;
			case 24: // Ctrl + X
				GuiScreen.setClipboardString(this.getSelectedText());
				this.writeText("");
				return true;
			default:
				if (par2 == im.getToggleKey())
				{
					im.toggleMode();
					return true;
				}
				switch (par2)
				{
				case Keyboard.KEY_BACK:
					if (GuiScreen.isCtrlKeyDown())
					{
						this.deleteWords(-1);
					}
					else
					{
						this.deleteFromCursor(-1);
					}

					return true;
				case Keyboard.KEY_HOME:
					if (im.reset())
					{
						this.writeText(im.getCommited());
						preedit = "";
					}
					if (GuiScreen.isShiftKeyDown())
					{
						this.setSelectionPos(0);
					}
					else
					{
						this.setCursorPositionZero();
					}

					return true;
				case Keyboard.KEY_LEFT:
					if (im.reset())
					{
						this.writeText(im.getCommited());
						preedit = "";
					}
					if (GuiScreen.isShiftKeyDown())
					{
						if (GuiScreen.isCtrlKeyDown())
						{
							this.setSelectionPos(this.getNthWordFromPos(-1, this.getSelectionEnd()));
						}
						else
						{
							this.setSelectionPos(this.getSelectionEnd() - 1);
						}
					}
					else if (GuiScreen.isCtrlKeyDown())
					{
						this.setCursorPosition(this.getNthWordFromCursor(-1));
					}
					else
					{
						this.moveCursorBy(-1);
					}

					return true;
				case Keyboard.KEY_RIGHT:
					if (im.reset())
					{
						this.writeText(im.getCommited());
						preedit = "";
					}
					if (GuiScreen.isShiftKeyDown())
					{
						if (GuiScreen.isCtrlKeyDown())
						{
							this.setSelectionPos(this.getNthWordFromPos(1, this.getSelectionEnd()));
						}
						else
						{
							this.setSelectionPos(this.getSelectionEnd() + 1);
						}
					}
					else if (GuiScreen.isCtrlKeyDown())
					{
						this.setCursorPosition(this.getNthWordFromCursor(1));
					}
					else
					{
						this.moveCursorBy(1);
					}

					return true;
				case Keyboard.KEY_END:
					if (im.reset())
					{
						this.writeText(im.getCommited());
						preedit = "";
					}
					if (GuiScreen.isShiftKeyDown())
					{
						this.setSelectionPos(super.getText().length());
					}
					else
					{
						this.setCursorPositionEnd();
					}

					return true;
				case Keyboard.KEY_DELETE:
					if (GuiScreen.isCtrlKeyDown())
					{
						this.deleteWords(1);
					}
					else
					{
						this.deleteFromCursor(1);
					}

					return true;
				default:
					if (ChatAllowedCharacters.isAllowedCharacter(par1))
					{
						if (super.getText().length() >= getMaxStringLength())
						{
							if (im.reset())
							{
								this.writeText(im.getCommited());
								preedit = "";
							}
						}
						else
						{
							if (im.input(par1, GuiScreen.isShiftKeyDown()))
								this.writeText(im.getCommited());
							else if (this.getCursorPosition() != this.getSelectionEnd())
								this.writeText("");
							if (super.getText().length() >= getMaxStringLength())
							{
								if (im.reset())
								{
									im.getCommited();
									preedit = "";
								}
							}
							else
							{
								preedit = im.getPreedit();
							}
						}
						return true;
					}
					else
					{
						return false;
					}
				}
			}
		}
		else
		{
			return false;
		}
	}

	/**
	 * Draws the textbox
	 */
	@Override
	public void drawTextBox()
	{
		if (this.getVisible())
		{
			if (this.getEnableBackgroundDrawing())
			{
				drawRect(this.xPos - 1, this.yPos - 1, this.xPos + this.width + 1, this.yPos + this.height + 1, -6250336);
				drawRect(this.xPos, this.yPos, this.xPos + this.width, this.yPos + this.height, -16777216);
			}

			String modeText = im.getMode() ? "\ud55c" : "\uc601";
			int modeXPos = getEnableBackgroundDrawing() ? this.xPos + 4 : this.xPos;
			int modeYPos = getEnableBackgroundDrawing() ? this.yPos + (this.height - 8) / 2 : this.yPos;
			int modeSize = fontRenderer.FONT_HEIGHT;

			drawRect(modeXPos, modeYPos, modeXPos + modeSize, modeYPos + modeSize, 0xFF0000FF);
			this.fontRenderer.drawString(modeText, modeXPos + 1, modeYPos, 0xFFFFFF);

			String fullText = super.getText();
			if (preedit.length() > 0)
			{
				fullText = fullText.substring(0, this.getCursorPosition()) + preedit + fullText.substring(this.getCursorPosition());
			}

			int textColor = this.isEnabled ? this.enabledColor : this.disabledColor;
			int cursorBy = this.getCursorPosition() - this.lineScrollOffset;
			int selectionBy = this.getSelectionEnd() - this.lineScrollOffset;
			String drawText = this.fontRenderer.trimStringToWidth(fullText.substring(this.lineScrollOffset), this.getWidth());

			boolean cursorInDrawText = cursorBy >= 0 && cursorBy <= drawText.length();
			boolean blinkCursor = this.isFocused && this.cursorCounter / 6 % 2 == 0 && cursorInDrawText;
			boolean verticalCursor = preedit.length() > 0 || this.getCursorPosition() < super.getText().length() || super.getText().length() >= this.getMaxStringLength();
			int textXPos = this.getEnableBackgroundDrawing() ? this.xPos + 4 + modeSize + 2: this.xPos + modeSize + 2;
			int textYPos = this.getEnableBackgroundDrawing() ? this.yPos + (this.height - 8) / 2 : this.yPos;
			int nextTextXPos = textXPos;

			if (selectionBy > drawText.length())
			{
				selectionBy = drawText.length();
			}

			if (drawText.length() > 0)
			{
				String s1 = cursorInDrawText ? drawText.substring(0, cursorBy) : drawText;
				nextTextXPos = this.fontRenderer.drawStringWithShadow(s1, textXPos, textYPos, textColor);
			}

			int cursorXPos = nextTextXPos;

			if (!cursorInDrawText)
			{
				cursorXPos = cursorBy > 0 ? textXPos + this.width : textXPos;
			}
			else if (verticalCursor)
			{
				cursorXPos = nextTextXPos - 1;
				--nextTextXPos;
			}

			if (cursorInDrawText)
			{
				this.fontRenderer.drawStringWithShadow(drawText.substring(cursorBy), nextTextXPos, textYPos, textColor);
			}

			if (selectionBy != cursorBy)
			{
				int selectionEndXPos = textXPos + this.fontRenderer.getStringWidth(drawText.substring(0, selectionBy));
				this.drawCursorVertical(cursorXPos, textYPos - 1, selectionEndXPos - 1, textYPos + 1 + this.fontRenderer.FONT_HEIGHT);
			}
			else if (blinkCursor)
			{
				if (preedit.length() > 0)
				{
					int preeditWidth = this.fontRenderer.getStringWidth(preedit);
					this.drawCursorVertical(cursorXPos, textYPos - 1, cursorXPos + preeditWidth, textYPos + 1 + this.fontRenderer.FONT_HEIGHT);
				}
				else if (verticalCursor)
				{
					Gui.drawRect(cursorXPos, textYPos - 1, cursorXPos + 1, textYPos + 1 + this.fontRenderer.FONT_HEIGHT, -3092272);
				}
				else
				{
					this.fontRenderer.drawStringWithShadow("_", cursorXPos, textYPos, textColor);
				}
			}
		}
	}

	/**
	 * draws the vertical line cursor in the textbox
	 */
	private void drawCursorVertical(int par1, int par2, int par3, int par4)
	{
		int i1;

		if (par1 < par3)
		{
			i1 = par1;
			par1 = par3;
			par3 = i1;
		}

		if (par2 < par4)
		{
			i1 = par2;
			par2 = par4;
			par4 = i1;
		}

		Tessellator tessellator = Tessellator.instance;
		GL11.glColor4f(0.0F, 0.0F, 255.0F, 255.0F);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_COLOR_LOGIC_OP);
		GL11.glLogicOp(GL11.GL_OR_REVERSE);
		tessellator.startDrawingQuads();
		tessellator.addVertex(par1, par4, 0.0D);
		tessellator.addVertex(par3, par4, 0.0D);
		tessellator.addVertex(par3, par2, 0.0D);
		tessellator.addVertex(par1, par2, 0.0D);
		tessellator.draw();
		GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
	}

	/**
	 * Sets the text colour for this textbox (disabled text will not use this colour)
	 */
	public void setTextColor(int p_146193_1_)
	{
		this.enabledColor = p_146193_1_;
	}

	public void setDisabledTextColour(int p_146204_1_)
	{
		this.disabledColor = p_146204_1_;
	}

	/**
	 * setter for the focused field
	 */
	@Override
	public void setFocused(boolean par1)
	{
		if (im.reset())
		{
			this.writeText(im.getCommited());
			preedit = "";
		}

		super.setFocused(par1);

		if (par1 && !this.isFocused)
		{
			this.cursorCounter = 0;
		}

		this.isFocused = par1;
	}

	@Override
	public void setEnabled(boolean par1)
	{
		if (im.reset())
		{
			this.writeText(im.getCommited());
			preedit = "";
		}

		super.setEnabled(par1);
		this.isEnabled = par1;
	}

	/**
	 * Sets the position of the selection anchor (i.e. position the selection was started at)
	 */
	public void setSelectionPos(int p_146199_1_)
	{
		super.setSelectionPos(p_146199_1_);

		try
		{
			this.lineScrollOffset = this.lineScrollOffsetField.getInt(this);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}
