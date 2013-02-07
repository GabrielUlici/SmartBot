package com.nightideaslab.smartbot;

import com.nightideaslab.util.ThreadExecutor;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.util.LinkedList;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class SearchActivity extends Activity
		implements ServiceListener
{
	public final static String TAG = "**SB Search**";

	private Context context;
	public Context getAppContext() { return context; }

	public final static String ROBOT_TYPE = "_naoqi._tcp.local.";
	public final static String DACP_TYPE = "_naoqi._tcp.local.";
	public final static String REMOTE_TYPE = "_naoqi._tcp.local.";
	public final static String HOSTNAME = "robotsoulreborn";

	private static JmDNS zeroConf = null;
	private static MulticastLock mLock = null;

	public static String servicename;
	public static String hostname;
	public static String host;
	public static int port;

	// this screen will run a network query of all Robots
	// upon selection it will try authenticating with that robot, and launch
	// the an error if failed
	protected void startProbe() throws Exception
	{
		runOnUiThread(new Runnable()
		{
			public void run()
			{
				adapter.known.clear();
				adapter.notifyDataSetChanged();
			}
		});
		// figure out our wifi address, otherwise bail
		WifiManager wifi = (WifiManager) SearchActivity.this.getSystemService(Context.WIFI_SERVICE);

		WifiInfo wifiinfo = wifi.getConnectionInfo();
		int intaddr = wifiinfo.getIpAddress();

		if (intaddr != 0) { 
			// Only worth doing if there's an actual wifi connection

			byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff),
					(byte) (intaddr >> 8 & 0xff),
					(byte) (intaddr >> 16 & 0xff),
					(byte) (intaddr >> 24 & 0xff) };
			InetAddress addr = InetAddress.getByAddress(byteaddr);

			Log.d(TAG,String.format("found intaddr=%d, addr=%s", intaddr, addr.toString()));
			// start multicast lock
			mLock = wifi.createMulticastLock("RobotSoulReborn lock");
			mLock.setReferenceCounted(true);
			mLock.acquire();

			zeroConf = JmDNS.create(addr, HOSTNAME);
			zeroConf.addServiceListener(ROBOT_TYPE, SearchActivity.this);
			zeroConf.addServiceListener(DACP_TYPE, SearchActivity.this);

		} 
		else checkWifiState();
	}

	protected void stopProbe()
	{
		zeroConf.removeServiceListener(ROBOT_TYPE, this);
		zeroConf.removeServiceListener(DACP_TYPE, this);

		ThreadExecutor.runTask(new Runnable()
		{
			public void run()
			{
				try
				{
					zeroConf.close();
					zeroConf = null;
				}
				catch (IOException e)
				{
					Log.d(TAG, String.format("ZeroConf Error: %s", e.getMessage()));
				}
			}
		});
		mLock.release();
		mLock = null;
	}

	public static JmDNS getZeroConf()
	{
		return zeroConf;
	}

	public void serviceAdded(ServiceEvent event)
	{
		// someone is yelling about their touch-able service
		// go figure out what their ip address is
		Log.w(TAG, String.format("serviceAdded(event=\n%s\n)", event.toString()));
		final String name = event.getName();

		// trigger delayed gui event
		// needs to be delayed because jmdns hasnt parsed txt info yet
		resultsUpdated.sendMessageDelayed(Message.obtain(resultsUpdated, -1, name), DELAY);
	}

	public void serviceRemoved(ServiceEvent event)
	{
		Log.w(TAG, String.format("serviceRemoved(event=\n%s\n)", event.toString()));
	}

	public void serviceResolved(ServiceEvent event)
	{
		Log.w(TAG, String.format("serviceResolved(event=\n%s\n)", event.toString()));
	}

	public final static int DONE = 3;

	public Handler resultsUpdated = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			if (msg.obj != null)
			{
				boolean result = adapter.notifyFound((String) msg.obj);
				// only update UI if a new one was added
				if (result)
				{
					adapter.notifyDataSetChanged();
				}
			}
		}
	};

	public final static int DELAY = 500;

	protected ListView list;
	protected SearchAdapter adapter;

	@Override
	public void onResume()
	{
		super.onResume();
		try
		{
			checkWifiState();
		}
		catch (NullPointerException npe)
		{
			npe.printStackTrace();
		}
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if (this.adapter != null)
		{
			this.adapter.known.clear();
		}
	}

	@Override
	public void onStop()
	{
		super.onStop();
		try
		{
			if (zeroConf != null) SearchActivity.this.stopProbe();
		}
		catch (NullPointerException npe)
		{
			npe.printStackTrace();
		}
	}

	/**
	 * Gets the current wifi state, and changes the text shown in the header as
	 * required.
	 */
	public void checkWifiState()
	{
		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		int intaddr = wifi.getConnectionInfo().getIpAddress();

		View header = adapter.footerView;
		if (!header.equals(adapter.footerView)) Log.e(TAG, "Header is wrong");
		else 
		{
			TextView title = (TextView) header.findViewById(android.R.id.text1);
			TextView explanation = (TextView) header.findViewById(android.R.id.text2);
			ProgressBar progress = (ProgressBar) header.findViewById(R.id.progress);

			if (wifi.getWifiState() == WifiManager.WIFI_STATE_DISABLED)
			{
				// Wifi is disabled
				title.setText(R.string.wifi_disabled_title);
				explanation.setText(R.string.wifi_disabled);
				progress.setVisibility(View.GONE);
			}
			else if (intaddr == 0)
			{
				// Wifi is enabled, but no network connection
				title.setText(R.string.no_network_title);
				explanation.setText(R.string.no_network);
				progress.setVisibility(View.VISIBLE);
			}
			else
			{
				// Wifi is enabled and there's a network
				title.setText(R.string.item_network_title);
				explanation.setText(R.string.item_network_caption);
				progress.setVisibility(View.VISIBLE);
			}
		}
	}

	/**
	 * Will set the state of the header view depending on Wifi state
	 * 
	 * @param state
	 */
	public void setState(int state)
	{
		View header = list.getChildAt(0);
		if (!header.equals(adapter.footerView))
			Log.e(TAG, "Header is wrong");
		else
		{
			TextView title = (TextView) header.findViewById(android.R.id.text1);
			TextView explanation = (TextView) header.findViewById(android.R.id.text2);
			ProgressBar progress = (ProgressBar) header.findViewById(R.id.progress);

			switch (state)
			{
				// Wifi is disabled, hide spinner, show error, linkify wifi in
				// settings
				case 0:
					title.setText(R.string.wifi_disabled_title);
					explanation.setText(R.string.wifi_disabled);
					progress.setVisibility(View.GONE);
					break;

				// Wifi is enabled but we're not connected to a network
				case 1:
					title.setText(R.string.no_network_title);
					explanation.setText(R.string.no_network);
					progress.setVisibility(View.VISIBLE);
					break;

				// Wifi is enabled and we're connected - show the standard text
				case 2:
					title.setText(R.string.item_network_title);
					explanation.setText(R.string.item_network_caption);
					progress.setVisibility(View.VISIBLE);
					break;
			}
		}
	}

	/**
	 * 
	 * Android Framework: onCreate
	 * 
	 * Initialize UI and app's resources
	 * 
	 * @param savedInstanceState
	 *            Object used to retrive active app state
	 * 
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.context = getApplicationContext();
	
//		final ActionBar actionBar = getActionBar();
//		@SuppressWarnings ( "deprecation" )
//      BitmapDrawable background = new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.error_robot_texture));
//		background.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
//		actionBar.setBackgroundDrawable(background);
		
		// Setting a background image to the action bar
		final ActionBar actionBar = getActionBar();
		Drawable background =getResources().getDrawable(R.drawable.w_top_bar);  
		actionBar.setBackgroundDrawable(background);
		
		setContentView(R.layout.gen_list);

		this.adapter = new SearchAdapter(this);

		this.list = (ListView) this.findViewById(android.R.id.list);
		this.list.addHeaderView(adapter.footerView, null, false);
		this.list.setAdapter(adapter);
		

		
		this.list.setOnItemClickListener(new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				// read ip/port from caption if present
				// pass off to TCPClient/Dashboard to try to connect to the
				// robot

				// Use the robot name
				String robot = ((TextView) view.findViewById(android.R.id.text1)).getText().toString();

				// Use the IP Address
				String address = ((TextView) view.findViewById(android.R.id.text2)).getText().toString();

				String port = "30000";
				// push off fake result to try login
				// this will start the pairing process if needed
				//Intent shell = new Intent(SearchActivity.this, DashboardMainActivity.class);
				//shell.putExtra("EXTRA_ADDRESS", address);
				//shell.putExtra("EXTRA_ROBOT", robot);
				//shell.putExtra("EXTRA_PORT", port);
				//startActivity(shell);
				// onActivityResult(-1, Activity.RESULT_OK, shell);

				// Intent i = new Intent(getApplicationContext(), Das.class);
				// startActivity(i);
			}
		});

		ThreadExecutor.runTask(new Runnable()
		{
			public void run()
			{
				try
				{
					SearchActivity.this.startProbe();
				}
				catch (Exception e)
				{
					Log.d(TAG, String.format("onCreate Error: %s", e.getMessage()));
				}
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		// someone thinks they are ready to pair with us
		if (resultCode == Activity.RESULT_CANCELED)
			return;

		// final String address =
		// data.getStringExtra(BackendService.EXTRA_ADDRESS);
		// final String library =
		// data.getStringExtra(BackendService.EXTRA_LIBRARY);
		// final String code = data.getStringExtra(BackendService.EXTRA_CODE);
		// Log.d(TAG,
		// String.format("onActivityResult with address=%s, library=%s, code=%s and resultcode=%d",
		// address,
		// library, code, resultCode));

		ThreadExecutor.runTask(new Runnable()
		{

			public void run()
			{
			try
			{
					// check to see if we can actually authenticate against the
					// library
					// backend.setLibrary(address, library, code);
					startProbe();
					System.out.println("inside the run");
					// if successful, then throw back to controlactivity
					// LibraryActivity.this.startActivity(new
					// Intent(LibraryActivity.this,
					// ControlActivity.class));
					// LibraryActivity.this.setResult(Activity.RESULT_OK);
					// LibraryActivity.this.finish();

			}
			catch (final ConnectException ce)
			{
				Log.e(TAG, String.format("ohhai we had problemzss, probably still unpaired"),ce);
				System.out.println("inside problemzsss");
				SearchActivity.this.runOnUiThread(new Runnable()
				{
					public void run()
					{
						Toast.makeText(SearchActivity.this,	"Connection error:"	
									+ (ce.getMessage().contains("ECONNREFUSED") ? " Connection refused"
											: ""), Toast.LENGTH_LONG).show();
					}
				});

				}
				catch (Exception e)
				{
					Log.e(TAG,String.format("ohhai we had problemz, probably still unpaired"),e);

					System.out.println("inside the problem");
					// we probably had a pairing issue, so start the pairing
					// server
					// and
					// wait for trigger
					// Intent intent = new Intent(SearchActivity.this,
					// DashboardActivity.class);
					// intent.putExtra("EXTRA_ADDRESS", address);
					// intent.putExtra(BackendService.EXTRA_LIBRARY, library);
					// LibraryActivity.this.startActivityForResult(intent, 1);
				}
			}
		});
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
		getMenuInflater().inflate(R.menu.search_menu, menu);
		return super.onCreateOptionsMenu(menu); 
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.menu_search_refresh:
			ThreadExecutor.runTask(new Runnable()
			{
				public void run()
				{
					try
					{
						startProbe();
					}
						catch (Exception e)
						{
							Log.d(TAG, String.format("onCreate Error: %s",e.getMessage()));
						}
				}
			});
			return true;

		case R.id.menu_search_manual:
			LayoutInflater inflater = (LayoutInflater) SearchActivity.this
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View view = inflater.inflate(R.layout.dia_text, null);
			final TextView address = (TextView) view
					.findViewById(android.R.id.text1);
			final TextView port = (TextView) view
					.findViewById(android.R.id.text2);
			// port.setText("9876");

			new AlertDialog.Builder(SearchActivity.this)
					.setView(view)
					.setPositiveButton(R.string.search_manual_pos,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									// try connecting to this specific ip
									// address
									// Intent shell = new Intent();

									// shell.putExtra("EXTRA_ADDRESS",
									// address.getText().toString());
									// shell.putExtra("EXTRA_ROBOT", "Manual");
									// shell.putExtra("EXTRA_PORT",
									// port.getText().toString());
									// onActivityResult(-1, Activity.RESULT_OK,
									// shell);
								//	Intent shell = new Intent(SearchActivity.this, DashboardMainActivity.class);
								//	startActivity(shell);
								}
							})
					.setNegativeButton(R.string.search_manual_neg,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
								}
							}).create().show();
			return true;

		case R.id.menu_search_settings:
		//	Intent shell = new Intent(SearchActivity.this, Preferences.class);
		//	startActivity(shell);
			return true;

		case R.id.menu_search_quit:
			new AlertDialog.Builder(SearchActivity.this)
					.setMessage("Are you sure you want to exit?")
					.setCancelable(false)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									finish();
								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).create().show();
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

	public class SearchAdapter extends BaseAdapter
	{
		protected Context context;
		protected LayoutInflater inflater;
		public View footerView;
		protected final LinkedList<ServiceInfo> known = new LinkedList<ServiceInfo>();

		public SearchAdapter(Context context)
		{
			this.context = context;
			this.inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			this.footerView = inflater.inflate(R.layout.search_layout, null, false);
		}

		public boolean notifyFound(String library)
		{
			boolean result = false;
			try {
				Log.d(TAG, String.format("DNS Name: %s", library));
				ServiceInfo serviceInfo = getZeroConf().getServiceInfo(
						ROBOT_TYPE, library);

				// try and get the DACP type only if we cannot find any
				// touchable
				if (serviceInfo == null) {
					serviceInfo = getZeroConf().getServiceInfo(DACP_TYPE,
							library);
				}

				if (serviceInfo == null) {
					return result; // nothing to add since serviceInfo is NULL
				}

				String robotName = serviceInfo.getPropertyString("CtlN");
				if (robotName == null) {
					robotName = serviceInfo.getName();
				}

				// check if we already have this DatabaseId
				for (ServiceInfo service : known) {
					String knownName = service.getPropertyString("CtlN");
					if (knownName == null) {
						knownName = service.getName();
					}
				}

				if (!known.contains(serviceInfo)) {
					known.add(serviceInfo);
					result = true;
				}
			} catch (Exception e) {
				Log.d(TAG, String.format(
						"Problem getting ZeroConf information %s",
						e.getMessage()));
			}

			return result;
		}

		public Object getItem(int position)
		{
			return known.get(position);
		}

		@Override
		public boolean hasStableIds()
		{
			return true;
		}

		public int getCount()
		{
			return known.size();
		}

		public long getItemId(int position)
		{
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent)
		{
			if (convertView == null)
				convertView = inflater.inflate(
						android.R.layout.simple_list_item_2, parent, false);
			try {
				// fetch the dns txt record to get library info
				final ServiceInfo serviceInfo = (ServiceInfo) this
						.getItem(position);

				servicename = serviceInfo.getServer();

				hostname = serviceInfo.getName();

				host = serviceInfo.getHostAddresses()[0];
				port = serviceInfo.getPort();

				Log.d(TAG, String.format("ZeroConf Server: %s", servicename));
				Log.d(TAG, String.format("ZeroConf Robot: %s", hostname));
				Log.d(TAG, String.format("ZeroConf IP Address: %s", host));
				Log.d(TAG, String.format("ZeroConf Port: %s", port));

				((TextView) convertView.findViewById(android.R.id.text1))
						.setText(hostname);
				((TextView) convertView.findViewById(android.R.id.text2))
						.setText(host);
			} catch (Exception e) {
				Log.d(TAG, String.format(
						"Problem getting ZeroConf information %s",
						e.getMessage()));
				((TextView) convertView.findViewById(android.R.id.text1))
						.setText("Unknown");
				((TextView) convertView.findViewById(android.R.id.text2))
						.setText("Unknown");
			}
			return convertView;
		}
	}
}
