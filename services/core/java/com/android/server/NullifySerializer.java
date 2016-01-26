package com.android.server;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Nullify the field
 * 
 * @author yli118
 *
 */
public class NullifySerializer extends Serializer<Object> {

	@Override
	public Object read(Kryo arg0, Input arg1, Class<Object> arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void write(Kryo arg0, Output arg1, Object arg2) {
		// TODO Auto-generated method stub
	}


}
