package com.rinkesh.btconnector.service;

/**
 * Created by rinkesh on 04/03/17.
 */


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.rinkesh.btconnector.db.ContactDetail;
import com.rinkesh.btconnector.db.GeneralDBManager;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoiceService extends Service {
    static final int MSG_RECOGNIZER_CANCEL = 2;
    static final int MSG_RECOGNIZER_START_LISTENING = 1;
    private static final String TAG = "VoiceService";
    public static int status;
    private volatile boolean mIsAlive;
    private boolean mIsListening;
    private CountDownTimer mNoSpeechCountDown;
    private final Messenger mServerMessenger;
    private SpeechRecognizer mSpeechRecognizer;
    private Intent mSpeechRecognizerIntent;
    private AudioManager mAudioManager;


    private static class IncomingHandler extends Handler {
        private VoiceService mtarget;

        IncomingHandler(VoiceService target) {
            this.mtarget = target;
        }

        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case VoiceService.MSG_RECOGNIZER_START_LISTENING /*1*/:
                        if (VoiceService.status == VoiceService.MSG_RECOGNIZER_START_LISTENING) {
                            if (VERSION.SDK_INT >= 16) {
                                this.mtarget.mNoSpeechCountDown.start();
                            }
                            if (!this.mtarget.mIsListening) {
                                this.mtarget.mSpeechRecognizer.startListening(this.mtarget.mSpeechRecognizerIntent);
                                this.mtarget.mIsListening = true;
                                this.mtarget.mIsAlive = false;
                                return;
                            }
                            return;
                        }
                        return;
                    case VoiceService.MSG_RECOGNIZER_CANCEL /*2*/:
                        this.mtarget.mSpeechRecognizer.cancel();
                        this.mtarget.mIsListening = false;
                        return;
                    default:
                        return;
                }
            } catch (Exception e) {
                Log.d("VoiceServicehandle msg", "failed");
            }
            Log.d("VoiceServicehandle msg", "failed");
        }
    }


    public VoiceService() {
        this.mServerMessenger = new Messenger(new IncomingHandler(this));
        this.mNoSpeechCountDown = new Counter(2000, 5);
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int startID) {
        super.onStartCommand(intent, flags, startID);
        status = MSG_RECOGNIZER_START_LISTENING;
        Log.d(TAG, "available = " + SpeechRecognizer.isRecognitionAvailable(this));
        Log.d(TAG, "On Start Command");
        return START_STICKY;
    }

    public void onDestroy() {
        status = 0;
        if (VERSION.SDK_INT >= 16) {
            this.mNoSpeechCountDown.cancel();
        }
        if (this.mSpeechRecognizer != null) {
            try {
                this.mSpeechRecognizer.stopListening();
                this.mSpeechRecognizer.cancel();
                this.mSpeechRecognizer.destroy();
                this.mSpeechRecognizer = null;
                updateSoundStream(false);
            } catch (Exception e) {
                Log.d("Recognizer destroy", "failed");
            }
        }
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Intent restartServiceTask = new Intent(getApplicationContext(), VoiceService.class);
        restartServiceTask.setPackage(getPackageName());
        PendingIntent restartPendingIntent = PendingIntent.getService(getApplicationContext(), 1, restartServiceTask, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager myAlarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        myAlarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartPendingIntent);
        Log.d(TAG, "OnTaskRemoved");
    }

    @Override
    public void onCreate() {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        this.mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        this.mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
        this.mSpeechRecognizerIntent = new Intent("android.speech.action.RECOGNIZE_SPEECH");
        this.mSpeechRecognizerIntent.putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form");
        this.mSpeechRecognizerIntent.putExtra("calling_package", getPackageName());
        resetListener();
        Log.d(TAG, "OnCreate");
        super.onCreate();
    }

    public void resetListener() {
        try {
            updateSoundStream(true);
            this.mServerMessenger.send(Message.obtain(null, MSG_RECOGNIZER_CANCEL));
            this.mServerMessenger.send(Message.obtain(null, MSG_RECOGNIZER_START_LISTENING));
        } catch (RemoteException e) {
        }
    }

    private void updateSoundStream(boolean isTrue) {
        mAudioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, isTrue);
        mAudioManager.setStreamMute(AudioManager.STREAM_ALARM, isTrue);
        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, isTrue);
        mAudioManager.setStreamMute(AudioManager.STREAM_RING, isTrue);
        mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, isTrue);
    }


    private void doCheckForCall(StringBuilder s) {
        if (s.toString().contains("call")) {
            Pattern p = Pattern.compile("(?<=(call) )(\\w+)");
            Matcher m = p.matcher(s);
            while (m.find()) {
                System.out.println(m.group());
                ContactDetail contactDetail = GeneralDBManager.getInstance().getContactFromName(m.group());
                if (contactDetail != null) {
                    String phoneNumber = contactDetail.phoneNumber;
                    Intent intent = new Intent("com.rinkesh.callnumber");
                    intent.putExtra("number", phoneNumber);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
//                    sendBroadcast(intent);
//                    callUser(phoneNumber);
                    break;
                }
            }

        }
    }


    private class SpeechRecognitionListener implements RecognitionListener {
        private SpeechRecognitionListener() {
        }

        public void onBeginningOfSpeech() {
            VoiceService.this.mIsAlive = true;
        }

        public void onBufferReceived(byte[] buffer) {
        }

        public void onEndOfSpeech() {
        }

        public void onError(int error) {
            Log.d(TAG, "OnError");
            VoiceService.this.resetListener();
        }

        public void onEvent(int eventType, Bundle params) {
        }

        public void onPartialResults(Bundle partialResults) {
        }

        public void onReadyForSpeech(Bundle params) {
        }

        public void onResults(Bundle results) {

            ArrayList<String> arrayList = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            Log.d(TAG, arrayList.toString());
            Toast.makeText(getApplicationContext(), arrayList.toString(), Toast.LENGTH_SHORT).show();
            StringBuilder sb = new StringBuilder();
            for (String result : arrayList) {
                sb.append(result).append(" ");
            }
            doCheckForCall(sb);
            VoiceService.this.resetListener();
        }


        public void onRmsChanged(float rmsdB) {
        }
    }

    private class Counter extends CountDownTimer {
        Counter(long millisInFuture, long interval) {
            super(millisInFuture, interval);
        }

        public void onTick(long millisUntilFinished) {
        }

        public void onFinish() {
            if (!VoiceService.this.mIsAlive) {
                VoiceService.this.resetListener();
            }
            start();
        }
    }

}

