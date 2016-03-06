package me.naturs.lrc.library;

import java.util.Arrays;
import java.util.List;

/**
 *
 * Created by naturs on 2016/3/6.
 */
class Sentence {

    private int startTime;
    private String lrc;
    private String[] splitLrc;
    private int line;
    private float textHeight;

    Sentence(int startTime, String lrc) {
        this.startTime = startTime;
        this.lrc = lrc;
    }

    int getStartTime() {
        return startTime;
    }

    String getLrc() {
        return lrc;
    }

    int getLine() {
        return line;
    }

    float getTextHeight() {
        return textHeight;
    }

    void update(int line, float textHeight, List<String> splitLrc) {
        this.line = line;
        this.textHeight = textHeight;
        this.splitLrc = splitLrc.toArray(new String[splitLrc.size()]);
    }

    String getSplitLrc(int index) {
        return splitLrc[index];
    }
}
