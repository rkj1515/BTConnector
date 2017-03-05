package com.rinkesh.btconnector.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.rinkesh.btconnector.R;
import com.rinkesh.btconnector.db.ContactDetail;
import com.rinkesh.btconnector.db.GeneralDBManager;
import com.rinkesh.btconnector.service.VoiceService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {

    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    private TextToSpeech t1;
    private TextView mStatusTv;
    private Button mActivateBtn;
    private Button mPairedBtn;
    private Button mScanBtn;
    private ProgressDialog mProgressDlg;
    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<>();
    private BluetoothAdapter mBluetoothAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initVariable();
        registerListner();
        MainActivityPermissionsDispatcher.fetchContactsWithCheck(this, getApplicationContext());
        MainActivityPermissionsDispatcher.recordAudioAndStartServiceWithCheck(this);

        fetchContacts(this);
        recordAudioAndStartService();
    }

    private void registerListner() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);

//        IntentFilter filter1 = new IntentFilter();
//        filter.addAction("com.rinkesh.callnumber");
//        registerReceiver(broadcastReceiver, filter1);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter("com.rinkesh.callnumber"));
    }

    @Override
    protected void onResume() {
//        Intent intent=new Intent("com.rinkesh.serviceresume");
//        intent.putExtra("isResume",true);
//        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.rinkesh.serviceresume"));
//        isServiceRunning=false;
//        recordAudioAndStartService();
        super.onResume();
    }



    private void initVariable() {
        mStatusTv = (TextView) findViewById(R.id.tv_status);
        mActivateBtn = (Button) findViewById(R.id.btn_enable);
        mPairedBtn = (Button) findViewById(R.id.btn_view_paired);
        mScanBtn = (Button) findViewById(R.id.btn_scan);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mProgressDlg = new ProgressDialog(this);
        mProgressDlg.setMessage("Scanning...");
        mProgressDlg.setCancelable(false);

        mProgressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mBluetoothAdapter.cancelDiscovery();
            }
        });

        if (mBluetoothAdapter == null) {
            showUnsupported();
        } else {
            mPairedBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                    if (pairedDevices == null || pairedDevices.size() == 0) {
                        showToast("No Paired Devices Found");
                    } else {
                        ArrayList<BluetoothDevice> list = new ArrayList<>();
                        list.addAll(pairedDevices);
                        Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
                        intent.putParcelableArrayListExtra("device.list", list);
                        startActivity(intent);
                    }
                }
            });

            mScanBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    mBluetoothAdapter.startDiscovery();
                }
            });
            mActivateBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mBluetoothAdapter.isEnabled()) {
                        mBluetoothAdapter.disable();
                        showDisabled();
                    } else {
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(intent, 1000);
                    }
                }
            });
            if (mBluetoothAdapter.isEnabled()) {
                showEnabled();
            } else {
                showDisabled();
            }
        }
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.rinkesh.callnumber")) {
                callUser(intent.getStringExtra("number"));
            }
        }
    };

    @NeedsPermission(Manifest.permission.CALL_PHONE)
    void callUser(String s) {
        if (s != null) {
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + s));
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            startActivity(intent);
        } else {
            openSystemVoice();
        }
    }

    @OnShowRationale(Manifest.permission.CALL_PHONE)
    void showRationaleForCall(final PermissionRequest request) {
        Toast.makeText(this, "Do Something", Toast.LENGTH_SHORT).show();
    }

    @OnPermissionDenied(Manifest.permission.CALL_PHONE)
    void showDeniedForCall() {
        Toast.makeText(this, "Permission Denied for Call", Toast.LENGTH_SHORT).show();
    }

    @OnNeverAskAgain(Manifest.permission.CALL_PHONE)
    void showNeverAskForCall() {
        Toast.makeText(this, "Never Ask Called", Toast.LENGTH_SHORT).show();
    }

    boolean isServiceRunning = false;

    //TODO
    @NeedsPermission(Manifest.permission.RECORD_AUDIO)
    void recordAudioAndStartService() {
        if (!isServiceRunning) {
            isServiceRunning = true;
            startService(new Intent(this, VoiceService.class));
        }
    }

    @OnShowRationale(Manifest.permission.RECORD_AUDIO)
    void showRationaleForAudio(final PermissionRequest request) {
        Toast.makeText(this, "Do Something", Toast.LENGTH_SHORT).show();
    }

    @OnPermissionDenied(Manifest.permission.RECORD_AUDIO)
    void showDeniedForAudio() {
        Toast.makeText(this, "Permission Denied for Call", Toast.LENGTH_SHORT).show();
    }

    @OnNeverAskAgain(Manifest.permission.RECORD_AUDIO)
    void showNeverAskForAudio() {
        Toast.makeText(this, "Never Ask Called", Toast.LENGTH_SHORT).show();
    }


    public void openSystemVoice() {
        List<ResolveInfo> activities = getPackageManager().queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() > 0) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "BT Connector APP");
            startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            final Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.setPackage("com.google.android.googlequicksearchbox");
            intent.putExtra(SearchManager.QUERY, matches.get(0));
            startActivity(intent);
        }
