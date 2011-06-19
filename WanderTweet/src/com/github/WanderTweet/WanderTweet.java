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
import twitter4j.conf.ConfigurationBuilder;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiConfiguration.Status;
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

		setupTextToSpeech();

		View addButton = this.findViewById(R.id.speak_button);
		addButton.setOnClickListener(this);		
		View twitterButton = this.findViewById(R.id.twitter_button);
		twitterButton.setOnClickListener(this);
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
		Uri uri = intent.getData();
		authenticateAndTest(uri);
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
				output += tweet.getFromUser() + ":" + tweet.getText() + "\n";
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

	private TextToSpeech mTts;

	private Twitter twitter;

	private RequestToken requestToken;

	private static final int CHECK_TTS_SUPPORTED = 0;

}