package com.nightideaslab.smartbot;

import com.nightideaslab.smartbot.R;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.MediaStore;

import android.widget.Toast;


/**
 * Settings activity for SmartBot
 * 
 * @author Gabriel Ulici
 */
public class Preferences extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
	/**
	 * SharedPreference name.
	 */
	static final String SHARED_NAME = "SmartBot";
	static final String PREFS_NAME = "defaultvalue";
	
	/**
	 * Filename of the change log.
	 */
	private static final String FILENAME_CHANGE_LOG = "changelog.html";
	
	/**
	 * Filename of the legal.
	 */
	private static final String FILENAME_LEGAL = "legal.html";
	
	/**
	 * Filename of the instructions.
	 */
	private static final String FILENAME_INSTRUCTIONS = "instructions.html";
	
	/**
	 * Filename of the to do list.
	 */
	private static final String FILENAME_TODO = "todo.html";
	
	/**
	 * Select background activity callback ID.
	 */
	private static final int SELECT_BACKGROUND = 1;
	
    @SuppressWarnings ( "deprecation" )
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		final ActionBar actionBar = getActionBar();
		Drawable background =getResources().getDrawable(R.drawable.w_top_bar);  
		actionBar.setBackgroundDrawable(background);
        
        getPrefs(this);
        
        final PreferenceManager manager = this.getPreferenceManager();
        manager.setSharedPreferencesName(Preferences.SHARED_NAME);
        this.addPreferencesFromResource(R.xml.preferences);
        
        final SharedPreferences preferences = manager.getSharedPreferences();
        final Resources resources = this.getResources();
        
//        //reset display and game
//        this.findPreference(resources.getString(R.string.settings_display_reset_key)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
//			public boolean onPreferenceClick(final Preference preference) {
//				(new AlertDialog.Builder(Preferences.this))
//					.setMessage(resources.getString(R.string.reset_display))
//					.setCancelable(false)
//					.setPositiveButton(resources.getString(R.string.yes), new DialogInterface.OnClickListener() {
//						public void onClick(final DialogInterface dialog, final int which) {
//							Preferences.this.loadDisplayAndGameDefaults();
//							
//							Toast.makeText(Preferences.this, resources.getString(R.string.reset_display_toast), Toast.LENGTH_LONG).show();
//						}
//					})
//					.setNegativeButton(resources.getString(R.string.no), null)
//					.show();
//				return true;
//			}
//		});
//        
//        //reset colors
//        this.findPreference(resources.getString(R.string.settings_color_reset_key)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
//			public boolean onPreferenceClick(final Preference preference) {
//				(new AlertDialog.Builder(Preferences.this))
//					.setMessage(resources.getString(R.string.reset_color))
//					.setCancelable(false)
//					.setPositiveButton(resources.getString(R.string.yes), new DialogInterface.OnClickListener() {
//						public void onClick(final DialogInterface dialog, final int which) {
//							Preferences.this.loadColorDefaults();
//							
//							Toast.makeText(Preferences.this, resources.getString(R.string.reset_color_toast), Toast.LENGTH_LONG).show();
//						}
//					})
//					.setNegativeButton(resources.getString(R.string.no), null)
//					.show();
//				return true;
//			}
//		});
//        
        //info email
        this.findPreference(resources.getString(R.string.information_contact_email_key)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(final Preference preference) {
				Preferences.this.infoEmail();
				return true;
			}
		});
        
        //info twitter
        this.findPreference(resources.getString(R.string.information_contact_twitter_key)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(final Preference preference) {
				Preferences.this.infoTwitter();
				return true;
			}
		});
        
        //info web author
        this.findPreference(resources.getString(R.string.information_contact_website_key)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(final Preference preference) {
				Preferences.this.infoWebAuthor();
				return true;
			}
		});
        
        //info web app
        this.findPreference(resources.getString(R.string.app_website_key)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(final Preference preference) {
				Preferences.this.infoWebApp();
				return true;
			}
		});
//        
//        //info market
//        this.findPreference(resources.getString(R.string.information_market_view_key)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
//			public boolean onPreferenceClick(final Preference preference) {
//				Preferences.this.infoMarket();
//				return true;
//			}
//		});
        
//        //instructions
//        this.findPreference(resources.getString(R.string.instructions_key)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
//			public boolean onPreferenceClick(final Preference preference) {
//				Preferences.this.viewInstructions();
//				return true;
//			}
//		});
        
        //change log
        this.findPreference(resources.getString(R.string.changelog_key)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(final Preference preference) {
				Preferences.this.viewChangelog();
				return true;
			}
		});
        
        //legal
        this.findPreference(resources.getString(R.string.legal_key)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(final Preference preference) {
				Preferences.this.viewLegal();
				return true;
			}
		});
        
        //todo
        this.findPreference(resources.getString(R.string.todo_key)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(final Preference preference) {
				Preferences.this.viewTodo();
				return true;
			}
		});
        
        //github Author
        this.findPreference(resources.getString(R.string.githubauthor_key)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Preferences.this.viewGitHubAuthor();
				return true;
			}
		});
        
        //github app
        this.findPreference(resources.getString(R.string.githubapp_key)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Preferences.this.viewGitHubApp();
				return true;
			}
		});
        
        
