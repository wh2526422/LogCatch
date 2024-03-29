package com.xbh.logcatch.util;

import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author: Huan.Wang
 * @Email: huan.wang@lango-tech.com
 * @Date: 2023/10/18 13:53
 * @Description:
 */
public class ThreadPoolManager {

    private static final String TAG = "ThreadPoolManager";
    private final ExecutorService mExecutors;
    private static ThreadPoolManager INSTANCE;

    private ThreadPoolManager() {
        Log.i(TAG, "ThreadPoolManager: init");
        mExecutors = Executors.newCachedThreadPool();
    }

    public static ThreadPoolManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ThreadPoolManager();
        }
        return INSTANCE;
    }

    public void submit(Runnable runnable) {
        Log.i(TAG, "submit: " + runnable.toString());
        if (mExecutors == null) {
            Log.e(TAG, "submit: Thread pool not init");
            return;
        }
        mExecutors.submit(runnable);
    }

    public void shutdown() {
        Log.i(TAG, "shutdown: ");
        if (mExecutors == null) {
            Log.e(TAG, "submit: Thread pool not init");
            return;
        }
        mExecutors.shutdown();
    }
}
