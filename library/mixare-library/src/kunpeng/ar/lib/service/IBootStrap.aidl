package kunpeng.ar.lib.service;

interface IBootStrap {
	
	 int getZIndex();
	 
	 String getPluginName();
	 
	 String getActivityPackage();
	 
	 String getActivityName();
	 
	 int getActivityRequestCode();
}