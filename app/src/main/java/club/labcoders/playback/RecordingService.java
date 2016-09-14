package club.labcoders.playback;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.widget.Toast;

import java.io.FileDescriptor;

public class RecordingService extends Service {
    AudioRecord audioRecord;
    RecordingServiceWorker worker;
    static final int SAMPLE_RATE = 44100;
    final int bufferSize;
    volatile CircularShortBuffer mainBuffer;

    public RecordingService() {
        bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
    }

    @Override
    public void onCreate() {
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );
    }

    @Override
    public void onStart(Intent intent, int flags) {
        handleCommand(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleCommand(intent);
        return START_STICKY;
    }

    private synchronized void handleCommand(Intent intent) {
        if(audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
            throw new RuntimeException("Failed to initialize audio recorder.");
        if(audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
            return;
        audioRecord.startRecording();
        worker = new RecordingServiceWorker();
        worker.start();
        Toast.makeText(this, "Started listener service.", Toast.LENGTH_SHORT).show();
    }

    public boolean isRecording() {
        return worker != null && worker.stillRunning;
    }

    @Override
    public void onDestroy() {
        worker.pleaseStop();
    }

    @Override
    public RecordingServiceBinder onBind(Intent intent) {
        return new RecordingServiceBinder();
    }

    class RecordingServiceBinder extends Binder {
        public RecordingService getService() {
            return RecordingService.this;
        }
    }

    public class RecordingServiceWorker extends Thread {
        volatile boolean stillRunning;

        public RecordingServiceWorker() {
            this.stillRunning = true;
        }

        public void pleaseStop() {
            stillRunning = false;
        }

        @Override
        public void run() {
            try {
                mainLoop();
            }
            finally {
                audioRecord.stop();
            }
        }

        private void mainLoop() {
            short[] audioBuffer = new short[bufferSize / 2];
            while(stillRunning) {
                audioRecord.read(audioBuffer, 0, audioBuffer.length);

            }
        }
    }
}
