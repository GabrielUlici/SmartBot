package com.nightideaslab.smartbot;

import android.app.ActionBar;
import android.app.TabActivity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

@SuppressWarnings("deprecation")
public class ButtonsActivity extends TabActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        // Setting a background image to the action bar
      	final ActionBar actionBar = getActionBar();
      	Drawable background =getResources().getDrawable(R.drawable.w_top_bar); 
      	actionBar.setBackgroundDrawable(background);
        
        setContentView(R.layout.buttons_layout);
        
        TabHost tabHost = getTabHost();
        
        // Tab for Movements
        TabSpec movementspec = tabHost.newTabSpec("Movements");
        movementspec.setIndicator("Movem-ents", getResources().getDrawable(R.drawable.icon_movements_tab));
        Intent photosIntent = new Intent(this, ButtonsMovementsActivity.class);
        movementspec.setContent(photosIntent);
        
        // Tab for Soccer
        TabSpec soccerspec = tabHost.newTabSpec("Soccer");
        soccerspec.setIndicator("Soccer", getResources().getDrawable(R.drawable.icon_soccer_tab));
        Intent songsIntent = new Intent(this, ButtonsSoccerActivity.class);
        soccerspec.setContent(songsIntent);
        
        // Tab for Body Control
        TabSpec bodycontrollspec = tabHost.newTabSpec("Body Control");
        bodycontrollspec.setIndicator("Body Control", getResources().getDrawable(R.drawable.icon_body_control_tab));
        Intent videosIntent = new Intent(this, ButtonsBodyControlActivity.class);
        bodycontrollspec.setContent(videosIntent);
        
        // Tab for Behaviors
        TabSpec behaviorspec = tabHost.newTabSpec("Behavior");
        behaviorspec.setIndicator("Behavior", getResources().getDrawable(R.drawable.icon_behavior_tab));
        Intent behaviorIntent = new Intent(this, ButtonsBehaviorsActivity.class);
        behaviorspec.setContent(behaviorIntent);
        
        // Adding all TabSpec to TabHost
        tabHost.addTab(movementspec); // Adding photos tab
        tabHost.addTab(soccerspec); // Adding songs tab
        tabHost.addTab(bodycontrollspec); // Adding videos tab
        tabHost.addTab(behaviorspec); // Adding videos tab
                
    }
}