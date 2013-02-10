package com.nightideaslab.smartbot;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import com.nightideaslab.util.Globals;
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
import android.widget.Button;
import android.widget.Toast;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;


public class AccelerometerButtonsActivity extends Fragment
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
	
	private android.os.Handler MsgQueue;						/** Async Message queue */
	
	boolean _bbCanSend;											/** TRUE = robot connect, can send data */
	boolean _bbStartSend;										/** TRUE = start sending accelerometer data to robot */
	private TCPClient _roboCOM;									/** Connection Thread */
	private Globals global;
	
	public static String address, robot, port;
	
	NumberFormat 	formatter,formatter3;
	
	int uitoken;												/** auto-inc number used to connect tcp and UI */
	
	private Context context;
	public Context getAppContext()	{ return context; }
	
	ConnectivityManager cm;										/** Check connection status */
	
	/** UI controls */
	
	Button btn_Acce_Sit, btn_Acce_Stand, btn_Acce_Start, btn_Acce_Stop;
	
	/**
	 * Constructor
	 * 
	 * Initialize members and load configuration from file.
	 */
	public AccelerometerButtonsActivity()
	{
		Log.d(TAG,"AccelerometerActivity()");
		bbHWSensorInitialized=false;
		setCOMStatus (null,false);
				
		global = new Globals();
		MsgQueue = new android.os.Handler (this);
		
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
	 * Async change connection status
	 * 
	 * @param conn			Connection object, refering to a thread
	 * @param bbCanSend		TRUE if data can be send 
	 */
	public void setCOMStatus (TCPClient conn, boolean bbCanSend)
	{
		synchronized (this)
		{
			_roboCOM=conn;
			_bbCanSend= (conn!=null) && bbCanSend;
		}
	}
	
	/**
	 * Async change connection status
	 * 
	 * @param bbCanSend		TRUE if data can be send
	 */
	public void setCOMStatus (boolean bbCanSend)
	{
		synchronized (this)
		{
			_bbCanSend=bbCanSend;
		}
	}
	
	/**
	 * check if connection object is active
	 * 
	 * @return TRUE if connection thread is active
	 */
	public boolean isConnecting ()
	{
		synchronized (this)
		{
			return _roboCOM!=null;
		}
	}
	
	/**
	 * Return Connection Active Status
	 * 
	 * @return TRUE if the connection is established, and can send data to robot
	 */
	public boolean isConnectionActive ()
	{
		synchronized (this)
		{
			return _bbCanSend;
		}
	}
	
   	/**
   	 * Execute thread connection manager
   	 */
   	public synchronized void startConnection ()
	{
   		synchronized (this)
   		{
   			if (_roboCOM!=null) return;
			
			uitoken++;
			_roboCOM = new TCPClient (MsgQueue,cm,uitoken);

			// debug parameters
			_roboCOM.msgHello="Start Connection ACCE";
							
	   	   	new Thread (_roboCOM).start();
   		}
	}
	
   	/**
   	 * Stop active connection, in exist.
   	 * 
   	 * @param bbUserNotify	TRUE to inform user about that
   	 */
	public synchronized void stopConnection (boolean bbUserNotify)
	{
		synchronized (this)
   		{
			if (_roboCOM==null) return;
			
			Log.d(TAG,"stopConnection");
			_roboCOM.sendMessageToServer("End Connection ACCE");
			{
				if (bbUserNotify && !_bbCanSend)
					bbUserNotify=false;
				_bbCanSend=false;
				// async quit: thread will terminate
				_roboCOM.quit();
				_roboCOM = null;
			}
   		}
		onRobotDisconnect (bbUserNotify);
	}
	
	/**
	 * Append a message to main application message pump
	 * 
	 * @param message	message ID
	 */
	public void sendEmptyMessage (int message)
	{
		MsgQueue.sendEmptyMessage(message);
	}
	
	/**
	 * Initialize UI controls of main activity
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		
		final Activity activity = getActivity();
		
		Log.e(TAG, "Setting the accelerometer layout");
		ViewGroup root = (ViewGroup) inflater.inflate(R.layout.accelerometer_buttons_layout, null);
		 
        btn_Acce_Stand = (Button) root.findViewById(R.id.btn_acc_stand);
        btn_Acce_Sit = (Button) root.findViewById(R.id.btn_acc_sit);
        btn_Acce_Stop = (Button) root.findViewById(R.id.btn_acc_stop);
        btn_Acce_Start = (Button) root.findViewById(R.id.btn_acc_start);
        
        /**
		 * Handling all button click events
		 * */
        btn_Acce_Sit.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				if (isConnectionActive())
				{
					_roboCOM.sendMessageToServer(global.urlSit);
				}
				else Toast.makeText(activity.getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
			}
		});
		
        btn_Acce_Stand.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				if (isConnectionActive())
				{
					_roboCOM.sendMessageToServer(global.urlStand);
				}
				else Toast.makeText(activity.getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
			}
		});
		
        btn_Acce_Start.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				if (isConnectionActive())
				{
					_roboCOM.sendMessageToServer(global.urlStand);
					_bbStartSend=true;
				}
				else Toast.makeText(activity.getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
			}
		});
		
        btn_Acce_Stop.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				if (isConnectionActive())
				{
					_bbStartSend=false;
					_roboCOM.sendMessageToServer(global.urlStopWalk);
				}
				else Toast.makeText(activity.getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
			}
		});
        
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
   		startConnection();
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
   		if (isConnectionActive()) _roboCOM.sendMessageToServer(global.urlStopWalk);
   		closeHWSensors ();
   		stopConnection (true);
   		super.onPause ();
   	}
   
   	@Override
   	public void onStop ()
   	{
   		Log.d(TAG,"onStop start");
   		if (isConnectionActive()) _roboCOM.sendMessageToServer(global.urlStopWalk);
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
		case MSG_UIREFRESHCOM:
			{
				if (msg.arg2==uitoken)
					UI_RefreshConnStatus ();
				return true;
			}
		case TCPClient.MSG_NEWSTATUSMESSAGE:
		case MSG_NEWSTATUSMESSAGE:	
			{
				if (msg.arg2==uitoken)
				{
					Log.e(TAG,"Status Message ACCE : " + (String)msg.obj);
				}
				return true;
			}
		case TCPClient.MSG_CONNECTIONSTART:
			{
				if (msg.arg2==uitoken)
				{
					onRobotConnect (true);
				}
				return true;
			}
		case TCPClient.MSG_CONNECTIONSTOP:
			{
				if (msg.arg2==uitoken)
				{
					boolean bbNotify;
					synchronized (this)
					{
						bbNotify=_bbCanSend;
					}
					onRobotDisconnect (bbNotify);
				}
				return true;
			}
		case TCPClient.MSG_SENT:
			{
				if (msg.arg2==uitoken)
					last_sent_msg = (String)msg.obj;
				return true;
			}
		case MSG_CENTER:
			{
				if (isConnecting())
				{
					stopConnection(true);
				}
				else
				{
					startConnection();
				}
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
		synchronized (this)
		{
			if (_bbCanSend && _bbStartSend) sendData (acc_val);
		}
	}
		
	/**
	 * Send data from sensors 
	 * 
	 * @param data
	 *            Accelerometer sensor data
	 */
	 public void sendData (accVal data)
	 {
		 	synchronized (this)
		 	{

		 		if (data.accx>0.2)  // bigger than 0.2 or 0.3
		 			_roboCOM.sendMessageToServer(global.urlRight);
		 			
		 		if (acc_val.accx<-0.2)  // this is the only way that it works
		 			_roboCOM.sendMessageToServer(global.urlLeft);
		 		
		 		if (data.accz>0.4)  // it is better like this because you need to incline it a bit more
		 			_roboCOM.sendMessageToServer(global.urlForward);
		 		
		 		if (data.accz<-0.3)  // it is better if the value is lower than -0.3 to move backwards
		 			_roboCOM.sendMessageToServer(global.urlBackward);
		 		
		 		if (data.accz>-0.17 && data.accz<0.10)  // This will stop the walk
		 			_roboCOM.sendMessageToServer(global.urlStopWalk);
		 		
		 	}
	 }
	
	/**
	 * Update sensor data UI
	 */
	private void UI_RefreshSensorData ()
	{
				
		long now = System.currentTimeMillis();
		
		if (now-ts_last_rate_calc>1000)
		{
			sensor_event_rate=last_nAccVal/((now-ts_last_rate_calc)/1000.0);
			ts_last_rate_calc=now;
		}
	}
	
	int old_conn_status=-1;
	
	/**
	 * Update Connection Status UI
	 */
	private synchronized void UI_RefreshConnStatus ()
	{
		TCPClient used=null;
		synchronized (this)
		{
			used=_roboCOM;
		}
		Log.e(TAG, "Is conection Active ACCE : " + isConnectionActive());
		
		if (!isConnecting())
		{
			if (old_conn_status==-2) return;
			old_conn_status=-2;
			return;
		}
		if (used!=null)
		{
			if (old_conn_status!=used.getConnStatus())
			{
				old_conn_status=used.getConnStatus();
				Log.e(TAG,"Connections Status ACCE : " + used.getConnStatusStr());
			}
		}
	}
	
	/**
	 * Refresh all UI controls.
	 */
	public void UI_Refresh ()
	{
		UI_RefreshSensorData ();
		UI_RefreshConnStatus ();
	}
	
	/**
	 * Callback: called when robot is connected.
	 * 
	 * @param bbUserNotify TRUE = report user the event (beep)
	 */
	protected void onRobotConnect (boolean bbUserNotify)
	{
		setCOMStatus (true);
		UI_RefreshConnStatus ();
	}
	
	/**
	 * Callback: called when robot is disconnected.
	 * 
	 * @param bbUserNotify TRUE= report user the event (beep)
	 */
	protected void onRobotDisconnect (boolean bbUserNotify)
	{
		UI_RefreshConnStatus ();
	}
  
}
