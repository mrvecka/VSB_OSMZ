package com.kru13.httpserver;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

public class HttpServerActivity extends AppCompatActivity implements OnClickListener{

	private SocketServer s;
	private ArrayList<String> Messages;
	private ArrayAdapter<String> arrayAdapter;
	private Camera mCamera;
	private CameraPreview mPreview;
	private byte[] picture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_http_server);

		StrictMode.ThreadPolicy policy = new
				StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		setContentView(R.layout.activity_http_server);

        Button btn1 = (Button)findViewById(R.id.button1);
        Button btn2 = (Button)findViewById(R.id.button2);

		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {

			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

			} else {
				// No explanation needed; request the permission
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						1);

			}
		}

		if (CameraPreview.checkCameraHardware(getApplicationContext()))
		{
			mCamera = CameraPreview.getCameraInstance();
			mCamera.setPreviewCallback(mPprev);
			mPreview = new CameraPreview(this, mCamera,mPicture);
			FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
			preview.addView(mPreview);
		}

        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        this.Messages = new ArrayList<String>();
		arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_gallery_item, this.Messages);

		ListView lv = (ListView)findViewById(R.id.listView);
		lv.setAdapter(arrayAdapter);

		mCamera.startPreview();
    }

    @Override
	public void onPause()
	{
		super.onPause();
		mCamera.stopPreview();
		if (mCamera != null) {
			mCamera.release();
			mCamera = null;
		}
	}

	public byte[] GetPicture()
	{
		return picture;
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.http_server, menu);
        return true;
    }


	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if (v.getId() == R.id.button1) {

			s = new SocketServer(mHandler,this);
			s.start();
		}
		if (v.getId() == R.id.button2) {
			s.close();
			try {
				s.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private final Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			Bundle bndl = msg.getData();
			ResponseMessage m = (ResponseMessage) bndl.getSerializable("REQUEST");
			Messages.add("Data send to: " + m.Host);
			Messages.add("Requested file: "+m.FileName);
			Messages.add("Data size: "+m.ResponseSize);
			arrayAdapter.notifyDataSetChanged();
		}
	};

	private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {

			picture = data;
			camera.startPreview();
		}
	};
	private Camera.PreviewCallback mPprev = new Camera.PreviewCallback() {

		@Override
		public void onPreviewFrame(byte[] bytes, Camera camera) {
			picture = convertoToJpeg(bytes, camera);
		}
	};

	public void takePicture(){
		mCamera.startPreview();
		mCamera.takePicture(null, null, mPicture);

		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				takePicture();
			}
		}, 500);
	}

	public byte[] convertoToJpeg(byte[] data, Camera camera) {

		YuvImage image = new YuvImage(data, ImageFormat.NV21,
				camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height, null);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int quality = 20; //set quality
		image.compressToJpeg(new Rect(0, 0, camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height), quality, baos);//this line decreases the image quality

		return baos.toByteArray();
	}





    
}
