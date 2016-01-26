package com.android.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Properties;

import android.app.ContextImpl;
import android.app.PendingIntent;
import android.app.PendingIntent.FinishedDispatcher;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.graphics.Rect;
import android.location.IGpsStatusListener;
import android.location.ILocationListener;
import android.location.ILocationManager;
import android.location.Location;
import android.location.LocationManager.ListenerTransport;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Parcel;
import android.os.UserHandle;
import android.os.WorkSource;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;

import com.android.server.am.PendingIntentRecord;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.FieldSerializer;

/**
 * @author yli118
 * 
 */
public class ServiceShareConfig {
	private static final String TAG = "ServiceShareConfig";

	private static String CONFIG_FILE_LOCATION = "/data/data/system_server/service.config.properties";
	
	//private static ThreadLocal<Boolean> isShareThread = new ThreadLocal<Boolean>();

	/**
	 * Flag indicates if this mobile is a service server
	 */
	public static boolean isServer = true;

	/**
	 * The tcp port server listens
	 */
	public static int serverPort = 0;

	/**
	 * The ip address of service server
	 */
	public static String serverIp = "";
	
	/**
	 * If there is any service sharing enabled
	 */
	public static boolean isSharingEnabled = false;

	/**
	 * Flag indicates if location service sharing is enabled
	 */
	public static boolean isLocationSharingEnabled = false;
	
	/**
	 * Flag indicates if notification service sharing is enabled
	 */
	public static boolean isNotificationSharingEnabled = false;

	/**
	 * The object id for location service
	 */
	public static final int LOCATION_SERVICE = 1;
	
	/**
	 * The object id for notification service
	 */
	public static final int NOTIFICATION_SERVICE = 2;
	
	private static int nextObjId = 10000;
	
	private static ServiceProxyHandler locationHandler = null;
	
	private static ServiceProxyHandler notificationHandler = null;
	
	private static Context context = null;
	
	// start - for test
	/*public static FileWriter fw = null;
	
	public static int nmeacnt = 0;
	
	public static int nmearepcnt = 0;
	
	public static int loccnt = 0;
	
	public static String gpsInitiator = null;
	
	public static Map<Object, Long> startTime = new HashMap<Object, Long>();*/
	
	// end - for test

