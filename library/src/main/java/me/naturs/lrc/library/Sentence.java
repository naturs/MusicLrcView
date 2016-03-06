package me.naturs.lrc.library;

/**
 *
 * Created by naturs on 2016/3/6.
 */
class Sentence {

    private int startTime;
    private String lrc;
    private String[] splitLrc;
    private int line;
    private float baseY;

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

    float getBaseY() {
        return baseY;
    }

    void update(int line, float baseY, String[] splitLrc) {
        this.line = line;
        this.baseY = baseY;
        this.splitLrc = splitLrc;
    }

    String getSplitLrc(int index) {
        return splitLrc[index];
    }
}
