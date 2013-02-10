package com.nightideaslab.smartbot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;

public class DashboardActivity extends Fragment
{
	
	public final static String TAG = "**SB Dashboard**";
	
	private Context context;
	public Context getAppContext()	{ return context; }
	
	ConnectivityManager cm;										/** Check connection status */
	
	Button btn_Dash_Speech, btn_Dash_Buttons, btn_Dash_Joystick, btn_Dash_Accelerometer, btn_Dash_Terminal, btn_Dash_Behaviors;
	
   // @Override
    //public void onCreate(Bundle savedInstanceState)
    //{
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		 
		super.onCreate(savedInstanceState);
		ViewGroup rootDash = (ViewGroup) inflater.inflate(R.layout.dashboard_layout, null);
    	
		final Activity activity = getActivity();
    	 
        Log.d(TAG,"Creating the DashBoard layout..");
        
        final Vibrator v = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
        		
		// Start Connecting with the robot
		//startConnection();
                
        /**
         * Creating all buttons instances
         * */

        btn_Dash_Speech = (Button) rootDash.findViewById(R.id.btn_dash_speech);
        btn_Dash_Buttons = (Button) rootDash.findViewById(R.id.btn_dash_buttons);
        btn_Dash_Joystick = (Button) rootDash.findViewById(R.id.btn_dash_joystick);
        btn_Dash_Accelerometer = (Button) rootDash.findViewById(R.id.btn_dash_accelerometer);
        //btn_Dash_Terminal = (Button) rootDash.findViewById(R.id.btn_dash_terminal);
        //btn_Dash_Behaviors = (Button) findViewById(R.id.btn_dash_behaviors);
        
        /**
         * Handling all button click events
         * */
        
        btn_Dash_Speech.setOnClickListener(new View.OnClickListener()
        {
			@Override
			public void onClick(View view)
			{
				Intent i = new Intent(activity.getApplicationContext(), SpeechActivity.class);
				startActivity(i);
				v.vibrate(50);
			}
		});
        
        btn_Dash_Buttons.setOnClickListener(new View.OnClickListener()
        {
			@Override
			public void onClick(View view)
			{
				Intent i = new Intent(activity.getApplicationContext(), ButtonsActivity.class);
				startActivity(i);
				v.vibrate(50);
			}
		});
        
        btn_Dash_Joystick.setOnClickListener(new View.OnClickListener()
        {
			@Override
			public void onClick(View view)
			{
				Intent i = new Intent(activity.getApplicationContext(), JoystickActivity.class);
				startActivity(i);
				v.vibrate(50);
			}
		});
        
        btn_Dash_Accelerometer.setOnClickListener(new View.OnClickListener()
        {
			@Override
			public void onClick(View view)
			{
				Intent i = new Intent(activity.getApplicationContext(), AccelerometerMainActivity.class);
				startActivity(i);
				v.vibrate(50);
			}
		});
        
//        btn_Dash_Terminal.setOnClickListener(new View.OnClickListener()
//        {
//			@Override
//			public void onClick(View view)
//			{
//				Intent i = new Intent(activity.getApplicationContext(), AccelerometerMainActivity.class);
//				startActivity(i);
//        		v.vibrate(50);
//			}
//		});

//        btn_Dash_Behaviors.setOnClickListener(new View.OnClickListener()
//        {
//			@Override
//			public void onClick(View view)
//			{
//				Intent i = new Intent(activity.getApplicationContext(), BehaviorsActivity.class);
//				startActivity(i);
//        		v.vibrate(50);
//			}
//		});
        
        return rootDash;
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
     * @see android.app.Activity#onRestart()
     */
//    @Override
//    public void onRestart ()
//    {
//    	Log.d(TAG,"onRestart start");
//		super.onRestart();
//    }

    /**
     * Android framework
     * 
     * @see android.app.Activity#onResume()
     */
   	@Override 
   	public void onResume ()
   	{
   		Log.d(TAG,"onResume start");
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
   		super.onPause ();
   	}
   
   	@Override
   	public void onStop ()
   	{
   		Log.d(TAG,"onStop start");
   		super.onStop();
   	}

   	@Override
   	public void onDestroy ()
   	{
   		Log.d(TAG,"onDestroy start");
   		super.onDestroy();
   	}
	
}