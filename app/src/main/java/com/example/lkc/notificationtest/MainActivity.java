package com.example.lkc.notificationtest;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final int TAKE_PHOTO = 1;
    private ImageView imageView;
    private Button takebutton, choosebutton;
    private Uri imageUri;
    Button button;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = (Button) findViewById(R.id.send_notice);
        takebutton = (Button) findViewById(R.id.take_photo);
        choosebutton = (Button) findViewById(R.id.take_picture);
        imageView = (ImageView) findViewById(R.id.set_img);
        choosebutton.setOnClickListener(this);
        button.setOnClickListener(this);
        takebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //创建File对象，用于存储拍照后的图片
                File outputImage = new File(getExternalCacheDir(), "output_image.jpg");
                try {
                    //如果缓存文件中存在就删除，不存在就创建。用于二次访问。
                    if (outputImage.exists()) {
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (Build.VERSION.SDK_INT >= 24) {
                    //安卓7.0访问SD目录要授权，使用内容提供器
                    imageUri = FileProvider.getUriForFile(MainActivity.this,
                            "com.example.lkc.NotificationTest.fileprovider", outputImage);
                } else {
                    imageUri = Uri.fromFile(outputImage);
                }
                //启动相机程序
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                //将拍摄的照片存储至指定路径
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, TAKE_PHOTO);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send_notice:
                Intent intent = new Intent(this, NotificationActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
                //NotificationManager创建通知管理通知通过getSystemService获取
                NotificationManager manager = (NotificationManager)
                        getSystemService(NOTIFICATION_SERVICE);
                //构建通知对象
                Notification notification = new NotificationCompat.Builder(this)
                        .setContentTitle("标题")//设置通知的标题
                        .setContentText("今天晚上白宫汇搞起啊！")//内容
                        .setWhen(System.currentTimeMillis())//设置时间 系统时间
                        .setSmallIcon(R.mipmap.ic_launcher)//设置小图标
                        .setLargeIcon(BitmapFactory.decodeResource
                                (getResources(), R.mipmap.ic_launcher))//设置大图标
                        .setAutoCancel(true)//设置点击之后自动cancel通知
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setVibrate(new long[]{0, 100, 100, 100})
//                        .setDefaults(NotificationCompat.DEFAULT_ALL)//设置通知为默认效果
                        .setVisibility(Notification.VISIBILITY_PRIVATE)
                        .setPriority(NotificationCompat.PRIORITY_MAX)//设置通知的重要程度
                        .setContentIntent(pendingIntent)//设置点击的意图
                        .build();//完成
                manager.notify(1, notification);//上传给管理
                break;
            case R.id.take_picture:
                //获取相册权限
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission
                        .WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest
                            .permission.WRITE_EXTERNAL_STORAGE}, 1);
                } else {
                    openAlbum();
                }
                break;
            default:
                break;

        }
    }
    //打开相册
    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, 2);
    }
    //判断是否已经获取了权限
    @Override
    public void onRequestPermissionsResult(int requestCode
            , String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbum();
                } else {
                    Toast.makeText(this, "You denied the perission", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        //通过Bitmap获取
                        Bitmap bitmap = BitmapFactory
                                .decodeStream(getContentResolver().openInputStream(imageUri));
                        imageView.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case 2:
                if (resultCode == RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= 19) {
                        handleImageOnKiteKat(data);
                    } else {
                        handleImageBeForeKitKat(data);
                    }
                }
                break;
            default:
                break;
        }
    }

    @TargetApi(19)
    private void handleImageBeForeKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUrl = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads")
                        , Long.valueOf(docId));
                imagePath = getImagePath(contentUrl, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            imagePath = uri.getPath();
        }
        displayImage(imagePath);
    }

    private void handleImageOnKiteKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);
    }

    private String getImagePath(Uri externalContentUri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(externalContentUri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    private void displayImage(String imagePath) {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory
                    .decodeFile(imagePath);
            imageView.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this, "failed to get image", Toast.LENGTH_SHORT).show();
        }
    }
}
