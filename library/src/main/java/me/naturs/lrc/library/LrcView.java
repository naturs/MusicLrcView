package me.naturs.lrc.library;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.StringRes;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 *
 * Created by naturs on 2016/3/6.
 */
public class LrcView extends View {

    private static final String TAG = LrcView.class.getSimpleName();

    private static final int DEFAULT_TEXT_SIZE_SP = 16;
    private static final int DEFAULT_DIVIDER_DP = 16;
    private static final int DEFAULT_INTER_DIVIDER_DP = 8;

    private Lyric mLyric;

    private Paint mNormalPaint;
    private Paint mCurrentPaint;
    private Paint mEmptyPaint;

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

    /**
     * 无歌词时显示在界面上的文字
     */
    private String mEmptyText;

    private int mViewWidth;
    private int mViewHeight;
    private float mCenterX;
    private float mCenterY;

    // 用户平滑切换歌词的
    private ValueAnimator mScrollAnimator;
    private float mCenterDrawOffset;

    /**
     * 是否需要更新歌词
     */
    private boolean mNeedInitLrc = true;

    private List<String> mTempList = new ArrayList<>();

    private GestureDetectorCompat mGestureDetector;

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

        float emptyTextSize = mTextSize;
        int emptyTextColor = normalTextColor;
        emptyTextSize = array.getDimension(R.styleable.LrcView_lrc_empty_text_size, emptyTextSize);
        emptyTextColor = array.getColor(R.styleable.LrcView_lrc_empty_text_color, emptyTextColor);
        mEmptyText = array.getString(R.styleable.LrcView_lrc_empty_text);

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

        mEmptyPaint = new Paint();
        mEmptyPaint.setAntiAlias(true);
        mEmptyPaint.setTextSize(emptyTextSize);
        mEmptyPaint.setColor(emptyTextColor);
        mEmptyPaint.setTextAlign(Paint.Align.CENTER);

        mGestureDetector = new GestureDetectorCompat(context, new LrcGestureListener(context));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewHeight = h;
        mCenterX = w * 0.5f;
        mCenterY = h * 0.5f;

        if (mNeedInitLrc) {
            initLrc(false);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long start = System.currentTimeMillis();
        super.onDraw(canvas);

//        canvas.drawLine(0, getCenterY(), mViewWidth, getCenterY(), mCurrentPaint);
//        canvas.drawLine(mCenterX, 0, mCenterX, mViewHeight, mCurrentPaint);

        if (mLyric == null || mLyric.isEmpty() || mNeedInitLrc) {
            if (!TextUtils.isEmpty(mEmptyText)) {
                drawText(canvas, mEmptyPaint, mEmptyText, mCenterY);
            }
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    private float getCenterY() {
        float y = mCenterY;

        return delta + delta1 + mCenterY + (delta == 0 ? mCenterDrawOffset : 0);
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
        mTempList.clear();
        return line;
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
        return currY - paint.descent();
    }

    private float getTextHeight() {
        return mTextSize;
    }

    /**
     * 使用动画效果过渡到新的歌词行
     */
    private float newLineWithAnimation(int oldIndex, int newIndex) {
        if (mLyric == null) {
            return 0;
        }
        if (mScrollAnimator != null && mScrollAnimator.isRunning()) {
            mScrollAnimator.end();
        }

        float distance = mLyric.getDistance(oldIndex, newIndex);

        mScrollAnimator = ValueAnimator.ofFloat(distance, 0);
        mScrollAnimator.setDuration(500);
        mScrollAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mCenterDrawOffset = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        mScrollAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCenterDrawOffset = 0;
                mScrollAnimator = null;
            }
        });
        mScrollAnimator.start();

