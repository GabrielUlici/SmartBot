package com.nightideaslab.smartbot;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import com.nightideaslab.util.TCPClient;
import com.nightideaslab.util.accVal;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;


public class AccelerometerActivity extends Fragment
		implements	android.hardware.SensorEventListener,
					android.os.Handler.Callback
{
	public static final int MSG_UIREFRESHCOM   	= 10000;		/** UI: Refresh Connection Status */
	public static final int MSG_CENTER			= 10003;		/** UI: touch on robot icon */
	public static final int MSG_NEWSTATUSMESSAGE= 10005;		/** UI: update application status line */
	public static final int MSG_STATUSMSG		= 10010;		/** Change status message */
	
	public final static String TAG = "**SB Accelerometer**";

	boolean bbHWSensorInitialized;
	private SensorManager sm;									/** Android Sensor Manager */
	public Sensor sensor_acc;									/** Accelerometer */
	
	accVal acc_val;
	long nAccVal;												/** counter */
	
	long last_sent_ms;
	String last_sent_msg;										/** last message sent to server */
	
	double sensor_event_rate;									/** sensor event rate */
	long ts_last_rate_calc;
	long last_nAccVal;
	long ts_connection;
	
	boolean _bbCanSend;											/** TRUE = robot connect, can send data */
	boolean _bbStartSend;										/** TRUE = start sending accelerometer data to robot */
	
	public static String address, robot, port;
	
	NumberFormat 	formatter,formatter3;
	
	int uitoken;												/** auto-inc number used to connect tcp and UI */
	
	private Context context;
	public Context getAppContext()	{ return context; }
	
	ConnectivityManager cm;										/** Check connection status */
	
	/** UI controls */
	TextView 	tv_accx, tv_accy, tv_accz, tv_roll, tv_pitch;
	
	/**
	 * Constructor
	 * 
	 * Initialize members and load configuration from file.
	 */
	public AccelerometerActivity()
	{
		Log.d(TAG,"AccelerometerActivity()");
		bbHWSensorInitialized=false;
						
		last_sent_ms=0;
		uitoken=0;
		_bbStartSend=false;
		sm=null;
		acc_val= new accVal();
		last_sent_ms=0;
		nAccVal=0;
		formatter = new DecimalFormat("#000.00");
		
		formatter3 = new DecimalFormat("#00");
		ts_connection=0;
		last_nAccVal=0;
	}
	
	/**
	 * Initialize UI controls of main activity
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		
		
		Log.e(TAG, "Setting the accelerometer layout");
		ViewGroup root = (ViewGroup) inflater.inflate(R.layout.accelerometer_layout, null);
		 
        tv_accx = (TextView) root.findViewById(R.id.dataAccX);
        tv_accy = (TextView) root.findViewById(R.id.dataAccY);
        tv_accz = (TextView) root.findViewById(R.id.dataAccZ);
        tv_roll = (TextView) root.findViewById(R.id.dataRoll);
        tv_pitch = (TextView) root.findViewById(R.id.dataPitch);
               
        return root; 
        
	}
	    
    /**
     * App onStart() (Android framework)
     * 
     * @see android.app.Activity#onStart()
     */
    @Override
    public void onStart ()
    {
    	Log.d(TAG,"onStart start");
	   	super.onStart ();   
    }

    /**
     * Android framework
     * 
     * @see android.app.Activity#onResume()
     */
   	@Override 
   	public void onResume ()
   	{
   		Log.d(TAG,"onResume start");
   		initHWSensors ();
   		super.onResume();
   		
   	}
   	
   	/**
   	 * Android framework
   	 * 
   	 * @see android.app.Activity#onPause()
   	 */
   	@Override
   	public void onPause ()
   	{
   		Log.d(TAG,"onPause start");
   		closeHWSensors ();
   		super.onPause ();
   	}
   
   	@Override
   	public void onStop ()
   	{
   		Log.d(TAG,"onStop start");
   		closeHWSensors ();
   		super.onStop();
   	}

   	@Override
   	public void onDestroy ()
   	{
   		Log.d(TAG,"onDestroy start");
   		super.onDestroy();
   	}

	/**
	 * Initialize Sensor: Accelerometer
	 */
	private void initHWSensors ()
	{
		final Activity activity = getActivity();
		Log.d(TAG,"initHWSensors");
		if (bbHWSensorInitialized) return;
		bbHWSensorInitialized=true;
		if (sm==null) sm = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
	    if (!initAccelerometerSensor ())
	    {
	    	Log.d(TAG,"Error we cannot initializaze the senzors");
	    	Toast.makeText(activity.getApplicationContext(),"Error we cannot initializaze the senzors" ,Toast.LENGTH_SHORT).show();
	    }
      
	}
		
	/**
	 * Release accelerometer
	 */
	private void closeHWSensors ()
	{
		final Activity activity = getActivity();
		Log.d(TAG,"closeHWSensors");
		if (!bbHWSensorInitialized) return;
		bbHWSensorInitialized=false;
		Log.d(TAG,"Closing sensors.");
		
		if (sm==null)
		{
			Log.d(TAG,"Sensor manager not available");
			Toast.makeText(activity.getApplicationContext(),"Error we cannot initializaze the senzors" ,Toast.LENGTH_SHORT).show();
			return;
		}
		
		if (sensor_acc!=null) sm.unregisterListener(this,sensor_acc);
		sensor_acc=null;	
		sm=null;
	}
	
	/**
	 * Acquire accelerometer device
	 * 
	 * Use Sensor Manager to acquire control of accelerometer sensor.
	 */
	private boolean initAccelerometerSensor ()
	{
		if (sm==null)
		{
			Log.d(TAG,"Sensor manager not available");
			return false;
		}
		
		try
		{                      
			sensor_acc= sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			if (sensor_acc==null)
			{
				Log.d(TAG,"Accelerometer sensors not available");
			   	return false;
			}
	
			boolean bbSuccess = sm.registerListener(this,sensor_acc,SensorManager.SENSOR_DELAY_NORMAL);	
			if (bbSuccess)
			{
				Log.d(TAG,"ACCELEROMETER SENSORS OK.");
		   		return true;
			}
			else
				Log.d(TAG,"Failed to request the accelerometer sensor.");
		}
		catch (Exception e)
		{
			Log.d(TAG,"Exception while getting the accelerometer sensor");
		}
		return false;
	}
	
	/**
	 * Changed sensor accuracy notification
	 * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor, int)
	 */
	public void onAccuracyChanged(Sensor arg0, int arg1) 
	{
	}
	
	/**
	 * Accelerometer Values notification
	 * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)
	 */
	public void onSensorChanged(SensorEvent event) 
	{
		if (event.accuracy==SensorManager.SENSOR_STATUS_UNRELIABLE)
		{
			onSensorUnreliable ();
			return;
		}
		
		int type=event.sensor.getType();
		switch (type)
		{
			case Sensor.TYPE_ACCELEROMETER :
				onAccVal (event.values[0],event.values[1],event.values[2],event.timestamp);
				
				break;
			default:
				Log.d(TAG,"Unknown sensor event: "+event.sensor.getType());
		}
	}
   	
	/**
	 * 
	 * Receives message from all application objects and execute them 
     * 
     * @param msg Message object
     * @see android.os.Handler.Callback#handleMessage(android.os.Message)
     */
	//@Override
	public boolean handleMessage(Message msg) 
	{	
		switch (msg.what)
		{
		case TCPClient.MSG_COMSTATUSCHANGE:
		case TCPClient.MSG_NEWSTATUSMESSAGE:
		case MSG_NEWSTATUSMESSAGE:	
			{
				if (msg.arg2==uitoken)
				{
					Log.e(TAG,"Status Message ACCE : " + (String)msg.obj);
				}
				return true;
			}
		
		case TCPClient.MSG_SENT:
			{
				if (msg.arg2==uitoken)
					last_sent_msg = (String)msg.obj;
				return true;
			}
		}
		return false;
	}

	/**
	 * Android Framework: called when sensor calibration is needed
	 */
	public void onSensorUnreliable ()
	{
		if ((System.currentTimeMillis())>5000)
		{
			Log.d(TAG,"You need to calibrate your sensors.");
		}
	}
	
	long last_data_ts=0;
	
	/**
	 *  Receives data from the accelerometer
	 *  
	 *  @param ax,ay,az 	Accelerometer data
	 *  @param ts			Time stamp of arrived data
	 */
	public void onAccVal (float ax, float ay, float az, long ts)
	{
		++nAccVal;
		++last_nAccVal;
				
		acc_val.accx = -ax;
		acc_val.accy = ay;
		acc_val.accz = az;
		acc_val.ms = ts;
			
		acc_val.roll = (float)Math.atan2 (ax,az);
		acc_val.pitch = (float)Math.atan2 (ay,az);
			
		acc_val.roll = (float)((acc_val.roll*180.0)/Math.PI);
		acc_val.pitch = (float)((acc_val.pitch*180.0)/Math.PI);
		
		if (true)
		{
			double coeff = 1/9.81;
			acc_val.accx=(float)(acc_val.accx*coeff);
			acc_val.accy=(float)(acc_val.accy*coeff);
			acc_val.accz=(float)(acc_val.accz*coeff);
		}
		if (ts-last_data_ts>(1000/3)*1000000)
		{
			UI_RefreshSensorData ();
			last_data_ts=ts;
		}
		
	}
		
	
	/**
	 * Update sensor data UI
	 */
	private void UI_RefreshSensorData ()
	{
		tv_accx.setText(formatter.format(acc_val.accx));
		tv_accy.setText(formatter.format(acc_val.accy));
		tv_accz.setText(formatter.format(acc_val.accz));
		tv_roll.setText(formatter.format(acc_val.roll));
		tv_pitch.setText(formatter.format(acc_val.pitch));
		
		long now = System.currentTimeMillis();
		
		if (now-ts_last_rate_calc>1000)
		{
			sensor_event_rate=last_nAccVal/((now-ts_last_rate_calc)/1000.0);
			ts_last_rate_calc=now;
		}
	}
	
	int old_conn_status=-1;
	

	
	/**
	 * Refresh all UI controls.
	 */
	public void UI_Refresh ()
	{
		UI_RefreshSensorData ();
	}
		
  
}
