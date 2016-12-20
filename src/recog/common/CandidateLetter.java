package recog.common;

import recog.util.HanUtil;

/**
 * 후보 한글 정보
 * Created by bdh92123 on 2016-12-01.
 */
public class CandidateLetter {
    private char chosung;
    private char jungsung;
    private char jongsung;
    private int bound1;
    private int bound2;

    public char getChosung() {
        return chosung;
    }

    public void setChosung(char chosung) {
        this.chosung = chosung;
    }

    public char getJungsung() {
        return jungsung;
    }

    public void setJungsung(char jungsung) {
        this.jungsung = jungsung;
    }

    public char getJongsung() {
        return jongsung;
    }

    public void setJongsung(char jongsung) {
        this.jongsung = jongsung;
    }

    public int getBound1() {
        return bound1;
    }

    public void setBound1(int bound1) {
        this.bound1 = bound1;
    }

    public int getBound2() {
        return bound2;
    }

    public void setBound2(int bound2) {
        this.bound2 = bound2;
    }

    public char getLetter() {
        return HanUtil.combine(chosung, jungsung, jongsung);
    }
}
