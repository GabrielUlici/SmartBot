package com.nightideaslab.smartbot;

import com.nightideaslab.util.Globals;
import com.nightideaslab.util.TCPClient;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class ButtonsMovementsActivity extends Activity
				implements android.os.Handler.Callback
{
	public static final int MSG_UIREFRESHCOM   	= 10000;		/** UI: Refresh Connection Status */
	public static final int MSG_CENTER			= 10003;		/** UI: touch on robot icon */
	public static final int MSG_NEWSTATUSMESSAGE= 10005;		/** UI: update application status line */
	public static final int MSG_STATUSMSG		= 10010;		/** Change status message */

	public final static String TAG = "**SB Buttons Behavior**";

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
	
	Button btn_BtnMove_TurnLeft, btn_BtnMove_Forward,
			btn_BtnMove_TurnRight, btn_BtnMove_Left,
			btn_BtnMove_Backward, btn_BtnMove_Right,
			btn_BtnMove_SitDown, btn_BtnMove_StopWalk,
			btn_BtnMove_Stand, btn_BtnMove_GetUpFaceUp,
			btn_BtnMove_GetUpFaceDown;
	
	/**
	 * Constructor
	 * 
	 * Initialize members and load configuration from file.
	 */
	public ButtonsMovementsActivity()
	{
		Log.d(TAG,"ButtonsMovementsActivity()");

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
			_roboCOM.msgHello="Start Connection Buttons Move";

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
			_roboCOM.sendMessageToServer("End Connection Buttons Move");
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
		
    public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.context=getApplicationContext();

		cm = (ConnectivityManager) getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        setContentView(R.layout.buttons_movements_layout);
        Log.d(TAG,"Creating the Buttons Movement layout..");
    	
     	// Start Connecting with the robot
     	startConnection();
     	
     	btn_BtnMove_TurnLeft = (Button) findViewById(R.id.btn_btnmove_turnleft);
     	btn_BtnMove_Forward = (Button) findViewById(R.id.btn_btnmove_forward);
		btn_BtnMove_TurnRight = (Button) findViewById(R.id.btn_btnmove_turnright);
		btn_BtnMove_Left = (Button) findViewById(R.id.btn_btnmove_left);
		btn_BtnMove_Backward = (Button) findViewById(R.id.btn_btnmove_backward);
		btn_BtnMove_Right = (Button) findViewById(R.id.btn_btnmove_right);
		btn_BtnMove_SitDown = (Button) findViewById(R.id.btn_btnmove_sit);
		btn_BtnMove_StopWalk = (Button) findViewById(R.id.btn_btnmove_stopwalk);
		btn_BtnMove_Stand = (Button) findViewById(R.id.btn_btnmove_stand);
		btn_BtnMove_GetUpFaceUp = (Button) findViewById(R.id.btn_btnmove_getupfaceup);
		btn_BtnMove_GetUpFaceDown = (Button) findViewById(R.id.btn_btnmove_getupfacedown);
     	 
      	btn_BtnMove_TurnLeft.setOnClickListener(new View.OnClickListener()
    		{
    			@Override
    			public void onClick(View view)
    			{
    				if (isConnectionActive())
    				{
    					_roboCOM.sendMessageToServer(global.urlTurnLeft);
    				}
    				else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
    			}
    		});
     	
     	btn_BtnMove_Forward.setOnClickListener(new View.OnClickListener()
    		{
    			@Override
    			public void onClick(View view)
    			{
    				if (isConnectionActive())
    				{
    					_roboCOM.sendMessageToServer(global.urlForward);
    				}
    				else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
    			}
    		});
     	
		btn_BtnMove_TurnRight.setOnClickListener(new View.OnClickListener()
    		{
    			@Override
    			public void onClick(View view)
    			{
    				if (isConnectionActive())
    				{
    					_roboCOM.sendMessageToServer(global.urlTurnRight);
    				}
    				else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
    			}
    		});
     	
		btn_BtnMove_Left.setOnClickListener(new View.OnClickListener()
    		{
    			@Override
    			public void onClick(View view)
    			{
    				if (isConnectionActive())
    				{
    					_roboCOM.sendMessageToServer(global.urlLeft);
    				}
    				else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
    			}
    		});
     	
		btn_BtnMove_Backward.setOnClickListener(new View.OnClickListener()
    		{
    			@Override
    			public void onClick(View view)
    			{
    				if (isConnectionActive())
    				{
    					_roboCOM.sendMessageToServer(global.urlBackward);
    				}
    				else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
    			}
    		});
     	
		btn_BtnMove_Right.setOnClickListener(new View.OnClickListener()
    		{
    			@Override
    			public void onClick(View view)
    			{
    				if (isConnectionActive())
    				{
    					_roboCOM.sendMessageToServer(global.urlRight);
    				}
    				else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
    			}
    		});
     	
		btn_BtnMove_SitDown.setOnClickListener(new View.OnClickListener()
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
     	
		btn_BtnMove_StopWalk.setOnClickListener(new View.OnClickListener()
    		{
    			@Override
    			public void onClick(View view)
    			{
    				if (isConnectionActive())
    				{
    					_roboCOM.sendMessageToServer(global.urlStopWalk);
    				}
    				else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
    			}
    		});
     	
		btn_BtnMove_Stand.setOnClickListener(new View.OnClickListener()
    		{
    			@Override
    			public void onClick(View view)
    			{
    				if (isConnectionActive())
    				{
    					_roboCOM.sendMessageToServer(global.urlStand);
    				}
    				else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
    			}
    		});
     	
		btn_BtnMove_GetUpFaceUp.setOnClickListener(new View.OnClickListener()
    		{
    			@Override
    			public void onClick(View view)
    			{
    				if (isConnectionActive())
    				{
    					_roboCOM.sendMessageToServer(global.urlGetUpFaceUp);
    				}
    				else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
    			}
    		});
     	
		btn_BtnMove_GetUpFaceDown.setOnClickListener(new View.OnClickListener()
    		{
    			@Override
    			public void onClick(View view)
    			{
    				if (isConnectionActive())
    				{
    					_roboCOM.sendMessageToServer(global.urlGetUpFaceDown);
    				}
    				else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot",Toast.LENGTH_SHORT).show();
    			}
    		});
     	     	
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
				Log.e(TAG,"Status Message Buttons Move : " + (String)msg.obj);
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
		Log.e(TAG, "Is conection Active Buttons Move : " + isConnectionActive());

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
				Log.e(TAG,"Connections Status Buttons Move : " + used.getConnStatusStr());
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