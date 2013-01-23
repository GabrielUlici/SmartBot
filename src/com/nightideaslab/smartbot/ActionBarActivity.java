package com.nightideaslab.smartbot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ActionBarActivity extends Fragment
{
	public final static String TAG = "**SB ActionBar**";
	
	private Context context;
	public Context getAppContext()	{ return context; }
	
	public String robot, statusBattery;
	
	TextView robotName;
	ImageView battery;
	Handler h;
	
	/**
	 * Constructor
	 * 
	 * Initialize members and load configuration from file.
	 */
	public ActionBarActivity()
	{
		Log.d(TAG,"ActionBarActivity()");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		 
		ViewGroup root = (ViewGroup) inflater.inflate(R.layout.actionbar_layout, null);
		
		robot = DashboardMainActivity.robot;
	    robotName = (TextView) root.findViewById(R.id.robot_Name);
	    battery = (ImageView) root.findViewById (R.id.batteryView);    
	   
	    // Seting the robot name
	    robotName.setText(robot);
	      
	    if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = 
			        new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
			}
	    
	    this.h = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                // process incoming messages here
                switch (msg.what) {
                    case 0:
                     		String text = (String) msg.obj;
                            statusBattery = "" + text.charAt(text.indexOf("<PRE>") + 5);
                    		setBattery();
                           	break;
                }
                super.handleMessage(msg);
            }
        };
        
        checkBattery();
	    	       
		return root;
	}
	
	public void checkBattery()
	{
		String msgs = "http://" + DashboardMainActivity.address + ":9559/?eval=ALSentinel.getBatteryLevel('')" ;

		try	{
 
    		// Perform action on click
        	URL url = new URL(msgs.toString());
            URLConnection conn = url.openConnection();
            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = "";
            while ((line = rd.readLine()) != null) {
        		Message lmsg;
                lmsg = new Message();
                lmsg.obj = line;
                lmsg.what = 0;
                ActionBarActivity.this.h.sendMessage(lmsg);
            }

    	}
    	catch (Exception e)	{
    		Log.e(TAG, "ERRROOORRR WEB : " + e);
    	}
			
	}
	
	public void setBattery()
	{		
		if (statusBattery.equalsIgnoreCase("5")) battery.setImageResource(R.drawable.battery5);
		if (statusBattery.equalsIgnoreCase("4")) battery.setImageResource(R.drawable.battery4);
		if (statusBattery.equalsIgnoreCase("3")) battery.setImageResource(R.drawable.battery3);
		if (statusBattery.equalsIgnoreCase("2")) battery.setImageResource(R.drawable.battery2);
		if (statusBattery.equalsIgnoreCase("1")) battery.setImageResource(R.drawable.battery1);
		if (statusBattery.equalsIgnoreCase("0")) battery.setImageResource(R.drawable.battery0);
	}
	
}
