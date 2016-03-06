package me.naturs.lrc.library;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by naturs on 2016/3/6.
 */
public class LrcView extends View {

    private static final String TAG = LrcView.class.getSimpleName();

    private Lyric mLyric = new Lyric();

    private Paint mNormalPaint;
    private Paint mCurrentPaint;

    private float mTextSize = 30;
    private float mDividerHeight = 60;
    private float mLrcDividerHeight = 10;

    private int mViewWidth;
    private int mViewHeight;
    private float mCenterX;
    private float mCenterY;

    private List<String> mTempList = new ArrayList<>();

    public LrcView(Context context) {
        this(context, null);
    }

    public LrcView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LrcView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_HARDWARE, null);
        int normalTextColor = Color.BLACK;
        int currentTextColor = Color.RED;

        mNormalPaint = new Paint();
        mNormalPaint.setAntiAlias(true);
        mNormalPaint.setTextSize(mTextSize);
        mNormalPaint.setColor(normalTextColor);
        mNormalPaint.setTextAlign(Paint.Align.CENTER);

        mCurrentPaint = new Paint();
        mCurrentPaint.setAntiAlias(true);
        mCurrentPaint.setTextSize(mTextSize);
        mCurrentPaint.setColor(currentTextColor);
        mCurrentPaint.setTextAlign(Paint.Align.CENTER);

        postDelayed(new Runnable() {
            @Override
            public void run() {
                int oldIndex = mLyric.getCurrentIndex();
                mLyric.newTime();
                int newIndex = mLyric.getCurrentIndex();
                newLineAnim(oldIndex, newIndex);
                postDelayed(this, 2000);
            }
        }, 3000);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewHeight = h;
        mCenterX = w * 0.5f;
        mCenterY = h * 0.5f;

        initLrc();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long start = System.currentTimeMillis();
        super.onDraw(canvas);

        canvas.drawLine(0, getCenterY(), mViewWidth, getCenterY(), mCurrentPaint);
        canvas.drawLine(mCenterX, 0, mCenterX, mViewHeight, mCurrentPaint);

        if (mLyric == null || mLyric.isEmpty()) {
            return;
        }

        float currY = getCenterY();

        final int currentIndex = mLyric.getCurrentIndex();

        // 行总高，如果是多行就是行高+行间距
        float textHeight;
        int line;
        final int currentTextLine;

        // draw current
        if (currentIndex >= 0) {
            currentTextLine = drawText(canvas, mCurrentPaint, currentIndex, currY);
        } else {
            currentTextLine = 0;
        }

        // draw before
        // 当前行的高度可能和其他行不一样，特别处理
        textHeight = getCurrentTextHeight();
        for (int i = currentIndex - 1; i >= 0; i --) {
            if (currY <= 0) {
                break;
            }
            // 移动到要draw的那行的底部
            currY -= textHeight + mDividerHeight;
            line = drawText(canvas, mNormalPaint, i, currY);
            textHeight = getNormalTextHeight() * line + mLrcDividerHeight * (line - 1);
        }

        // draw after
        currY = getCenterY();
        // 当前行的高度可能和其他行不一样，特别处理
        textHeight = Math.max(currentTextLine - 1, 0) * (getCurrentTextHeight() + mLrcDividerHeight)
                + getNormalTextHeight();
        int size = mLyric.size();
        for (int i = currentIndex + 1; i < size; i ++) {
            if (currY >= mViewHeight - getPaddingBottom()) {
                break;
            }
            // 移动到要draw的那行的底部
            currY += textHeight + mDividerHeight;
            line = drawText(canvas, mNormalPaint, i, currY);
            textHeight = getNormalTextHeight() * line + mLrcDividerHeight * (line - 1);
        }

        canvas.clipRect(0, 0, mViewWidth, mViewHeight);
