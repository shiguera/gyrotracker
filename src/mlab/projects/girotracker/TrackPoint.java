package mlab.projects.girotracker;

import android.location.Location;

public class TrackPoint extends Location {
	// Objeto TrackPoint
	// Consta de un 'user', una etiqueta del track 'label' y un Location.
	
	private String user;
	private String label;

	public TrackPoint(String trackUser, String trackLabel, Location l) {
		super(l);
		this.user=trackUser;
		this.label=trackLabel;		
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public boolean isDiferent(TrackPoint other) {
		if(this.user.equals(other.getUser()) && 
			this.label.equals(other.getLabel()) && 
			this.getTime()==other.getTime()) {
			return false;
		}
		return true;
	}
	public TrackPoint set(TrackPoint trackPoint) {
		//Log.d("HAL","TrackPoint.set()\n"+this.toString()+"\nother"+trackPoint.toString());
		super.set(trackPoint);
		this.user=trackPoint.getUser();
		this.label=trackPoint.getLabel();
		return this;
	}
	@Override 
	public String toString() {
		String cad="";
		//cad+=user+","+label+","+Halib.locToString(this);
		cad+=label+","+Halib.locToString(this);
		return cad;
	}

}
