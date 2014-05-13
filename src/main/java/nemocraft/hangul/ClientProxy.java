package nemocraft.hangul;

import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy
{

	@Override
	public void init(FMLInitializationEvent event)
	{
		KeyBindingRegistry.registerKeyBinding(new HangulKeyHandler());
	}

	@Override
	public int getToggleKey()
	{
		return HangulKeyHandler.inputToggle.keyCode;
	}
}
