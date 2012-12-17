/**
 * (c) 2011-2012 Sense Tecnic Systems Inc.   All rights reserved.
 */

package com.sensetecnic.container;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.webkit.WebView;

public class HtmlCallbackActivity extends Activity{ 

	public static final String URI_PREFIX = "http://www.sinfulseven.net/coffeeshop/";
	public static final String URI_SEPARATOR = "?";
	public static final String URI_APPLICATION_SEPARATOR = "&";
	public static final String ABORT_CODE = "!ABORT";
	public static final String[] MODES = { "scan", "camera", "gallery", "uploadfile", "nfc", "accel", "quit", "app", "launch", "gencode", "browser" };

	public static final int idLength = 3;
	
	// request codes
	private static final int SCAN_RQ_CODE = 0;
	private static final int CAPTURE_IMAGE_RQ_CODE = 1;
	private static final int CHOOSE_IMAGE_RQ_CODE = 2;
	private static final int FILE_UPLOAD_CODE = 3;
	private static final int NFC_TRANSFER_CODE = 4;

	// progress dialog
	private ProgressDialog pd;

	// request params - global
	private String callbackUrl;

	// request params - camera
	String name, tag, type, shouldPersist = "0", uploadPhotoUrl, field;
	private File photo;
	private long captureTime = 0;
	
	// request params - File uploading
	private File uploadFile;
	
	// request params - nfc
	private String message;
	
	// request params - accelerometer rate
	private double accel_rate;
	private String player_number;
	
	//private boolean inProgress;
	
	//initializes webView
	//private WebView webView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.html_callback);

		System.out.println("Created");

		MultipartEntity reqEntity = new MultipartEntity();  
		/* null out previous values, if any */
		photo = null;
		callbackUrl = null;
		name = null;
		tag = null;
		type = null;
		uploadFile = null;
		message = null;
		accel_rate = 0;
		player_number = "0";
		

		
		Uri data = getIntent().getData(); 
		if (data != null) { 
			String requestingUri = data.toString();
			if (!requestingUri.startsWith(URI_PREFIX))
				return;

			String request = requestingUri.substring(URI_PREFIX.length());
			int separatorIndex = request.indexOf(URI_SEPARATOR);
			int requestIndex = request.indexOf(URI_APPLICATION_SEPARATOR);
			

			String mode = separatorIndex == -1 ? request : request.substring(0, separatorIndex);
			System.out.println("Mode = " + mode);
			if (!isValidMode(mode))
				return;

			try {
				String strEntity = request.substring(separatorIndex+1);
				final StringEntity entity = new StringEntity(strEntity, null);
				entity.setContentType(URLEncodedUtils.CONTENT_TYPE);
				List<NameValuePair> parameters = URLEncodedUtils.parse(entity);
				handleRequest(mode, parameters);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.println("Intent data is null");
		}
		System.out.println(data.toString());
		
	}

