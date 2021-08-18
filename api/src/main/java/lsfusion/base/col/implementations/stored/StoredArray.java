package lsfusion.base.col.implementations.stored;

import java.io.*;

public class StoredArray<T> {
    private int size = 0;
    private final StoredArraySerializer serializer;
    private final StoredArrayFileManager fileManager;
    private RandomAccessFile indexFile;
    private RandomAccessFile dataFile;
    
    public StoredArray(StoredArraySerializer serializer) throws IOException {
        this(0, serializer);
    }

    public StoredArray(int size, StoredArraySerializer serializer) throws IOException {
        this(size, serializer, new StoredArrayFileManagerImpl());
    }
    
    public StoredArray(T[] array, StoredArraySerializer serializer) throws IOException {
        this(array, serializer, new StoredArrayFileManagerImpl());
    }
    
    public StoredArray(T[] array, StoredArraySerializer serializer, StoredArrayFileManager fileManager) throws IOException {
        this.serializer = serializer;
        this.fileManager = fileManager;
        createInitialState(array);
    }

    public StoredArray(int initialSize, StoredArraySerializer serializer, StoredArrayFileManager fileManager) throws IOException {
        this.serializer = serializer;
        this.fileManager = fileManager;
        createInitialState(initialSize);
    }
    
    public StoredArray(StoredArray<? extends T> source) throws IOException {
        this.serializer = source.serializer;
        this.fileManager = new StoredArrayFileManagerImpl();
        createInitialState(0);
        for (int i = 0; i < source.size(); ++i) {
            append(source.get(i));
        }
    }
    
    public void append(T element) throws IOException {
        appendElement(element);
    }
    
    public void set(int index, T element) throws IOException {
        assert index >= 0 && index < size;
        if (element == null) {
            setNull(index);
        } else {
            byte[] elementBuf = serializer.serialize(element);
            int newLen = elementBuf.length + Short.BYTES;

            seekToIndex(index);
            int offset = indexFile.readInt();
            int len = indexFile.readInt();

            int newOffset = (newLen <= len ? offset : (int) dataFile.length());
            setIndexData(index, newOffset, newLen);
            seekToObject(newOffset);
            writeElementData(serializer.getId(element), elementBuf);
        }
    }

    private void setNull(int index) throws IOException {
        seekToIndex(index);
        indexFile.readInt();
        indexFile.writeInt(0);
    }
    
    // time-consuming operation
    public void insert(int index, T element) throws IOException {
        assert index >= 0 && index <= size;
        
        if (index == size) {
            append(element);
            return;
        }

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
    }
    
    public T get(int index) throws IOException {
        assert index < size;
        int len = prepareForElementReading(index);
        if (len > 0) {
            return readElement(len);
        }
        return null;
    }
    
    public int size() {
        return size;
    }
    
    private void createInitialState(T[] array) throws IOException {
        openFiles();
        for (T element : array) {
            appendElement(element);
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
        int len = 0;
        if (element != null) {
            seekToObject(offset);
            len = writeElement(element);
        } 
        setIndexData(size, offset, len);
        ++size;
    }
    
    private void setIndexData(int index, int offset, int len) throws IOException {
        seekToIndex(index);
        indexFile.writeInt(offset);
        indexFile.writeInt(len);
    }
    
    private T readElement(int len) throws IOException {
        int id = dataFile.readShort();
        byte[] elementBuf = new byte[len - Short.BYTES];
        dataFile.read(elementBuf);
        return (T) serializer.deserialize(id, elementBuf);
    }
    
    private int writeElement(T element) throws IOException {
        if (element != null) {
            byte[] buf = serializer.serialize(element);
            writeElementData(serializer.getId(element), buf);
            return Short.BYTES + buf.length;
        }
        return 0;
    }
    
    private void writeElementData(int id, byte[] elementData) throws IOException {
        dataFile.writeShort(id);
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

    public void squeeze() throws IOException {
        File tmpIndexFile = fileManager.tmpIndexFile();
        File tmpDataFile = fileManager.tmpDataFile();
        
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

    private void openFiles() throws IOException {
        dataFile = fileManager.openDataFile();
        indexFile = fileManager.openIndexFile();
    }
    
    private void closeFiles() throws IOException {
        dataFile.close();
        indexFile.close();
    }
    
    private void seekToIndex(int index) throws IOException {
        indexFile.seek(index * Integer.BYTES * 2);
    }
    
    private void seekToObject(int offset) throws IOException {
        dataFile.seek(offset);
    }

    public static class StoredArrayException extends RuntimeException {
        
    }
}
