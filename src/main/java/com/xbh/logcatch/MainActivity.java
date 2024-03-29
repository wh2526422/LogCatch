package com.xbh.logcatch;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.xbh.logcatch.bean.LogState;
import com.xbh.logcatch.bean.StateCallBack;
import com.xbh.logcatch.databinding.ActivityMainBinding;
import com.xbh.logcatch.util.DialogHelper;
import com.xbh.logcatch.util.FileUtils;
import com.xbh.logcatch.util.ThreadPoolManager;

public class MainActivity extends AppCompatActivity implements StateCallBack {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding mainBinding;
    private CatchLogService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(LayoutInflater.from(this));
        setContentView(mainBinding.getRoot());

        Intent service = new Intent(this, CatchLogService.class);
        bindService(service, conn, BIND_AUTO_CREATE);

    }

    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CatchLogService.MyBinder binder = (CatchLogService.MyBinder) service;
            mService = binder.getService();
            mService.setStateCallback(MainActivity.this);
            LogState logState = readRunState();
            onStateChange(logState);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    public void onButtonClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_start) {
            CatchLogService.startAction(this, "start");
        } else if (id == R.id.btn_stop) {
            CatchLogService.startAction(this, "stop");
        } else if (id == R.id.btn_clean) {
            CatchLogService.startAction(this, "clean");
        } else if (id == R.id.btn_copy) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                showVolumeDialog();
            } else {
                Toast.makeText(this, "SDK version is too low", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onStateChange(LogState state) {
        Log.i(TAG, "onStateChange: " + state);
        switch (state) {
            case STATE_STOP:
                mainBinding.btnStart.setEnabled(true);
                mainBinding.btnStop.setEnabled(false);
                break;
            case STATE_RUNNING:
                mainBinding.btnStart.setEnabled(false);
                mainBinding.btnStop.setEnabled(true);
                break;
        }
    }

    private LogState readRunState() {
        return CatchLogService.readRunState(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void showVolumeDialog() {
        DialogHelper.showChoiceDialog(this, new DialogHelper.DialogCallBack() {
            @Override
            public void call(CharSequence path) {
                CatchLogService.startAction(MainActivity.this, "stop");
                ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.show();
                ThreadPoolManager.getInstance().submit(() -> {
                    boolean result = FileUtils.copyFolder(Environment.getExternalStorageDirectory() + "/xbh_log", path + "/xbh_log");
                    mService.myHandler.post(() -> {
                        progressDialog.dismiss();
                        if (result) {
                            Toast.makeText(MainActivity.this, "log copy success", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "log copy fail", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }
        });
    }
}