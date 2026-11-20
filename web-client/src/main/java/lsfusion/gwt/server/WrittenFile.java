package lsfusion.gwt.server;

import com.google.common.base.Throwables;
import lsfusion.base.lambda.EConsumer;

import java.io.*;

// a file the web server wrote
class WrittenFile {
    public final File file;
    private volatile EConsumer<OutputStream, IOException> writer; // not written yet

    public WrittenFile(File file, EConsumer<OutputStream, IOException> writer) {
        this.file = file;
        this.writer = writer;
    }

    // writes the file, if it is not written yet
    public synchronized void write() {
        if(writer != null) {
            try(OutputStream outStream = org.apache.commons.io.FileUtils.openOutputStream(file)) {
                writer.accept(outStream);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
            writer = null;
        }
    }

    public boolean exists() { // does not wait for a write in progress: it is asked inside the registry's compute
        return writer != null || file.exists();
    }

    // false if the file is gone
    public boolean read(EConsumer<InputStream, IOException> consumer) {
        write();
        try(FileInputStream inStream = new FileInputStream(file)) {
            consumer.accept(inStream);
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
