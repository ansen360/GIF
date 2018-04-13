package com.ansen.gif;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ansen on 2017/5/12 18:58.
 *
 * @E-mail: ansen360@126.com
 * @Blog: "http://blog.csdn.net/qq_25804863"
 * @Github: "https://github.com/ansen360"
 * @PROJECT_NAME: GIF
 * @PACKAGE_NAME: com.ansen.gif
 * @Description: TODO
 */
public class BitmapRetriever {

    private static final int μs = 1000 * 1000;  // 1 μs
    private static final Boolean DEBUG = false;

    private List<Bitmap> bitmaps = new ArrayList<>();
    private int width = 0;
    private int height = 0;
    private int start = 0;
    private int end = 0;
    private int fps = 5;    // 帧数


    public List<Bitmap> generateBitmaps(String path) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(path);
        double interval = μs / fps;
        long duration = (Long.decode(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000);
        if (end > 0) {
            duration = end * μs;
        }
        for (long i = start * μs; i < duration; i += interval) {
            /** 在给定的时间位置上获取一帧图片
             * (视频质量不高或其他原因 可能出现总是获取为同一帧画面,
             * 也就是 假设获取50帧画面,实际只有10帧有效,其余有重复画面)
             */
            Bitmap frame = mmr.getFrameAtTime((long) i, MediaMetadataRetriever.OPTION_CLOSEST);
            if (frame != null) {
                try {
                    bitmaps.add(scale(frame));
                    debugSaveBitmap(frame, "" + i);
                } catch (OutOfMemoryError oom) {
                    oom.printStackTrace();
                    break;
                }
            }
        }
        return bitmaps;
    }

    // 设置分辨率大小
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    // 截取视频的起始时间(单位 s)
    public void setDuration(int begin, int end) {
        this.start = begin;
        this.end = end;
    }

    // 设置码流率
    public void setFPS(int fps) {
        this.fps = fps;
    }

    private Bitmap scale(Bitmap bitmap) {
        return Bitmap.createScaledBitmap(bitmap,
                width > 0 ? width : bitmap.getWidth(),
                height > 0 ? height : bitmap.getHeight(),
                true);
    }

    public void debugSaveBitmap(Bitmap bm, String picName) {
        if (DEBUG) {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/Screenshots/";
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
            File f = new File(file.getAbsolutePath(), "DEBUG__" + picName + ".png");
            if (f.exists()) {
                f.delete();
            }
            try {
                FileOutputStream out = new FileOutputStream(f);
                bm.compress(Bitmap.CompressFormat.PNG, 90, out);
                out.flush();
                out.close();
//                sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE", Uri.fromFile(f)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
