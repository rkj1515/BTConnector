package com.rinkesh.btconnector.incomming;

import android.content.Context;
import android.content.Intent;

import java.util.Date;

import static com.rinkesh.btconnector.util.Utils.speechIntent;


public class CallReceiver extends PhoneCallReceiver {


    @Override
    protected void onIncomingCallStarted(Context ctx, String number, Date start) {
        speechIntent = new Intent(ctx, CallerTTSService.class);
        speechIntent.putExtra("number",number);
        ctx.startService(speechIntent);
    }

    @Override
    protected void onOutgoingCallStarted(Context ctx, String number, Date start) {
//        Can Announce Name for OutGoing Call
    }

    @Override
    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {
        ctx.stopService(speechIntent);
    }

    @Override
    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {
    }

    @Override
    protected void onMissedCall(Context ctx, String number, Date start) {
        ctx.stopService(speechIntent);
    }
}
