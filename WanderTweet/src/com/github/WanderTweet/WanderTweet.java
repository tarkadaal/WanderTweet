package com.github.WanderTweet;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Tweet;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class WanderTweet extends Activity implements OnClickListener, OnInitListener{
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Intent i = new Intent(this, WanderTweetService.class);
		startService(i);

		setupTextToSpeech();

		View button = this.findViewById(R.id.speak_button);
		button.setOnClickListener(this);		
		button = this.findViewById(R.id.twitter_button);
		button.setOnClickListener(this);
		button = this.findViewById(R.id.toggle_service_button);
		button.setOnClickListener(this);

		setupLocationInformation();
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.speak_button:
			if (mTts == null) {
				setupTextToSpeech();
			}
			else
			{
				speak(getString(R.string.text_to_speak));
			}
			break;

		case R.id.twitter_button:
			testTwitter();
			break;

		case R.id.toggle_service_button:
			Intent i = new Intent(this, WanderTweetService.class);
			if(isMyServiceRunning())
			{
				stopService(i);}
			else{ 
				startService(i);
			}
			break;
		}
	}

	public void onInit(int arg0) {
		// TODO Auto-generated method stub

	}

	protected void onActivityResult(
			int requestCode, int resultCode, Intent data) {
		if (requestCode == CHECK_TTS_SUPPORTED) {
			startOrInstallTextToSpeech(resultCode);
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		String s = intent.getAction();
		if(s!=null && s.equals("android.intent.action.VIEW"))
		{
			Uri uri = intent.getData();
			authenticateAndTest(uri);
		}
	}

	private void setupTextToSpeech() {
		Intent checkIntent = new Intent();
		checkIntent
		.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		startActivityForResult(checkIntent, CHECK_TTS_SUPPORTED);
	}

	private void startOrInstallTextToSpeech(int resultCode) {
		if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
			// success, create the TTS instance
			mTts = new TextToSpeech(this, this);
			mTts.setLanguage(Locale.UK);
			mTts.speak("Initial setup", TextToSpeech.QUEUE_ADD, null);

		} else {
			// missing data, install it
			Intent installIntent = new Intent();
			installIntent.setAction(
				TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
			startActivity(installIntent);
		}
	}

	private void speak(String text) {
		mTts.speak(text, TextToSpeech.QUEUE_ADD, null);
	}

	private void testTwitter(){
		TextView tv = (TextView)findViewById(R.id.textView1);
		tv.setText("You hit the right Button!");
		tv.invalidate();
		try {
			twitter = new TwitterFactory().getInstance();
			twitter.setOAuthConsumer("QLPtTUqsMOHFvm362ML88A", "Rg4aeXYbiRVyg85dhiAKkvvQPojVTRAl315TjjFbU");
			requestToken = twitter.getOAuthRequestToken("WanderTweet://connect");
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthorizationURL())));

		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			tv.setText(e.getMessage());
			tv.invalidate();
		} catch (TwitterException e) {
			// TODO Auto-generated catch block
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			tv.setText(errors.toString());
			tv.invalidate();
		}

	}

	private void authenticateAndTest(Uri uri) {
		TextView tv = (TextView)findViewById(R.id.textView1);
		try {
			String verifier = uri.getQueryParameter("oauth_verifier");
			AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);
			String token = accessToken.getToken();
			String secret = accessToken.getTokenSecret();
			twitter.setOAuthAccessToken(accessToken);
			String status = twitter.getScreenName();
			String message = "You are logged into Twitter. Successfully got screen name: " + status ;

			speak(message);

			Query query = new Query("geocode:52.037313,-0.792046,0.5km");
			QueryResult result = twitter.search(query);
			String output = ""; 
			for (Tweet tweet : result.getTweets()) {
				String tweetMessage = tweet.getFromUser() + ":" + tweet.getText() + "\n";
				speak(tweetMessage);
				output += tweetMessage;
			}
			tv.setText(output);
			tv.invalidate();
		}
		catch (TwitterException e){
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			tv.setText(errors.toString());
			tv.invalidate();
		}
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

		locationManager.requestLocationUpdates(provider, 1000 * 60 * 10, 0, locationListener);

	}

	private TextToSpeech mTts;

	private void makeUseOfNewLocation(Location location) {
		// TODO Auto-generated method stub
		TextView tv = (TextView)findViewById(R.id.textView1);
		tv.setText(location.toString());
		tv.invalidate();
	}

	private boolean isMyServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if ("com.github.WanderTweet.WanderTweetService".equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private Twitter twitter;

	private RequestToken requestToken;

	private static final int CHECK_TTS_SUPPORTED = 0;

}