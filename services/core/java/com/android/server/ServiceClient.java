package com.android.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import android.util.Slog;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.rmi.ObjectSpace;

import dalvik.system.BlockGuard;

/**
 * A service share client
 * 
 * @author yli118
 * 
 */
public class ServiceClient {
	private static final String TAG = "ServiceClient";

	/**
	 * Service sharing client endpoint
	 */
	private static Client client = null;

	/**
	 * Kryo object for serialization
	 */
	private static Kryo kryo = null;

	/**
	 * ObjectSpace used in this client
	 */
	private static ObjectSpace objectSpace;

	public static void startClient() {
		if (!ServiceShareConfig.isSharingEnabled) {
			return;
		}
		try {
			client = new Client();
			client.start();
			client.connect(50000, ServiceShareConfig.serverIp, ServiceShareConfig.serverPort);
			/*Thread t = new Thread() {
				public void run() {
					while(true) {
						client.updateReturnTripTime();
						Slog.e("ServiceClient", "we get rtt is: " + client.getReturnTripTime());
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			};
			t.start();*/
			kryo = client.getKryo();
			ObjectSpace.registerClasses(kryo);
			objectSpace = new ObjectSpace(client);
			objectSpace.setExecutor(Executors.newFixedThreadPool(100, new ThreadFactory() {
				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r) {
						public void run() {
							//this.setPriority(MAX_PRIORITY);
							BlockGuard.setThreadPolicy(BlockGuard.LAX_POLICY);
							//ServiceShareConfig.setShareThread(true);
							super.run();
						}
					};
				}
			}));
		} catch (Exception e) {
			Slog.e(TAG, "start service sharing failed", e);
		}
	}

	/**
	 * Get a remote service proxy
	 * 
	 * @param objId
	 * @param objclz
	 * @return
	 */
	public static <T> T getRemoteProxy(Integer objId, Class<T> objclz) {
		return ObjectSpace.getRemoteObject(client, objId, objclz);
	}

	public static Kryo getKryo() {
		return kryo;
	}

	public static ObjectSpace getObjectSpace() {
		return objectSpace;
	}
}