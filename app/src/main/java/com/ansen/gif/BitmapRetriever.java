package com.ansen.gif;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;

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

    private static final int US_OF_S = 1000 * 1000;

    private List<Bitmap> bitmaps = new ArrayList<>();
    private int width = 0;
    private int height = 0;
    private int begin = 0;
    private int end = 0;
    private int fps = 5;


    public List<Bitmap> generateBitmaps(String path) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(path);
        double inc = US_OF_S / fps;
        for (double i = begin * US_OF_S; i < end * US_OF_S; i += inc) {
            // 在给定的时间位置上获取一帧图片
            Bitmap frame = mmr.getFrameAtTime((long) i, MediaMetadataRetriever.OPTION_CLOSEST);
            if (frame != null) {
                bitmaps.add(scale(frame));
            }
        }
        return bitmaps;
    }

    // 设置分辨率大小
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setScope(int begin, int end) {
        this.begin = begin;
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
}
