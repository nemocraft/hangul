package nemocraft.hangul;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ChatAllowedCharacters;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import shedar.mods.ic2.nuclearcontrol.gui.GuiTextArea;

/**
 * IC2 Nuclear Control 모드의 텍스트 카드 메모창(GuiTextArea) 대체 클래스
 * 
 * @author shedar, nemocraft
 */
public class IC2NCGuiTextArea extends GuiTextArea
{
	private final int lineCount;
	private final int maxStringLength = 32;
	private int cursorCounter;
	private int cursorPosition = 0;
	private int cursorLine = 0;
	private boolean isFocused = false;
	private final String[] text;
	private final String[] textResult;

	private final FontRenderer fontRenderer;

	private final int xPos;
	private final int yPos;
	private final int width;
	private final int height;

	/** input method */
	private final InputMethod im;
	private String preedit = "";

	public IC2NCGuiTextArea(FontRenderer fontRenderer, int xPos, int yPos, int width, int height, int lineCount)
	{
		super(fontRenderer, xPos, yPos, width, height, lineCount);

		this.xPos = xPos;
		this.yPos = yPos;
		this.width = width;
		this.height = height;
		this.fontRenderer = fontRenderer;
		this.lineCount = lineCount;
		text = new String[lineCount];
		textResult = new String[lineCount];
		for(int i=0; i<lineCount; i++)
		{
			text[i] = "";
		}

		im = new Hangul();
	}

	@Override
	public String[] getText()
	{
		if (preedit.isEmpty())
			return text;

		for(int i=0; i<lineCount; i++)
		{
			if (i == cursorLine)
			{
				String prev = text[cursorLine].substring(0, cursorPosition);
				String next = text[cursorLine].substring(cursorPosition);
				textResult[cursorLine] = prev + preedit + next;
			}
			else
			{
				textResult[i] = text[i];
			}
		}

		return textResult;
	}

	@Override
	public void drawTextBox()
	{
		drawRect(xPos - 1, yPos - 1, xPos + width + 1, yPos + height + 1, 0xFFA0A0A0);
		drawRect(xPos, yPos, xPos + width, yPos + height, 0xFF000000);
		int textColor = 0xE0E0E0;

		int textLeft = xPos + 4;
		int textTop = yPos + (height - lineCount*(fontRenderer.FONT_HEIGHT+1)) / 2;

		for(int i=0; i<lineCount; i++)
		{
			if (i == cursorLine && !preedit.isEmpty())
			{
				String prev = text[cursorLine].substring(0, cursorPosition);
				String next = text[cursorLine].substring(cursorPosition);
				fontRenderer.drawStringWithShadow(prev + preedit + next, textLeft, textTop + (fontRenderer.FONT_HEIGHT + 1) * i, textColor);
			}
			else
			{
				fontRenderer.drawStringWithShadow(text[i], textLeft, textTop + (fontRenderer.FONT_HEIGHT + 1) * i, textColor);
			}
		}
		textTop+=(fontRenderer.FONT_HEIGHT + 1)*cursorLine;
		int cursorPositionX = textLeft + fontRenderer.getStringWidth(text[cursorLine].substring(0, Math.min(text[cursorLine].length(), cursorPosition)))-1;
		boolean drawCursor = isFocused && cursorCounter / 6 % 2 == 0;
		if(drawCursor)
		{
			if (preedit.length() > 0)
			{
				int preeditWidth = this.fontRenderer.getStringWidth(preedit);
				this.drawCursorVertical(cursorPositionX, textTop - 1, cursorPositionX + preeditWidth, textTop + 1 + this.fontRenderer.FONT_HEIGHT);
			}
			else
			{
				drawCursorVertical(cursorPositionX, textTop - 1, cursorPositionX+1, textTop + 1 + fontRenderer.FONT_HEIGHT);
			}
		}

		String modeText = im.getMode() ? "\ud55c" : "\uc601";
		int modeSize = fontRenderer.FONT_HEIGHT;
		int modeXPos = xPos + width - modeSize - 2;
		int modeYPos = yPos + height - modeSize - 2;

		drawRect(modeXPos, modeYPos, modeXPos + modeSize, modeYPos + modeSize, 0xFF0000FF);
		this.fontRenderer.drawString(modeText, modeXPos + 1, modeYPos, 0xFFFFFFFF);
	}

