package nemocraft.hangul;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy
{
	/** 한글/영문 전환 키의 설명 및 기본값 */
	public static KeyBinding inputToggle = new KeyBinding("key.hangul", Keyboard.KEY_LCONTROL, "nemocraft.hangul");

	@Override
	public void init(FMLInitializationEvent event)
	{
		ClientRegistry.registerKeyBinding(inputToggle);
	}

	@Override
	public int getToggleKey()
	{
		return inputToggle.getKeyCode();
	}
}
