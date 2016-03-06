package me.naturs.lrc;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import me.naturs.lrc.library.LrcView;

public class MainActivity extends AppCompatActivity {

    LrcView mLrcView;
    Button mControlBtn;
    SeekBar mSeekBar;
    MediaPlayer mMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLrcView = (LrcView) findViewById(R.id.lrc_view);
        mSeekBar = (SeekBar) findViewById(R.id.seek_bar);
        mControlBtn = (Button) findViewById(R.id.play_pause_btn);

        mControlBtn.setClickable(false);

        mSeekBar.setMax(100);
        mSeekBar.setEnabled(false);

        mControlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                    mControlBtn.setText("开始");
                    mLrcView.removeCallbacks(mRunnable);
                } else {
                    mMediaPlayer.start();
                    mControlBtn.setText("暂停");
                    mLrcView.post(mRunnable);
                }
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mMediaPlayer.seekTo((int) (mMediaPlayer.getDuration() * 1.0f * progress / 100));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mMediaPlayer = new MediaPlayer();

        try {
            AssetFileDescriptor afd = getAssets().openFd("1.mp3");
            mMediaPlayer.setDataSource(afd.getFileDescriptor());
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mControlBtn.setClickable(true);
                    mSeekBar.setEnabled(true);
                }
            });
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void loadLrc() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(getResources().getAssets().open("1.lrc")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String line;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLrcView.removeCallbacks(mRunnable);
        mMediaPlayer.stop();
    }

    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mSeekBar.setProgress((int) (100f * mMediaPlayer.getCurrentPosition() / mMediaPlayer.getDuration()));

            mLrcView.postDelayed(this, 1000);
        }
    };
}
