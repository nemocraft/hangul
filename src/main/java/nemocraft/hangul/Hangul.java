package nemocraft.hangul;

/**
 * 쿼티 영문자를 입력받아 두벌식 한글로 바꿔주는 한글 오토마타 클래스
 *
 * @author nemocraft
 * @since 2013
 */
public class Hangul implements InputMethod
{
	/**
	 * 한글/영문 상태를 기록
	 */
	private static boolean mode = false;

	/**
	 * 현재 상태가 한글 모드인지 확인
	 * @return true일 경우 한글 모드
	 */
	@Override
	public boolean getMode()
	{
		return mode;
	}

	/**
	 * 한글/영문 모드를 전환
	 */
	@Override
	public void toggleMode()
	{
		mode = !mode;
	}

	/**
	 * 한글/영문 전환에 사용되는 키의 코드값을 확인
	 * @return 한/영 전환 키의 코드값
	 */
	@Override
	public int getToggleKey()
	{
		return HangulMod.proxy.getToggleKey();
	}

	/**
	 * 쿼티 영문자 입력을 두벌식 한글 자음, 모음으로 변경
	 * 앞의 26개는 쉬프트를 누르지 않은 상태, 뒤의 26개는 쉬프트를 누른 상태
	 * 숫자가 18보다 작거나 같을 경우 자음, 20보다 크거나 같을 경우 모음
	 */
	private static final int[] qwertyToHangul = new int[]
			{  6, 37,14,11,3, 5, 18,28,22,24,20,40,38,33,21,25,7, 0, 2, 9, 26,17,12,16,32,15,
		6, 37,14,11,4, 5, 18,28,22,24,20,40,38,33,23,27,8, 1, 2, 10,26,17,13,16,32,15};
	// a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z

	// ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ
	// rRseEfaqQtTdwWczxvg
	// 0123456789012345678

	// ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ
	// koiOjpuPh___yn___bm_l
	// _________kol__jpl__l_
	// 012345678901234567890

	// ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ
	// rR_s__ef_______aq_tTdwczxvg (except E,Q,W from first)
	// __t_wg__raqtxvg__t_________
	// 123456789012345678901234567

