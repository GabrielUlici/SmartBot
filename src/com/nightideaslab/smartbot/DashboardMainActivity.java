package com.nightideaslab.smartbot;

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class DashboardMainActivity extends FragmentActivity
{
	public final static String TAG = "**SB DashboardMain**";
	
	public static boolean backtwice = false;  //this is a flag
	private boolean doubleBackToExitPressedOnce = false; //this is a flag
	
	public static String address, robot, port;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Setting a background image to the action bar
      	final ActionBar actionBar = getActionBar();
      	Drawable background =getResources().getDrawable(R.drawable.w_top_bar);  
      	actionBar.setBackgroundDrawable(background);
             	
      	address = this.getIntent().getStringExtra("EXTRA_ADDRESS");
 		robot = this.getIntent().getStringExtra("EXTRA_ROBOT");
 		port = this.getIntent().getStringExtra("EXTRA_PORT");

 		System.out.println("DASH Address : " + address);
 		System.out.println("DASH Robot : " + robot);
 		System.out.println("DASH Port : " + port);

        Log.d(TAG,"DashBoardMainActivity()");
        setContentView(R.layout.dashboard_main_layout);
          	
    }
    
    /**
     * Android framework
     * 
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
        this.doubleBackToExitPressedOnce = false;
    }
    
	/**
	 * Closes the connection with the robot only after 2 pressed on the back key  
	 * 	the first time will display a notification and the second time will 
	 * 		disconnect. In this way we prevent accidental disconnections.
	 */
    @Override
    public void onBackPressed()
    {
        if (doubleBackToExitPressedOnce)
        {
            super.onBackPressed();
            Toast.makeText(this, "Disconnected from the robot", Toast.LENGTH_SHORT).show();
            return;
        }
        
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to disconect from the robot", Toast.LENGTH_SHORT).show();
        
        /**
         *  Reseting the variable doubleBackToExitPressedOnce after 5 seconds
         */
        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
            	doubleBackToExitPressedOnce=false;   
            }
        }, 5000); 
    } 
        
	/**
	 * Menu Creation
	 * 
	 * @param line
	 *            String to be displayed on debugger
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.dashboard_menu, menu);
		return super.onCreateOptionsMenu(menu); 
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.menu_dashboard_disconnect:
			// TODO add the disconnect function
			Toast.makeText(getApplicationContext(), "Disconnected from the robot",Toast.LENGTH_SHORT).show();
			finish();
			return true;
			
		case R.id.menu_dashboard_settings:
			Intent shell = new Intent(DashboardMainActivity.this, Preferences.class);
			startActivity(shell);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Android Framework: called when menu is closed
	 */
	@Override
	public void onOptionsMenuClosed (Menu menu)
	{
	}
}
