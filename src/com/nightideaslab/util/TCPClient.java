package com.nightideaslab.util;

import com.nightideaslab.smartbot.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.concurrent.ArrayBlockingQueue;
//import java.net.URL;
//import java.net.URLConnection;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;

import android.net.ConnectivityManager;
//import android.os.Message;
import android.util.Log;

/**
 * TCP Connection Manager.
 * 
 * Runs in its own thread and communicates 
 * 			with the activity's by messages
 * 
 */
public class TCPClient implements Runnable
{
	public static final int MSG_CONNECTIONSTART	= 12000;		/** send to the app when communication has been established */
	public static final int MSG_CONNECTIONSTOP	= 12001;		/** sent to the app when communication has been lost */
	public static final int MSG_SENT			= 12002;		/** sent to the app when a message has been sent */
	public static final int MSG_COMSTATUSCHANGE	= 12003;		/** sent to the app when connection status change */
	public static final int MSG_NEWSTATUSMESSAGE= 12004;		/** sent to the app when needs to display a message on UI */
	
	public static final byte STATUS_DISCONNECTED 	= 0;		/** status STATUS_DISCONNECTED */
	public static final byte STATUS_CONNECTING 		= 1;		/** status STATUS_CONNECTING */
	public static final byte STATUS_CONNECTED		= 2;		/** status STATUS_CONNECTED */
	public static final byte STATUS_PAUSEBEFORERETRY= 3;		/** status STATUS_PAUSEBEFORERETRY */
		
	public boolean bbAutoConnection;							/** Repeated attempts to connect automatically	*/
    public boolean bbFlushMessageOnExit;					
    public boolean bbInsertMsgNumber;							/** TRUE Insert before each message the number of packet sent	*/
    
    public String msgHello;										/** Starting message to server. Disabled if null or ""	*/
    public String msgQuit;										/** Closing message to server. Disabled if null or ""	*/
    public String msgAck;										/** Debugging Ack message. Disabled if null or ""	*/
    
	String TAG = "**TCP CLIENT**";								/** LOG TAG */
	
    Socket socket;
    OutputStream outstream;
    PrintWriter outobj;
    BufferedReader br;
    
    public String address, port;								/** Address and port to connect to, received from Dashboard Activity */
	public int PORT;											/** Parses a string and returns an integer */ 
    
    static final int RETRY_TIMEOUT_MILLISEC = 5000;				/** retry connection delay */
    static final int DELAY_SOCKET_READ = 10;			
    static final int SEND_DELAY_NOTEMPTY = 0;					/** delay between two consecutive posts	*/
    static final int SEND_DELAY_EMPTY = 10;						/** delay when no message to send	*/
    static final int ms_IDLE_ACK = 500;							/** ACK time interval	*/
    
    static final int MAX_QUEUED_MESSAGE = 20;			
    volatile boolean bbExitRequest;								/** TRUE if request to exit	*/
    volatile boolean bbExited;									/** Thread exited	*/
    volatile boolean bbRequestToClose;							/** TRUE for closing the actual connection	*/
    
    byte _conn_status;											/** connection status, @see STATUS_DISCONNECTED */ 
    										
    final ArrayBlockingQueue<String> msg_to_send;				/** pending messages to send */
    final NumberFormat formatter2;								/** String describing format of sensor values */
    android.os.Handler observer;								/** Container */
    final ConnectivityManager cm;								/** to check Internet status */
    
    int nToAck;													/** statistics, ack counter */
    int n_tcpclient_counter;
    int n_tcppacket_sent;										/** line sent counter */
    long next_ms_ack;											/** time to next ack */

    int token;													/** token to communicate with app */
    
    Handler h;
    