	/**
	 * 한글 자음, 모음을 입력받아 한글을 조합하기 위한 상태 구현부
	 * @author nemocraft
	 */
	private enum State
	{
		/**
		 * 초기 상태
		 * 자음이 입력되면 초성 상태로 이동
		 * 모음이 입력되면 입력된 글자에 따라 중성 상태로 분기
		 */
		INITIAL {
			@Override
			public State doInput(Hangul outer, int input)
			{
				if (input < 20)
				{
					outer.putFirst(input);
					return FIRST;
				}
				else
				{
					outer.putMid(input);
					return switch_mid(input);
				}
			}
		},
		/**
		 * 초성이 입력된 상태
		 * 자음이 입력되면 조합중이던 글자를 완성하고 초성 상태 유지
		 * 모음이 입력되면 입력된 글자에 따라 중성 상태로 분기
		 */
		FIRST {
			@Override
			public State doInput(Hangul outer, int input)
			{
				if (input < 20)
				{
					outer.commit();
					outer.putFirst(input);
					return FIRST;
				}
				else
				{
					outer.putMid(input);
					return switch_mid(input);
				}
			}
		},
		/**
		 * 중성이 입력된 상태
		 * 종성으로 들어갈 수 없는 글자(ㄸ,ㅃ,ㅉ)가 오거나 초성이 없을 경우 조합중이던 글자를 완성하고 초성 상태로 이동
		 * 종성으로 들어갈 수 있는 글자가 오면 입력된 글자에 따라 종성 상태로 분기
		 * 모음이 오면 조합중이던 글자를 완성하고 초성 없이 중성 상태 유지
		 */
		MID {
			@Override
			public State doInput(Hangul outer, int input)
			{
				if (input < 20)
				{
					int last = firstToLast[input];
					if (last == 0 || !outer.hasFirst())
					{
						outer.commit();
						outer.putFirst(input);
						return FIRST;
					}
					outer.putLast(input, last);
					return switch_last(input);
				}
				else
				{
					outer.commit();
					outer.putMid(input);
					return switch_mid(input);
				}
			}
		},
		/**
		 * 중성 ㅗ 조합 상태
		 * ㅗ 와 함께 조합될 수 있는 ㅏ,ㅐ,ㅣ 가 올 경우 조합을 하고 중성 상태로 이동
		 * 그 외의 경우는 중성 상태(MID)와 동일
		 */
		MID_O {
			@Override
			public State doInput(Hangul outer, int input)
			{
				switch (input)
				{
				case 20: // ㅗ + ㅏ
					outer.putMidCombine(input, 29);
					return MID;
				case 21: // ㅗ + ㅐ
					outer.putMidCombine(input, 30);
					return MID;
				case 40: // ㅗ + ㅣ
					outer.putMidCombine(input, 31);
					return MID;
				default:
					return MID.doInput(outer, input);
				}
			}
		},
		/**
		 * 중성 ㅜ 조합 상태
		 * ㅜ 와 함께 조합될 수 있는 ㅓ,ㅔ,ㅣ 가 올 경우 조합을 하고 중성 상태로 이동
		 * 그 외의 경우는 중성 상태(MID)와 동일
		 */
		MID_U {
			@Override
			public State doInput(Hangul outer, int input)
			{
				switch (input)
				{
				case 24: // ㅜ + ㅓ
					outer.putMidCombine(input, 34);
					return MID;
				case 25: // ㅜ + ㅔ
					outer.putMidCombine(input, 35);
					return MID;
				case 40: // ㅜ + ㅣ
					outer.putMidCombine(input, 36);
					return MID;
				default:
					return MID.doInput(outer, input);
				}
			}
		},
		/**
		 * 중성 ㅡ 조합 상태
		 * ㅡ 와 함께 조합될 수 있는 ㅣ 가 올 경우 조합을 하고 중성 상태로 이동
		 * 그 외의 경우는 중성 상태(MID)와 동일
		 */
		MID_EU {
			@Override
			public State doInput(Hangul outer, int input)
			{
				switch (input)
				{
				case 40: // ㅡ + ㅣ
					outer.putMidCombine(input, 39);
					return MID;
				default:
					return MID.doInput(outer, input);
				}
			}
		},
		/**
		 * 종성이 입력된 상태
		 * 자음이 입력될 경우 조합을 완성하고 초성 상태로 이동
		 * 모음이 입력될 경우 마지막 입력을 제외한 나머지 입력으로 한글 조합을 완성하고
		 * 남은 마지막 글자를 초성으로 옮긴 다음 중성 상태로 이동
		 */
		LAST {
			@Override
			public State doInput(Hangul outer, int input)
			{
				if (input < 20)
				{
					outer.commit();
					outer.putFirst(input);
					return FIRST;
				}
				else
				{
					outer.commitExceptLastOne();
					outer.putMid(input);
					return switch_mid(input);
				}
			}
		},
		/**
		 * 중성 ㄱ 조합 상태
		 * ㄱ 과 함께 조합될 수 있는 ㅅ 이 올 경우 조합을 하고 종성 상태로 이동
		 * 그 외의 경우는 종성 상태(LAST)와 동일
		 */
		LAST_G {
			@Override
			public State doInput(Hangul outer, int input)
			{
				switch (input)
				{
				case 9: // ㄱ + ㅅ
					outer.putLastCombine(input, 3);
					return LAST;
				default:
					return LAST.doInput(outer, input);
				}
			}
		},
		/**
		 * 중성 ㄴ 조합 상태
		 * ㄴ 과 함께 조합될 수 있는 ㅈ,ㅎ 이 올 경우 조합을 하고 종성 상태로 이동
		 * 그 외의 경우는 종성 상태(LAST)와 동일
		 */
		LAST_N {
			@Override
			public State doInput(Hangul outer, int input)
			{
				switch (input)
				{
				case 12: // ㄴ + ㅈ
					outer.putLastCombine(input, 5);
					return LAST;
				case 18: // ㄴ + ㅎ
					outer.putLastCombine(input, 6);
					return LAST;
				default:
					return LAST.doInput(outer, input);
				}
			}
		},
		/**
		 * 중성 ㄹ 조합 상태
		 * ㄹ 과 함께 조합될 수 있는 ㄱ,ㅁ,ㅂ,ㅅ,ㅌ,ㅍ,ㅎ 이 올 경우 조합을 하고 종성 상태로 이동
		 * 그 외의 경우는 종성 상태(LAST)와 동일
		 */
		LAST_R {
			@Override
			public State doInput(Hangul outer, int input)
			{
				switch (input)
				{
				case 0: // ㄹ + ㄱ
					outer.putLastCombine(input, 9);
					return LAST;
				case 6: // ㄹ + ㅁ
					outer.putLastCombine(input, 10);
					return LAST;
				case 7: // ㄹ + ㅂ
					outer.putLastCombine(input, 11);
					return LAST;
				case 9: // ㄹ + ㅅ
					outer.putLastCombine(input, 12);
					return LAST;
				case 16: // ㄹ + ㅌ
					outer.putLastCombine(input, 13);
					return LAST;
				case 17: // ㄹ + ㅍ
					outer.putLastCombine(input, 14);
					return LAST;
				case 18: // ㄹ + ㅎ
					outer.putLastCombine(input, 15);
					return LAST;
				default:
					return LAST.doInput(outer, input);
				}
			}
		},
		/**
		 * 중성 ㅂ 조합 상태
		 * ㅂ 과 함께 조합될 수 있는 ㅅ 이 올 경우 조합을 하고 종성 상태로 이동
		 * 그 외의 경우는 종성 상태(LAST)와 동일
		 */
		LAST_B {
			@Override
			public State doInput(Hangul outer, int input)
			{
				switch (input)
				{
				case 9: // ㅂ + ㅅ
					outer.putLastCombine(input, 18);
					return LAST;
				default:
					return LAST.doInput(outer, input);
				}
			}
		};

