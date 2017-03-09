package com.example.mobile_map;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.lbsapi.BMapManager;
import com.baidu.lbsapi.MKGeneralListener;
import com.baidu.lbsapi.panoramaview.PanoramaView;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMap.OnMapClickListener;
import com.baidu.mapapi.map.BaiduMap.OnMapLongClickListener;
import com.baidu.mapapi.map.BaiduMap.OnMarkerClickListener;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.navisdk.adapter.BNOuterTTSPlayerCallback;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BNRoutePlanNode.CoordinateType;
import com.baidu.navisdk.adapter.BNaviSettingManager;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.baidu.navisdk.adapter.BaiduNaviManager.NaviInitListener;
import com.baidu.navisdk.adapter.BaiduNaviManager.RoutePlanListener;
import com.example.mobile_map.MyOrientationListener.OnOrientationListener;

public class MainActivity extends Activity {

	private MapView mMapView;
	private BaiduMap mbaiduMap;
	private double mLongitude;
	private double mLatitude;
	private LocationClient locationClient;
	private float mCurrentX;
	private boolean isFirstIn = true;
	private MyLocationListener locationListener;
	private BitmapDescriptor mIconLocation;
	private LocationMode locationmode;
	private Context context;
	private MyOrientationListener myOrientationListener;
	private BitmapDescriptor mMarker;
	private RelativeLayout mMarkerly;
	private ArrayList<Marker> list;
	private LatLng mDestLocation;
	private LatLng mStartLocation;
	private BMapManager mBMapManager;
	public static List<Activity> activityList = new LinkedList<Activity>();

