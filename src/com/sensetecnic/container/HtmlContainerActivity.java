/**
 * (c) 2011-2012 Sense Tecnic Systems Inc.   All rights reserved.
 */

package com.sensetecnic.container;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

public class HtmlContainerActivity extends Activity implements SensorEventListener {

	private WebView webView;
	private String callbackUrl;
	private String uploadDimensions;
	private String uploadDimensionsTest;
	private String nfc_upload;
	private String qr_upload;
	private String currentUrl;
	private String player;
	private double accelerometer_rate;
	private boolean accelerometer_enabled = false;
	
	private boolean justPlayedMedia = false;
	private ProgressBar pbLoading;

	public ProgressDialog pd; 
	public static final int MEDIA_TYPE_IMAGE = 1;
	private static final int CAPTURE_IMAGE_RQ_CODE = 1;
	private static final int CHOOSE_IMAGE_RQ_CODE = 2;
	
	
	// Sensor management for accelerometer data
	private SensorManager sm;
	private Sensor accelerometers;
	private float [] linear_accel = new float[3];
	
	// For uploading accelerometer data periodically
	TimerTask uploadAccelTask;
	Timer uploadAccel;
	
	
	
	private static final String OVERRIDE_PREFIX = "http://www.sinfulseven.net/coffeeshop/";
	private static final String DEFAULT_URL = "http://sinfulseven.net/coffeeshop";
	
	

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		System.out.println(DEFAULT_URL);
		System.out.println("-------------STARTING-------------");
		
