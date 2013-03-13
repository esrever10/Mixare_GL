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
package kunpeng.ar.plugin.connection;

import kunpeng.ar.data.convert.DataConvertor;
import kunpeng.ar.plugin.PluginConnection;
import kunpeng.ar.plugin.remoteobjects.RemoteDataHandler;

import kunpeng.ar.lib.service.IDataHandlerService;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class DataHandlerServiceConnection extends PluginConnection implements
		ServiceConnection {

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		// get instance of the aidl binder
		IDataHandlerService iDataHandlerService = IDataHandlerService.Stub
				.asInterface(service);
		RemoteDataHandler rm = new RemoteDataHandler(iDataHandlerService);
		rm.buildDataHandler();
		DataConvertor.getInstance().addDataProcessor(rm);
		storeFoundPlugin();
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		DataConvertor.getInstance().clearDataProcessors();
	}

}
