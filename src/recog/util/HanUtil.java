package recog.util;

import java.util.Arrays;

/**
 * Created by bdh92123 on 2016-11-11.
 */
public class HanUtil {

    // 유니코드 한글 시작 : 44032, 끝 : 55199
    private static final int BASE_CODE = 44032;
    private static final int CHOSUNG = 588;
    private static final int JUNGSUNG = 28;

    // 초성 리스트. 00 ~ 18
    public static final char[] CHOSUNG_LIST = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ',
            'ㅅ', 'ㅆ', 'ㅇ' , 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    // 중성 리스트. 00 ~ 20
    public static final char[] JUNGSUNG_LIST = {
            'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ',
            'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ', 'ㅙ', 'ㅚ',
            'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ',
            'ㅡ', 'ㅢ', 'ㅣ'
    };

    // 종성 리스트. 00 ~ 27 + 1(1개 없음)
    public static final char[] JONGSUNG_LIST = {
            ' ', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ',
            'ㄹ', 'ㄺ', 'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ',
            'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ',
            'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    public static char combine(char chosung, char jungsung, char jongsung) {
        if(jungsung == jongsung && jungsung == 0) {
            return chosung;
        } else if(chosung == jongsung && chosung == 0) {
            return jungsung;
        } else if(chosung == jungsung && chosung == 0) {
            return jongsung;
        }
        if(jongsung == 0) {
            jongsung = ' ';
        }
        char letter = (char) (BASE_CODE + (Arrays.binarySearch(CHOSUNG_LIST, chosung) * 21 + Arrays.binarySearch(JUNGSUNG_LIST, jungsung)) * 28 + Arrays.binarySearch(JONGSUNG_LIST, jongsung));
        return letter;
    }

    public static boolean isChosung(char letter) {
        return Arrays.binarySearch(CHOSUNG_LIST, letter) >= 0;
    }

    public static boolean isJongsung(char letter) {
        return Arrays.binarySearch(JONGSUNG_LIST, letter) >= 0;
    }

    public static boolean isJungsung(char letter) {
        return Arrays.binarySearch(JUNGSUNG_LIST, letter) >= 0;
    }

    /**
     * 한글 형태 체크
     * 0 : 인식불가
     * 1 : 가
     * 2 : 강
     * 3 : 고
     * 4 : 공
     * 5 : 과
     * 6 : 광
     * @param letter
     * @return
     */
    public static int getHangulType(char letter) {
        if(isChosung(letter)) {
            return 0;
        }

        char jungsung = getJungsung(letter);
        boolean useJongsung = getJongsung(letter) != ' ';
        int jungsungType = 0;

        jungsungType = getJungsungType(jungsung);

        if(useJongsung) {
            if(jungsungType == 0) {
                return 2;
            } else if(jungsungType == 1) {
                return 4;
            } else if(jungsungType == 2) {
                return 6;
            }
        } else {
            if(jungsungType == 0) {
                return 1;
            } else if(jungsungType == 1) {
                return 3;
            } else if(jungsungType == 2) {
                return 5;
            }
        }

        return 0;
    }

    public static int getJungsungType(char jungsung) {
        int jungsungType = 0;

        switch (jungsung) {
            case 'ㅏ':
            case 'ㅐ':
            case 'ㅑ':
            case 'ㅒ':
            case 'ㅓ':
            case 'ㅔ':
            case 'ㅕ':
            case 'ㅖ':
            case 'ㅣ':
                jungsungType = 0;
                break;
            case 'ㅗ':
            case 'ㅛ':
            case 'ㅜ':
            case 'ㅠ':
            case 'ㅡ':
                jungsungType = 1;
                break;
            case 'ㅘ':
            case 'ㅙ':
            case 'ㅚ':
            case 'ㅝ':
            case 'ㅞ':
            case 'ㅟ':
            case 'ㅢ':
                jungsungType = 2;
        }

        return jungsungType;
    }

    public static char getChosung(char letter) {
        int cBase = letter - BASE_CODE;

        int c = cBase / CHOSUNG;
        return CHOSUNG_LIST[c];
    }

    public static char getJungsung(char letter) {
        int cBase = letter - BASE_CODE;

        int c = (cBase % CHOSUNG) / JUNGSUNG;
        return JUNGSUNG_LIST[c];
    }

    public static char getJongsung(char letter) {
        int cBase = letter - BASE_CODE;

        int c = cBase % JUNGSUNG;
        return JONGSUNG_LIST[c];
    }

    public static void main(String[] args) {
        System.out.println(getChosung('봚'));
        System.out.println(getJungsung('봚'));
        System.out.println(getJongsung('봚'));
    }
}