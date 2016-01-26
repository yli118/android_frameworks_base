package com.android.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import android.util.Slog;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.kryonet.rmi.ObjectSpace;

import dalvik.system.BlockGuard;

/**
 * A service share server
 * 
 * @author yli118
 * 
 */
public class ServiceServer {
	private static final String TAG = "ServiceServer";

	/**
	 * Service sharing server endpoint
	 */
	private static Server server = null;

	/**
	 * Kryo object for serialization
	 */
	private static Kryo kryo = null;

	/**
	 * The object space for the server connections to the client
	 */
	private static ObjectSpace objectSpace = null;

	/**
	 * Start server service sharing
	 * 
	 * @throws Exception
	 */
	public static void startServer() {
		if (!ServiceShareConfig.isSharingEnabled) {
			return;
		}
		try {
			server = new Server();
			server.start();
			server.bind(ServiceShareConfig.serverPort);
			server.addListener(new Listener() {
				@Override
				public void connected(Connection connection) {
					objectSpace.addConnection(connection);
					/*connection.addListener(new Listener() {
						public void received (Connection connection, Object object) {
							if(object instanceof InvokeMethod) {
								InvokeMethod method = (InvokeMethod) object;
								Slog.i(TAG, "get invocation called with method: " + method.objectID + " - " + method.cachedMethod);
							}
						}
					});*/
				}
			});
			kryo = server.getKryo();
			ObjectSpace.registerClasses(kryo);
			objectSpace = new ObjectSpace();
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
	 * Register a sharing service for remote to call
	 * 
	 * @param objId
	 * @param serviceobj
	 */
	public static void registerService(Integer objId, Object serviceobj) {
		objectSpace.register(objId, serviceobj);
	}

	public static Kryo getKryo() {
		return kryo;
	}

	public static ObjectSpace getObjectSpace() {
		return objectSpace;
	}
}