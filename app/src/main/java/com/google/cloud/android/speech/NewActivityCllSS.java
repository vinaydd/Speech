package com.google.cloud.android.speech;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class NewActivityCllSS extends AppCompatActivity {

    // the audio recording options
    private static final int RECORDING_RATE = 16000;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static String TAG = "AudioClient";
    // the minimum buffer size needed for audio recording
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL, FORMAT);
    byte[] bufferFinal;
    String m_androidId;
    boolean isFlag = false;
    Socket mSocket;
    TextView text ;
    // the button the user presses to send the audio stream to the server
    private Button sendAudioButton, stope;
    // the audio recorder
    private AudioRecord recorder;
    // are we currently sending audio data
    private boolean currentlySendingAudio = false;
    CountDownTimer  countDownTimer;
    private   String preText;
    private ResultAdapter mAdapter;
    private RecyclerView mRecyclerView;

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    // {"transcript":"liver is normal in size 13.6           ","isFinal":true}
                    try {
                        String transcript = data.getString("transcript");
                        boolean isFinal = data.getBoolean("isFinal");


                        text.setText(transcript);
                        getTimerCheckTimeDeffirence(transcript);
                       /* if(isFinal){
                            text.setText("");
                            gotoBottomSet(list);
                        }else {
                            text.setText(transcript);
                        }*/
                        System.out.println(data);
                    } catch (Exception e) {

                    }
                }
            });
        }
    };

    private void gotoBottomSet(String String ) {
        text.setText(null);
        mAdapter.addResult(String);
        mRecyclerView.smoothScrollToPosition(0);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_activity);
        text = (TextView) findViewById(R.id.text);

        Log.i(TAG, "Creating the Audio Client with minimum buffer of " + BUFFER_SIZE + " bytes");
        m_androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        {
            try {
                mSocket = IO.socket("http://13.234.239.51:8000");
            } catch (URISyntaxException e) {
            }
        }
        mSocket.connect();
        System.out.println("soketconnected:::" + mSocket.connected());
        // set up the button
        sendAudioButton = (Button) findViewById(R.id.btnStart);
        stope = (Button) findViewById(R.id.btnStop);
        sendAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStreamingAudio();
            }
        });
        stope.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopStreamingAudio();
            }
        });


        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ArrayList<String> results =new ArrayList<>();
        mAdapter = new ResultAdapter(results);
        mRecyclerView.setAdapter(mAdapter);
    }
    private void startStreamingAudio() {
        Log.i(TAG, "Starting the audio stream");
        gotojoinSoket();
    }
    private void stopStreamingAudio() {
        Log.i(TAG, "Stopping the audio stream");
        currentlySendingAudio = false;
        isFlag = false;
        recorder.release();
        stopeGoogleStreeming();
    }

    private void attemptSend(byte[] data) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("user_id", m_androidId);
            jsonObject.put("data", data);
            jsonObject.put("source", "android");
            mSocket.emit("binaryData", jsonObject);
        } catch (Exception e) {
            Log.d("sqad", e.getMessage());
            e.printStackTrace();
        }
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
        currentlySendingAudio = true;
        mSocket.on("speechData", onNewMessage);
        startStreaming();

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
    }

    @Override
    public void onDestroy() {
        countDownTimer.cancel();
        super.onDestroy();
        mSocket.disconnect();
        mSocket.off("speechData", onNewMessage);
    }

    private void startStreaming() {
        Log.i(TAG, "Starting the background thread to stream the audio data");
        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDING_RATE, CHANNEL, FORMAT, BUFFER_SIZE * 4);

                    Log.d(TAG, "AudioRecord recording...");
                    recorder.startRecording();
                    while (currentlySendingAudio == true) {
                        // read the data into the buffer
                        int read = recorder.read(buffer, 0, buffer.length);
                        if (read > 0) {
                           // Log.d(TAG, "AudioRecord recording buffer..." + buffer);
                        }
                        bufferFinal = buffer;
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DataOutputStream w = new DataOutputStream(baos);
                        w.writeInt(100);
                        w.write(bufferFinal);
                        w.flush();
                        byte[] result = baos.toByteArray();
                        attemptSend(result);
                        bufferFinal = null;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception: " + e);
                }
            }
        });
        streamThread.start();
    }

    public void  getTimerCheckTimeDeffirence(final String list){
        if(countDownTimer!=null){
            countDownTimer.cancel();
        }
        countDownTimer =  new CountDownTimer(2000,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }
            @Override
            public void onFinish() {
                text.setText("");
              //  String  inputeText   =  removeMatchStringFromStart(list);

                preText  =  list;
                System.out.println(list);
                gotoBottomSet(list);

                //  stopStreamingAudio();
                // gotojoinSoket();


            }
        }.start();
    }


    private static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.item_result, parent, false));
            text = (TextView) itemView.findViewById(R.id.text);
        }

    }

    private  class ResultAdapter extends RecyclerView.Adapter<ViewHolder> {
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

    public  String removeMatchStringFromStart(String currentText){
        System.out.println(currentText);
        System.out.println(preText);
        if (currentText != null && preText!=null && preText.length()>0 && currentText.startsWith(preText)) {
            return currentText.split(preText)[1];
        }
        return currentText;
    }


}