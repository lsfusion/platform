package lsfusion.base.col.implementations.stored;

import lsfusion.base.Pair;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class StoredArray<T> {
    private final StoredArraySerializer serializer;
    private final StoredArrayFileManager fileManager;
    
    private int storedSize = 0;
    private final int CHUNK_SIZE = 100;
    
    private RandomAccessFile indexFile;
    private RandomAccessFile dataFile;
    
    private final ArrayList<T> dataBuffer = new ArrayList<>();

    private final int olBufferSize = Long.BYTES + Integer.BYTES;
    private final byte[] offsetLenBuffer = new byte[olBufferSize];
    
    public StoredArray(StoredArraySerializer serializer) {
        this(0, serializer);
    }

    public StoredArray(int size, StoredArraySerializer serializer) {
        this(size, serializer, null);
    }
    
    public StoredArray(T[] array, StoredArraySerializer serializer) throws StoredArrayCreationException {
        this(array, serializer, null);
    }
    
    public StoredArray(T[] array, StoredArraySerializer serializer, StoredArrayFileManager fileManager) throws StoredArrayCreationException {
        try {
            this.serializer = serializer;
            this.fileManager = (fileManager == null ? new StoredArrayFileManagerImpl() : fileManager);
            createInitialState(array);
        } catch (Exception e) {
            throw new StoredArrayCreationException(e);
        }
    }

    public StoredArray(int size, T[] array, StoredArraySerializer serializer, StoredArrayFileManager fileManager) throws StoredArrayCreationException {
        try {
            this.serializer = serializer;
            this.fileManager = (fileManager == null ? new StoredArrayFileManagerImpl() : fileManager);
            createInitialState(size, array);
        } catch (Exception e) {
            throw new StoredArrayCreationException(e);
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
        assert index >= 0 && index < size();
        if (index >= storedSize) {
            dataBuffer.set(index - storedSize, element);
        } else {
            try {
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                serializer.serialize(element, outStream);
                int newLen = outStream.size();

                seekToIndex(index);
                ByteBuffer buf = readOffsetLen();
                long offset = buf.getLong();
                int len = buf.getInt();

                long newOffset = (newLen <= len ? offset : dataFile.length());
                setIndexData(index, newOffset, newLen);
                seekToObject(newOffset);
                writeElementData(outStream.toByteArray());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }        
    }

    public void sort() {
        sort(null, null);
    }

    public void sort(int[] order) {
        sort(null, order);
    }

    public <V> void sort(StoredArray<V> mappedArray) {
        sort(mappedArray, null);
    }

    // loads hashes of all elements into memory
    public <V> void sort(StoredArray<V> mappedArray, int[] order) {
        assert order == null || order.length == size();
        ArrayList<Pair<Integer, Integer>> hashes = new ArrayList<>();
        for (int i = 0; i < size(); ++i) {
            Object element = get(i);
            hashes.add(new Pair<>(element == null ? 0 : element.hashCode(), i));
        }

        hashes.sort((o1, o2) -> {
            if (o1.first < o2.first) return -1;
            if (o1.first > o2.first) return 1;
            return Integer.compare(o1.second, o2.second);
        });
        rearrange(hashes);
        if (mappedArray != null) {
            mappedArray.rearrange(hashes);
        }
        if (order != null) {
            for (int i = 0; i < hashes.size(); ++i) {
                order[hashes.get(i).second] = i;
            }
        }
    }

    // Uses only the second integer of each pair. Takes an array of pairs only to get rid of unnecessary copying
    private void rearrange(ArrayList<Pair<Integer, Integer>> sortedHashes) {
        try {
            flushBuffer(); // ?

            File tmpIndexFile = fileManager.tmpIndexFile();
            try (
                DataOutputStream tmpIndexStream = new DataOutputStream(new FileOutputStream(tmpIndexFile))
            ) {
                for (int i = 0; i < size(); ++i) {
                    Pair<Long, Integer> offsetLen = getOffsetLen(sortedHashes.get(i).second);
                    tmpIndexStream.writeLong(offsetLen.first);
                    tmpIndexStream.writeInt(offsetLen.second);
                }
            }

            indexFile.close();
            fileManager.replaceIndexFileByTmpFile();
            indexFile = fileManager.openIndexFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // time-consuming operation
    public void insert(int index, T element) {
        assert index >= 0 && index <= size();
        
        if (index == size()) {
            append(element);
        } else if (index >= storedSize) {
            dataBuffer.add(index - storedSize, element);
        } else {
            try {
                long offset = dataFile.length();
                seekToObject(offset);
                int len = writeElement(element);

                seekToIndex(index);
                long nextOffset = 0;
                int nextLen = 0;
                for (int i = index; i <= storedSize; ++i) {
                    if (i < storedSize) {
                        ByteBuffer buf = readOffsetLen();
                        nextOffset = buf.getLong();
                        nextLen = buf.getInt();
                    }
                    setIndexData(i, offset, len);
                    offset = nextOffset;
                    len = nextLen;
                }
                ++storedSize;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
    
    public T get(int index) {
        assert index < size();
        if (index < storedSize) {
            try {
                int len = prepareForElementReading(index);
                return readElement(len);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            return dataBuffer.get(index - storedSize);
        }
    }
    
    public int size() {
        return storedSize + dataBuffer.size();
    }
    
    private void createInitialState(T[] array) throws IOException {
        createInitialState(array.length, array);
    }

    private void createInitialState(int size, T[] array) throws IOException {
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

    private void flushBuffer() throws IOException {
        ByteArrayOutputStream dataStreamBuf = new ByteArrayOutputStream();
        ByteArrayOutputStream indexStreamBuf = new ByteArrayOutputStream();
        
        int prevDataStreamSize = 0;
        long dataOffset = dataFile.length();
        seekToObject(dataOffset);
        seekToIndex(storedSize);
        for (T element : dataBuffer) {
            serializer.serialize(element, dataStreamBuf);
            int elementSize = dataStreamBuf.size() - prevDataStreamSize;
            ByteBuffer buffer = ByteBuffer.allocate(olBufferSize);
            buffer.putLong(dataOffset);
            buffer.putInt(elementSize);
            indexStreamBuf.write(buffer.array());
            dataOffset += elementSize;
            prevDataStreamSize = dataStreamBuf.size();
        }

        dataFile.write(dataStreamBuf.toByteArray());
        indexFile.write(indexStreamBuf.toByteArray());
        storedSize += dataBuffer.size();
        dataBuffer.clear();
    }
    
    private void appendElement(T element) throws IOException {
        dataBuffer.add(element);
        if (dataBuffer.size() >= CHUNK_SIZE) {
            flushBuffer();
        }
    }
    
    private void setIndexData(int index, long offset, int len) throws IOException {
        seekToIndex(index);
        ByteBuffer buffer = ByteBuffer.allocate(olBufferSize);
        buffer.putLong(offset);
        buffer.putInt(len);
        indexFile.write(buffer.array());
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
        Pair<Long, Integer> offsetLen = getOffsetLen(index);
        long offset = offsetLen.first;
        int len = offsetLen.second; 
        if (len > 0) {
            seekToObject(offset);
        }
        return len;
    }

    private Pair<Long, Integer> getOffsetLen(int index) throws IOException {
        assert index < storedSize;
        seekToIndex(index);
        ByteBuffer buf = readOffsetLen();
        long offset = buf.getLong();
        int len = buf.getInt();
        return new Pair<>(offset, len);
    }
    
    private ByteBuffer readOffsetLen() throws IOException {
        indexFile.read(offsetLenBuffer);
        return ByteBuffer.wrap(offsetLenBuffer);
    }    
    
    public void squeeze() {
        File tmpIndexFile = fileManager.tmpIndexFile();
        File tmpDataFile = fileManager.tmpDataFile();
        try {
            flushBuffer(); // todo [dale]: we can remove the flushBuffer call and process stored and buffered separately
            try (
                DataOutputStream tmpIndexStream = new DataOutputStream(new FileOutputStream(tmpIndexFile));
                FileOutputStream tmpDataStream = new FileOutputStream(tmpDataFile)
            ) {
                long total = 0;
                for (int i = 0; i < size(); ++i) {
                    int len = prepareForElementReading(i);
                    byte[] buf = new byte[len];
                    dataFile.read(buf);
                    tmpIndexStream.writeLong(total);
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
    
    private void seekToIndex(long index) throws IOException {
        indexFile.seek(index * olBufferSize);
    }
    
    private void seekToObject(long offset) throws IOException {
        dataFile.seek(offset);
    }

    public static class StoredArrayCreationException extends Exception {
        public StoredArrayCreationException(Throwable cause) {
            super(cause);
        }
    }

    public static boolean isStoredArraysEnabled() {
        String isEnabled = System.getProperty("storedArraysEnabled");
        return "true".equalsIgnoreCase(isEnabled);
    }

//    private static class StoredArrayIterator<T> implements Iterator<T> {
//        private final StoredArray<? extends T> array;
//        private int index;
//
//        public StoredArrayIterator(StoredArray<? extends T> array) {
//            this.array = array;
//            index = 0;
//        }
//
//        @Override
//        public boolean hasNext() {
//            return index >= array.size();
//        }
//
//        @Override
//        public T next() {
//            return array.get(index++);
//        }
//    }
}
