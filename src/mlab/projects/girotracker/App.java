package mlab.projects.girotracker;

import android.app.Application;

public class App extends Application {
	
	public String DEFAULT_USER="defaultuser";
	public String DEFAULT_PASSWORD="defaultpass";
	public String DEFAULT_LABEL="TrackPrueba";
	public int MIN_RECORD_INTERVAL=5000; // 5 segundos
	public int MAX_RECORD_INTERVAL=600000; // 600 sg= 10 min 
	public int DEFAULT_RECORD_INTERVAL=100000; // 10 SEGUNDOS
	public int MIN_TRANSMIT_INTERVAL=10000; // 10 segundos
	public int MAX_TRANSMIT_INTERVAL=1200000; // 1200 sg = 20 min
	public int DEFAULT_TRANSMIT_INTERVAL=60000; // 1 min=60 sg= 60000 msg
	public long MIN_BUFFERSIZE=(long)(7000); // aprox 70 bytes/punto
	public long MAX_BUFFERSIZE=(long)(140000); 
	public long DEFAULT_BUFFERSIZE= (long) 35000; // 500 puntos = 500x70 = 35000 bytes

	@Override
	public void onCreate() {
		super.onCreate();	
	}
	
	// Parámetros de configuración
	// Usuario de la base de datos
	private String user=DEFAULT_USER;
	private String password=DEFAULT_PASSWORD;
	private String label=DEFAULT_LABEL;
	
	// Parametros para RecordManager
	// Intervalo en milisg entre registros de posición sucesivos
	private int recordInterval=DEFAULT_RECORD_INTERVAL;
	// Parametros para TransmitManager
	// Intervalo en milisg entre transmisiones sucesivas
	private int transmitInterval=DEFAULT_TRANSMIT_INTERVAL;
	
	// Parámetros para GpsManager
	// Distancia mínima en m que fuerza la actualización de posición con el GPS
	private float gpsMinDistance=10;
	// gpsMinTime : sg entre actualizaciones del GPSManager
	private long gpsMinTime=0;
	// Parámetros para BufferManager
	// Longitud en bytes del buffer. 
	// El número de puntos : aprox 70 bytes/punto
	private long bufferSize=DEFAULT_BUFFERSIZE; // 500 puntos= 70x500 = 35000 bytes
	
	
	public long getGpsMinTime() {
		return gpsMinTime;
	}

	public void setGpsMinTime(long gpsMinTime) {
		this.gpsMinTime = gpsMinTime;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String labell) {
		this.label = labell;
	}

	public int getRecordInterval() {
		return recordInterval;
	}

	public void setRecordInterval(int recordInterval) {
		this.recordInterval = recordInterval;
	}

	public void setGpsMinDistance(float minDistance) {
		this.gpsMinDistance = minDistance;
	}
	
	public float getGpsMinDistance() {
		return gpsMinDistance;
	}

	public int getTransmitInterval() {
		return transmitInterval;
	}

	public void setTransmitInterval(int transmitInterval) {
		this.transmitInterval = transmitInterval;
	}

	public long getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(long buffSize) {
		this.bufferSize = buffSize;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUser() {
		return this.user;
	}

	public void setUser(String user) {
		this.user = user;
	}
}
