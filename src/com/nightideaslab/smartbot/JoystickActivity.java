package com.nightideaslab.smartbot;

import com.nightideaslab.util.Globals;
import com.nightideaslab.util.TCPClient;
import com.zerokol.views.JoystickView;
import com.zerokol.views.JoystickView.OnJoystickMoveListener;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class JoystickActivity extends Activity 
		implements android.os.Handler.Callback
{
	public static final int MSG_UIREFRESHCOM   	= 10000;		/** UI: Refresh Connection Status */
	public static final int MSG_CENTER			= 10003;		/** UI: touch on robot icon */
	public static final int MSG_NEWSTATUSMESSAGE= 10005;		/** UI: update application status line */
	public static final int MSG_STATUSMSG		= 10010;		/** Change status message */
	
	public final static String TAG = "**SB Joystick**";

	long last_sent_ms;

	String last_sent_msg;										/** last message sent to server */

	private android.os.Handler MsgQueue;						/** Async Message queue */

	boolean _bbCanSend;											/** TRUE = robot connect, can send data */
	private TCPClient _roboCOM;									/** Connection Thread */
	private Globals global;

	public static String address, robot, port;

	int uitoken;												/** auto-inc number used to connect tcp and UI */

	private Context context;
	public Context getAppContext()	{ return context; }

	ConnectivityManager cm;										/** Check connection status */
	
	//private int angleTemp;
	//private int powerTemp;
	private JoystickView joystickL;
	private JoystickView joystickR;
	
	Button btn_Joy_Sit, btn_Joy_Stand, btn_Joy_Rightkick, btn_Joy_Leftkick, btn_Joy_Rightbackkick, btn_Joy_Leftbackkick;

	Handler h;
	
	/**
	 * Constructor
	 * 
	 * Initialize members and load configuration from file.
	 */
	public JoystickActivity()
	{
		Log.d(TAG,"JoystickActivity()");

		setCOMStatus (null,false);

		global = new Globals();
		MsgQueue = new android.os.Handler (this);

		last_sent_ms=0;
		uitoken=0;
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
			_roboCOM.msgHello="Start Connection JOYS";

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
			_roboCOM.sendMessageToServer("End Connection JOYS");
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

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.context=getApplicationContext();

		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = 
			        new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
			}
		
		cm = (ConnectivityManager) getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);

		Log.d(TAG,"Creating the JoyStick layout..");
		setContentView(R.layout.joystick_layout);

		// Setting a background image to the action bar
		final ActionBar actionBar = getActionBar();
		Drawable background =getResources().getDrawable(R.drawable.w_top_bar);
		actionBar.setBackgroundDrawable(background);
		
		// Start Connecting with the robot
		startConnection();
						
		joystickL = (JoystickView) findViewById(R.id.joystickViewL);
		joystickR = (JoystickView) findViewById(R.id.joystickViewR);
		btn_Joy_Sit = (Button) findViewById(R.id.btn_joy_sit);
		btn_Joy_Stand = (Button) findViewById(R.id.btn_joy_stand);
		btn_Joy_Leftkick = (Button) findViewById(R.id.btn_joy_leftkick);
		btn_Joy_Rightkick = (Button) findViewById(R.id.btn_joy_rightkick);
		btn_Joy_Leftbackkick = (Button) findViewById(R.id.btn_joy_leftbackkick);
		btn_Joy_Rightbackkick = (Button) findViewById(R.id.btn_joy_rightbackkick);
		
		btn_Joy_Sit.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				if (isConnectionActive())
				{
					_roboCOM.sendMessageToServer(global.urlSit);
				}
				else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
			}
		});
		
		btn_Joy_Stand.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				if (isConnectionActive())
				{
					
					//_roboCOM.sendMessageToServer(global.urlStand);
					_roboCOM.sendMessageToWeb(global.urlRunBehavior,"getup");		
		
				}
				else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
			}
		});
		
		btn_Joy_Leftkick.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlForwardLeftKick);
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
				}
			});
		
		btn_Joy_Rightkick.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlForwardRightKick);
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
				}
			});
		
		btn_Joy_Leftbackkick.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlBackLeftKick);
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
				}
			});
		
		btn_Joy_Rightbackkick.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlBackRightKick);
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
				}
			});
		
		
		joystickL.setOnJoystickMoveListener(new OnJoystickMoveListener() {

			@Override
			public void onValueChanged(int angle, int power, int direction) {
				//angleTemp = angle;
				//powerTemp = power;
				//angleTextView.setText(" " + String.valueOf(angle) + "°");
				//powerTextView.setText(" " + String.valueOf(power) + "%");
				switch (direction) {
				case JoystickView.FRONT:
					Log.e(TAG, "L FRONT");
					if (isConnectionActive()) _roboCOM.sendMessageToServer(global.urlHeadUp);
					break;
				case JoystickView.FRONT_RIGHT:
					Log.e(TAG, "L FRONT_RIGHT");
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer("movehead 50 -35 0.5");
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
					break;
				case JoystickView.RIGHT:
					Log.e(TAG, "L RIGHT");
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlHeadLeft);
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
					break;
				case JoystickView.RIGHT_BOTTOM:
					Log.e(TAG, "L RIGHT_BOTTOM");
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer("movehead 50 28 0.5");
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
					break;
				case JoystickView.BOTTOM:
					Log.e(TAG, "L BOTTOM");
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlHeadDown);
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
					break;
				case JoystickView.BOTTOM_LEFT:
					Log.e(TAG, "L BOTTOM_LEFT");
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer("movehead -50 28 0.5");
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
					break;
				case JoystickView.LEFT:
					Log.e(TAG, "L LEFT");
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlHeadRight);
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
					break;
				case JoystickView.LEFT_FRONT:
					Log.e(TAG, "L LEFT_FRONT");
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer("movehead -50 -35 0.5");
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
					break;
				default:
					Log.e(TAG, "L STOP");
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlHeadCenter);
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
				}
			}
		}, JoystickView.DEFAULT_LOOP_INTERVAL);

		joystickR.setOnJoystickMoveListener(new OnJoystickMoveListener() {

			@Override
			public void onValueChanged(int angle, int power, int direction) {
				//angleTemp = angle;
				//powerTemp = power;
				//angleTextView.setText(" " + String.valueOf(angle) + "°");
				//powerTextView.setText(" " + String.valueOf(power) + "%");
				switch (direction) {
				case JoystickView.FRONT:
					Log.e(TAG, "R FRONT");
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlForward);
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
					break;
				case JoystickView.FRONT_RIGHT:
					Log.e(TAG, "R FRONT_RIGHT");
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlTurnLeft);
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
					break;
				case JoystickView.RIGHT:
					Log.e(TAG, "R RIGHT");
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlLeft);
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
					break;
				case JoystickView.RIGHT_BOTTOM:
					Log.e(TAG, "R RIGHT_BOTTOM");
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlTurnRight);
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
					break;
				case JoystickView.BOTTOM:
					Log.e(TAG, "R BOTTOM");
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlBackward);
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
					break;
				case JoystickView.BOTTOM_LEFT:
					Log.e(TAG, "R BOTTOM_LEFT");
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlTurnLeft);
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
					break;
				case JoystickView.LEFT:
					Log.e(TAG, "R LEFT");
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlRight);
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
					break;
				case JoystickView.LEFT_FRONT:
					Log.e(TAG, "R LEFT_FRONT");
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlTurnRight);
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
					break;
				default:
					Log.e(TAG, "R STOP");
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlStopWalk);
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
				}
			}
		}, JoystickView.DEFAULT_LOOP_INTERVAL);
		
	}

	/**
	 * App onStart() (Android framework)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart ()
	{
		Log.d(TAG,"onStart start");
		super.onStart ();   
	}

	/**
	 * Android framework
	 * 
	 * @see android.app.Activity#onRestart()
	 */
	@Override
	protected void onRestart ()
	{
		Log.d(TAG,"onRestart start");
		super.onRestart();
	}

	/**
	 * Android framework
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override 
	protected void onResume ()
	{
		Log.d(TAG,"onResume start");
		startConnection();
		super.onResume();
	}

	/**
	 * Android framework
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause ()
	{
		Log.d(TAG,"onPause start");
		stopConnection (true);
		super.onPause ();
	}

	@Override
	protected void onStop ()
	{
		Log.d(TAG,"onStop start");
		super.onStop();
	}

	@Override
	protected void onDestroy ()
	{
		Log.d(TAG,"onDestroy start");
		super.onDestroy();
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
				//tv_status.setText((String)msg.obj);
				Log.e(TAG,"Status Message JOYS : " + (String)msg.obj);
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
		//tv_btStopAndGo.setEnabled(isConnectionActive());
		Log.e(TAG, "Is conection Active JOYS : " + isConnectionActive());

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
				//tv_connStatus.setText(used.getConnStatusStr());
				Log.e(TAG,"Connections Status JOYS : " + used.getConnStatusStr());
			}
		}
	}

	/**
	 * Refresh all UI controls.
	 */
	public void UI_Refresh ()
	{
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
