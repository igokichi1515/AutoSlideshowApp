package jp.techacademy.naoki.jinguji.autoslideshowapp;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final String TAG = "ImageUtils";

    Button prev, next, autoStart, autoStop;
    Cursor cursor;
    MyTimerTask timerTask = null;
    Timer mTimer = null;
    Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //IDの取得
        prev = (Button) findViewById(R.id.prev);
        next = (Button) findViewById(R.id.next);
        autoStart = (Button) findViewById(R.id.autoStart);
        autoStop = (Button) findViewById(R.id.autoStop);

        prev.setOnClickListener(this);
        next.setOnClickListener(this);
        autoStart.setOnClickListener(this);
        autoStop.setOnClickListener(this);

        // Android 6.0以降の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている
                getContentsInfo();
            } else {
                // 許可されていないので許可ダイアログを表示する
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
            }
            // Android 5系以下の場合
        } else {
            getContentsInfo();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

        for(int i = 0; i < permissions.length; i++) {
            switch (requestCode) {
                case PERMISSIONS_REQUEST_CODE:
                    if (grantResults.length > 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        getContentsInfo();
                    } else {
                        Toast.makeText(MainActivity.this, "権限をONにしないとアプリが使えません", Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void getContentsInfo() {

        // 画像の情報を取得する
        ContentResolver resolver = getContentResolver();
        cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
                null, // 項目(null = 全項目)
                null, // フィルタ条件(null = フィルタなし)
                null, // フィルタ用パラメータ
                null // ソート (null ソートなし)
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {

                Bitmap bmp = null;
                Bitmap dst = null;

                int fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                Long id = cursor.getLong(fieldIndex);
                Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                try {
                    bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    dst = MainActivity.resizeBitmapToDisplaySize(this, bmp);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ImageView imageView = (ImageView) findViewById(R.id.slideView);
                imageView.setImageBitmap(dst);

            }
        }
    }
    @Override
    public void onClick(View v) {

        if (v != null) {
            if (cursor != null) {
                switch (v.getId()) {
                    //自動再生スタートボタンが押されたとき
                    case R.id.autoStart:

                        autoStart.setVisibility(View.GONE);
                        autoStop.setVisibility(View.VISIBLE);
                        prev.setEnabled(false);
                        next.setEnabled(false);

                        if (mTimer == null) {
                            //タイマーの初期化処理
                            timerTask = new MyTimerTask();
                            mTimer = new Timer(true);
                            mTimer.schedule(timerTask, 0, 2000);
                        }
                        break;

                    //自動再生ストップボタンが押されたとき
                    case R.id.autoStop:

                        autoStop.setVisibility(View.GONE);
                        autoStart.setVisibility(View.VISIBLE);
                        prev.setEnabled(true);
                        next.setEnabled(true);

                        if (mTimer != null) {
                            mTimer.cancel();
                            mTimer = null;
                        }
                        break;

                    case R.id.next:

                        if (cursor.isLast()) {
                            cursor.moveToFirst();
                            ImageSet();
                        } else {
                            cursor.moveToNext();
                            ImageSet();
                        }
                        break;

                    case R.id.prev:

                        if (cursor.isFirst()) {
                            cursor.moveToLast();
                            ImageSet();
                        } else {
                            cursor.moveToPrevious();
                            ImageSet();
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void ImageSet(){

        Bitmap bmp;
        Bitmap dst = null;

        int fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
        Long id = cursor.getLong(fieldIndex);
        Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

        try {
            bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            dst = MainActivity.resizeBitmapToDisplaySize(this, bmp);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ImageView imageView = (ImageView) findViewById(R.id.slideView);
        imageView.setImageBitmap(dst);
    }

    public static Bitmap resizeBitmapToDisplaySize(Activity activity, Bitmap src){

        int srcWidth = src.getWidth(); // 元画像のwidth
        int srcHeight = src.getHeight(); // 元画像のheight

        // 画面サイズを取得する
        Matrix matrix = new Matrix();
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float screenWidth = (float) metrics.widthPixels;
        float screenHeight = (float) metrics.heightPixels;

        float widthScale = screenWidth / srcWidth;
        float heightScale = screenHeight / srcHeight;

        if (widthScale > heightScale) {
            matrix.postScale(heightScale, heightScale);
        } else {
            matrix.postScale(widthScale, widthScale);
        }
        // リサイズ
        Bitmap dst = Bitmap.createBitmap(src, 0, 0, srcWidth, srcHeight, matrix, true);
        src = null;
        return dst;
    }

    class MyTimerTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {

                    if (cursor.isLast()) {
                        cursor.moveToFirst();
                        ImageSet();
                    }else{
                        cursor.moveToNext();
                        ImageSet();
                    }
                }
            });
        }
    }
}