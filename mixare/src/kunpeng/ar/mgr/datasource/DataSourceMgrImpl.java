/*
 * Copyleft 2012 - Peer internet solutions 
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
package kunpeng.ar.mgr.datasource;

import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

import kunpeng.ar.MixContext;
import kunpeng.ar.data.DataSource;
import kunpeng.ar.data.DataSourceStorage;
import kunpeng.ar.mgr.downloader.DownloadRequest;


import android.content.SharedPreferences;

class DataSourceMgrImpl implements DataSourceManager {

	private final ConcurrentLinkedQueue<DataSource> allDataSources=new ConcurrentLinkedQueue<DataSource>(); 
	
	private final MixContext ctx;

	public DataSourceMgrImpl(MixContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public boolean isAtLeastOneDatasourceSelected() {
		boolean atLeastOneDatasourceSelected = false;
		for (DataSource ds : this.allDataSources) {
			if (ds.getEnabled())
				atLeastOneDatasourceSelected = true;
		}
		return atLeastOneDatasourceSelected;
	}



	public void setAllDataSourcesforLauncher(DataSource datasource) {
		this.allDataSources.clear(); // TODO WHY? CLEAN ALL
		this.allDataSources.add(datasource);
	}

	public void refreshDataSources() {
		this.allDataSources.clear();
//		SharedPreferences settings = ctx.getSharedPreferences("DataSourcesPrefs", 0);
//		SharedPreferences.Editor editor = settings.edit();
//		editor.clear();
//		editor.commit();
		DataSourceStorage.getInstance(ctx).fillDefaultDataSources();

		int size = DataSourceStorage.getInstance().getSize();

		// copy the value from shared preference to adapter
		for (int i = 0; i < size; i++) {
			String fields[] = DataSourceStorage.getInstance().getFields(i);
			this.allDataSources.add(new DataSource(fields[0], fields[1],
					fields[2], fields[3], fields[4]));
		}
	}

	public void requestDataFromAllActiveDataSource(double lat, double lon,
			double alt, float radius, String query) {
		for (DataSource ds : allDataSources) {
			/*
			 * when type is OpenStreetMap iterate the URL list and for selected
			 * URL send data request
			 */
			//ello test only BAIDU
			if (/*ds.getType() == DataSource.TYPE.WIKIPEDIA && */ds.getEnabled()) {
				requestData(ds, lat, lon, alt, radius, Locale.getDefault()
						.getLanguage(),query);
			}
		}

	}

	private void requestData(DataSource datasource, double lat, double lon,
			double alt, float radius, String locale,String query) {
		DownloadRequest request = new DownloadRequest(datasource,
				datasource.createRequestParams(lat, lon, alt, radius, locale,query));
		ctx.getDownloadManager().submitJob(request);

	}

}
