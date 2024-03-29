package com.xbh.logcatch.util;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.widget.Toast;

import com.xbh.logcatch.R;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * @Author: Huan.Wang
 * @Email: huan.wang@lango-tech.com
 * @Date: 2023/10/10 21:09
 * @Description:
 */
public class DialogHelper {

    public interface DialogCallBack {
        void call(CharSequence str);
    }
    public static void showChoiceDialog(Context context, DialogCallBack callBack) {
        StorageManager storageManager = getSystemService(context, StorageManager.class);
        List<StorageVolume> storageVolumes = storageManager.getStorageVolumes();
        CharSequence[] items = new CharSequence[storageVolumes.size()];
        for (int i = 0; i < items.length; i++) {
            File directory = storageVolumes.get(i).getDirectory();
            String path = null;
            if (directory != null) {
                path = directory.getPath();
                items[i] = path;
            }
        }
        CharSequence[] newItems = Arrays.stream(items).filter(new Predicate<CharSequence>() {
            @Override
            public boolean test(CharSequence charSequence) {
                return !charSequence.equals("/storage/emulated/0");
            }
        }).toArray(CharSequence[]::new);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.select_path);
        if (newItems.length != 0) {
            builder.setSingleChoiceItems(newItems, 0, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (callBack != null) {
                        callBack.call(newItems[which]);
                    }

                    dialog.dismiss();
                }
            });
        }

        builder.create().show();
    }
}
