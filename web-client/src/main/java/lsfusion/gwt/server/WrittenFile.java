package lsfusion.gwt.server;

import com.google.common.base.Throwables;
import lsfusion.base.lambda.EConsumer;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// a file the web server wrote, with the SHA-256 of what it wrote: read back only while it still has it
class WrittenFile {
    public final File file;
    private volatile EConsumer<OutputStream, IOException> writer; // not written yet: written when first read
    private byte[] digest;

    public WrittenFile(File file, EConsumer<OutputStream, IOException> writer) {
        this.file = file;
        this.writer = writer;
    }

    // writes the file, if it is not written yet
    public synchronized void write() {
        if(writer != null) {
            MessageDigest digest;
            try(OutputStream outStream = new DigestOutputStream(org.apache.commons.io.FileUtils.openOutputStream(file), digest = MessageDigest.getInstance("SHA-256"))) {
                writer.accept(outStream);
            } catch (IOException | NoSuchAlgorithmException | RuntimeException e) {
                FileUtils.deleteFile(file); // a half-written file is of no use (and a streamed one can be gigabytes)
                throw Throwables.propagate(e);
            }
            this.digest = digest.digest();
            writer = null;
        }
    }

    public boolean exists() { // does not wait for a write in progress: it is asked inside the registry's compute
        return writer != null || file.exists();
    }

    private static final long MAX_CHECKED_IN_MEMORY = 1024 * 1024; // a bigger file is checked by an extra read, not in memory

    // false if the file is gone, or changed since it was written
    public boolean read(EConsumer<InputStream, IOException> consumer) {
        write();
        try(FileInputStream inStream = new FileInputStream(file)) {
            long size = inStream.getChannel().size();
            byte[] readDigest;
            InputStream checkedStream;
            if(size <= MAX_CHECKED_IN_MEMORY) {
                byte[] bytes = IOUtils.toByteArray(inStream, size);
                readDigest = DigestUtils.sha256(bytes);
                checkedStream = new ByteArrayInputStream(bytes);
            } else {
                readDigest = DigestUtils.sha256(inStream);
                inStream.getChannel().position(0);
                checkedStream = inStream;
            }
            if(!MessageDigest.isEqual(readDigest, digest)) {
                GLoggers.invocationLogger.error("file changed after it was written: " + file.getPath());
                return false;
            }
            consumer.accept(checkedStream);
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
