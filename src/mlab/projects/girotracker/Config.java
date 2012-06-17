package mlab.projects.girotracker;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class Config extends Activity implements View.OnKeyListener, View.OnFocusChangeListener {

	private String DEFAULT_USER="defaultuser";
	private String DEFAULT_PASSWORD="defaultpass";
	private String DEFAULT_LABEL="TrackPrueba";
	private int MIN_RECORD_INTERVAL=5000; // 5 segundos
	private int MAX_RECORD_INTERVAL=600000; // 600 sg= 10 min 
	private int MIN_TRANSMIT_INTERVAL=10000; // 10 segundos
	private int MAX_TRANSMIT_INTERVAL=1200000; // 1200 sg = 20 min
	private long MIN_BUFFERSIZE=(long)(7000); // aprox 70 bytes/punto
	private long MAX_BUFFERSIZE=(long)(140000); 
	
	private RelativeLayout mainpanel;
	private TextView tv_recordinterval, tv_transmitinterval, tv_buffersize, tv_recordingtime, tv_statuslabel;
	private EditText editUser, editPassword, editLabel;
	private SeekBar sb_recordinterval, sb_transmitinterval, sb_buffersize;
	private App app;
	private TransmitManager transmitManager;
	String nick, pass, tracklabel;
	int recordInterval, transmitInterval; // Medidos en msg
	long  bufferSize; // Medido en bytes ; 1 punto aprox 70 bytes
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Leer el archivo config.xml e inicializar las variables relacionadas
		initView();
		
		// Referencia a variables globales
		app = (App)this.getApplicationContext();
		
		// Inicializar variables globales
		nick=app.getUser();
		pass = app.getPassword();
		tracklabel=app.getLabel();
		this.recordInterval=app.getRecordInterval();
		this.transmitInterval=app.getTransmitInterval();
		this.bufferSize=app.getBufferSize();
		
		// Mostrar valores iniciales en pantalla
		editUser.setText(this.nick);
		editPassword.setText(this.pass);		
		editLabel.setText(this.tracklabel);	
		updateSeekBarRecordInterval(this.recordInterval);
		updateSeekBarTransmitInterval(this.transmitInterval);
		updateSeekBarBufferSize(this.bufferSize);		
		updateTextViewRecordingTime();
		
		transmitManager=new TransmitManager(this);
	}
	private void initView() {
		this.setContentView(R.layout.config);
		mainpanel=(RelativeLayout)this.findViewById(R.id.mainpanel);
		// EditText's
		editUser=(EditText)findViewById(R.id.editUser);
		editPassword=(EditText)findViewById(R.id.editPassword);		
		editLabel=(EditText)findViewById(R.id.editLabel);
		editLabel.setOnFocusChangeListener(this);
		// TextView's
		tv_recordinterval=(TextView)this.findViewById(R.id.tv_recordinterval);
		tv_transmitinterval=(TextView)this.findViewById(R.id.tv_transmitinterval);
		tv_buffersize=(TextView)this.findViewById(R.id.tv_buffersize);
		tv_recordingtime=(TextView)this.findViewById(R.id.config_tv_recordingtime);
		tv_statuslabel=(TextView)this.findViewById(R.id.config_lbl_status);
		
		// SeekBar's
		sb_recordinterval=(SeekBar)this.findViewById(R.id.sb_recordinterval);
		sb_recordinterval.setOnSeekBarChangeListener(onSeekBarChangeListener);
		sb_transmitinterval=(SeekBar)this.findViewById(R.id.sb_transmitinterval);
		sb_transmitinterval.setOnSeekBarChangeListener(onSeekBarChangeListener);
		sb_buffersize=(SeekBar)this.findViewById(R.id.sb_buffersize);
		sb_buffersize.setOnSeekBarChangeListener(onSeekBarChangeListener);
		
		// Buttons
		//btn_checkuser=(Button)this.findViewById(R.id.config_btn_checkuser);
		
		
		// Escuchador de tecla pulsada para panel principal
		mainpanel.setOnKeyListener(this);
	}
	private void updateSeekBarRecordInterval(int recordintervalmili) {
		Log.d("HAL","updateSeekBarRecordInterval("+String.format("%d", recordintervalmili)+")");
		double proporcion=((double)(recordintervalmili-MIN_RECORD_INTERVAL))/((double)(MAX_RECORD_INTERVAL-MIN_RECORD_INTERVAL)); 
		int barvalue=(int) Math.round(100.0*proporcion);
		this.sb_recordinterval.setProgress(barvalue);
		String cad=String.format("%d", (int)Math.round((double)recordintervalmili/1000));
		tv_recordinterval.setText(cad);
	}
	private void updateSeekBarTransmitInterval(int transmitintervalmili) {
		double proporcion=((double)(transmitintervalmili-MIN_TRANSMIT_INTERVAL))/((double)(MAX_TRANSMIT_INTERVAL-MIN_TRANSMIT_INTERVAL));
		int barvalue=(int)Math.round(100.0*proporcion);
		this.sb_transmitinterval.setProgress(barvalue);
		String cad=String.format("%d",(int)Math.round((double)transmitintervalmili/1000.0));
		tv_transmitinterval.setText(cad);
	}
	private void updateSeekBarBufferSize(long buffersizebytes) {
		double proporcion=((double)(buffersizebytes-MIN_BUFFERSIZE))/((double)(MAX_BUFFERSIZE-MIN_BUFFERSIZE));
		int barvalue=(int)Math.round(100.0*proporcion);
		this.sb_buffersize.setProgress(barvalue);
		String cad=String.format("%d", Math.round((double)buffersizebytes/70.0));
		tv_buffersize.setText(cad);		
	}
	private void updateTextViewRecordingTime() {
		int numpuntos=(int)Math.round((double)(bufferSize)/70.0);
		int segundos=recordInterval/1000*numpuntos;
		int horas=0;
		int minutos=0;
		if(segundos>3600) {
			horas=(int)Math.floor((double)segundos/3600.0);
			segundos=segundos-horas*3600;
		}
		if(segundos>60) {
			minutos=(int)Math.floor(((double)segundos)/60.0);
			segundos=segundos-minutos*60;
		}
		String cad = String.format("%02d:%02d:%02d",horas,minutos,segundos);
		tv_recordingtime.setText(cad);
	}
	private void updateStatusLabel(String msg) {
		this.tv_statuslabel.setText(msg);
	}
	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onStop() {	
		checkUser();		
		updateGlobals();
		super.onStop();	
	}
	
	public void onClick(View v) {
		Vibrator vibrator =(Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		vibrator.vibrate(50);
		switch(v.getId()) {
		case(R.id.config_btn_done):
			finish();
		case(R.id.config_btn_checkuser):
			checkUser();
			break;
		}
	}
	
	private OnSeekBarChangeListener onSeekBarChangeListener = new OnSeekBarChangeListener() {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			int interval;
			if(seekBar==sb_recordinterval) {
				//Config.this.updateSeekBarRecordInterval(progress*1000);
				interval=(int)Math.round((MAX_RECORD_INTERVAL-MIN_RECORD_INTERVAL));
				recordInterval=(int)Math.round(((double)MIN_RECORD_INTERVAL+(double)interval*(double)progress/100.0));					
				Config.this.updateSeekBarRecordInterval(recordInterval);
			} else if (seekBar==sb_transmitinterval) {
				interval=(int)Math.round((MAX_TRANSMIT_INTERVAL-MIN_TRANSMIT_INTERVAL));
				transmitInterval=(int)Math.round(((double)(MIN_TRANSMIT_INTERVAL)+(double)interval*(double)progress/100.0));				
				Config.this.updateSeekBarTransmitInterval(transmitInterval);
			} else {
				long interv=Math.round((MAX_BUFFERSIZE-MIN_BUFFERSIZE));
				bufferSize=Math.round(((double)(MIN_BUFFERSIZE)+(double)interv*(double)progress/100.0));
				Config.this.updateSeekBarBufferSize(bufferSize);
			}
			updateTextViewRecordingTime();
		}
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
		}
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub			
		}
	};
	
	private void updateGlobals() {
	
		// User, password
		try {
			app.setUser(editUser.getText().toString().trim());
			app.setPassword(editPassword.getText().toString().trim());
			app.setLabel(editLabel.getText().toString().trim());
		} catch (Exception e) {
			app.setUser(this.DEFAULT_USER);
			app.setPassword(this.DEFAULT_PASSWORD);
			app.setLabel(this.DEFAULT_LABEL);
		}
		app.setRecordInterval(recordInterval);
		app.setTransmitInterval(transmitInterval);
		app.setBufferSize(bufferSize);
	}
	
	private int checkUser() {
		int resp=0;
		String msg="";
		if(transmitManager.isTransmitterEnabled()) {			
			try {
				nick=editUser.getText().toString().trim();
				pass=editPassword.getText().toString().trim();
			} catch (Exception e) {
				Toast.makeText(getApplicationContext(), "user or password contains errors", 2000).show();
				//nick=app.DEFAULT_USER;
				//pass=app.DEFAULT_PASSWORD;
			}
			resp=transmitManager.checkUser(nick, pass);
			msg="Cheking user, pasword: ";
			if (resp==1) {
				msg+="OK";
			} else {
				msg+="ERROR";
			}
		} else {
			resp=-1;
			msg="Internet is not available. Check connection";
		}
		Toast.makeText(getApplicationContext(), msg, 2000).show();
		updateStatusLabel(msg);
		return resp;
	}
	
	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
//		final int view = v.getId();
//        switch (view) {
//            case R.id.editRecordInterval:
//            	
//                break;
//            case R.id.editTransmitInterval:
//                break;
//
//        }
		return false;
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		Log.d("HAL","Config.onFocusChange()");
		final int view = v.getId();
		if(!hasFocus) {
	        switch (view) {	
	        	case R.id.editUser:
	        		//updateUser();
	        		break;
	        	case R.id.editPassword:
	            	//updatePassword();
	                break;
	        	case R.id.editLabel:
	            	//updateLabel();
	                break;
	        }
		}		
	}	
}
