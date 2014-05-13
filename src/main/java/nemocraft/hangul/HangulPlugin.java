package nemocraft.hangul;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.SortingIndex;;

/**
 * ASM Transformer 실행을 위한 coremod 플러그인 클래스
 * getASMTransformerClass 메소드만 사용됨
 *
 * @author nemocraft
 */
@SortingIndex(2000)
public class HangulPlugin implements IFMLLoadingPlugin
{
	@Override
	public String[] getASMTransformerClass()
	{
		return new String[] {"nemocraft.hangul.HangulClassTransformer"};
	}

	@Override
	public String getAccessTransformerClass()
	{
		return null;
	}

	@Override
	public String getModContainerClass()
	{
		return null;
	}

	@Override
	public String getSetupClass()
	{
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data)
	{

	}
}
