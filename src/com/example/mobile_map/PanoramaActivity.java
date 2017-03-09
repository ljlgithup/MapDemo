package com.example.mobile_map;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.baidu.lbsapi.BMapManager;
import com.baidu.lbsapi.MKGeneralListener;
import com.baidu.lbsapi.panoramaview.PanoramaView;

public class PanoramaActivity extends Activity {

	private PanoramaView mPanoView;
	private BMapManager mBMapManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 初始化BMapManager要在setContentView()之前调用。
		initBMapManager();
		setContentView(R.layout.activity_panorama);
		mPanoView = (PanoramaView) findViewById(R.id.panorama);
		mPanoView.setShowTopoLink(true);// 设置是否显示临街头的箭头
		// 设置全景图的缩放级别
		// level分为1-5级
		mPanoView.setPanoramaLevel(2);
		// 设置全景图的显示的级别
		mPanoView
				.setPanoramaImageLevel(PanoramaView.ImageDefinition.ImageDefinitionHigh);
		// double lon = 116.404;
		// double lat = 39.945;
		mPanoView.setPanorama(12971348, 4826239);
	}

	private void initBMapManager() {
		mBMapManager = new BMapManager(this.getApplicationContext());
		mBMapManager.init(new MKGeneralListener() {

			@Override
			public void onGetPermissionState(int iError) {
				// 非零值表示key验证未通过
				if (iError != 0) {
					// 授权Key错误：
					Toast.makeText(
							PanoramaActivity.this,
							"请在AndoridManifest.xml中输入正确的授权Key,并检查您的网络连接是否正常！error: "
									+ iError, Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(PanoramaActivity.this, "key认证成功",
							Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	@Override
	protected void onPause() {
		super.onPause();
		mPanoView.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mPanoView.onResume();
	}

	@Override
	protected void onDestroy() {
		mPanoView.destroy();
		super.onDestroy();
	}
}
