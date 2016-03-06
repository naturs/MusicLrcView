package me.naturs.lrc.library;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

/**
 *
 * Created by naturs on 2016/3/6.
 */
class Lyric {

    private TreeMap<Integer, Sentence> lrcMap;

    private ArrayList<Integer> lrcTimeList;

    private int currentTime;
    private int currentIndex = 0;

    Lyric() {
        lrcMap = new TreeMap<>();
        lrcTimeList = new ArrayList<>(50);

        Random random = new Random();
        for (int i = 0; i < 50; i ++) {
            int key = random.nextInt(1000) + 200;
            Sentence sentence = new Sentence(key, randromStr(random));
            lrcMap.put(key,  sentence);
        }
        lrcTimeList.addAll(lrcMap.keySet());
    }

    String randromStr(Random random) {
        int len = random.nextInt(100) + 10;
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<len;i++) {
            sb.append((char)(random.nextInt(130) + 1));
        }
        return sb.toString();
    }

    void newTime() {
        currentIndex ++;
        currentTime = lrcTimeList.get(currentIndex);
    }

    boolean isEmpty() {
        return lrcMap.isEmpty();
    }

    void update(int time) {
//        int nextTime = lrcMap.lastKey();
//        if (time < nextTime) {
//            nextTime = lrcMap.higherKey(time);
//        }
        if (lrcMap.containsKey(time)) {
            currentTime = time;
        } else {
            currentTime = lrcMap.firstKey();
            if (time > currentTime) {
                currentTime = lrcMap.floorKey(time);
            }
        }
        currentIndex = lrcTimeList.indexOf(currentTime);
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
