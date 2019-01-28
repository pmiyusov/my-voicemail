package com.pmiyusov.mvm.viewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.pmiyusov.mvm.Log;
import com.pmiyusov.mvm.R;

import java.io.File;

public class VideoPlayerActivity extends Activity implements SurfaceHolder.Callback, MediaPlayer.OnPreparedListener, VideoControllerView.MediaPlayerControl {
    private static final String TAG = "VideoPlayerActivity";
    SurfaceView videoSurface;
    MediaPlayer player;
    VideoControllerView controller;
    String directoryPath;
    String fileName;
    static String filePath;
    public static VideoPlayerActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        directoryPath = getIntent().getStringExtra("VOICEMAIL_DIRECTORY_PATH");
        fileName = getIntent().getStringExtra("VOICEMAIL_FILE_NAME");
        filePath = directoryPath + "/" + fileName;
        setContentView(R.layout.activity_video_player);
        instance = this;
        videoSurface = (SurfaceView) findViewById(R.id.videoSurface);
        SurfaceHolder videoHolder = videoSurface.getHolder();
        videoHolder.addCallback(this);
        // on empty file ask for delete an exit

    }

    @Override
    public void onStart() {
        super.onStart();
        if (new File(filePath).length() == 0) {
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    fileDeleteDialog(filePath);
                    Looper.loop();
                }
            }.start();
            synchronized (fileName) {
                try {
                    fileName.wait();
                } catch (InterruptedException e) {
                    //fileDeleteDialog done
                    setResult(RESULT_OK);
                    finish();
                }
            }
            setResult(RESULT_OK);
            finish();
        }
        player = new MediaPlayer();
        try {
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setDataSource(this, Uri.fromFile(new File(filePath)));
            player.setOnPreparedListener(this);
            controller = new VideoControllerView(this);
        } catch (Exception e) {
            e.printStackTrace();
            Toast t = Toast.makeText(getApplicationContext(),
                    "Error playing file " + filePath,
                    Toast.LENGTH_LONG);
            t.show();
            Log.e(TAG, "Player exception on file " + filePath);
            e.printStackTrace();
            setResult(RESULT_OK);
            finish();

        }

//        } catch (IllegalArgumentException e) {
//            e.printStackTrace();
//        } catch (SecurityException e) {
//            e.printStackTrace();
//        } catch (IllegalStateException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    	
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        controller.show();
        return false;
    }

    // Implement SurfaceHolder.Callback
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
        //  	player.setDisplay(null); // no video
        player.setDisplay(holder);
        try {
            player.prepareAsync();
        } catch (Exception e) {
            Toast t = Toast.makeText(getApplicationContext(),
                    "Error on player.prepareAsync(): " + e,
                    Toast.LENGTH_LONG);
            t.show();
            Log.e(TAG, " prepareAsync exception");
            e.printStackTrace();
            setResult(RESULT_OK);
            finish();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        setResult(RESULT_OK);
        finish(); // no need to save/restore state

    }
    // End SurfaceHolder.Callback

    // Implement MediaPlayer.OnPreparedListener
    @Override
    public void onPrepared(MediaPlayer mp) {
        controller.setMediaPlayer(this);
        controller.setAnchorView((FrameLayout) findViewById(R.id.videoSurfaceContainer));
        player.start();
        controller.show(0);
    }
    // End MediaPlayer.OnPreparedListener

    // Implement VideoMediaController.MediaPlayerControl
    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        return player.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        return player.getDuration();
    }

    @Override
    public boolean isPlaying() {
        return player.isPlaying();
    }

    @Override
    public void pause() {
        player.pause();
    }

    @Override
    public void seekTo(int i) {
        player.seekTo(i);
    }

    @Override
    public void start() {
        player.start();
    }

    @Override
    public boolean isFullScreen() {
        return false;
    }

    @Override
    public void toggleFullScreen() {

    }

    // End VideoMediaController.MediaPlayerControl
    // @pm
    @Override
    public void onPause() {
        super.onPause();
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onStop() {
        super.onStop();
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onBackPressed() {
        // pause();
        // super.onBackPressed();
        controller.hide();
        player.release();
        setResult(RESULT_OK);
        finish(); // TODO Do we need to finish activity here?
    }

    @Override
    public void onResume() {
        super.onResume();
        // TODO something depending on player state
    }

    public void release() {
        player.release();
        setResult(RESULT_OK);
        finish();
    }

    public void reset() {
        player.reset();
        setResult(RESULT_OK);
        finish();
    }

    void fileDeleteDialog(String filePath) {
        final File file = new File(filePath);
        long fileLength = file.length();
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                synchronized (fileName) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            file.delete();
                            // crash RecordingListActivity.mBoxContentListAdapter.notifyDataSetChanged();
                            fileName.notify();
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            // just return
                            fileName.notify();
                            break;
                    }
                }
            }
        };
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setMessage("File \n" + file.getName() + " \nsize \t" + fileLength + "\nDelete?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

}
