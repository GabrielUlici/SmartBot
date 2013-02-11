package com.nightideaslab.smartbot;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;

public class SmartBot extends Activity
{
	private ProgressBar mProgressBar;
	private Handler mHandler = new Handler();
	
	private int mProgressStatus = 0;
	private Thread mThread;
	private Runnable mRunnable;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.smart_bot);
		
		mProgressBar = (ProgressBar) findViewById(R.id.progress_horizontal);
		
		mRunnable = new Runnable()
		{				
		
			@Override
			public void run()
			{
				while(mProgressStatus<100)
				{
					try
					{																				
						/** Increment progress bar status by 1 */
						mProgressStatus++;
							
						/** Sleep this thread for 50 ms */
						Thread.sleep(20);
					}
					catch(Exception e)
					{
						Log.d("Wait Exception",e.toString());
					}
						
					/** For every increment, sent a message to UI thread to increment the progress bar */
					mHandler.post(new Runnable()
					{						
						@Override
						public void run()
						{
							mProgressBar.setProgress(mProgressStatus);
						}
					});
				}
					
				/**
				 *  Launch the Search activity
				 */
				mHandler.post(new Runnable()
				{
					@Override
					public void run()
					{
						//Finished the progress bar
						final Intent mainIntent = new Intent(SmartBot.this, SearchActivity.class);
						SmartBot.this.startActivity(mainIntent);
						SmartBot.this.finish();
					}
				});			
			}
		};
		
		/** Calling the progress thread */	
		startProgress();
		
	}
	
	/**
	 * Starts the thread to update the progress bar
	 */
	protected void startProgress ()
	{	
		/** Create a new thread */
		mThread = new Thread(mRunnable);
		
		if(mProgressStatus==100)
			mProgressStatus=0;		
		
		/** Starts the new thread */
		mThread.start();	
	}
}