        return distance;
    }

    private void initLrcInternal() {
        long start = System.currentTimeMillis();


        if (mLyric == null || mLyric.isEmpty()) {
            mNeedInitLrc = false;
            return;
        }
        if (mViewWidth <= 0) {
            mNeedInitLrc = true;
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
        mNeedInitLrc = false;

        print("initLrcInternal 耗时：" + (System.currentTimeMillis() - start));

        postInvalidate();
    }

    private void setLrcInternal(TreeMap<Integer, String> lrc) {
        if (lrc == null) {
            return;
        }
        if (mLyric == null) {
            mLyric = new Lyric();
        }
        mLyric.clear();
        mLyric.add(lrc);
        mNeedInitLrc = true;
    }

    public void setLrc(final String filePath) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                TreeMap<Integer, String> lrc = LrcParser.parseFile(filePath);
                setLrcInternal(lrc);
                initLrc(true);
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void setLrc(final InputStream inputStream) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                TreeMap<Integer, String> lrc = LrcParser.parse(inputStream);
                setLrcInternal(lrc);
                initLrc(true);
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void setLrc(TreeMap<Integer, String> lrc) {
        setLrcInternal(lrc);
        initLrc(false);
    }

    public void clear() {
        if (mLyric != null) {
            mLyric.clear();
        }
        invalidate();
    }

    /**
     * 设置歌词、UI属性发生改变后都要调用该方法
     * @param sync 是否同步修改
     */
    public void initLrc(boolean sync) {
        if (sync) {
            initLrcInternal();
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    initLrcInternal();
                }
            }).start();
        }
    }

    /**
     * 更新音乐进度
     * @param millisecond 播放进度，毫秒
     */
    public void update(int millisecond) {
        if (mLyric == null || mLyric.isEmpty() || mNeedInitLrc) {
            return;
        }
        int oldIndex = mLyric.getCurrentIndex();

        // 如果当前歌词没变会返回false
        if (mLyric.update(millisecond)) {
            int newIndex = mLyric.getCurrentIndex();
            delta1 += newLineWithAnimation(oldIndex, newIndex);
        }
    }

    // ================== 更新UI属性的方法 =================== //


    public void setDividerHeight(float dividerHeight) {
        mDividerHeight = dividerHeight;
        mNeedInitLrc = true;
    }

    public void setInterDividerHeight(float interDividerHeight) {
        mInterDividerHeight = interDividerHeight;
        mNeedInitLrc = true;
    }

    public void setTextSize(float textSize) {
        mTextSize = textSize;
        mNeedInitLrc = true;
    }

    public void setNormalTextColor(int color) {
        mNormalPaint.setColor(color);
        invalidate();
    }

    public void setCurrentTextColor(int color) {
        mCurrentPaint.setColor(color);
        invalidate();
    }

    public void setEmptyText(@StringRes int resId, Object... args) {
        setEmptyText(getResources().getString(resId, args));
    }

    public void setEmptyText(String emptyText) {
        mEmptyText = emptyText;
        invalidate();
    }

    // ================== 更新UI属性的方法 =================== //

    private static void print(String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        Log.d(TAG, message);
    }

    private float delta;
    private float delta1;


    private class LrcGestureListener implements GestureDetector.OnGestureListener {
        private  boolean first;

        private final int mTouchSlop;

        public LrcGestureListener(Context context) {
            final ViewConfiguration configuration = ViewConfiguration.get(context);
            mTouchSlop = configuration.getScaledTouchSlop();
        }

        @Override
        public boolean onDown(MotionEvent e) {
//            delta = 0;
            first = true;
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            print("onSingleTapUp");
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            if (first) {
                print("first onScroll :" + distanceY);
                first = false;
                if (distanceY > 0) { // 向上
                    if (distanceY > mTouchSlop) {
                        distanceY = distanceY - mTouchSlop;
                    }
                } else { // 向下
                    if (Math.abs(distanceY) > mTouchSlop) {
                        distanceY = mTouchSlop - Math.abs(distanceY);
                    }
                }
//                return true;
            }
            delta += -distanceY;
            invalidate();
//            print("onScroll：" + (distanceY ) );
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            print("onLongPress");
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }

}