		/**
		 * 자음 입력을 종성으로 변환하기 위한 테이블
		 */
		private static final int[] firstToLast = new int[]
				{1, 2, 4, 7, 0, 8, 16,17,0, 19,20,21,22,0, 23,24,25,26,27};

		/**
		 * 자음, 모음 값을 입력하여 한글 조합을 진행
		 * @param outer Hangul 클래스의 인스턴스(this 사용)
		 * @param input 입력된 글자값
		 * @return 입력을 처리한 상태값
		 */
		public abstract State doInput(Hangul outer, int input);

		/**
		 * 모음 입력 중 추가 조합이 가능한 ㅗ,ㅜ,ㅡ 와 나머지를 구분하기 위한 메소드
		 * @param input 모음 글자값
		 * @return 입력을 처리한 상태값
		 */
		private static State switch_mid(int input)
		{
			switch (input)
			{
			case 28:
				return MID_O;
			case 33:
				return MID_U;
			case 38:
				return MID_EU;
			default:
				return MID;
			}
		}

		/**
		 * 자음 입력 중 종성으로 추가 조합이 가능한 ㄱ,ㄴ,ㄹ,ㅂ 과 나머지를 구분하기 위한 메소드
		 * @param input 자음 글자값
		 * @return 입력을 처리한 상태값
		 */
		private static State switch_last(int input)
		{
			switch (input)
			{
			case 0:
				return LAST_G;
			case 2:
				return LAST_N;
			case 5:
				return LAST_R;
			case 7:
				return LAST_B;
			default:
				return LAST;
			}
		}
	}

	/**
	 * 자음 글자값을 유니코드 글자값으로 변경하기 위한 테이블
	 */
	private static final char[] jaumTable = new char[]
			{0x3131, 0x3132, 0x3134, 0x3137, 0x3138, 0x3139, 0x3141, 0x3142, 0x3143, 0x3145,
		0x3146, 0x3147, 0x3148, 0x3149, 0x314a, 0x314b, 0x314c, 0x314d, 0x314e};
	/**
	 * 모음 글자값을 유니코드 글자값으로 변경하기 위한 테이블
	 */
	private static final char[] moumTable = new char[]
			{0x314f, 0x3150, 0x3151, 0x3152, 0x3153, 0x3154, 0x3155, 0x3156, 0x3157, 0x3158,
		0x3159, 0x315a, 0x315b, 0x315c, 0x315d, 0x315e, 0x315f, 0x3160, 0x3161, 0x3162,
		0x3163};

	/**
	 * 현재 상태를 기록
	 */
	private State state = State.INITIAL;

	/**
	 * 조합중인 입력을 기록
	 */
	private int first, mid1, mid2, midCombined, last1, last2, lastFirst, lastCombined;

	/**
	 * 조합이 완성된 글자
	 */
	private String commited;
	/**
	 * 현재 조합중인 글자
	 */
	private String preedit;

