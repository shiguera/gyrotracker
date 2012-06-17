package mlab.projects.girotracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class SplashActivity extends Activity {
	// Tiempo en milisegundos que se muestra  la pantalla
	private final int SHOWTIME=2000;
	// La variable firstExecution la utilizamos para saber si estamos
	// en el arranque inicial o no
	private Boolean firstExecution = true;

	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			startActivity(new Intent(SplashActivity.this,
					Main.class));
		}
	};

	@Override
	public void onCreate(final Bundle icicle) {
		super.onCreate(icicle);
		this.setContentView(R.layout.splash_activity);
	}

	@Override
	public void onStart() {
		super.onStart();
		// move to the next screen via a delayed message
		if (firstExecution == true) {
			new Thread() {
				@Override
				public void run() {
					handler.sendMessageDelayed(handler.obtainMessage(), SHOWTIME);
				};
			}.start();
		} else {
			finish();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		firstExecution = false;
	}
}
