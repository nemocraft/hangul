package nemocraft.hangul;

import java.util.EnumSet;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.KeyBindingRegistry.KeyHandler;
import cpw.mods.fml.common.TickType;

public class HangulKeyHandler extends KeyHandler
{
	/** 한글/영문 전환 키의 설명 및 기본값 */
	public static KeyBinding inputToggle = new KeyBinding("key.hangul", Keyboard.KEY_LCONTROL);

	public HangulKeyHandler()
	{
		super(new KeyBinding[] { inputToggle });
	}

	@Override
	public String getLabel()
	{
		return "nemocraftHangul";
	}

	@Override
	public void keyDown(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd, boolean isRepeat)
	{
		// nothing
	}

	@Override
	public void keyUp(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd)
	{
		// nothing
	}

	@Override
	public EnumSet<TickType> ticks()
	{
		return null;
	}
}
