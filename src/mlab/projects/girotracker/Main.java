/*
 * Copyright (C) 2010 Mercatorlab S.L. 
 *
 * Licensed under the Attribution-ShareAlike 3.0 Unported (CC BY-SA 3.0) (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 *
 *      http://creativecommons.org/licenses/by-sa/3.0/
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 * Girotracker es un tracker diseñado para la Gyroaventura 2012
 * organizada por la Asociación Española de Amigos del Autogiro
 *
 */
package mlab.projects.girotracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Actividad principal de la aplicación. 
 * 
 * @author Santiago Higuera (2012)
 *
 */
public class Main extends Activity implements GpsController,  SensorEventListener {   
	//
	private App app;
	//  Threads and Handlers
	Thread blinkThread, recordThread, transmitThread;
	BlinkHandler blinkHandler;
	RecordHandler recordHandler;
	TransmitHandler transmitHandler;
	// Vibrator
	Vibrator vibrator; 
	SensorManager sensorManager;
	Sensor magnetic;
	Sensor accelerometer;

	
	// UI elements
	TextView labelVelocity, labelBearing, labelLon, labelLat, labelAlt, labelSats, labelStatus;
	ImageButton btnTrack, btnRecord, btnTransmit, btnMap, btnConfig, btnExit;
	ImageView img_brujula;
	
	// GpsManager
	GpsManager gpsManager;
	// LocationRecorder
	BufferManager bufferManager;
	// TransmitManager
	TransmitManager transmitManager;
	
	// Status variables
	boolean isTracking, isRecording, isTransmitting, isFirstFixDone;
	TrackPoint lastTrackPointSent;
	
	/**
	 * Activity Live Cycle Method
	 */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("HAL","Main.onCreate()");
        // Layout
        setContentView(R.layout.main);
        asignViews();
        
        // Referenciar el contexto de la Application
        app=(App)this.getApplicationContext();
        
        // Handlers
        blinkHandler=new BlinkHandler();
        recordHandler=new RecordHandler();
        transmitHandler=new TransmitHandler();        

        // Vibrator
        vibrator =(Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    	magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
        // loadPreferences
        loadPreferences();
                
        // GpsManager
        gpsManager=new GpsManager(this);

        // bufferManager
        bufferManager=new BufferManager(this);
        //bufferManager.emptyBuffer();
        
        // transmitManager
        transmitManager=new TransmitManager(this);
        
        // Status inicial
        isTracking=false;
        isTransmitting=false;
        isRecording=false;
        isFirstFixDone=false;
        lastTrackPointSent= new TrackPoint(app.getUser(), app.getLabel(), new Location(LocationManager.GPS_PROVIDER));     
    }
	private void asignViews() {
		// Asignar View's
        labelLon=(TextView)findViewById(R.id.editLon);
        labelLat=(TextView)findViewById(R.id.editLat);
        labelAlt=(TextView)findViewById(R.id.editAlt);
        labelVelocity=(TextView)findViewById(R.id.editVelocity);
        labelBearing=(TextView)findViewById(R.id.editBearing);
        labelSats=(TextView)findViewById(R.id.editSats);
        labelStatus=(TextView)findViewById(R.id.labelStatus);
        btnTrack=(ImageButton)findViewById(R.id.btnTrack);
        btnRecord=(ImageButton)findViewById(R.id.btnRecord);
        btnTransmit=(ImageButton)findViewById(R.id.btnTransmit);
        btnMap=(ImageButton)findViewById(R.id.btnMap);
        btnConfig=(ImageButton)findViewById(R.id.btnConfig);
        btnExit=(ImageButton)findViewById(R.id.btnExit);
        img_brujula=(ImageView)findViewById(R.id.main_img_brujula);
        
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Log.d("HAL", "Main.onStart()");		
		updateLocationLabels(gpsManager.getLastLocation());
        setButtonTrack(!isTracking);
        setButtonRecord(!isRecording);
        setButtonTransmit(!isTransmitting);
	}
	
	@Override
	protected void onPause() {
		Log.d("HAL","Main.onPause()");
		sensorManager.unregisterListener(this,magnetic);
		sensorManager.unregisterListener(this,accelerometer);
		super.onPause();
	}
	@Override
	protected void onRestart() {
		Log.d("HAL","Main.onRestart()");
		super.onRestart();
	}
	
