package org.ansj.app.crf.model;

import junit.framework.Assert;
import org.ansj.library.DATDictionary;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.zip.GZIPOutputStream;

public class DeserializationSecurityTest {

	@Test
	public void rejectsUnexpectedCrfModelClassesBeforeDeserialization() throws Exception {
		Payload.deserialized = false;
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream output = new ObjectOutputStream(new GZIPOutputStream(bytes))) {
			output.writeUTF(CRFModel.version);
			output.writeObject(new Payload());
		}

		try {
			new CRFModel().loadModel(new ByteArrayInputStream(bytes.toByteArray()));
			Assert.fail("unexpected model class should be rejected");
		} catch (InvalidClassException expected) {
			Assert.assertFalse(Payload.deserialized);
		}
	}

	@Test
	public void rejectsUnexpectedDictionaryClassesBeforeDeserialization() throws Exception {
		Payload.deserialized = false;
		File dictionaryFile = Files.createTempFile("ansj-dictionary", ".ser").toFile();
		try {
			try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(dictionaryFile))) {
				output.writeObject(new Payload());
			}

			try {
				DATDictionary.loadFromFile(dictionaryFile.getAbsolutePath());
				Assert.fail("unexpected dictionary class should be rejected");
			} catch (InvalidClassException expected) {
				Assert.assertFalse(Payload.deserialized);
			}
		} finally {
			Files.deleteIfExists(dictionaryFile.toPath());
		}
	}

	@Test
	public void preservesDictionarySerializationFormat() throws Exception {
		File dictionaryFile = Files.createTempFile("ansj-dictionary", ".ser").toFile();
		try {
			DATDictionary.write2File(dictionaryFile.getAbsolutePath());
			Assert.assertEquals(DATDictionary.arrayLength,
					DATDictionary.loadFromFile(dictionaryFile.getAbsolutePath()).arrayLength);
		} finally {
			Files.deleteIfExists(dictionaryFile.toPath());
		}
	}

	private static final class Payload implements Serializable {
		private static final long serialVersionUID = 1L;
		private static boolean deserialized;

		private void readObject(ObjectInputStream input) throws Exception {
			deserialized = true;
			input.defaultReadObject();
		}
	}
}
