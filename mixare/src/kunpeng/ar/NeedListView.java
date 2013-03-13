/*
 * Copyright (C) 2010- Peer internet solutions
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
package kunpeng.ar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

/**
 * This class holds vectors with informaction about sources, their description
 * and whether they have been selected.
 */
public class NeedListView extends ListActivity {
	private ListAdapter adapter = null;
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("title", "银行");
		map.put("info", "bank");
		map.put("img", R.drawable.easyicon_cn_24);
		list.add(map);

		map = new HashMap<String, Object>();
		map.put("title", "学校");
		map.put("info", "school");
		map.put("img", R.drawable.easyicon_cn_24);
		list.add(map);

		map = new HashMap<String, Object>();
		map.put("title", "饭店");
		map.put("info", "restaurant");
		map.put("img", R.drawable.easyicon_cn_24);
		list.add(map);

		adapter = new ListAdapter(this,list,false);
		setListAdapter(adapter);
	}

	private List<Map<String, Object>> getData() {
		return adapter.getData();
	}

	@SuppressWarnings("unused")
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String title = (String) getData().get(position).get("title");
		Intent intent = new Intent(this.getBaseContext(), ARView.class);
		intent.putExtra("query", title);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		startActivity(intent);
	}

}
