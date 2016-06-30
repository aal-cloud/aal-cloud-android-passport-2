/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidble_demo_hy;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.iwit.bluetoothcommunication.util.encodeUtil;

/**
 * For a given BLE device, this Activity provides the user interface to connect,
 * display data, and display GATT services and characteristics supported by the
 * device. The Activity communicates with {@code BluetoothLeService}, which in
 * turn interacts with the Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity implements OnClickListener {
	private final static String TAG = DeviceControlActivity.class
			.getSimpleName();
	/**
	 * 血压
	 */
	private final static int BP = 1;
	/**
	 * 血糖
	 */
	private final static int GLU = 2;
	// private final static int BP = 1;
	private PowerManager pm;
	private PowerManager.WakeLock mWakeLock;

	private Commands commands;
	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

	private TextView mConnectionState;
	private TextView mDataField, mSendDataField, txt_message;
	private String mDeviceName;
	private String mDeviceAddress;
	private ExpandableListView mGattServicesList;
	private BluetoothLeService mBluetoothLeService;
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	private boolean mConnected = false;

	private final String LIST_NAME = "NAME";
	private final String LIST_UUID = "UUID";

	private BluetoothGatt mBluetoothGatt;
	// private EditText edt_message;
	private Button btn_bs_send, btn_bp_send;
	private String notifyString = "";
	private int deviceType;
	private byte[] sendDataByte;
	private byte[] getProcessData = new byte[8];

	private Runnable mRunnable = new Runnable() {
		@Override
		public void run() {

			mHandler.sendEmptyMessage(2);
		}
	};

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 0:
				displayData(notifyString);
				break;
			case 1:
				// 显示发送的命令值
				StringBuilder builder = new StringBuilder(sendDataByte.length);
				for (byte b : sendDataByte) {
					builder.append(String.format("%02X ", b));
				}
				displaySendData(builder.toString());

				break;
			case 2:

				// 此处要连接发送两次命令，不然血压收不到过程和结果包．
				sendDataByte();
				// sendDataByte();
				break;

			default:
				break;
			}

		}

	};

	// Code to manage Service lifecycle.
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
					.getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}

			if (mBluetoothLeService.connect(mDeviceAddress)) {
				mBluetoothGatt = mBluetoothLeService.getGatt();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};

	//
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				mConnected = true;
				updateConnectionState(R.string.connected, Color.GREEN);
				invalidateOptionsMenu();

			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
					.equals(action)) {
				mConnected = false;
				updateConnectionState(R.string.disconnected, Color.RED);
				invalidateOptionsMenu();
				clearUI();
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {

				if (mBluetoothGatt != null) {
					displayGattServices(mBluetoothGatt.getServices());
					btn_bp_send.setEnabled(true);
				}

			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				// 获取设备上传的数据

				byte[] notify = intent
						.getByteArrayExtra(BluetoothLeService.EXTRA_NOTIFY_DATA); // Log.e(TAG,
																					// //
																					// "有数据回来了"

				if (notify != null) {

					getData(notify);

				}
			}
		}
	};

	ArrayList<Byte> dataPackage = new ArrayList<Byte>();

	/**
	 * 校验命令包
	 */
	private void checkDataPackage(byte[] data) {
		for (int i = 0; i < data.length; i++) {
			dataPackage.add(data[i]);
		}

	}

	private void getData(byte notify[]) {
		Log.e(TAG, "227=" + notify.length);
		if (notify[0] == 0x55) {
			if (notify.length >= 10 && notify[1] == 0x10 && notify[2] == 0) {
				deviceType = notify[5];// 得到机种号：血压1；血糖2；
				mHandler.postDelayed(mRunnable, 300);
			}
			if (deviceType == 1) {
				if (notify.length == 6 && notify[1] == 0x06) {
					if (notify[2] == 1) {// 开始包
						mHandler.postDelayed(mRunnable, 300);
					} else if (notify[2] == 4) {// 结束包
						// Log.e(TAG, "238我收到结束包了");
					}
				} else if (notify.length >= 13 && notify[1] == (byte) 0x0f
						&& notify[2] == 3) {
					// 血压的结果包
					Log.e(TAG, "结果");
					getDeviceData(deviceType, notify);
					sendDataByte();
				}

			} else if (deviceType == 2) {
				if (notify[1] == 14 && notify[2] == 3) {
					// 血糖的结果包
					getDeviceData(deviceType, notify);
					sendDataByte();
				}
			}

		}

		StringBuilder builder = new StringBuilder(notify.length);
		for (byte b : notify) {
			builder.append(String.format("%02X ", b));
		}
		notifyString = builder.toString();
		mHandler.sendEmptyMessage(0);
	}

	private void clearUI() {
		mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
		// mDataField.setText(R.string.no_data);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gatt_services_characteristics);
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");

		final Intent intent = getIntent();
		mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
		commands = new Commands();
		// Sets up UI references.
		((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
		mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
		mConnectionState = (TextView) findViewById(R.id.connection_state);
		mDataField = (TextView) findViewById(R.id.data_value);

		mSendDataField = (TextView) findViewById(R.id.data_send_value);
		txt_message = (TextView) findViewById(R.id.txt_message);
		txt_message.setMovementMethod(ScrollingMovementMethod.getInstance());

		/**
		 * 血糖
		 */

		// btn_bs_send = (Button) findViewById(R.id.btn_bs_send);
		/**
		 * 血压
		 */
		btn_bp_send = (Button) findViewById(R.id.btn_bp_send);

		// btn_bs_send.setOnClickListener(this);
		btn_bp_send.setOnClickListener(this);
		getActionBar().setTitle(mDeviceName);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mWakeLock.acquire();
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		if (mBluetoothLeService != null) {
			final boolean result = mBluetoothLeService.connect(mDeviceAddress);
			Log.d(TAG, "Connect request result=" + result);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mWakeLock.release();
		unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mServiceConnection);
		mBluetoothLeService = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.gatt_services, menu);
		if (mConnected) {
			menu.findItem(R.id.menu_connect).setVisible(false);
			menu.findItem(R.id.menu_disconnect).setVisible(true);
		} else {
			menu.findItem(R.id.menu_connect).setVisible(true);
			menu.findItem(R.id.menu_disconnect).setVisible(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_connect:
			mBluetoothLeService.connect(mDeviceAddress);
			return true;
		case R.id.menu_disconnect:
			mBluetoothLeService.disconnect();
			return true;
		case android.R.id.home:
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void updateConnectionState(final int resourceId, final int color) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mConnectionState.setText(resourceId);
				mConnectionState.setTextColor(color);
			}
		});
	}

	/**
	 * 显示设备上传的数据；因有多个包数据，在包后用;号隔开．
	 * 
	 * @param data
	 */
	private void displayData(String data) {
		if (data != null) {
			mDataField.setText(data);
		}
	}

	private void displaySendData(String data) {
		if (data != null) {
			mSendDataField.setText(data);
		}
	}

	/**
	 * 转换后的数据
	 */
	private void displayTransformationData(String data) {
		// StringBuffer
		txt_message.setText(txt_message.getText() + "\n" + data);
		// sendDataByte();

	}

	private void displayGattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null)
			return;
		String uuid = null;
		String UUIDServiceString = getResources().getString(
				R.string.uuid_service);
		String UUIDWriteString = getResources().getString(R.string.uuid_write);
		String UUIDReadString = getResources().getString(R.string.uuid_read);
		ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
		ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
		mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

		// Loops through available GATT Services.
		for (BluetoothGattService gattService : gattServices) {
			HashMap<String, String> currentServiceData = new HashMap<String, String>();
			uuid = gattService.getUuid().toString();
			if (uuid.equals(SampleGattAttributes.GATT_SERVICE_PRIMARY)) {
				currentServiceData.put(LIST_NAME, UUIDServiceString);
				currentServiceData.put(LIST_UUID, uuid);
			} else {
				continue;
			}
			gattServiceData.add(currentServiceData);

			ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
			List<BluetoothGattCharacteristic> gattCharacteristics = gattService
					.getCharacteristics();
			ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

			// Loops through available Characteristics.
			for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
				charas.add(gattCharacteristic);
				HashMap<String, String> currentCharaData = new HashMap<String, String>();
				uuid = gattCharacteristic.getUuid().toString();
				if (uuid.equals(SampleGattAttributes.CHARACTERISTIC_WRITEABLE)) {
					currentCharaData.put(LIST_NAME, UUIDWriteString);
					currentCharaData.put(LIST_UUID, uuid);

				} else if (uuid
						.equals(SampleGattAttributes.CHARACTERISTIC_NOTIFY)) {
					currentCharaData.put(LIST_NAME, UUIDReadString);
					currentCharaData.put(LIST_UUID, uuid);

				}

				gattCharacteristicGroupData.add(currentCharaData);
			}
			mGattCharacteristics.add(charas);
			gattCharacteristicData.add(gattCharacteristicGroupData);
		}

		SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
				this, gattServiceData,
				android.R.layout.simple_expandable_list_item_2, new String[] {
						LIST_NAME, LIST_UUID }, new int[] { android.R.id.text1,
						android.R.id.text2 }, gattCharacteristicData,
				android.R.layout.simple_expandable_list_item_2, new String[] {
						LIST_NAME, LIST_UUID }, new int[] { android.R.id.text1,
						android.R.id.text2 });
		mGattServicesList.setAdapter(gattServiceAdapter);
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter
				.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_bp_send:
			sendDataByte();
			mHandler.sendEmptyMessage(1);
			break;
		}

	}

	/**
	 * 向设备发送命令
	 */
	private void sendDataByte() {

		sendDataByte = commands.getSystemdate(commands.CMD_HEAD,
				commands.CMD_LENGTH_ELEVEN, commands.CMD_CATEGORY_FIVE);
		SampleGattAttributes.sendMessage(mBluetoothGatt, sendDataByte);

	}

	/**
	 * 根据不同的设备　显示测量的结果值
	 * 
	 * @param type
	 */
	private void getDeviceData(int type, byte[] b) {

		switch (type) {
		case BP:
			clearUI();
			if (b.length > 8) {
				for (int i = 0; i < b.length; i++) {
					Log.e(TAG, "463===" + b[i]);
				}

				String resultBG = "高压：" + getShort(b, 8) + " 低：" + b[10]
						+ " 心：" + b[11];
				displayTransformationData(resultBG);

			}
			break;
		case GLU:
			String result4 = getShort(b, 9) + "";

			swithXueTang(result4);
			break;
		default:
			break;
		}

	}

	public static short getShort(byte[] b, int index) {
		Log.e(TAG, "508==" + b[index + 1] + "==" + (b[index + 1] << 8) + "=="
				+ b[index]);
		return (short) (((b[index + 1] << 8) | b[index] & 0xff));
	}

	public void swithXueTang(String result) {
		double resultValue = Double.parseDouble(result);
		resultValue = resultValue / 18;
		NumberFormat nf = NumberFormat.getInstance();
		nf.setRoundingMode(RoundingMode.HALF_UP);// 设置四舍五入
		nf.setMinimumFractionDigits(1);// 设置最小保留几位小数
		nf.setMaximumFractionDigits(1);// 设置最大保留几位小数
		displayTransformationData(nf.format(resultValue) + "mmol");

	}
}
