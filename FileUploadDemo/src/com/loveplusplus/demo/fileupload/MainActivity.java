package com.loveplusplus.demo.fileupload;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;

public class MainActivity extends Activity {

	
	private String mCurrentPhotoPath;//图片路径
	private String mCurrentVideoPath;//视频路径
	private static final int REQUEST_TAKE_PHOTO = 0;
	private static final int REQUEST_TAKE_VIDEO = 1;
	private static final String TAG = "MainActivity";
	public ProgressDialog progressDialog;
	private ImageView mImageView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mImageView = (ImageView) findViewById(R.id.imageView1);
	}

	

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {

		if (requestCode == REQUEST_TAKE_PHOTO) {
			if (resultCode == Activity.RESULT_OK) {

				//添加到图库,这样可以在手机的图库程序中看到程序拍摄的照片
				PictureUtil.galleryAddPic(this,mCurrentPhotoPath);

				mImageView.setImageBitmap(PictureUtil
						.getSmallBitmap(mCurrentPhotoPath));

			} else {
				// 取消照相后，删除已经创建的临时文件。
				PictureUtil.deleteTempFile(mCurrentPhotoPath);
			}
		}else if(requestCode == REQUEST_TAKE_VIDEO){
			if(resultCode == Activity.RESULT_OK){
				
				Uri uri=intent.getData();
				mCurrentVideoPath=getRealPathFromURI(uri);
				
				Log.d(TAG, mCurrentVideoPath);
			}
		}

	}

	public String getRealPathFromURI(Uri contentUri) {
	    String[] proj = { MediaStore.Images.Media.DATA };
	    Cursor cursor = managedQuery(contentUri, proj, null, null, null);
	    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	    cursor.moveToFirst();
	    return cursor.getString(column_index);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_photo:
			takePhoto();
			return true;
		case R.id.menu_video:
			takeVideo();
			return true;
		case R.id.menu_upload:
			upload();
			return true;
		case R.id.menu_upload_video:
			uploadVideo();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}

	}

	private void uploadVideo() {
		if (mCurrentVideoPath != null) {
			FileUploadTask task = new FileUploadTask();
			task.execute(mCurrentVideoPath,"1");
		} else {
			Toast.makeText(this, "请先点击录像按钮拍摄视频", Toast.LENGTH_SHORT).show();
		}
	}



	private void takeVideo() {
		    Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		    startActivityForResult(takeVideoIntent, REQUEST_TAKE_VIDEO);
	}



	/**
	 * 拍照
	 */
	private void takePhoto() {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		try {
			//指定存放拍摄照片的位置
			File f = createImageFile();
			takePictureIntent
					.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
			startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 把程序拍摄的照片放到 SD卡的 Pictures目录中 sheguantong 文件夹中
	 * 照片的命名规则为：sheqing_20130125_173729.jpg
	 * @return
	 * @throws IOException
	 */
	@SuppressLint("SimpleDateFormat")
	private File createImageFile() throws IOException {
		
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String timeStamp = format.format(new Date());
		String imageFileName = "sheqing_" + timeStamp + ".jpg";
		
		File image = new File(PictureUtil.getAlbumDir(), imageFileName);
		mCurrentPhotoPath = image.getAbsolutePath();
		return image;
	}

	/**
	 * 上传到服务器
	 */
	private void upload() {

		if (mCurrentPhotoPath != null) {
			FileUploadTask task = new FileUploadTask();
			task.execute(mCurrentPhotoPath);
		} else {
			Toast.makeText(this, "请先点击拍照按钮拍摄照片", Toast.LENGTH_SHORT).show();
		}
	}

	private class FileUploadTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			String filePath = params[0];
			FileBean bean = new FileBean();
			
			String type=params[1];//上传图片还是视频，图片需要压缩
			
			String content;
			if(null!=type&&"1".equals(type)){
				content=VideoUtil.videoToString(filePath);
			}else{
				content=PictureUtil.bitmapToString(filePath);
			}
			bean.setFileContent(content);
			
			File f=new File(filePath);
			String fileName=f.getName();
			bean.setFileName(fileName);
			
			
			Gson gson = new Gson();
			String json = gson.toJson(bean);

			MessageHelper helper = new MessageHelper(MainActivity.this);
		//	return helper.sendMsg(json);//使用webservice
			return helper.sendPost(json);//使用http post
		}

		@Override
		protected void onPreExecute() {
			progressDialog = new ProgressDialog(MainActivity.this);
			progressDialog.setMessage("正在提交,请稍候...");
			progressDialog.show();
		}

		@Override
		protected void onPostExecute(String result) {
			progressDialog.dismiss();
			Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
		}

	}

}
