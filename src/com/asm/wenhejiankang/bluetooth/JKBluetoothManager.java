package com.asm.wenhejiankang.jkbluetooth;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class JKBluetoothManager {
	public static final String MUUID = "00001101-0000-1000-8000-00805F9B34FB";

	/*蓝牙状态值
	 * STATE_CLOSED 关闭的
	 * STATE_OPENFAIL 打开失败，临时状态
	 * STATE_OPENED 打开的
	 * STATE_DISCOVERYING 正在寻找设备
	 * STATE_FOUND 找到设备，临时状态
	 * STATE_DISCOVERIED 寻找结束
	 * STATE_CONNECTING 正在连接
	 * STATE_CONNECTED 连接成功
	 * STATE_CONNECTEDFAIL 连接失败，临时状态
	 * STATE_DISCONNECTED 中断连接，临时状态
	 */
	public static final int STATE_CLOSED = 0;
	public static final int STATE_OPENFAIL = 1;
	public static final int STATE_OPENED = 2;
	public static final int STATE_DISCOVERYING = 3;
	public static final int STATE_FOUND = 4;
	public static final int STATE_DISCOVERIED = 5;
	public static final int STATE_CONNECTING = 6;
	public static final int STATE_CONNECTED = 7;
	public static final int STATE_CONNECTEDFAIL = 8;
	public static final int STATE_DISCONNECTED=9;

	private List<BluetoothDevice> devices = null;
	private JKBluetoothManager manager = null;
	private BluetoothAdapter adapter = null;
	private BroadcastReceiver mReceiver = null;
	private Context mContext = null;
	private OnStateChangedListener stateChangedListener = null;
	private OnBloudOxygenDataChangedListener bloudOxygenDataChangedListener = null;
	private OnGlycemicIndexDataChangedListener glycemicIndexDataChangedListener = null;
	private OnBloudPressureDataChangedListener bloudPressureDataChangedListener = null;
	private OnAnimalHeatDataChangedListener animalHeatDataChangedListener = null;
	private DataCommunicationThread mDataCommunicationThread = null;

	private int state = 0;
	private BufferedInputStream bis = null;
	private BufferedOutputStream bos = null;
	private BluetoothSocket socket = null;
	private BluetoothDevice currentDevice = null;

	private JKBluetoothManager() {
		state = STATE_CLOSED;
		adapter = BluetoothAdapter.getDefaultAdapter();
		if (!adapter.isEnabled())
			if (!adapter.enable())
				notifyState(STATE_OPENFAIL);
		setState(STATE_OPENED);
		devices = new ArrayList<BluetoothDevice>();
		mReceiver = new BlueToothReceiver();
		stateChangedListener = new OnStateChangedListener();
		bloudOxygenDataChangedListener = new OnBloudOxygenDataChangedListener();
		glycemicIndexDataChangedListener = new OnGlycemicIndexDataChangedListener();
		bloudPressureDataChangedListener = new OnBloudPressureDataChangedListener();
		animalHeatDataChangedListener = new OnAnimalHeatDataChangedListener();
	}

	/*
	 * 得到一个实例
	 * 第一次获得实例要调用getInstance(Context c)传入context
	 */
	public JKBluetoothManager getInstance() {
		if (manager == null)
			manager = new JKBluetoothManager();
		return manager;
	}

	public JKBluetoothManager getInstance(Context c) {
		mContext = c;
		return getInstance();
	}

	/*
	 * 设置蓝牙状态监听器
	 */
	public void setOnStateChangedListener(OnStateChangedListener sc) {
		stateChangedListener = sc;
	}
	/*
	 * 设置血氧仪监听器
	 */
	public void setOnBloudOxygenDataChangedListener(OnBloudOxygenDataChangedListener l) {
		bloudOxygenDataChangedListener = l;
	}
	/*
	 * 设置血糖仪监听器
	 */
	public void setOnGlycemicIndexDataChangedListener(OnGlycemicIndexDataChangedListener l) {
		glycemicIndexDataChangedListener = l;
	}
	/*
	 * 设置血压仪监听器
	 */
	public void setOnBloudPressureDataChangedListener(OnBloudPressureDataChangedListener l) {
		bloudPressureDataChangedListener = l;
	}

	/*
	 * 查找设备，可以在蓝牙状态监听器里监听
	 */
	public void discoveryValidDevices() {
		registerDevicesFoundBroadCast();
		devices.clear();
		if (adapter.isDiscovering())
			adapter.cancelDiscovery();
		adapter.startDiscovery();
		setState(STATE_DISCOVERYING);
	}

	/*
	 * 中止查找设备
	 */
	public void cancleDiscoveryValidDevices() {
		if (state == STATE_DISCOVERYING) {
			adapter.cancelDiscovery();
			notifyState(STATE_DISCOVERYING);
			setState(STATE_DISCOVERIED);
		}
	}

	/*
	 * 连接
	 */
	public boolean connect(BluetoothDevice de) {
		if (state != STATE_DISCOVERIED)
			return false;
		if (devices.size() <= 0)
			return false;
		if (!BluetoothAdapter.checkBluetoothAddress(de.getAddress()))
			return false;
		if (state == STATE_DISCOVERYING || adapter.isDiscovering())
			adapter.cancelDiscovery();
		setState(STATE_CONNECTING);
		new ConnectThread(de, MUUID).start();
		return true;
	}

	/*
	 * 中止连接
	 */
	public boolean disConnect() throws IOException {
		if (state != STATE_CONNECTED)
			return false;
		if (socket == null)
			return false;
		mDataCommunicationThread.close();
		socket.close();
		bis.close();
		bos.close();
		socket = null;
		mDataCommunicationThread = null;
		bis = null;
		bos = null;
		notifyState(STATE_DISCONNECTED);
		state=STATE_DISCOVERIED;
		return true;
	}
	
	/*
	 * 获得查找到的设备列表
	 * 设备名称，地址，类别可以自己获取
	 */
	public List<BluetoothDevice> getDeviceList(){
		if(state==STATE_DISCOVERYING)
			return null;
		return devices;
	}
	
	/*
	 * 获得蓝牙状态
	 */
	public int getState() {
		return state;
	}

	/*
	 * 应用退出应调用此方法释放资源
	 */
	public void destory() throws IOException{
		disConnect();
		adapter.disable();
	}
	
	
	
	private void setState(int i) {
		state = i;
		notifyState(state);
	}

	private void notifyState(int i) {
		stateChangedListener.onStateChanged(state);
	}

	private boolean registerDevicesFoundBroadCast() {
		if (mContext == null)
			return false;
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		mContext.registerReceiver(mReceiver, filter);
		return true;
	}

	private boolean unRegisterDevicesFoundBroadCast() {
		if (mContext == null)
			return false;
		mContext.unregisterReceiver(mReceiver);
		return true;
	}

	class BlueToothReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				setState(STATE_DISCOVERYING);
			} else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				devices.add(d);
				notifyState(STATE_FOUND);
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				setState(STATE_DISCOVERIED);
				unRegisterDevicesFoundBroadCast();
			}
		}
	}

	public class OnStateChangedListener {
		public void onStateChanged(int s) {
		};
	}

	/*--------------------------------以下各监听类值若为-1或null，则说明此帧不含该数据，应该无视------------------------------*/
	/*
	 * highest 高压
	 * lowest 低压
	 * rate 心律
	 */
	public class OnBloudPressureDataChangedListener {
		public void onDataChanged(int highest, int lowest, int rate) {

		}
	}

	/*
	 * gi
	 * 血糖指数
	 */
	public class OnGlycemicIndexDataChangedListener {
		public void onDataChanged(float gi) {

		}
	}

	/*
	 * ls 血氧体积描记图数据 ,10个字节
	 * saturability 血氧饱和度
	 * rate 脉率
	 * vqi 灌注指数
	 */
	public class OnBloudOxygenDataChangedListener {
		public void onDataChanged(int[] ls, float saturability, int rate, float vqi) {

		}
	}

	/*
	 * tp 温度
	 * temMetric 温度度量
	 * 		0 摄氏度 
	 * 		1华氏度
	 * temType 温度类型
	 * 		0人体温度
	 * 		1物体温度
	 * 		2环境温度
	 * state 状态
	 * 		0正常
	 * 		1测量温度过低
	 * 		2测量温度过高
	 * 		3环境温度过低
	 * 		4环境温度过高
	 * 		5EEPROM出错
	 * 		6传感器出错
	 */
	public class OnAnimalHeatDataChangedListener {
		public void onDataChanged(float tp, int temMetric,int temType,int state) {
			
		}
	}

	class ConnectThread extends Thread {
		BluetoothDevice de = null;
		String suuid = null;

		public ConnectThread(BluetoothDevice d, String s) {
			de = d;
			suuid = s;
		}

		@Override
		public synchronized void start() {
			// TODO 自动生成的方法存根
			try {
				super.start();
				socket = de.createInsecureRfcommSocketToServiceRecord(UUID.fromString(suuid));
				socket.connect();
				bis = new BufferedInputStream(socket.getInputStream());
				bos = new BufferedOutputStream(socket.getOutputStream());
				currentDevice = de;
				setState(STATE_CONNECTED);
				mDataCommunicationThread = new DataCommunicationThread(bis, bos);
				mDataCommunicationThread.start();
			} catch (Exception e) {
				e.printStackTrace();
				notifyState(STATE_CONNECTEDFAIL);
				try {
					throw e;
				} catch (Exception e1) {
					// TODO 自动生成的 catch 块
					e1.printStackTrace();
				}
			}
		}
	}

	class DataCommunicationThread extends Thread {
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		boolean isOn = false;

		public DataCommunicationThread(BufferedInputStream in, BufferedOutputStream out) {
			// TODO 自动生成的构造函数存根
			this.in = in;
			this.out = out;
			isOn = true;
		}

		@Override
		public void run() {
			// TODO 自动生成的方法存根
			try {
				super.run();
				int[] data = new int[20];
				boolean isFirst=true;
				while (isOn) {
					int i, j;
					if(!isFirst)
					in.reset();
					isFirst=false;
					for (i = 0; i < 20; i++) {
						in.mark(20);
						if ((j = in.read()) == 0xFE && i != 0)
							break;
						data[i] = j;
					}

					if (data[0] != 0xFE)
						throw new JKBlutToothException("data head error");
					if (data[1] != 0x6A)
						throw new JKBlutToothException("is not buletooth");
					switch (data[2]) {
					// 血氧仪
					case 0x76:
							if (data[3] == 0x51) {
								// 体积描记图
								bloudOxygenDataChangedListener.onDataChanged(subInts(data, 5, 10), -1, -1, -1);
							} else if (data[3] == 0x52) {
								// 血氧饱和度和脉率数据
								bloudOxygenDataChangedListener.onDataChanged(null, data[7], data[6], data[8]);
							} else if (data[3] == 0x53) {
								// 血氧饱和度和脉率报警限
								bloudOxygenDataChangedListener.onDataChanged(null, data[7], data[6], data[8]);
							}
						break;
					// 血压仪
					case 0x73:
							if (data[3] == 0x5A) {
								bloudPressureDataChangedListener.onDataChanged(data[4], data[5], data[6]);
							}
						break;
					// 耳温仪
					case 0x72:
							if (data[3] == 0x5A) {
								if(data[4]==0x55)
									break;
								//StringBuilder sb=new StringBuilder(Integer.parseInt(Integer.toHexString(data[4]) + Integer.toHexString(data[5]),16));
								String s1=Integer.toHexString(data[4]);
								String s2=Integer.toHexString(data[5]);
								i=Integer.parseInt(s1+s2,16);
								StringBuffer sb=new StringBuffer();
								sb.append(i);
								float f = Float.valueOf(sb.insert(sb.length()-2, ".").toString());
								byte ss = (byte) data[6];
								int metric, ty, st;
								ty = st = 0;
								metric = getBit(ss, 7);
								int b6 = getBit(ss, 6);
								int b5 = getBit(ss, 5);
								if (b6 == 0 && b5 == 0)
									ty = 0;
								else if (b6 == 0 && b5 == 1)
									ty = 1;
								else if (b6 == 1 && b5 == 0)
									ty = 2;
								int b4 = getBit(ss, 4);
								int b3 = getBit(ss, 3);
								int b2 = getBit(ss, 2);
								if (b4 == 0 && b3 == 0 && b2 == 0)
									st = 0;
								else if (b4 == 0 && b3 == 0 && b2 == 1)
									st = 1;
								else if (b4 == 0 && b3 == 1 && b2 == 0)
									st = 2;
								else if (b4 == 0 && b3 == 1 && b2 == 1)
									st = 3;
								else if (b4 == 1 && b3 == 0 && b2 == 0)
									st = 4;
								else if (b4 == 1 && b3 == 0 && b2 == 1)
									st = 5;
								else if (b4 == 1 && b3 == 1 && b2 == 0)
									st = 6;
								else if (b4 == 1 && b3 == 1 && b2 == 1)
									st = 7;
								animalHeatDataChangedListener.onDataChanged(f, metric, ty, st);
							}
						break;
					// 血糖仪
					case 0x75:
							if (data[3] == 0x5A) {
								if(data[6]!=0x88)
									break;
								glycemicIndexDataChangedListener.onDataChanged(Float.valueOf(Integer.parseInt(Integer.toHexString(data[4]) + Integer.toHexString(data[5]),16) / 18));
							}
					default:
						throw new JKBlutToothException("not a valid device");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		public void close() {
			isOn = false;
		}

		public void writeData(byte[] b) {

		}
	}
	
	public static int[] subInts(int[] b, int bpos, int length) {
		int[] da = new int[length];
		for (int i = 0; i < length; i++)
			da[i] = b[bpos + i];
		return da;
	}

	public static int[] bytesToInts(byte[] b) {
		int[] i = new int[b.length];
		for (int j = 0; j < b.length; j++)
			i[j] = (int) b[j];
		return i;
	}

	public static byte[] intsToBytes(int[] i) {
		byte[] b = new byte[i.length];
		for (int j = 0; j < i.length; j++)
			b[j] = (byte) i[j];
		return b;
	}

	public static int getBit(byte b, int i) {
		return (b << i) >> 7;
	}

	
}
