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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Toast;

public class WanderTweet extends Activity implements OnClickListener, OnInitListener{

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		View button = this.findViewById(R.id.speak_button);
		button.setOnClickListener(this);		
		button = this.findViewById(R.id.twitter_button);
		button.setOnClickListener(this);
		button = this.findViewById(R.id.toggle_service_button);
		button.setOnClickListener(this);

		setupTextToSpeech();
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.speak_button:
			if (mTts == null) {
				setupTextToSpeech();
			}
			else
			{
				mTts.speak(getString(R.string.text_to_speak), TextToSpeech.QUEUE_ADD, null);
			}
			break;

		case R.id.twitter_button:
			testTwitter();
			break;

		case R.id.toggle_service_button:
			toggleService();
			break;
		}
	}

	public void onInit(int arg0) {
		// TODO Auto-generated method stub

	}

	protected void onActivityResult(
			int requestCode, int resultCode, Intent data) {
		if (requestCode == CHECK_TTS_SUPPORTED) {
			installOrStartTextToSpeech(resultCode);
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		String s = intent.getAction();
		if(s!=null && s.equals("android.intent.action.VIEW"))
		{
			Uri uri = intent.getData();
			try {
				authenticateTwitter(uri);
			} catch (TwitterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void setupTextToSpeech() {
		Toast.makeText(this, "Checking for Text To Speech packages...", 2000);
		Intent checkIntent = new Intent();
		checkIntent
		.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		startActivityForResult(checkIntent, CHECK_TTS_SUPPORTED);
	}

	private void installOrStartTextToSpeech(int resultCode) {
		if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
			// missing data, install it
			Intent installIntent = new Intent();
			installIntent.setAction(
				TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
			startActivity(installIntent);
			finish();
		} 			
		else{
			startTextToSpeech();
			startTwitterAuthentication();
		}
	}

	private void startTextToSpeech() {
		mTts = new TextToSpeech(this, this);
		mTts.setLanguage(Locale.UK);
	}

	private void toggleService() {
		Intent i = new Intent(this, WanderTweetService.class);
		WanderTweetService.TEXT_TO_SPEECH = mTts;
		WanderTweetService.TWITTER = mTwitter;
		if(isMyServiceRunning()){
			stopService(i);}
		else{ 
			startService(i);
		}
	}

	private void startTwitterAuthentication() {
		TextView tv = (TextView)findViewById(R.id.textView1);
		try {
			mTwitter = new TwitterFactory().getInstance();
			mTwitter.setOAuthConsumer("QLPtTUqsMOHFvm362ML88A", "Rg4aeXYbiRVyg85dhiAKkvvQPojVTRAl315TjjFbU");
			
			SharedPreferences preferences = getPreferences(MODE_PRIVATE);
			String accessToken = preferences.getString("accessToken", null);
			String accessSecret = preferences.getString("accessSecret", null);
			
			if(accessToken != null && accessSecret != null)
			{
				AccessToken t = new AccessToken(accessToken, accessSecret);
				mTwitter.setOAuthAccessToken(t);
			}
			else
			{
			requestToken = mTwitter.getOAuthRequestToken("WanderTweet://connect");
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthorizationURL())));
			}
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

	private void authenticateTwitter(Uri uri) throws TwitterException {
		String verifier = uri.getQueryParameter("oauth_verifier");
		AccessToken accessToken = mTwitter.getOAuthAccessToken(requestToken, verifier);
		String token = accessToken.getToken();
		String secret = accessToken.getTokenSecret();
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("accessToken", token);
		editor.putString("accessSecret", secret);
		editor.commit();
		mTwitter.setOAuthAccessToken(accessToken);
	}

	private void testTwitter() {
		TextView tv = (TextView)findViewById(R.id.textView1);
		try {
			Query query = new Query("geocode:52.037313,-0.792046,0.5km");
			QueryResult result = mTwitter.search(query);
			String output = ""; 
			for (Tweet tweet : result.getTweets()) {
				String tweetMessage = tweet.getFromUser() + ":" + tweet.getText() + "\n";
	
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

	private boolean isMyServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if ("com.github.WanderTweet.WanderTweetService".equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private TextToSpeech mTts;

	private Twitter mTwitter;

	private RequestToken requestToken;

	private static final int CHECK_TTS_SUPPORTED = 0;

}