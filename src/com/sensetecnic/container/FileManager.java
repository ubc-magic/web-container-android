package com.sensetecnic.container;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;

public class FileManager extends Activity{

	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		String externalDrive = Environment.getExternalStorageState();
		if(!externalDrive.equals(Environment.MEDIA_MOUNTED)){
			//
		}
		else {
			//
		}
	}
}
