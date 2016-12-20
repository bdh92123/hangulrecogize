package recog.service;

import javafx.geometry.Point2D;
import recog.common.CandidateLetter;
import recog.common.Rectangle;
import recog.common.Segment;
import recog.common.SegmentGroup;
import recog.util.HanUtil;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static recog.util.HanUtil.*;

/**
 * 한글 인식기
 * Created by bdh92123 on 2016-11-04.
 */
@SuppressWarnings("ALL")
public class Trainer {
    public static final String JAUM_FILENAME = "jaum.txt";
    public static final String MOUM_FILENAME = "moum.txt";

    private static Map<String, StringBuffer> chainToJaumDb = new TreeMap<>();
    private static Map<String, StringBuffer> chainToMoumDb = new TreeMap<>();
    private static Map<Character, List<String>> jaumToChainDb = new HashMap<Character, List<String>>();
    private static Map<Character, List<String>> moumToChainDb = new HashMap<Character, List<String>>();

    static {
        try {
            for(String file : getResourceFiles("train/")) {
                trainFromCoordinateFile(getResourceAsStream("train/" + file));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        trainFromChainFile(new File(JAUM_FILENAME));
        trainFromChainFile(new File(MOUM_FILENAME));
        System.out.println("학습된 자음 개수 : " + chainToJaumDb.size());
        System.out.println("학습된 모음 개수 : " + chainToMoumDb.size());
    }

    private static List<String> getResourceFiles(String path) throws IOException {
        List<String> filenames = new ArrayList<>();

        try (
            InputStream in = getResourceAsStream( path );
            BufferedReader br = new BufferedReader( new InputStreamReader( in ) ) ) {
            String resource;

            while( (resource = br.readLine()) != null ) {
                filenames.add( resource );
            }
        }

        return filenames;
    }

    private static InputStream getResourceAsStream( String resource ) {
        final InputStream in
                = getContextClassLoader().getResourceAsStream( resource );

        return in == null ? Trainer.class.getResourceAsStream( resource ) : in;
    }

    private static ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * 좌표 문자열 정보로부터 SegmentGroup 추출
     * @param coordString
     * @return
     */
    private static SegmentGroup getSegmentGroupFromCoord(String coordString) {
        Pattern pattern = Pattern.compile("\\((-?[0-9]+),(-?[0-9]+)\\)");
        Matcher matcher = pattern.matcher(coordString);

        Segment prevSegment = null;

        SegmentGroup segmentGroup = new SegmentGroup();

        while(matcher.find()) {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));

            Segment segment = new Segment();
            segment.setX(x);
            segment.setY(y);

            if(x == -1 && y == -1 && segmentGroup.getSegments().size() > 0) {
                prevSegment = null;
                segmentGroup.getSegments().add(null);
                continue;
            } else if(prevSegment != null) {
                prevSegment.setDx(x - prevSegment.getX());
                prevSegment.setDy(y - prevSegment.getY());
                segmentGroup.getSegments().add(prevSegment);
            }

            prevSegment = segment;
        }

        return segmentGroup;
    }

