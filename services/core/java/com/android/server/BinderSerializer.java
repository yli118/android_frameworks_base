package com.android.server;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import android.os.Binder;
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
import com.google.dexmaker.stock.ProxyBuilder;

import dalvik.system.BlockGuard;

/**
 * A binder class which can be used to serialize the Binder classes in the
 * system
 * 
 * @author yli118
 * 
 */
public class BinderSerializer extends Serializer<Object> {

	private Class binderClazz;

	private Class[] ifaceClasses;

	private ObjectSpace objectSpace;

	// TODO: take care of object life cycle, with this method, it cannot be GC
	private Map<RemoteObjId, Object> remoteObjCache = new HashMap<RemoteObjId, Object>();

	public BinderSerializer(Class binderClazz, Class[] ifaceClasses, ObjectSpace objectSpace) {
		this.binderClazz = binderClazz;
		this.ifaceClasses = ifaceClasses;
		this.objectSpace = objectSpace;
	}

	@Override
	public Object read(Kryo kryo, Input input, Class<Object> clazz) {
		try {
			int objectId = input.readInt();
			RemoteObjId remoteObjId = new RemoteObjId(Connection.currentConnection(), objectId);
			if (remoteObjCache.containsKey(remoteObjId)) {
				return remoteObjCache.get(remoteObjId);
			} else {
				final Object remoteObj = ObjectSpace.getRemoteObject(Connection.currentConnection(), objectId, ifaceClasses);
				Object object = ProxyBuilder.forClass(binderClazz).dexCache(new File("/data/data/system_server/")).handler(new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if (method.getName().equals("asBinder")) {
							return ProxyBuilder.callSuper(proxy, method, args);
						} else if (method.getName().equals("toString")) {
							return this.toString();
						} else if (method.getName().equals("hashCode")) {
							return this.hashCode();
						} else if (method.getName().equals("equals")) {
							return proxy == args[0];
						}

						try {
							Binder.class.getDeclaredMethod(method.getName(), method.getParameterTypes());
							Slog.e("BinderSerializer", "we get a method invocation at binder: " + method);
							return ProxyBuilder.callSuper(proxy, method, args);
						} catch (NoSuchMethodException nsme) {
							// do nothing, this is not a binder method
						}

						for (Class ifaceClz : ifaceClasses) {
							try {
								Method ifaceMethod = ifaceClz.getDeclaredMethod(method.getName(), method.getParameterTypes());
								BlockGuard.setThreadPolicy(BlockGuard.LAX_POLICY);
								Slog.e("BinderSerializer", "we get a method invocation at iface: " + ifaceMethod);
								return ifaceMethod.invoke(remoteObj, args);
							} catch (NoSuchMethodException nsme) {
								// do nothing, this is not a method in this
								// class
							}
						}
						Slog.e("BinderSerializer", "we get a method invocation at null: " + method);
						return null;
					}
				}).build();
				remoteObjCache.put(remoteObjId, object);

				return object;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void write(Kryo kryo, Output output, Object object) {
		int objectId;
		if (objectSpace.isRegistered(object)) {
			objectId = objectSpace.getRegisterId(object);
		} else {
			objectId = ServiceShareConfig.getAndIncrementObjId();
			objectSpace.register(objectId, object);
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