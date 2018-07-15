package com.prsuhas.cordova.plugins.encryptedexoplayer;

import org.apache.cordova.LOG;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.CipherOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class EncryptedExoplayer extends CordovaPlugin {    
    public static final String ACTION_ENCRYPT_VIDEO = "encrypt";
    public static final String ACTION_PLAY_VIDEO = "play";

    private static final int ACTIVITY_CODE_DOWNLOAD_MEDIA = 7;
    private static final int ACTIVITY_CODE_PLAY_MEDIA = 9;

    private CallbackContext callbackContext;

    public static final String AES_ALGORITHM = "AES";
    public static final String AES_TRANSFORMATION = "AES/CTR/NoPadding";

    private Cipher mCipher;
    private SecretKeySpec mSecretKeySpec;
    private IvParameterSpec mIvParameterSpec;

    byte[] key, iv;

    private static final String TAG = "EncryptedExoplayer";

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        LOG.d(TAG, "Initializing EncryptedExoplayer");
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;        

        if (ACTION_ENCRYPT_VIDEO.equals(action)) {
            String videoFile = args.getString(0);
            String outputPath = args.getString(1);
            String key = args.getString(2);

            return encryptVideo(videoFile, outputPath, key);
        } else if (ACTION_PLAY_VIDEO.equals(action)) {
            String videoUri = args.getString(0);
            String key = args.getString(1);

            return playVideo(ExoplayerActivity.class, videoUri, key);
        } else {
            callbackContext.error("EncryptedExoplayer." + action + " is not a supported method.");
            return false;
        }

        return false;
    }

    private static byte[] getKeyBytes(final byte[] key) throws Exception {
        byte[] keyBytes = new byte[16];
        System.arraycopy(key, 0, keyBytes, 0, Math.min(key.length, keyBytes.length));
        return keyBytes;
    }

    public boolean encryptVideo(final String videoFile, final String outputFile, final String mKey) {
        key = new byte[16];
        iv = new byte[16];

        key = mKey.getBytes();
        iv = getKeyBytes(key);

        mSecretKeySpec = new SecretKeySpec(key, AES_ALGORITHM);
        mIvParameterSpec = new IvParameterSpec(iv);        

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    Cipher encryptionCipher = Cipher.getInstance(AES_TRANSFORMATION);
                    encryptionCipher.init(Cipher.ENCRYPT_MODE, mSecretKeySpec, mIvParameterSpec);

                    InputStream inputStream = new FileInputStream(videoFile);
                    FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                    CipherOutputStream cipherOutputStream = new CipherOutputStream(fileOutputStream, encryptionCipher);

                    byte buffer[] = new byte[1024 * 1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        cipherOutputStream.write(buffer, 0, bytesRead);
                    }

                    inputStream.close();
                    cipherOutputStream.close();

                    // Remove unencrypted video file
                    try{
                        File unEncryptedFile = new File(Uri.parse(videoFile));
                        unEncryptedFile.delete();
                    }catch(Exception e){
                        e.printStackTrace();
                    }                    

                    callbackContext.success("File successfully encrypted.");

                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                }
            }
        });

        return true;
    }

    private boolean playVideo(final Class activityClass, final String videoUri, final String key) {
        final CordovaInterface cordovaObj = cordova;
        final CordovaPlugin plugin = this;

        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                final Intent streamIntent = new Intent(cordovaObj.getActivity().getApplicationContext(), activityClass);
                Bundle extras = new Bundle();
                extras.putString("action", "play");
                extras.putString("videoUri", videoUri);                
                extras.putString("key", key);

                cordovaObj.startActivityForResult(plugin, streamIntent, ACTIVITY_CODE_PLAY_MEDIA);
            }
        });
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.v(TAG, "onActivityResult: " + requestCode + " " + resultCode);
        super.onActivityResult(requestCode, resultCode, intent);
        if (ACTIVITY_CODE_PLAY_MEDIA == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                this.callbackContext.success();
            } else if (Activity.RESULT_CANCELED == resultCode) {
                String errMsg = "Error";
                if (intent != null && intent.hasExtra("message")) {
                    errMsg = intent.getStringExtra("message");
                }
                this.callbackContext.error(errMsg);
            }
        }
    }
    
}
