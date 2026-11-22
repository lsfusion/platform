package lsfusion.server.logics.form.interactive.changed;

import org.junit.Test;

import java.io.*;
import java.util.Base64;

import static lsfusion.base.BaseUtils.inlineFileSeparator;
import static org.junit.Assert.assertFalse;

// a string value sent to a client : what lies between two inline file separators is never read as a serialized object
public class FormChangesTest {

    // the marker of the serialized image kind, which strings no longer have
    private static final String SERIALIZED_IMAGE_MARKER = "<DFSRKNFDVSDRRES/>";

    @Test
    public void anObjectSerializedIntoAStringIsNotRead() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(bytes);
        dataStream.writeBoolean(true);
        ObjectOutputStream objectStream = new ObjectOutputStream(dataStream);
        objectStream.writeObject(new Probe());
        objectStream.flush();

        Probe.read = false;
        try {
            FormChanges.convertFileValue(inlineFileSeparator + SERIALIZED_IMAGE_MARKER + Base64.getEncoder().encodeToString(bytes.toByteArray()) + inlineFileSeparator, null);
        } catch (RuntimeException e) {
            // the text between the separators names no file
        }
        assertFalse(Probe.read);
    }

    public static class Probe implements Serializable {
        static boolean read;

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            read = true;
            in.defaultReadObject();
        }
    }
}
