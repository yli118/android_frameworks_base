package com.android.server;

import android.os.Bundle;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * used to serialize bootclassloader
 * 
 * @author yli118
 *
 */
public class BootClassloaderSerializer extends Serializer<ClassLoader> {

	@Override
	public ClassLoader read(Kryo kryo, Input input, Class<ClassLoader> clazz) {
		return Bundle.class.getClassLoader();
	}

	@Override
	public void write(Kryo arg0, Output arg1, ClassLoader arg2) {
		// do nothing
	}
	
}