	@Override
	protected void onResume() {
		Log.d("HAL","Main.onResume()");
        setButtonTrack(!isTracking);
        setButtonRecord(!isRecording);
        setButtonTransmit(!isTransmitting);
        sensorManager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
	}
	
	@Override
	protected void onStop() {
		Log.d("HAL", "Main.onStop()");
		// Guardar las preferencias de usuario
		savePreferences();	
		super.onStop();
	}
	@Override
	protected void onDestroy() {
		Log.d("HAL", "Main.onDestroy()");
    	// Detener el tracking, si está activo
    	if(isTracking) {
    		stopTracking();	
    	}
		super.onDestroy();
    }
	/**
	 *  Carga el fichero con las preferencias de usuario
	 */
 	private void loadPreferences() {
 		Log.d("HAL", "Main.loadPreferences()");
 		try {
	        SharedPreferences settings = getPreferences(0);       
	        app.setUser(settings.getString("user", app.getUser()));
	        app.setPassword(settings.getString("password", app.getPassword()));
	        app.setLabel(settings.getString("label", app.getLabel()));
	        app.setRecordInterval(settings.getInt("recordInterval", app.getRecordInterval()));        
	        app.setTransmitInterval(settings.getInt("gpsTransmitInterval", app.getTransmitInterval()));
	        app.setGpsMinDistance(settings.getFloat("gpsMinDistance", app.getGpsMinDistance()));        
	        app.setGpsMinTime(settings.getLong("gpsMinTime", app.getGpsMinTime()));        
	        app.setBufferSize(settings.getLong("bufferSize", app.getBufferSize()));
 		} catch (Exception e) {
 			Toast.makeText(getApplicationContext(), 
 					"No fué posible cargar el fichero de preferencias", 2000).show();
 			Log.w("HAL", "Main.loadPreferences() - ERROR :"+e.getMessage());
 		}
 	}
 	private void savePreferences() {
 		Log.d("HAL", "Main.savePreferences()");
 		SharedPreferences settings = getPreferences(0);
 		SharedPreferences.Editor editor = settings.edit();
 		editor.putString("user", app.getUser());
 		editor.putString("password", app.getPassword());
 		editor.putString("label", app.getLabel());
 		editor.putInt("recordInterval", app.getRecordInterval());
 		editor.putInt("transmitInterval", app.getTransmitInterval());
 		editor.putLong("gpsMinTime", app.getGpsMinTime());
 		editor.putFloat("gpsMinDistance", app.getGpsMinDistance());
 		editor.putLong("bufferSize", (long)(app.getBufferSize()));
 		// Commit the edits!
 		editor.commit();		
 	}

 	/**
 	 *  Controlador de respuesta al evento onClick de los botones
 	 * @param v Objeto View que activó el evento onClick
 	 */
 	public void onClick(View v) {
 		vibrator.vibrate(50);
     	switch(v.getId()) {
     	case(R.id.btnTrack):
     		toggleTracker();
     		break;
     	case(R.id.btnRecord):
     		toggleRecorder();
     		break;
     	case(R.id.btnTransmit):
     		toggleTransmitter();
     		break;
     	case(R.id.btnMap):
     		//Toast.makeText(getApplicationContext(), "Opción no implementada", 2000).show();
     		//stopTracking();
     		Intent i = new Intent(Main.this, MapquestMap.class);
     		Main.this.startActivity(i);
     		break;
     	case(R.id.btnConfig):
     		stopTracking();
     		Intent j = new Intent(Main.this, Config.class);
     		Main.this.startActivity(j);
     		break;
     	case(R.id.btnExit):
     		finish();
     		break;
     	}
     }
 	
