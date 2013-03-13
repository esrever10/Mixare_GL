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
package kunpeng.ar.plugin;

/**
 * A custom exception that will be thrown when something "unexpected" happens while loading a plugin.
 * @author A. Egal
 */
public class PluginNotFoundException extends RuntimeException{
	
	private static final long serialVersionUID = 1L;

	public PluginNotFoundException() {
		super();
	}
	
	public PluginNotFoundException(Throwable throwable){
		super(throwable);
	}

	public PluginNotFoundException(String message) {
		super(message);
	}

}