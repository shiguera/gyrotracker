package mlab.projects.girotracker;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

public class GpsManager {

	// main:  actividad que instancia el GpsManager.
	// Tiene que implementar el interface GpsController
	private GpsController main;
	
	// locationManager
	private LocationManager locationManager;

	//private LocationProvider locationProvider;
	
	// minTime : the minimum time interval for notifications, in milliseconds. 
	// This field is only used as a hint to conserve power, 
	// and actual time between location updates may be greater or lesser than this value.
	private long minTime;
	
	// minDistance : the minimum distance interval for notifications, in meters
	private float minDistance;
	
	// gpsEventFirstFix : Indica si ya se ha producido el evento FIRST_FIX
	private boolean gpsEventFirstFix;
	
	// lastLocation: Almacena el último objeto location leido del GPS
	private Location lastLocation;

	// gpsStatus: Almacena el ultimo objeto GpsStatus leido del GPS
	private GpsStatus gpsStatus;
	
	// gpsSatellite: Lista de satelites utilizada en el último location (lastLocation)
	private ArrayList<GpsSatellite> gpsSatellite; 
	
	// numpuntos: Número de puntos leidos desde que se activó el GPS
	private int numpuntos;
	
	// Constructor
	GpsManager(GpsController activ) {
		Log.d("HAL","GpsManager.builder()");
		this.main=activ;
		this.minTime=10;
		this.minDistance=10.0f;
		this.initGps();
		lastLocation=new Location(LocationManager.GPS_PROVIDER);
		gpsSatellite=new ArrayList<GpsSatellite>();
		gpsEventFirstFix=false;
	}		
	
	// initGps() 
	//        Inicializa el LocationManager del GPS
	//    Devuelve:
	//        true: Si la inicialización se realizó sin problemas
	//        false: en caso contrario
	public boolean initGps() {
        Log.d("HAL","GpsManager.initGps()");
		this.locationManager = (LocationManager) ((Activity)this.main).getSystemService(Context.LOCATION_SERVICE);
        if (isGpsEnabled()) {
        	//this.locationProvider = this.locationManager.getProvider(LocationManager.GPS_PROVIDER);       	
        	Log.d("HAL","GpsManager.initGps(): isGpsEnabled=true");
        	return true;
        } else {
        	Log.w("HAL","GpsManager.initGps()-WARNING: isGpsEnabled=false");
        	return false;
        }
	}
	
	// isGpsEnabled()
	//    Devuelve:
	//        true: si el proveedor Gps está disponible
	//        false: en caso contrario
	public boolean isGpsEnabled() {
        try {
        	return this.locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
        	return false;
        }
	}
	
	// isGpsEventFirsFix()
	//    Devuelve:
	//        true: si el evento FIRST_FIX ya se ha producido
	//        false: en caso contrario
	public boolean isGpsEventFirstFix() {
		return gpsEventFirstFix;
	}
	
