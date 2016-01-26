package com.android.server;

import java.io.FileDescriptor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Slog;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.rmi.ObjectSpace;

/**
 * A serializer which can be used to serialize Binder Proxy
 * 
 * @author yli118
 * 
 */
public class BinderProxySerializer extends Serializer<Object> {
	private Class iclass;

	private ObjectSpace objectSpace;
	
	// TODO: take care of object life cycle, with this method, it cannot be GC
	private Map<RemoteObjId, Object> remoteObjCache = new HashMap<RemoteObjId, Object>();

	public BinderProxySerializer(Class icClass, ObjectSpace objectSpace) {
		this.iclass = icClass;
		this.objectSpace = objectSpace;
	}

	@Override
	public Object read(Kryo kryo, Input input, Class<Object> clazz) {
		int objectId = input.readInt();
		RemoteObjId remoteObjId = new RemoteObjId(Connection.currentConnection(), objectId);
		Object resultObj = null;
		if(remoteObjCache.containsKey(remoteObjId)) {
			resultObj = remoteObjCache.get(remoteObjId);
		} else {
			final Object remoteObj = ObjectSpace.getRemoteObject(Connection.currentConnection(), objectId, iclass);
			// Slog.e("BinderProxySerializer", "to proxy the interface: " +
			// iclass + " -- " + remoteObj);
			Object objProxy = Proxy.newProxyInstance(BinderProxySerializer.class.getClassLoader(), new Class[] { iclass }, new InvocationHandler() {

				private IBinder mObject = new IBinderObject();

				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					try {
						if (method.getName().equals("asBinder")) {
							return mObject;
						} else if (method.getName().equals("toString")) {
							return this.toString();
						} else if (method.getName().equals("hashCode")) {
							return this.hashCode();
						}
						// Slog.e("BinderProxySerializer",
						// "we receive a call in binder proxy, name:" +
						// method.getName() + " arg: " +
						// Arrays.toString(method.getParameterTypes()) + " -- "
						// + iclass);
						Method ifaceMethod = iclass.getDeclaredMethod(method.getName(), method.getParameterTypes());
						return ifaceMethod.invoke(remoteObj, args);
					} catch (Exception e) {
						Slog.e("BinderProxySerializer", "invoke error", e);
					}
					return null;
				}
			});
			remoteObjCache.put(remoteObjId, objProxy);
			resultObj = objProxy;
		}
				
		Slog.e("BinderProxySerializer", "we get remote object as " + resultObj + ", id: " + objectId);
		return resultObj;
	}
	
	Map<Object, Integer> registeredBinder = new HashMap<Object, Integer>();

	@Override
	public void write(Kryo kryo, Output output, Object object) {
		int objectId;
		IBinder binder = null;
		if (object instanceof android.location.ILocationListener.Stub.Proxy) {
			android.location.ILocationListener.Stub.Proxy proxy = (android.location.ILocationListener.Stub.Proxy) object;
			binder = proxy.asBinder();
		} else if (object instanceof android.content.IIntentSender.Stub.Proxy) {
			android.content.IIntentSender.Stub.Proxy proxy = (android.content.IIntentSender.Stub.Proxy) object;
			binder = proxy.asBinder();
		} else if (object instanceof android.location.IGpsStatusListener.Stub.Proxy) {
			android.location.IGpsStatusListener.Stub.Proxy proxy = (android.location.IGpsStatusListener.Stub.Proxy) object;
			binder = proxy.asBinder();
		}
		if (registeredBinder.containsKey(binder)) {
			objectId = registeredBinder.get(binder);
		} else {
			objectId = ServiceShareConfig.getAndIncrementObjId();
			objectSpace.register(objectId, object);
			registeredBinder.put(binder, objectId);
		}
		output.writeInt(objectId);
	}
	
	static class RemoteObjId {
		Connection connection;
		
		int objectId;
		
		public RemoteObjId(Connection connection, int objectId) {
			this.connection = connection;
			this.objectId = objectId;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((connection == null) ? 0 : connection.hashCode());
			result = prime * result + objectId;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RemoteObjId other = (RemoteObjId) obj;
			if (connection == null) {
				if (other.connection != null)
					return false;
			} else if (!connection.equals(other.connection))
				return false;
			if (objectId != other.objectId)
				return false;
			return true;
		}
		
	}
	
	static class IBinderObject implements IBinder {

		@Override
		public String getInterfaceDescriptor() throws RemoteException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean pingBinder() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isBinderAlive() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public IInterface queryLocalInterface(String descriptor) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void dump(FileDescriptor fd, String[] args) throws RemoteException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void dumpAsync(FileDescriptor fd, String[] args) throws RemoteException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void linkToDeath(DeathRecipient recipient, int flags) throws RemoteException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean unlinkToDeath(DeathRecipient recipient, int flags) {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
}