//        callUser("9406977555");
    }


    @Override
    public void onPause() {
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
        }
        Intent intent = new Intent("com.rinkesh.serviceresume");
        intent.putExtra("isPause",true);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    private void showEnabled() {
        mStatusTv.setText("Bluetooth is On");
        mStatusTv.setTextColor(Color.BLUE);

        mActivateBtn.setText("Disable");
        mActivateBtn.setEnabled(true);

        mPairedBtn.setEnabled(true);
        mScanBtn.setEnabled(true);
    }

    private void showDisabled() {
        mStatusTv.setText("Bluetooth is Off");
        mStatusTv.setTextColor(Color.RED);

        mActivateBtn.setText("Enable");
        mActivateBtn.setEnabled(true);

        mPairedBtn.setEnabled(false);
        mScanBtn.setEnabled(false);
    }

    private void showUnsupported() {
        mStatusTv.setText("Bluetooth is unsupported by this device");

        mActivateBtn.setText("Enable");
        mActivateBtn.setEnabled(false);

        mPairedBtn.setEnabled(false);
        mScanBtn.setEnabled(false);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    showToast("Enabled");
                    showEnabled();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                mDeviceList = new ArrayList<>();
                mProgressDlg.show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mProgressDlg.dismiss();
                Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                newIntent.putParcelableArrayListExtra("device.list", mDeviceList);
                startActivity(newIntent);
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDeviceList.add(device);
                showToast("Found device " + device.getName());
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }


    @OnShowRationale(Manifest.permission.READ_CONTACTS)
    void showRationaleForReadContact(final PermissionRequest request) {
        Toast.makeText(this, "Do Something", Toast.LENGTH_SHORT).show();
    }

    @OnPermissionDenied(Manifest.permission.READ_CONTACTS)
    void showDeniedForReadContact() {
        Toast.makeText(this, "Permission Denied for Call", Toast.LENGTH_SHORT).show();
    }

    @OnNeverAskAgain(Manifest.permission.READ_CONTACTS)
    void showNeverAskForReadContact() {
        Toast.makeText(this, "Never Ask Called", Toast.LENGTH_SHORT).show();
    }


    @NeedsPermission(Manifest.permission.READ_CONTACTS)
    public void fetchContacts(final Context context) {
//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                String phoneNumber = null;
//                Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
//                String _ID = ContactsContract.Contacts._ID;
//                String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
//                String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;
//                Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
//                String Phone_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
//                String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;
//                StringBuffer output = new StringBuffer();
//                ContentResolver contentResolver = getContentResolver();
//                Cursor cursor = contentResolver.query(CONTENT_URI, null, null, null, null);
//                if (GeneralDBManager.getInstance().getCount() != 0) {
//                    return;
//                }
//                if (cursor.getCount() > 0) {
//
//                    while (cursor.moveToNext()) {
//                        String contact_id = cursor.getString(cursor.getColumnIndex(_ID));
//                        String name = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));
//                        long hasPhoneNumber = Long.parseLong(cursor.getString(cursor.getColumnIndex(HAS_PHONE_NUMBER)));
//                        if (hasPhoneNumber > 0) {
//                            output.append("\n Name:" + name);
//                            Cursor phoneCursor = contentResolver.query(PhoneCONTENT_URI, null, Phone_CONTACT_ID + " = ?", new String[]{contact_id}, null);
//                            while (phoneCursor.moveToNext()) {
//                                phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(NUMBER));
//                                output.append("\n Number:" + phoneNumber);
//                                System.out.println("Number:::::" + phoneNumber);
//                                System.out.println("Contact:::::" + output.toString());
//                                if (!GeneralDBManager.getInstance().isContactAvailable(contact_id)) {
//                                    ContactDetail contactDetail = new ContactDetail();
//                                    contactDetail.contactId = contact_id;
//                                    contactDetail.name = name;
//                                    contactDetail.phoneNumber = phoneNumber;
//                                    contactDetail.save();
//                                }
//                            }
//                            phoneCursor.close();
//                        }
//                    }
//                }
//            }
//        });
//        thread.start();
        new ContactSync().execute(this);
    }


    public class ContactSync extends AsyncTask<Context, Integer, String> {
        ProgressDialog pd = new ProgressDialog(MainActivity.this);
        int totalCount = 0;

        @Override
        protected void onPreExecute() {
            pd.setMessage("Please wait contact detail sync in progress...");
            pd.setCancelable(false);
            pd.show();
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
//            pd.setMessage("Please Wait.." + values + "/" + totalCount);
            pd.setProgress(values[0]);
            super.onProgressUpdate(values);
        }

        @Override
        protected String doInBackground(Context... params) {
            String phoneNumber = null;
            Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
            String _ID = ContactsContract.Contacts._ID;
            String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
            String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;
            Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            String Phone_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
            String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;
            StringBuffer output = new StringBuffer();
            ContentResolver contentResolver = getContentResolver();
            Cursor cursor = contentResolver.query(CONTENT_URI, null, null, null, null);
            if (GeneralDBManager.getInstance().getCount() != 0) {
                return null;
            }
            int count = 0;
            totalCount = cursor.getCount();
            if (cursor.getCount() > 0) {
                count++;
                while (cursor.moveToNext()) {
                    String contact_id = cursor.getString(cursor.getColumnIndex(_ID));
                    String name = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));
                    long hasPhoneNumber = Long.parseLong(cursor.getString(cursor.getColumnIndex(HAS_PHONE_NUMBER)));
                    if (hasPhoneNumber > 0) {
                        publishProgress(count);
                        output.append("\n Name:" + name);
                        Cursor phoneCursor = contentResolver.query(PhoneCONTENT_URI, null, Phone_CONTACT_ID + " = ?", new String[]{contact_id}, null);
                        while (phoneCursor.moveToNext()) {
                            phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(NUMBER));
                            output.append("\n Number:" + phoneNumber);
                            System.out.println("Number:::::" + phoneNumber);
                            System.out.println("Contact:::::" + output.toString());
                            if (!GeneralDBManager.getInstance().isContactAvailable(contact_id)) {
                                ContactDetail contactDetail = new ContactDetail();
                                contactDetail.contactId = contact_id;
                                contactDetail.name = name;
                                contactDetail.phoneNumber = phoneNumber;
                                contactDetail.save();
                            }
                        }
                        phoneCursor.close();
                    }
                }
                cursor.close();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String o) {
            if (pd.isShowing()) {
                pd.dismiss();
            }
            super.onPostExecute(o);
        }

    }
}
