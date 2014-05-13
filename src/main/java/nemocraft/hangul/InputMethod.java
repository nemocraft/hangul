package nemocraft.hangul;

/**
 * 입력기 구현을 위한 기본 인터페이스
 *
 * @author nemocraft
 * @since 2013
 */
public interface InputMethod
{
	/**
	 * 현재 입력 상태를 확인
	 * @return true일 경우 한글 모드
	 */
	public boolean getMode();

	/**
	 * 입력 모드를 전환
	 */
	public void toggleMode();

	/**
	 * 입력 모드 전환에 사용되는 키의 코드값을 확인
	 * @return 모드 전환 키의 코드값
	 */
	public int getToggleKey();

	/**
	 * 키보드로 입력받은 글자를 전달하여 입력기 실행
	 * @param key 입력된 글자
	 * @param shift 쉬프트가 눌렸는지 여부
	 * @return 조합된 글자가 있는지 여부
	 */
	public boolean input(char key, boolean shift);

	/**
	 * 조합중인 입력 1개를 지움
	 * @return 조합중인 입력을 지웠는지 여부, 거짓일 경우 조합중인 입력이 없음
	 */
	public boolean delete();

	/**
	 * 조합중인 글자를 완성하고 상태를 초기화
	 * @return 이미 초기화된 상태일 경우 거짓
	 */
	public boolean reset();

	/**
	 * 조합이 완성된 글자를 얻고, 객체 내부의 기록을 삭제
	 * @return 완성된 글자
	 */
	public String getCommited();

	/**
	 * 현재 조합중인 글자를 얻음
	 * @return 조합중인 글자
	 */
	public String getPreedit();
}
