package lsfusion.base.col.implementations.stored;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public interface StoredArrayFileManager {
    RandomAccessFile openIndexFile() throws FileNotFoundException;
    RandomAccessFile openDataFile() throws FileNotFoundException;

    File tmpIndexFile();
    File tmpDataFile();
    
    void replaceFilesByTmpFiles() throws IOException;
    void replaceIndexFileByTmpFile() throws IOException;

    void deleteFiles();
}
