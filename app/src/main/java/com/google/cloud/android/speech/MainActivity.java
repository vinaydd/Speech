/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.android.speech;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.primitives.Bytes;
import com.google.protobuf.ByteString;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class MainActivity extends AppCompatActivity implements MessageDialogFragment.Listener {
    private static final String FRAGMENT_MESSAGE_DIALOG = "message_dialog";
    private static final String STATE_RESULTS = "results";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
    private SpeechService mSpeechService;
    private VoiceRecorder mVoiceRecorder;

    private  boolean subjectiveFlag;
    private  boolean objectiveFlag;

    int chiefComplaint = 0;

    int review = 0;
    private  String subjectiveText ="";
    private  String objectiveText="";

    boolean isFlag = false;
    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {
        @Override
        public void onVoiceStart() {
            showStatus(true);
            if (mSpeechService != null) {
                mSpeechService.startRecognizing(mVoiceRecorder.getSampleRate());
            }
        }

        @Override
        public void onVoice(byte[] data, int size) {
            if (mSpeechService != null) {
               mSpeechService.recognize(data, size);
               //attemptSend(data);

              /*  final File file = new File(Environment.getExternalStorageDirectory(), "recording.pcm");
                OutputStream os = null;
                try {
                    os = new FileOutputStream(file);
                    os.write(data);
                    byte[] dataz = fullyReadFileToBytes(file);
                    if (file.exists() && file.isFile())
                    {
                        file.delete();
                    }
                    attemptSend(dataz);
                } catch (Exception e) {
                    e.printStackTrace();
                }*/

                // Starts writing the bytes in it

            }
        }


        @Override
        public void onVoiceEnd() {
            showStatus(false);
            if (mSpeechService != null) {
                mSpeechService.finishRecognizing();
            }
        }

    };

    // Resource caches
    private int mColorHearing;
    private int mColorNotHearing;

    // View references
    private TextView mStatus;
    private TextView mText;
    private ResultAdapter mAdapter;
    private RecyclerView mRecyclerView;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mSpeechService = SpeechService.from(binder);
            mSpeechService.addListener(mSpeechServiceListener);
            mStatus.setVisibility(View.VISIBLE);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSpeechService = null;
        }

    };

    String m_androidId;

    Socket mSocket;

    TextView subjective,objective;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        subjective  = (TextView) findViewById(R.id.subjective);
        objective  =  (TextView) findViewById(R.id.objective);

        m_androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        {
            try {
                mSocket = IO.socket("http://13.234.239.51:8000");
            } catch (URISyntaxException e) {
            }
        }

        mSocket.connect();
        System.out.println("soketconnected:::" + mSocket.connected());

        final Resources resources = getResources();
        final Resources.Theme theme = getTheme();
        mColorHearing = ResourcesCompat.getColor(resources, R.color.status_hearing, theme);
        mColorNotHearing = ResourcesCompat.getColor(resources, R.color.status_not_hearing, theme);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mStatus = (TextView) findViewById(R.id.status);
        mText = (TextView) findViewById(R.id.text);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        final ArrayList<String> results = savedInstanceState == null ? null :
                savedInstanceState.getStringArrayList(STATE_RESULTS);
        mAdapter = new ResultAdapter(results);
        mRecyclerView.setAdapter(mAdapter);


        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                gotojoinSoket();
            }
        }, 300);

    }


    public byte[] fullyReadFileToBytes(File f) throws IOException {
        int size = (int) f.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis = new FileInputStream(f);
        ;
        try {

            int read = fis.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            fis.close();
        }

        return bytes;
    }

    private void attemptSend(byte[] data) {
        System.out.println(data);
      //  printBytes(data);

        String finalValues= printBytes(data);
        byte [] datas = printBytes(data).getBytes();
       // System.out.println(finalValues);


          //String  sdfs  = toCSV(data);


        try {


            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream w = new DataOutputStream(baos);

            w.writeInt(100);
            w.write(data);

            w.flush();

            byte[] result = baos.toByteArray();



            JSONObject jsonObject = new JSONObject();
            jsonObject.put("user_id", m_androidId);
            jsonObject.put("data", result);
           // jsonObject.put("source", "android");

            System.out.println(jsonObject);


            mSocket.emit("binaryData", jsonObject);
        } catch (Exception e) {
            Log.d("sqad", e.getMessage());
            e.printStackTrace();
        }
    }

    public static String toCSV(byte[] array) {
        String result = "";
        if (array.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (Byte s : array) {
                sb.append(s).append(",");
            }

            result = sb.deleteCharAt(sb.length() - 1).toString();
        }
        return result;
    }


   // Read more: https://javarevisited.blogspot.com/2016/03/how-to-convert-array-to-comma-separated-string-in-java.html#ixzz6EyacROUt


    public  String printBytes(byte[] array) {
        String output ="";
        for (int k = 0; k < array.length; k++) {

          //  output   = output + "\0x" + UnicodeFormatter.byteToHex(array[k]);
            output   = "utf8Bytes" + "[" + k + "] = " + "0x" + UnicodeFormatter.byteToHex(array[k]);
           // System.out.println("utf8Bytes" + "[" + k + "] = " + "0x" + UnicodeFormatter.byteToHex(array[k]));
        }
        return  output;
    }

    private void gotojoinSoket() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("user_id", m_androidId);
            jsonObject.put("data", "Connect");
            mSocket.emit("join", jsonObject);
        } catch (Exception e) {
            Log.d("sqad", e.getMessage());
            e.printStackTrace();
        }

        if (!isFlag) {
            isFlag = true;
            getStartStreming();
        }

    }

    private void getStartStreming() {
        String sampledata = "";
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("user_id", m_androidId);
            jsonObject.put("data", sampledata);
            mSocket.emit("startGoogleCloudStream", jsonObject);
        } catch (Exception e) {
            Log.d("sqad", e.getMessage());
            e.printStackTrace();
        }

    }


    public void stopeGoogleStreeming() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("user_id", m_androidId);
            jsonObject.put("data", "");
            mSocket.emit("endGoogleCloudStream", jsonObject);
        } catch (Exception e) {
            Log.d("sqad", e.getMessage());
            e.printStackTrace();
        }
        //  mSocket.disconnect();

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();
        mSocket.off("speechData", onNewMessage);
    }


    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    System.out.println("data:" + data);
                }
            });
        }
    };


    @Override
    protected void onStart() {
        super.onStart();
        // Prepare Cloud Speech API
        bindService(new Intent(this, SpeechService.class), mServiceConnection, BIND_AUTO_CREATE);

        // Start listening to voices
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecorder();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO)) {
            showPermissionMessageDialog();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    @Override
    protected void onStop() {
        // Stop listening to voice
        stopVoiceRecorder();
        // Stop Cloud Speech API
        mSpeechService.removeListener(mSpeechServiceListener);
        unbindService(mServiceConnection);
        mSpeechService = null;

        stopeGoogleStreeming();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            outState.putStringArrayList(STATE_RESULTS, mAdapter.getResults());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (permissions.length == 1 && grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecorder();
            } else {
                showPermissionMessageDialog();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_file:
                mSpeechService.recognizeInputStream(getResources().openRawResource(R.raw.audio));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
        }
        mVoiceRecorder = new VoiceRecorder(mVoiceCallback);
        mVoiceRecorder.start();
    }

    private void stopVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
            mVoiceRecorder = null;
        }
    }

    private void showPermissionMessageDialog() {
        MessageDialogFragment
                .newInstance(getString(R.string.permission_message))
                .show(getSupportFragmentManager(), FRAGMENT_MESSAGE_DIALOG);
    }

    private void showStatus(final boolean hearingVoice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStatus.setTextColor(hearingVoice ? mColorHearing : mColorNotHearing);
            }
        });
    }

    @Override
    public void onMessageDialogDismissed() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO_PERMISSION);
    }

    private final SpeechService.Listener mSpeechServiceListener = new SpeechService.Listener() {
        @Override
        public void onSpeechRecognized(final String text, final boolean isFinal) {
            if (isFinal) {
                mVoiceRecorder.dismiss();
            }
            if (mText != null && !TextUtils.isEmpty(text)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinal) {
                            mText.setText(null);
                         //   mAdapter.addResult(text);
                          //  mRecyclerView.smoothScrollToPosition(0);
                            if(subjectiveFlag){
                                if(subjectiveText !=null && subjectiveText.equalsIgnoreCase("")){
                                    subjectiveText=  text.trim();
                                }else {
                                    subjectiveText=  subjectiveText +" "+ text.trim();
                                }
                                String str =  subjectiveText
                                        .replaceAll("chief complaints","")
                                        .replaceAll("Chief Complaints","")
                                        .replaceAll("chief complaint","")
                                        .replaceAll("Chief Complaint","")
                                        .replaceAll("review of systems","")
                                        .replaceAll("Review of Systems","")
                                        .replaceAll("review of system","")
                                        .replaceAll("Review of System","");

                                if(str.equalsIgnoreCase("s")){

                                }else {
                                    subjective.setText(str);
                                }
                            }

                            if(objectiveFlag){
                                if(objectiveText!=null && objectiveText.equalsIgnoreCase("")){
                                    objectiveText=  text.trim();
                                }else {
                                    objectiveText=  objectiveText +" "+ text.trim();
                                }
                                String str =  objectiveText
                                        .replaceAll("chief complaints","")
                                        .replaceAll("Chief Complaints","")
                                        .replaceAll("chief complaint","")
                                        .replaceAll("Chief Complaint","")
                                        .replaceAll("review of systems","")
                                        .replaceAll("Review of Systems","")
                                        .replaceAll("review of system","")
                                        .replaceAll("Review of System","");
                                if(str.equalsIgnoreCase("s")){
                                }else {
                                    objective.setText(str);
                                }

                            }
                        } else {

                            Log.d("pre",text);
                            int chiefOccurCount =  count("chief complaint",text);
                            int reviewOccCount =  count("review of system",text);
                            if(chiefOccurCount> chiefComplaint){
                                Log.d("bbb",text);
                                subjectiveFlag = true;
                                objectiveFlag = false;
                                chiefComplaint+=1;
                            }
                            if(reviewOccCount>review){
                                subjectiveFlag = false;
                                objectiveFlag = true;
                                review+=1;
                            }
                            mText.setText(text);
                        }
                    }
                });
            }
        }
    };


    public static int count(String text, String find) {
        int index = 0, count = 0, length = find.length();
        while( (index = text.indexOf(find, index)) != -1 ) {
            index += length; count++;
        }
        return count;
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {

        TextView text;

        ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.item_result, parent, false));
            text = (TextView) itemView.findViewById(R.id.text);
        }

    }

    private static class ResultAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final ArrayList<String> mResults = new ArrayList<>();

        ResultAdapter(ArrayList<String> results) {
            if (results != null) {
                mResults.addAll(results);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.text.setText(mResults.get(position));
        }

        @Override
        public int getItemCount() {
            return mResults.size();
        }

        void addResult(String result) {
            mResults.add(0, result);
            notifyItemInserted(0);
        }

        public ArrayList<String> getResults() {
            return mResults;
        }

    }




}