 	// Actualizaciones de pantalla 	
 	/**
 	 * Cumplimiento del Interface GpsController
 	 * El GpsController llama a este método cada vez que dispone de una nueva localización
 	 */
 	@Override 
 	public void updateLocation(Location loc) {
 		Log.d("HAL", "Main.updateLocation()");
 		updateLocationLabels(loc);
 	}
 	/**
 	 * Actualiza la visualización en pantalla de las variables de
 	 * localización
 	 * @param loc Objeto Location actual
 	 */
 	private void updateLocationLabels(Location loc) {
 		Log.d("HAL", "Main.updateLocationLabels()");
		this.labelLon.setText(Halib.lonToString(loc.getLongitude()));
		this.labelLat.setText(Halib.latToString(loc.getLatitude()));
		// Paso velocidades a Km/h
		double v=loc.getSpeed()*3.6;
		this.labelVelocity.setText(String.format("%.0f", v));
		this.labelAlt.setText(String.format("%.0f", loc.getAltitude()));
		this.labelBearing.setText(String.format("%.0f",loc.getBearing()));	
		this.labelSats.setText(String.format("%d/%.0f", this.gpsManager.getNumsats(), this.gpsManager.getAccuracy()));		
	}
 	/**
 	 * Inicializa la etiqueta parpadeante
 	 */
 	private void initLabelStatus() {
 		Log.d("HAL", "Main.initLabelStatus()");
 		updateLabelStatus(View.VISIBLE, Color.RED, statusToString());
 	}
 	/**
 	 * Actualiza el valor de la etiqueta parpadeante
 	 * @param visibility int View.VISIBLE, View.INVISIBLE
 	 * @param color int Color.red
 	 * @param text String Texto de la etiqueta track:record:transmit
 	 */
 	private void updateLabelStatus(int visibility, int color, String text) {
 		labelStatus.setVisibility(visibility);
 		labelStatus.setTextColor(color);
 		labelStatus.setText(text);
 	}
 	private void setButtonTrack(boolean mode) {
		if(mode) {
			btnTrack.setImageResource(R.drawable.buttonoff);			
		} else {
			btnTrack.setImageResource(R.drawable.buttonon);						
		}
		
	}
 	private void setButtonRecord(boolean mode) {
		if(mode) {
			btnRecord.setImageResource(R.drawable.buttonoff);			
		} else {
			btnRecord.setImageResource(R.drawable.buttonon);						
		}
	}
 	private void setButtonTransmit(boolean mode) {
		if(mode) {
			btnTransmit.setImageResource(R.drawable.buttonoff);			
		} else {
			btnTransmit.setImageResource(R.drawable.buttonon);						
		}
	}
	
 	/**
 	 * Entrega la cadena de la etiqueta parpadeante
 	 * @return String cadena track:record:transmit
 	 */
 	private String statusToString() {
 		String cad=(isTracking?"Tracking":"");
		cad+=":";
		cad+=(isRecording ?"Recording":"");
		cad+=":";
		cad+=(isTransmitting?"Transmitting":"");
		return cad;
 	}
 	
 	// Activate deactivate tracking
 	private boolean toggleTracker() {
 		Log.d("HAL", "Main.toggleTracker()");
 		boolean resp= false;
 		if(isTracking) {
 			resp=stopTracking();
 		} else {
 			resp=startTracking();
 		}
 		return resp;
 	}
 	private boolean startTracking() {
		Log.d("HAL", "Main.startTracking()");	
		this.gpsManager.startGpsUpdates();
		if (this.gpsManager.isGpsEnabled()) {
			setButtonTrack(false);
			isTracking=true;
			initLabelStatus();
			blinkThread =new Thread(new Blinker());
			blinkThread.start();			
		} else {
			isTracking=false;
			Log.d("HAL", "    ERROR, no fue posible activar el tracker. El GPS no está activado");
			Halib.alert(this, "ERROR", "No es posible activar el tracker,\n"+
					"El GPS no está activado");
		}
		return isTracking;
	}
	private boolean stopTracking() {
		Log.d("HAL", "MAin.stopTracking()");
		
		if(isTracking) {
			if(isRecording) {
				stopRecording();
			}
			if(isTransmitting) {
				stopTransmitting();
			}
			this.gpsManager.stopGpsUpdates();
			setButtonTrack(true);
			updateLocationLabels(gpsManager.getLastLocation());
			isTracking=false;
		}
		return true;
	}
	
	// Activate, deactivate Recording
	private boolean toggleRecorder() {
		Log.d("HAL", "Main.toggleRecorder()");
 		boolean resp= false;
 		if(isRecording) {
 			resp=stopRecording();
 		} else {
 			resp=startRecording();
 		}
 		return resp;
	}
	private boolean startRecording() {
		Log.d("HAL", "Main.startRecording()");
		if(!isTracking) {
			Log.d("HAL","    Error: You must first activate the tracking");
			Toast.makeText(getApplicationContext(), "You must first activate the tracking", 2000).show();
			return false;
		}
		if(!isRecording) {
			recordThread=new Thread(new Recorder());
			isRecording=true;
			recordThread.start();
			setButtonRecord(false);
		}
		return isRecording;
	}
	private boolean stopRecording() {
		Log.d("HAL", "Main.stopRecording()");
		if(isRecording) {
			if(isTransmitting) {
				stopTransmitting();
			}
			isRecording=false;
			setButtonRecord(true);
		} else {
			return false;
		}
		return true;
	}
	
