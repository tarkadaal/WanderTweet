package com.github.WanderTweet;

import java.util.List;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Tweet;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;


public class WanderTweetService extends Service {
	/**
	 * Class for clients to access.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with
	 * IPC.
	 */
	public class LocalBinder extends Binder {
		WanderTweetService getService() {
			return WanderTweetService.this;
		}
	}

	@Override
	public void onCreate() {
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		// Display a notification about us starting.  We put an icon in the status bar.
		CharSequence text = getText(R.string.local_service_started);
		showNotification(text);
		
		setupForegroundStatus();
		setupLocationInformation();

		// Start up the thread running the service.  Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block.  We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		HandlerThread thread = new HandlerThread("ServiceStartArguments", 
			android.os.Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		// Get the HandlerThread's Looper and use it for our Handler 
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("LocalService", "Received start id " + startId + ": " + intent);

		Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

		// For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the job
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		mServiceHandler.sendMessage(msg);

		// If we get killed, after returning from here, restart
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		// Cancel the persistent notification.
		mNM.cancel(NOTIFICATION);

		// Tell the user we stopped.
		Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
		mServiceHandler.stop();
		SharedReferences.TEXT_TO_SPEECH.stop();		
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	private void setupForegroundStatus() {
		// TODO Auto-generated method stub
		Notification n = buildNotification("WanderTweet is running!");
		startForeground(NOTIFICATION, n);
	}

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification(CharSequence text) {
		// In this sample, we'll use the same text for the ticker and the expanded notification


		Notification notification = buildNotification(text);

		// Send the notification.
		mNM.notify(NOTIFICATION, notification);
	}

	private Notification buildNotification(CharSequence text) {
		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.icon, text,
			System.currentTimeMillis());


		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
			new Intent(this, WanderTweet.class), 0);


		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.local_service_label),
			text, contentIntent);
		return notification;
	}
	
	private void setupLocationInformation() {
		// Acquire a reference to the system Location Manager
		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

		// Define a listener that responds to location updates
		LocationListener locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				// Called when a new location is found by the network location provider.
				makeUseOfNewLocation(location);
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {}

			public void onProviderEnabled(String provider) {}

			public void onProviderDisabled(String provider) {}
		};

		// Register the listener with the Location Manager to receive location updates
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		String provider = locationManager.getBestProvider(criteria, true);

		locationManager.requestLocationUpdates(provider, 1000 * 60 * 2, 0, locationListener);
	}

	private void makeUseOfNewLocation(Location location) {
		mLocation = location;
	}

	private NotificationManager mNM;

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private int NOTIFICATION = R.string.local_service_started;

	// This is the object that receives interactions from clients.  See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();
	
	private Location mLocation = new Location("default");
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	
	
	// Handler that receives messages from the thread
	private final class ServiceHandler extends Handler {
	
		public ServiceHandler(Looper looper) {
			super(looper);
		}
		
		@Override
		public void handleMessage(Message msg) {
			// Normally we would do some work here, like download a file.
			// For our sample, we just sleep for 5 seconds.
	
			try{
				Thread.sleep(5000); //5 sec pause for the LocationListener to fire - can't be bothered doing it properly right at the minute.
				Integer count = 0;
				while(mContinue)
				{
					count = queryTwitterAndSpeakResults(count);
	
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
	
			}
	
			// Stop the service using the startId, so that we don't stop
			// the service in the middle of handling another job
			stopSelf(msg.arg1);
		}

		public void stop()
		{
			mContinue=false;
		}
	
		private Integer queryTwitterAndSpeakResults(Integer count)
				throws TwitterException, InterruptedException {
			String queryString = 
				"geocode:" + 
				mLocation.getLatitude() + 
				"," + 
				mLocation.getLongitude() + 
				",0.5km";
			Query query = new Query(queryString);
			QueryResult result;
		
			result = SharedReferences.TWITTER.search(query);
		
			List<Tweet> tweets = result.getTweets();
			Integer size = tweets.size();
			SharedReferences.TEXT_TO_SPEECH.speak("Found " + size.toString() + " tweets." , TextToSpeech.QUEUE_ADD, null);
			if(size > 0)
			for (Tweet tweet : tweets.subList(0, size < 9 ? size : 9 )) {
				String tweetMessage = tweet.getFromUser() + ":" + tweet.getText() + "\n";
				String message = "This is message number "	+ count.toString() + ".   " + tweetMessage;
				SharedReferences.TEXT_TO_SPEECH.speak(message, TextToSpeech.QUEUE_ADD, null);
				SharedReferences.TEXT_TO_SPEECH.playSilence(10000, TextToSpeech.QUEUE_ADD, null);
				count++;
			}
		
			Thread.sleep(120000);
			return count;
		}

		private Boolean mContinue = true;
	}
}


