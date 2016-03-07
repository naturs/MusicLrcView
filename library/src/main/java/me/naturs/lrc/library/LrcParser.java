package me.naturs.lrc.library;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * Created by naturs on 2016/3/7.
 */
public class LrcParser {

    public static String lrc = "lyric=[ti:蝴蝶泉边]\n" +
            "[ar:黄雅莉]\n" +
            "[al:崽崽]\n" +
            "[ti:蝴蝶泉边]\n" +
            "[ar:黄雅莉]\n" +
            "[al:崽崽]\n" +
            "[00:00.65] 蝴蝶泉边\n" +
            "[00:03.03] 黄雅莉\n" +
            "[00:05.88] 词曲:彭青\n" +
            "[00:28.28] 我看到满片花儿都开放\n" +
            "[00:33.96] 隐隐约约有声歌唱\n" +
            "[00:37.06] 开出它最灿烂笑的模样\n" +
            "[00:40.79] 要比那日光还要亮\n" +
            "[00:45.63]\n" +
            "[00:46.32] 荡漾着清澄流水的泉啊\n" +
            "[00:49.78] 多么美丽的小小村庄\n" +
            "[00:53.69]\n" +
            "[00:54.27] 我看到淡淡飘动的云儿\n" +
            "[00:58.11] 印在花衣上\n" +
            "[01:03.22] 我唱着妈妈唱着的歌谣\n" +
            "[01:06.52] 牡丹儿绣在金匾上\n" +
            "[01:09.71] 我哼着爸爸哼过的曲调\n" +
            "[01:13.10] 绿绿的草原上牧牛羊\n" +
            "[01:17.87]\n" +
            "[01:19.07] 环绕着扇动银翅的蝶啊\n" +
            "[01:22.32] 追回那遥远古老的时光\n" +
            "[01:25.94] 传诵着自由勇敢的鸟啊\n" +
            "[01:29.04] 一直不停唱\n" +
            "[01:33.41]\n" +
            "[01:34.25] 叶儿上轻轻跳动的水花\n" +
            "[01:37.20] 偶尔沾湿了我发梢\n" +
            "[01:40.38] 阳光下那么奇妙的小小人间\n" +
            "[01:44.25] 变模样\n" +
            "[01:47.86]\n" +
            "[02:54.40][01:56.46] 我唱着妈妈唱着的歌谣\n" +
            "[02:59.00][02:02.22] 牡丹儿绣在金匾上\n" +
            "[03:02.00][02:05.40] 我哼着爸爸哼过的曲调\n" +
            "[03:05.48][02:08.88] 绿绿的草原上牧牛羊\n" +
            "[03:22.70][03:08.69][02:12.25] 环绕着扇动银翅的蝶啊\n" +
            "[03:25.47][03:12.21][02:15.58] 追回那遥远古老的时光\n" +
            "[03:28.77][03:15.47][02:18.73] 传诵着自由勇敢的鸟啊\n" +
            "[03:32.18][03:18.85][02:22.18] 一直不停唱\n" +
            "[03:35.29][02:25.77] 一直不停唱\n" +
            "[02:34.52]\n" +
            "[03:42.11] 叶儿上轻轻跳动的水花\n" +
            "[03:45.56] 偶尔沾湿了我发梢\n" +
            "[03:48.85] 阳光下那么奇妙的小小人间\n" +
            "[03:53.58] 变模样\n" +
            "[03:58.34]\n";

    /**
     * 将歌词解析成一行一行的
     */
    private static final Pattern PATTERN_LRC_LINE = Pattern.compile("(\\[.*\\].*)\\s*");

    /**
     * 解析[ti:xxx]、[ar:xxx]这种
     */
    private static final Pattern PATTERN_LRC_HEADER_LINE = Pattern.compile("\\[[a-zA-Z]+:(.*)\\]");

    /**
     * 解析[03:35.29][02:25.77] xxx 这种
     */
    private static final Pattern PATTERN_LRC_TIME_LINE = Pattern.compile("((\\[\\d+:\\d+\\.\\d+\\])+)(.*)");

    /**
     * 解析[03:35.29][02:25.77]
     */
    private static final Pattern PATTERN_LRC_TIME = Pattern.compile("\\[(\\d+):(\\d+.\\d+)\\]");

    public static TreeMap<Integer, String> parseFile(String filePath) {
        InputStream input = null;
        try {
            input = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return parse(input);
    }

    public static TreeMap<Integer, String> parse(InputStream input) {
        if (input == null) {
            return null;
        }
        List<String> lineList = new ArrayList<>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(input));
            String line;
            while ((line = br.readLine()) != null) {
                lineList.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return parseInternal(lineList);
    }

    /**
     *
     * @param rawLyric 全部歌词
     */
    public static TreeMap<Integer, String> parse(String rawLyric) {
        if (TextUtils.isEmpty(rawLyric) || TextUtils.isEmpty(rawLyric.trim())) {
            return null;
        }
        rawLyric = rawLyric.trim();

        Matcher matcher = PATTERN_LRC_LINE.matcher(rawLyric);
        List<String> lineList = new ArrayList<>();
        while (matcher.find()) {
            lineList.add(matcher.group(1));
        }
        return parseInternal(lineList);
    }

    private static TreeMap<Integer, String> parseInternal(List<String> lineList) {
        if (lineList == null || lineList.isEmpty()) {
            return null;
        }

        Matcher matcher;
        TreeMap<Integer, String> lrcMap = new TreeMap<>();

        int headerStart = Integer.MIN_VALUE;
        for (String line : lineList) {
            if (TextUtils.isEmpty(line) || TextUtils.isEmpty(line.trim())) {
                continue;
            }
            line = line.trim();
            matcher = PATTERN_LRC_HEADER_LINE.matcher(line);
            if (matcher.find()) {
                // [ti:xxx]
                lrcMap.put(headerStart ++, matcher.group(1));
            } else {
                matcher = PATTERN_LRC_TIME_LINE.matcher(line);
                // [03:35.29][02:25.77] xxx
                if (matcher.find()) {
                    // [03:35.29][02:25.77]
                    String time = matcher.group(1);
                    // xxx
                    String content = matcher.group(3);

                    // 解析多个时间
                    Matcher m = PATTERN_LRC_TIME.matcher(time);
                    // [03:35.29]
                    while (m.find()) {
                        int minute = Integer.parseInt(m.group(1));
                        float second = Float.parseFloat(m.group(2));
                        lrcMap.put((int) (minute * 60 * 1000 + second * 1000), content);
                    }
                }
            }
        }

        // 此时歌词已按时间大小排好序

        return lrcMap;
    }

}