	/**
	 * Given a parsed-out mode and a list of request parameters, handle the request.
	 * @param mode
	 * @param parameters
	 */
	public void handleRequest(String mode, List<NameValuePair> parameters) {

		System.out.println("Handle Request Functioned: " + mode);
		//if (!inProgress)
		//{
		if (mode.equals("scan")) {
			// leech parameters out of request
			for (NameValuePair pair : parameters) {
				if (pair.getName().equals("ret")) {
					callbackUrl = pair.getValue();
					}
				else if (pair.getName().equals("player")) 
					player_number = pair.getValue();				
				// ignore other parameters as irrelevant for this mode
			}	

			// ready to scan
			Intent intent = new Intent("com.google.zxing.client.android.SCAN");
			intent.putExtra("com.google.zxing.client.android.SCAN.SCAN_MODE", "QR_CODE_MODE");
			System.out.println("Ready to scan qr code");
			startActivityForResult(intent, SCAN_RQ_CODE);
			System.out.println("Scanning this");

		} else if (mode.equals("camera") || mode.equals("gallery")) {
			// leech parameters out of request
			for (NameValuePair pair : parameters) {
				if (pair.getName().equals("ret")) 
					callbackUrl = pair.getValue();
				else if (pair.getName().equals("name")) 
					name = pair.getValue();
				else if (pair.getName().equals("tag"))
					tag = pair.getValue();
				else if (pair.getName().equals("type"))
					type = pair.getValue();
				else if (pair.getName().equals("persist"))
					shouldPersist = pair.getValue();
				else if (pair.getName().equals("upload_url"))
					uploadPhotoUrl = pair.getValue();					
				else if (pair.getName().equals("field"))
					field = pair.getValue();
				else if (pair.getName().equals("player"))
					player_number = pair.getValue();
				// ignore other parameters as irrelevant for this mode
			}		

			// ready to take photo or choose it from the gallery
			if (mode.equals("camera")) {
				captureTime = System.currentTimeMillis();
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				
				System.out.println("Starting the Photo Taking");
				
				photo = getOutputMediaFile(MEDIA_TYPE_IMAGE);
				Uri cameraFileUri = Uri.fromFile(photo); // create a file to save the image
				intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraFileUri); // set the image file name
				startActivityForResult(intent, CAPTURE_IMAGE_RQ_CODE);
				
				System.out.println("Concluding the System Imaging");
				
			}
			 else {
				// we can't know about the photo object yet, because we haven't chosen anything
				// defer assignment until later
				// can be used to retreive a gallery image
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("image/*");
				startActivityForResult(intent, CHOOSE_IMAGE_RQ_CODE);
			}
		} else if (mode.equals("uploadfile"))
		{
			System.out.println("Starting the file browser");
			
			for (NameValuePair pair : parameters) {
				if (pair.getName().equals("ret")) 
					callbackUrl = pair.getValue();
				else if (pair.getName().equals("name")) 
					name = pair.getValue();
				else if (pair.getName().equals("tag"))
					tag = pair.getValue();
				else if (pair.getName().equals("type"))
					type = pair.getValue();
				else if (pair.getName().equals("persist"))
					shouldPersist = pair.getValue();
				else if (pair.getName().equals("upload_url"))
					uploadPhotoUrl = pair.getValue();
				else if (pair.getName().equals("field"))
					field = pair.getValue();				
				else if (pair.getName().equals("player"))
						player_number = pair.getValue();
				// ignore other parameters as irrelevant for this mode
			}
			// ready to upload
			Intent intent = new Intent("com.nexes.manager.LAUNCH");
			startActivityForResult(intent, FILE_UPLOAD_CODE);
			
			
		}
		else if (mode.equals("nfc")) {
			for (NameValuePair pair : parameters) {
				if (pair.getName().equals("message"))
				{
					message = pair.getValue();
				}
				else if (pair.getName().equals("player"))
					player_number = pair.getValue();
						
			}
			Intent intent = new Intent(HtmlCallbackActivity.this, NFCOperation.class);
			intent.putExtra("ndefmessage", message);
			startActivityForResult(intent, NFC_TRANSFER_CODE);
		}
		
		else if (mode.equals("accel")) {
			for (NameValuePair pair : parameters) {
				if (pair.getName().equals("frequency"))
				{
					String accel_temp = pair.getValue();
					accel_rate = Double.parseDouble(accel_temp);
				}
				else if (pair.getName().equals("player"))
					player_number = pair.getValue();
			}
			
			Intent accel_callbackactivity = getIntent();
			accel_callbackactivity.putExtra("accel_rate", accel_rate);
			accel_callbackactivity.putExtra("method", "accel");
			accel_callbackactivity.putExtra("playernumber", player_number);
			
			System.out.println("Set the intent call back actiity values");
			
			setResult(RESULT_OK, accel_callbackactivity);
			System.out.println("SetResult OK");
			
			finish();
			
		}
		
		else if (mode.equals("quit")) {
			// Save a flag to preferences that the HtmlChallengeActivity should finish.
			SharedPreferences settings = getSharedPreferences("container_prefs", 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("quitChallenge", true);
			editor.commit();

			finish();
		} else if (mode.equals("app")) {
			String appName = null;
			String accessToken = null;
			for (NameValuePair pair : parameters) {
				if (pair.getName().equals("android_activity")) { 
					appName = pair.getValue();					
				}
				else if (pair.getName().equals("access_token")) {
					accessToken = pair.getValue();
				}
			}
			launchNativeApp(appName, accessToken);
			finish();			
		} else if (mode.equals("gencode")) {
			String code = null;
			String type = null;

			for (NameValuePair pair : parameters) {
				if (pair.getName().equals("type")) { 
					type = pair.getValue();					
				}
				else if (pair.getName().equals("code")) {
					code = pair.getValue();
				}
			}
			generateCode(type, code);

		} else if (mode.equals("browser")) {
			String url = null;
			for (NameValuePair pair : parameters) {
				if (pair.getName().equals("url")) { 
					url = pair.getValue();					
				}
			}
			launchBrowser(url);
		}
		//inProgress = false;
	}

