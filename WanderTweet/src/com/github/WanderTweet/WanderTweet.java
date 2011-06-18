package com.github.WanderTweet;

import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.View;
import android.view.View.OnClickListener;

public class WanderTweet extends Activity implements OnClickListener, OnInitListener{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
		View addButton = this.findViewById(R.id.speak_button);
		addButton.setOnClickListener(this);
    }
    
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.speak_button:
			if (mTts == null) {
				Intent checkIntent = new Intent();
				checkIntent
						.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
				startActivityForResult(checkIntent, CHECK_TTS_SUPPORTED);
			}
			else
			{
				speak();
			}
			break;
		}
	}
	
	protected void onActivityResult(
	        int requestCode, int resultCode, Intent data) {
	    if (requestCode == CHECK_TTS_SUPPORTED) {
	        if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
	            // success, create the TTS instance
	            mTts = new TextToSpeech(this, this);
	            mTts.setLanguage(Locale.UK);
	            speak();
	            
	        } else {
	            // missing data, install it
	            Intent installIntent = new Intent();
	            installIntent.setAction(
	                TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
	            startActivity(installIntent);
	        }
	    }
	}

	private void speak() {
		mTts.speak(getString(R.string.text_to_speak), TextToSpeech.QUEUE_ADD, null);
	}

	private TextToSpeech mTts;
	private static final int CHECK_TTS_SUPPORTED = 0;
	@Override
	public void onInit(int arg0) {
		// TODO Auto-generated method stub
		
	}
}