	// startGpsUpdates()
	//
	public boolean startGpsUpdates() {
		Log.d("HAL","GpsManager.startGpsUpdates()");
		if(!isGpsEnabled()) {
			initGps();
		}
		if (isGpsEnabled()) {
        	this.locationManager.addGpsStatusListener(this.gpsStatusListener);
            //Log.d("HAL", "    GpsStatusListener asignado");
        	this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, this.minTime, 
        			this.minDistance, this.locationListener);
            //Log.d("HAL", "    GpsManager: requestLocationUpdates started");
        	return true;
        } else {
        	return false;
        }
	}
	public void stopGpsUpdates() {
		// Detener actualizaciones del GPS
		Log.d("HAL","GpsManager.stopGpsUpdates()");
		if (this.locationManager != null) {
			//Log.d("HAL", "    GpsManager: Removing updates");
			this.locationManager.removeUpdates(this.locationListener);
			//Log.d("HAL", "    GpsManager: Removed locationListener");
			this.locationManager.removeGpsStatusListener(this.gpsStatusListener);
			//Log.d("HAL", "    GpsManager: Removed gpsStatusListener");			
		}
		this.gpsSatellite = new ArrayList<GpsSatellite>();
	}
	
	//
	// gpsStatusListener
	//
	Listener gpsStatusListener = new GpsStatus.Listener() {		
		
		@Override
		public void onGpsStatusChanged(int event) {
			String text="";
			switch(event) {
			case(GpsStatus.GPS_EVENT_SATELLITE_STATUS):
				text="GPS_EVENT_SATELLITE_STATUS";
				GpsStatus status = locationManager.getGpsStatus(null);
				updateGpsStatus(status);				
				break;
			case(GpsStatus.GPS_EVENT_STARTED):
				text="GPS_EVENT_STARTED";
				break;
			case(GpsStatus.GPS_EVENT_STOPPED):
				text="GPS_EVENT_STOPED";
				break;
			case(GpsStatus.GPS_EVENT_FIRST_FIX):
				text="GPS_EVENT_FIRST_FIX";
				GpsManager.this.gpsEventFirstFix=true;
				break;
			}
			//Log.d("HAL", "GpsManager.onGpsStatusChanged(): "+text);
		}
	};
	
	// updateGpsStatus()
	//        Actualiza los valores de gpsStatus, satelites y otros
	private void updateGpsStatus(GpsStatus status) {
		this.gpsStatus=status;
		Iterable<GpsSatellite> sats = gpsStatus.getSatellites();
		Iterator<GpsSatellite> it = sats.iterator();
		gpsSatellite = new ArrayList<GpsSatellite>();
		while(it.hasNext()) {
			GpsSatellite sat = (GpsSatellite) it.next();
			gpsSatellite.add(sat);
		}
	}

	private void updateLocation(Location loc) {
		// Actualizar lastLocation
		lastLocation.set(loc);
		// Cambio la fecha por el bug detectado en la fecha UTC de los gps de Samsung Galaxy
    	Date now = new Date();
        long tt=now.getTime();
        lastLocation.setTime(tt);
    	main.updateLocation(lastLocation);
    	// Actualizar numero de puntos leidos
    	numpuntos++;
	}
	
	//
	// locationListener
	//
	LocationListener locationListener = new LocationListener() {
	    public void onLocationChanged(Location loc) {
	    	Log.d("HAL","LocationListener.onLocationChanged():\n"+
	    			"  P"+String.valueOf(numpuntos)+"-"+Halib.locToString(loc));
	    	updateLocation(loc);
	    }
	    public void onStatusChanged(String provider, int status, Bundle extras) {
	    	switch(status) {
	    	case(LocationProvider.AVAILABLE):
	    		Log.d("HAL","LocationListener.onStatusChanged(): LocationProvider.AVAILABLE");
	    		
	    		break;
	    	case(LocationProvider.TEMPORARILY_UNAVAILABLE):
	    		Log.d("HAL","LocationListener.onStatusChanged(): LocationProvider.TEMPORARILY_UNAVAILABLE");

	    		break;
	    	case(LocationProvider.OUT_OF_SERVICE):
	    		Log.d("HAL","LocationListener.onStatusChanged(): LocationProvider.OUT_OF_SERVICE");
	    		
	    		break;
	    	}
	    	//Toast.makeText(getApplicationContext(), "Status Changed", 2000).show();
	    }
	    public void onProviderEnabled(String provider) {
	    	Log.d("HAL","LocationListener.onProviderEnabled()");
	    }	
	    public void onProviderDisabled(String provider) {
	    	Log.d("HAL","LocationListener.onProviderDisabled()");
	    }
	};
	
	public long getMinTime() {
		return minTime;
	}
	public void setMinTime(long minTime) {
		this.minTime = minTime;
	}
	public float getMinDistance() {
		return minDistance;
	}
	public void setMinDistance(float minDistance) {
		this.minDistance = minDistance;
	}
	public int getNumsats() {
		return gpsSatellite.size();
	}
	public Location getLastLocation() {
		return lastLocation;
	}
	public float getAccuracy() {
		return lastLocation.getAccuracy();
	}
	public int getNumpuntos() {
		return numpuntos;
	}
	
}
