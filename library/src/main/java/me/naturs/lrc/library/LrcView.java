package me.naturs.lrc.library;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by naturs on 2016/3/6.
 */
public class LrcView extends View {

    private static final String TAG = LrcView.class.getSimpleName();

    private static final int DEFAULT_TEXT_SIZE_SP = 16;
    private static final int DEFAULT_DIVIDER_DP = 16;
    private static final int DEFAULT_INTER_DIVIDER_DP = 8;

    private Lyric mLyric = new Lyric();

    private Paint mNormalPaint;
    private Paint mCurrentPaint;

    /**
     * 歌词文字大小
     */
    private float mTextSize;
    /**
     * 两句不同歌词之间的行间距
     */
    private float mDividerHeight;
    /**
     * 单句歌词如果歌词分行了，行间距
     */
    private float mInterDividerHeight;

    private int mViewWidth;
    private int mViewHeight;
    private float mCenterX;
    private float mCenterY;

    public LrcView(Context context) {
        this(context, null);
    }

    public LrcView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LrcView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        setLayerType(LAYER_TYPE_HARDWARE, null);

        DisplayMetrics metrics = getResources().getDisplayMetrics();

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.LrcView, defStyleAttr, 0);

        int normalTextColor = Color.BLACK;
        int currentTextColor = Color.RED;
        mTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, DEFAULT_TEXT_SIZE_SP, metrics);
        mDividerHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_DIVIDER_DP, metrics);
        mInterDividerHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_INTER_DIVIDER_DP, metrics);
        normalTextColor = array.getColor(R.styleable.LrcView_lrc_normal_text_color, normalTextColor);
        currentTextColor = array.getColor(R.styleable.LrcView_lrc_current_text_color, currentTextColor);
        mTextSize = array.getDimension(R.styleable.LrcView_lrc_text_size, mTextSize);
        mDividerHeight = array.getDimension(R.styleable.LrcView_lrc_divider_height, mDividerHeight);
        mInterDividerHeight = array.getDimension(R.styleable.LrcView_lrc_inter_divider_height, mInterDividerHeight);

        array.recycle();

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

//        canvas.drawLine(0, getCenterY(), mViewWidth, getCenterY(), mCurrentPaint);
//        canvas.drawLine(mCenterX, 0, mCenterX, mViewHeight, mCurrentPaint);

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
        textHeight = getTextHeight();
        for (int i = currentIndex - 1; i >= 0; i --) {
            if (currY <= getPaddingTop()) {
                break;
            }
            // 移动到要draw的那行的底部
            currY -= textHeight + mDividerHeight;
            line = drawText(canvas, mNormalPaint, i, currY);
            textHeight = getTextHeight() * line + mInterDividerHeight * (line - 1);
        }

        // draw after
        currY = getCenterY();
        textHeight = Math.max(currentTextLine - 1, 0) * (getTextHeight() + mInterDividerHeight)
                + getTextHeight();
        int size = mLyric.size();
        for (int i = currentIndex + 1; i < size; i ++) {
            if (currY >= mViewHeight - getPaddingBottom()) {
                break;
            }
            // 移动到要draw的那行的底部
            currY += textHeight + mDividerHeight;
            line = drawText(canvas, mNormalPaint, i, currY);
            textHeight = getTextHeight() * line + mInterDividerHeight * (line - 1);
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
                        getTextDrawBaseY(paint, baseY - (line - 1 - i) * (getTextHeight() + mInterDividerHeight)),
                        paint);
            } else {
                // 画当前行以及下面的部分
                canvas.drawText(
                        str,
                        getTextDrawBaseX(paint),
                        getTextDrawBaseY(paint, baseY + i * (getTextHeight() + mInterDividerHeight)),
                        paint);
            }
        }
        return line;
    }

    private void splitLrc(Paint paint, String text, float allowMaxWidth, List<String> list) {
        if (text == null) {
            text = "";
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

    private float getTextHeight() {
        return mTextSize;
    }

    private void initLrc() {
        long start = System.currentTimeMillis();
        if (mLyric == null || mLyric.isEmpty()) {
            return;
        }
        List<String> tempList = new ArrayList<>();
        int size = mLyric.size();
        int line;
        float baseY = 0;
        float allowMaxWidth = mViewWidth - getPaddingLeft() - getPaddingRight();
        for (int i = 0; i < size; i++) {
            tempList.clear();
            splitLrc(mNormalPaint, mLyric.getLrc(i), allowMaxWidth, tempList);
            line = tempList.size();
            mLyric.update(i, line, baseY, tempList.toArray(new String[line]));
            baseY += (line - 1) * (getTextHeight() + mInterDividerHeight) + mDividerHeight
                    + getTextHeight();
        }
        print("耗时：" + (System.currentTimeMillis() - start));
    }

    private void newLineAnim(int oldIndex, int newIndex) {
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
        return mLyric.getDistance(oldIndex, newIndex);
    }

    private float mAnimOffset;

    private static void print(String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        Log.d(TAG, message);
    }
}
