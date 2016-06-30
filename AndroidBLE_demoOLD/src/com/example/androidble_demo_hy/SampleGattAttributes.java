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

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import com.iwit.bluetoothcommunication.util.encodeUtil;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

/**
 * This class includes a small subset of standard GATT attributes for
 * demonstration purposes.
 */
public class SampleGattAttributes {
	// ע�������UUID��ͨ�������豸���ӡ����
	/**
	 * ������uuid��gatt������BluetoothGattService�����
	 */
	public static String GATT_SERVICE_PRIMARY = "00001000-0000-1000-8000-00805f9b34fb";
	/**
	 * ���Ծ����BluetoothGattCharacteristic����� �������ݵ�����uuid
	 */
	public static String CHARACTERISTIC_WRITEABLE = "00001001-0000-1000-8000-00805f9b34fb";
	/**
	 * notify����uuid
	 */
	public static String CHARACTERISTIC_NOTIFY = "00001002-0000-1000-8000-00805f9b34fb";
	public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
	/**
	 * ������ȡ���ݵ�����uuid
	 */
	public static String CHARACTERISTIC_READABLE = "00001003-0000-1000-8000-00805f9b34fb";
	public final static String BP_SERVICE = "D44BC439-ABFD-45A2-B575-925416129601";

	/**
	 * message����Ϊ16���������ַ�����ÿ�����17�ֽ�
	 * 
	 * @return
	 */
	public static boolean sendMessage(BluetoothGatt gatt, byte[] data) {
		if (gatt == null || data.length == 0)
			return false;

		BluetoothGattService service = gatt.getService(UUID
				.fromString(GATT_SERVICE_PRIMARY));
		// Log.e("TAG", "69" + service);

		if (service == null)
			return false;
		BluetoothGattCharacteristic characteristic = service
				.getCharacteristic(UUID.fromString(CHARACTERISTIC_WRITEABLE));

		if (characteristic == null)
			return false;
		characteristic.setValue(data);
		characteristic
				.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
		return gatt.writeCharacteristic(characteristic);
	}

	/**
	 * notify���Լ�characteristic.getProperties() ==
	 * BluetoothGattCharacteristic.PROPERTY_NOTIFY
	 * �õ�notify���ԣ�ʹ��notify����������ʵʱ���մӻ�����
	 */
	public static boolean notify(BluetoothGatt gatt) {
		if (gatt == null)
			return false;

		BluetoothGattService service = gatt.getService(UUID
				.fromString(GATT_SERVICE_PRIMARY));

		if (service == null)
			return false;

		BluetoothGattCharacteristic characteristic = service
				.getCharacteristic(UUID.fromString(CHARACTERISTIC_NOTIFY));
		if (characteristic != null) {
			BluetoothGattDescriptor descriptor = characteristic
					.getDescriptor(UUID
							.fromString(CLIENT_CHARACTERISTIC_CONFIG));
			descriptor
					.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			gatt.writeDescriptor(descriptor);
		}

		return gatt.setCharacteristicNotification(characteristic, true);
	}

	/**
	 * �õ�readable���ԣ�����������ȡ�ӻ�����
	 */
	public static boolean readMessage(BluetoothGatt gatt) {
		if (gatt == null)
			return false;

		BluetoothGattService service = gatt.getService(UUID
				.fromString(GATT_SERVICE_PRIMARY));

		if (service == null)
			return false;

		BluetoothGattCharacteristic characteristic = service
				.getCharacteristic(UUID.fromString(CHARACTERISTIC_READABLE));

		return gatt.readCharacteristic(characteristic);
	}

}
