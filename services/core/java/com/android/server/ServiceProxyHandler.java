package com.android.server;

import static com.esotericsoftware.minlog.Log.error;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;

import android.app.PendingIntent;
import android.location.LocationRequest;
import android.os.Binder;
import android.util.Slog;

import com.google.dexmaker.stock.ProxyBuilder;

import dalvik.system.BlockGuard;

/**
 * A proxy handler for shared services
 * 
 * @author yli118
 * 
 */
public class ServiceProxyHandler implements InvocationHandler {
	private static final String TAG = "ServiceProxyHandler";

	private Class<?> serviceInterface;
	
	/**
	 * The remote object to call if this is a client
	 */
	private Object remoteObj;

	private Integer hashCode;

	private String toString;
	
	public ServiceProxyHandler(Class<?> serviceInterface) {
		this.serviceInterface = serviceInterface;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		//Slog.i(TAG, "we got method invocation: " + method.getClass() + "." + method.getName());
		/*
		 * if this mobile is serving as a server or has not enabled service sharing
		 */
		if (!ServiceShareConfig.isSharingEnabled || ServiceShareConfig.isServer || remoteObj == null) {
			return ProxyBuilder.callSuper(proxy, method, args);
		}
		if (method.getName().equals("requestLocationUpdates")) {
		    if (((LocationRequest) args[0]).getProvider().equals("passive")) {
		        return ProxyBuilder.callSuper(proxy, method, args);
		    }
		}
		/*if (!method.getName().equals("requestLocationUpdates") && !method.getName().equals("removeUpdates") && !method.getName().equals("addGpsStatusListener") && !method.getName().equals("removeGpsStatusListener")) {
			return ProxyBuilder.callSuper(proxy, method, args);
		}*/
		/* this mobile is served as a sharing client */
		// if it is binder method
		/*Method[] binderMethods = Binder.class.getDeclaredMethods();
		for(Method binderMethod : binderMethods) {
			if (method.getName().equals(binderMethod.getName())) {
				return ProxyBuilder.callSuper(proxy, method, args);
			}
		}*/
		try {
			Binder.class.getDeclaredMethod(method.getName(), method.getParameterTypes());
			return ProxyBuilder.callSuper(proxy, method, args);
		} catch (NoSuchMethodException nsme) {
			// do nothing, this is not a binder method
		}
		long startTime = System.currentTimeMillis();
		Object result = null;
		/*while(remoteObj == null) {
			Slog.e(TAG, "we got remote obj as null and wait");
			Slog.e(TAG, Arrays.toString(Thread.currentThread().getStackTrace()));
			Thread.sleep(1000);
		}*/
		
		BlockGuard.setThreadPolicy(BlockGuard.LAX_POLICY);
		// service interface methods 
		Method interfaceMethod = serviceInterface.getDeclaredMethod(method.getName(), method.getParameterTypes());
		Slog.i(TAG, "we got a remote method call with name: " + interfaceMethod.getName());
		if (interfaceMethod.getName().equals("hashCode")) {
			if (hashCode != null) {
				result = hashCode;
			} else {
				hashCode = (Integer) interfaceMethod.invoke(remoteObj, args);
				result = hashCode;
			}
		} else if (interfaceMethod.getName().equals("toString")) {
			if (toString != null) {
				result = toString;
			} else {
				toString = (String) interfaceMethod.invoke(remoteObj, args);
				result = toString;
			}
		} else if (interfaceMethod.getName().equals("systemRunning")) {
			// do nothing for a client
			result = null;
		} else {
			/*if(interfaceMethod.getName().equals("requestLocationUpdates")) {
				int index = 0;
				for(Object object : args) {
					if(object == null) continue;
					Slog.e(TAG, "arg " + index++ + object);
					if(object.getClass().getName().contains("PendingIntent")) {
						PendingIntent pint = (PendingIntent) object;
						Slog.e(TAG, "pending intent inner target is: " + pint.getTarget());
					}
				}
			}*/
			result = interfaceMethod.invoke(remoteObj, args);
			// return method.invoke(remoteObj, args);
		}
		long endTime = System.currentTimeMillis();
		//Slog.e(TAG, "For evaluation, the execution time for method: " + interfaceMethod.getName() + ", time: " + (endTime - startTime));
		
		return result;
	}

	public void setRemoteObj(Object remoteObj) {
		this.remoteObj = remoteObj;
	}

}
