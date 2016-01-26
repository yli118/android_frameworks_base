package com.android.server;

/**
 * A virtual (phantom) interface so that the shared service can have its proxy to be able to invoke system running method
 * 
 * @author yli118
 *
 */
public interface IVirtualSystemRunning {
	public void systemRunning();
}
