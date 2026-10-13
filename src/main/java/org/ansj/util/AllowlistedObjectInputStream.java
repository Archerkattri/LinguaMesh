package org.ansj.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.HashSet;
import java.util.Set;

public final class AllowlistedObjectInputStream extends ObjectInputStream {

	private final Set<String> allowedClasses;

	public AllowlistedObjectInputStream(InputStream input, Set<String> allowedClasses) throws IOException {
		super(input);
		this.allowedClasses = new HashSet<>(allowedClasses);
	}

	@Override
	protected Class<?> resolveClass(ObjectStreamClass descriptor) throws IOException, ClassNotFoundException {
		String className = descriptor.getName();
		if (!allowedClasses.contains(className)) {
			throw new InvalidClassException("Serialized class is not allowed", className);
		}
		return super.resolveClass(descriptor);
	}

	@Override
	protected Class<?> resolveProxyClass(String[] interfaces) throws IOException {
		throw new InvalidClassException("Serialized proxy classes are not allowed");
	}
}
