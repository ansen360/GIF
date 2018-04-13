- **实现原理:**

**1.读取视频文件,将视频文件解析为Bitmap序列**

**2.将Bitmap 序列编码生成 GIF 文件**

- [**代码下载**](https://github.com/ansen360/GIF)

- **代码流程**

打开Android系统文件管理:
```
    Intent intent = new Intent();
    intent.setType("video/*");
    intent.setAction(Intent.ACTION_GET_CONTENT);
    startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_CODE);
```
Activity回调中获取选择的文件URI
```
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Uri videoUri = data.getData();
                mFilePath = getRealFilePath(this, videoUri);
            }
        }
    }
```
通过Uri获取文件的真实路径
```
    /**
     * Android4.4+,通过Uri获取文件绝对路径
     */
    public String getRealFilePath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public String getDataColumn(Context context, Uri uri, String selection,
                                String[] selectionArgs) {

        Cursor cursor = null;
        String column = MediaStore.Images.ImageColumns.DATA;
        String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
```
将视频文件解析为Bitmap序列(也就是Bitmap的集合),原理是通过MediaMetadataRetriever提供的方法,不断的在给定的时间位置上获取一帧图片,然后保存到集合中.(该方法常用来获取视频文件的缩略图)
```
    BitmapRetriever extractor = new BitmapRetriever();
    extractor.setFPS(10);
    // 截取视频的起始时间
    extractor.setDuration(0, 5);
    extractor.setSize(720, 1280);
    List<Bitmap> bitmaps = extractor.generateBitmaps(mFilePath);
```
通过MediaMetadataRetriever解析单位时间上的一帧画面
```
    // com.ansen.gif.BitmapRetriever
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
```

将Bitmap序列中数据按照 GIF 的文件格式编码生成GIF图片
```
    String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
            + String.valueOf(System.currentTimeMillis()) + ".gif";
    GIFEncoder encoder = new GIFEncoder();
    encoder.init(bitmaps.get(0));
    encoder.start(filePath);
    for (int i = 1; i < bitmaps.size(); i++) {
        encoder.addFrame(bitmaps.get(i));
    }
    encoder.finish();
```


**仿Iphone拍摄动态GIF图的实现思路同此,1秒内连拍多张图片(Bitmap),将图片的集合按如上方式制作GIF图片**