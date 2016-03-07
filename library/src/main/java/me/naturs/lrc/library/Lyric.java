package me.naturs.lrc.library;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * Created by naturs on 2016/3/6.
 */
class Lyric {

    private final TreeMap<Integer, Sentence> lrcMap;

    private final ArrayList<Integer> lrcTimeList;

    private int currentTime;
    private int currentIndex = 0;

    Lyric() {
        lrcMap = new TreeMap<>();
        lrcTimeList = new ArrayList<>(50);
    }

    void add(Map<Integer, String> lrc) {
        if (lrc == null) {
            return;
        }
        int time;
        for (Map.Entry<Integer, String> entry : lrc.entrySet()) {
            time = entry.getKey();
            lrcMap.put(time, new Sentence(time, entry.getValue()));
        }
        lrcTimeList.addAll(lrcMap.keySet());
    }

    void clear() {
        lrcMap.clear();
        lrcTimeList.clear();
        currentTime = 0;
        currentIndex = 0;
    }

    boolean isEmpty() {
        return lrcMap.isEmpty();
    }

    boolean update(int time) {
        final int oldCurrentTime = currentTime;
        if (lrcMap.containsKey(time)) {
            currentTime = time;
        } else {
            currentTime = lrcMap.firstKey();
            if (time > currentTime) {
                currentTime = lrcMap.floorKey(time);
            }
        }
        if (oldCurrentTime != currentTime) {
            currentIndex = lrcTimeList.indexOf(currentTime);
            return true;
        }
        return false;
    }

    int getCurrentIndex() {
        return currentIndex;
    }

    int size() {
        return lrcTimeList.size();
    }

    String getLrc(int index) {
        return lrcMap.get(lrcTimeList.get(index)).getLrc();
    }

    Sentence getSentence(int index) {
        return lrcMap.get(lrcTimeList.get(index));
    }

    void update(int index, int line, float baseY, String[] splitLrc) {
        Sentence sentence = lrcMap.get(lrcTimeList.get(index));
        sentence.update(line, baseY, splitLrc);
    }

    float getDistance(int oldIndex, int newIndex) {
        return getSentence(newIndex).getBaseY() - getSentence(oldIndex).getBaseY();
    }

}
