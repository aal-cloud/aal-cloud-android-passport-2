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

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
	private final static String TAG = BluetoothLeService.class.getSimpleName();

	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String mBluetoothDeviceAddress;
	private BluetoothGatt mBluetoothGatt;
	private int mConnectionState = STATE_DISCONNECTED;

	private static final int STATE_DISCONNECTED = 0;
	private static final int STATE_CONNECTING = 1;
	private static final int STATE_CONNECTED = 2;

	public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
	public final static String EXTRA_NOTIFY_DATA = "com.example.bluetooth.le.EXTRA_NOTIFY_DATA";
	public final static String EXTRA_READ_DATA = "com.example.bluetooth.le.EXTRA_READ_DATA";

	public final static String BP_SERVICE = "D44BC439-ABFD-45A2-B575-925416129601";
	public final static String ERWEN_SERVICE = "BEF8D6C9-9C21-4C9E-B632-BD58C1009F9F";
	public final static String WRITE_CHARACTERISTIC = "D44BC439-ABFD-45A2-B575-925416129600";

	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

		/**
		 * ����״̬�仯�Ļص�
		 */
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {
			String intentAction;
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				Log.e(TAG, "78成功");
				mBluetoothGatt.discoverServices();
				intentAction = ACTION_GATT_CONNECTED;
				mConnectionState = STATE_CONNECTED;
				broadcastUpdate(intentAction);

			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				Log.e(TAG, "83失败");
				intentAction = ACTION_GATT_DISCONNECTED;
				mConnectionState = STATE_DISCONNECTED;
				broadcastUpdate(intentAction);
			}
		}

		/**
		 * ����gatt����Ļص�
		 */
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {

				Log.e(TAG, "98notify:" + SampleGattAttributes.notify(gatt));
				broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
				printUuid(gatt);
			} else {
				Log.e(TAG, "onServicesDiscovered received: " + status);
			}
		}

		/**
		 * ������readable���Ե����ݽ���
		 */
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			Log.e(TAG, "onCharacteristicRead()");
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_DATA_AVAILABLE, EXTRA_READ_DATA,
						characteristic);
			} else {
				Log.e(TAG, "onCharacteristicRead() - status = " + status);
			}
		}

		/**
		 * 
		 */
		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {

			byte[] value = characteristic.getValue();
			Log.e(TAG, "127=" + value.length);
			broadcastUpdate(ACTION_DATA_AVAILABLE, EXTRA_NOTIFY_DATA,
					characteristic);
		}

	};

	private void broadcastUpdate(final String action) {
		final Intent intent = new Intent(action);
		sendBroadcast(intent);
	}

	protected void printUuid(BluetoothGatt gatt) {
		List<BluetoothGattService> gattServices = gatt.getServices();
		if (gattServices != null) {
			for (BluetoothGattService bluetoothGattService : gattServices) {
				// Log.e(TAG, "146Service uuid: "
				// + bluetoothGattService.getUuid().toString());
				List<BluetoothGattCharacteristic> characteristics = bluetoothGattService
						.getCharacteristics();
				for (BluetoothGattCharacteristic bluetoothGattCharacteristic : characteristics) {
					// Log.e(TAG,
					// "149Characteristic uuid: "
					// + bluetoothGattCharacteristic.getUuid()
					// .toString()
					// + ", properties: "
					// + bluetoothGattCharacteristic
					// .getProperties());
					List<BluetoothGattDescriptor> descriptors = bluetoothGattCharacteristic
							.getDescriptors();
					for (BluetoothGattDescriptor bluetoothGattDescriptor : descriptors) {
						// Log.e(TAG, "153descriptor uuid: "
						// + bluetoothGattDescriptor.getUuid().toString());
					}
				}
			}
		}

	}

	ArrayList<Byte> bytes = new ArrayList<Byte>();
	byte[] getValue = null;

	private void broadcastUpdate(String action, String extra,
			BluetoothGattCharacteristic characteristic) {
		final Intent intent = new Intent(action);
		final byte[] data = characteristic.getValue();
		// Log.i(TAG, "uuid:" + characteristic.getUuid().toString());
		// double cnum = Math.ceil((data[9]+256)/18);

		if (data != null && data.length > 0) {

			// for (int i = 0; i < data.length; i++) {
			// //Log.e(TAG, "179=" + data[i]);
			// if (data[i] == 0x55) {
			//
			// }
			//
			// }

			// Log.e(TAG, "183=结束" );

			intent.putExtra(extra, data);
			sendBroadcast(intent);
		}
	}

	/**
	 * 对数据进行缓存
	 * 
	 * @param bytes
	 */
	// private void checkData(ArrayList<Byte> bytes) {
	// if (bytes.get(0) == 0x55) {
	//
	// }
	//
	// }

	public class LocalBinder extends Binder {
		BluetoothLeService getService() {
			return BluetoothLeService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// After using a given device, you should make sure that
		// BluetoothGatt.close() is called
		// such that resources are cleaned up properly. In this particular
		// example, close() is
		// invoked when the UI is disconnected from the Service.
		close();
		return super.onUnbind(intent);
	}

	private final IBinder mBinder = new LocalBinder();

	/**
	 * Initializes a reference to the local Bluetooth adapter.
	 * 
	 * @return Return true if the initialization is successful.
	 */
	public boolean initialize() {
		// For API level 18 and above, get a reference to BluetoothAdapter
		// through
		// BluetoothManager.
		if (mBluetoothManager == null) {
			mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			if (mBluetoothManager == null) {
				Log.e(TAG, "Unable to initialize BluetoothManager.");
				return false;
			}
		}

		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null) {
			Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
			return false;
		}

		return true;
	}

	/**
	 * Connects to the GATT server hosted on the Bluetooth LE device.
	 * 
	 * @param address
	 *            The device address of the destination device.
	 * 
	 * @return Return true if the connection is initiated successfully. The
	 *         connection result is reported asynchronously through the
	 *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 *         callback.
	 */
	public boolean connect(final String address) {
		if (mBluetoothAdapter == null || address == null) {
			Log.w(TAG,
					"BluetoothAdapter not initialized or unspecified address.");
			return false;
		}

		// Previously connected device. Try to reconnect.
		if (mBluetoothDeviceAddress != null
				&& address.equals(mBluetoothDeviceAddress)
				&& mBluetoothGatt != null) {
			Log.d(TAG,
					"Trying to use an existing mBluetoothGatt for connection.");
			if (mBluetoothGatt.connect()) {
				mConnectionState = STATE_CONNECTING;
				return true;
			} else {
				return false;
			}
		}

		final BluetoothDevice device = mBluetoothAdapter
				.getRemoteDevice(address);
		if (device == null) {
			Log.w(TAG, "Device not found.  Unable to connect.");
			return false;
		}
		// We want to directly connect to the device, so we are setting the
		// autoConnect
		// parameter to false.
		mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
		Log.e(TAG, "Trying to create a new connection.");
		mBluetoothDeviceAddress = address;
		mConnectionState = STATE_CONNECTING;
		return true;
	}

	/**
	 * Disconnects an existing connection or cancel a pending connection. The
	 * disconnection result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	public void disconnect() {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.disconnect();
	}

	public BluetoothGatt getGatt() {
		return mBluetoothGatt;
	}

	/**
	 * After using a given BLE device, the app must call this method to ensure
	 * resources are released properly.
	 */
	public void close() {
		if (mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.close();
		mBluetoothGatt = null;
	}

}