    /**
     * Constructor
     * 
     * @param _observer		Application object to be notified of status change
     * @param _cm			Connectivity Manager
     * @param _token		A unique number used to communicate with the application 
     */
    public TCPClient (android.os.Handler _observer, ConnectivityManager _cm, int _token)
    {
    	cm = _cm;
    	formatter2 = new DecimalFormat("#0.000000");
    	observer = _observer;
    	Log.i (TAG,"Initializing the TCP Client");

    	bbExitRequest=bbExited=false;
    	msg_to_send = new ArrayBlockingQueue<String> (MAX_QUEUED_MESSAGE);
    	
    	n_tcpclient_counter=0;
    	n_tcppacket_sent=0;
 
		_conn_status=STATUS_DISCONNECTED;
		bbAutoConnection=true;
		bbFlushMessageOnExit=false;
		bbInsertMsgNumber =false;
				
		msgHello=null;
		msgAck=null;
		msgQuit=null;
		bbRequestToClose=false;
		next_ms_ack=0;
		token = _token;
		
		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = 
			        new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
			}
		
    }
 
    /**
     * Active Wait. Monitor exit request.
     * 
     * Delay for milliseconds.
     * @param	millisec			Delay (ms)
     * @param min_abs_millisec		Minimum delay: the routine can exit by Quit invocation.
     */
    private void activeWait (long millisec, long min_abs_millisec)
    {
    	final long delta_ms = 100;
    	Log.i (TAG,"activeWait (millisec="+millisec+" , min_abs_millisec="+min_abs_millisec);
		while ((!bbExitRequest && millisec>0) || min_abs_millisec>0 )
		{
			millisec-=delta_ms;
			min_abs_millisec-=delta_ms;
			try	{ Thread.sleep(delta_ms); } catch (Exception ee) {}
			if (bbExited) break;
		}
    }    
      
    /**
     * main loop 
     * 
     * Sends message, reads server responses, eventually repeats connection attempts.
     * Exit when Quit() is called.
     */
    @Override
	public void run() 
    { 	  
    	Thread.currentThread().setName("TCPClient");
    	Log.i (TAG,"Starting thread");
 	   	bbExitRequest=false;
 	   	bbExited=false;
 	   	while (bbExitRequest==false)
 	   	{
  		   ++n_tcpclient_counter;
	   
 		   if (socket==null)
 		   {
 			  if (bbAutoConnection && !try_connect ()) 
 			  {
 				  setConnStatus (STATUS_PAUSEBEFORERETRY);
 				  activeWait(RETRY_TIMEOUT_MILLISEC,100);
 				  continue;
 			  }
 		   }
 		   else
 		   {
 			   if (bbRequestToClose)
 			   {
 				  bbRequestToClose=false;
 				   try_close (true);
 			   }
 			   else
 			   {				   
 				   try
 				   {
					   int n=5;
					   while (n>0 && br.ready())
					   {
						   onReceived (br.readLine()); 
	 					   n--;	 					   
	 				   }
 				   }
				   catch (Exception e)
				   {
					   
				   }
		 		  boolean bbSent=send_first_message ();
		 		  if (!bbSent)
				  	{
					  if (next_ms_ack>0 && next_ms_ack<System.currentTimeMillis() && !bbExitRequest && msgAck!=null && msgAck.length()>0)
			        	 {
			        		 send_message (msgAck);
			        		 bbSent=true;
			        	 }
					}
	 			  
	 			  if (!bbExitRequest)
	 			  {
	 				  int delay = msg_to_send.size()>0?SEND_DELAY_NOTEMPTY:SEND_DELAY_EMPTY;
	 				  
	 				  if ( delay>0)
	 				  {
						  try
						  {
							  Thread.sleep(delay, 0);
						  }
						  catch (InterruptedException e)
						  {
							 
						  }
	 				  }
	 			  } 
 			   }
 		  }
 	   }
 	   	
 	   if (bbFlushMessageOnExit && msg_to_send.size()>0 && socket!=null)
 	   {
 		   synchronized (msg_to_send)
 		   {
 			   while (send_first_message()); 
 		   }
 	   }
 	   
 	   try_close (false);
 	   Log.i (TAG,"TCPClient Thread closed.");
 	   bbExited=true;
 	   observer=null;
    }
   
    /**
     * Send first message in queue, if any
     * 
     * @return TRUE if a message has been sent,
     * 		   FALSE if no message exist or an error occurs.
     */
    private boolean send_first_message ()
    {
    	String ss=null;
    	synchronized (this)
    	{
	    	if (msg_to_send.size()>0)
	    	{
				ss=msg_to_send.poll();
			}
    	}
		if (ss!=null && ss.length()>0) 
		{
			return send_message (ss);
		}
		return false;
    } 

    /**
     * Send a message to server
     * 
     * @param message ASCII line to send
     * @return TRUE if no error occurs
     */
    private boolean send_message (String message)
    {
    	++n_tcppacket_sent;
    	if (bbInsertMsgNumber) message=n_tcppacket_sent+" "+message;
    	return send_message_ascii (message);
    }
        
    /**
     * Send ASCII + Line End Sequence
     * 
     * Send the message to witch is addressed
     * 
     * @param message ASCII line to send
     * @return TRUE if no error occurs
     */  
    private boolean send_message_ascii (String message)
    {
    	if (socket==null || bbExitRequest==true) return false; 
	    try 
	    {
	    	next_ms_ack = System.currentTimeMillis()+ms_IDLE_ACK; 	
			outobj.println(message);
			outobj.flush ();
			outstream.flush ();
	    } 
	    catch (Exception e)
	    {
	    	displayStatusMessage (false, "Error "+e.getMessage()+". Resetting socket and reconnect. ");
	    	try_close (true);
	    	return false;
	    }
	    if (observer!=null)
		{
	   	 	Message msg = Message.obtain();
	   	 	msg.what = MSG_SENT;
	   	 	msg.obj = message;
	   	 	msg.arg2 = token;
	   	 	observer.sendMessage(msg);
		}
	    return true;
    }

    /**
     * Try to connect to server.
     * 
     * @return TRUE if connection is established
     * @return FALSE if connection is not established
     */
    private synchronized boolean try_connect ()
    {
    	if (socket!=null)
    	{
    		try_close (false);
    	}
    	
    	try
    	{
    		// TODO make a better way to receive the IP address and the port
    		
    		address = DashboardMainActivity.address;
			port = DashboardMainActivity.port;

			PORT = Integer.parseInt(port);
    		
			System.out.println("TCP Address : " + address);
			System.out.println("TCP Port : " + PORT);
			
    		setConnStatus (STATUS_CONNECTING);
			InetAddress serverAddr = InetAddress.getByName(address);	//TCPServer.SERVERIP
			Log.d(TAG, "Connecting to server "+address+ " port "+PORT);
			
			socket = new Socket(serverAddr, PORT);
			outstream = socket.getOutputStream();
			outobj = new PrintWriter( new BufferedWriter(new OutputStreamWriter(outstream),512),true);
			br =new BufferedReader(new InputStreamReader(socket.getInputStream()));
			Log.d (TAG,"Connected");
			if (msgHello!=null && msgHello.length()>0)
				send_message (msgHello);
			onConnStarted ();
			return true;
    	}
    	catch (Exception e)
    	{
    		Log.e (TAG,"Imposible to connect to the server : "+e.getMessage());
    		if (socket!=null)
    		{
    			try
    			{
    				socket.close ();
    			}
    			catch (Exception ee)
    			{
    				Log.e (TAG,"Error during the closing of the socket : "+ee.getMessage());
    			}
    		}
    		
    		socket=null;
    		outstream=null;
    		outobj=null;
    		br=null;
    		activeWait (RETRY_TIMEOUT_MILLISEC,0);
    		onConnError ("Unable to connect to " + address + " : " + PORT);
    	}
    	return false;
    }
    
    /**
     * Close server connection
     * 
     * @param bbInhibitMsgSend	if TRUE don't send message
     */
    private synchronized void try_close (boolean bbInhibitMsgSend)
    {	
    	if (socket==null) return;	
    	Log.i (TAG,"Closing connection to server");
    	if (bbInhibitMsgSend==false && msgQuit!=null && msgQuit.length()>0)
    	{
	    	try
	    	{
	    		send_message (msgQuit);
	    	}
	    	catch (Exception e)
	    	{
	    		Log.e (TAG,"Error when sending the Quit message : " + e.getMessage());
	    	}
    	}
    	try
    	{
    		socket.close();
    	}
    	catch (Exception e)
    	{
    		Log.e (TAG,"Error during socket close() : " + e.getMessage());
    	}
    	
    	socket=null;
    	outstream=null;
		outobj=null;
		br=null;
		
		// delete the messages in the queue..
		msg_to_send.clear();
    	onConnClosed ();    	
    }
	 
	/**
	 * Callback when connection is established
	 */
    protected void onConnStarted ()				
    {
    	setConnStatus (STATUS_CONNECTED);
    	displayStatusMessage (false, "connected.");
    	if (observer!=null)
    	{
	    	Message msg = Message.obtain();
	    	msg.what= MSG_CONNECTIONSTART;
	    	msg.obj = this;
	    	msg.arg2 = token;
	    	observer.sendMessage(msg);
    	}
    }
    
    /**
     * Callback on connection error
     * 
     * Updates UI interface with status message
     * 
     * @param status	Message status to display
     */
    protected void onConnError (String status)		
    {
    	displayStatusMessage (true, status);
    	if (observer!=null)
    	{
	    	Message msg = Message.obtain();
	    	msg.what= MSG_CONNECTIONSTOP;
	    	msg.obj = this;
	    	msg.arg2 = token;
	    	observer.sendMessage(msg);
    	}
    }
    
    /**
     * Callback on connection closed
     */
    protected void onConnClosed ()					
    {
    	setConnStatus (STATUS_DISCONNECTED);
    }
	 
    /**
     * Retrieve connection status string description
     * 
     * @return String describing connection status
     */
	 public String getConnStatusStr ()
	 {
		 switch (_conn_status)
		 {
		 case STATUS_DISCONNECTED 		: return "disconnected";
		 case STATUS_CONNECTING 		: return "connecting";
		 case STATUS_CONNECTED 			: return "connected";
		 case STATUS_PAUSEBEFORERETRY	: return "waiting for rec.";
		 }
		 return "..";
	 }
	 /**
	  * Retrieve connection status
	  * 
	  * @return connection status code:
	  * 		0 : disconnected
	  * 		1 : connecting
	  * 		2 : connected
	  * 		3 : waiting for reconnection
	  */
	 public int getConnStatus ()
	 {
		 return _conn_status;
	 }
	 
	 /**
     * Queues a message to be sent to the server
     * 
     * It's a limited FIFO: if more than MAX_QUEUED_MESSAGE messages pending, delete the oldest one.
     * 
     * @param s RAW ASCII message to be sent to server.
     */
    public void sendMessageToServer (String s) throws IllegalStateException 
    {
    	if (bbExitRequest==true)
    	{
    		Log.w (TAG,"Request of sending messages when closing the connection.");
    		return;
    	}
    	
    	if (socket==null)
    	{
    		Log.w (TAG,"Request of sending messages to server without connection.");
    		return;
    	}
    	
    	synchronized (msg_to_send)
    	{
	    	if (msg_to_send.size()==MAX_QUEUED_MESSAGE-1)
	    	{
	    		String ss=msg_to_send.poll();
	    		Log.w (TAG,"QUEUE Full. deleting message : "+ss);
	    	}	
	    	msg_to_send.add(s);
    	}
    	Log.i (TAG,"Sending the message to the server : "+s);
    }
    
    /**
     * Queues a message to be sent to the http server of the NAO
     * 
     * @param function	function calling the message.
     * 			Example. ALBehaviorManager.runBehavior
     * @param msg	message to the server.
     */
    public void sendMessageToWeb (final String function, String msg) throws IllegalStateException
    {
    	String msgs = "http://" + DashboardMainActivity.address + ":9559/?eval=" + function + "('" + msg.toString() +  "')" ;
    	
    	System.out.println("The message that i want to send to the webpage" + msgs);
    	
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
                TCPClient.this.h.sendMessage(lmsg);
            }

    	}
    	catch (Exception e)	{
    		Log.e(TAG, "ERRROOORRR WEB : " + e);
    	}
		   	
    	this.h = new Handler() {

	            @Override
	            public void handleMessage(Message msg) {
	                // process incoming messages here
	                switch (msg.what) {
	                    case 0:
	                    	//Toast.makeText(getApplicationContext(), (String) msg.obj,Toast.LENGTH_SHORT).show();
	                    	System.out.println("The returned text : " + (String) msg.obj);
	                       	break;
	                }
	                super.handleMessage(msg);
	            }
	        };
    	
    }
    
    /**
     * Queues a message to be sent to the server
     * 
     * @param msg	message to the server.
     */	
    public void sendMessageToServer (StringBuffer msg) throws IllegalStateException 
    {
    	sendMessageToServer (msg.toString());
    }
    
    /**
     * Tell if a connection is established with the server
     * 
     * @return TRUE if connection is active
     */
    public boolean isConnected ()
    {
    	return (_conn_status==STATUS_CONNECTED);
    }
	
    /**
     * Retrieve the number of messages waiting to be shipped to the server
     * 
     * @return the number of messages waiting to be shipped
     */
	public int getMsgPendingCount ()
	{
		synchronized (msg_to_send)
		{
			return msg_to_send.size(); 
		}
	}
		
	 /**
	  * Close current connection, if any
	  */
    public void stop ()
    {
    	Log.i (TAG,"stop(): Requested to close the current connection. Wait for confirmation from send thread.");
    	bbRequestToClose = true;	
    }
    
    /**
     * Stop connection and end thread. 
     */
    public void quit ()
    {
    	Log.i (TAG,"quit(): Requested to stop thread. Wait for confirmation from send thread.");
    
    	bbExitRequest=true;
     	
    	activeWait (1000,100);
		if (bbExited==true) 
		{
			Log.i(TAG,"stop(): send thread has confirmed to stop.");
			return;
		}
   	
		Log.e(TAG,"stop(): TCP Client (send thread) not responding.");
    }
    
    StringBuffer bufferstr = new StringBuffer(1000);
    
    /**
     * Composes the string to send to the server to update a property
     * 
     * @param attrib	Attribute name
     * @param value		Attribute value
     */
    protected void sendPropertyToServer (String attrib, String value)
    {
    	synchronized (bufferstr)
    	{
    		bufferstr.setLength(0);
    		bufferstr.append(attrib);
    		bufferstr.append(' ');
    		bufferstr.append(value);
    		sendMessageToServer (bufferstr);
    	}
    }

    /**
     * Callback:  Receives line from server
     * 
     * Receives all line coming from server, looking for "ERROR". if found,
     * display a notification in the UI, in red
     * 
     * @param line	ASCII String line from the server
     */
    protected void onReceived (String line)
    {
    	Log.i(TAG,"Received from server : " + line);
    	if (line.indexOf("ERROR")!=-1)
    	{
    		displayStatusMessage(true, line);
    	}
    }
     
    /**
     * Updates application status line
     * 
     * @param bbError		TRUE if statusLine represents an error
     * @param statusLine	Status string to display
     */
	protected void displayStatusMessage (boolean bbError, String statusLine)
	{
		Log.e(TAG,(bbError?"ERRORE:":"STATUS:")+statusLine);
		if (observer!=null)
		{
			Message msg = Message.obtain();
			msg.what=MSG_NEWSTATUSMESSAGE;
			msg.obj = statusLine;
			msg.arg2 = token;
			observer.sendMessage(msg);
		}
	}
	
	/**
	 * UI: tell app to refresh connection status
	 * 
	 * @param status	update UI connection status
	 */
	protected void setConnStatus (byte status)
	{
		_conn_status=status;
		if (observer!=null)
		{
			Message msg = Message.obtain();
			msg.what = MSG_COMSTATUSCHANGE;
			msg.obj = this;
			msg.arg2=token;
			observer.sendMessage(msg);
		}
	}  
}
