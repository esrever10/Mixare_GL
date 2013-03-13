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
package kunpeng.ar.data.convert;

import java.util.ArrayList;
import java.util.List;

import kunpeng.ar.ARView;
import kunpeng.ar.POIMarker;
import kunpeng.ar.data.DataHandler;
import kunpeng.ar.data.DataSource;
import kunpeng.ar.data.convert.DataProcessor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import kunpeng.ar.lib.HtmlUnescape;
import kunpeng.ar.lib.marker.Marker;

import android.util.Log;

/**
 * A data processor for wikipedia urls or data, Responsible for converting raw data (to json and then) to marker data.
 * @author A. Egal
 */
public class BaiduDataProcessor extends DataHandler implements DataProcessor{

	public static final int MAX_JSON_OBJECTS = 1000;
	
	@Override
	public String[] getUrlMatch() {
		String[] str = {"baidu"};
		return str;
	}

	@Override
	public String[] getDataMatch() {
		String[] str = {"baidu"};
		return str;
	}
	
	@Override
	public boolean matchesRequiredType(String type) {
		if(type.equals(DataSource.TYPE.BAIDU.name())){
			return true;
		}
		return false;
	}

	@Override
	public List<Marker> load(String rawData, int taskId, int colour) throws JSONException {
		List<Marker> markers = new ArrayList<Marker>();
		JSONObject root = convertToJSON(rawData);
		String status = root.getString("status");
		if(status.equals("OK"))
		{
			JSONArray dataArray = root.getJSONArray("results");
			int top = Math.min(MAX_JSON_OBJECTS, dataArray.length());
	
			for (int i = 0; i < top; i++) {
				JSONObject jo = dataArray.getJSONObject(i);
				
				Marker ma = null;
				if (jo.has("address") && jo.has("location") && jo.has("name")) {
		
//					Log.v(ARView.TAG, "processing Baidupedia JSON object");
					JSONObject location = jo.getJSONObject("location");
					double lat = location.getDouble("lat");
					double lng = location.getDouble("lng");
					//no unique ID is provided by the web service according to http://www.geonames.org/export/wikipedia-webservice.html
					ma = new POIMarker(
							"",
							HtmlUnescape.unescapeHTML(jo.getString("name"), 0), 
							lat, 
							lng, 
							0.0, 
							jo.getString("detail_url"), 
							taskId, colour);
					markers.add(ma);
				}
			}
		}
		else if(status.equals("OVER_MAX_LIMIT"))
		{
			//So what should I do
		}
		return markers;
	}
	
	private JSONObject convertToJSON(String rawData){
		try {
			return new JSONObject(rawData);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	
}