	private void putFirst(int input)
	{
		first = input;
	}

	private void putMid(int input)
	{
		mid1 = input;
		midCombined = input;
	}

	private void putMidCombine(int input, int combined)
	{
		mid2 = input;
		midCombined = combined;
	}

	private void putLast(int input, int last)
	{
		last1 = input;
		lastFirst = last;
		lastCombined = last;
	}

	private void putLastCombine(int input, int combined)
	{
		last2 = input;
		lastCombined = combined;
	}

	private boolean hasFirst()
	{
		return first != -1;
	}

	private void composite()
	{
		if (mid1 == -1)
		{
			if (first == -1)
				preedit = "";
			else
				preedit = Character.toString(jaumTable[first]);
		}
		else
		{
			if (first == -1)
				preedit = Character.toString(moumTable[midCombined - 20]);
			else
			{
				int index = ((first * 21) + (midCombined - 20)) * 28;
				if (last1 != -1)
					index += lastCombined;
				preedit = Character.toString((char)(index + 0xac00));
			}
		}
	}

	private void commit()
	{
		composite();
		commited = commited + preedit;
		first = -1;
		mid1 = -1;
		mid2 = -1;
		last1 = -1;
		last2 = -1;
	}

	private void commitExceptLastOne()
	{
		int next;

		if (last2 == -1)
		{
			next = last1;
			last1 = -1;
		}
		else
		{
			next = last2;
			last2 = -1;
			lastCombined = lastFirst;
		}

		commit();

		first = next;
	}

	/**
	 * 한글 오토마타 객체 생성자
	 * 객체 초기화를 담당
	 */
	public Hangul()
	{
		commited = "";
		state = State.INITIAL;
		first = -1;
		mid1 = -1;
		mid2 = -1;
		last1 = -1;
		last2 = -1;
	}

	/**
	 * 한글 입력 처리
	 * @param key 입력된 글자
	 * @param shift 쉬프트가 눌렸는지 여부
	 * @return 조합된 글자가 있는지 여부
	 */
	@Override
	public boolean input(char key, boolean shift)
	{
		int index = mode ? Character.toLowerCase(key) - 'a' : -1;
		if ((index >= 0) && (index < 26))
		{
			if (shift) index += 26;
			state = state.doInput(this, qwertyToHangul[index]);

			return (commited.length() > 0);
		}
		else
		{
			if (state != State.INITIAL)
				reset();

			commited = commited + key;

			return true;
		}
	}

	/**
	 * 조합중인 입력 1개를 지움
	 * @return 조합중인 입력을 지웠는지 여부, 거짓일 경우 조합중인 입력이 없음
	 */
	@Override
	public boolean delete()
	{
		switch (state)
		{
		case INITIAL:
			return false;
		case FIRST:
			first = -1;
			state = State.INITIAL;
			break;
		case MID:
		case MID_O:
		case MID_U:
		case MID_EU:
			if (mid2 != -1)
			{
				mid2 = -1;
				midCombined = mid1;
				state = State.switch_mid(mid1);
			}
			else
			{
				mid1 = -1;
				state = State.FIRST;
			}
			break;
		case LAST:
		case LAST_G:
		case LAST_N:
		case LAST_R:
		case LAST_B:
			if (last2 != -1)
			{
				last2 = -1;
				lastCombined = lastFirst;
				state = State.switch_last(last1);
			}
			else
			{
				last1 = -1;
				state = State.MID;
			}
			break;
		}

		return true;
	}

	/**
	 * 조합중인 글자를 완성하고 상태를 초기화
	 * @return 이미 초기화된 상태일 경우 거짓
	 */
	@Override
	public boolean reset()
	{
		if (state == State.INITIAL)
			return false;

		commit();

		state = State.INITIAL;
		first = -1;
		mid1 = -1;
		mid2 = -1;
		last1 = -1;
		last2 = -1;

		return true;
	}

	/**
	 * 조합이 완성된 글자를 얻고, 객체 내부의 기록을 삭제
	 * @return 완성된 글자
	 */
	@Override
	public String getCommited()
	{
		String ret = commited;
		commited = "";
		return ret;
	}

	/**
	 * 현재 조합중인 글자를 얻음
	 * @return 조합중인 글자
	 */
	@Override
	public String getPreedit()
	{
		composite();
		return preedit;
	}
}