//        print("draw 耗时：" + (System.currentTimeMillis() - start));
    }



    private float getCenterY() {
        return mCenterY + mAnimOffset;
    }

    private int drawText(Canvas canvas, Paint paint, int index, float baseY) {
        Sentence sentence = mLyric.getSentence(index);
        int line = sentence.getLine();
        String str;
        for (int i = 0; i < line; i++) {
            str = sentence.getSplitLrc(i);
            if (baseY < getCenterY()) {
                // 画当前行上面的部分
                canvas.drawText(
                        str,
                        getTextDrawBaseX(paint),
                        getTextDrawBaseY(paint, baseY - (line - 1 - i) * (getTextHeight(paint, str) + mLrcDividerHeight)),
                        paint);
            } else {
                // 画当前行以及下面的部分
                canvas.drawText(
                        str,
                        getTextDrawBaseX(paint),
                        getTextDrawBaseY(paint, baseY + i * (getTextHeight(paint, str) + mLrcDividerHeight)),
                        paint);
            }
        }
        return line;
    }

    private int drawText(Canvas canvas, Paint paint, String text, float baseY) {
        splitStr(paint, text, mTempList);
        int line = mTempList.size();

        String str;
        for (int i = 0; i < line; i++) {
            str = mTempList.get(i);
            if (baseY < getCenterY()) {
                // 画当前行上面的部分
                canvas.drawText(
                        str,
                        getTextDrawBaseX(paint),
                        getTextDrawBaseY(paint, baseY - (line - 1 - i) * (getTextHeight(paint, str) + mLrcDividerHeight)),
                        paint);
            } else {
                // 画当前行以及下面的部分
                canvas.drawText(
                        str,
                        getTextDrawBaseX(paint),
                        getTextDrawBaseY(paint, baseY + i * (getTextHeight(paint, str) + mLrcDividerHeight)),
                        paint);
            }
        }
        mTempList.clear();
        return line;
    }

    private void splitStr(Paint paint, String text, List<String> list) {
        if (text == null) {
            text = "";
        }

        float allowMaxWidth = mViewWidth - getPaddingLeft() - getPaddingRight();

        list.clear();
        splitLrc(paint, text, allowMaxWidth, list);
    }

    private void splitLrc(Paint paint, String text, float allowMaxWidth, List<String> list) {
        if (text == null) {
            return;
        }
        text = text.trim();
        int overflow = text.length() - paint.breakText(text, true, allowMaxWidth, null);
        int contained = text.length() - overflow;
        String cutPrevious = text.substring(0, contained);
        if (overflow > 0 && cutPrevious.contains(" ")) {
            cutPrevious = cutPrevious.substring(0, cutPrevious.lastIndexOf(" "));
        }
        list.add(cutPrevious);

        if (overflow > 0) {
            splitLrc(paint, text.substring(cutPrevious.length(), text.length()), allowMaxWidth, list);
        }
    }

    private float getTextDrawBaseX(Paint paint) {
        return mCenterX;
    }

    private float getTextDrawBaseY(Paint paint, float currY) {
//        return currY + (Math.abs(paint.ascent() + Math.abs(paint.descent()))) / 2;
        return currY - paint.descent();
    }

    private float getNormalTextHeight() {
        return mNormalPaint.getTextSize();
    }

    private float getCurrentTextHeight() {
        return mCurrentPaint.getTextSize();
    }

    private float getTextHeight(Paint paint, String text) {
        return paint.getTextSize();
    }

    private void initLrc() {
        long start = System.currentTimeMillis();
        if (mLyric == null || mLyric.isEmpty()) {
            return;
        }
        int size = mLyric.size();
        int line;
        float textHeight;
        float baseY = 0;
        for (int i = 0; i < size; i++) {
//            print("i=" + mLyric.getLrc(i));
            mTempList.clear();
            splitStr(mNormalPaint, mLyric.getLrc(i), mTempList);
            line = mTempList.size();
            textHeight = line * getNormalTextHeight() + (line - 1) * mLrcDividerHeight;
            mLyric.update(i, line, baseY, mTempList);
            baseY += (line - 1) * (getNormalTextHeight() + mLrcDividerHeight) + mDividerHeight
                    + getNormalTextHeight();
        }
        mTempList.clear();
        print("耗时：" + (System.currentTimeMillis() - start));
    }

    private void newLineAnim(int oldIndex, int newIndex) {
        print("newLineAnim:" + getDistance(oldIndex, newIndex));
        ValueAnimator animator = ValueAnimator.ofFloat(getDistance(oldIndex, newIndex), 0);
        animator.setDuration(500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAnimOffset = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.start();
    }

    private float getDistance(int oldIndex, int newIndex) {
//        int oldLine
        return mLyric.getSentence(newIndex).getTextHeight() - mLyric.getSentence(oldIndex).getTextHeight();
    }

    private float mAnimOffset;

    private static void print(String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        Log.d(TAG, message);
    }
}
