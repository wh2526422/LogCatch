package com.xbh.logcatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * @Author: Huan.Wang
 * @Email: huan.wang@lango-tech.cn
 * @Date: 2023/8/31 17:32
 * @Description:
 */
public class CatchLogBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, CatchLogService.class);
        context.startService(service);
    }
}
