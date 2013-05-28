package platform.base;

import java.util.Arrays;

public class ByteArray {

    public final byte[] array;

    public ByteArray(byte[] array) {
        this.array = array;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ByteArray && Arrays.equals(array, ((ByteArray) o).array);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(array);
    }
}