		// Initialize Sensor management information 
		sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometers = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.html_challenge);
		pbLoading = (ProgressBar)findViewById(R.id.pbLoading);
		webView = (WebView) findViewById(R.id.webview);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		webView.setWebChromeClient(new ContainerWebChromeClient());
		
		webView.setWebViewClient(new WebViewClient() {

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				System.out.println("Test this url: " + url);
				if (url.startsWith(OVERRIDE_PREFIX)) {
					cancelAccelUpload();
					System.out.println("Starting the override Prefix");
					Intent intent = new Intent(HtmlContainerActivity.this, HtmlCallbackActivity.class);
					intent.setData(Uri.parse(url));
					startActivityForResult(intent, 1);
					return true;
				} 
				else if (url.startsWith("http://www.youtube.com")) {
					System.out.println("Going to Youtube:");
					Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url)); startActivity(i);
					return true;
				} else if (url.startsWith("http://www.facebook.com")) {
					Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url)); startActivity(i);
					return true;
				} else if (url.endsWith(".mp3") || url.endsWith(".wav")) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.parse(url), "audio/*");
					startActivity(intent);   
					justPlayedMedia = true;
					return true;
				} else if (url.endsWith(".mp4") || url.endsWith(".3gp")) {
					Intent intent = new Intent(Intent.ACTION_VIEW); 
					intent.setDataAndType(Uri.parse(url), "video/*");
					startActivity(intent); 
					justPlayedMedia = true;
					return true;
				}
				
				System.out.println("Ordinary URL: " + url);
				// ordinary link
				return false;
			}

		});

		String url = getIntent().getStringExtra("url");
		String result = getIntent().getStringExtra("result");
		System.out.println ("Result for the common page is" + result);
		
		currentUrl = url;

		if (url != null) {
			if (url.startsWith(OVERRIDE_PREFIX)) {
				Intent intent = new Intent(HtmlContainerActivity.this, HtmlCallbackActivity.class);
				intent.setData(Uri.parse(url));
				startActivity(intent);
				// do we want to load a webpage from this scan?  if not, we're done.
				if (!url.contains("ret="))
					finish();
			}
			else {
				System.out.println("Webview loading: " + url);
				webView.loadUrl(url);
			}

		} else {
			// load default url
			System.out.println("Webview loading: " + url);
			webView.loadUrl(DEFAULT_URL);
		}

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.html_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.refresh:
			doRefresh();
			return true;
		case R.id.gotourl:
			typeUrl();
			return true;
		case R.id.testaccel:
			testaccel();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	protected void onSaveinstanceState(Bundle outState) {
		webView.saveState(outState);
	}
	
	private void testaccel() {
		Intent intent = new Intent(HtmlContainerActivity.this, Accelerometer.class);
		startActivity(intent);
	}

	
	private void typeUrl() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		final EditText input = new EditText(this);
		final LinearLayout layout = new LinearLayout(this);
		final TextView instructions = new TextView(this);
		input.setText("http://www.sinfulseven.net/coffeeshop");
		input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
		layout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		input.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1f));
		instructions.setText("Enter URL:");
		instructions.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f));
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.addView(instructions);
		layout.addView(input);
		alert.setView(layout);
		alert.setPositiveButton("Go", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString().trim();
				webView.loadUrl(value);
			}
		});
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.cancel();
			}
		});
		alert.show();
	}


	@Override
	public void onResume() {
		super.onResume();
		if (justPlayedMedia) {
			justPlayedMedia = false;
			return;
		}

		sm.registerListener(this, accelerometers,SensorManager.SENSOR_DELAY_NORMAL);
		
		// load callback URL from preferences, if it exists, and whether the user has requested a quit.  
		// These preferences will only exist if, while we were suspended, our sub-launched self wrote the preferences 
		// because it wants us to refresh the page or quit.
		SharedPreferences settings = getSharedPreferences("container_prefs", 0);
		callbackUrl = settings.getString("callbackUrl", "");
		
		nfc_upload = settings.getString("nfc_retrieved_message", "");
		System.out.println("nfc_upload : " + nfc_upload);
		
		System.out.println("callbackURL is: " + callbackUrl);
		boolean shouldQuit = settings.getBoolean("quitChallenge", false);

		// now clear the preferences again so that we don't refresh ourselves/quit if suspended by some other
		// activity
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("callbackUrl", "");
		editor.putString("nfc_retrieved_message", "");
		editor.putBoolean("quitChallenge", false);
		editor.commit();


		
		
		if (shouldQuit) {
			doLeaveChallengeConfirmation();
		}

		else if (callbackUrl != null && !callbackUrl.equals("")) {
			if (callbackUrl.equals(HtmlCallbackActivity.ABORT_CODE)) {
				if (currentUrl == null || currentUrl.startsWith(DEFAULT_URL)) 
					finish();
			}
			else {
				System.out.println("Webview loading: " + callbackUrl);
				webView.loadUrl(callbackUrl);
				currentUrl = callbackUrl;
			}
		}
		else if (nfc_upload != null && !nfc_upload.equals("")) {
			webView.loadUrl("javascript:gotNFC('"+nfc_upload+"');");
		}
//		else if (uploadDimensions != null && !uploadDimensions.equals("")){
//			webView.loadUrl("javascript:gotImage('"+uploadDimensions+"');");
//		}

	}


	@Override
	public void onBackPressed() {
		if (webView.canGoBack()) {
			webView.goBack();
		}
		else {
			doLeaveChallengeConfirmation();
		}
		cancelAccelUpload();
	}

    @Override
    protected void onStop() {
        sm.unregisterListener(this);
        cancelAccelUpload();
        super.onStop();
    }


	/**
	 * Handle page refresh
	 */
	private void doRefresh() {
		webView.reload();
	}

	private void startAccelUpload()
	{
		uploadAccel = new Timer();
		uploadAccel.schedule(new SendAccelerometerData(), (long)(1.0/accelerometer_rate * 1000), (long)(1.0/accelerometer_rate* 1000));
	}
	
	private void cancelAccelUpload()
	{
		if (accelerometer_enabled)
		{
		uploadAccel.cancel();
		}
	}
	
	/**
	 * Handle leaving the challenge via the back button or the menu option.
	 */
	private void doLeaveChallengeConfirmation() {
		finish();
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		System.out.println("On Activity Result");
		if (resultCode == RESULT_OK)
		{
			if (data.getStringExtra("method").equals("nfc"))
			{
				nfc_upload = data.getStringExtra("uploadResult");
				System.out.println("NFC upload result: " + nfc_upload);
				if (nfc_upload != null && !nfc_upload.equals("")) {
					webView.loadUrl("javascript:gotNFC('"+nfc_upload+"');");
				}
					
			}
			else if (data.getStringExtra("method").equals("qr"))
			{
				System.out.println("QR value");
				qr_upload=data.getStringExtra("qr_result");
				if (qr_upload != null && !qr_upload.equals("")) {
					webView.loadUrl("javascript:gotQR('"+qr_upload+"');");
				}
				
			}
			else if (data.getStringExtra("method").equals("accel"))
			{
				System.out.println("ACCEL");
				accelerometer_rate = data.getDoubleExtra("accel_rate", 5.0);
				accelerometer_enabled = true;
				player = data.getStringExtra("playernumber");
				startAccelUpload();
				System.out.println("start accel Upload");
			}
			else if (data.getStringExtra("method").equals("upload"))
			{
				System.out.println("Upload this shit now");
				uploadDimensionsTest=data.getStringExtra("uploadResult");
				System.out.println("Upload Dimension Test" + uploadDimensionsTest);
				if (uploadDimensionsTest != null && !uploadDimensionsTest.equals("")){
					webView.loadUrl("javascript:gotImage('"+uploadDimensionsTest+"');");
				}
				System.out.println("uploadDimesnions passed over" );
			}
		}
		System.out.println("Onactivityresult complete");
	}

	/**
	 * Custom web chrome client so that we can grant geolocation privileges automatically and display alerts if they come up
	 * 
	 * @author tom
	 *
	 */
	class ContainerWebChromeClient extends WebChromeClient {
		@Override
		public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
			callback.invoke(origin, true, false);
		}

		@Override
		public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
			new AlertDialog.Builder(view.getContext()).setMessage(message).setCancelable(true).show();
			result.confirm();
			return true;
		}

		public void onProgressChanged(WebView view, int progress) 
		{
			if (progress < 100 && pbLoading.getVisibility() == ProgressBar.GONE) {
				pbLoading.setVisibility(ProgressBar.VISIBLE);				
			}
			pbLoading.setProgress(progress);
			if (progress == 100) {
				pbLoading.setVisibility(ProgressBar.GONE);
			}
		}

	}


	private class SendAccelerometerData extends TimerTask {

		@Override
		public void run() {
			String accelerometer_value = null;
			accelerometer_value = "{ \"x\": \"" + linear_accel[0] + "\",\"y\": \"" + linear_accel[1] + "\", \"z\": \"" + linear_accel[2] + "\", \"player\": \"" + player + "\" }";
//			JSONArray json_accelerometer_value = new JSONArray(Arrays.asList(linear_accel));
//			accelerometer_value = json_accelerometer_value.toString();
			System.out.println ("Accelerometer values are: " + accelerometer_value);
			
			if (linear_accel == null )
			{
	            System.out.println("No Accelerometer Data found.");
				uploadAccel.cancel();
			}
			else{
			webView.loadUrl("javascript:updateAccelData('"+accelerometer_value+"');");
			}
		}
		
	}

	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	public void onSensorChanged(SensorEvent event) {
		synchronized (this) {
            final float alpha = (float) 0.8;
            float [] gravity = new float[3];
            

            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
            linear_accel[0] = event.values[0] - gravity[0];
            linear_accel[1] = event.values[1] - gravity[1];
            linear_accel[2] = event.values[2] - gravity[2];
            
            
		}
		
	}

}