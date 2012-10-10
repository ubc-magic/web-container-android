package com.sensetecnic.container;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


public class Accelerometer extends Activity implements SensorEventListener {

	
	private static final String tag = "Accelerometer";
	private SensorManager sm;
	private Sensor accelerometers;
	 
	
	TextView xacc= null;
	TextView yacc = null;
	TextView zacc = null;
	TextView xorient = null;
	TextView yorient = null;
	TextView zorient = null;
	TextView text = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometers = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
        setContentView(R.layout.test_accelerometer);
        
    	System.out.println("accelerometer initiated");
 
        xacc = (TextView) findViewById(R.id.xvalue);
        yacc = (TextView) findViewById(R.id.yvalue);
        zacc = (TextView) findViewById(R.id.zvalue);
        xorient = (TextView) findViewById(R.id.xvalues);
        yorient = (TextView) findViewById(R.id.yvalues);
        zorient = (TextView) findViewById(R.id.zvalues);
 
 
    }
 
 
 
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
        	
        	
            final float alpha = (float) 0.8;
            float [] gravity = new float[3];
            float [] linear_accel = new float[3];
            float [] geomagneticMatrix = new float[3];
            float [] accelerometerValues = new float[3];
            
            switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                accelerometerValues = event.values.clone();
                System.out.println("getting values for accelerometer field");
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                geomagneticMatrix = event.values.clone();
                System.out.println("getting values for magnetic field");
                break;
            default:
                break;
            }   
            
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
            linear_accel[0] = event.values[0] - gravity[0];
            linear_accel[1] = event.values[1] - gravity[1];
            linear_accel[2] = event.values[2] - gravity[2];
            System.out.println("linear accel 1 =" + linear_accel[0]);
            
            
            xacc.setText("Accel X: " + linear_accel[0]);
            yacc.setText("Accel Y: " + linear_accel[1]);
            zacc.setText("Accel Z: " + linear_accel[2]);
            
            if (geomagneticMatrix != null  && accelerometerValues != null){
	        	float[] R = new float[16];
	        	float[] I = new float[16];
	            float[] values = new float[3];
	        	SensorManager.getRotationMatrix(R, null, accelerometerValues, geomagneticMatrix);
	        	SensorManager.getOrientation(R, values);
	        	System.out.println("geomagnetic matrix being tested " + accelerometerValues[0] + " " + geomagneticMatrix[0]);
	            
	            xorient.setText("Orientation X: " + values[0]);
	            yorient.setText("Orientation Y: " + values[1]);
	            zorient.setText("Orientation Z: " + values[2]);
            }
        }
    }
    
 
    public void onAccuracyChanged(int sensor, int accuracy) {
    	Log.d(tag,"onAccuracyChanged: " + sensor + ", accuracy: " + accuracy);
 
    }
 
 

	@Override
    protected void onResume() {
        super.onResume();
        sm.registerListener(this, 
        		accelerometers,
                SensorManager.SENSOR_DELAY_NORMAL);
    }
 
    @Override
    protected void onStop() {
        sm.unregisterListener(this);
        super.onStop();
    }



	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}


 
 
}
