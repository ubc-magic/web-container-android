/**
 * (c) 2011-2012 Sense Tecnic Systems Inc.   All rights reserved.
 */

package com.sensetecnic.container;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.LinearLayout.LayoutParams;

public class HtmlContainerActivity extends Activity {

	private WebView webView;
	private String callbackUrl;
	private String currentUrl;
	private boolean justPlayedMedia = false;
	private ProgressBar pbLoading;

	public ProgressDialog pd; 
	public static final int MEDIA_TYPE_IMAGE = 1;
	private static final int CAPTURE_IMAGE_RQ_CODE = 1;
	private static final int CHOOSE_IMAGE_RQ_CODE = 2;
	
	
	private static final String OVERRIDE_PREFIX = "sensetecnic://";
	private static final String DEFAULT_URL = "http://www.google.com";
	

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
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
				if (url.startsWith(OVERRIDE_PREFIX)) {
					Intent intent = new Intent(HtmlContainerActivity.this, HtmlCallbackActivity.class);
					intent.setData(Uri.parse(url));
					startActivity(intent);
					return true;
				} 
				else if (url.startsWith("http://www.youtube.com")) {
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

				// ordinary link
				return false;
			}

		});

		String url = getIntent().getStringExtra("url");
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
		case R.id.camera:
			camerafunction();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void testaccel() {
		Intent intent = new Intent(HtmlContainerActivity.this, Accelerometer.class);
		startActivity(intent);
	}
	
	private void camerafunction() {
		File photo;
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		photo = getOutputMediaFile(MEDIA_TYPE_IMAGE);
		Uri cameraFileUri = Uri.fromFile(photo); // create a file to save the image
		intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraFileUri); // set the image file name
		startActivityForResult(intent, CAPTURE_IMAGE_RQ_CODE);
	}
	
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
	
	private void typeUrl() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		final EditText input = new EditText(this);
		final LinearLayout layout = new LinearLayout(this);
		final TextView instructions = new TextView(this);
		input.setText("http://");
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

		// load callback URL from preferences, if it exists, and whether the user has requested a quit.  
		// These preferences will only exist if, while we were suspended, our sub-launched self wrote the preferences 
		// because it wants us to refresh the page or quit.
		SharedPreferences settings = getSharedPreferences("container_prefs", 0);
		callbackUrl = settings.getString("callbackUrl", "");
		boolean shouldQuit = settings.getBoolean("quitChallenge", false);

		// now clear the preferences again so that we don't refresh ourselves/quit if suspended by some other
		// activity
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("callbackUrl", "");
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

	}


	@Override
	public void onBackPressed() {
		if (webView.canGoBack()) {
			webView.goBack();
		}
		else {
			doLeaveChallengeConfirmation();
		}
	}


	/**
	 * Handle page refresh
	 */
	private void doRefresh() {
		webView.reload();
	}

	/**
	 * Handle leaving the challenge via the back button or the menu option.
	 */
	private void doLeaveChallengeConfirmation() {
		finish();
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


}