	// Activate Deactivate Transmitting
	private boolean toggleTransmitter() {
		Log.d("HAL","Main.toggleTransmitter()");
		boolean resp= false;
		if(isTransmitting) {
			resp=stopTransmitting();
		} else {
			resp=startTransmitting();
		}
		return resp;	
	}
	private boolean startTransmitting() {
		Log.d("HAL","Main.startTransmitting()");
		if(!transmitManager.isTransmitterEnabled()) {
			Log.d("HAL","    Error: Transmitter is not enabled");
			Toast.makeText(getApplicationContext(), "Internet is not enabled", 2000).show();
			return false;
		}
		if(!isTracking) {
			Log.d("HAL","    Error: You must first activate the tracking");
			Toast.makeText(getApplicationContext(), "You must first activate the tracking", 2000).show();
			return false;
		}
		if(!isRecording) {
			startRecording();
		}
		
		transmitThread= new Thread(new Transmitter());
		transmitThread.start();
		isTransmitting=true;
		this.setButtonTransmit(false);
		return true;
	}
	private boolean stopTransmitting() {
		Log.d("HAL","Main->stopTransmitting()");
		if(isTransmitting) {
			isTransmitting=false;
			setButtonTransmit(true);
			return true;
		} else {
			return false;
		}
	}
	
