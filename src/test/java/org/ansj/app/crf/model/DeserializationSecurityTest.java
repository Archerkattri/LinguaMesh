package org.ansj.app.crf.model;

import junit.framework.Assert;
import org.ansj.domain.PersonNatureAttr;
import org.ansj.library.DATDictionary;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.zip.GZIPOutputStream;

public class DeserializationSecurityTest {

	@Test
	public void personAttributeVIsStoredSeparatelyFromU() {
		PersonNatureAttr attribute = new PersonNatureAttr();
		attribute.set('V', 3.5f);
		Assert.assertEquals(3.5f, attribute.getV(), 0.0f);
		Assert.assertEquals(0.0f, attribute.getU(), 0.0f);
	}

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
	public void rejectsOversizedCrfModelArrays() throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream output = new ObjectOutputStream(new GZIPOutputStream(bytes))) {
			output.writeUTF(CRFModel.version);
			output.writeObject(new float[1_000_001][]);
		}

		try {
			new CRFModel().loadModel(new ByteArrayInputStream(bytes.toByteArray()));
			Assert.fail("oversized model array should be rejected");
		} catch (InvalidClassException expected) {
			Assert.assertNotNull(expected.getMessage());
		}
	}

	@Test
	public void rejectsLegacySerializedDictionariesBeforeDeserialization() throws Exception {
		Payload.deserialized = false;
		File dictionaryFile = Files.createTempFile("ansj-dictionary", ".ser").toFile();
		try {
			try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(dictionaryFile))) {
				output.writeObject(new Payload());
			}

			try {
				DATDictionary.loadFromFile(dictionaryFile.getAbsolutePath());
				Assert.fail("unexpected dictionary class should be rejected");
			} catch (IOException expected) {
				Assert.assertFalse(Payload.deserialized);
			}
		} finally {
			Files.deleteIfExists(dictionaryFile.toPath());
		}
	}

	@Test
	public void legacyCacheWriterFailsExplicitly() throws Exception {
		File dictionaryFile = Files.createTempFile("ansj-dictionary", ".ser").toFile();
		try {
			try {
				DATDictionary.write2File(dictionaryFile.getAbsolutePath());
				Assert.fail("legacy cache writer should be disabled");
			} catch (IOException expected) {
				Assert.assertTrue(expected.getMessage().contains("unsupported"));
			}
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
