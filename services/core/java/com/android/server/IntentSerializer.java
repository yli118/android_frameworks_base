package com.android.server;

import java.net.URISyntaxException;

import android.content.Intent;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * A serializer used to do intent serialization
 * 
 * @author yli118
 *
 */
public class IntentSerializer extends Serializer<Intent> {

	@Override
	public Intent read(Kryo kryo, Input input, Class<Intent> clazz) {
		String intentUri = input.readString();
		try {
			return Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	public void write(Kryo kryo, Output output, Intent object) {
		output.writeString(object.toUri(Intent.URI_INTENT_SCHEME));
	}
}