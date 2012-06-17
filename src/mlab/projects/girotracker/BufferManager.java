package mlab.projects.girotracker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

/**
 * Clase para gestionar el buffer de posiciones almacenadas
 * @author shiguera
 *
 */
public class BufferManager {
	
	/**
	 *  Nombre del fichero disco utilizado
	 */
	private final String BUFFER_FILE_NAME="buffer"; 
	
	/**
	 *  Tamaño máximo del fichero disco. 
	 *  Definido en App. Se necesitan aproximadamente 70 bytes/location
	 */
	private long maxBufferSize; 
	
	/**
	 *  Número de líneas que se borraran al principio del buffer 
	 *  para hacer sitio cuando este lleno (por defecto el 20%)
	 */
	private int emptyLines; 
	
	/**
	 *  Acceso a las variables globales de la aplicación
	 */
	private App app;
	
	/**
	 *  Actividad principal de la aplicacion
	 */
	private Activity main;
	
	/**
	 *  Se gestiona el bloqueo del fichero, para evitar
	 *  que se añadan cadenas cuando se está en mitad de un envío
	 */
	private boolean isLocked;
	
	
	/**
	 *  Constructor de la clase. 
	 * @param mainActivity La Actividad principal de la aplicación
	 *     que instancia el BuferManager
	 */
	BufferManager(Activity mainActivity) {
		Log.d("HAL","BufferManager.builder()");
		this.main=mainActivity;
		this.app=(App)main.getApplicationContext();
		maxBufferSize=app.getBufferSize();
		emptyLines=(int)Math.floor(maxBufferSize*0.2/70);
		// Comprobar si existe el fichero del buffer, si no crearlo
		if(Halib.existsFile(main, BUFFER_FILE_NAME)==false) {
			Log.d("HAL","BufferManager.builder(): Creating a new buffer file");	
			createBufferFile();
		}
		isLocked=false;
	}
	
	/**
	 * Añade una cadena de localización al buffer a partir de 
	 * un objeto TrackPoint
	 * @param trackPoint Objeto TrackPoint que define la localización que 
	 * se quiere añadir al buffer
	 * @return 1 si el punto se añade al buffer
	 * 0 o negativo si no fue posible añadir el punto (por estar bloqueado
	 * el fichero u otro motivo)
	 */
	public int addTrackPoint(TrackPoint trackPoint) {
		int resp=0;
		boolean lockStatus=isLocked;
		if (!isLocked) {
			isLocked=true;
			String content=trackPoint.toString()+"\n";
			resp=Halib.appendToFile(main, BUFFER_FILE_NAME, content);
			Log.d("HAL","BufferManager.addTrackPoint():\n  "+content);
			checkBufferSize();
		} else {
			Log.w("HAL","BufferManager.addTrackPoint()-WARNING: Point not added. File Locked");	
			resp=-1;
		}
		isLocked=lockStatus;
		return resp;
	}
	private void checkBufferSize() {
		long buffsize=Halib.fileSize(main, BUFFER_FILE_NAME);
		Log.d("HAL","BufferManager.checkBufferSize(): "+String.valueOf(buffsize));
		if(buffsize>this.maxBufferSize) {
			isLocked=true;	
			deleteLines(this.emptyLines);
			isLocked=false;
		}
	}
	private int deleteLines(int linesToDelete) {
		Log.d("HAL","BufferManager.deleteLines():"+String.valueOf(linesToDelete));
		int result=0;
		isLocked=true;
		String line = "";
		int contador=0;	
		InputStream in;
		BufferedReader reader;
		OutputStream out;
 		BufferedWriter writer;

		try {
			in = main.openFileInput(this.BUFFER_FILE_NAME);
			reader = new BufferedReader(new InputStreamReader(in),(int) (app.getBufferSize()+200));
			out = main.openFileOutput("tmp", Context.MODE_APPEND);
			writer = new BufferedWriter(new OutputStreamWriter(out),(int) (app.getBufferSize()+200));
			while ((line = reader.readLine()) != null) {
				contador++;
				//Log.w("HAL",String.valueOf(contador));
				if(contador>linesToDelete) {
		 			writer.write(line+"\n");
				}
			}
 			writer.flush();
 			writer.close();
 			out.close();
 			reader.close();
			in.close();
			// Borrar el fichero antiguo y renombrar el temporal
			File old=main.getFileStreamPath(this.BUFFER_FILE_NAME);
			old.delete();
			main.getFileStreamPath("tmp").renameTo(old);
			result=1;
		} catch (Exception e) {
			Log.e("HAL","BufferManager.deleteLines()-ERROR: "+e.getMessage());
			result=0;
		}
		isLocked=false;
		return result;
	}
	private int createBufferFile() {
		Log.d("HAL","BufferManager.createBufferFile()");
		isLocked=true;
		int resp=Halib.createEmptyFile(main, BUFFER_FILE_NAME);
		isLocked=false;
		return resp;
	}
	
	/**
	 * Vacia el buffer borrando su contenido
	 * @return Devuelve 1 si la operación tuvo exito, 0 o negativo si
	 * se produjeron errores
	 */
	public int emptyBuffer() {
		Log.d("HAL","BufferManager.emptyBuffer()");
		isLocked=true;
		int resp=createBufferFile();
		isLocked=false;
		return resp;
	}
		
	/**
	 * Bloquea el buffer, de forma que no se podrán hacer operaciones
	 * de lectura y/o escritura hasta que se desactive el bloqueo
	 * @return true si se pudo bloquear el fichero
	 *         false si no se pudo (ya estaba bloqueado)
	 */
	public boolean lock() {
		boolean resp=false;
		if(!isLocked) {
			isLocked=true;
			resp = true;
		}
		return resp;
	}
	
	/**
	 * Desbloquea el buffer permitiendo operaciones de lectura o escritura 
	 * sobre el mismo
	 * @return true si fue posible desbloquear el fichero
	 *         false si no fue posible desbloquear (ya estaba bloqueado)
	 */
	public boolean unLock() {
		boolean resp=false;
		if(isLocked) {
			isLocked=false;
			resp=true;
		}
		return resp;
	}
	
	/**
	 * Indica si el fichero del buffer se encuentra o no bloqueado
	 * @return 
	 * 
	 * 		true si el fichero está bloqueado<p>
	 * 		false si el fichero no está bloqueado
	 */
	public boolean isLocked() {
		return isLocked;
	}

	/**
	 * Devuelve el buffer en forma de un String con una línea por cada location 
	 * @return String con las líneas del buffer
	 */
	public String bufferToString() {
		return Halib.readFile(main, BUFFER_FILE_NAME);
	}
	
}