	public static void loadConfig() {
		//Log.DEBUG();
		Properties prop = new Properties();
		InputStream input = null;

		try {
			//fw = new FileWriter(new File("/data/data/system_server/location.log"));
			
			input = new FileInputStream(CONFIG_FILE_LOCATION);

			// load a properties file
			prop.load(input);

			// get the property value and print it out
			isServer = Boolean.parseBoolean(prop.getProperty("isServer"));
			serverPort = Integer.parseInt(prop.getProperty("serverPort"));
			serverIp = prop.getProperty("serverIp");
			isLocationSharingEnabled = Boolean.parseBoolean(prop.getProperty("isLocationSharingEnabled"));
			isNotificationSharingEnabled = Boolean.parseBoolean(prop.getProperty("isNotificationSharingEnabled"));
			// do the or operation to the all sharing flag
			isSharingEnabled = isLocationSharingEnabled || isNotificationSharingEnabled;
			Slog.e(TAG, "is server: " + isServer + ", port: " + serverPort + ", ip: " + serverIp + ", isnotification enabled: " + isNotificationSharingEnabled + ", islocation enabled: " + isLocationSharingEnabled);
		} catch (IOException ex) {
			Slog.e(TAG, "loading config properties error", ex);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					Slog.e(TAG, "loading config properties error", e);
				}
			}
		}
	}
	
	public static void startServiceShare() {
		Kryo kryo = null;
		if(isServer) {
			ServiceServer.startServer();
			kryo = ServiceServer.getKryo();
		} else {
			ServiceClient.startClient();
			kryo = ServiceClient.getKryo();
		}
		if(isSharingEnabled) {
			registerLocationClasses(kryo);
		}
		Slog.i(TAG, "Service sharing started successfully");
	}
	
	private static void registerLocationClasses(Kryo kryo) {
		kryo.register(Bundle.class.getClassLoader().getClass());
		kryo.register(Class.class);
		kryo.register(String[].class);
		kryo.register(long[].class);
		kryo.register(int[].class);
		kryo.register(Parcel.class);
		kryo.register(Rect.class);
		kryo.register(ArrayMap.class);
		kryo.register(Intent.class);
		kryo.register(ComponentName.class);
		kryo.register(ArraySet.class);
		kryo.register(IIntentSender.class);
		kryo.register(ArrayList.class);
		kryo.register(float[].class);
		if(isLocationSharingEnabled) {
			kryo.register(LocationManagerService.class);
			kryo.register(ILocationManager.class);
			kryo.register(IGpsStatusListener.class);
			registerClazzMethods(kryo, ILocationManager.class);
		}
		/*if(isNotificationSharingEnabled) {
			kryo.register(NotificationManagerService.class);
			kryo.register(INotificationManager.class);
			registerClazzMethods(kryo, INotificationManager.class);
		kryo.register(RemoteViews.class);
		kryo.register(RemoteViews.ReflectionAction.class);
		kryo.register(RemoteViews.ActionException.class);
		kryo.register(RemoteViews.BitmapReflectionAction.class);
		kryo.register(RemoteViews.MemoryUsageCounter.class);
		kryo.register(RemoteViews.OnClickHandler.class);
		kryo.register(RemoteViews.SetDrawableParameters.class);
		kryo.register(RemoteViews.SetEmptyView.class);
		kryo.register(RemoteViews.SetOnClickFillInIntent.class);
		kryo.register(RemoteViews.ReflectionAction.class);
		kryo.register(RemoteViews.SetPendingIntentTemplate.class);
		kryo.register(RemoteViews.TextViewDrawableAction.class);
		kryo.register(RemoteViews.SetRemoteViewsAdapterIntent.class);
		kryo.register(RemoteViews.SetOnClickPendingIntent.class);
		kryo.register(RemoteViews.SetRemoteViewsAdapterList.class);
		kryo.register(RemoteViews.TextViewSizeAction.class);
		kryo.register(RemoteViews.ViewGroupAction.class);
		kryo.register(RemoteViews.ViewPaddingAction.class);
		kryo.register(RemoteViews.BitmapCache.class);
		kryo.register(RemoteViews.MutablePair.class);
		kryo.register(RemoteViews.ReflectionActionWithoutParams.class);
		kryo.register(Bitmap.class);
		kryo.register(WorkSource.class);
		}*/
		kryo.register(PendingIntent.class);
		kryo.register(UserHandle.class);
		kryo.register(DeadObjectException.class);
		registerClazzMethods(kryo, PendingIntentRecord.class);
		kryo.register(ContextImpl.class, new ContextSerializer(context));
		kryo.register(Intent.class, new IntentSerializer());
		int regId = 100000;
		if(!isServer) {
			kryo.register(ILocationListener.Stub.Proxy.class, new BinderProxySerializer(ILocationListener.class, ServiceClient.getObjectSpace()), regId++);
			kryo.register(IGpsStatusListener.Stub.Proxy.class, new BinderProxySerializer(IGpsStatusListener.class, ServiceClient.getObjectSpace()), regId++);
			kryo.register(IIntentSender.Stub.Proxy.class, new BinderProxySerializer(IIntentSender.class, ServiceClient.getObjectSpace()), regId++);
			kryo.register(FinishedDispatcher.class, new BinderSerializer(FinishedDispatcher.class, new Class[] {IIntentReceiver.class, Runnable.class}, ServiceClient.getObjectSpace()), 100002);
			kryo.register(FinishedDispatcher.class, new BinderProxySerializer(IIntentReceiver.class, ServiceClient.getObjectSpace()), regId++);
			//kryo.register(PendingIntentRecord.class, new BinderProxySerializer(IIntentSender.class, ServiceClient.getObjectSpace()), regId++);
			kryo.register(PendingIntentRecord.class, new BinderSerializer(PendingIntentRecord.class, new Class[] {IIntentSender.class}, ServiceClient.getObjectSpace()), regId++);
			kryo.register(ListenerTransport.class, new BinderProxySerializer(ILocationListener.class, ServiceClient.getObjectSpace()), regId++);
		} else {
			kryo.register(ILocationListener.Stub.Proxy.class, new BinderProxySerializer(ILocationListener.class, ServiceServer.getObjectSpace()), regId++);
			kryo.register(IGpsStatusListener.Stub.Proxy.class, new BinderProxySerializer(IGpsStatusListener.class, ServiceServer.getObjectSpace()), regId++);
			kryo.register(IIntentSender.Stub.Proxy.class, new BinderProxySerializer(IIntentSender.class, ServiceServer.getObjectSpace()), regId++);
			kryo.register(FinishedDispatcher.class, new BinderSerializer(FinishedDispatcher.class, new Class[] {IIntentReceiver.class, Runnable.class}, ServiceServer.getObjectSpace()), 100002);
			kryo.register(FinishedDispatcher.class, new BinderProxySerializer(IIntentReceiver.class, ServiceServer.getObjectSpace()), regId++);
			//kryo.register(PendingIntentRecord.class, new BinderProxySerializer(IIntentSender.class, ServiceServer.getObjectSpace()), regId++);
			kryo.register(PendingIntentRecord.class, new BinderSerializer(PendingIntentRecord.class, new Class[] {IIntentSender.class}, ServiceServer.getObjectSpace()), regId++);
			kryo.register(ListenerTransport.class, new BinderProxySerializer(ILocationListener.class, ServiceClient.getObjectSpace()), regId++);
		}
		FieldSerializer bundleSerializer = new FieldSerializer(kryo, Bundle.class);
		bundleSerializer.getField("mClassLoader").setSerializer(new BootClassloaderSerializer());
		kryo.register(Bundle.class, bundleSerializer);
		FieldSerializer locationSerializer = new FieldSerializer(kryo, Location.class);
		locationSerializer.getField("mExtras").setSerializer(new NullifySerializer());
		kryo.register(Location.class, locationSerializer);
	}

	private static void registerClazzMethods(Kryo kryo, Class clazz) {
		Method[] methods = clazz.getDeclaredMethods();
		for(Method method : methods) {
			Class<?>[] argTypes = method.getParameterTypes();
			for(Class<?> clz : argTypes) {
				kryo.register(clz);
			}
			kryo.register(method.getReturnType());
		}
	}
	
	public static ServiceProxyHandler getLocationHandler() {
		return locationHandler;
	}

	public static void setLocationHandler(ServiceProxyHandler mlocationHandler) {
		locationHandler = mlocationHandler;
	}
	
	public static ServiceProxyHandler getNotificationHandler() {
		return notificationHandler;
	}

	public static void setNotificationHandler(ServiceProxyHandler notificationHandler) {
		ServiceShareConfig.notificationHandler = notificationHandler;
	}

	/*public static void setShareThread(boolean mShareThread) {
		isShareThread.set(mShareThread);
	}
	
	public static boolean isShareThread() {
		if(isShareThread.get() == null) {
			return false;
		}
		return isShareThread.get();
	}*/
	
	public static int getAndIncrementObjId() {
		return nextObjId++;
	}
	
	public static void setContext(Context mcontext) {
		context = mcontext;
	}

	public static Context getContext() {
		return context;
	}

	public static void main(String[] args) {
		CONFIG_FILE_LOCATION = "/home/leon/service.config.properties";
		loadConfig();
		System.out.println(isServer + " -- " + serverPort + " -- " + serverIp
				+ " -- " + isLocationSharingEnabled);
	}
}
