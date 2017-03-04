package com.rinkesh.btconnector.incomming;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeechService;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.Nullable;
import android.util.Log;

import com.rinkesh.btconnector.db.ContactDetail;
import com.rinkesh.btconnector.db.GeneralDBManager;
import com.rinkesh.btconnector.util.Utils;

import java.util.Locale;


public class CallerTTSService extends Service implements TextToSpeech.OnInitListener {
    private static final String TAG = CallerTTSService.class.getName();
    private TextToSpeech tts;
    private final String UTTERANCE_ID = "FINISHED_PLAYING";
    private boolean isInit;
    private String texts;
    private int currentRingVolume;
    private int currentMusicVolume;
    private boolean pauseRingtone = true;

    AudioManager audioManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private UtteranceProgressListener utteranceProgressListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {

        }

        @Override
        public void onDone(String utteranceId) {
            if (utteranceId.equals(UTTERANCE_ID)) {
                if (Utils.isCallActive(getApplicationContext())) {
                    speak();
                } else {
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, currentRingVolume, 0);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentMusicVolume, 0);
                    stopSelf();
                }
            }
        }

        @Override
        public void onError(String utteranceId) {
            stopSelf();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        tts = new TextToSpeech(getApplicationContext(), this);
        tts.setOnUtteranceProgressListener(utteranceProgressListener);
        Log.d(TAG, "onCreate");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        if (intent.getStringExtra("number") != null) {
            String number = intent.getStringExtra("number");
            ContactDetail contactDetail = GeneralDBManager.getInstance().getContactFromNumber(number);
            if (contactDetail != null) {
                String name = contactDetail.name;
                texts = name + " Calling Please answer";
            }
        }

        if (isInit) {
            speak();
        }

        return TextToSpeechService.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onInit(int status) {
        Log.d(TAG, "onInit");
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.getDefault());
            if (result != TextToSpeech.LANG_MISSING_DATA
                    && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                speak();
                isInit = true;
            }
        }
    }


    public void speak() {
        if (tts == null || texts == null || texts.equals("")) {
            return;
        }
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int ringVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
        int musicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        currentRingVolume = ringVolume;
        currentMusicVolume = musicVolume;

        if (ringVolume > 0) {
            audioManager.setStreamVolume(AudioManager.STREAM_RING, 1, AudioManager.FLAG_PLAY_SOUND);
        }

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(texts, TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
        }
    }

}
