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
    private int mCurrentLine;

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
                mLyric.newTime();
                newLineAnim();
                postDelayed(this, 2000);
            }
        }, 1000);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewHeight = h;
        mCenterX = w * 0.5f;
        mCenterY = h * 0.5f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
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
            currentTextLine = drawText(canvas, mCurrentPaint, mLyric.getLrc(currentIndex), currY);
        } else {
            currentTextLine = 0;
        }

        mCurrentLine = currentTextLine;

        // draw before
        // 当前行的高度可能和其他行不一样，特别处理
        textHeight = getCurrentTextHeight();
        for (int i = currentIndex - 1; i >= 0; i --) {
            if (currY <= 0) {
                break;
            }
            // 移动到要draw的那行的底部
            currY -= textHeight + mDividerHeight;
            line = drawText(canvas, mNormalPaint, mLyric.getLrc(i), currY);
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
            line = drawText(canvas, mNormalPaint, mLyric.getLrc(i), currY);
            textHeight = getNormalTextHeight() * line + mLrcDividerHeight * (line - 1);
        }

        canvas.clipRect(0, 0, mViewWidth, mViewHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    private float getCenterY() {
        return mCenterY + mAnimOffset;
    }

    private int drawText(Canvas canvas, Paint paint, String text, float baseY) {
        if (text == null) {
            text = "";
        }

        float allowMaxWidth = mViewWidth - getPaddingLeft() - getPaddingRight();

        mTempList.clear();
        splitLrc(paint, text, allowMaxWidth, mTempList);
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

    private void newLineAnim() {
        ValueAnimator animator = ValueAnimator.ofFloat((mCurrentLine - 1) * (getCurrentTextHeight() + mLrcDividerHeight) + mDividerHeight + getNormalTextHeight(), 0.0f);
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

    private float mAnimOffset;

    private static void print(String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        Log.d(TAG, message);
    }
}
