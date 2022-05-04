package lsfusion.base.col.implementations.stored;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class StoredArrayFileManagerImpl implements StoredArrayFileManager {
    private static final String tmpFolder = System.getProperty("java.io.tmpdir") + "/.lsfusion";
    private final String id;
    
    public StoredArrayFileManagerImpl() throws IOException {
        id = generateUniqueName();
        init();
    }
    
    public StoredArrayFileManagerImpl(String id) throws IOException {
        this.id = id;
        init();
    }
    
    @Override
    public RandomAccessFile openIndexFile() throws FileNotFoundException {
        return new RandomAccessFile(getIndexFilePath(), "rws");
    }

    @Override
    public RandomAccessFile openDataFile() throws FileNotFoundException {
        return new RandomAccessFile(getDataFilePath(), "rws");
    }

    @Override
    public File tmpIndexFile() {
        File indexFile = new File(getTmpIndexFilePath());
        indexFile.deleteOnExit();
        return indexFile;
    }
    
    @Override
    public File tmpDataFile() {
        File dataFile = new File(getTmpDataFilePath());
        dataFile.deleteOnExit();
        return dataFile;
    }
    
    @Override
    public void replaceFilesByTmpFiles() throws IOException {
        replaceIndexFileByTmpFile();
        Files.move(Paths.get(getTmpDataFilePath()), Paths.get(getDataFilePath()), REPLACE_EXISTING);
    }
    
    @Override
    public void replaceIndexFileByTmpFile() throws IOException {
        Files.move(Paths.get(getTmpIndexFilePath()), Paths.get(getIndexFilePath()), REPLACE_EXISTING);
    }
    
    @Override
    public void deleteFiles() {
        new File(getDataFilePath()).delete();
        new File(getIndexFilePath()).delete();
    }

    private void init() throws IOException {
        Files.createDirectories(Paths.get(tmpFolder + "/" + id.substring(0, 2)));
    }
    
    protected String generateUniqueName() {
        return UUID.randomUUID().toString();
    }

    protected String getDataFilePath() {
        return getTemporaryFileName(id + ".data");
    }

    protected String getIndexFilePath() {
        return getTemporaryFileName(id + ".index");
    }

    protected String getTmpDataFilePath() {
        return getTemporaryFileName(id + "_tmp.data");
    }

    protected String getTmpIndexFilePath() {
        return getTemporaryFileName(id + "_tmp.index");
    }

    protected String getTemporaryFileName(String name) {
        return tmpFolder + "/" + name.substring(0, 2) + "/" + name.substring(2);
    }
}
