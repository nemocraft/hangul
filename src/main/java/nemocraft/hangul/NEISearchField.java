package nemocraft.hangul;

import static codechicken.lib.gui.GuiDraw.*;

import org.lwjgl.input.Keyboard;

import codechicken.nei.SearchField;

/**
 * Not Enough Items 모드의 검색창(SearchField) 대체 클래스
 *
 * @author ChickenBones, nemocraft
 */
public class NEISearchField extends SearchField
{
	/** input method */
	private final InputMethod im;
	private String preedit = "";

	public NEISearchField(String ident)
	{
		super(ident);
		im = new Hangul();
	}

	@Override
	public void draw(int mousex, int mousey)
	{
		drawBox();

		String modeText = im.getMode() ? "\ud55c" : "\uc601";
		int modeXPos = x + 4;
		int modeYPos = y + (h + 1) / 2 - 3;
		int modeSize = fontRenderer.FONT_HEIGHT;

		drawRect(modeXPos, modeYPos, modeSize, modeSize, 0xFF0000FF);
		drawString(modeText, modeXPos + 1, modeYPos, 0xFFFFFFFF);

		String drawtext = text();
		int textWidth = getStringWidth(drawtext);
		int textx = centered ? x+(w-textWidth-modeSize-2)/2 + modeSize + 2 : x + 4 + modeSize + 2;
		int texty = y + (h + 1) / 2 - 3;

		if(drawtext.length() > getMaxTextLength())
		{
			int startOffset = drawtext.length() - getMaxTextLength();
			if(startOffset < 0 || startOffset > drawtext.length())
				startOffset = 0;
			drawtext = drawtext.substring(startOffset);
		}

		if(focused() && (cursorCounter / 6) % 2 == 0)
			drawtext = drawtext + '_';

		drawString(drawtext, textx, texty, getTextColour());
	}

	@Override
	public boolean handleKeyPress(int keyID, char keyChar)
	{
		if(!focused())
			return false;

		if(keyID == Keyboard.KEY_BACK)
		{
			if (im.delete())
			{
				preedit = im.getPreedit();
				backdowntime = System.currentTimeMillis();
				onTextChange(text());
				return true;
			}
			// im.delete()가 실패할 경우 상위 클래스 메소드 호출
		}
		else if(keyID == Keyboard.KEY_RETURN || keyID == Keyboard.KEY_ESCAPE || keyChar == 22) // Ctrl + V
		{
			if (im.reset())
			{
				String input = super.text() + im.getCommited();
				if(isValid(input))
					super.setText(input);
				preedit = "";
			}
			// 상위 클래스 메소드 호출
		}
		else if(keyID == im.getToggleKey())
		{
			im.toggleMode();
			return true;
		}
		else if(isValid(Character.toString(keyChar)))
		{
			boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
			if (im.input(keyChar, shift))
				super.setText(super.text() + im.getCommited());
			preedit = im.getPreedit();
			onTextChange(text());
			return true;
		}

		return super.handleKeyPress(keyID, keyChar);
	}

	@Override
	public void setText(String s)
	{
		if(im.reset())
		{
			im.getCommited();
			preedit = "";
		}
		super.setText(s);
	}

	@Override
	public String text()
	{
		return super.text() + preedit;
	}

	@Override
	public void loseFocus()
	{
		if(im.reset())
		{
			String input = super.text() + im.getCommited();
			if(isValid(input))
				super.setText(input);
			preedit = "";
		}
		super.loseFocus();
	}

	@Override
	public void gainFocus()
	{
		super.gainFocus();
		if(im.reset())
		{
			String input = super.text() + im.getCommited();
			if(isValid(input))
				super.setText(input);
			preedit = "";
		}
	}

	private int getMaxTextLength()
	{
		return w / 6 - 2;
	}
}
