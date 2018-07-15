package com.prsuhas.cordova.plugins.encryptedexoplayer;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.content.Intent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

import java.io.File;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ExoplayerActivity extends Activity {
    private static final String TAG = "EncryptedExoplayer";
    
    public static final String AES_ALGORITHM = "AES";
    public static final String AES_TRANSFORMATION = "AES/CTR/NoPadding";
    
    private String mVideoUri;    

    private Cipher mCipher;
    private SecretKeySpec mSecretKeySpec;
    private IvParameterSpec mIvParameterSpec;

    private File mEncryptedFile;
    private Context mContext;

    private PlayerView mPlayerView = null;
    private ProgressBar mProgressBar = null;

    private SimpleExoPlayer player = null;

    private byte[] key = null;
    private byte[] iv = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        key = new byte[16];
        iv = new byte[16];

        Bundle b = getIntent().getExtras();
        mVideoUri = b.getString("videoUri");        
        key = b.getString("key").getBytes();
        iv = getKeyBytes(key);

        RelativeLayout relLayout = new RelativeLayout(this);
        relLayout.setBackgroundColor(Color.BLACK);
        RelativeLayout.LayoutParams relLayoutParam = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        relLayoutParam.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        
        mPlayerView = new PlayerView(mContext);
        mPlayerView.setLayoutParams(relLayoutParam);
        relLayout.addView(mPlayerView);

        // Create progress throbber
        mProgressBar = new ProgressBar(this);
        mProgressBar.setIndeterminate(true);
        // Center the progress bar
        RelativeLayout.LayoutParams pblp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        pblp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        mProgressBar.setLayoutParams(pblp);
        // Add progress throbber to view
        relLayout.addView(mProgressBar);
        mProgressBar.bringToFront();

        mSecretKeySpec = new SecretKeySpec(key, AES_ALGORITHM);
        mIvParameterSpec = new IvParameterSpec(iv);

        try {
            mCipher = Cipher.getInstance(AES_TRANSFORMATION);
            mCipher.init(Cipher.DECRYPT_MODE, mSecretKeySpec, mIvParameterSpec);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mEncryptedFile = new File(Uri.parse(mVideoUri).toString());
        
        setContentView(relLayout, relLayoutParam);

        playVideo();        
    }

    private static byte[] getKeyBytes(final byte[] key) throws Exception {
        byte[] keyBytes = new byte[16];
        System.arraycopy(key, 0, keyBytes, 0, Math.min(key.length, keyBytes.length));
        return keyBytes;
    }    

    public void playVideo() {
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
        player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        SurfaceView mSurfaceView = (SurfaceView) mPlayerView.getVideoSurfaceView();
        mSurfaceView.setSecure(true);
        mPlayerView.setPlayer(player);

        DataSource.Factory dataSourceFactory = new EncryptedFileDataSourceFactory(mCipher, mSecretKeySpec,
                mIvParameterSpec, bandwidthMeter);
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

        try {
            Uri uri = Uri.fromFile(mEncryptedFile);
            Log.d(TAG, "uri: " + uri.toString());
            MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                    .setExtractorsFactory(extractorsFactory).createMediaSource(uri);
            player.prepare(videoSource);
            player.setPlayWhenReady(false);
            mProgressBar.setVisibility(View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "Pausing video.");
        if (player != null) {
            player.setPlayWhenReady(false);
            player.getPlaybackState();
        }
        super.onPause();
    }

    private void stop() {
        Log.d(TAG, "Stopping video.");
        if (player != null) {
            player.setPlayWhenReady(false);
            player.getPlaybackState();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
    }

    private void wrapItUp(int resultCode, String message) {
        Intent intent = new Intent();
        intent.putExtra("message", message);
        setResult(resultCode, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // If we're leaving, let's finish the activity
        wrapItUp(RESULT_OK, null);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // The screen size changed or the orientation changed... don't restart the
        // activity
        super.onConfigurationChanged(newConfig);
    }
}