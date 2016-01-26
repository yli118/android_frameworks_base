package com.android.server;

import android.content.Context;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * A serializer which can be used to read / write context
 * 
 * @author yli118
 *
 */
public class ContextSerializer extends Serializer<Context> {
	
	private Context context;
	
	public ContextSerializer(Context context) {
		this.context = context;
	}

	@Override
	public Context read(Kryo kryo, Input input, Class<Context> clazz) {
		return context;
	}

	@Override
	public void write(Kryo kryo, Output output, Context object) {
		// write nothing
	}

}