	/**
	 * Launches a native app based on the given name.  
	 * @param appName
	 */
	private void launchNativeApp(String appName, String accessToken) {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.setComponent(new ComponentName(appName.substring(0, appName.lastIndexOf(".")), appName));
		intent.putExtra("accessToken", accessToken);
		startActivity(intent);
		finish();
	}
	
	private void launchBrowser(String url) {
		Uri uriUrl = Uri.parse(url);
	    Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);  
	    startActivity(launchBrowser); 
		finish();
	}


	private void generateCode(String type, String code) {
		if ("upc".equalsIgnoreCase(type))
			type = "UPC_A";
		else if ("qr".equalsIgnoreCase(type))
			type = "QR_CODE";

		System.out.println("generating code: type=["+type+"], code=["+code+"]");

		Intent intent = new Intent("com.google.zxing.client.android.ENCODE");

		intent.putExtra("ENCODE_FORMAT", type);
		intent.putExtra("ENCODE_DATA", code);
		intent.putExtra("ENCODE_TYPE", "TEXT_TYPE"); 
		//intent.putExtra("ENCODE_TYPE","CODE_128");

		startActivity(intent);
		finish();
	}

	/**
	 * Handle showing mobile challenge in HTML view.
	 * @param v
	 */
	public void showMobileChallenge(String challengeUrl) {
		Intent intent = new Intent(this, HtmlContainerActivity.class);
		intent.putExtra("url", challengeUrl);
		startActivity(intent);
		finish();
	}
	@Override
	public void onResume()
	{
		super.onResume();
	}




	/**
	 * Called when QR code scanning or photo taking is complete.
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		System.out.println("why are we not here yet");
		// QR code scan complete
		System.out.println(" Starting onActivity Result");
		System.out.println("Request Code : " + requestCode);
		System.out.println("Result Code: " + resultCode);
		
		if (requestCode == SCAN_RQ_CODE) {
			if (resultCode == RESULT_OK) {
				String contents = intent.getStringExtra("SCAN_RESULT");
				
				//System.out.println("Contents of Scan " + contents);
				// Scan was successful.  Replace {CODE} with scan results
				if (contents.startsWith("http://"))
					{
					String finalUrl = contents;
					System.out.println("Final URL: " + finalUrl);
					
					// Save callback to be refreshed in the caller version of this app
					SharedPreferences settings = getSharedPreferences("container_prefs", 0);
					SharedPreferences.Editor editor = settings.edit();
					editor.putString("callbackUrl", finalUrl);
	
					// Commit the edits!
					editor.commit(); 
					}
				else {
					Intent qr_callbackactivity = getIntent();
					qr_callbackactivity.putExtra("method", "qr");
					qr_callbackactivity.putExtra("qr_result", contents);
					qr_callbackactivity.putExtra("playernumber", player_number);
					setResult(RESULT_OK, qr_callbackactivity);
					// start async task to post QR code activity
					//new QRCodeActivityTask().execute(contents, "I scanned a QR code!");
					finish();
					}
			} else {
				// Canceled, use abort code
				SharedPreferences settings = getSharedPreferences("container_prefs", 0);
				SharedPreferences.Editor editor = settings.edit();
				editor.putString("callbackUrl", ABORT_CODE);

				// Commit the edits!
				editor.commit();
				finish();
			}
		}

		// Camera capture or gallery selection complete; compress and upload file to server
		else if (requestCode == CAPTURE_IMAGE_RQ_CODE || requestCode == CHOOSE_IMAGE_RQ_CODE) {
			if (resultCode == RESULT_OK) {
				try {
					pd = ProgressDialog.show(this, "", 	"Compressing photo...", true);
					new CompressAndUploadPhotoTask().execute(requestCode == CAPTURE_IMAGE_RQ_CODE ? 
							Uri.fromFile(photo) : intent.getData());		            
				} catch (Exception e) {
					finish();
				}
			} else {
				finish();
			}
		}
		else if (requestCode == FILE_UPLOAD_CODE)
		{
			if(resultCode == RESULT_OK) {
				String resultstring = intent.getStringExtra("filepath");
				System.out.println("Result string: " + resultstring);
				photo= new File (resultstring);
				pd = ProgressDialog.show(this, "", 	"Uploading File ...", true);
				new PostPhotoTask().execute();
			}
		}
		
		else if (requestCode == NFC_TRANSFER_CODE)
		{
			if(resultCode == RESULT_OK) {
				String resultstring = intent.getStringExtra("nfc_value");
				System.out.println("NFC result String: " + resultstring);
				Intent nfc_callbackactivity = getIntent();
				nfc_callbackactivity.putExtra("uploadResult", resultstring);
				nfc_callbackactivity.putExtra("method", "nfc");
				nfc_callbackactivity.putExtra("playernumber", player_number);
				setResult(RESULT_OK, nfc_callbackactivity);
				
				finish();
			}
		}
		

		System.out.println(" Finishing onActivity Result");
	}


	
	class CompressAndUploadPhotoTask extends AsyncTask<Uri, Void, Boolean> {
		protected Boolean doInBackground(Uri... params) {
			try {
				InputStream imageStream = getContentResolver().openInputStream(params[0]);
				BitmapFactory.Options options=new BitmapFactory.Options();
				options.inSampleSize = 4;
				Bitmap image = BitmapFactory.decodeStream(imageStream, null, options);
				if (photo == null) {
					photo = getOutputMediaFile(MEDIA_TYPE_IMAGE);
				}

				int rotation = -1, rotateDegrees = 0;
				long fileSize = photo.length();
				Cursor mediaCursor = HtmlCallbackActivity.this.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[] {MediaStore.Images.ImageColumns.ORIENTATION, MediaStore.MediaColumns.SIZE }, MediaStore.MediaColumns.DATE_ADDED + ">=?", new String[]{String.valueOf(captureTime/1000 - 1)}, MediaStore.MediaColumns.DATE_ADDED + " desc");
				if (mediaCursor != null && captureTime != 0 && mediaCursor.getCount() !=0 ) {
					while(mediaCursor.moveToNext()){
						long size = mediaCursor.getLong(1);
						//Extra check to make sure that we are getting the orientation from the proper file
						if(size == fileSize){
							rotation = mediaCursor.getInt(0);
							break;
						}
					}
				}
				if (rotation == -1) {
					ExifInterface exif = new ExifInterface(photo.getAbsolutePath());
					rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
					rotateDegrees = 0;
				}
				switch(rotation) {
				case ExifInterface.ORIENTATION_ROTATE_90:
					rotateDegrees-=90;
				case ExifInterface.ORIENTATION_ROTATE_180:
					rotateDegrees-=90;
				case ExifInterface.ORIENTATION_ROTATE_270:
					rotateDegrees-=90;
				}
				Matrix matrix = new Matrix();
				matrix.postRotate(rotateDegrees);
				image = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);

				FileOutputStream out = new FileOutputStream(photo);
				
				ByteArrayOutputStream bao = new ByteArrayOutputStream();
				image.compress(CompressFormat.JPEG,  90,  out);


				System.out.println("Compress and Upload Task Completed");
				return true;
			} catch (Exception e) {
				return false;
			} 
			
			
		}
	
		protected void onPostExecute(Boolean result) {
			System.out.println("On Post Execute");
			if (result == false)
				finish();

			if (pd != null)
				pd.dismiss();
			pd = ProgressDialog.show(HtmlCallbackActivity.this, "", "Uploading photo...", true);
			new PostPhotoTask().execute();
		}
	}


	// uploads photo we just took to the server, and then finishes up
	
	class PostPhotoTask extends AsyncTask<String, Void, String> {
		protected String doInBackground(String... params) {
			try {
				String url = uploadPhotoUrl;
				
				
				System.out.println("Photo Url Photo: " + uploadPhotoUrl);
				MultipartEntity reqEntity = new MultipartEntity();  
				
				field = "userfile";
				
				System.out.println("field = userfile");
				FileBody bin = new FileBody(photo); // "image/jpg");

				
				//System.out.println("player string: " + playerstring.toString());
				reqEntity.addPart(field, bin);
				System.out.println("assign field in bin");
				reqEntity.addPart("player", new StringBody(player_number));

				//System.out.println("trying to add player number");
				//reqEntity.addPart("name", new StringBody(name));
				//reqEntity.addPart("tag", new StringBody(tag));
				
				//reqEntity.addPart("type", new StringBody(type));
				//System.out.println("add type as a field");
				
				//reqEntity.addPart("upload", new StringBody("Upload"));
				
				//reqEntity.addPart("shouldPersist", new StringBody(shouldPersist));
				
				System.out.println("file body complete" );
				System.out.println("File Entity: " + reqEntity.toString());
				HttpResponse serverResponse = uploadPhoto(url, reqEntity);
				return new BasicResponseHandler().handleResponse(serverResponse);
			} catch (Exception e) {
				return null;
			}
		}
		protected void onPostExecute(String result) {
			if (pd != null)
				pd.dismiss();

			if (result == null)
				finish();
			
			System.out.println("Uploaded to " + uploadPhotoUrl + " with response " + result);
			
			//Passes the result of the upload back out of the callbackactivity
			Intent callbackactivity = getIntent();
			callbackactivity.putExtra("uploadResult", result);
			callbackactivity.putExtra("method", "upload");
			callbackactivity.putExtra("playernumber", player_number);
			setResult(RESULT_OK, callbackactivity);
			
			
			finish();
			
			try {
				JSONObject data = (JSONObject) new JSONTokener(result).nextValue();
				int id = data.getInt("id");
				// Scan was successful.  Replace {CODE} with scan results
				String finalUrl = callbackUrl.replaceAll("\\{id\\}", id+"");

				// Save callback to be refreshed in the caller version of this app
				SharedPreferences settings = getSharedPreferences("container_prefs", 0);
				SharedPreferences.Editor editor = settings.edit();
				editor.putString("callbackUrl", finalUrl);
				//editor.putString("uploadDimensions", result);

				// Commit the edits!
				editor.commit();
				finish();

			} catch (Exception e) {
				finish();
			}
		}
		
		
	}



	/** HELPERS **/


	/**
	 * Helper to determine if a mode is supported by this activity.
	 * @param mode
	 * @return
	 */
	private boolean isValidMode(String mode) {
		for (int i = 0; i < MODES.length; i++)
			if (MODES[i].equals(mode))
				return true;

		return false;
	}


	public static final int MEDIA_TYPE_IMAGE = 1;


	/** Create a File for saving an image or video */
	private static File getOutputMediaFile(int type){
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		File mediaStorageDir = new File(Environment.getExternalStorageDirectory() + "/meeImages");
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.

		// Create the storage directory if it does not exist
		if (! mediaStorageDir.exists()){
			if (! mediaStorageDir.mkdirs()){
				Log.d("MEE", "Failed to create directory");
				return null;
			}
		}

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File mediaFile;
		if (type == MEDIA_TYPE_IMAGE){
			mediaFile = new File(mediaStorageDir.getPath() + File.separator +
					"IMG_"+ timeStamp + ".jpg");
		} else {
			return null;
		}

		return mediaFile;
	}

	private synchronized HttpResponse uploadPhoto(String url, MultipartEntity reqEntity) {
		// Create a new HttpClient and Post Header  
		DefaultHttpClient httpclient = new DefaultHttpClient();  
		HttpPost httppost = new HttpPost(url);
		HttpResponse response = null;
		System.out.println("Attempting to upload the Multipart Entity");
		try {  
			if (reqEntity != null)
				httppost.setEntity(reqEntity);

			// Execute HTTP Post Request  
			response = httpclient.execute(httppost);
			System.out.println("***** RESPONSE: "+response.getStatusLine().toString());
			System.out.println("Response is: " + response.getStatusLine().toString());

		} catch (ClientProtocolException e) {  
			e.printStackTrace(System.out);
		} catch (IOException e) {  
			e.printStackTrace(System.out);
		}

		return response;
	}

	
}
