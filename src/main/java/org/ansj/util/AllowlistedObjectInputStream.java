package org.ansj.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

public final class AllowlistedObjectInputStream extends ObjectInputStream {

	private static final long MAX_DEPTH = 50;
	private static final long MAX_REFERENCES = 5_000_000;
	private static final long MAX_STREAM_BYTES = 128L * 1024 * 1024;
	private static final long MAX_ARRAY_LENGTH = 1_000_000;

	private final Set<String> allowedClasses;

	public AllowlistedObjectInputStream(InputStream input, Set<String> allowedClasses) throws IOException {
		super(input);
		this.allowedClasses = new HashSet<>(allowedClasses);
		installFilter();
	}

	private void installFilter() throws IOException {
		try {
			Class<?> filterType;
			try {
				filterType = Class.forName("java.io.ObjectInputFilter");
			} catch (ClassNotFoundException java8) {
				filterType = Class.forName("sun.misc.ObjectInputFilter");
			}
			Class<?> infoType = Class.forName(filterType.getName() + "$FilterInfo");
			Class<?> statusType = Class.forName(filterType.getName() + "$Status");
			Object filter = Proxy.newProxyInstance(
					AllowlistedObjectInputStream.class.getClassLoader(),
					new Class<?>[] { filterType },
					new FilterHandler(infoType, statusType, allowedClasses));
			try {
				Method setter = ObjectInputStream.class.getMethod("setObjectInputFilter", filterType);
				setter.invoke(this, filter);
			} catch (NoSuchMethodException java8) {
				Class<?> configType = Class.forName(filterType.getName() + "$Config");
				Method setter = configType.getMethod("setObjectInputFilter", ObjectInputStream.class, filterType);
				setter.invoke(null, this, filter);
			}
		} catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			throw new IOException("This Java runtime does not support serialization filters", e);
		}
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

	private static final class FilterHandler implements InvocationHandler {
		private final Method depth;
		private final Method references;
		private final Method streamBytes;
		private final Method arrayLength;
		private final Method serialClass;
		private final Object allowed;
		private final Object undecided;
		private final Object rejected;
		private final Set<String> allowedClasses;

		private FilterHandler(Class<?> infoType, Class<?> statusType, Set<String> allowedClasses) throws NoSuchMethodException {
			depth = infoType.getMethod("depth");
			references = infoType.getMethod("references");
			streamBytes = infoType.getMethod("streamBytes");
			arrayLength = infoType.getMethod("arrayLength");
			serialClass = infoType.getMethod("serialClass");
			allowed = status(statusType, "ALLOWED");
			undecided = status(statusType, "UNDECIDED");
			rejected = status(statusType, "REJECTED");
			this.allowedClasses = allowedClasses;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] arguments) {
			if (!"checkInput".equals(method.getName()) || arguments == null || arguments.length != 1) {
				return rejected;
			}
			try {
				Object info = arguments[0];
				if ((Long) depth.invoke(info) > MAX_DEPTH
						|| (Long) references.invoke(info) > MAX_REFERENCES
						|| (Long) streamBytes.invoke(info) > MAX_STREAM_BYTES
						|| (Long) arrayLength.invoke(info) > MAX_ARRAY_LENGTH) {
					return rejected;
				}
				Class<?> type = (Class<?>) serialClass.invoke(info);
				if (type == null) {
					return undecided;
				}
				return allowedClasses.contains(type.getName()) ? allowed : rejected;
			} catch (IllegalAccessException | InvocationTargetException e) {
				return rejected;
			}
		}

		private static Object status(Class<?> statusType, String name) {
			for (Object status : statusType.getEnumConstants()) {
				if (name.equals(status.toString())) {
					return status;
				}
			}
			throw new IllegalArgumentException("Unsupported serialization filter status: " + name);
		}
	}
}
