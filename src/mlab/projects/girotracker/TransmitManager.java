package mlab.projects.girotracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

/**
 * Es la clase encargada de realizar la petición HttpPost
 * 
 * @author shiguera
 *
 */
public class TransmitManager {

	private final String url="url_to_php_files_directory";
	private Activity main;
	private App app;
	private ConnectivityManager connManager;
	
	/**
	 * Constructor del TransmitManager.
	 * @param mainActivity Activity Actividad principal de la aplicación
	 */
	TransmitManager(Activity mainActivity) {
		Log.d("HAL", "TransmitManager.builder()");
		this.main=mainActivity;
		this.app=(App)main.getApplication();
        connManager = (ConnectivityManager) main.getSystemService(Context.CONNECTIVITY_SERVICE);        
	}

	public boolean isTransmitterEnabled() {
		boolean isTransmitterEnabled=false;
		try {
			isTransmitterEnabled = connManager.getActiveNetworkInfo().isAvailable();
		} catch (Exception e) {
			isTransmitterEnabled=false;
			Log.e("HAL", "TransmitManager.isTransmitterEnabled()-ERROR:\n  "+e.getMessage());	
		}
		return isTransmitterEnabled;
	}
	public boolean isRoaming() {
		boolean isRoaming=false;
		try {
	        isRoaming=connManager.getActiveNetworkInfo().isRoaming();			
		} catch (Exception e) {
			Log.e("HAL", "TransmitManager.isRoaming()-ERROR:\n  "+e.getMessage());	
			isRoaming=false;
		}
		return isRoaming;
	}	
	
	/**
	 * Realiza una petición HttpPost y envía el buffer a la url:
	 *     http://mercatorlab.com/autogiro/addPointList.php
	 * @param bufferToString String El buffer como un String con las 
	 * lineas separadas por un '\n'
	 * @return int Número de líneas afectadas por la operación o negativo si hay error
	 */
    public int sendBuffer(String bufferToString) {
    	int result=0;
    	BufferedReader in = null;
		try {
			HttpClient client = new DefaultHttpClient();
			HttpPost request = new HttpPost(this.url+"addPointList.php");
			ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
			postParameters.add(new BasicNameValuePair("user", app.getUser()));
			postParameters.add(new BasicNameValuePair("pass", app.getPassword()));
			postParameters.add(new BasicNameValuePair("points", bufferToString));
			UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(postParameters);
			request.setEntity(formEntity);
			HttpResponse response = client.execute(request);
			in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			StringBuffer sb = new StringBuffer("");
			String line = "";
			String NL = System.getProperty("line.separator");
			while ((line = in.readLine()) != null) {
				sb.append(line + NL);
			}
			String cadresp=sb.toString().trim();
			Log.d("HAL","TransmitManager.sendBuffer()-cadresp:-"+cadresp+"-");
			result = Integer.parseInt(cadresp);
			Log.d("HAL","TransmitManager.sendBuffer()-result: "+String.valueOf(result));
			in.close();
		} catch (ClientProtocolException ce) {
			Log.e("HAL","TransmitManager.sendBuffer()-ERROR:\n  "+ce.getMessage());
			result=-2;
		} catch (IOException ioe) {
			Log.e("HAL","TransmitManager.sendBuffer()-ERROR:\n  "+ioe.getMessage());
			result=-3;
		} catch (NumberFormatException ne) {
			// Error al tratar de convertir la respuesta en un entero
			Log.e("HAL","TransmitManager.sendBuffer()-ERROR:\n  "+ne.getMessage());
			result=-4;
		}
		return result;
	}	
    /**
	 * Realiza una petición HttpPost y solicita validación de usuario:
	 *     http://mercatorlab.com/autogiro/checkUser.php
	 * @param nick String nick del usuario
	 * @param pass String password del usuario
	 * @return int 1 si correcto. Cero si incorrecto. Negativo si error
	 */
    public int checkUser(String nick, String pass) {
    	Log.d("HAL","TransmitManager.checkUser("+nick+","+pass+")");
    	int result=0;
    	BufferedReader in = null;
		try {
			HttpClient client = new DefaultHttpClient();
			HttpPost request = new HttpPost(this.url+"checkUser.php");
			ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
			postParameters.add(new BasicNameValuePair("user", nick));
			postParameters.add(new BasicNameValuePair("pass", pass));
			UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(postParameters);
			request.setEntity(formEntity);
			HttpResponse response = client.execute(request);
			in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			StringBuffer sb = new StringBuffer("");
			String line = "";
			String NL = System.getProperty("line.separator");
			while ((line = in.readLine()) != null) {
				sb.append(line + NL);
			}
			String cadresp=sb.toString().trim();
			Log.d("HAL","TransmitManager.checkUser()-cadresp:-"+cadresp+"-");
			result = Integer.parseInt(cadresp);
			Log.d("HAL","TransmitManager.checkUser()-result: "+String.valueOf(result));
			in.close();
		} catch (ClientProtocolException ce) {
			Log.e("HAL","TransmitManager.checkUser()-ERROR:\n  "+ce.getMessage());
			result=-2;
		} catch (IOException ioe) {
			Log.e("HAL","TransmitManager.checkUser()-ERROR:\n  "+ioe.getMessage());
			result=-3;
		} catch (NumberFormatException ne) {
			// Error al tratar de convertir la respuesta en un entero
			Log.e("HAL","TransmitManager.checkUser()-ERROR:\n  "+ne.getMessage());
			result=-4;
		}
		return result;
	}	
}
