package lsfusion.base.col.implementations.stored;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class StoredArrayTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    
    
    public static StoredArraySerializerImpl serializer = new StoredArraySerializerImpl();
    static {
        serializer.register(SerializableClass.class, SerializableClass::serialize, SerializableClass::deserialize);
        serializer.register(DerivedSerializableClass.class, DerivedSerializableClass::serialize, DerivedSerializableClass::deserialize);
    }

    @Test
    public void createWithArray() {
        SerializableClass[] array = initArray();
        StoredArray<SerializableClass> stored = new StoredArray<>(array, serializer);
        assertEquals(stored.size(), array.length);
    }

    @Test 
    public void createWithZeroLengthArray() {
        StoredArray<SerializableClass> stored = new StoredArray<>(new SerializableClass[0], serializer);
        assertEquals(stored.size(), 0);
    }

    @Test
    public void createWithSize() {
        StoredArray<SerializableClass> stored = new StoredArray<>(10, serializer);
        assertEquals(stored.size(), 10);
    }
    
    @Test 
    public void createEmpty() {
        StoredArray<SerializableClass> stored = new StoredArray<>(serializer);
        assertEquals(stored.size(), 0);
    }
    
    @Test
    public void createWithArrayAndGet() {
        SerializableClass[] array = initArray();
        StoredArray<SerializableClass> stored = new StoredArray<>(array, serializer);
        checkEquality(array, stored);
    }

    @Test
    public void createWithArrayAndGetReversed() {
        SerializableClass[] array = initArray();
        StoredArray<SerializableClass> stored = new StoredArray<>(array, serializer);
        for (int i = stored.size() - 1; i >= 0; --i) {
            assertEquals(array[i], stored.get(i));
        }
    }

    @Test
    public void createWithCopyConstructor() {
        SerializableClass[] array = initArrayWithNulls();
        StoredArray<SerializableClass> stored = new StoredArray<>(array, serializer);
        StoredArray<SerializableClass> target = new StoredArray<>(stored);
        checkEquality(array, target);
    }

    @Test 
    public void checkArrayCopy() {
        SerializableClass[] array = initArrayWithNulls();
        StoredArray<SerializableClass> source = new StoredArray<>(array, serializer);
        StoredArray<SerializableClass> target = new StoredArray<>(source);
        source.set(0, new SerializableClass("test", 50, false));
        assertNull(target.get(0));
    }
    
    @Test
    public void checkUnserializableObjectAssert() {
        SerializableClass[] array = initArray();
        array[3] = new OtherSerializableClass("triangle", 5, true);
        thrown.expect(AssertionError.class);
        StoredArray<SerializableClass> stored = new StoredArray<>(array, serializer);
    }
    
    @Test
    public void addAndGet() {
        StoredArray<SerializableClass> stored = new StoredArray<>(serializer);
        SerializableClass[] array = initArray();
        for (SerializableClass element : array) {
            stored.append(element);    
        }
        checkEquality(array, stored);
    }

    @Test
    public void addAndImmediateGet() {
        SerializableClass[] array = initArray();
        StoredArray<SerializableClass> stored = new StoredArray<>(serializer);
        for (int i = 0; i < stored.size(); ++i) {
            stored.append(array[i]);
            assertEquals(array[i], stored.get(i));
        }
    }

    @Test
    public void createAndReverseMultipleTimes() {
        SerializableClass[] array = initArray();
        StoredArray<SerializableClass> stored = new StoredArray<>(array, serializer);
        int n = stored.size();
        for (int step = 0; step < 10; ++step) {
            for (int i = 0; i < n / 2; ++i) {
                SerializableClass a = stored.get(i);
                SerializableClass b = stored.get(n - i - 1);
                stored.set(i, b);
                stored.set(n - i - 1, a);
            }
        }
        checkEquality(array, stored);
    }

    @Test
    public void randomAddWithIndex() {
        SerializableClass[] array = initArray();
        StoredArray<SerializableClass> stored = new StoredArray<>(new SerializableClass[array.length], serializer);
        int n = stored.size();
        List<Integer> indexes = IntStream.range(0, n).boxed().collect(Collectors.toList());
        Collections.shuffle(indexes);
        for (int i = 0; i < n; ++i) {
            stored.set(indexes.get(i), array[indexes.get(i)]);
        }
        checkEquality(array, stored);
    }
    
    @Test
    public void createWithSizeAndCheckForNulls() {
        StoredArray<SerializableClass> stored = new StoredArray<>(10, serializer);
        for (int i = 0; i < stored.size(); ++i) {
            SerializableClass cls = stored.get(i);
            assertNull(cls);
        }
    }

    @Test
    public void createWithArrayWithNullsAndGet() {
        SerializableClass[] array = initArrayWithNulls();
        StoredArray<SerializableClass> stored = new StoredArray<>(array, serializer);
        checkEquality(array, stored);
    }

    @Test
    public void appendWithNullsAndGet() {
        SerializableClass[] array = initArrayWithNulls();
        StoredArray<SerializableClass> stored = new StoredArray<>(serializer);
        for (SerializableClass obj : array) {
            stored.append(obj);    
        }
        checkEquality(array, stored);
    }

    @Test
    public void setWithNullsAndGet() {
        SerializableClass[] array = initArrayWithNulls();
        StoredArray<SerializableClass> stored = new StoredArray<>(array.length, serializer);
        for (int i = 0; i < array.length; ++i) {
            stored.set(i, array[i]);
        }
        checkEquality(array, stored);
    }
    
    @Test
    public void squeeze() {
        SerializableClass[] array = initArrayWithNulls();
        final DerivedSerializableClass weighty = new DerivedSerializableClass("LongStringStringString", 45, false, "                    ");
        StoredArray<SerializableClass> stored = new StoredArray<>(array, serializer);
        for (int i = 0; i < stored.size(); ++i) {
            if (stored.get(i) == null) {
                stored.set(i, weighty);
            }
        }
        stored.squeeze();
        for (int i = 0; i < stored.size(); ++i) {
            if (array[i] == null) {
                assertEquals(weighty, stored.get(i));
            } else {
                assertEquals(array[i], stored.get(i));
            }
        }
    }
    
    @Test
    public void insertToBeginning() {
        SerializableClass[] array = initArrayWithNulls();
        int mid = array.length / 2;
        StoredArray<SerializableClass> stored = new StoredArray<>(serializer);
        for (int i = mid; i < array.length; ++i) {
            stored.append(array[i]);
        }
        for (int i = 0; i < mid; ++i) {
            stored.insert(i, array[i]);
        }
        checkEquality(array, stored);
    }

    @Test
    public void insertToMiddle() {
        SerializableClass[] array = initArrayWithNulls();
        int mid = array.length / 4;
        StoredArray<SerializableClass> stored = new StoredArray<>(serializer);
        for (int i = 0; i < mid; ++i) {
            stored.append(array[i]);
        }
        for (int i = array.length - mid; i < array.length; ++i) {
            stored.append(array[i]);
        }
        for (int i = array.length - mid - 1; i >= mid; --i) {
            stored.insert(mid, array[i]);
        }
        checkEquality(array, stored);
    }

    @Test
    public void insertToEnd() {
        SerializableClass[] array = initArrayWithNulls();
        StoredArray<SerializableClass> stored = new StoredArray<>(serializer);
        for (int i = 0; i < array.length; ++i) {
            stored.insert(i, array[i]);
        }
        checkEquality(array, stored);
    }
    
    
    private void checkEquality(SerializableClass[] array, StoredArray<SerializableClass> stored) {
        assertEquals(array.length, stored.size());
        for (int i = 0; i < stored.size(); ++i) {
            assertEquals(array[i], stored.get(i));
        }
    }
    
    private SerializableClass[] initArray() {
        List<SerializableClass> result = new ArrayList<>();
        NameClass name = new NameClass("Square");
        for (int i = 0; i < 1; ++i) {
            result.add(new SerializableClass(name, 10, true));
            result.add(new SerializableClass(name, 8, false));
            result.add(new SerializableClass("Circle", 5, false));
            result.add(new DerivedSerializableClass("Tricky circle", 20, true, "add"));    
            result.add(new SerializableClass("Triangle", 100000, true));
            result.add(new DerivedSerializableClass("Tricky triangle", -454, false, "add"));
        }
        return result.toArray(new SerializableClass[0]);
    }

    private SerializableClass[] initArrayWithNulls() {
        SerializableClass[] array = new SerializableClass[10];
        array[1] = new SerializableClass("Square", 10, true);
        array[3] = new SerializableClass("Square", 8, false);
        array[4] = new SerializableClass("Circle", 5, false);
        array[5] = new DerivedSerializableClass("Tricky circle", 20, true, "add");
        array[8] = new DerivedSerializableClass("Tricky triangle", -454, false, "add");
        return array;
    }

    private static class SerializableClass {
        private final NameClass name;
        public int cnt;
        public boolean isLarge;

        public SerializableClass(String name, int cnt, boolean isLarge) {
            this(new NameClass(name), cnt, isLarge);
        }

        public SerializableClass(NameClass name, int cnt, boolean isLarge) {
            this.name = name;
            this.cnt = cnt;
            this.isLarge = isLarge;
        }

        public String getName() {
            return name.getName();
        }

        protected byte[] serialize() {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try {
                ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
                objStream.writeObject(name.getName());
                objStream.writeInt(cnt);
                objStream.writeBoolean(isLarge);
                objStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return byteStream.toByteArray();
        }

        public static byte[] serialize(Object o) {
            return ((SerializableClass)o).serialize();
        }

        public static SerializableClass deserialize(byte[] buf) {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(buf);
            try {
                ObjectInputStream objStream = new ObjectInputStream(byteStream);
                String name = (String)objStream.readObject();
                int cnt = objStream.readInt();
                boolean isLarge = objStream.readBoolean();
                return new SerializableClass(name, cnt, isLarge);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SerializableClass that = (SerializableClass) o;
            return cnt == that.cnt &&
                    isLarge == that.isLarge &&
                    Objects.equals(name.getName(), that.name.getName());
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, cnt, isLarge);
        }
    }

    private static class DerivedSerializableClass extends SerializableClass {
        private final String additional;

        public DerivedSerializableClass(String name, int cnt, boolean isLarge, String additional) {
            super(name, cnt, isLarge);
            this.additional = additional;
        }

        @Override
        protected byte[] serialize() {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try {
                ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
                objStream.writeObject(getName());
                objStream.writeInt(cnt);
                objStream.writeBoolean(isLarge);
                objStream.writeObject(additional);
                objStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return byteStream.toByteArray();
        }

        public static byte[] serialize(Object o) {
            return ((DerivedSerializableClass)o).serialize();
        }

        public static DerivedSerializableClass deserialize(byte[] buf) {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(buf);
            try {
                ObjectInputStream objStream = new ObjectInputStream(byteStream);
                String name = (String)objStream.readObject();
                int cnt = objStream.readInt();
                boolean isLarge = objStream.readBoolean();
                String additional = (String)objStream.readObject();
                return new DerivedSerializableClass(name, cnt, isLarge, additional);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            DerivedSerializableClass that = (DerivedSerializableClass) o;
            return Objects.equals(additional, that.additional);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), additional);
        }
    }

    private static class OtherSerializableClass extends SerializableClass {
        public OtherSerializableClass(String name, int cnt, boolean isLarge) {
            super(name, cnt, isLarge);
        }
    }

    private static class NameClass implements Serializable {
        private final String name;

        public NameClass(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
