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

/**
 * This class is the main application which uses the other classes for different
 * functionalities.
 * It sets up the camera screen and the augmented screen which is in front of the
 * camera screen.
 * It also handles the main sensor events, touch events and location events.
 */

import static android.hardware.SensorManager.SENSOR_DELAY_GAME;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import kunpeng.ar.lib.MixUtils;
import kunpeng.ar.lib.gui.PaintScreen;
import kunpeng.ar.lib.marker.Marker;
import kunpeng.ar.lib.render.Matrix;
import android.view.View;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.util.FloatMath;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.opengl.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

import kunpeng.ar.LabelMaker;
import kunpeng.ar.MatrixTrackingGL;
import kunpeng.ar.Projector;
import kunpeng.ar.Cluster.Label;
import kunpeng.ar.Cluster.Label.State;
import kunpeng.ar.data.DataHandler;
import kunpeng.ar.data.DataSourceStorage;
import android.opengl.GLSurfaceView.Renderer;

public class ARView extends Activity implements SensorEventListener,
		OnTouchListener {

	private CameraSurface camScreen;
	private AugmentedView augScreen;
	private AugmentedGLView augGLScreen;

	private boolean isInited;
	private static PaintScreen dWindow;
	private static DataView dataView;
	private boolean fError;
	public static final float orientationValues[] = new float[3];
	// ----------
	private MixViewDataHolder mixViewData;

	// TAG for logging
	public static final String TAG = "AR";

	// why use Memory to save a state? MixContext? activity lifecycle?
	// private static MixView CONTEXT;

	/* string to name & access the preference file in the internal storage */
	public static final String PREFS_NAME = "MyPrefsFileForMenuItems";

	private FrameLayout mframe;

	static int mRotation = 1;

	// static float touchX = 0;
	// static float touchY = 0;
	// static float touchW = 0;
	// static float touchH = 0;

	public MixViewDataHolder getMixViewData() {
		if (mixViewData == null) {
			// TODO: VERY inportant, only one!
			mixViewData = new MixViewDataHolder(new MixContext(this));
		}
		return mixViewData;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// MixView.CONTEXT = this;
		try {
			handleIntent(getIntent());

			final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			getMixViewData().setmWakeLock(
					pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
							"My Tag"));

			killOnError();
			requestWindowFeature(Window.FEATURE_NO_TITLE); // 设置无标题
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN); // 设置全屏

			mframe = new FrameLayout(this);
			maintainCamera();
			maintainAugmentGLView();
			maintainAugmentR();

			setContentView(mframe);
			maintainZoomBar();
			maintainToolBar();

			if (!isInited) {
				// getMixViewData().setMixContext(new MixContext(this));
				// getMixViewData().getMixContext().setDownloadManager(new
				// DownloadManager(mixViewData.getMixContext()));
				setdWindow(new PaintScreen());
				setDataView(new DataView(getMixViewData().getMixContext()));
				// 设置query
				Bundle bundle = getIntent().getExtras();
				String query = bundle.getString("query");
				query = URLEncoder.encode(query, "utf-8");
				getDataView().setQuery(query);

				/* set the radius in data view to the last selected by the user */
				setZoomLevel();
				isInited = true;

			}
			/*
			 * Get the preference file PREFS_NAME stored in the internal memory
			 * of the phone
			 */
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

			/* check if the application is launched for the first time */
			if (settings.getBoolean("firstAccess", false) == false) {
				firstAccess(settings);

			}

		} catch (Exception ex) {
			doError(ex);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		try {

			this.getMixViewData().getmWakeLock().release();

			try {
				getMixViewData().getMixContext().cancelMsgPopUp();
				getMixViewData().getSensorMgr().unregisterListener(this,
						getMixViewData().getSensorGrav());
				getMixViewData().getSensorMgr().unregisterListener(this,
						getMixViewData().getSensorMag());
				getMixViewData().setSensorMgr(null);

				getMixViewData().getMixContext().getLocationFinder()
						.switchOff();
				getMixViewData().getMixContext().getDownloadManager()
						.switchOff();
				augGLScreen.onPause();

				// 从frame layout中删除，否则view无法被销毁
				mframe.removeAllViews();
				camScreen = null;
				augScreen = null;
				augGLScreen = null;

			} catch (Exception ignore) {
			}

			if (fError) {
				finish();
			}
		} catch (Exception ex) {
			doError(ex);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		try {
			try {
				Log.d(TAG + " WorkFlow", "MixView - onDestroy called");
				setDataView(null);
			} catch (Exception ignore) {
			}

			if (fError) {
				finish();
			}
		} catch (Exception ex) {
			doError(ex);
		}
	}

	/**
	 * {@inheritDoc} Mixare - Receives results from other launched activities
	 * Base on the result returned, it either refreshes screen or not. Default
	 * value for refreshing is false
	 */
	protected void onActivityResult(final int requestCode,
			final int resultCode, Intent data) {
		Log.d(TAG + " WorkFlow", "MixView - onActivityResult Called");
		// check if the returned is request to refresh screen (setting might be
		// changed)
		try {
			if (data.getBooleanExtra("RefreshScreen", false)) {
				Log.d(TAG + " WorkFlow",
						"MixView - Received Refresh Screen Request .. about to refresh");
				repaint();
				refreshDownload();
			}

		} catch (Exception ex) {
			// do nothing do to mix of return results.
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		try {

			this.getMixViewData().getmWakeLock().acquire();

			killOnError();
			getMixViewData().getMixContext().doResume(this);

			repaint();
			getDataView().doStart();
			getDataView().clearEvents();

			getMixViewData().getMixContext().getDataSourceManager()
					.refreshDataSources();

			float angleX = 0, angleY = 0;
			int marker_orientation = -90;

			// display text from left to right and keep it horizontal
			angleX = (float) Math.toRadians(marker_orientation);
			getMixViewData().getM1().set(1f, 0f, 0f, 0f,
					(float) FloatMath.cos(angleX),
					(float) -FloatMath.sin(angleX), 0f,
					(float) FloatMath.sin(angleX),
					(float) FloatMath.cos(angleX));

			angleX = (float) Math.toRadians(marker_orientation);
			angleY = (float) Math.toRadians(marker_orientation);
			getMixViewData().getM2().set((float) FloatMath.cos(angleX), 0f,
					(float) FloatMath.sin(angleX), 0f, 1f, 0f,
					(float) -FloatMath.sin(angleX), 0f,
					(float) FloatMath.cos(angleX));
			getMixViewData().getM3().set(1f, 0f, 0f, 0f,
					(float) FloatMath.cos(angleY),
					(float) -FloatMath.sin(angleY), 0f,
					(float) FloatMath.sin(angleY),
					(float) FloatMath.cos(angleY));

			getMixViewData().getM4().toIdentity();

			for (int i = 0; i < getMixViewData().getHistR().length; i++) {
				getMixViewData().getHistR()[i] = new Matrix();
			}

			getMixViewData().setSensorMgr(
					(SensorManager) getSystemService(SENSOR_SERVICE));

			getMixViewData().setSensors(
					getMixViewData().getSensorMgr().getSensorList(
							Sensor.TYPE_ACCELEROMETER));
			if (getMixViewData().getSensors().size() > 0) {
				getMixViewData().setSensorGrav(
						getMixViewData().getSensors().get(0));
			}

			getMixViewData().setSensors(
					getMixViewData().getSensorMgr().getSensorList(
							Sensor.TYPE_MAGNETIC_FIELD));
			if (getMixViewData().getSensors().size() > 0) {
				getMixViewData().setSensorMag(
						getMixViewData().getSensors().get(0));
			}
			// 注册传感器
			getMixViewData().getSensorMgr().registerListener(this,
					getMixViewData().getSensorGrav(), SENSOR_DELAY_GAME);
			getMixViewData().getSensorMgr().registerListener(this,
					getMixViewData().getSensorMag(), SENSOR_DELAY_GAME);

			try {
				GeomagneticField gmf = getMixViewData().getMixContext()
						.getLocationFinder().getGeomagneticField();
				if (gmf != null) {
					angleY = (float) Math.toRadians(-gmf.getDeclination());
				}
				getMixViewData().getM4().set((float) FloatMath.cos(angleY), 0f,
						(float) FloatMath.sin(angleY), 0f, 1f, 0f,
						(float) -FloatMath.sin(angleY), 0f,
						(float) FloatMath.cos(angleY));
			} catch (Exception ex) {
				Log.d("AR Navigator", "GPS Initialize Error", ex);
			}

			getMixViewData().getMixContext().getDownloadManager().switchOn();
			getMixViewData().getMixContext().getLocationFinder().switchOn();
			augGLScreen.onResume();
		} catch (Exception ex) {
			doError(ex);
			try {
				if (getMixViewData().getSensorMgr() != null) {
					getMixViewData().getSensorMgr().unregisterListener(this,
							getMixViewData().getSensorGrav());
					getMixViewData().getSensorMgr().unregisterListener(this,
							getMixViewData().getSensorMag());
					getMixViewData().setSensorMgr(null);
				}

				if (getMixViewData().getMixContext() != null) {
					getMixViewData().getMixContext().getLocationFinder()
							.switchOff();
					getMixViewData().getMixContext().getDownloadManager()
							.switchOff();
				}
			} catch (Exception ignore) {
			}
		}
		ARView.getDataView().setRadius(calcZoomLevel());
		Log.d("-------------------------------------------", "resume");
		// if (getDataView().isFrozen()
		// && getMixViewData().getSearchNotificationTxt() == null) {
		// getMixViewData().setSearchNotificationTxt(new TextView(this));
		// getMixViewData().getSearchNotificationTxt().setWidth(
		// getdWindow().getWidth());
		// getMixViewData().getSearchNotificationTxt().setPadding(10, 2, 0, 0);
		// getMixViewData().getSearchNotificationTxt().setText(
		// getString(R.string.search_active_1) + " "
		// + DataSourceList.getDataSourcesStringList()
		// + getString(R.string.search_active_2));
		// ;
		// getMixViewData().getSearchNotificationTxt().setBackgroundColor(
		// Color.DKGRAY);
		// getMixViewData().getSearchNotificationTxt().setTextColor(
		// Color.WHITE);
		//
		// getMixViewData().getSearchNotificationTxt()
		// .setOnTouchListener(this);
		// addContentView(getMixViewData().getSearchNotificationTxt(),
		// new LayoutParams(LayoutParams.FILL_PARENT,
		// LayoutParams.WRAP_CONTENT));
		// } else if (!getDataView().isFrozen()
		// && getMixViewData().getSearchNotificationTxt() != null) {
		// getMixViewData().getSearchNotificationTxt()
		// .setVisibility(View.GONE);
		// getMixViewData().setSearchNotificationTxt(null);
		// }
	}

	/**
	 * {@inheritDoc} Customize Activity after switching back to it. Currently it
	 * maintain and ensures view creation.
	 */
	protected void onRestart() {
		super.onRestart();
		mframe.removeAllViewsInLayout();
		mframe.removeAllViews();
		maintainCamera();
		maintainAugmentGLView();
		maintainAugmentR();
		maintainZoomBar();
		maintainToolBar();
	}

	/* ********* Operators ********** */

	public void repaint() {

		setDataView(null); // It's smelly code, but enforce garbage collector
							// to release data.
		setDataView(new DataView(getMixViewData().getMixContext()));
		setdWindow(new PaintScreen());
		// setZoomLevel(); //@TODO Caller has to set the zoom. This function
		// repaints only.
	}

	/**
	 * Checks camScreen, if it does not exist, it creates one.
	 */
	private void maintainCamera() {
		if (camScreen == null) {
			camScreen = new CameraSurface(this);
		}
		mframe.addView(camScreen);
	}

	/**
	 * Checks augScreen, if it does not exist, it creates one.
	 */
	private void maintainAugmentR() {
		if (augScreen == null) {
			augScreen = new AugmentedView(this);
		}
		mframe.addView(augScreen);
	}

	/**
	 * Creates a AugmentGLView and adds it to view.
	 */
	private void maintainAugmentGLView() {
		if (augGLScreen == null) {
			augGLScreen = new AugmentedGLView(this);
		}

		mframe.addView(augGLScreen);
		augGLScreen.setZOrderMediaOverlay(true);// 重要
	}

	/**
	 * Creates a zoom bar and adds it to view.
	 */
	private void maintainZoomBar() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		FrameLayout frameLayout = createZoomBar(settings);
		addContentView(frameLayout, new FrameLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT,
				Gravity.BOTTOM));
	}

	/**
	 * Creates a tool bar and adds it to view.
	 */
	private void maintainToolBar() {
		FrameLayout frameLayout = createToolBar();
		addContentView(frameLayout, new FrameLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
				Gravity.RIGHT | Gravity.TOP));
	}

	/**
	 * Refreshes Download TODO refresh downloads
	 */
	private void refreshDownload() {
		// try {
		// if (getMixViewData().getDownloadThread() != null){
		// if (!getMixViewData().getDownloadThread().isInterrupted()){
		// getMixViewData().getDownloadThread().interrupt();
		// getMixViewData().getMixContext().getDownloadManager().restart();
		// }
		// }else { //if no download thread found
		// getMixViewData().setDownloadThread(new Thread(getMixViewData()
		// .getMixContext().getDownloadManager()));
		// //@TODO Syncronize DownloadManager, call Start instead of run.
		// mixViewData.getMixContext().getDownloadManager().run();
		// }
		// }catch (Exception ex){
		// }
	}

	public void refresh() {
		getDataView().refresh();
	}

	public void setErrorDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.connection_error_dialog));
		builder.setCancelable(false);

		/* Retry */
		builder.setPositiveButton(R.string.connection_error_dialog_button1,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						fError = false;
						// TODO improve
						try {
							maintainCamera();
							maintainAugmentGLView();
							maintainAugmentR();

							repaint();
							setZoomLevel();
						} catch (Exception ex) {
							// Don't call doError, it will be a recursive call.
							// doError(ex);
						}
					}
				});
		/* Open settings */
		builder.setNeutralButton(R.string.connection_error_dialog_button2,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Intent intent1 = new Intent(
								Settings.ACTION_WIRELESS_SETTINGS);
						startActivityForResult(intent1, 42);
					}
				});
		/* Close application */
		builder.setNegativeButton(R.string.connection_error_dialog_button3,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						System.exit(0); // wouldn't be better to use finish (to
										// stop the app normally?)
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public float calcZoomLevel() {

		int myZoomLevel = getMixViewData().getMyZoomBar().getProgress();
		float myout = 5;

		if (myZoomLevel <= 26) {
			myout = myZoomLevel / 25f;
		} else if (25 < myZoomLevel && myZoomLevel < 50) {
			myout = (1 + (myZoomLevel - 25)) * 0.38f;
		} else if (25 == myZoomLevel) {
			myout = 1;
		} else if (50 == myZoomLevel) {
			myout = 10;
		} else if (50 < myZoomLevel && myZoomLevel < 75) {
			myout = (10 + (myZoomLevel - 50)) * 0.83f;
		} else {
			myout = (30 + (myZoomLevel - 75) * 2f);
		}

		return myout;
	}

	/**
	 * Handle First time users. It display license agreement and store user's
	 * acceptance.
	 * 
	 * @param settings
	 */
	private void firstAccess(SharedPreferences settings) {
		SharedPreferences.Editor editor = settings.edit();
		AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
		builder1.setMessage(getString(R.string.license));
		builder1.setNegativeButton(getString(R.string.close_button),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});
		AlertDialog alert1 = builder1.create();
		alert1.setTitle(getString(R.string.license_title));
//		alert1.show();
		editor.putBoolean("firstAccess", true);

		// value for maximum POI for each selected BAIDU URL to be active by
		// default is 5
		editor.putInt("osmMaxObject", 5);
		editor.commit();

		// add the default datasources to the preferences file
		DataSourceStorage.getInstance().fillDefaultDataSources();
	}

	/**
	 * Create zoom bar and returns FrameLayout. FrameLayout is created to be
	 * hidden and not added to view, Caller needs to add the frameLayout to
	 * view, and enable visibility when needed.
	 * 
	 * @param SharedOreference
	 *            settings where setting is stored
	 * @return FrameLayout Hidden Zoom Bar
	 */
	private FrameLayout createZoomBar(SharedPreferences settings) {
		getMixViewData().setMyZoomBar(new SeekBar(this));
		getMixViewData().getMyZoomBar().setMax(100);
		getMixViewData().getMyZoomBar().setProgress(
				settings.getInt("zoomLevel", 65));
		getMixViewData().setZoomLevel(String.valueOf(calcZoomLevel()));
		getMixViewData().setZoomProgress(
				getMixViewData().getMyZoomBar().getProgress());
		getMixViewData().getMyZoomBar().setOnSeekBarChangeListener(
				myZoomBarOnSeekBarChangeListener);
		getMixViewData().getMyZoomBar().setVisibility(View.VISIBLE);
		FrameLayout frameLayout = new FrameLayout(this);

		frameLayout.setMinimumWidth(3000);
		frameLayout.addView(getMixViewData().getMyZoomBar());
		frameLayout.setPadding(10, 0, 10, 10);
		return frameLayout;
	}

	/**
	 * Create tool bar and returns FrameLayout. FrameLayout is created to be
	 * hidden and not added to view, Caller needs to add the frameLayout to
	 * view, and enable visibility when needed.
	 * 
	 * @return FrameLayout Hidden Zoom Bar
	 */
	private FrameLayout createToolBar() {
		int numColumns = 2;
		GridView myToolBar = new GridView(this);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(160,
				FrameLayout.LayoutParams.WRAP_CONTENT);
		myToolBar.setLayoutParams(params);
		myToolBar.setBackgroundResource(R.drawable.ic_menu_bg);
		myToolBar.setNumColumns(numColumns);
		myToolBar.setColumnWidth(80);
		myToolBar.setGravity(Gravity.CENTER);
		myToolBar.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
		myToolBar.setHorizontalSpacing(1);

		List<HashMap<String, Integer>> data = new ArrayList<HashMap<String, Integer>>();
		Integer ic[] = { R.drawable.ic_menu_run_page, R.drawable.ic_menu_manage };
		for (int i = 0; i < numColumns; i++) {
			HashMap<String, Integer> map = new HashMap<String, Integer>();
			map.put("image", ic[i]);
			data.add(map);
		}
		SimpleAdapter adapter = new SimpleAdapter(this, data,
				R.layout.toolbaritem, new String[] { "image" },
				new int[] { R.id.show_iv });
		myToolBar.setAdapter(adapter);
		myToolBar.setOnTouchListener(this);
		myToolBar.setVisibility(View.VISIBLE);
		myToolBar.setOnItemClickListener(new GridView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View arg1,
					int position, long arg3) {
				for (int i = 0; i < parent.getCount(); i++) {
					// View v=parent.getChildAt(i);
					if (position == i) {
						// 当前选中的Item改变背景颜色
						// arg1.setBackgroundResource(R.drawable.bottombg_h);
						switch (position) {
						/* List view */
						case 0:
							/*
							 * if the list of titles to show in alternative list
							 * view is not empty
							 */
							if (getDataView().getDataHandler().getMarkerCount() > 0) {
								Intent intent1 = new Intent(ARView.this,
										ARListView.class);
								startActivityForResult(intent1, 42);
							}
							/* if the list is empty */
							else {
								getMixViewData().getMixContext().doMsgPopUp(
										R.string.empty_list);
							}
							break;
						/* Map View */
						case 1:
							// Intent intent2 = new Intent(MixView.this,
							// MixMap.class);
							// startActivityForResult(intent2, 20);
							// Looper.prepare();
							String query;
							String region;
							try {
								// query = URLEncoder.encode("海底捞", "utf-8");
								// region = URLEncoder.encode("上海", "utf-8");
								double lat = getDataView().getCurFix()
										.getLatitude(), lon = getDataView()
										.getCurFix().getLongitude();
								String url = "http://api.map.baidu.com/place/search?query="
										+ getDataView().getQuery()
										+ "&location="
										+ lat
										+ ","
										+ lon
										+ "&radius="
										+ getDataView().getRadius()
										+ "&region=" + "&output=html";

								ARView.getDataView().getContext()
										.loadMixViewWebPage(url);
								// MixView.getDataView().getState().setDetailsView(true);
							} catch (Exception e) {
								e.printStackTrace();
							}
							break;
						default:
							break;
						}
					} else {
						// v.setBackgroundResource(R.drawable.portal_navigation_1bottom);
					}
				}
			}
		});

		getMixViewData().setMyToolBar(myToolBar);

		FrameLayout frameLayout = new FrameLayout(this);
		frameLayout.addView(getMixViewData().getMyToolBar());
		frameLayout.setPadding(0, 10, 10, 0);// left,top,right,bottom

		return frameLayout;
	}

	/* ********* Operator - Menu ***** */
	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// int base = Menu.FIRST;
	// /* define the first */
	// MenuItem item1 = menu.add(base, base, base,
	// getString(R.string.menu_item_1));
	// MenuItem item2 = menu.add(base, base + 1, base + 1,
	// getString(R.string.menu_item_2));
	// MenuItem item3 = menu.add(base, base + 2, base + 2,
	// getString(R.string.menu_item_3));
	// MenuItem item4 = menu.add(base, base + 3, base + 3,
	// getString(R.string.menu_item_4));
	// MenuItem item5 = menu.add(base, base + 4, base + 4,
	// getString(R.string.menu_item_5));
	// MenuItem item6 = menu.add(base, base + 5, base + 5,
	// getString(R.string.menu_item_6));
	// MenuItem item7 = menu.add(base, base + 6, base + 6,
	// getString(R.string.menu_item_7));
	//
	// /* assign icons to the menu items */
	// item1.setIcon(drawable.icon_datasource);
	// item2.setIcon(android.R.drawable.ic_menu_view);
	// item3.setIcon(android.R.drawable.ic_menu_mapmode);
	// item4.setIcon(android.R.drawable.ic_menu_zoom);
	// item5.setIcon(android.R.drawable.ic_menu_search);
	// item6.setIcon(android.R.drawable.ic_menu_info_details);
	// item7.setIcon(android.R.drawable.ic_menu_share);
	//
	// return true;
	// }
	//
	// @Override
	// public boolean onOptionsItemSelected(MenuItem item) {
	// switch (item.getItemId()) {
	// /* Data sources */
	// case 1:
	// if (!getDataView().isLauncherStarted()) {
	// Intent intent = new Intent(MixView.this, DataSourceList.class);
	// startActivityForResult(intent, 40);
	// } else {
	// Toast.makeText(this, getString(R.string.no_website_available),
	// Toast.LENGTH_LONG).show();
	// }
	// break;
	// /* List view */
	// case 2:
	// /*
	// * if the list of titles to show in alternative list view is not
	// * empty
	// */
	// if (getDataView().getDataHandler().getMarkerCount() > 0) {
	// Intent intent1 = new Intent(MixView.this, MixListView.class);
	// startActivityForResult(intent1, 42);
	// }
	// /* if the list is empty */
	// else {
	// Toast.makeText(this, R.string.empty_list, Toast.LENGTH_LONG)
	// .show();
	// }
	// break;
	// /* Map View */
	// case 3:
	// // Intent intent2 = new Intent(MixView.this, MixMap.class);
	// // startActivityForResult(intent2, 20);
	// break;
	// /* zoom level */
	// case 4:
	// getMixViewData().getMyZoomBar().setVisibility(View.VISIBLE);
	// getMixViewData().setZoomProgress(
	// getMixViewData().getMyZoomBar().getProgress());
	// break;
	// /* Search */
	// case 5:
	// onSearchRequested();
	// break;
	// /* GPS Information */
	// case 6:
	// Location currentGPSInfo = getMixViewData().getMixContext()
	// .getLocationFinder().getCurrentLocation();
	// AlertDialog.Builder builder = new AlertDialog.Builder(this);
	// builder.setMessage(getString(R.string.general_info_text) + "\n\n"
	// + getString(R.string.longitude)
	// + currentGPSInfo.getLongitude() + "\n"
	// + getString(R.string.latitude)
	// + currentGPSInfo.getLatitude() + "\n"
	// + getString(R.string.altitude)
	// + currentGPSInfo.getAltitude() + "m\n"
	// + getString(R.string.speed) + currentGPSInfo.getSpeed()
	// + "km/h\n" + getString(R.string.accuracy)
	// + currentGPSInfo.getAccuracy() + "m\n"
	// + getString(R.string.gps_last_fix)
	// + new Date(currentGPSInfo.getTime()).toString() + "\n");
	// builder.setNegativeButton(getString(R.string.close_button),
	// new DialogInterface.OnClickListener() {
	// public void onClick(DialogInterface dialog, int id) {
	// dialog.dismiss();
	// }
	// });
	// AlertDialog alert = builder.create();
	// alert.setTitle(getString(R.string.general_info_title));
	// alert.show();
	// break;
	// /* Case 6: license agreements */
	// case 7:
	// AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
	// builder1.setMessage(getString(R.string.license));
	// /* Retry */
	// builder1.setNegativeButton(getString(R.string.close_button),
	// new DialogInterface.OnClickListener() {
	// public void onClick(DialogInterface dialog, int id) {
	// dialog.dismiss();
	// }
	// });
	// AlertDialog alert1 = builder1.create();
	// alert1.setTitle(getString(R.string.license_title));
	// alert1.show();
	// break;
	//
	// }
	// return true;
	// }

	/* ******** Operators - Sensors ****** */

	private SeekBar.OnSeekBarChangeListener myZoomBarOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
		Toast t;

		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			float myout = calcZoomLevel();
			getDataView().setRadius(myout);
			getMixViewData().setZoomLevel(String.valueOf(myout));
			getMixViewData().setZoomProgress(
					getMixViewData().getMyZoomBar().getProgress());

			t.setText(getString(R.string.radius) + String.valueOf(myout));
			t.show();
		}

		@SuppressLint("ShowToast")
		public void onStartTrackingTouch(SeekBar seekBar) {
			Context ctx = seekBar.getContext();
			t = Toast.makeText(ctx, getString(R.string.radius),
					Toast.LENGTH_LONG);
			// zoomChanging= true;
		}

		public void onStopTrackingTouch(SeekBar seekBar) {
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();
			/* store the zoom range of the zoom bar selected by the user */
			editor.putInt("zoomLevel", getMixViewData().getMyZoomBar()
					.getProgress());
			editor.commit();
			getMixViewData().getMyZoomBar().setVisibility(View.VISIBLE);
			// zoomChanging= false;

			getMixViewData().getMyZoomBar().getProgress();

			t.cancel();
			// repaint after zoom level changed.
			repaint();
			setZoomLevel();
		}
	};

	public void onSensorChanged(SensorEvent evt) {
		try {

			if (evt.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				getMixViewData().getGrav()[0] = evt.values[0];
				getMixViewData().getGrav()[1] = evt.values[1];
				getMixViewData().getGrav()[2] = evt.values[2];

				if (augScreen != null)
					augScreen.postInvalidate();
			} else if (evt.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				getMixViewData().getMag()[0] = evt.values[0];
				getMixViewData().getMag()[1] = evt.values[1];
				getMixViewData().getMag()[2] = evt.values[2];

				if (augScreen != null)
					augScreen.postInvalidate();
			}

			SensorManager.getRotationMatrix(getMixViewData().getRTmp(),
					getMixViewData().getI(), getMixViewData().getGrav(),
					getMixViewData().getMag());

			// 获取旋转方向
			mRotation = Compatibility.getRotation(this);
			if (mRotation == 1) {
				SensorManager.remapCoordinateSystem(getMixViewData().getRTmp(),
						SensorManager.AXIS_X, SensorManager.AXIS_MINUS_Y,
						getMixViewData().getRot());
			} else {
				SensorManager.remapCoordinateSystem(getMixViewData().getRTmp(),
						SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_Z,
						getMixViewData().getRot());
			}

			getMixViewData().getTempR().set(getMixViewData().getRot()[0],
					getMixViewData().getRot()[1], getMixViewData().getRot()[2],
					getMixViewData().getRot()[3], getMixViewData().getRot()[4],
					getMixViewData().getRot()[5], getMixViewData().getRot()[6],
					getMixViewData().getRot()[7], getMixViewData().getRot()[8]);

			getMixViewData().getFinalR().toIdentity();
			getMixViewData().getFinalR().prod(getMixViewData().getM4());
			getMixViewData().getFinalR().prod(getMixViewData().getM1());
			getMixViewData().getFinalR().prod(getMixViewData().getTempR());
			getMixViewData().getFinalR().prod(getMixViewData().getM3());
			getMixViewData().getFinalR().prod(getMixViewData().getM2());
			getMixViewData().getFinalR().invert();

			getMixViewData().getHistR()[getMixViewData().getrHistIdx()]
					.set(getMixViewData().getFinalR());
			getMixViewData().setrHistIdx(getMixViewData().getrHistIdx() + 1);
			if (getMixViewData().getrHistIdx() >= getMixViewData().getHistR().length)
				getMixViewData().setrHistIdx(0);

			getMixViewData().getSmoothR().set(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f,
					0f);
			for (int i = 0; i < getMixViewData().getHistR().length; i++) {
				getMixViewData().getSmoothR().add(
						getMixViewData().getHistR()[i]);
			}
			getMixViewData().getSmoothR().mult(
					1 / (float) getMixViewData().getHistR().length);

			getMixViewData().getMixContext().updateSmoothRotation(
					getMixViewData().getSmoothR());
			// 获取Orientation
			SensorManager.getOrientation(getMixViewData().getRot(),
					orientationValues);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		try {
			int ea = event.getAction();
			switch (ea) {
			case MotionEvent.ACTION_DOWN:
				break;
			case MotionEvent.ACTION_MOVE:
				break;
			case MotionEvent.ACTION_UP:
				break;
			}
			return true;
		} catch (Exception ex) {
			// doError(ex);
			ex.printStackTrace();
			return super.onTouchEvent(event);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		try {
			killOnError();

			if (keyCode == KeyEvent.KEYCODE_BACK) {
				if (getDataView().isDetailsView()) {
					getDataView().keyEvent(keyCode);
					getDataView().setDetailsView(false);
					return true;
				} else {
					// TODO handle keyback to finish app correctly
					return super.onKeyDown(keyCode, event);
				}
			} else if (keyCode == KeyEvent.KEYCODE_MENU) {
				return super.onKeyDown(keyCode, event);
			} else {
				getDataView().keyEvent(keyCode);
				return false;
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			return super.onKeyDown(keyCode, event);
		}
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD
				&& accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE
				&& getMixViewData().getCompassErrorDisplayed() == 0) {
			// for (int i = 0; i < 2; i++) {
			// Toast.makeText(getMixViewData().getMixContext(),
			// getString(R.string.compass_data_error),
			// Toast.LENGTH_LONG).show();
			// }
			getMixViewData().getMixContext().doMsgPopUp(
					R.string.compass_data_error);
			getMixViewData().setCompassErrorDisplayed(
					getMixViewData().getCompassErrorDisplayed() + 1);
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// getDataView().setFrozen(false);
		// if (getMixViewData().getSearchNotificationTxt() != null) {
		// getMixViewData().getSearchNotificationTxt()
		// .setVisibility(View.GONE);
		// getMixViewData().setSearchNotificationTxt(null);
		// }
		return false;
	}

	/* ************ Handlers ************ */

	public void doError(Exception ex1) {
		if (!fError) {
			fError = true;

			setErrorDialog();

			ex1.printStackTrace();
			try {
			} catch (Exception ex2) {
				ex2.printStackTrace();
			}
		}

		try {
			augScreen.invalidate();
		} catch (Exception ignore) {
		}
	}

	public void killOnError() throws Exception {
		if (fError)
			throw new Exception();
	}

	private void handleIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			doMixSearch(query);
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}

	@SuppressWarnings("deprecation")
	private void doMixSearch(String query) {
		DataHandler jLayer = getDataView().getDataHandler();
		if (!getDataView().isFrozen()) {
			ARListView.originalMarkerList = jLayer.getMarkerList();
			// MixMap.originalMarkerList = jLayer.getMarkerList();
		}

		ArrayList<Marker> searchResults = new ArrayList<Marker>();
		Log.d("SEARCH-------------------0", "" + query);
		if (jLayer.getMarkerCount() > 0) {
			for (int i = 0; i < jLayer.getMarkerCount(); i++) {
				Marker ma = jLayer.getMarker(i);
				if (ma.getTitle().toLowerCase().indexOf(query.toLowerCase()) != -1) {
					searchResults.add(ma);
					/* the website for the corresponding title */
				}
			}
		}
		if (searchResults.size() > 0) {
			getDataView().setFrozen(true);
			jLayer.setMarkerList(searchResults);
		} else {
			getMixViewData().getMixContext().doMsgPopUp(
					R.string.search_failed_notification);
		}
	}

	/* ******* Getter and Setters ********** */

	public boolean isZoombarVisible() {
		return getMixViewData().getMyZoomBar() != null
				&& getMixViewData().getMyZoomBar().getVisibility() == View.VISIBLE;
	}

	public String getZoomLevel() {
		return getMixViewData().getZoomLevel();
	}

	/**
	 * @return the dWindow
	 */
	static PaintScreen getdWindow() {
		return dWindow;
	}

	/**
	 * @param dWindow
	 *            the dWindow to set
	 */
	static void setdWindow(PaintScreen dWindow) {
		ARView.dWindow = dWindow;
	}

	/**
	 * @return the dataView
	 */
	static DataView getDataView() {
		return dataView;
	}

	/**
	 * @param dataView
	 *            the dataView to set
	 */
	static void setDataView(DataView dataView) {
		if (getDataView() != null && dataView == null) {
			// clear stored data
			getDataView().clearEvents();
			getDataView().cancelRefreshTimer();
		}
		ARView.dataView = dataView;
	}

	public int getZoomProgress() {
		return getMixViewData().getZoomProgress();
	}

	private void setZoomLevel() {
		float myout = calcZoomLevel();

		getDataView().setRadius(myout);
		// caller has the to control of zoombar visibility, not setzoom
		// mixViewData.getMyZoomBar().setVisibility(View.INVISIBLE);
		getMixViewData().setZoomLevel(String.valueOf(myout));
		// setZoomLevel, caller has to call refreash download if needed.
		// mixViewData.setDownloadThread(new
		// Thread(mixViewData.getMixContext().getDownloadManager()));
		// mixViewData.getDownloadThread().start();

		getMixViewData().getMixContext().getDownloadManager().switchOn();

	};

}

/**
 * @author daniele
 * 
 */
class CameraSurface extends SurfaceView implements SurfaceHolder.Callback {
	ARView app;
	SurfaceHolder holder;
	Camera camera;

	CameraSurface(Context context) {
		super(context);

		try {
			app = (ARView) context;

			holder = getHolder();
			holder.addCallback(this);
			holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		} catch (Exception ex) {

		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		try {
			if (camera != null) {
				try {
					camera.stopPreview();
				} catch (Exception ignore) {
				}
				try {
					camera.release();
				} catch (Exception ignore) {
				}
				camera = null;
			}

			camera = Camera.open();
			camera.setPreviewDisplay(holder);
		} catch (Exception ex) {
			try {
				if (camera != null) {
					try {
						camera.stopPreview();
					} catch (Exception ignore) {
					}
					try {
						camera.release();
					} catch (Exception ignore) {
					}
					camera = null;
				}
			} catch (Exception ignore) {

			}
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		try {
			if (camera != null) {
				try {
					camera.stopPreview();
				} catch (Exception ignore) {
				}
				try {
					camera.release();
				} catch (Exception ignore) {
				}
				camera = null;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		try {
			Camera.Parameters parameters = camera.getParameters();
			try {
				List<Camera.Size> supportedSizes = null;
				// On older devices (<1.6) the following will fail
				// the camera will work nevertheless
				supportedSizes = Compatibility
						.getSupportedPreviewSizes(parameters);

				// preview form factor
				float ff = (float) w / h;
				Log.d(ARView.TAG, "Screen res: w:" + w + " h:" + h
						+ " aspect ratio:" + ff);

				// holder for the best form factor and size
				float bff = 0;
				int bestw = 0;
				int besth = 0;
				Iterator<Camera.Size> itr = supportedSizes.iterator();

				// we look for the best preview size, it has to be the closest
				// to the
				// screen form factor, and be less wide than the screen itself
				// the latter requirement is because the HTC Hero with update
				// 2.1 will
				// report camera preview sizes larger than the screen, and it
				// will fail
				// to initialize the camera
				// other devices could work with previews larger than the screen
				// though
				while (itr.hasNext()) {
					Camera.Size element = itr.next();
					// current form factor
					float cff = (float) element.width / element.height;
					// check if the current element is a candidate to replace
					// the best match so far
					// current form factor should be closer to the bff
					// preview width should be less than screen width
					// preview width should be more than current bestw
					// this combination will ensure that the highest resolution
					// will win
					Log.d(ARView.TAG, "Candidate camera element: w:"
							+ element.width + " h:" + element.height
							+ " aspect ratio:" + cff);
					if ((ff - cff <= ff - bff) && (element.width <= w)
							&& (element.width >= bestw)) {
						bff = cff;
						bestw = element.width;
						besth = element.height;
					}
				}
				Log.d(ARView.TAG, "Chosen camera element: w:" + bestw + " h:"
						+ besth + " aspect ratio:" + bff);
				// Some Samsung phones will end up with bestw and besth = 0
				// because their minimum preview size is bigger then the screen
				// size.
				// In this case, we use the default values: 480x320
				if ((bestw == 0) || (besth == 0)) {
					Log.d(ARView.TAG, "Using default camera parameters!");
					bestw = 480;
					besth = 320;
				}
				parameters.setPreviewSize(bestw, besth);
			} catch (Exception ex) {
				parameters.setPreviewSize(480, 320);
			}

			camera.setParameters(parameters);
			camera.startPreview();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}

class Cluster {
	private List<Label> labels = null;

	Cluster() {
		labels = new Vector<Label>();
	}

	void addLabel(int id, float x, float y, Marker ma) {
		Label label = new Label();
		label.setId(id);
		label.setLocation(x, y);
		label.setMarker(ma);
		labels.add(label);
	}

	void sort(boolean nearToFar) {
		if (nearToFar) {
			Collections.sort(labels);
		} else {
			Collections.reverse(labels);
		}
	}

	void clear() {
		labels.clear();
	}

	int size() {
		return labels.size();
	}

	Label get(int location) {
		return labels.get(location);
	}

	public void setScreenLocation(int index, float x, float y) {
		labels.get(index).setScreenLocation(x, y);
	}

	public void setTextLocation(int index, float x, float y) {
		labels.get(index).setTextLocation(x, y);
	}

	public void setTextSize(int index, float width, float height) {
		labels.get(index).setTextSize(width, height);
	}

	static float intersect(float a1, float a2, float b1, float b2) {
		float pMax = Math.max(a2, b2);
		float pMin = Math.min(a1, b1);
		float width = pMax - pMin + 1;
		float aWidth = a2 - a1 + 1;
		float bWidth = b2 - b1 + 1;
		float result = (aWidth + bWidth) - width;
		return result > 0 ? result : 0;
	}

	static boolean isOverlap(float x1, float y1, float w1, float h1, float x2,
			float y2, float w2, float h2) {
		float s1 = w1 * h1;
		float w = intersect(x1, x1 + w1, x2, x2 + w2);
		float h = intersect(y1, y1 + h1, y2, y2 + h2);
		float s = w * h;
		if (s / s1 > 0.5) {
			return true;
		}
		return false;
	}

	private boolean isOverlap(int a, int b) {
		float x1 = labels.get(a).getTextX();
		float y1 = labels.get(a).getTextY();
		float w1 = labels.get(a).getTextWidth();
		float h1 = labels.get(a).getTextHeight();
		float s1 = w1 * h1;

		float x2 = labels.get(b).getTextX();
		float y2 = labels.get(b).getTextY();
		float w2 = labels.get(b).getTextWidth();
		float h2 = labels.get(b).getTextHeight();
		float s2 = w2 * h2;

		float w = intersect(x1, x1 + w1, x2, x2 + w2);
		float h = intersect(y1, y1 + h1, y2, y2 + h2);
		float s = w * h;
		if (s / s1 > 0.1 && s / s2 > 0.1) {
			return true;
		}
		return false;
	}

	public void process(Cluster cluster) {
		for (int i = 0; i < cluster.size(); ++i) {
			for (int j = i + 1; j < cluster.size(); ++j) {
				boolean re = isOverlap(i, j);
				if (re) {
					if ((getState(i) == State.SINGLE || getState(i) == State.FATHER)
							&& getState(j) == State.SINGLE) {
						addChild(i, j);
					}
				} else {
					// Todo.
				}
			}
		}
	}

	void setState(int index, State state) {
		labels.get(index).setState(state);
	}

	State getState(int index) {
		return labels.get(index).getState();
	}

	void addChild(int father, int child) {
		setState(father, State.FATHER);
		setState(child, State.CHILD);
		labels.get(father).getChildList().add(labels.get(child));
	}

	public static class Label implements Comparable {
		private float x;
		private float y;
		private int id;
		private Marker marker;
		private float screenX;
		private float screenY;
		public float textX;
		public float textY;
		public float textW;
		public float textH;
		public float labelX;
		public float labelY;
		public float labelWidth;
		public float labelHeight;
		private Vector<Label> childList = null;
		public State state;

		public enum State {
			CHILD, FATHER, SINGLE
		};

		Label() {
			childList = new Vector<Label>();
			state = State.SINGLE;
		}

		public Vector<Label> getChildList() {
			return childList;
		}

		public void setState(State state) {
			this.state = state;
		}

		public State getState() {
			return state;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public void setLocation(float x, float y) {
			this.x = x;
			this.y = y;
		}

		public float getLocationX() {
			return x;
		}

		public float getLocationY() {
			return y;
		}

		public void setMarker(Marker marker) {
			this.marker = marker;
		}

		public Marker getMarker() {
			return marker;
		}

		public float getScreenX() {
			return screenX;
		}

		public float getScreenY() {
			return screenY;
		}

		public void setScreenLocation(float x, float y) {
			this.screenX = x;
			this.screenY = y;
		}

		public void setTextLocation(float x, float y) {
			this.textX = x;
			this.textY = y;
		}

		public void setTextSize(float w, float h) {
			this.textW = w;
			this.textH = h;
		}

		public float getTextWidth() {
			return textW;
		}

		public float getTextHeight() {
			return textH;
		}

		public float getTextX() {
			return textX;
		}

		public float getTextY() {
			return textY;
		}

		@Override
		public int compareTo(Object another) {
			if (this.marker.getDistance() > ((Label) another).marker
					.getDistance()) {
				return 1;
			} else if (this.marker.getDistance() < ((Label) another).marker
					.getDistance()) {
				return -1;
			}
			return 0;
		}
	}
}

class OpenGLRenderer implements Renderer {
	float one = 1f;
	ARView app;
	AugmentedGLView parent;
	int width = 0;
	int height = 0;
	private Projector mProjector;
	private LabelMaker mLabels;
	private Paint mLabelPaint;
	private Cluster cluster = null;
	private boolean isStop;

	public OpenGLRenderer(AugmentedGLView parent) {
		mProjector = new Projector();
		mLabelPaint = new Paint();
		mLabelPaint.setTextSize(20);
		mLabelPaint.setAntiAlias(true);
		mLabelPaint.setARGB(0xff, 0x00, 0xff, 0x00);
		cluster = new Cluster();
		this.parent = parent;
	}

	public void stopRenderer() {
		isStop = true;
	}

	public void resumeRenderer() {
		isStop = false;
	}

	public boolean isStop() {
		return isStop;
	}

	/*
	 * OpenGL 是一个非常底层的画图接口，它所使用的缓冲区存储结构是和我们的 java 程序中不相同的。 Java
	 * 是大端字节序(BigEdian)，而 OpenGL 所需要的数据是小端字节序(LittleEdian)。 所以，我们在将 Java 的缓冲区转化为
	 * OpenGL 可用的缓冲区时需要作一些工作。建立buff的方法如下
	 */
	public Buffer bufferUtil(int[] arr) {
		IntBuffer mBuffer;

		// 先初始化buffer,数组的长度*4,因为一个int占4个字节
		ByteBuffer qbb = ByteBuffer.allocateDirect(arr.length * 4);
		// 数组排列用nativeOrder
		qbb.order(ByteOrder.nativeOrder());

		mBuffer = qbb.asIntBuffer();
		mBuffer.put(arr);
		mBuffer.position(0);

		return mBuffer;
	}

	private Buffer bufferUtil(float[] arr) {
		FloatBuffer mBuffer;

		// 先初始化buffer,数组的长度*4,因为一个float占4个字节
		ByteBuffer qbb = ByteBuffer.allocateDirect(arr.length * 4);
		// 数组排列用nativeOrder
		qbb.order(ByteOrder.nativeOrder());

		mBuffer = qbb.asFloatBuffer();
		mBuffer.put(arr);
		mBuffer.position(0);

		return mBuffer;
	}

	public void setMixView(ARView mixView) {
		this.app = mixView;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.opengl.GLSurfaceView.Renderer#onSurfaceCreated(javax.microedition
	 * .khronos.opengles.GL10, javax.microedition.khronos.egl.EGLConfig)
	 */
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

		// 告诉系统对透视进行修正，会使透视图看起来好看点
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
		// 黑色背景
		gl.glClearColor(0, 0, 0, 0);// 红，绿，蓝，apaha
		// 启动阴影平滑
		gl.glShadeModel(GL10.GL_SMOOTH);

		// 设置深度缓存
		gl.glClearDepthf(1.0f);
		// 启用深度测试
		gl.glEnable(GL10.GL_DEPTH_TEST);
		// 所做深度测试的类型
		gl.glDepthFunc(GL10.GL_LEQUAL);

		if (mLabels != null) {
			mLabels.shutdown(gl);
		} else {
			mLabels = new LabelMaker(true, 512, 1024, app, parent);
		}

		mLabels.initialize(gl);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.opengl.GLSurfaceView.Renderer#onDrawFrame(javax.microedition.
	 * khronos.opengles.GL10)
	 */

	public void onDrawFrame(GL10 gl) {
		// long start = 0, end = 0;

		// start = System.currentTimeMillis();

		// 清除屏幕和深度缓存
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glPushMatrix();
		// 重置当前的模型观察矩阵
		gl.glLoadIdentity();

		// 允许设置顶点
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

		// 开启颜色渲染功能
		gl.glEnableClientState(GL10.GL_COLOR_BUFFER_BIT);
		// 设置颜色，单调着色 （r,g,b,a）
		gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);

		GLU.gluLookAt(gl, 0, 0, 1, 0, 0, 0, 0, 1, 0);
		gl.glTranslatef(0f, -1f, 0f);

		// 根据旋转方向进行角度调整
		float trRot = 0;
		if (ARView.mRotation == 0) {
			trRot = 90;
		} else if (ARView.mRotation == 1) {
			trRot = 0;
		}

		float tr0 = ARView.orientationValues[2];
		gl.glRotatef((float) (-tr0 * 180 / Math.PI) - trRot, 1.0f, 0.0f, 0.0f);

		float tr1 = ARView.orientationValues[1];
		gl.glRotatef((float) (tr1 * 180 / Math.PI), 0.0f, 1.0f, 0.0f);

		float tr2 = ARView.orientationValues[0];
		gl.glRotatef((float) (-tr2 * 180 / Math.PI), 0.0f, 0.0f, 1.0f);

		gl.glRotatef(90, 0.0f, 0.0f, 1.0f);// 机身坐标系和世界坐标系的绑定并没有使屏幕朝向正北，So。。。

		float tr3 = (float) (tr2 * 180 / Math.PI);
		int radius = 30;
		int side = 1;

		gl.glPushMatrix();
		gl.glColor4f(0.5f, 0.5f, 1.0f, 1.0f);
		// 画网格
		for (float i = -radius * one; i <= radius * one; i = i + side * one) {
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bufferUtil(new float[] {
					-i, radius * one, 0, -i, -radius * one, 0 }));
			gl.glDrawArrays(GL10.GL_LINES, 0, 2);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bufferUtil(new float[] {
					-radius * one, -i, 0, (radius + 1) * one, -i, 0 }));
			gl.glDrawArrays(GL10.GL_LINES, 0, 2);
		}
		// 方向针
		gl.glColor4f(0.0f, 1.0f, 0.0f, 1.0f);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bufferUtil(new float[] { 0, 0,
				0, radius * one, 0, 0 }));
		gl.glDrawArrays(GL10.GL_LINES, 0, 2);

		gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bufferUtil(new float[] { 0, 0,
				0, 0, radius * one, 0 }));
		gl.glDrawArrays(GL10.GL_LINES, 0, 2);

		gl.glColor4f(1.0f, 1.0f, 0.0f, 1.0f);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bufferUtil(new float[] { 0, 0,
				0, 0, 0, radius * one }));
		gl.glDrawArrays(GL10.GL_LINES, 0, 2);

		gl.glColor4f(0.0f, 1.0f, 1.0f, 1.0f);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bufferUtil(new float[] { 0, 0,
				0, 0, -radius * one, 0 }));
		gl.glDrawArrays(GL10.GL_LINES, 0, 2);

		gl.glColor4f(1.0f, 0.0f, 1.0f, 1.0f);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bufferUtil(new float[] { 0, 0,
				0, -radius * one, 0, 0 }));
		gl.glDrawArrays(GL10.GL_LINES, 0, 2);

		gl.glPopMatrix();

		gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
		gl.glTranslatef(0f, 1f, 0f);

		mProjector.getCurrentModelView(gl);

		if (ARView.getDataView() != null && cluster != null) {
			float scale = ARView.getDataView().getRadius() * 1000 / radius;
			DataHandler jLayer = ARView.getDataView().getDataHandler();

			cluster.clear();
			mLabels.beginAdding(gl);
			for (int i = 0; i < jLayer.getMarkerCount(); i++) {
				Marker pm = jLayer.getMarker(i);
				if (pm.getDistance() < app.calcZoomLevel() * 1000) {
					float x = (pm.getLocationVector().x / scale) * one;
					float y = (pm.getLocationVector().z / scale) * one;

					int id = mLabels.add(gl, "(" + pm.getTitle() + ")",
							mLabelPaint);
					// int id = mLabels.add(gl,
					// "("+(int)(x*100)/100.0+","+(int)(y*100)/100.0+")",
					// mLabelPaint);
					cluster.addLabel(id, x, y, pm);

				}
			}
			mLabels.endAdding(gl);
			cluster.sort(false);
			for (int i = 0; i < cluster.size(); ++i) {

				float[] scratch = new float[8];
				scratch[0] = cluster.get(i).getLocationX();
				scratch[1] = cluster.get(i).getLocationY();
				scratch[2] = 0.0f;
				scratch[3] = 1.0f;
				mProjector.project(scratch, 0, scratch, 4);
				float sx = scratch[4];
				float sy = scratch[5];
				cluster.setScreenLocation(i, sx, sy);
				float labelHeight = mLabels.getHeight(i);
				float labelWidth = mLabels.getWidth(i);
				cluster.setTextSize(i, labelWidth, labelHeight);
				float tx = sx - labelWidth * 0.5f;
				float ty = sy - labelHeight * 0.5f;
				cluster.setTextLocation(i, tx, ty);
			}

			// cluster.process(cluster);
			
			for (int i = 0; i < cluster.size(); ++i) {
				gl.glBindTexture(GL10.GL_TEXTURE_2D, 0);// 解除上个纹理绑定，不解除画不出来啊！
				float x = cluster.get(i).getLocationX();
				float y = cluster.get(i).getLocationY();
				// float sx = cluster.get(i).getScreenX();
				// float sy = cluster.get(i).getScreenY();

				if (cluster.get(i).getState() == State.FATHER) {

					float k = (float) Math.tan(tr2);
					if (((tr3 > 0 && tr3 < 90) && (x / k + y < 0))
							|| ((tr3 > 90 && tr3 < 180) && (x / k + y < 0))
							|| ((tr3 > -90 && tr3 < 0) && (x / k + y > 0))
							|| ((tr3 > -180 && tr3 < -90) && (x / k + y > 0))) {
						RectF labelRect = new RectF();
						mLabels.drawInOne(gl, cluster.get(i).getTextX(),
								cluster.get(i).getTextY(), cluster.get(i)
										.getId(), width, height, 1, labelRect);
						cluster.get(i).labelX = labelRect.left;
						cluster.get(i).labelY = labelRect.top;
						cluster.get(i).labelWidth = labelRect.right
								- labelRect.left;
						cluster.get(i).labelHeight = labelRect.bottom
								- labelRect.top;
					}

				} else if (cluster.get(i).getState() == State.SINGLE) {
					// Triangle triangle = new Triangle(new float[] { x, y, 0,
					// x,
					// y - one, -one, x, y + one, -one });
					// gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
					// triangle.draw(gl);

					float k = (float) Math.tan(tr2);
					if (((tr3 > 0 && tr3 < 90) && (x / k + y < 0))
							|| ((tr3 > 90 && tr3 < 180) && (x / k + y < 0))
							|| ((tr3 > -90 && tr3 < 0) && (x / k + y > 0))
							|| ((tr3 > -180 && tr3 < -90) && (x / k + y > 0))) {
						RectF labelRect = new RectF();
						mLabels.drawInOne(gl, cluster.get(i).getTextX(),
								cluster.get(i).getTextY(), cluster.get(i)
										.getId(), width, height, 0, labelRect);
						cluster.get(i).labelX = labelRect.left;
						cluster.get(i).labelY = labelRect.top;
						cluster.get(i).labelWidth = labelRect.right
								- labelRect.left;
						cluster.get(i).labelHeight = Math.abs(labelRect.top
								- labelRect.bottom);
					}

				}
			}

		}

		// 关闭颜色渲染
		gl.glDisableClientState(GL10.GL_COLOR_BUFFER_BIT);
		// 取消顶点设置
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glPopMatrix();

		// end = System.currentTimeMillis();
		// System.out.println("程序运行时间： " + (end - start) + "ms");
	}

	public void onTouch(float xPress, float yPress) {
		if (cluster != null) {
			int r = 20;
			// MixView.touchX = xPress;
			// MixView.touchY = height-yPress;
			Vector<Label> labels = new Vector<Label>();
			for (int i = 0; i < cluster.size(); ++i) {
				if (cluster.get(i) != null) {

					float textX = cluster.get(i).labelX;
					float textY = cluster.get(i).labelY;
					float textWidth = cluster.get(i).labelWidth;
					float textHeight = cluster.get(i).labelHeight;

					if (Cluster.isOverlap(xPress - r, yPress - r, 2 * r, 2 * r,
							textX, textY, textWidth, textHeight)) {
						// try {
						// if (cluster.get(i).getState() == State.SINGLE) {
						// Looper.prepare();
						// MixView.getDataView().getState().nextLStatus =
						// MixState.NOT_STARTED;
						// String webpage = MixUtils.parseAction(cluster
						// .get(i).getMarker().getURL());
						// MixView.getDataView().getState()
						// .setDetailsView(true);
						// MixView.getDataView().getContext()
						// .loadMixViewWebPage(webpage);
						//
						// } else if (cluster.get(i).getState() == State.FATHER)
						// {
						// Message msg = new Message();
						// msg.what = parent.ID_POPUPWINDOW;
						// msg.obj = cluster.get(i);
						// parent.getmHandler().sendMessage(msg);
						// }
						// } catch (Exception ex) {
						// ex.printStackTrace();
						// }
						// break;
						labels.add(cluster.get(i));
						// MixView.touchX = cluster.get(i).labelX;
						// MixView.touchY = cluster.get(i).labelY;
						// MixView.touchW = cluster.get(i).labelWidth;
						// MixView.touchH = cluster.get(i).labelHeight;
					}

				}
			}
			if (labels.size() == 1) {
//				Looper.prepare();
				Message msg = new Message();
				msg.what = parent.ID_LOADWEBPAGE;
				msg.obj = labels;
				parent.getmHandler().sendMessage(msg);
			} else if (labels.size() > 1) {
				Message msg = new Message();
				msg.what = parent.ID_POPUPWINDOW;
				msg.obj = labels;
				parent.getmHandler().sendMessage(msg);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.opengl.GLSurfaceView.Renderer#onSurfaceChanged(javax.microedition
	 * .khronos.opengles.GL10, int, int)
	 */
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		float radio = (float) width / height;

		this.width = width;
		this.height = height;

		// 设置OpenGL场景的大小
		gl.glViewport(0, 0, width, height);
		mProjector.setCurrentView(0, 0, width, height);
		// 设置投影矩阵,投影矩阵负责为场景增加透视
		gl.glMatrixMode(GL10.GL_PROJECTION);
		// 重置投影矩阵
		gl.glLoadIdentity();

		// 设置视口的大小 前四个参数去顶窗口的大小，分别是左，右，下，上，后两个参数分别是在场景中所能绘制深度的起点和终点
		gl.glFrustumf(-radio, radio, -1, 1, 1, 30);
		mProjector.getCurrentProjection(gl);
		// 指明任何新的变换即那个会影响 模型观察矩阵
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	void setLabels(Cluster cluster) {
		this.cluster = cluster;
	}

}

class AugmentedGLView extends GLSurfaceView implements OnTouchListener {
	ARView app;
	private OpenGLRenderer mRenderer;
	private Cluster cluster = null;
	private PopupWindow popWindow = null;
	public final int ID_POPUPWINDOW = 1;
	public final int ID_LOADWEBPAGE = 2;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			Vector<Label> labels = (Vector<Label>) msg.obj;
			switch (msg.what) {
			case ID_POPUPWINDOW:
				makePopWindow(labels);
				break;
			case ID_LOADWEBPAGE:
				ARView.getDataView().getState().nextLStatus = MixState.NOT_STARTED;
				String webpage = MixUtils.parseAction(labels.get(0).getMarker()
						.getURL());
				ARView.getDataView().getState().setDetailsView(true);
				try {
					ARView.getDataView().getContext()
							.loadMixViewWebPage(webpage);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
		};
	};

	public AugmentedGLView(Context context) {
		super(context);
		// 下面两行是做透明的 防止遮住摄像层
		this.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
		getHolder().setFormat(PixelFormat.TRANSLUCENT);

		mRenderer = new OpenGLRenderer(this);
		cluster = new Cluster();
		this.setGLWrapper(new GLSurfaceView.GLWrapper() {
			public GL wrap(GL gl) {
				return new MatrixTrackingGL(gl);
			}
		});
		this.setRenderer(mRenderer);
		mRenderer.setLabels(cluster);

		try {
			app = (ARView) context;
			app.killOnError();
			mRenderer.setMixView(app);
		} catch (Exception ex) {
			app.doError(ex);
		}
	}

	public void makePopWindow(Vector<Label> labels) {

		LayoutInflater lay = app.getLayoutInflater();
		View popupWindow_view = lay.inflate(R.layout.popupwindow, null);
		popWindow = new PopupWindow(popupWindow_view,
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true);

		popWindow.setBackgroundDrawable(getResources().getDrawable(
				R.drawable.bk));

		ListView listView = (ListView) popupWindow_view.findViewById(R.id.lv);

		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

		for (int i = 0; i < labels.size(); ++i) {
			Map<String, Object> cmap = new HashMap<String, Object>();
			cmap.put("title", labels.get(i).getMarker().getTitle());
			cmap.put(
					"info",
					Integer.toString((int) labels.get(i).getMarker()
							.getDistance())
							+ "米");
			cmap.put("img", R.drawable.easyicon_cn_24);
			cmap.put("url", labels.get(i).getMarker().getURL());
			list.add(cmap);
		}
		final ListAdapter adapter = new ListAdapter(this.getContext(), list,
				true);

		listView.setAdapter(adapter);
		listView.setItemsCanFocus(false);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				popWindow.dismiss();
				List<Map<String, Object>> mapList = adapter.getData();
				Map<String, Object> map = mapList.get(position);
				String url = (String) map.get("url");
				ARView.getDataView().getState().nextLStatus = MixState.NOT_STARTED;
				String webpage = MixUtils.parseAction(url);
				ARView.getDataView().getState().setDetailsView(true);

				try {
					ARView.getDataView().getContext()
							.loadMixViewWebPage(webpage);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		popWindow.setFocusable(true);
		popWindow.update();
		popWindow.setWidth(600);
		popWindow.setHeight(400);
		popWindow.showAtLocation(this, Gravity.CENTER_VERTICAL, 0, 0);
	}

	public boolean onTouchEvent(final MotionEvent me) {

		if (me.getAction() == MotionEvent.ACTION_UP) {
			final float height = this.getHeight();
			queueEvent(new Runnable() {
				// 这个方法会在渲染线程里被调用
				public void run() {
					mRenderer.onTouch(me.getX(), height - me.getY());
				}
			});
		}

		return true;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		return false;
	}

	public Handler getmHandler() {
		return mHandler;
	}

	public void setmHandler(Handler mHandler) {
		this.mHandler = mHandler;
	}

}

class AugmentedView extends View {
	ARView app;

	int xSearch = 200;
	int ySearch = 10;
	int searchObjWidth = 0;
	int searchObjHeight = 0;
	Paint zoomPaint = new Paint();

	public AugmentedView(Context context) {
		super(context);

		try {
			app = (ARView) context;

			app.killOnError();
		} catch (Exception ex) {
			app.doError(ex);
		}
	}

	@Override
	// TODO Auto-generated method stub
	protected void onDraw(Canvas canvas) {

		try {
			app.killOnError();
			// TODO Auto-generated catch block

			ARView.getdWindow().setWidth(canvas.getWidth());
			ARView.getdWindow().setHeight(canvas.getHeight());

			ARView.getdWindow().setCanvas(canvas);

			if (app.isZoombarVisible()) {
				zoomPaint.setColor(Color.WHITE);
				zoomPaint.setTextSize(14);

				int width = canvas.getWidth() / 100 * 4;
				int height = canvas.getHeight() / 100 * 95;
				canvas.drawText("0km", width, height, zoomPaint);

				int zoomProgress = app.getZoomProgress();
				if (zoomProgress > 92) {
					width = canvas.getWidth() / 100 * 92;
				} else if (zoomProgress < 8) {
					width = canvas.getWidth() / 100 * 8;
				} else {
					width = canvas.getWidth() / 100 * zoomProgress;
				}
				canvas.drawText(app.getZoomLevel() + "km", width, height,
						zoomPaint);
			}

			Log.d(ARView.TAG + " WorkFlow", "MixView - onDraw called");
			if (ARView.getDataView() != null) {
				if (!ARView.getDataView().isInited()) {
					ARView.getDataView().init(ARView.getdWindow().getWidth(),
							ARView.getdWindow().getHeight());
				}
				Log.d(ARView.TAG + " WorkFlow",
						"MixView - getDataView() != null");
				ARView.getDataView().draw(ARView.getdWindow());
			}

		} catch (Exception ex) {
			app.doError(ex);
		}

	}
}

/**
 * Internal class that holds Mixview field Data.
 * 
 * @author A B
 */
class MixViewDataHolder {
	private final MixContext mixContext;
	private float[] RTmp;
	private float[] Rot;
	private float[] I;
	private float[] grav;
	private float[] mag;
	private SensorManager sensorMgr;
	private List<Sensor> sensors;
	private Sensor sensorGrav;// TYPE_ACCELEROMETER 三轴加速度感应器
	private Sensor sensorMag;// TYPE_MAGNETIC_FIELD 磁场感应
	private int rHistIdx;
	private Matrix tempR;
	private Matrix finalR;
	private Matrix smoothR;
	private Matrix[] histR;
	private Matrix m1;
	private Matrix m2;
	private Matrix m3;
	private Matrix m4;
	private SeekBar myZoomBar;
	private WakeLock mWakeLock;
	private int compassErrorDisplayed;
	private String zoomLevel;
	private int zoomProgress;
	private TextView searchNotificationTxt;
	private GridView myToolBar;

	public MixViewDataHolder(MixContext mixContext) {
		this.mixContext = mixContext;
		this.RTmp = new float[9];
		this.Rot = new float[9];
		this.I = new float[9];
		this.grav = new float[3];
		this.mag = new float[3];
		this.rHistIdx = 0;
		this.tempR = new Matrix();
		this.finalR = new Matrix();
		this.smoothR = new Matrix();
		this.histR = new Matrix[60];
		this.m1 = new Matrix();
		this.m2 = new Matrix();
		this.m3 = new Matrix();
		this.m4 = new Matrix();
		this.compassErrorDisplayed = 0;
	}

	/* ******* Getter and Setters ********** */
	public MixContext getMixContext() {
		return mixContext;
	}

	public float[] getRTmp() {
		return RTmp;
	}

	public void setRTmp(float[] rTmp) {
		RTmp = rTmp;
	}

	public float[] getRot() {
		return Rot;
	}

	public void setRot(float[] rot) {
		Rot = rot;
	}

	public float[] getI() {
		return I;
	}

	public void setI(float[] i) {
		I = i;
	}

	public float[] getGrav() {
		return grav;
	}

	public void setGrav(float[] grav) {
		this.grav = grav;
	}

	public float[] getMag() {
		return mag;
	}

	public void setMag(float[] mag) {
		this.mag = mag;
	}

	public SensorManager getSensorMgr() {
		return sensorMgr;
	}

	public void setSensorMgr(SensorManager sensorMgr) {
		this.sensorMgr = sensorMgr;
	}

	public List<Sensor> getSensors() {
		return sensors;
	}

	public void setSensors(List<Sensor> sensors) {
		this.sensors = sensors;
	}

	public Sensor getSensorGrav() {
		return sensorGrav;
	}

	public void setSensorGrav(Sensor sensorGrav) {
		this.sensorGrav = sensorGrav;
	}

	public Sensor getSensorMag() {
		return sensorMag;
	}

	public void setSensorMag(Sensor sensorMag) {
		this.sensorMag = sensorMag;
	}

	public int getrHistIdx() {
		return rHistIdx;
	}

	public void setrHistIdx(int rHistIdx) {
		this.rHistIdx = rHistIdx;
	}

	public Matrix getTempR() {
		return tempR;
	}

	public void setTempR(Matrix tempR) {
		this.tempR = tempR;
	}

	public Matrix getFinalR() {
		return finalR;
	}

	public void setFinalR(Matrix finalR) {
		this.finalR = finalR;
	}

	public Matrix getSmoothR() {
		return smoothR;
	}

	public void setSmoothR(Matrix smoothR) {
		this.smoothR = smoothR;
	}

	public Matrix[] getHistR() {
		return histR;
	}

	public void setHistR(Matrix[] histR) {
		this.histR = histR;
	}

	public Matrix getM1() {
		return m1;
	}

	public void setM1(Matrix m1) {
		this.m1 = m1;
	}

	public Matrix getM2() {
		return m2;
	}

	public void setM2(Matrix m2) {
		this.m2 = m2;
	}

	public Matrix getM3() {
		return m3;
	}

	public void setM3(Matrix m3) {
		this.m3 = m3;
	}

	public Matrix getM4() {
		return m4;
	}

	public void setM4(Matrix m4) {
		this.m4 = m4;
	}

	public SeekBar getMyZoomBar() {
		return myZoomBar;
	}

	public void setMyZoomBar(SeekBar myZoomBar) {
		this.myZoomBar = myZoomBar;
	}

	public WakeLock getmWakeLock() {
		return mWakeLock;
	}

	public void setmWakeLock(WakeLock mWakeLock) {
		this.mWakeLock = mWakeLock;
	}

	public int getCompassErrorDisplayed() {
		return compassErrorDisplayed;
	}

	public void setCompassErrorDisplayed(int compassErrorDisplayed) {
		this.compassErrorDisplayed = compassErrorDisplayed;
	}

	public String getZoomLevel() {
		return zoomLevel;
	}

	public void setZoomLevel(String zoomLevel) {
		this.zoomLevel = zoomLevel;
	}

	public int getZoomProgress() {
		return zoomProgress;
	}

	public void setZoomProgress(int zoomProgress) {
		this.zoomProgress = zoomProgress;
	}

	public TextView getSearchNotificationTxt() {
		return searchNotificationTxt;
	}

	public void setSearchNotificationTxt(TextView searchNotificationTxt) {
		this.searchNotificationTxt = searchNotificationTxt;
	}

	public GridView getMyToolBar() {
		return myToolBar;
	}

	public void setMyToolBar(GridView myToolBar) {
		this.myToolBar = myToolBar;
	}

}
