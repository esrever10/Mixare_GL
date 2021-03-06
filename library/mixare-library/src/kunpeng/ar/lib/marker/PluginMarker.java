/*
 * Copyright (C) 2012- Peer internet solutions & Finalist IT Group
 * 
 * This file is part of AR Navigator.
 * 
 * This program is free software: you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version. 
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details. 
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
package kunpeng.ar.lib.marker;

import java.net.URLDecoder;

import kunpeng.ar.lib.gui.Label;
import kunpeng.ar.lib.gui.TextObj;
import kunpeng.ar.lib.marker.draw.ClickHandler;
import kunpeng.ar.lib.marker.draw.DrawCommand;
import kunpeng.ar.lib.marker.draw.ParcelableProperty;
import kunpeng.ar.lib.marker.draw.PrimitiveProperty;
import kunpeng.ar.lib.reality.PhysicalPlace;
import kunpeng.ar.lib.render.ARVector;
import kunpeng.ar.lib.render.Camera;


import android.location.Location;

/**
 * A plugin marker that should be extended by marker plugins.
 * @author A. Egal
 *
 */
public abstract class PluginMarker{
	
	private String ID;
	protected String title;
	protected boolean underline = false;
	private String URL;
	protected PhysicalPlace mGeoLoc;
	// distance from user to mGeoLoc in meters
	protected double distance;
	// The marker color
	private int colour;

	private boolean active;
	// Draw properties
	protected boolean isVisible;

	public ARVector cMarker = new ARVector();
	protected ARVector signMarker = new ARVector();

	protected ARVector locationVector = new ARVector();
	private ARVector origin = new ARVector(0, 0, 0);
	private ARVector upV = new ARVector(0, 1, 0);
	public Label txtLab = new Label();
	protected TextObj textBlock;
	
	public PluginMarker(int id, String title, double latitude, double longitude, double altitude, String link, int type, int colour) {
		super();

		this.active = true;
		this.title = title;
		this.mGeoLoc = new PhysicalPlace(latitude,longitude,altitude);
		if (link != null && link.length() > 0) {
			URL = "webpage:" + URLDecoder.decode(link);
			this.underline = true;
		}
		this.colour = colour;

		this.ID= id + "##"+ type +"##"+title;

	}	
	
	public String getURL() {
		return URL;
	}

	public double getLatitude() {
		return mGeoLoc.getLatitude();
	}

	public double getLongitude() {
		return mGeoLoc.getLongitude();
	}

	public double getAltitude() {
		return mGeoLoc.getAltitude();
	}

	public ARVector getLocationVector() {
		return locationVector;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public String getID() {
		return ID;
	}

	public void setID(String iD) {
		this.ID = iD;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public int getColour() {
		return colour;
	}

	public boolean isVisible() {
		return isVisible;
	}

	public ClickHandler fClick() {
		return new ClickHandler(URL, active, txtLab, signMarker, cMarker);
	}

	public abstract int getMaxObjects();

	public ARVector getCMarker() {
		return cMarker;
	}

	public ARVector getSignMarker() {
		return signMarker;
	}

	public boolean getUnderline() {
		return underline;
	}

	public String getTitle() {
		return title;
	}

	public abstract DrawCommand[] remoteDraw();
	
	public void update(Location curGPSFix) {
		// An elevation of 0.0 probably means that the elevation of the
		// POI is not known and should be set to the users GPS height
		// Note: this could be improved with calls to
		// http://www.geonames.org/export/web-services.html#astergdem
		// to estimate the correct height with DEM models like SRTM, AGDEM or
		// GTOPO30
		if (mGeoLoc.getAltitude() == 0.0)
			mGeoLoc.setAltitude(curGPSFix.getAltitude());

		// compute the relative position vector from user position to POI
		// location
		PhysicalPlace.convLocToVec(curGPSFix, mGeoLoc, locationVector);
	}

	public void calcPaint(Camera viewCam, float addX, float addY) {
		cCMarker(origin, viewCam, addX, addY);
		calcV(viewCam);
	}
	
	private void cCMarker(ARVector originalPoint, Camera viewCam, float addX, float addY) {
		// Temp properties
		ARVector tmpa = new ARVector(originalPoint);
		ARVector tmpc = new ARVector(upV);
		tmpa.add(locationVector); //3 
		tmpc.add(locationVector); //3
		tmpa.sub(viewCam.lco); //4
		tmpc.sub(viewCam.lco); //4
		tmpa.prod(viewCam.transform); //5
		tmpc.prod(viewCam.transform); //5

		ARVector tmpb = new ARVector();
		viewCam.projectPoint(tmpa, tmpb, addX, addY); //6
		cMarker.set(tmpb); //7
		viewCam.projectPoint(tmpc, tmpb, addX, addY); //6
		signMarker.set(tmpb); //7
	}

	private void calcV(Camera viewCam) {
		isVisible = true;
		
		if (cMarker.z < -1f) {
			isVisible = true;

			//if (MixUtils.pointInside(cMarker.x, cMarker.y, 0, 0,
			//		viewCam.width, viewCam.height)) {
			//}
		}
	}
	
	public void setTxtLab(Label txtLab) {
		this.txtLab = txtLab;
	}

	public Label getTxtLab() {
		return txtLab;
	}
	
	public void setExtras(String name, ParcelableProperty parcelableProperty){
		//can be overriden
	}
	
	public void setExtras(String name, PrimitiveProperty primitiveProperty){
		//can be overriden
	}
	

}
