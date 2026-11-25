package lsfusion.server.logics.form.interactive.changed;

import lsfusion.base.file.AppImage;
import lsfusion.base.file.IOUtils;
import lsfusion.base.file.StringWithFiles;
import lsfusion.interop.base.view.ColorTheme;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.language.ScriptedStringUtils;
import org.junit.Test;

import java.io.*;
import java.util.Base64;

import static lsfusion.base.BaseUtils.inlineFileSeparator;
import static lsfusion.base.BaseUtils.inlineImageSeparator;
import static org.junit.Assert.*;

// a string value sent to a client : a resource or an image between two separators is read only when the platform itself put it there,
// and nothing there is read as a serialized object
public class FormChangesTest {

    // the marker of the serialized image kind, which strings no longer have
    private static final String SERIALIZED_IMAGE_MARKER = "<DFSRKNFDVSDRRES/>";

    // a test resource that no code names : the test that wraps it uses its absolute name, so the relative one stays unwrapped whatever order the tests run in
    private static final String RESOURCE = "lsfusion/server/logics/form/interactive/changed/FormChangesTest.txt";

    @Test
    public void aResourceNameThatCameWithDataIsNotFound() {
        assertNotFound("a" + inlineFileSeparator + RESOURCE + inlineFileSeparator + "b");
    }

    @Test
    public void anImageNameThatCameWithDataIsNotFound() {
        assertNotFound("a" + inlineFileSeparator + inlineImageSeparator + "images/add.png;x" + inlineFileSeparator + "b");
    }

    @Test
    public void aResourceThePlatformPutThereIsSentAsTheResource() throws IOException {
        StringWithFiles sent = (StringWithFiles) FormChanges.convertFileValue("a" + ScriptedStringUtils.wrapResource("/" + RESOURCE) + "b", null);
        assertArrayEquals(new String[] {"a", "b"}, sent.prefixes);
        assertEquals(1, sent.files.length);
        StringWithFiles.Resource resource = (StringWithFiles.Resource) sent.files[0];
        assertEquals(RESOURCE, resource.name);
        try (InputStream stream = FormChangesTest.class.getResourceAsStream("/" + RESOURCE)) {
            assertArrayEquals(IOUtils.readBytesFromStream(stream), resource.raw.getBytes());
        }
    }

    @Test
    public void anImageThePlatformPutThereIsSentAsTheImage() {
        StringWithFiles sent = (StringWithFiles) FormChanges.convertFileValue(ScriptedStringUtils.wrapImage("images/edit.png;x"), null);
        assertNotNull(((AppImage) sent.files[0]).getImage(ColorTheme.DEFAULT));
    }

    @Test
    public void anIconIsWrappedOnlyAsAWhole() throws IOException {
        String icon = "a" + inlineFileSeparator + inlineFileSeparator + RESOURCE + inlineFileSeparator + ".png";
        AppServerImage.convertFileValue(AppServerImage.createActionImage(icon + ";x").get(null));
        assertTrue(ScriptedStringUtils.isWrapped(icon));
        assertNotFound(inlineFileSeparator + RESOURCE + inlineFileSeparator);
    }

    @Test
    public void anImageNameThatCameWithDataReadsOnlyAnImageFile() throws IOException {
        FormChanges.NeedImage image = new FormChanges.NeedImage(null, AppServerImage::createActionImage);
        assertNotNull(((AppImage) FormChanges.convertFileValue(image, "images/add.png;x", null)).getImage(ColorTheme.DEFAULT));
        try {
            FormChanges.convertFileValue(image, RESOURCE + ";x", null);
            fail("a file that is not an image is read as an image");
        } catch (FileNotFoundException e) { // not read
        }
    }

    @Test
    public void anIconThatIsNotAnImageFileIsLeftOut() throws IOException {
        String icon = "an icon of no image.txt";
        assertNull(AppServerImage.convertFileValue(AppServerImage.createActionImage(icon + ";x").get(null)));
        assertFalse(ScriptedStringUtils.isWrapped(icon));
    }

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

    private static void assertNotFound(String value) {
        try {
            FormChanges.convertFileValue(value, null);
            fail("a name that came with data is read");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof FileNotFoundException);
        }
    }
}