//        
//        //xda
//        this.findPreference(resources.getString(R.string.xda_key)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
//			public boolean onPreferenceClick(Preference preference) {
//				Preferences.this.viewXda();
//				return true;
//			}
//		});
        
//        //background image
//        this.findPreference(resources.getString(R.string.settings_color_bgimage_key)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
//			public boolean onPreferenceClick(final Preference preference) {
//				Preferences.this.startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI), Preferences.SELECT_BACKGROUND);
//				return true;
//			}
//		});
//        
//        //clear background image
//        this.findPreference(resources.getString(R.string.settings_color_bgimageclear_key)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
//			public boolean onPreferenceClick(final Preference preference) {
//				Preferences.this.getPreferenceManager().getSharedPreferences().edit().putString(resources.getString(R.string.settings_color_bgimage_key), null).commit();
//				Toast.makeText(Preferences.this, R.string.settings_color_bgimageclear_toast, Toast.LENGTH_SHORT).show();
//				return true;
//			}
//		});

        //Register as a preference change listener
       // Wallpaper.PREFERENCES.registerOnSharedPreferenceChangeListener(this);
      //  this.onSharedPreferenceChanged(Wallpaper.PREFERENCES, null);
        
   
    }
    
    static SharedPreferences getPrefs(Context context) {
        PreferenceManager.setDefaultValues(context, PREFS_NAME, MODE_PRIVATE,
                R.xml.preferences, false);
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }
    
    
    /**
     * Open change log.
     */
    private void viewChangelog() {
		final Intent intent = new Intent(this, About.class);
		intent.putExtra(About.EXTRA_FILENAME, Preferences.FILENAME_CHANGE_LOG);
		intent.putExtra(About.EXTRA_TITLE, this.getResources().getString(R.string.changelog_title));
		this.startActivity(intent);
    }
    
    /**
     * Open instructions.
     */
    private void viewInstructions() {
		final Intent intent = new Intent(this, About.class);
		intent.putExtra(About.EXTRA_FILENAME, Preferences.FILENAME_INSTRUCTIONS);
		intent.putExtra(About.EXTRA_TITLE, this.getResources().getString(R.string.instructions_title));
		this.startActivity(intent);
    }
    
    /**
     * Open legal
     */
    private void viewLegal() {
		final Intent intent = new Intent(this, About.class);
		intent.putExtra(About.EXTRA_FILENAME, Preferences.FILENAME_LEGAL);
		intent.putExtra(About.EXTRA_TITLE, this.getResources().getString(R.string.legal_title));
		this.startActivity(intent);
    }
    
    /**
     * Open todo
     */
    private void viewTodo() {
		final Intent intent = new Intent(this, About.class);
		intent.putExtra(About.EXTRA_FILENAME, Preferences.FILENAME_TODO);
		intent.putExtra(About.EXTRA_TITLE, this.getResources().getString(R.string.todo_title));
		this.startActivity(intent);
    }
    
    /**
     * Open GitHub App
     */
    private void viewGitHubApp() {
    	final Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(this.getResources().getString(R.string.githubapp_href)));
		
		this.startActivity(intent);
    }
    
    
    /**
     * Open GitHub Author
     */
    private void viewGitHubAuthor() {
    	final Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(this.getResources().getString(R.string.githubauthor_href)));
		
		this.startActivity(intent);
    }
    
	@SuppressWarnings ( "deprecation" )
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
		final boolean all = (key == null);
		final Resources resources = this.getResources();
		
		//Only enable clear bg image when a bg image is set
		final String bgimage = resources.getString(R.string.settings_color_bgimage_key);
		if (all || key.equals(bgimage)) {
			final boolean enabled = preferences.getString(bgimage, null) != null;
			this.findPreference(resources.getString(R.string.settings_color_bgimageclear_key)).setEnabled(enabled);
			this.findPreference(resources.getString(R.string.settings_color_bgopacity_key)).setEnabled(enabled);
		}
		
		//If the icon rows or cols are explicitly changed then clear the widget locations
		final String iconRows = resources.getString(R.string.settings_display_iconrows_key);
		final String iconCols = resources.getString(R.string.settings_display_iconcols_key);
		if (all || key.equals(iconRows) || key.equals(iconCols)) {
			//final int rows = preferences.getInt(iconRows, resources.getInteger(R.integer.display_iconrows_default));
			//final int cols = preferences.getInt(iconCols, resources.getInteger(R.integer.display_iconcols_default));
			final String widgetLocations = resources.getString(R.string.settings_display_widgetlocations_key);
			
			if (!all) {
				//Clear any layouts
				preferences.edit().putString(widgetLocations, resources.getString(R.string.display_widgetlocations_default)).commit();
			}
			
			//Update with counts
			//((WidgetLocationsPreference)this.findPreference(widgetLocations)).setIconCounts(rows, cols);
		}
    }

   

	@SuppressWarnings ( "deprecation" )
    @Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			final Resources resources = this.getResources();
			
			switch (requestCode) {
				case Preferences.SELECT_BACKGROUND:
					//Store the string value of the background image
				    this.getPreferenceManager().getSharedPreferences().edit().putString(resources.getString(R.string.settings_color_bgimage_key), data.getDataString()).commit();
				    Toast.makeText(this, R.string.settings_color_bgimage_toast, Toast.LENGTH_SHORT).show();
					break;
					
				default:
					super.onActivityResult(requestCode, resultCode, data);
			}
		}
	}

	/**
	 * Launch an intent to send an email.
	 */
	private void infoEmail() {
        final Resources resources = this.getResources();
		final Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("plain/text");
		intent.putExtra(Intent.EXTRA_EMAIL, new String[] { resources.getString(R.string.information_contact_email_data) });
		intent.putExtra(Intent.EXTRA_SUBJECT, resources.getString(R.string.app_name));
		
		this.startActivity(intent);
    }
    
	/**
	 * Launch an intent to view twitter page.
	 */
    private void infoTwitter() {
        final Resources resources = this.getResources();
		final Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(resources.getString(R.string.information_contact_twitter_data)));
		
		this.startActivity(intent);
    }
    
    /**
     * Launch an intent to view website.
     */
    private void infoWebApp() {
        final Resources resources = this.getResources();
    	final Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(resources.getString(R.string.app_website_data)));
		
		this.startActivity(intent);
    }
    
    /**
     * Launch an intent to view website.
     */
    private void infoWebAuthor() {
        final Resources resources = this.getResources();
    	final Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(resources.getString(R.string.information_contact_website_data)));
		
		this.startActivity(intent);
    }
    
    /**
     * Launch an intent to view other market applications.
     */
    private void infoMarket()
    {
        final Resources resources = this.getResources();
    	final Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(resources.getString(R.string.information_market_view_data)));
		
		this.startActivity(intent);
    }
    
    /**
     * Reset display preferences to their defaults.
     */
    private void loadDisplayAndGameDefaults() {
        final Resources resources = this.getResources();
	    @SuppressWarnings ( "deprecation" )
        final SharedPreferences.Editor editor = Preferences.this.getPreferenceManager().getSharedPreferences().edit();

		//user controllable
		editor.remove(resources.getString(R.string.settings_game_usercontrol_key));
		//fps
		editor.remove(resources.getString(R.string.settings_display_fps_key));
		//show walls
		editor.remove(resources.getString(R.string.settings_display_showwalls_key));
		//icon rows
		editor.remove(resources.getString(R.string.settings_display_iconrows_key));
		//icon cols
		editor.remove(resources.getString(R.string.settings_display_iconcols_key));
		//icon row spacing
		editor.remove(resources.getString(R.string.settings_display_rowspacing_key));
		//icon col spacing
		editor.remove(resources.getString(R.string.settings_display_colspacing_key));
		//widget locations
		editor.remove(resources.getString(R.string.settings_display_widgetlocations_key));
		//padding top
		editor.remove(resources.getString(R.string.settings_display_padding_top_key));
		//padding bottom
		editor.remove(resources.getString(R.string.settings_display_padding_bottom_key));
		//padding left
		editor.remove(resources.getString(R.string.settings_display_padding_left_key));
		//padding right
		editor.remove(resources.getString(R.string.settings_display_padding_right_key));
	
		editor.commit();
    }
    
    /**
     * Reset color preferences to their defaults.
     */
    @SuppressWarnings ( "deprecation" )
    private void loadColorDefaults() {
        final Resources resources = this.getResources();
		final SharedPreferences.Editor editor = Preferences.this.getPreferenceManager().getSharedPreferences().edit();

		//background
		editor.remove(resources.getString(R.string.settings_color_background_key));
		//walls
		editor.remove(resources.getString(R.string.settings_color_walls_key));
		//background image
		editor.remove(resources.getString(R.string.settings_color_bgimage_key));
		//background opacity
		editor.remove(resources.getString(R.string.settings_color_bgopacity_key));
		//player
		editor.remove(resources.getString(R.string.settings_color_player_key));
		//opponent
		editor.remove(resources.getString(R.string.settings_color_opponent_key));
		
		editor.commit();
    }
}
