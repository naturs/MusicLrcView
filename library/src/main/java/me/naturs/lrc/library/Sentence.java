package me.naturs.lrc.library;

/**
 *
 * Created by naturs on 2016/3/6.
 */
class Sentence {

    private int startTime;
    private String lrc;
    private int line;
    private int textHeight;

    private boolean inited;

    public Sentence(int startTime, String lrc) {
        this.startTime = startTime;
        this.lrc = lrc;
    }

    public int getStartTime() {
        return startTime;
    }

    public String getLrc() {
        return lrc;
    }

    public int getLine() {
        return line;
    }

    public int getTextHeight() {
        return textHeight;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public void setTextHeight(int textHeight) {
        this.textHeight = textHeight;
    }

    public boolean isInited() {
        return inited;
    }
}
