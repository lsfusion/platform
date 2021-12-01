package lsfusion.base.col.implementations.stored;

import java.io.*;

public class StoredArray<T> {
    private final StoredArraySerializer serializer;
    private final StoredArrayFileManager fileManager;
    private int size = 0;
    private RandomAccessFile indexFile;
    private RandomAccessFile dataFile;
    
    public StoredArray(StoredArraySerializer serializer) {
        this(0, serializer);
    }

    public StoredArray(int size, StoredArraySerializer serializer) {
        this(size, serializer, null);
    }
    
    public StoredArray(T[] array, StoredArraySerializer serializer) {
        this(array, serializer, null);
    }
    
    public StoredArray(T[] array, StoredArraySerializer serializer, StoredArrayFileManager fileManager) {
        try {
            this.serializer = serializer;
            this.fileManager = (fileManager == null ? new StoredArrayFileManagerImpl() : fileManager);
            createInitialState(array);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public StoredArray(T[] array, int size, StoredArraySerializer serializer, StoredArrayFileManager fileManager) {
        try {
            this.serializer = serializer;
            this.fileManager = (fileManager == null ? new StoredArrayFileManagerImpl() : fileManager);
            createInitialState(array, size);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    public StoredArray(int initialSize, StoredArraySerializer serializer, StoredArrayFileManager fileManager) {
        try {
            this.serializer = serializer;
            this.fileManager = (fileManager == null ? new StoredArrayFileManagerImpl() : fileManager);
            createInitialState(initialSize);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    public StoredArray(StoredArray<? extends T> source) {
        // todo [dale]: can be optimized
        try {
            this.serializer = source.serializer;
            this.fileManager = new StoredArrayFileManagerImpl();
            createInitialState(0);
            for (int i = 0; i < source.size(); ++i) {
                append(source.get(i));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    public void append(T element) {
        try {
            appendElement(element);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    public void set(int index, T element) {
        assert index >= 0 && index < size;
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            serializer.serialize(element, outStream);
            int newLen = outStream.size();

            seekToIndex(index);
            int offset = indexFile.readInt();
            int len = indexFile.readInt();

            int newOffset = (newLen <= len ? offset : (int) dataFile.length());
            setIndexData(index, newOffset, newLen);
            seekToObject(newOffset);
            writeElementData(outStream.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // time-consuming operation
    public void insert(int index, T element) {
        assert index >= 0 && index <= size;
        
        if (index == size) {
            append(element);
            return;
        }
        
        try {
            int offset = (int) dataFile.length();
            seekToObject(offset);
            int len = writeElement(element);

            seekToIndex(index);
            int nextOffset = 0, nextLen = 0;
            for (int i = index; i <= size; ++i) {
                if (i < size) {
                    nextOffset = indexFile.readInt();
                    nextLen = indexFile.readInt();
                }
                setIndexData(i, offset, len);
                offset = nextOffset;
                len = nextLen;
            }
            ++size;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    public T get(int index) {
        assert index < size;
        try {
            int len = prepareForElementReading(index);
            return readElement(len);
        } catch (IOException e) {
            throw new UncheckedIOException(e); 
        }
    }
    
    public int size() {
        return size;
    }
    
    private void createInitialState(T[] array) throws IOException {
        createInitialState(array, array.length);
    }

    private void createInitialState(T[] array, int size) throws IOException {
        openFiles();
        for (int i = 0; i < size; ++i) {
            appendElement(array[i]);
        }
    }
    
    private void createInitialState(int size) throws IOException {
        openFiles();
        for (int i = 0; i < size; ++i) {
            appendElement(null);
        }
    }
    
    private void appendElement(T element) throws IOException {
        int offset = (int) dataFile.length();
        seekToObject(offset);
        int len = writeElement(element);
        setIndexData(size, offset, len);
        ++size;
    }
    
    private void setIndexData(int index, int offset, int len) throws IOException {
        seekToIndex(index);
        indexFile.writeInt(offset);
        indexFile.writeInt(len);
    }
    
    private T readElement(int len) throws IOException {
        byte[] elementBuf = new byte[len];
        dataFile.read(elementBuf);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(elementBuf);
        return (T) serializer.deserialize(inputStream);
    }
    
    private int writeElement(T element) throws IOException {
        ByteArrayOutputStream oStream = new ByteArrayOutputStream();
        serializer.serialize(element, oStream);
        int len = oStream.size(); 
        writeElementData(oStream.toByteArray());
        return len;
    }
    
    private void writeElementData(byte[] elementData) throws IOException {
        dataFile.write(elementData);
    }
    
    private int prepareForElementReading(int index) throws IOException {
        seekToIndex(index);
        int offset = indexFile.readInt();
        int len = indexFile.readInt();
        if (len > 0) {
            seekToObject(offset);
        }
        return len;
    }

    public void squeeze() {
        File tmpIndexFile = fileManager.tmpIndexFile();
        File tmpDataFile = fileManager.tmpDataFile();
        try {
            try (
                DataOutputStream tmpIndexStream = new DataOutputStream(new FileOutputStream(tmpIndexFile));
                FileOutputStream tmpDataStream = new FileOutputStream(tmpDataFile)
            ) {
                int total = 0;
                for (int i = 0; i < size; ++i) {
                    int len = prepareForElementReading(i);
                    byte[] buf = new byte[len];
                    dataFile.read(buf);
                    tmpIndexStream.writeInt(total);
                    tmpIndexStream.writeInt(len);
                    tmpDataStream.write(buf);
                    total += len;
                }
            }

            closeFiles();
            fileManager.replaceFilesByTmpFiles();
            openFiles();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public StoredArraySerializer getSerializer() {
        return serializer;
    }

    @Override
    protected void finalize() throws Throwable {
        closeFiles();
        
        // todo [dale]: FileUtils.safeDelete
        fileManager.deleteFiles();
    }

    private void openFiles() throws FileNotFoundException {
        dataFile = fileManager.openDataFile();
        indexFile = fileManager.openIndexFile();
    }
    
    private void closeFiles() throws IOException {
        dataFile.close();
        indexFile.close();
    }
    
    private void seekToIndex(int index) throws IOException {
        indexFile.seek((long) index * Integer.BYTES * 2);
    }
    
    private void seekToObject(int offset) throws IOException {
        dataFile.seek(offset);
    }
}