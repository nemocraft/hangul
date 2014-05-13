package nemocraft.hangul;

import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * 마인크래프트 한글 입력을 지원하기 위한 포지용 모드
 *
 * @author nemocraft
 * @since 2013
 */
@Mod(modid="nemocrafthangul", version="1.4", acceptableRemoteVersions="*")
public class HangulMod
{
	/** 로그 기록 */
	public static Logger logger;

	@SidedProxy(clientSide = "nemocraft.hangul.ClientProxy", serverSide = "nemocraft.hangul.CommonProxy")
	public static CommonProxy proxy;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		logger = event.getModLog();
	}

	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		proxy.init(event);
	}
}
