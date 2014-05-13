package nemocraft.hangul;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.common.event.FMLInitializationEvent;

public class CommonProxy
{
	public void init(FMLInitializationEvent event)
	{
		// nothing
	}

	public int getToggleKey()
	{
		return Keyboard.CHAR_NONE;
	}
}
