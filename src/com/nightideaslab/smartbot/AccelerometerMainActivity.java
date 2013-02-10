package com.nightideaslab.smartbot;

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class AccelerometerMainActivity extends FragmentActivity{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accelerometer_main_layout);
        
     // Setting a background image to the action bar
      	final ActionBar actionBar = getActionBar();
      	Drawable background =getResources().getDrawable(R.drawable.w_top_bar); 
      	actionBar.setBackgroundDrawable(background);
    }
    
//    /**
//	 * Menu Creation
//	 * 
//	 * @param line
//	 *            String to be displayed on debugger
//	 */
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu)
//	{
//		getMenuInflater().inflate(R.menu.accelerometer_menu, menu);
//		return super.onCreateOptionsMenu(menu); 
//	}
//
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item)
//	{
//		switch (item.getItemId())
//		{
//		case R.id.menu_accelerometer_debug:
//			Toast.makeText(getApplicationContext(), "Touch the debug menu",Toast.LENGTH_SHORT).show();
//			return true;
//			
//		case R.id.menu_accelerometer_normal:
//			
//			Toast.makeText(getApplicationContext(), "Touch the normal menu",Toast.LENGTH_SHORT).show();
//		}
//		return super.onOptionsItemSelected(item);
//	}
//	
//	/**
//	 * Android Framework: called when menu is closed
//	 */
//	@Override
//	public void onOptionsMenuClosed (Menu menu)
//	{
//	}
//  
}
