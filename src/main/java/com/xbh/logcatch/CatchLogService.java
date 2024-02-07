package com.xbh.logcatch;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xbh.logcatch.bean.LogState;
import com.xbh.logcatch.bean.StateCallBack;
import com.xbh.logcatch.util.ThreadPoolManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CatchLogService extends Service {

    private static final String TAG = "XBH_Catch_log";
    private static final String ACTION_CATCH_LOG = "com.xbh.logcatch.action.catch_log";
    private static final String EXTRA_PARAM1 = "com.xbh.logcatch.extra.PARAM1";
    private static final Object mLock = new Object();
    private boolean mStopCatch = true;
    private StateCallBack mCallBack;
    private File logDir;
    private File dateDir;
    MyHandler myHandler;
    static final class MyHandler extends Handler {
        private final Context ctx;
        public MyHandler(Context context) {
            super(context.getMainLooper());
            this.ctx = context;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0x01) {
                Toast.makeText(ctx, "Log clean success", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    public class MyBinder extends Binder {
        public CatchLogService getService() {
            return CatchLogService.this;
        }
    }

    public void setStateCallback(StateCallBack callback) {
        this.mCallBack = callback;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        myHandler = new MyHandler(this);
        logDir = new File(Environment.getExternalStorageDirectory() + "/xbh_log");
        checkAndCreate(logDir, true);
        newWorkThread();
    }

    public static void startAction(Context context, String param1) {
        Intent intent = new Intent(context, CatchLogService.class);
        intent.setAction(ACTION_CATCH_LOG);
        intent.putExtra(EXTRA_PARAM1, param1);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_CATCH_LOG.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                handleAction(param1);
            } else {
                if(readRunState(this) == LogState.STATE_RUNNING) {
                    handleAction("start");
                }
            }
        }
        return START_STICKY;
    }

    private void newWorkThread() {
        Runnable mMainRunnable = () -> shotTask("main");
        Runnable mSystemRunnable = () -> shotTask("system");
        Runnable mEventRunnable = () -> shotTask("events");
        Runnable mKernelRunnable = () -> shotTask("kernel");
        Runnable changeBuff = this::changeLogBuff;

        ThreadPoolManager.getInstance().submit(changeBuff);
        ThreadPoolManager.getInstance().submit(mMainRunnable);
        ThreadPoolManager.getInstance().submit(mSystemRunnable);
        ThreadPoolManager.getInstance().submit(mEventRunnable);
        ThreadPoolManager.getInstance().submit(mKernelRunnable);
    }

    private void changeLogBuff() {
        try {
            Runtime.getRuntime().exec("logcat -G 4m");
            Log.i(TAG, "change logcat buff to 4m");
        } catch (IOException e) {
            Log.e(TAG, "java.io.IOException ", e);
        }
    }

    private void shotTask(String type) {
        do {
            try {
                if (mStopCatch) {
                    synchronized (mLock) {
                        mLock.wait();
                    }
                }
                Log.i(TAG, "start to catch " + type + " log");

                checkAndCreate(logDir, true);
                checkAndCreate(dateDir, true);

                Process exec = Runtime.getRuntime().exec("logcat -v time -b " + type);
                InputStream inputStream = exec.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                File mainFile = new File(dateDir.getPath() + "/" + type + "_log.txt");
                checkAndCreate(mainFile, false);

                FileOutputStream fos = new FileOutputStream(mainFile);
                String line;
                while ((line = reader.readLine()) != null) {
                    fos.write((line + "\n").getBytes());
                    if (mStopCatch || !mainFile.exists() || !logDir.exists() || !dateDir.exists()) break;
                }

                fos.flush();
                fos.close();
                inputStream.close();
                reader.close();
                Log.i(TAG, "stop catching " + type + " log");

                Thread.sleep(1000);
            } catch (Exception e) {
                Log.e(TAG, "Exception " + type , e);
                break;
            }
        } while (true);
    }

    private static void saveRunState(Context context, String state) {
        SharedPreferences preferences = context.getSharedPreferences("catch_log", Context.MODE_PRIVATE);
        preferences.edit().putString("run_state", state).apply();
    }

    public static LogState readRunState(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("catch_log", Context.MODE_PRIVATE);
        return LogState.valueOf(preferences.getString("run_state", LogState.STATE_STOP.toString()));
    }

    private void handleAction(String param1) {
        switch (param1) {
            case "start":
                startCatchLog();
                if (mCallBack != null) mCallBack.onStateChange(LogState.STATE_RUNNING);
                break;
            case "stop":
                mStopCatch = true;
                saveRunState(this, LogState.STATE_STOP.toString());
                if (mCallBack != null) mCallBack.onStateChange(LogState.STATE_STOP);
                break;
            case "clean":
                mStopCatch = true;
                saveRunState(this, LogState.STATE_STOP.toString());
                if (mCallBack != null) mCallBack.onStateChange(LogState.STATE_STOP);

                ThreadPoolManager.getInstance().submit(() -> {
                    deleteChildFile(logDir, false);
                    myHandler.sendEmptyMessage(0x01);
                });
        }
    }

    private void checkAndCreate(File file, boolean isDir) {
        if (!file.exists()) {
            boolean res = false;
            if (isDir) {
                res = file.mkdir();
                Log.i(TAG, "checkAndCreateDir mkdir " + file.getPath()+ " : " + res);
            } else {
                try {
                    res = file.createNewFile();
                } catch (IOException e) {
                    Log.e(TAG, "java.io.IOException ", e);
                }
                Log.i(TAG, "checkAndCreate file " + file.getPath() + " : " + res);
            }
        }
    }

    private String getTimeFormat(long time) {
        SimpleDateFormat sdf = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
        sdf.applyPattern("yyyy-MM-dd-HH-mm-ss");
        return sdf.format(new Date(time));
    }

    private void startCatchLog() {
        String formattedTime = getTimeFormat(System.currentTimeMillis());
        dateDir = new File(logDir.getPath() + "/" + formattedTime);
        checkAndCreate(dateDir, true);

        mStopCatch = false;

        synchronized (mLock) {
            mLock.notifyAll();
        }
        saveRunState(this, LogState.STATE_RUNNING.toString());
    }

    /**
     * 递归删除文件和文件夹
     *
     * @param file
     *            要删除的根目录
     */
    public void deleteChildFile(File file, boolean deleteRoot) {
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] childFile = file.listFiles();
                if (childFile == null || childFile.length == 0) {
                    if (deleteRoot) {
                        file.delete();
                    }
                    return;
                }
                for (File f : childFile) {
                    deleteChildFile(f, true);
                }
                if (deleteRoot) {
                    file.delete();
                }
            } else if (file.isFile()) {
                file.delete();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ThreadPoolManager.getInstance().shutdown();
        myHandler = null;
    }
}