	private void drawCursorVertical(int left, int top, int right, int bottom)
	{
		int var5;

		if (left < right)
		{
			var5 = left;
			left = right;
			right = var5;
		}

		if (top < bottom)
		{
			var5 = top;
			top = bottom;
			bottom = var5;
		}

		Tessellator var6 = Tessellator.instance;
		GL11.glColor4f(0.0F, 0.0F, 255.0F, 255.0F);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_COLOR_LOGIC_OP);
		GL11.glLogicOp(GL11.GL_OR_REVERSE);
		var6.startDrawingQuads();
		var6.addVertex(left, bottom, 0.0D);
		var6.addVertex(right, bottom, 0.0D);
		var6.addVertex(right, top, 0.0D);
		var6.addVertex(left, top, 0.0D);
		var6.draw();
		GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
	}

	@Override
	public void setCursorPosition(int x, int y)
	{
		if (im.reset())
		{
			this.writeText(im.getCommited());
			preedit = "";
		}

		if(y >= text.length)
			y = text.length - 1;
		cursorPosition = x;
		cursorLine = y;
		int lineLength = text[y].length();

		if (cursorPosition < 0)
		{
			cursorPosition = 0;
		}

		if (this.cursorPosition > lineLength)
		{
			this.cursorPosition = lineLength;
		}
	}

	@Override
	public void deleteFromCursor(int count)
	{
		if (((count == 1) || (count == -1)) && im.delete())
		{
			preedit = im.getPreedit();
		}
		else if (text[cursorLine].length() != 0)
		{
			if (im.reset())
			{
				this.writeText(im.getCommited());
				preedit = "";
			}

			boolean back = count < 0;
			String curLine =  text[cursorLine];
			int left = back ? cursorPosition + count : cursorPosition;
			int right = back ? cursorPosition : cursorPosition + count;
			String newLine = "";

			if (left >= 0)
			{
				newLine = curLine.substring(0, left);
			}

			if (right < curLine.length())
			{
				newLine = newLine + curLine.substring(right);
			}
			text[cursorLine] = newLine;
			if(back)
			{
				setCursorPosition(cursorPosition + count, cursorLine);
			}
		}
	}

	@Override
	public void writeText(String additionalText)
	{
		String newLine = "";
		String filteredText = ChatAllowedCharacters.filerAllowedCharacters(additionalText);
		int freeCharCount = this.maxStringLength - text[cursorLine].length();

		if (text[cursorLine].length() > 0)
		{
			newLine = newLine + text[cursorLine].substring(0, cursorPosition);
		}

		if (freeCharCount < filteredText.length())
		{
			newLine = newLine + filteredText.substring(0, freeCharCount);
		}
		else
		{
			newLine = newLine + filteredText;
		}

		int nextCursor = newLine.length();

		if (text[cursorLine].length() > 0 && cursorPosition < text[cursorLine].length())
		{
			newLine = newLine + text[cursorLine].substring(cursorPosition);
		}

		text[cursorLine] = newLine;
		cursorPosition = nextCursor;
	}

	private void setCursorLine(int delta)
	{
		if (im.reset())
		{
			this.writeText(im.getCommited());
			preedit = "";
		}

		int newCursorLine = cursorLine + delta;
		if(newCursorLine < 0)
			newCursorLine = 0;
		if(newCursorLine >= lineCount)
			newCursorLine = lineCount - 1;
		cursorPosition = Math.min(cursorPosition, text[newCursorLine].length());
		cursorLine = newCursorLine;
	}

	private void deleteLine()
	{
		if (im.reset())
		{
			im.getCommited();
			preedit = "";
		}

		for (int i = cursorLine + 1; i < lineCount; i++)
		{
			text[i - 1] = text[i];
		}

		text[lineCount - 1] = "";

		cursorPosition = 0;
	}

	private void insertLine()
	{
		if (im.reset())
		{
			this.writeText(im.getCommited());
			preedit = "";
		}

		if (cursorLine >= lineCount)
		{
			cursorLine = lineCount - 1;
			cursorPosition = 0;
			return;
		}

		String prev = text[cursorLine].substring(0, cursorPosition);
		String next = text[cursorLine].substring(cursorPosition);
		text[cursorLine] = prev;
		cursorPosition = 0;

		if (cursorLine == lineCount - 1)
		{
			return;
		}

		cursorLine++;

		for (int i = lineCount - 1; i > cursorLine; --i)
		{
			text[i] = text[i - 1];
		}

		text[cursorLine] = next;
	}

	@Override
	public void mouseClicked(int x, int y, int par3)
	{
		isFocused = x >= xPos && x < xPos + width && y >= yPos && y < yPos + height;
	}

	@Override
	public boolean isFocused()
	{
		return isFocused;
	}

	@Override
	public void setFocused(boolean focused)
	{
		isFocused = focused;
	}

	@Override
	public boolean textAreaKeyTyped(char par1, int par2)
	{
		if (this.isFocused)
		{
			switch (par1)
			{
			case 1: // Ctrl + A
				setCursorPosition(text[cursorLine].length(), cursorLine);
				return true;
			case 4: // Ctrl + D
				deleteLine();
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
					deleteFromCursor(-1);
					return true;
				case Keyboard.KEY_HOME:
					setCursorPosition(0, cursorLine);
					return true;
				case Keyboard.KEY_LEFT:
					setCursorPosition(cursorPosition-1, cursorLine);
					return true;
				case Keyboard.KEY_RIGHT:
					setCursorPosition(cursorPosition+1, cursorLine);
					return true;
				case Keyboard.KEY_UP:
					setCursorLine(-1);
					return true;
				case Keyboard.KEY_DOWN:
					setCursorLine(1);
					return true;
				case Keyboard.KEY_END:
					setCursorPosition(text[cursorLine].length(), cursorLine);
					return true;
				case Keyboard.KEY_DELETE:
					deleteFromCursor(1);
					return true;
				case Keyboard.KEY_RETURN:
				case Keyboard.KEY_NUMPADENTER:
					insertLine();
					return true;
				default:
					if (ChatAllowedCharacters.isAllowedCharacter(par1))
					{
						//this.writeText(Character.toString(par1));
						if (this.text[cursorLine].length() >= maxStringLength)
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
							if (this.text[cursorLine].length() >= maxStringLength)
							{
								if (im.reset())
								{
									im.getCommited();
									preedit = "";
								}
							}
							else
								preedit = im.getPreedit();
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
}