    // Runnables
	/**
	 * Runnable para el blinkThread
	 * @author shiguera
	 *
	 */
	private class Blinker implements Runnable {
		@Override
		public void run() {
			Message msg;
			while(isTracking) {
				try { 
					Thread.sleep(600);
					msg = blinkHandler.obtainMessage();
					blinkHandler.sendMessage(msg);
				} catch (Exception e) {
					Log.e("HAL","Fallo en blinkHandler");
					Toast.makeText(getApplication(), "Fallo en blinkThread", 2000).show();
				}
			}
			msg = blinkHandler.obtainMessage();
			blinkHandler.sendMessage(msg);
		}
	}
	/**
	 *  Runnable para RecordThread
	 */
	private class Recorder implements Runnable {
		@Override
		public void run() {
			Log.d("HAL","Main.Recorder.run()");
			Message msg;
			while(isRecording) {
				try { 
					//Log.d("HAL","Main.Recorder.run(): Loop event");
					msg = recordHandler.obtainMessage();
					Main.this.recordHandler.sendMessage(msg);
					Thread.sleep(Main.this.app.getRecordInterval());
				} catch (Exception e) {
					Log.e("HAL","Main.Recorder.run()-ERROR: "+e.getMessage());
					Toast.makeText(getApplication(), "Fallo en Main.GpsRecorder()\n"+e.getMessage(), 2000).show();
				}
			}
			//Log.d("HAL","Main.Recorder.run(): Exiting loop");
		}
	}
	/**
	 *  Class Transmitter: Runnable para el TransmitThread
	 * @author Santiago Higuera (2012)
	 *
	 */
	private class Transmitter implements Runnable {
		@Override
		public void run() {
			Log.d("HAL","Main.Transmitter.Run()");
			Message msg;
			while(isTransmitting) {
				try { 
					msg = transmitHandler.obtainMessage();
					transmitHandler.sendMessage(msg);
					Thread.sleep(Main.this.app.getTransmitInterval());
				} catch (InterruptedException e) {
					Log.e("HAL","Main.Transmitter.run()-ERROR: "+e.getMessage());
					Toast.makeText(getApplication(), "Main.Transmitter.run()-ERROR\n"+e.getMessage(), 2000).show();
				}
			}
		}
	}
	/**
	 * Manejador del evento parpadeo
	 * Recibe la orden del BlinkHandler a través de 
	 * su Runnable Blinker
	 * @author shiguera
	 */
	private class BlinkHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			// Alterna la visibility en cada ciclo
			int vis=(labelStatus.getVisibility()==View.VISIBLE)?View.INVISIBLE:View.VISIBLE;
			int col =Color.WHITE;
			updateLabelStatus(vis, col, statusToString());
		}
	}
	/**
	 * Manejador del evento añadir punto al buffer
	 * La orden de añadir un punto al buffer la da el RecordThread,
	 * a través de su Runnable Recorder.
	 * @author shiguera
	 */
	private class RecordHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Log.d("HAL", "Main.RecordHandler.handleMessage()");
			if(gpsManager.isGpsEventFirstFix()) {
				TrackPoint trackPoint= new TrackPoint(app.getUser(), 
					app.getLabel(), gpsManager.getLastLocation());
				//Log.e("HAL", trackPoint.toString());	
				if(trackPoint.isDiferent(lastTrackPointSent)) {
					//Log.w("HAL","isDiferent");
					bufferManager.addTrackPoint(trackPoint);
					lastTrackPointSent.set(trackPoint);
				}
			} else {
				Log.w("HAL", "Main.RecordHandler.handleMessage(): !isGpsEventFirstFix");	
			}
		}
	}
	/**
	 * Manejador del evento Transmitir.
	 * Recibe la orden de transmitir del TransmitThread a través
	 * de su Runnable Transmitter
	 * @author shiguera
	 *
	 */
	private class TransmitHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Log.d("HAL", "Main.TransmitHandler.handleMessage()");			
			// Comprobar que esta activada la red
			if(transmitManager.isTransmitterEnabled()) {
				// Bloquear el buffer para transmitir
				if(bufferManager.lock()==true) {
					// Recuperar el buffer como cadena 
					String bufferToString=bufferManager.bufferToString();
					// Comprobar si está vacío
					if(bufferToString.length()>0) {
						//Log.d("HAL","Main.TransmitHandler.handleMessage():\n"+  bufferToString);
						// Transmitir el buffer con el TransmitManager
						int result=transmitManager.sendBuffer(bufferToString);					
						//Log.w("HAL","Main.TransmitHandler.handleMessage():\n  executeHttpPost.result:"+result);
						if (result>0) {
							bufferManager.emptyBuffer();
						} else {
							Log.e("HAL","Main.TransmitHandler.handleMessage()-Error al enviar buffer: "+
								String.valueOf(result));
							Toast.makeText(Main.this, "Error "+String.valueOf(result)
								+" sending data", 2000).show();
						}						
					} else {
						Log.w("HAL","Main.TransmitHandler.handleMessage()- Buffer vacío: ");
					}
					bufferManager.unLock();					
				} else {
					Log.w("HAL","Main.TransmitHandler.handleMessage()-WARNING: Buffer Locked");
					Toast.makeText(Main.this, "Buffer locked", 2000).show();
				}
			} else {
				Log.e("HAL","Main.TransmitHandler.handleMessage()-Error: Transmitter not enabled");
				Toast.makeText(Main.this, "Transmitter not enabled", 2000).show();
			}
			
		}
	}
	
	
	//-------- Sensores ---------
	float[] acceleromterVector=new float[3];
	float[] magneticVector=new float[3];
	float[] resultMatrix=new float[9];
	float[] values=new float[3];
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		float azimuth_angle; // = event.values[0];
	    //float pitch_angle = event.values[1];
	    //float roll_angle = event.values[2];		
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			acceleromterVector=event.values;
	        //Log.d("HAL","Accelerometer");
		} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticVector=event.values;
	        //Log.d("HAL","Magnetic");
		}
        SensorManager.getRotationMatrix(resultMatrix, null, acceleromterVector, magneticVector);
        SensorManager.getOrientation(resultMatrix, values);
        // the azimuts
        azimuth_angle =(float) Math.toDegrees(values[0]);
        //Log.d("HAL","Azimuth:"+String.valueOf(azimuth_angle));
        // the pitch
        //y = (float) Math.toDegrees(values[1]);
        // the roll
        ///z = (float) Math.toDegrees(values[2]);  
	        		
        Bitmap bMap = BitmapFactory.decodeResource(getResources(), R.drawable.brujula1);
        Matrix mat = new Matrix();
        mat.postRotate(-azimuth_angle);
        Bitmap bMapRotate = Bitmap.createBitmap(bMap, 0, 0, bMap.getWidth(), bMap.getHeight(), mat, true);
        img_brujula.setImageBitmap(bMapRotate);	
	}
	
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}
}