	private static final String APP_FOLDER_NAME = "BNSDKSimpleDemo";
	private String mSDCardPath = null;
	public static final String ROUTE_PLAN_NODE = "routePlanNode";
	public static final String SHOW_CUSTOM_ITEM = "showCustomItem";
	public static final String RESET_END_NODE = "resetEndNode";
	public static final String VOID_MODE = "voidMode";
	private final static String authBaseArr[] = {
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.ACCESS_FINE_LOCATION };
	private final static String authComArr[] = { Manifest.permission.READ_PHONE_STATE };
	private boolean hasInitSuccess = false;
	private boolean hasRequestComAuth = false;
	String authinfo = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 在使用SDK各组件之前初始化context信息，传入ApplicationContext
		// 注意该方法要再setContentView方法之前实现
		SDKInitializer.initialize(getApplicationContext());
		setContentView(R.layout.activity_main);
		this.context = this;
		initView();
		// 初始化定位
		initLocation();
		initMarker();
		if (initDirs()) {
			initNavi();
		}
		// 在地图上长按，添加目标点
		mbaiduMap.setOnMapLongClickListener(new OnMapLongClickListener() {

			@Override
			public void onMapLongClick(LatLng arg0) {
				Toast.makeText(getApplicationContext(), "设置目的地成功", 0).show();
				mDestLocation = arg0;
				addDestInfoOverlay(arg0);
//				//开启全景图
//				Intent intent = new Intent(context,PanoramaActivity.class);
//				MainActivity.this.startActivity(intent);
			}
		});
		mbaiduMap.setOnMarkerClickListener(new OnMarkerClickListener() {

			@Override
			public boolean onMarkerClick(Marker marker) {
				Bundle extrainfo = marker.getExtraInfo();
				Info info = (Info) extrainfo.getSerializable("info");
				ImageView iv = (ImageView) mMarkerly
						.findViewById(R.id.id_info_img);
				TextView name = (TextView) mMarkerly
						.findViewById(R.id.id_info_name);
				TextView distance = (TextView) mMarkerly
						.findViewById(R.id.id_info_distance);
				TextView zan = (TextView) mMarkerly
						.findViewById(R.id.id_info_zan);

				iv.setImageResource(info.getImgId());
				name.setText(info.getName());
				distance.setText(info.getDistance());
				zan.setText(info.getZan() + "");

				InfoWindow infoWindow;
				TextView tv = new TextView(context);
				tv.setBackgroundResource(R.drawable.location_tips);
				tv.setPadding(30, 20, 30, 50);
				tv.setText(info.getName());
				tv.setTextColor(Color.parseColor("#ffffff"));

				final LatLng latLng = marker.getPosition();
				Point p = mbaiduMap.getProjection().toScreenLocation(latLng);
				// p.y -= 47;
				LatLng ll = mbaiduMap.getProjection().fromScreenLocation(p);

				infoWindow = new InfoWindow(tv, ll, -47);
				mbaiduMap.showInfoWindow(infoWindow);
				mMarkerly.setVisibility(View.VISIBLE);
				return true;
			}
		});
		mbaiduMap.setOnMapClickListener(new OnMapClickListener() {

			@Override
			public boolean onMapPoiClick(MapPoi arg0) {
				return false;
			}

			@Override
			public void onMapClick(LatLng arg0) {
				mbaiduMap.hideInfoWindow();
				mMarkerly.setVisibility(View.GONE);
			}
		});
	}

	private boolean hasCompletePhoneAuth() {
		// TODO Auto-generated method stub

		PackageManager pm = this.getPackageManager();
		for (String auth : authComArr) {
			if (pm.checkPermission(auth, this.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
				return false;
			}
		}
		return true;
	}

	private void routeplanToNavi(boolean flag) {
		CoordinateType coType = CoordinateType.GCJ02;
		if (!hasInitSuccess) {
			Toast.makeText(MainActivity.this, "还未初始化!", Toast.LENGTH_SHORT)
					.show();
		}
		// 权限申请
		if (android.os.Build.VERSION.SDK_INT >= 23) {
			// 保证导航功能完备
			if (!hasCompletePhoneAuth()) {
				if (!hasRequestComAuth) {
					hasRequestComAuth = true;
					// this.requestPermissions(authComArr, authComRequestCode);
					return;
				} else {
					Toast.makeText(MainActivity.this, "没有完备的权限!",
							Toast.LENGTH_SHORT).show();
				}
			}

		}
		BDLocation bdLocation = new BDLocation();
		bdLocation.setLatitude(mStartLocation.latitude);
		bdLocation.setLongitude(mStartLocation.longitude);
		BDLocation starBD = bdTogc(bdLocation);

		BDLocation bdLocation2 = new BDLocation();
		bdLocation2.setLatitude(mDestLocation.latitude);
		bdLocation2.setLongitude(mDestLocation.longitude);
		BDLocation destBD = bdTogc(bdLocation2);

		BNRoutePlanNode sNode = null;
		BNRoutePlanNode eNode = null;
		sNode = new BNRoutePlanNode(starBD.getLongitude(),
				starBD.getLatitude(), "我的地点", null, coType);
		eNode = new BNRoutePlanNode(destBD.getLongitude(),
				destBD.getLatitude(), "目标地点", null, coType);

		if (sNode != null && eNode != null) {
			List<BNRoutePlanNode> list = new ArrayList<BNRoutePlanNode>();
			list.add(sNode);
			list.add(eNode);
			BaiduNaviManager.getInstance().launchNavigator(this, list, 1, flag,
					new DemoRoutePlanListener(sNode));
		}
	}

	/**
	 * bd09ll转gcj02
	 * 
	 * @param loc
	 * @return
	 */
	private BDLocation bdTogc(BDLocation loc) {
		return locationClient.getBDLocationInCoorType(loc,
				BDLocation.BDLOCATION_BD09LL_TO_GCJ02);
	}

	public class DemoRoutePlanListener implements RoutePlanListener {

		private BNRoutePlanNode mBNRoutePlanNode = null;

		public DemoRoutePlanListener(BNRoutePlanNode node) {
			mBNRoutePlanNode = node;
		}

		@Override
		public void onJumpToNavigator() {
			/*
			 * 设置途径点以及resetEndNode会回调该接口
			 */

			for (Activity ac : activityList) {

				if (ac.getClass().getName().endsWith("BNDemoGuideActivity")) {

					return;
				}
			}
			Intent intent = new Intent(MainActivity.this,
					BNDemoGuideActivity.class);
			Bundle bundle = new Bundle();
			bundle.putSerializable(ROUTE_PLAN_NODE, mBNRoutePlanNode);
			intent.putExtras(bundle);
			startActivity(intent);

		}

		@Override
		public void onRoutePlanFailed() {
			// TODO Auto-generated method stub
			Toast.makeText(MainActivity.this, "算路失败", Toast.LENGTH_SHORT)
					.show();
		}
	}

	private boolean initDirs() {
		mSDCardPath = getSdcardDir();
		if (mSDCardPath == null) {
			return false;
		}
		File f = new File(mSDCardPath, APP_FOLDER_NAME);
		if (!f.exists()) {
			try {
				f.mkdir();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	private String getSdcardDir() {
		if (Environment.getExternalStorageState().equalsIgnoreCase(
				Environment.MEDIA_MOUNTED)) {
			return Environment.getExternalStorageDirectory().toString();
		}
		return null;
	}

	private void initNavi() {

		BNOuterTTSPlayerCallback ttsCallback = null;

		// 申请权限
		if (android.os.Build.VERSION.SDK_INT >= 23) {

			if (!hasBasePhoneAuth()) {

				// this.requestPermissions(authBaseArr, authBaseRequestCode);
				return;

			}
		}

		BaiduNaviManager.getInstance().init(this, mSDCardPath, APP_FOLDER_NAME,
				new NaviInitListener() {
					@Override
					public void onAuthResult(int status, String msg) {
						if (0 == status) {
							authinfo = "key校验成功!";
						} else {
							authinfo = "key校验失败, " + msg;
						}
						MainActivity.this.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								Toast.makeText(MainActivity.this, authinfo,
										Toast.LENGTH_LONG).show();
							}
						});
					}

					@Override
					public void initSuccess() {
						Toast.makeText(MainActivity.this, "百度导航引擎初始化成功",
								Toast.LENGTH_SHORT).show();
						hasInitSuccess = true;
						initSetting();
					}

					@Override
					public void initStart() {
						Toast.makeText(MainActivity.this, "百度导航引擎初始化开始",
								Toast.LENGTH_SHORT).show();
					}

					@Override
					public void initFailed() {
						Toast.makeText(MainActivity.this, "百度导航引擎初始化失败",
								Toast.LENGTH_SHORT).show();
					}

				}, null, ttsHandler, ttsPlayStateListener);

	}

	/**
	 * 内部TTS播报状态回传handler
	 */
	private Handler ttsHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int type = msg.what;
			switch (type) {
			case BaiduNaviManager.TTSPlayMsgType.PLAY_START_MSG: {
				// showToastMsg("Handler : TTS play start");
				break;
			}
			case BaiduNaviManager.TTSPlayMsgType.PLAY_END_MSG: {
				// showToastMsg("Handler : TTS play end");
				break;
			}
			default:
				break;
			}
		}
	};

	/**
	 * 内部TTS播报状态回调接口
	 */
	private BaiduNaviManager.TTSPlayStateListener ttsPlayStateListener = new BaiduNaviManager.TTSPlayStateListener() {

		@Override
		public void playEnd() {
			// showToastMsg("TTSPlayStateListener : TTS play end");
		}

		@Override
		public void playStart() {
			// showToastMsg("TTSPlayStateListener : TTS play start");
		}
	};

	private void initSetting() {
		// BNaviSettingManager.setDayNightMode(BNaviSettingManager.DayNightMode.DAY_NIGHT_MODE_DAY);
		BNaviSettingManager
				.setShowTotalRoadConditionBar(BNaviSettingManager.PreViewRoadCondition.ROAD_CONDITION_BAR_SHOW_ON);
		BNaviSettingManager.setVoiceMode(BNaviSettingManager.VoiceMode.Veteran);
		// BNaviSettingManager.setPowerSaveMode(BNaviSettingManager.PowerSaveMode.DISABLE_MODE);
		BNaviSettingManager
				.setRealRoadCondition(BNaviSettingManager.RealRoadCondition.NAVI_ITS_ON);
	}

	private boolean hasBasePhoneAuth() {
		// TODO Auto-generated method stub

		PackageManager pm = this.getPackageManager();
		for (String auth : authBaseArr) {
			if (pm.checkPermission(auth, this.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 添加目标点
	 * 
	 * @param arg0
	 */
	private void addDestInfoOverlay(LatLng arg0) {
		mbaiduMap.clear();
		OverlayOptions options = new MarkerOptions().position(arg0)
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.maker))
				.zIndex(5);
		Marker marker = (Marker) mbaiduMap.addOverlay(options);
		list.add(marker);
	}

	/**
	 * 标注覆盖物
	 */
	private void initMarker() {
		mMarker = BitmapDescriptorFactory.fromResource(R.drawable.maker);
		mMarkerly = (RelativeLayout) findViewById(R.id.id_maker_ly);
	}

	private void initLocation() {
		locationmode = LocationMode.NORMAL;
		// 声明LocationClient类
		locationClient = new LocationClient(context);
		locationListener = new MyLocationListener();
		// 注册监听函数
		locationClient.registerLocationListener(locationListener);

		// 初始化图标
		mIconLocation = BitmapDescriptorFactory
				.fromResource(R.drawable.navi_map_gps_locked);
		myOrientationListener = new MyOrientationListener(context);
		myOrientationListener
				.setOnOrientationListener(new OnOrientationListener() {

					@Override
					public void onOrientationChanged(float x) {
						mCurrentX = x;
					}
				});

		LocationClientOption option = new LocationClientOption();
		// option.setLocationMode(LocationMode.Hight_Accuracy);//
		// 可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
		option.setCoorType("bd09ll");// 可选，默认gcj02，设置返回的定位结果坐标系
		option.setScanSpan(1000);// 可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
		option.setIsNeedAddress(true);// 可选，设置是否需要地址信息，默认不需要
		option.setOpenGps(true);// 可选，默认false,设置是否使用gps
		// option.setLocationNotify(true);//
		// 可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
		// option.setIsNeedLocationDescribe(true);//
		// 可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
		// option.setIsNeedLocationPoiList(true);//
		// 可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
		// option.setIgnoreKillProcess(false);//
		// 可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
		// option.SetIgnoreCacheException(false);//
		// 可选，默认false，设置是否收集CRASH信息，默认收集
		// option.setEnableSimulateGps(false);// 可选，默认false，设置是否需要过滤gps仿真结果，默认需要
		locationClient.setLocOption(option);
	}

	class MyLocationListener implements BDLocationListener {

		@Override
		public void onReceiveLocation(BDLocation location) {
			MyLocationData data = new MyLocationData.Builder()//
					.direction(mCurrentX)//
					.accuracy(location.getRadius())//
					.latitude(location.getLatitude())//
					.longitude(location.getLongitude())//
					.build();
			mbaiduMap.setMyLocationData(data);

			// 设置自定义图标
			MyLocationConfiguration config = new MyLocationConfiguration(
					locationmode, true, mIconLocation);
			mbaiduMap.setMyLocationConfigeration(config);

			// 获取经度、纬度
			mLongitude = location.getLongitude();
			mLatitude = location.getLatitude();
			// 当前的位置为开始位置。
			mStartLocation = new LatLng(location.getLatitude(),
					location.getLongitude());

			if (isFirstIn) {
				LatLng latLng = new LatLng(location.getLatitude(),
						location.getLongitude());
				MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
				mbaiduMap.animateMapStatus(msu);
				isFirstIn = false;
				Toast.makeText(context, location.getAddrStr(),
						Toast.LENGTH_LONG).show();
			}

		}
	}

	private void initView() {
		// 获取地图控件引用
		mMapView = (MapView) findViewById(R.id.bmapView);
		mbaiduMap = mMapView.getMap();
		MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(15.0f);
		mbaiduMap.setMapStatus(msu);
		list = new ArrayList<Marker>();
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.id_map_common:
			// 普通地图
			mbaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
			break;
		case R.id.id_map_site:
			// 卫星地图
			mbaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
			break;
		case R.id.id_map_traffic:
			// 实时交通图
			if (mbaiduMap.isTrafficEnabled()) {
				mbaiduMap.setTrafficEnabled(false);
				item.setTitle("实时交通(OFF)");
			} else {
				mbaiduMap.setTrafficEnabled(true);
				item.setTitle("实时交通(ON)");
			}
			break;
		case R.id.id_map_location:
			// 我的位置
			centerToMyLocation();
			break;
		case R.id.id_map_mocknav:
			// 模拟导航
			if (mDestLocation != null) {
				routeplanToNavi(false);
			} else {
				Toast.makeText(getApplicationContext(), "请先设置目标", 0).show();
			}
			break;
		case R.id.id_map_realnav:
			// 开始导航
			if (mDestLocation != null) {
				routeplanToNavi(true);
			} else {
				Toast.makeText(getApplicationContext(), "请先设置目标", 0).show();
			}
			break;
		case R.id.id_map_mode_common:
			locationmode = LocationMode.NORMAL;
			break;
		case R.id.id_map_mode_following:
			locationmode = LocationMode.FOLLOWING;
			break;
		case R.id.id_map_mode_compass:
			locationmode = LocationMode.COMPASS;
			break;
		case R.id.id_add_overlay:
			addOverlays(Info.infos);
			break;
		case R.id.id_remove_overlay:
			for (int i = 0; i < list.size(); i++) {
				list.get(i).remove();
			}
			list.clear();
			break;
		case R.id.id_map_hot:
			// 开启交通图
			if (mbaiduMap.isBaiduHeatMapEnabled()) {
				mbaiduMap.setBaiduHeatMapEnabled(false);
				item.setTitle("关闭热力地图(OFF)");
			} else {
				mbaiduMap.setBaiduHeatMapEnabled(true);
				item.setTitle("关闭热力地图(ON)");
			}
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * 添加覆盖物
	 * 
	 * @param infos
	 */
	private void addOverlays(List<Info> infos) {
		mbaiduMap.clear();
		LatLng latlng = null;
		Marker marker = null;
		OverlayOptions option;
		for (Info info : infos) {
			// 定义Maker坐标点
			latlng = new LatLng(info.getLatitude(), info.getLongitude());
			// 构建MarkerOption，用于在地图上添加Marker
			option = new MarkerOptions().position(latlng).icon(mMarker);
			// 在地图上添加Marker，并显示
			marker = (Marker) mbaiduMap.addOverlay(option);
			Bundle bundle = new Bundle();
			bundle.putSerializable("info", info);
			marker.setExtraInfo(bundle);
			list.add(marker);
		}
		// 更新坐标点
		MapStatusUpdate newLatLng = MapStatusUpdateFactory.newLatLng(latlng);
		mbaiduMap.setMapStatus(newLatLng);
	}

	/**
	 * 定位的我的位置
	 */
	private void centerToMyLocation() {
		LatLng latLng = new LatLng(mLatitude, mLongitude);
		MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
		mbaiduMap.animateMapStatus(msu);
	}

	@Override
	protected void onStart() {
		super.onStart();
		// 开启定位
		mbaiduMap.setMyLocationEnabled(true);
		if (!locationClient.isStarted()) {
			locationClient.start();
		}
		// 开启方向传感器
		myOrientationListener.start();

	}

	@Override
	protected void onStop() {
		super.onStop();
		mbaiduMap.setMyLocationEnabled(false);
		locationClient.stop();
		// 关闭方向传感器
		myOrientationListener.stop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// 在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
		mMapView.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// 在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
		mMapView.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// 在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
		mMapView.onResume();
	}
}
