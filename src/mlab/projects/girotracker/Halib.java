package mlab.projects.girotracker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.text.format.Time;
import android.util.Log;

/**
 * 
 * 
 * @author Santiago Higuera (2012)
 *
 */
public class Halib {
	static public void alert(Context context, String title, String message) {
		new AlertDialog.Builder(context)
        	.setTitle(title)
        	.setMessage(message)
        	.setNeutralButton("Close", new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dlg, int sumthin) {
        			// no hay que hacer nada, se cierra solo
        		}
        	})
        	.show();
	}
	static public String lonToString(double lon) {
		String cad="";
		String EW = (lon>0)? "E":"W";
		double abslon = Math.abs(lon);
		int grados = (int) abslon;
		double resto = abslon - grados;
		double min = resto * 60;
		cad = String.format("%03dº%02.2f'%s", grados, min, EW).replace(',', '.');
		return cad;
	}
    static public String latToString(double lat) {
    	String cad="";
		String NS = (lat>0)? "N":"S";
		double abslat = Math.abs(lat);
		int grados = (int) abslat;
		double resto = abslat - grados;
		double min = resto * 60;
		cad = String.format("%02dº%02.2f'%s", grados, min, NS).replace(',', '.');
		return cad;
    }
    static public String dateTimeToString(long t, boolean gmt) {    	
    	String cad=Halib.dateToString(t,gmt);
    	cad+="T"+Halib.timeToString(t,gmt);
    	return cad;
    }
    static public String timeToString(long t, boolean gmt) {
    	Log.i("HAL","Halib.timeToString()");
    	Calendar cal=Calendar.getInstance();
    	cal.setTimeInMillis(t);
    	if(gmt) {
    		cal.setTimeZone(TimeZone.getTimeZone("gmt"));
    	}
    	String date=String.format("%02d", cal.get(Calendar.HOUR_OF_DAY))+":"+
    			String.format("%02d", cal.get(Calendar.MINUTE))+":"+
    			String.format("%02d", cal.get(Calendar.SECOND));
    	Log.d("HAL","Time:"+date);
        return date;
    }
    static public String dateToString(long t, boolean gmt) {
    	Log.i("HAL","Halib.dateToString()");
    	Calendar cal=Calendar.getInstance();
    	if(gmt) {
    		cal.setTimeZone(TimeZone.getTimeZone("gmt"));
    	}
    	cal.setTimeInMillis(t);
    	String date=cal.get(Calendar.YEAR)+"-"+String.format("%02d", cal.get(Calendar.MONTH)+1)+"-"+
    			String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));
    	Log.d("HAL","Date:"+date);
        return date;
    }
    static public String timeToShortString(Time t) {
    	String cad=t.format3339(false);
    	//String cad = String.format("%4d%02d%02dT%02d%02d%02dZ", 
    	//		t.year,t.month,t.monthDay,t.hour,t.minute,t.second);
    	return cad;
    }
    static public String locToString(Location loc) {
    	Log.i("HAL","Halib.locToString()");
    	String cad ="";
    	long t = loc.getTime();
    	cad += dateToString(t,true);
    	cad += ","+timeToString(t,true);
    	String pp = String.format("%.6f", loc.getLongitude()).replace(',', '.');
    	cad+=","+pp;
    	pp = String.format("%.6f", loc.getLatitude()).replace(',', '.');
    	cad+=","+pp;
    	pp = String.format("%.6f", loc.getAltitude()).replace(',', '.');
    	cad+=","+pp;
    	double vkmh=loc.getSpeed()*3.6;
    	pp = String.format("%.6f", vkmh).replace(',', '.');
    	cad+=","+pp;
    	pp = String.format("%.6f", loc.getBearing()).replace(',', '.');
    	cad+=","+pp;
    	pp=String.format("%.1f", loc.getAccuracy()).replace(',', '.');
    	cad+=","+pp;
    	//Log.i("HAL","   Halib: loc= "+cad);
    	return cad;
    }
    static public String locToShortString(Location loc) {
    	String cad ="";
    	Time t = new Time();
    	t.setToNow();
    	cad += timeToShortString(t);
    	String pp = String.format("%.3f", loc.getLongitude()).replace(',', '.');
    	cad+=","+pp;
    	pp = String.format("%.3f", loc.getLatitude()).replace(',', '.');
    	cad+=","+pp;
    	return cad;
    }
    static public String getCurrentDate(boolean gmt) {
    	DateFormat df = DateFormat.getDateInstance();
    	if(gmt) {
    		df.setTimeZone(TimeZone.getTimeZone("gmt"));
    	}
    	String date=df.format(new Date());
        return date;
//    	Calendar calendar = Calendar.getInstance();
//        String date = calendar.get(Calendar.YEAR)+"-"+calendar.get(Calendar.MONTH)+"-"+calendar.get(Calendar.DAY_OF_MONTH);
//        return date;
    }
    
    static public String getCurrentTime(boolean gmt) {
    	//Calendar calendar = Calendar.getInstance();
    	//String time = calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE)+
        	//	":"+calendar.get(Calendar.SECOND);
    	DateFormat df = DateFormat.getTimeInstance();
    	if(gmt) {
    		df.setTimeZone(TimeZone.getTimeZone("gmt"));
    	}
    	String time=df.format(new Date());
        return time;
    }
    // Acceso a ficheros del área privada de la aplicación
    static public int deleteFile(Context context, String filename) {
		Log.i("HAL","Halib.deleteFile()");
		int result=0;
		try {
			File file=context.getFileStreamPath(filename);
			if(file!=null) {
				boolean isdeleted=file.delete();
				if(isdeleted) {
					result=1;
				}
			}
		} catch (Exception e) {
			Log.e("HAL","Halib.deleteFile() ERROR: "+e.getMessage());			
		}
		return result;
	}
    static public int appendToFile(Context context, String filename, String content) {
 		//Log.i("HAL","Halib.appendToFile()");
     	int result=0;
 		OutputStream out;
 		BufferedWriter writer;
 		try {
 			out = context.openFileOutput(filename, Context.MODE_APPEND);
 			writer = new BufferedWriter(new OutputStreamWriter(out),content.length());
 			writer.write(content);
 			writer.flush();
 			writer.close();
 			out.close();
 			result=1;
 		} catch (Exception e) {
 			Log.e("HAL","Halib.appendToFile()-Error: "+e.getMessage());
 			result=-1;
 		}
 		return result;
 	}
    static public int writeFile(Context context, String filename, String content) {
		Log.i("HAL","Halib.writeFile()");
    	int result=0;
		OutputStream out;
		BufferedWriter writer;
		try {
			out = context.openFileOutput(filename, Context.MODE_PRIVATE);
			writer = new BufferedWriter(new OutputStreamWriter(out),content.length()+200);
			writer.write(content);
			writer.flush();
			writer.close();
			out.close();
			result=1;
		} catch (Exception e) {
			Log.e("HAL","Halib.writeFile()-Error: "+e.getMessage());
			result=-1;
		}
		return result;
	}
    /**
     * Lee un fichero de la zona reservada de la aplicación con openFileInput()
     * @param context Context Contexto de ejecución
     * @param filename String Nombre del fichero que se quiere leer
     * @return String Contenido del fichero o una cadena vacia si hay error 
     */
    static public String readFile(Context context, String filename) {
		Log.d("HAL","Halib.readFile()");
    	String content = "";
    	int count=0;
		try {
			InputStream in = context.openFileInput(filename);
			int filesize=(int)Halib.fileSize(context, filename);
			if(filesize>0) {
				Log.d("HAL","Halib.readFile()- "+String.valueOf(filesize)+" (filesize)");
				BufferedReader reader= new BufferedReader(new InputStreamReader(in), (int)Halib.fileSize(context, filename));
				String line = "";
				while ((line = reader.readLine()) != null) {
					content += line + "\n";
					count++;
				}
				in.close();
				Log.d("HAL","Halib.readFile()- "+String.valueOf(count)+" lines");
			} else {
				Log.w("HAL","Halib.readFile()- Buffer empty "+String.valueOf(filesize)+" (filesize)");
			}			 
		} catch (FileNotFoundException e) {
			Log.e("HAL","Halib.readFile() ERROR: "+e.getMessage());
			content="";
		} catch (IOException ioe) {
			Log.e("HAL","Halib.readFile() ERROR: "+ioe.getMessage());
			content="";
		}
		return content;
	}
    
    /**
     * Crea un fichero vacío en el area reservada de la aplicación
     * @param context Context de la aplicación
     * @param filename Nombre del fichero que se quiere crear
     * @return 1 si el fichero se crea
     *         0 o negativo si se produjo algún error y no se crea el fichero
     */
    static public int createEmptyFile(Context context, String filename) {
		Log.d("HAL","Halib.createEmptyFile()");
		int resp=0;
		try {
			OutputStream out = context.openFileOutput(filename, Context.MODE_PRIVATE);
			out.close();
			resp=1;
		} catch (FileNotFoundException e1) {
			Log.e("HAL","Halib.createEmptyFile() ERROR: "+e1.getMessage());	
			resp=-1;
		} catch (IOException e2) {
			Log.e("HAL","Halib.createEmptyFile() ERROR: "+e2.getMessage());	
			resp=-2;
		}
		return resp;
	}
    static public long fileSize(Context context, String filename) {
		//Log.i("HAL","Halib.fileSize(Context context, String filename)");
		long counter=0l;
		try {
			File file = context.getFileStreamPath(filename);
			counter=file.length();
		} catch (Exception e) {
			Log.e("HAL","Halib.fileSize()-ERROR: "+e.getMessage());
			counter=-1l;
		}
		return counter;
	}
    static public boolean existsFile(Context context, String filename) {
		boolean exists=false;
		String[] files=context.fileList();
		for(int i=0; i< files.length; i++) {
			if(files[i].equals(filename)) {
				exists=true;
				break;
			}
		}
		return exists;
	}
}
