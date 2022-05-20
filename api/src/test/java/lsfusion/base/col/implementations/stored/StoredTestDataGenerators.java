package lsfusion.base.col.implementations.stored;

import java.io.*;
import java.math.BigInteger;
import java.util.*;

public class StoredTestDataGenerators {
    private static final int LARGE_SIZE = 10000;
    private static final int MEDIUM_SIZE = 500;

    public static StoredClass[] simpleArray() {
        SerializableNameClass name = new SerializableNameClass("Square");
        return new StoredClass[]{
            new StoredClass(name, 10, true),
            new StoredClass(name, 8, false),
            new StoredClass("Circle", 5, false),
            new DerivedStoredClass("Tricky circle", 20, true, "add"),
            new StoredClass("Triangle", 100000, true),
            new DerivedStoredClass("Tricky triangle", -454, false, "add"),
            new StoredClass("Rectangle", 1, true),
            new StoredClass("Rectangle", 1, false),
            new DerivedStoredClass("Tricky rectangle", 1, true, "add"),
            new StoredClass("Circle", -1, true)    
        };
    }

    public static StoredClass[] repeatedArray(int size) {
        StoredClass[] arr = simpleArray();  
        List<StoredClass> result = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            result.add(arr[i % arr.length]);
        }
        return result.toArray(new StoredClass[0]);
    }

    public static StoredClass[] arrayWithNulls() {
        return new StoredClass[]{
            null,
            new StoredClass("Square", 10, true),
            null,
            new StoredClass("Square", 8, false),
            new StoredClass("Circle", 5, false),
            new DerivedStoredClass("Tricky circle", 20, true, "add"),
            null,
            null,
            new DerivedStoredClass("Tricky triangle", -454, false, "add"),
            null
        };
    }

    public static StoredClass[] sortedArray() {
        StoredClass[] array = simpleArray();
        Arrays.sort(array, Comparator.comparingInt(StoredClass::hashCode));
        return array;
    }

    static String[] stringArray() {
        return new String[]{"Hello", " ", "World", "!", "4", "5", "6", "7", "8", "9"};
    }

    public static Object[] mixedArray() {
        return new Object[]{
            null,
            new StoredClass("Square", 10, true),
            "text",
            new BigInteger("3454654657567567567567567"),
            null,
            new StoredClass("Square", 8, false),
            null,
            "text2",
            new StoredClass("Circle", 5, false),
            null
        };
    }

    public static StoredClass[] mediumSimpleArray() { return replicateStoredArray(simpleArray(), MEDIUM_SIZE); }

    public static StoredClass[] largeSimpleArray() {
        return replicateStoredArray(simpleArray(), LARGE_SIZE);
    }

    public static StoredClass[] largeArrayWithNulls() {
        return replicateStoredArray(arrayWithNulls(), LARGE_SIZE);
    }

    public static StoredClass[] largeSortedArray() {
        StoredClass[] array = largeSimpleArray();
        Arrays.sort(array, Comparator.comparingInt(StoredClass::hashCode));
        return array;
    }

    public static String[] mediumStringArray() {
        return replicateStringArray(stringArray(), MEDIUM_SIZE);
    }

    public static String[] largeStringArray() {
        return replicateStringArray(stringArray(), LARGE_SIZE);
    }

    private static String[] replicateStringArray(String[] array, int size) {
        String[] res = new String[size];
        for (int i = 0; i < size; ++i) {
            res[i] = array[i % array.length];
        }
        return res;
    }

    public static Object[] mediumMixedArray() { return replicateArray(mixedArray(), MEDIUM_SIZE); }

    public static Object[] largeMixedArray() {
        return replicateArray(mixedArray(), LARGE_SIZE);
    }
    
    private static StoredClass[] replicateStoredArray(StoredClass[] array, int size) {
        StoredClass[] res = new StoredClass[size];
        int sz = array.length;
        for (int i = 0; i < size; ++i) {
            StoredClass obj = array[i % sz];
            if (obj != null) {
                try {
                    StoredClass stored = obj.clone();
                    stored.cnt = i;
                    res[i] = stored;
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                res[i] = null;
            }
        }
        return res;
    }

    private static Object[] replicateArray(Object[] array, int size) {
        Object[] list = new Object[size];
        int sz = array.length;
        for (int i = 0; i < size; ++i) {
            Object obj = array[i % sz];
            if (obj instanceof StoredClass) {
                try {
                    StoredClass stored = ((StoredClass) obj).clone();
                    stored.cnt = i;
                    list[i] = stored;
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                list[i] = obj;
            }
        }
        return list;
    }
    
    public static class StoredClass implements Cloneable {
        private SerializableNameClass name;
        public int cnt;
        public boolean isLarge;

        public StoredClass(String name, int cnt, boolean isLarge) {
            this(new SerializableNameClass(name), cnt, isLarge);
        }

        public StoredClass(SerializableNameClass name, int cnt, boolean isLarge) {
            this.name = name;
            this.cnt = cnt;
            this.isLarge = isLarge;
        }

        public StoredClass clone() throws CloneNotSupportedException {
            StoredClass cloned = (StoredClass) super.clone();
            cloned.name = new SerializableNameClass(name.getName());
            return cloned;
        }

        public String getName() {
            return name.getName();
        }

        protected void serialize(ByteArrayOutputStream byteStream) {
            try (ObjectOutputStream objStream = new ObjectOutputStream(byteStream)) {
                objStream.writeObject(name.getName());
                objStream.writeInt(cnt);
                objStream.writeBoolean(isLarge);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public static void serialize(Object o, StoredArraySerializer serializer, ByteArrayOutputStream stream) {
            ((StoredClass)o).serialize(stream);
        }

        public static StoredClass deserialize(ByteArrayInputStream byteStream, StoredArraySerializer serializer) {
            try {
                ObjectInputStream objStream = new ObjectInputStream(byteStream);
                String name = (String)objStream.readObject();
                int cnt = objStream.readInt();
                boolean isLarge = objStream.readBoolean();
                return new StoredClass(name, cnt, isLarge);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StoredClass that = (StoredClass) o;
            return cnt == that.cnt &&
                    isLarge == that.isLarge &&
                    Objects.equals(name.getName(), that.name.getName());
        }

        @Override
        public int hashCode() {
            return Objects.hash(name.getName(), cnt, isLarge);
        }
    }

    public static class DerivedStoredClass extends StoredClass {
        private String additional;

        public DerivedStoredClass(String name, int cnt, boolean isLarge, String additional) {
            super(name, cnt, isLarge);
            this.additional = additional;
        }

        public DerivedStoredClass clone() throws CloneNotSupportedException {
            return (DerivedStoredClass) super.clone();
        }

        @Override
        protected void serialize(ByteArrayOutputStream byteStream) {
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
        }

        public static void serialize(Object o, StoredArraySerializer serializer, ByteArrayOutputStream oStream) {
            ((DerivedStoredClass)o).serialize(oStream);
        }

        public static DerivedStoredClass deserialize(ByteArrayInputStream byteStream, StoredArraySerializer serializer) {
            try {
                ObjectInputStream objStream = new ObjectInputStream(byteStream);
                String name = (String)objStream.readObject();
                int cnt = objStream.readInt();
                boolean isLarge = objStream.readBoolean();
                String additional = (String)objStream.readObject();
                return new DerivedStoredClass(name, cnt, isLarge, additional);
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
            DerivedStoredClass that = (DerivedStoredClass) o;
            return Objects.equals(additional, that.additional);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), additional);
        }
    }

    public static class OtherStoredClass extends StoredClass {
        public OtherStoredClass(String name, int cnt, boolean isLarge) {
            super(name, cnt, isLarge);
        }
    }

    public static class SerializableNameClass implements Serializable {
        private final String name;

        public SerializableNameClass(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
