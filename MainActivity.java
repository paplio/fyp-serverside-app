package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

public class MainActivity extends AppCompatActivity {
    String selectedFilePath;
    String actions[] = {"extract", "compress", "subtitles", "error"};
    String choice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button extractor = findViewById(R.id.extractaudio);

        Button compressor = findViewById(R.id.compressvideo);

        extractor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    choice = actions[0];
                    extractAudio();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    choice = actions[3];
                }
            }
        });

        compressor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    choice = actions[1];
                    compressVideo();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    choice = actions[3];
                }
            }
        });

    }



    void extractAudio() throws FileNotFoundException {

        String postUrl = "http://3.22.70.87:8080/fromVideoExtractAudio";

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        final String contentType = "video/mp4";
        final AssetFileDescriptor fd;
        fd = contentResolver.openAssetFileDescriptor(Uri.fromFile(new File(selectedFilePath)), "r");

        System.out.println(selectedFilePath);

        RequestBody videoFile = new RequestBody() {
            @Override
            public long contentLength() {
                return fd.getDeclaredLength();
            }

            @Override
            public MediaType contentType() {
                return MediaType.parse(contentType);
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                try (InputStream is = fd.createInputStream()) {
                    sink.writeAll(Okio.buffer(Okio.source(is)));
                }
            }
        };
        RequestBody postBodyImage = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("uploadedVideo", selectedFilePath.substring((selectedFilePath.lastIndexOf('/') + 1), selectedFilePath.length()), videoFile)
                .build();

        TextView responseText = findViewById(R.id.responseText);
        responseText.setText("Please wait ...");

        postRequest(postUrl, postBodyImage);
    }

    void compressVideo() throws FileNotFoundException {

        String postUrl = "http://3.22.70.87:8080/compressVideo";

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        final String contentType = "video/mp4";
        final AssetFileDescriptor fd;
        fd = contentResolver.openAssetFileDescriptor(Uri.fromFile(new File(selectedFilePath)), "r");

        System.out.println(selectedFilePath);

        RequestBody videoFile = new RequestBody() {
            @Override
            public long contentLength() {
                return fd.getDeclaredLength();
            }

            @Override
            public MediaType contentType() {
                return MediaType.parse(contentType);
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                try (InputStream is = fd.createInputStream()) {
                    sink.writeAll(Okio.buffer(Okio.source(is)));
                }
            }
        };
        RequestBody postBodyImage = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("uploadedVideo", selectedFilePath.substring((selectedFilePath.lastIndexOf('/') + 1), selectedFilePath.length()), videoFile)
                .build();

        TextView responseText = findViewById(R.id.responseText);
        responseText.setText("Please wait ...");

        postRequest(postUrl, postBodyImage);
    }


    void postRequest(String postUrl, RequestBody postBody) {

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(postUrl)
                .post(postBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Cancel the post on failure.
                call.cancel();

                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView responseText = findViewById(R.id.responseText);
                        //responseText.setText("Failed to Connect to Server");
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if(choice.equals("extract")){

                            TextView responseText = findViewById(R.id.responseText);
                            try {
                                String app_dir = "accessibility";
                                File file = new File(Environment.getExternalStorageDirectory() + "/"+app_dir, "final_output.mp3");
                                if (!file.exists()) {
                                    file.mkdirs();
                                    System.out.println("we made accessibility!"+file.getAbsolutePath());
                                }

                                BufferedSink data = Okio.buffer(Okio.sink(file));
                                data.writeAll(response.body().source());

                                System.out.println("Wrote file!");
                                data.close();

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            //responseText.setText(response.body().string());
                            responseText.setText("Extracted audio!");
                            }

                        else if(choice.equals("compress")){
                            System.out.println("\nCOMPRESSING\n");
                            TextView responseText = findViewById(R.id.responseText);
                            try {
                                String app_dir = "accessibility";
                                File file = new File(Environment.getExternalStorageDirectory() + "/"+app_dir, "compressed_video.mp4");
                                if (!file.getParentFile().exists()) {
                                    file.getParentFile().mkdirs();
                                    System.out.println("we made accessibility!"+file.getAbsolutePath());
                                }

                                //RUN JUST ONCE
                                FFmpeg ffmpeg2 = FFmpeg.getInstance(getApplicationContext());
                                try {
                                    ffmpeg2.loadBinary(new LoadBinaryResponseHandler() {

                                        @Override
                                        public void onStart() {}

                                        @Override
                                        public void onFailure() {}

                                        @Override
                                        public void onSuccess() {}

                                        @Override
                                        public void onFinish() {}
                                    });
                                } catch (FFmpegNotSupportedException e) {
                                    // Handle if FFmpeg is not supported by device
                                }

                                //
                                //passing through ffmpeg android lib one time to ensure encoding is correct
                                FFmpeg ffmpeg = FFmpeg.getInstance(getApplicationContext());
                                try{
                                    String[] cmd = {"-i",file.getPath(),file.getPath()};
                                    ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {
                                        @Override
                                        public void onStart() {
                                            System.out.println("\n---------COMMAND\nSTART\n");
                                        }
                                        @Override
                                        public void onProgress(String message) {
                                            System.out.println("Android pass: Progress"+message);
                                        }
                                        @Override
                                        public void onFailure(String message) {
                                            System.out.println("\n---------COMMAND\nFAILURE\n" + message);
                                        }
                                        @Override
                                        public void onSuccess(String message) {
                                            System.out.println("\n---------COMMAND\nSUCCESS\n");
                                        }
                                        @Override
                                        public void onFinish() {
                                            System.out.println("\n---------COMMAND\nFINISH\n");
                                            //video.setVideoPath(output);
                                            //video.start();
                                        }
                                    });
                                } catch (FFmpegCommandAlreadyRunningException e) {
                                    // Handle if FFmpeg is already running
                                    e.printStackTrace();
                                }


                                BufferedSink data = Okio.buffer(Okio.sink(file));
                                data.writeAll(response.body().source());

                                System.out.println("Compressed video!");
                                responseText.setText("Compressed video!");
                                data.close();

                            } catch (IOException e) {
                                e.printStackTrace();
                            }


                        }
                    }
                });
            }
        });
    }

    public void selectImage(View v) {
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int reqCode, int resCode, Intent data) {
        super.onActivityResult(reqCode, resCode, data);
        if (resCode == RESULT_OK && data != null) {
            Uri uri = data.getData();

            selectedFilePath = getPath(getApplicationContext(), uri);
            EditText imgPath = findViewById(R.id.imgPath);
            imgPath.setText(selectedFilePath);
            Toast.makeText(getApplicationContext(), selectedFilePath, Toast.LENGTH_LONG).show();
        }
    }

    // Implementation of the getPath() method and all its requirements is taken from the StackOverflow Paul Burke's answer: https://stackoverflow.com/a/20559175/5426539
    public static String getPath(final Context context, final Uri uri) {

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

                // TODO handle non-primary volumes
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
                final String[] selectionArgs = new String[]{
                        split[1]
                };

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

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }


}