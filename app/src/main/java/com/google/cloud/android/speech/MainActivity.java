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
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.type.Color;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Locale;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class MainActivity extends AppCompatActivity implements MessageDialogFragment.Listener {
    private static final String FRAGMENT_MESSAGE_DIALOG = "message_dialog";
    private static final String STATE_RESULTS = "results";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
    int chiefComplaint = 0;
    int review = 0;
    int hpi_count = 0;
    boolean isFlag = false;
    String m_androidId;
    Socket mSocket;
    TextView subjective, objective, hpi;
    Button btn;
    private SpeechService mSpeechService;
    private VoiceRecorder mVoiceRecorder;
    private boolean chiefFlag;
    private boolean reviewFlag;
    private boolean hpiFlag;
    private String chiefText = "";
    private String reviewText = "";
    private String hpi_text = "";
    private TextToSpeech textToSpeech;
    // Resource caches
    private int mColorHearing;
    private int mColorNotHearing;
    // View references
    private TextView mStatus;
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
    private TextView mText;
    private ResultAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private boolean isSetFlag;
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
                        if (isSetFlag) {
                            if (isFinal) {
                                mText.setText(null);
                                //   mAdapter.addResult(text);
                                //  mRecyclerView.smoothScrollToPosition(0);
                                if (chiefFlag) {
                                    if (chiefText != null && chiefText.equalsIgnoreCase("")) {
                                        chiefText = text.trim();
                                    } else {
                                        chiefText = chiefText.trim() + " " + text.trim();
                                    }
                                    String str = chiefText
                                            .replaceAll("chief complaints", "")
                                            .replaceAll("Chief Complaints", "")
                                            .replaceAll("chief complaint", "")
                                            .replaceAll("Chief Complaint", "")
                                            .replaceAll("review of systems", "")
                                            .replaceAll("Review of Systems", "")
                                            .replaceAll("review of system", "")
                                            .replaceAll("Review of System", "");

                                    if (str.equalsIgnoreCase("s")) {

                                    } else {
                                        subjective.setText(str);
                                    }
                                }

                                if (reviewFlag) {
                                    if (reviewText != null && reviewText.equalsIgnoreCase("")) {
                                        reviewText = text.trim();
                                    } else {
                                        reviewText = reviewText.trim() + " " + text.trim();
                                    }
                                    String str = reviewText
                                            .replaceAll("chief complaints", "")
                                            .replaceAll("Chief Complaints", "")
                                            .replaceAll("chief complaint", "")
                                            .replaceAll("Chief Complaint", "")
                                            .replaceAll("review of systems", "")
                                            .replaceAll("Review of Systems", "")
                                            .replaceAll("review of system", "")
                                            .replaceAll("Review of System", "");
                                    if (str.equalsIgnoreCase("s")) {
                                    } else {
                                        objective.setText(str);
                                    }
                                }


                                if (hpiFlag) {
                                    if (hpi_text != null && hpi_text.equalsIgnoreCase("")) {
                                        hpi_text = text.trim();
                                    } else {
                                        hpi_text = hpi_text.trim() + " " + text.trim();
                                    }
                                    String str = hpi_text
                                            .replaceAll("hpi", "")
                                            .replaceAll("Hpi", "")
                                            .replaceAll("HPI", "");
                                    if (str.equalsIgnoreCase("s")) {
                                    } else {
                                        hpi.setText(str);
                                    }
                                }


                            } else {

                                Log.d("pre", text);
                                int chiefOccurCount = count("chief complaint", text);
                                int reviewOccCount = count("review of system", text);
                                int hpiOccCount = count("hpi", text);
                                if (chiefOccurCount > chiefComplaint) {
                                    Log.d("pre", "sub:" + text);
                                    chiefFlag = true;
                                    reviewFlag = false;
                                    hpiFlag = false;

                                    //chiefComplaint+=1;
                                }
                                if (reviewOccCount > review) {
                                    Log.d("pre", "obj:" + text);
                                    chiefFlag = false;
                                    reviewFlag = true;
                                    hpiFlag = false;
                                    //review+=1;
                                }
                                if (hpiOccCount > hpi_count) {
                                    Log.d("pre", "obj:" + text);
                                    chiefFlag = false;
                                    reviewFlag = false;
                                    hpiFlag = true;
                                }
                                mText.setText(text);
                            }
                        }
                    }
                });
            }
        }
    };
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

    public static int count(String text, String find) {
        int index = 0, count = 0, length = find.length();
        while ((index = text.indexOf(find, index)) != -1) {
            index += length;
            count++;
        }
        return count;
    }


    // Read more: https://javarevisited.blogspot.com/2016/03/how-to-convert-array-to-comma-separated-string-in-java.html#ixzz6EyacROUt

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = (Button) findViewById(R.id.button);
        subjective = (TextView) findViewById(R.id.subjective);
        objective = (TextView) findViewById(R.id.objective);
        hpi = (TextView) findViewById(R.id.hpi);

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


        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int ttsLang = textToSpeech.setLanguage(Locale.US);
                    if (ttsLang == TextToSpeech.LANG_MISSING_DATA
                            || ttsLang == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "The Language is not supported!");
                    } else {
                        Log.i("TTS", "Language Supported.");
                    }
                    Log.i("TTS", "Initialization success.");
                } else {
                    Toast.makeText(getApplicationContext(), "TTS Initialization failed!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (!isSetFlag) {
                    AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
                    String data = "Hi Doctor please proceed";
                    Log.i("TTS", "button clicked: " + data);
                    int speechStatus = textToSpeech.speak(data, TextToSpeech.QUEUE_FLUSH, null);
                    if (speechStatus == TextToSpeech.ERROR) {
                        Log.e("TTS", "Error in converting Text to Speech!");
                    }
                    btn.setText("stop dictation");
                    btn.setBackgroundColor(getResources().getColor(R.color.red));
                    isSetFlag = true;
                } else {
                    btn.setText("start dictation");
                    btn.setBackgroundColor(getResources().getColor(R.color.accent));
                    isSetFlag = false;
                }

            }

        });

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

        String finalValues = printBytes(data);
        byte[] datas = printBytes(data).getBytes();
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

    public String printBytes(byte[] array) {
        String output = "";
        for (int k = 0; k < array.length; k++) {

            //  output   = output + "\0x" + UnicodeFormatter.byteToHex(array[k]);
            output = "utf8Bytes" + "[" + k + "] = " + "0x" + UnicodeFormatter.byteToHex(array[k]);
            // System.out.println("utf8Bytes" + "[" + k + "] = " + "0x" + UnicodeFormatter.byteToHex(array[k]));
        }
        return output;
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
