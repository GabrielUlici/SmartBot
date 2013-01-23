package com.nightideaslab.smartbot;

import java.util.ArrayList;

import com.nightideaslab.util.Globals;
import com.nightideaslab.util.TCPClient;

//import edu.stanford.*;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class SpeechActivity extends Activity
implements android.os.Handler.Callback
{
	public static final int MSG_UIREFRESHCOM   	= 10000;		/** UI: Refresh Connection Status */
	public static final int MSG_CENTER			= 10003;		/** UI: touch on robot icon */
	public static final int MSG_NEWSTATUSMESSAGE= 10005;		/** UI: update application status line */
	public static final int MSG_STATUSMSG		= 10010;		/** Change status message */

	protected static final int RESULT_SPEECH = 1;

	public final static String TAG = "**SB Speech**";
	
	long last_sent_ms;

	String last_sent_msg;										/** last message sent to server */

	private android.os.Handler MsgQueue;						/** Async Message queue */

	boolean _bbCanSend;											/** TRUE = robot connect, can send data */
	private TCPClient _roboCOM;									/** Connection Thread */
	private Globals global;

	public static String address, robot, port;

	int uitoken;												/** auto-inc number used to connect tcp and UI */

	int subject;
	
	String resultSpeech;
	
	private Context context;
	public Context getAppContext()	{ return context; }

	ConnectivityManager cm;										/** Check connection status */
		
	Button btn_Speech_Speak;

	/**
	 * Constructor
	 * 
	 * Initialize members and load configuration from file.
	 */
	public SpeechActivity()
	{
		Log.d(TAG,"SpeechActivity()");

		setCOMStatus (null,false);

		global = new Globals();
		MsgQueue = new android.os.Handler (this);
	
		subject =  0;
		last_sent_ms=0;
		uitoken=0;
	}
	
//	public void appendToFile(String text)
//		{       
//		   File logFile = new File("sdcard/SmartBot.txt");
//		   if (!logFile.exists())
//		   {
//		      try
//		      {
//		         logFile.createNewFile();
//		      } 
//		      catch (IOException e)
//		      {
//		         e.printStackTrace();
//		      }
//		   }
//		   try
//		   {
//		      //BufferedWriter for performance, true to set append to file flag
//		      BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
//		      buf.append(text);
//		      buf.newLine();
//		      buf.close();
//		   }
//		   catch (IOException e)
//		   {
//		      e.printStackTrace();
//		   }
//		}
	
	/**
	 * Closes the connection with the robot because if it is done  
	 * 	in onResume it will kill the connection when we press the 
	 * 		speech button and it will not work.
	 */
	public void onBackPressed()
	{
		Log.e(TAG,"BACK KEY PRESSED");
		stopConnection (true);
		finish();
		return;
	}

	/**
	 * Async change connection status
	 * 
	 * @param conn			Connection object, referring to a thread
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
			_roboCOM.msgHello="Start Connection Speech";

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
			_roboCOM.sendMessageToServer("End Connection Speech");
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
		
		// Setting a background image to the action bar
		final ActionBar actionBar = getActionBar();
		Drawable background =getResources().getDrawable(R.drawable.w_top_bar);
		actionBar.setBackgroundDrawable(background);		
		
		cm = (ConnectivityManager) getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);

		Log.d(TAG,"Creating the Speech layout..");
	
		setContentView(R.layout.speech_layout);
		
		// Start Connecting with the robot
		startConnection();
		
		// TODO when the orientation changes do a stop connect and start connect
		int ot = getResources().getConfiguration().orientation;
		System.out.println("Orientation : " + ot );
		switch(ot)
		        {

		        case  Configuration.ORIENTATION_LANDSCAPE:
		            Log.d("my orient" ,"ORIENTATION_LANDSCAPE");
		        break;
		        case Configuration.ORIENTATION_PORTRAIT:
		            Log.d("my orient" ,"ORIENTATION_PORTRAIT");
		            break;

//		        case Configuration.ORIENTATION_SQUARE:
//		            Log.d("my orient" ,"ORIENTATION_SQUARE");
//		            break;
		        case Configuration.ORIENTATION_UNDEFINED:
		            Log.d("my orient" ,"ORIENTATION_UNDEFINED");
		            break;
		            default:
		            Log.d("my orient", "default val");
		            break;
		        }

		btn_Speech_Speak = (Button) findViewById(R.id.btn_speech_speech);
		
		btn_Speech_Speak.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

				intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
									RecognizerIntent.LANGUAGE_MODEL_FREE_FORM );
				intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell Nao what to do.");			
				try
				{
					startActivityForResult(intent, RESULT_SPEECH);
				}
				catch (ActivityNotFoundException a)
				{
					// Speech to Text is not present
					showAlertDialog(SpeechActivity.this, "No Speech to Text",
							"Ops! Your device doesn't support Speech to Text.", false);
				}
			}
		});		
	}

	/**
	 * Function to display simple Alert Dialog
	 * @param context - application context
	 * @param title - alert dialog title
	 * @param message - alert message
	 * @param status - success/failure (used to set icon)
	 * */
	@SuppressWarnings ( "deprecation" )
    public void showAlertDialog(Context context, String title, String message, Boolean status)
	{
		AlertDialog alertDialog = new AlertDialog.Builder(context).create();

		// Setting Dialog Title
		alertDialog.setTitle(title);

		// Setting Dialog Message
		alertDialog.setMessage(message);
		
		// Setting alert dialog icon
		alertDialog.setIcon((status) ? R.drawable.success : R.drawable.fail);

		// Setting OK Button
		alertDialog.setButton("OK", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which) {}
		});

		// Showing Alert Message
		alertDialog.show();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode)
		{
		case RESULT_SPEECH:
		{
			if (resultCode == RESULT_OK && null != data)
			{
				ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				
				//sending the array to be checked.
				speechCheck(text);	
			}
			break;
		}
		}
	}

	/**
	 * Checks the ArrayList returned by the Speech recognizer
	 * 		for the commands that we can send to the robot
	 * 
	 * @param text The ArrayList from RESULT_SPEECH
	 */
	public void speechCheck (ArrayList<String> text)
	{
		System.out.println("The array list : " + text);
		
		//TODO Create a better parser for the array list
		
		int index = 0;
		// when this is set to false then it will stop searching for another command
		boolean ok = true;
		// to see if we send a message 
		boolean msg_sent = false; 

		while ((index<text.size()) && (ok))
		{
			resultSpeech = text.get(index);

			if (resultSpeech!=null)
			{
				if (resultSpeech.equalsIgnoreCase("turn left") || resultSpeech.equalsIgnoreCase("turnleft") || resultSpeech.equalsIgnoreCase("turn to your left"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlTurnLeft);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("forward") || resultSpeech.equalsIgnoreCase("move forward") || resultSpeech.equalsIgnoreCase("go forward"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlForward);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("turn right") || resultSpeech.equalsIgnoreCase("turnright") || resultSpeech.equalsIgnoreCase("turn to your right"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlTurnRight);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("left") || resultSpeech.equalsIgnoreCase("move left") || resultSpeech.equalsIgnoreCase("go left") )
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlLeft);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("backwards") || resultSpeech.equalsIgnoreCase("move back") || resultSpeech.equalsIgnoreCase("move backwards") || resultSpeech.equalsIgnoreCase("go back") || resultSpeech.equalsIgnoreCase("go backwards"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlBackward);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("right") || resultSpeech.equalsIgnoreCase("move right") || resultSpeech.equalsIgnoreCase("go right"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlRight);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("start walk") || resultSpeech.equalsIgnoreCase("walk"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlStartWalk);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("stop walk") || resultSpeech.equalsIgnoreCase("stop"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlStopWalk);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("stand") || resultSpeech.equalsIgnoreCase("stand up"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlStand);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("sit") || resultSpeech.equalsIgnoreCase("sit down"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlSit);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("stand down") || resultSpeech.equalsIgnoreCase("standdown"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlSitDown);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("getup") || resultSpeech.equalsIgnoreCase("get up"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlGetUp);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("getup faceup") || resultSpeech.equalsIgnoreCase("get up face up"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlGetUpFaceUp);
						ok = false;
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("getup facedown") || resultSpeech.equalsIgnoreCase("get up face down"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlGetUpFaceDown);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("forward left kick") || resultSpeech.equalsIgnoreCase("forwardleftkick") || resultSpeech.equalsIgnoreCase("leftkick") || resultSpeech.equalsIgnoreCase("left kick") || resultSpeech.equalsIgnoreCase("kick with your left") || resultSpeech.equalsIgnoreCase("kik with your left") || resultSpeech.equalsIgnoreCase("kick with your left foot") || resultSpeech.equalsIgnoreCase("kik with your left foot") || resultSpeech.equalsIgnoreCase("kick with your left leg") || resultSpeech.equalsIgnoreCase("kik with your left leg"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlStand);
						_roboCOM.sendMessageToServer(global.urlForwardLeftKick);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("kick") || resultSpeech.equalsIgnoreCase("kik") || resultSpeech.equalsIgnoreCase("kick with your foot") || resultSpeech.equalsIgnoreCase("kik with your foot") || resultSpeech.equalsIgnoreCase("kick with your leg") || resultSpeech.equalsIgnoreCase("kik with your leg"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlKickForUs);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("forward right kick") || resultSpeech.equalsIgnoreCase("forwardrightkick") || resultSpeech.equalsIgnoreCase("rightkick") || resultSpeech.equalsIgnoreCase("right kick") || resultSpeech.equalsIgnoreCase("kick with your right") || resultSpeech.equalsIgnoreCase("kik with your right") || resultSpeech.equalsIgnoreCase("kick with your right foot") || resultSpeech.equalsIgnoreCase("kik with your right foot") || resultSpeech.equalsIgnoreCase("kick with your right leg") || resultSpeech.equalsIgnoreCase("kik with your right leg"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlStand);
						_roboCOM.sendMessageToServer(global.urlForwardRightKick);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("side left kick") || resultSpeech.equalsIgnoreCase("side left kick"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlStand);
						_roboCOM.sendMessageToServer(global.urlSideLeftKick);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("side right kick") || resultSpeech.equalsIgnoreCase("siderightkick"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlStand);
						_roboCOM.sendMessageToServer(global.urlSideRightKick);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("save left") || resultSpeech.equalsIgnoreCase("saveleft"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlStand);
						_roboCOM.sendMessageToServer(global.urlSaveLeft);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("save right") || resultSpeech.equalsIgnoreCase("saveright"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlStand);
						_roboCOM.sendMessageToServer(global.urlSaveRight);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("back left kick") || resultSpeech.equalsIgnoreCase("backleftkick"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlStand);
						_roboCOM.sendMessageToServer(global.urlBackLeftKick);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("back right kick") || resultSpeech.equalsIgnoreCase("backrightkick"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlStand);
						_roboCOM.sendMessageToServer(global.urlBackRightKick);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("move head center") || resultSpeech.equalsIgnoreCase("head center") || resultSpeech.equalsIgnoreCase("move your head to center"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer("setstiffness HeadYaw 1");
						_roboCOM.sendMessageToServer("setstiffness HeadPitch 1");
						_roboCOM.sendMessageToServer(global.urlHeadCenter);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("move head left") || resultSpeech.equalsIgnoreCase("head left") || resultSpeech.equalsIgnoreCase("move your head to left"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer("setstiffness HeadYaw 1");
						_roboCOM.sendMessageToServer("setstiffness HeadPitch 1");
						_roboCOM.sendMessageToServer(global.urlHeadLeft);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("move head right") || resultSpeech.equalsIgnoreCase("head right") || resultSpeech.equalsIgnoreCase("move your head to right"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer("setstiffness HeadYaw 1");
						_roboCOM.sendMessageToServer("setstiffness HeadPitch 1");
						_roboCOM.sendMessageToServer(global.urlHeadRight);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("move head up") || resultSpeech.equalsIgnoreCase("head up") || resultSpeech.equalsIgnoreCase("move your head up"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer("setstiffness HeadYaw 1");
						_roboCOM.sendMessageToServer("setstiffness HeadPitch 1");
						_roboCOM.sendMessageToServer(global.urlHeadUp);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}

				if (resultSpeech.equalsIgnoreCase("move head down") || resultSpeech.equalsIgnoreCase("head down") || resultSpeech.equalsIgnoreCase("move your head down"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer("setstiffness HeadYaw 1");
						_roboCOM.sendMessageToServer("setstiffness HeadPitch 1");
						_roboCOM.sendMessageToServer(global.urlHeadDown);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("hands up") || resultSpeech.equalsIgnoreCase("handsup") || resultSpeech.equalsIgnoreCase("move hands up") || resultSpeech.equalsIgnoreCase("raise your hands"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlHandsUp);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("hands down") || resultSpeech.equalsIgnoreCase("handsdown") || resultSpeech.equalsIgnoreCase("move hands down") || resultSpeech.equalsIgnoreCase("lower your hands"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlHandsDown);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("do you know to dance") || resultSpeech.equalsIgnoreCase("do you dance") || resultSpeech.equalsIgnoreCase("do you know how to dance"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlDance);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("dance") || resultSpeech.equalsIgnoreCase("nao dance") || resultSpeech.equalsIgnoreCase("dance for us"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlDanceForUs);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("hello") || resultSpeech.equalsIgnoreCase("hello nao"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlHello);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("story") || resultSpeech.equalsIgnoreCase("tell me a story") || resultSpeech.equalsIgnoreCase("tell us a story")  || resultSpeech.equalsIgnoreCase("do you know any stories"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlStory);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("eyes") || resultSpeech.equalsIgnoreCase("eye animation") || resultSpeech.equalsIgnoreCase("blink your eyes") || resultSpeech.equalsIgnoreCase("eyes animation"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlEyes);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("hand up") || resultSpeech.equalsIgnoreCase("handup") || resultSpeech.equalsIgnoreCase("move hand up") || resultSpeech.equalsIgnoreCase("raise your hand"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlHandUp);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("right hand up") || resultSpeech.equalsIgnoreCase("raise your right hand") || resultSpeech.equalsIgnoreCase("raise your right arm") || resultSpeech.equalsIgnoreCase("right arm up"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlRightHandUp);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("left hand up") || resultSpeech.equalsIgnoreCase("raise your left hand") || resultSpeech.equalsIgnoreCase("raise your left arm") || resultSpeech.equalsIgnoreCase("left arm up"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlLeftHandUp);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("left hand down") || resultSpeech.equalsIgnoreCase("lower your left hand") || resultSpeech.equalsIgnoreCase("lower your left arm") || resultSpeech.equalsIgnoreCase("left arm down"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlLeftHandDown);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("right hand down") || resultSpeech.equalsIgnoreCase("lower your right hand") || resultSpeech.equalsIgnoreCase("lower your right arm") || resultSpeech.equalsIgnoreCase("right arm down"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlRightHandDown);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("3 musketeers") || resultSpeech.equalsIgnoreCase("3 musketeer") || resultSpeech.equalsIgnoreCase("3 musketeers story") || resultSpeech.equalsIgnoreCase("tell me the 3 musketeers story") || resultSpeech.equalsIgnoreCase("tell us the 3 musketeers story") || resultSpeech.equalsIgnoreCase("tell the 3 musketeers story") || resultSpeech.equalsIgnoreCase("three musketeers") || resultSpeech.equalsIgnoreCase("three musketeer") || resultSpeech.equalsIgnoreCase("three musketeers story") || resultSpeech.equalsIgnoreCase("tell me the three musketeers story") || resultSpeech.equalsIgnoreCase("tell us the three musketeers story") || resultSpeech.equalsIgnoreCase("tell the three musketeers story"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.url3MusketeersStory);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("star wars") || resultSpeech.equalsIgnoreCase("starwars") || resultSpeech.equalsIgnoreCase("star wars story") || resultSpeech.equalsIgnoreCase("tell me the star wars story") || resultSpeech.equalsIgnoreCase("tell us the star wars story") || resultSpeech.equalsIgnoreCase("tell the star wars story") || resultSpeech.equalsIgnoreCase("starwars story") || resultSpeech.equalsIgnoreCase("tell me the starwars story") || resultSpeech.equalsIgnoreCase("tell us the starwars story") || resultSpeech.equalsIgnoreCase("tell the starwars story"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlStarWarsStory);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("caravan") || resultSpeech.equalsIgnoreCase("caravan dance")  || resultSpeech.equalsIgnoreCase("do the caravan dance"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlCaravanPalaceDance);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("taichi") || resultSpeech.equalsIgnoreCase("taichi dance")  || resultSpeech.equalsIgnoreCase("do the taichi dance") || resultSpeech.equalsIgnoreCase("tai chi") || resultSpeech.equalsIgnoreCase("tai chi dance")  || resultSpeech.equalsIgnoreCase("do the tai chi dance"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlTaiChiDance);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("thriller") || resultSpeech.equalsIgnoreCase("thriller dance")  || resultSpeech.equalsIgnoreCase("do the thriller dance"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlThrillerDance);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("vangelis") || resultSpeech.equalsIgnoreCase("vangelis dance")  || resultSpeech.equalsIgnoreCase("do the vangelis dance"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlVangelisDance);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("presentation") || resultSpeech.equalsIgnoreCase("present your self")  || resultSpeech.equalsIgnoreCase("present yourself") || resultSpeech.equalsIgnoreCase("nao presentation") || resultSpeech.equalsIgnoreCase("nao present your self")  || resultSpeech.equalsIgnoreCase("nao present yourself")  || resultSpeech.equalsIgnoreCase("can you present present yourself")   || resultSpeech.equalsIgnoreCase("nao can you present yourself"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlPresentation);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("follow me") || resultSpeech.equalsIgnoreCase("nao follow me")  || resultSpeech.equalsIgnoreCase("nao can you follow me")  || resultSpeech.equalsIgnoreCase("can you follow me"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlFollowMe);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("go and rest") || resultSpeech.equalsIgnoreCase("nao go and rest")  || resultSpeech.equalsIgnoreCase("nao rest")  || resultSpeech.equalsIgnoreCase("rest")  || resultSpeech.equalsIgnoreCase("go to rest"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlGoAndRest);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("find ball") || resultSpeech.equalsIgnoreCase("find the ball") || resultSpeech.equalsIgnoreCase("find the red ball"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlSendToRAgent + " " + global.urlFindBall);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("go to ball") || resultSpeech.equalsIgnoreCase("goto ball") || resultSpeech.equalsIgnoreCase("go to the red ball") || resultSpeech.equalsIgnoreCase("goto the red ball") || resultSpeech.equalsIgnoreCase("approach the red ball") || resultSpeech.equalsIgnoreCase("approach the ball") || resultSpeech.equalsIgnoreCase("go to the ball") || resultSpeech.equalsIgnoreCase("approach ball"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlSendToRAgent + " " + global.urlGoToBall);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("attack") || resultSpeech.equalsIgnoreCase("attacker") || resultSpeech.equalsIgnoreCase("play") || resultSpeech.equalsIgnoreCase("nao attack") || resultSpeech.equalsIgnoreCase("nao attacker") || resultSpeech.equalsIgnoreCase("nao play soccer") || resultSpeech.equalsIgnoreCase("play soccer"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlSendToRAgent + " " + global.urlAttacker);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("penalized"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlSendToRAgent + " " + global.urlPenalty);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("defend"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlSendToRAgent + " " + global.urlDefend);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
				if (resultSpeech.equalsIgnoreCase("initial"))
				{
					if (isConnectionActive())
					{
						_roboCOM.sendMessageToServer(global.urlSendToRAgent + " " + global.urlInitial);
						ok = false;
						Toast.makeText(getApplicationContext(), "Sent : " + resultSpeech ,Toast.LENGTH_SHORT).show();
						msg_sent = true;
					}
					else Toast.makeText(getApplicationContext(), "Unable to send the command to the robot because we are not connected",Toast.LENGTH_SHORT).show();
				}
				
			}
			else
			{
				Toast.makeText(getApplicationContext(), "An error has occurred",Toast.LENGTH_SHORT).show();	
			}

			index++;
		} //end of the while
		
		if (msg_sent != true && isConnectionActive())
			{
				_roboCOM.sendMessageToServer(global.urlUnknownCommand);				
				Toast.makeText(getApplicationContext(), "No Command Found",Toast.LENGTH_SHORT).show();
			}	
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
		//stopConnection (true); // DO NOT UNCOMMENT THIS LINE BECAUSE 
									//YOU WILL LOOSE THE CONNECTION TO THE ROBOT
		super.onPause ();
	}

	@Override
	protected void onStop ()
	{
		Log.d(TAG,"onStop start");
		super.onStop();
	}

	protected void closeApp ()
	{
		Log.d(TAG,"closeApp start");
		finish ();
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
				Log.e(TAG,"Status Message Speech : " + (String)msg.obj);
				Toast.makeText(getApplicationContext(), (String)msg.obj ,Toast.LENGTH_SHORT).show();
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
		Log.e(TAG, "Is conection Active Speech : " + isConnectionActive());

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
				Log.e(TAG,"Connections Status Speech : " + used.getConnStatusStr());
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