    /**
     * 좌표 정보파일로부터 자모음 학습
     * @param stream
     * @return
     */
    public static boolean trainFromCoordinateFile(InputStream stream) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(stream, "euc-kr"));
            String line = null;
            while((line = br.readLine()) != null) {
                if(line.isEmpty()) {
                    continue;
                }

                char letter = line.charAt(0);
                String coordString = line.substring(1).trim();
                SegmentGroup segmentGroup = getSegmentGroupFromCoord(coordString);
                List<Segment> normalized = normalize(segmentGroup.getSegments());
                segmentGroup = new SegmentGroup(normalized);
                if(segmentGroup.getSegments().get(segmentGroup.getSegments().size() - 1) == null) {
                    segmentGroup.getSegments().remove(segmentGroup.getSegments().size() - 1);
                }

                String chainCode = segmentGroup.toChainCode();
                train(letter, chainCode);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if(br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 좌표 파일로부터 인식률 테스트
     * @param stream
     * @return
     */
    public static int[] testFromCoordinateFile(InputStream stream) {
        BufferedReader br = null;
        int ok = 0, no = 0;
        try {
            br = new BufferedReader(new InputStreamReader(stream, "euc-kr"));
            String line = null;
            while((line = br.readLine()) != null) {
                if(line.isEmpty()) {
                    continue;
                }

                char letter = line.charAt(0);
                String coordString = line.substring(1).trim();
                SegmentGroup segmentGroup = getSegmentGroupFromCoord(coordString);
                List<Segment> normalized = normalize(segmentGroup.getSegments());
                segmentGroup = new SegmentGroup(normalized);
                if(segmentGroup.getSegments().get(segmentGroup.getSegments().size() - 1) == null) {
                    segmentGroup.getSegments().remove(segmentGroup.getSegments().size() - 1);
                }

                String chainCode = segmentGroup.toChainCode();
                List<CandidateLetter> candidate = recognize(segmentGroup);
                if(candidate.size() > 0 && candidate.get(0).getLetter() == letter) {
                    ok++;
                } else {
                    no++;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new int[]{ok, ok + no};
        }
    }

    /**
     * 자모음 학습
     * @param letter
     * @param chainCode
     * @return
     */
    public static boolean train(char letter, String chainCode) {
        if(chainCode.isEmpty() || letter == '\0') {
            return false;
        }

        if(isChosung(letter) || isJongsung(letter)) {
            // 9는 ㅇ과 ㅎ에만 해당되기 때문에 이상한 학습데이터가 오는것을 방지
            if((chainCode.contains("9") && letter != 'ㅇ' && letter != 'ㅎ') || (letter == 'ㅇ' && !chainCode.equals("9"))) {
                return false;
            }
            if(chainToJaumDb.get(chainCode) == null) {
                chainToJaumDb.put(chainCode, new StringBuffer());
            }
            if(chainToJaumDb.get(chainCode).indexOf(String.valueOf(letter)) == -1) {
                chainToJaumDb.get(chainCode).append(letter);
                if(jaumToChainDb.get(letter) == null) {
                    jaumToChainDb.put(letter, new ArrayList<>());
                }
                jaumToChainDb.get(letter).add(chainCode);
            } else
                return false;
        } else if(isJungsung(letter)) {
            if(chainToMoumDb.get(chainCode) == null) {
                chainToMoumDb.put(chainCode, new StringBuffer());
            }
            if(chainToMoumDb.get(chainCode).indexOf(String.valueOf(letter)) == -1) {
                chainToMoumDb.get(chainCode).append(letter);
                if(moumToChainDb.get(letter) == null) {
                    moumToChainDb.put(letter, new ArrayList<>());
                }
                moumToChainDb.get(letter).add(chainCode);
            } else
                return false;
        }

        return true;
    }

    /**
     * 체인코드 파일로부터 학습
     * @param file
     * @return
     */
    public static boolean trainFromChainFile(File file) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "euc-kr"));
            String line = null;
            while((line = br.readLine()) != null) {
                if(line.isEmpty()) {
                    continue;
                }
                String temp[] = line.split(" ");
                char letter = temp[0].charAt(0);
                String chainCode = temp[1];

                train(letter, chainCode);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if(br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 자모음 학습 데이터 파일로 저장
     * @throws IOException
     */
    public static void saveToChainFile() throws IOException {
        StringBuffer jaumDb = new StringBuffer();
        for (Map.Entry<Character, List<String>> entry : jaumToChainDb.entrySet()) {
            Character jaum = entry.getKey();
            for (String chainCode : entry.getValue()) {
                jaumDb.append(jaum).append(' ').append(chainCode).append(System.lineSeparator());
            }
        }

        StringBuffer moumDb = new StringBuffer();
        for (Map.Entry<Character, List<String>> entry : moumToChainDb.entrySet()) {
            Character moum = entry.getKey();
            for (String chainCode : entry.getValue()) {
                moumDb.append(moum).append(' ').append(chainCode).append(System.lineSeparator());
            }
        }

        FileWriter writer = new FileWriter(JAUM_FILENAME);
        writer.write(jaumDb.toString());
        writer.flush();
        writer.close();

        writer = new FileWriter(MOUM_FILENAME);
        writer.write(moumDb.toString());
        writer.flush();
        writer.close();
    }

    /**
     * SegmentGroup로부터 학습
     * @param letter
     * @param segmentGroup
     * @return
     */
    public static boolean train(char letter, SegmentGroup segmentGroup) {
        return train(letter, segmentGroup.toChainCode());
    }

    /**
     * 자음 조합이 가능하면 조합된 복합자음을 리턴
     * @param c1
     * @param c2
     * @param isChosung
     * @return
     */
    private static char combineJaum(char c1, char c2, boolean isChosung) {
        if(c1 == 'ㅂ' && c2 == 'ㅂ') {
            return 'ㅃ';
        } else if(c1 == 'ㅈ' && c2 == 'ㅈ') {
            return 'ㅉ';
        } else if(c1 == 'ㄷ' && c2 == 'ㄷ') {
            return 'ㄸ';
        } else if(c1 == 'ㄱ' && c2 == 'ㄱ') {
            return 'ㄲ';
        } else if(c1 == 'ㅅ' && c2 == 'ㅅ') {
            return 'ㅆ';
        }
        if(isChosung) {
            return 0;
        }
        if(c1 == 'ㄱ' && c2 == 'ㅅ') {
            return 'ㄳ';
        } else if(c1 == 'ㄴ' && c2 == 'ㅈ') {
            return 'ㄵ';
        } else if(c1 == 'ㄴ' && c2 == 'ㅎ') {
            return 'ㄶ';
        } else if(c1 == 'ㄹ' && c2 == 'ㄱ') {
            return 'ㄺ';
        } else if(c1 == 'ㄹ' && c2 == 'ㅁ') {
            return 'ㄻ';
        } else if(c1 == 'ㄹ' && c2 == 'ㅂ') {
            return 'ㄼ';
        } else if(c1 == 'ㄹ' && c2 == 'ㅅ') {
            return 'ㄽ';
        } else if(c1 == 'ㄹ' && c2 == 'ㅌ') {
            return 'ㄾ';
        } else if(c1 == 'ㄹ' && c2 == 'ㅍ') {
            return 'ㄿ';
        } else if(c1 == 'ㄹ' && c2 == 'ㅎ') {
            return 'ㅀ';
        } else if(c1 == 'ㅂ' && c2 == 'ㅅ') {
            return 'ㅄ';
        }
        return 0;
    }

    /**
     * SegmentGroup로부터 한글 인식
     * @param segmentGroup
     * @return
     */
    public static List<CandidateLetter> recognize(SegmentGroup segmentGroup) {
        String chainCode = segmentGroup.toChainCode();
        char[] chainCodeArr = chainCode.toCharArray();

        List<CandidateLetter> candidateLetters = _recognize(chainCodeArr, 0, 0);
        List<CandidateLetter> filteredByBound = new ArrayList<>();

        for(CandidateLetter candidateLetter : candidateLetters) {
            char letter = HanUtil.combine(candidateLetter.getChosung(), candidateLetter.getJungsung(), candidateLetter.getJongsung());

            SegmentGroup choGroup = null;
            SegmentGroup jungGroup = null;
            SegmentGroup jongGroup = null;

            int hangulType = HanUtil.getHangulType(letter);
            int predictHangulType = 0;
            // 종성 없음
            if(hangulType == 1 || hangulType == 3 || hangulType == 5) {
                choGroup = segmentGroup.subGroup(0, candidateLetter.getBound1());
                jungGroup = segmentGroup.subGroup(candidateLetter.getBound1(), segmentGroup.size());
                predictHangulType = getPredictHangulType(letter, choGroup, jungGroup);
            }
            // 종성 있음
            else if(hangulType == 2 || hangulType == 4 || hangulType == 6) {
                choGroup = segmentGroup.subGroup(0, candidateLetter.getBound1());
                jungGroup = segmentGroup.subGroup(candidateLetter.getBound1(), candidateLetter.getBound2());
                jongGroup = segmentGroup.subGroup(candidateLetter.getBound2(), segmentGroup.size());
                predictHangulType = getPredictHangulType(letter, choGroup, jungGroup, jongGroup);
            }

            if(predictHangulType == hangulType) {
                filteredByBound.add(candidateLetter);
            }
        }

        if(filteredByBound.size() == 0) {
            return candidateLetters;
        }

        return filteredByBound;
    }

    /**
     * 한글의 6가지 형태 추측
     * 0 : 인식불가
     * 1 : 가 형태
     * 2 : 강 형태
     * 3 : 고 형태
     * 4 : 공 형태
     * 5 : 과 형태
     * 6 : 광 형태
     * @param letter
     * @param choGroup
     * @param jungGroup
     * @return
     */
    private static int getPredictHangulType(char letter, SegmentGroup choGroup, SegmentGroup jungGroup) {
        char jongsung = getJongsung(letter);
        Point2D chosungCenter = choGroup.getCenter();
        Point2D jungsungCenter = jungGroup.getCenter();

        SegmentGroup allGroup = choGroup.expand(jungGroup);
        Rectangle rect = allGroup.getRect();
        double widthError =  rect.getWidth() * 0.2d;
        double heightError =  rect.getHeight() * 0.2d;

        int tempValue;
        // 우측 중성
        if(jungsungCenter.getX() - chosungCenter.getX() > widthError
                && choGroup.getRect().getIntersectWidthProjection(jungGroup.getRect()) < widthError
                && choGroup.getRect().getIntersectHeightProjection(jungGroup.getRect()) > heightError) {
            return 1;
        }
        // 하단 중성
        else if(choGroup.getRect().getIntersectWidthProjection(jungGroup.getRect()) > widthError
                && choGroup.getRect().getIntersectHeightProjection(jungGroup.getRect()) < heightError
                && jungsungCenter.getY() - chosungCenter.getY() > heightError) {
            return 3;
        }
        // 우하단 중성
        else if(jungsungCenter.getY() - chosungCenter.getY() > heightError
                && choGroup.getRect().getIntersectWidthProjection(jungGroup.getRect()) > widthError
                && choGroup.getRect().getIntersectHeightProjection(jungGroup.getRect()) > heightError) {
            return 5;
        }

        return 0;
    }

    private static int getPredictHangulType(char letter, SegmentGroup choGroup, SegmentGroup jungGroup, SegmentGroup jongGroup) {
        int predictHangulType = getPredictHangulType(letter, choGroup, jungGroup);

        SegmentGroup allGroup = choGroup.expand(jungGroup).expand(jongGroup);
        Rectangle rect = allGroup.getRect();
        double widthError =  rect.getWidth() * 0.1d;
        double heightError =  rect.getHeight() * 0.1d;

        // 종성이 초성을 일정이상 침범하면
        if(jongGroup.getRect().getIntersectHeightProjection(choGroup.getRect()) > heightError) {
            return 0;
        }
        // 종성이 중성을 일정이상 침범하면
        if(jongGroup.getRect().getIntersectHeightProjection(jungGroup.getRect()) > heightError) {
            return 0;
        }

        if(predictHangulType == 1) {
            return 2;
        } else if(predictHangulType == 3) {
            return 4;
        } else if(predictHangulType == 5) {
            return 6;
        }

        return 0;
    }

    private static List<CandidateLetter> _recognize(char chainArr[], int start, int chojungjong) {
        boolean chosungCheck = chojungjong == 0;
        boolean chosung2Check = chojungjong == 1;
        boolean jungsungCheck = chojungjong == 2;
        boolean jongsungCheck = chojungjong == 3;
        boolean jongsung2Check = chojungjong == 4;

        while(chainArr[start] == '&') {
            start++;
        }

        if(chosungCheck) {
            List<CandidateLetter> result = new ArrayList<>();
            for(String chosungChain : chainToJaumDb.keySet()) {
                if(Arrays.equals(Arrays.copyOfRange(chainArr, 0, chosungChain.length()), chosungChain.toCharArray())) {
                    char chosungs[] = chainToJaumDb.get(chosungChain).toString().toCharArray();
                    if(chosungChain.length() == chainArr.length) {
                        for(char chosung : chosungs) {
                            CandidateLetter letter = new CandidateLetter();
                            letter.setChosung(chosung);
                            letter.setBound1(chosungChain.length());
                            result.add(letter);
                        }
                    } else if(chainArr[chosungChain.length()] == '&'){
                        List<CandidateLetter> candidateLetters = _recognize(chainArr, chosungChain.length() + 1, 1);
                        for(CandidateLetter rest : candidateLetters) {
                            for(char chosung : chosungs) {
                                if(rest.getChosung() != 0) {
                                    chosung = combineJaum(chosung, rest.getChosung(), true);
                                    if(chosung == 0) {
                                        continue;
                                    }
                                }
                                CandidateLetter letter = new CandidateLetter();
                                letter.setChosung(chosung);
                                letter.setJungsung(rest.getJungsung());
                                letter.setJongsung(rest.getJongsung());
                                letter.setBound1(rest.getBound1());
                                letter.setBound2(rest.getBound2());
                                result.add(letter);
                            }
                        }
                    }
                }
            }
            return result;
        } else if(chosung2Check) {
            List<CandidateLetter> result = new ArrayList<>();
            for(String chosungChain : chainToJaumDb.keySet()) {
                if(Arrays.equals(Arrays.copyOfRange(chainArr, start, start + chosungChain.length()), chosungChain.toCharArray())) {
                    char chosungs[] = chainToJaumDb.get(chosungChain).toString().toCharArray();
                    if(start + chosungChain.length() == chainArr.length) {
                        for(char chosung : chosungs) {
                            CandidateLetter letter = new CandidateLetter();
                            letter.setChosung(chosung);
                            result.add(letter);
                        }
                    } else if(chainArr[start + chosungChain.length()] == '&'){
                        List<CandidateLetter> rests = _recognize(chainArr, start + chosungChain.length() + 1, 2);
                        for(CandidateLetter rest : rests) {
                            for(char chosung : chosungs) {
                                CandidateLetter letter = new CandidateLetter();
                                letter.setChosung(chosung);
                                letter.setJungsung(rest.getJungsung());
                                letter.setJongsung(rest.getJongsung());
                                letter.setBound1(rest.getBound1());
                                letter.setBound2(rest.getBound2());
                                result.add(letter);
                            }
                        }
                    }
                }
            }

            result.addAll(_recognize(chainArr, start, 2));
            return result;
        } else if(jungsungCheck) {
            List<CandidateLetter> result = new ArrayList<>();
            for(String jungsungChain : chainToMoumDb.keySet()) {
                if(Arrays.equals(Arrays.copyOfRange(chainArr, start, start + jungsungChain.length()), jungsungChain.toCharArray())) {
                    char jungsungs[] = chainToMoumDb.get(jungsungChain).toString().toCharArray();
                    if(start + jungsungChain.length() == chainArr.length) {
                        for(char jungsung : jungsungs) {
                            CandidateLetter letter = new CandidateLetter();
                            letter.setJungsung(jungsung);
                            letter.setBound1(start);
                            result.add(letter);
                        }
                    } else if(chainArr[start + jungsungChain.length()] == '&'){
                        List<CandidateLetter> jongsungs = _recognize(chainArr, start + jungsungChain.length() + 1, 3);
                        for(char jungsung : jungsungs) {
                            for(CandidateLetter jongsung : jongsungs) {
                                CandidateLetter letter = new CandidateLetter();
                                letter.setJungsung(jungsung);
                                letter.setJongsung(jongsung.getJongsung());
                                letter.setBound1(start);
                                letter.setBound2(jongsung.getBound2());
                                result.add(letter);
                            }
                        }
                    }
                }
            }
            return result;
        } else if(jongsungCheck) {
            List<CandidateLetter> result = new ArrayList<>();
            for(String jongsungChain : chainToJaumDb.keySet()) {
                if (Arrays.equals(Arrays.copyOfRange(chainArr, start, start + jongsungChain.length()), jongsungChain.toCharArray())) {
                    char jongsungs[] = chainToJaumDb.get(jongsungChain).toString().toCharArray();
                    if(start + jongsungChain.length() == chainArr.length) {
                        for (char jongsung : jongsungs) {
                            CandidateLetter letter = new CandidateLetter();
                            letter.setBound2(start);
                            letter.setJongsung(jongsung);
                            result.add(letter);
                        }
                    } else if(chainArr[start + jongsungChain.length()] == '&'){
                        List<CandidateLetter> jongsungs2 = _recognize(chainArr, start + jongsungChain.length() + 1, 4);
                        for(char jongsung : jongsungs) {
                            for(CandidateLetter jongsung2 : jongsungs2) {
                                char combinedJongsung = combineJaum(jongsung, jongsung2.getJongsung(), false);
                                if(combinedJongsung == 0) {
                                    continue;
                                }
                                CandidateLetter letter = new CandidateLetter();
                                letter.setBound2(start);
                                letter.setJongsung(combinedJongsung);
                                result.add(letter);
                            }
                        }
                    }
                }
            }
            return result;
        } else if(jongsung2Check) {
            List<CandidateLetter> result = new ArrayList<>();
            for(String jongsungChain : chainToJaumDb.keySet()) {
                if (Arrays.equals(Arrays.copyOfRange(chainArr, start, chainArr.length), jongsungChain.toCharArray())) {
                    char jongsungs[] = chainToJaumDb.get(jongsungChain).toString().toCharArray();
                    if(start + jongsungChain.length() == chainArr.length) {
                        for (char jongsung : jongsungs) {
                            CandidateLetter letter = new CandidateLetter();
                            letter.setJongsung(jongsung);
                            letter.setBound2(start);
                            result.add(letter);
                        }
                    }
                }
            }
            return result;
        }

        return null;
    }

    /**
     * Segment 정규화
     * @param segments
     * @return
     */
    public static List<Segment> normalize(List<Segment> segments) {
        List<Segment> _regularSegments = new ArrayList<>();
        List<Segment> regularSegments = new ArrayList<>();
        List<Segment> copySegments = new ArrayList<>(segments);

        int circle_check = 0;
        int circle_start = 0;
        int circle_lastcheck = 0;
        int clockwise = 0;
        Segment prevSegment = null;

        // ㅇ검사
        for(int i = 0; i < copySegments.size(); i++) {
            Segment segment = copySegments.get(i);
            if(segment == null) {
                circle_check = 0;
                prevSegment = null;
                continue;
            }

            if(circle_check == 0) {
                circle_check++;
                circle_start = i;
                prevSegment = segment;
                continue;
            }
            int distance = prevSegment.getDirection() - segment.getDirection();
            if(distance == 0) {
                continue;
            }

            if(circle_check == 1) {
                clockwise = (distance > 0) ? -1 : 1;
                circle_lastcheck = segment.getDirection() - '0';
                circle_check++;
            } else if(circle_check > 1 && circle_check < 6){
                int expect = circle_lastcheck + clockwise;
                int expect2;
                expect = expect > 8 ? expect % 8: expect;
                expect = expect < 1 ? 8 : expect;
                expect2 = expect + clockwise;
                expect2 = expect2 > 8 ? expect2 % 8: expect2;
                expect2 = expect2 < 1 ? 8 : expect2;

                if(segment.getDirection() - '0' == expect || segment.getDirection() - '0' == expect2) {
                    circle_check++;
                    circle_lastcheck = segment.getDirection() - '0';
                } else {
                    circle_check = 0;
                }
            } else if(circle_check >= 6) {
                double cx = 0, cy = 0, cw = 0, ch = 0, cleft = 0xfffff, cright = -1, ctop = 0xfffff, cbottom = -1;

                for(int j = 0; j < i - circle_start + 1; j++) {
                    Segment tempSegment = copySegments.get(circle_start);
                    if(tempSegment.getX() < cleft) {
                        cleft = tempSegment.getX();
                    }
                    if(tempSegment.getX() > cright) {
                        cright = tempSegment.getX();
                    }
                    if(tempSegment.getY() < ctop) {
                        ctop = tempSegment.getY();
                    }
                    if(tempSegment.getY() > cbottom) {
                        cbottom = tempSegment.getY();
                    }
                    if(tempSegment.getEndX() < cleft) {
                        cleft = tempSegment.getEndX();
                    }
                    if(tempSegment.getEndX() > cright) {
                        cright = tempSegment.getEndX();
                    }
                    if(tempSegment.getEndY() < ctop) {
                        ctop = tempSegment.getEndY();
                    }
                    if(tempSegment.getEndY() > cbottom) {
                        cbottom = tempSegment.getEndY();
                    }
                    copySegments.remove(circle_start);
                }
                cx = (cleft + cright) / 2;
                cy = (ctop + cbottom) / 2;
                cw = (cright - cleft) / 2;
                ch = (cbottom - ctop) / 2;

                Segment circleSegment = new Segment();
                circleSegment.setX(cx);
                circleSegment.setY(cy);
                circleSegment.setDx(cw);
                circleSegment.setDy(ch);
                circleSegment.setCircle(true);
                copySegments.add(circle_start, circleSegment);
                circle_check = 0;

                i = circle_start + 1;
                while(i < copySegments.size() && (segment = copySegments.get(i)) != null) {
                    copySegments.remove(i);
                }
            }

            prevSegment = segment;
        }

        _regularSegments = _normaliza(copySegments);

        // 정규화 후에도 남아있는 연속코드 처리
        for(int i = 0; i < _regularSegments.size(); i++) {
            Segment segment = _regularSegments.get(i);
            if(segment == null) {
                regularSegments.add(null);
                continue;
            }

            if(i > 0 && regularSegments.get(regularSegments.size() - 1) != null) {
                Segment tempSegment = regularSegments.get(regularSegments.size() - 1);
                if(tempSegment.getDirection() == segment.getDirection()) {
                    tempSegment.setDx(segment.getEndX() - tempSegment.getX());
                    tempSegment.setDy(segment.getEndY() - tempSegment.getY());
                    continue;
                }
            }

            regularSegments.add(segment);
        }

        if(regularSegments.size() > 1) {
            // 획끝에 좌상획 제거
            prevSegment = regularSegments.get(0);
            for(int i = 1; i < regularSegments.size(); i++) {
                Segment segment = regularSegments.get(i);
                if(segment == null && (prevSegment != null && prevSegment.getDirection() - '0' == 2)) {
                    regularSegments.remove(i - 1);
                    i--;
                    prevSegment = (i == 0 ? null : regularSegments.get(i - 1));
                    i--;
                    continue;
                } else if(i == regularSegments.size() - 1 && segment != null && segment.getDirection() - '0' == 2) {
                    regularSegments.remove(i);
                    break;
                }
                prevSegment = segment;
            }

            // 손떨림 보정
            SegmentGroup tempGroup = new SegmentGroup(regularSegments);
            char tempChainArr[] = tempGroup.toChainCode().toCharArray();
            int left = 0, right = 1, tick = 1;

            while(right < regularSegments.size()) {
                while(right < regularSegments.size() && (tempChainArr[left] == '&' || tempChainArr[right] == '&')) {
                    left++;
                    right++;
                }
                if(right >= regularSegments.size()) {
                    break;
                }
                while(right < tempChainArr.length && tempChainArr[right] == tempChainArr[left + tick] && chainDistance(tempChainArr[left] - '0', tempChainArr[left + 1] - '0') <= 2) {
                    right++;
                    tick = tick == 0 ? 1 : 0;
                }
                if(right - left > 2) {
                    Segment leftSegment = regularSegments.get(left);
                    Segment rightSegment = regularSegments.get(right - 1);
                    if(rightSegment == null || leftSegment == null) {
                        break;
                    }
                    leftSegment.setDx(rightSegment.getEndX() - leftSegment.getX());
                    leftSegment.setDy(rightSegment.getEndY() - leftSegment.getY());
                    for(int i = 0; i < right - left - 1; i++) {
                        regularSegments.remove(left + 1);
                    }
                    right = left + 1;
                    tempChainArr = new SegmentGroup(regularSegments).toChainCode().toCharArray();
                } else {
                    left++;
                    right = left + 1;
                }
                tick = 1;
            }
        }

        return regularSegments;
    }

    private static int chainDistance(int c1, int c2) {
        int chainDistance = c1 < c2 ? Math.min(c2 - c1, c1 + 8 - c2) : Math.min(c1 - c2, c2 + 8 - c1);
        return chainDistance;
    }

    private static List<Segment> _normaliza(List<Segment> segments) {
        Segment tempSegment = null;
        int pivot = -1;
        List<Segment> _regularSegments = new ArrayList<>();

        try {
            for(int i = 0; i<segments.size(); i++) {
                Segment segment = segments.get(i);
                if(i == 0) {
                    tempSegment = (Segment) segments.get(0).clone();
                    pivot = tempSegment.getDirection() - '0';
                }
                if(segment == null) {
                    if(tempSegment != null) {
                        _regularSegments.add(tempSegment);
                    }
                    _regularSegments.add(null);
                    tempSegment = null;
                    continue;
                } else if(segment.getDirection() == '9'){
                    _regularSegments.add(segment);
                    tempSegment = null;
                    continue;
                }

                if(tempSegment == null) {
                    tempSegment = (Segment) segment.clone();
                    pivot = tempSegment.getDirection() - '0';
                } else if(!tempSegment.equals(segment)){
                    tempSegment.setDx(segment.getX() - tempSegment.getX());
                    tempSegment.setDy(segment.getY() - tempSegment.getY());
                }

                int c = segment.getDirection() - '0';
                int chainDistance = c < pivot ? Math.min(pivot - c, c + 8 - pivot) : Math.min(c - pivot, pivot + 8 - c);
                if((c > 0 && chainDistance >= 2) || i == segments.size() - 1) {
                    _regularSegments.add(tempSegment);
                    pivot = segment.getDirection() - '0';
                    tempSegment = (Segment) segment.clone();
                }
            }
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return _regularSegments;
    }
}
