package lsfusion.client.form.controller.remote.serialization;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.file.AppImage;
import lsfusion.base.file.IOUtils;
import lsfusion.client.form.ClientForm;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.filter.ClientRegularFilter;
import lsfusion.client.form.filter.ClientRegularFilterGroup;
import lsfusion.client.form.filter.user.ClientFilter;
import lsfusion.client.form.filter.user.ClientFilterControls;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientObject;
import lsfusion.client.form.object.table.ClientToolbar;
import lsfusion.client.form.object.table.grid.ClientGrid;
import lsfusion.client.form.object.table.grid.user.toolbar.ClientCalculations;
import lsfusion.client.form.object.table.tree.ClientTreeGroup;
import lsfusion.client.form.property.ClientPivotColumn;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.form.remote.serialization.SerializationUtil;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

import static lsfusion.base.ApiResourceBundle.getString;

public class ClientSerializationPool {
    private final Map<Integer, Class<? extends ClientCustomSerializable>> idToClass = new HashMap<>();
    private final Map<Long, ClientCustomSerializable> objects = new HashMap<>();

    public ClientForm context;
    private static final int NULL_REF_CLASS = -1;

    public ClientSerializationPool() {
        this(null);
    }

    public ClientSerializationPool(ClientForm context) {
        this.context = context;
        // порядок добавления должен соответствовать порядку в ServerSerializationPool
        addMapping(ClientForm.class);
        addMapping(ClientComponent.class);
        addMapping(ClientContainer.class);
        addMapping(ClientGroupObject.class);
        addMapping(ClientTreeGroup.class);
        addMapping(ClientGrid.class);
        addMapping(ClientToolbar.class);
        addMapping(ClientFilter.class);
        addMapping(ClientFilterControls.class);
        addMapping(ClientCalculations.class);
        addMapping(ClientObject.class);
        addMapping(ClientPropertyDraw.class);
        addMapping(ClientRegularFilter.class);
        addMapping(ClientRegularFilterGroup.class);
        addMapping(ClientPivotColumn.class);
    }

    protected void addMapping(Class<? extends ClientCustomSerializable> clazz) {
        int classId = idToClass.size();
        idToClass.put(classId, clazz);
    }

    public ClientCustomSerializable get(int classId, int id) {
        return objects.get(getLongId(classId, id));
    }

    public void put(int classId, int id, ClientCustomSerializable value) {
        objects.put(getLongId(classId, id), value);
    }

    private long getLongId(int classId, int id) {
        return (((long) classId) << 32L) | id;
    }

    public Class<? extends ClientCustomSerializable> getClassById(int classId) {
        if (!idToClass.containsKey(classId)) {
            throw new IllegalArgumentException(getString("serialization.unknown.class.identifier.serialization", classId));
        }
        return idToClass.get(classId);
    }

    public <T extends ClientCustomSerializable> List<T> deserializeList(DataInputStream inStream) throws IOException {
        return (List<T>) deserializeCollection(new ArrayList<T>(), inStream);
    }

    public <T extends ClientCustomSerializable> Set<T> deserializeSet(DataInputStream inStream) throws IOException {
        return (Set<T>) deserializeCollection(new HashSet<T>(), inStream);
    }

    public <T extends ClientCustomSerializable> Collection<T> deserializeCollection(Collection<T> collection, DataInputStream inStream) throws IOException {
        int size = inStream.readInt();
        for (int i = 0; i < size; ++i) {
            T element = deserializeObject(inStream);
            collection.add(element);
        }
        return collection;
    }

    public <K extends ClientCustomSerializable, V extends ClientCustomSerializable> Map<K, V> deserializeMap(DataInputStream inStream) throws IOException {
        HashMap<K, V> result = new HashMap<>();

        int size = inStream.readInt();
        for (int i = 0; i < size; ++i) {
            K key = deserializeObject(inStream);
            V value = deserializeObject(inStream);
            result.put(key, value);
        }

        return result;
    }

    public <T extends ClientCustomSerializable> T deserializeObject(DataInputStream inStream) throws IOException {
        int classId = inStream.readInt();
        if (classId == NULL_REF_CLASS) {
            return null;
        }

        Class<? extends ClientCustomSerializable> clazz = getClassById(classId);

        ClientCustomSerializable instance;
        if (ClientIdentitySerializable.class.isAssignableFrom(clazz)) {
            int id = inStream.readInt();
            instance = get(classId, id);
            if (instance == null) {
                instance = createNewInstance(inStream, clazz, id, classId);
            }
        } else {
            instance = createNewInstance(inStream, clazz, -1, classId);
        }

        return (T) instance;
    }

    private ClientCustomSerializable createNewInstance(DataInputStream inStream,
                                                    Class<? extends ClientCustomSerializable> clazz,
                                                    int id, int classId) {
        try {
            Constructor<? extends ClientCustomSerializable> ctor = clazz.getConstructor();
            ClientCustomSerializable instance = ctor.newInstance();

            if (ClientIdentitySerializable.class.isAssignableFrom(clazz)) {
                put(classId, id, instance);
                ((ClientIdentitySerializable) instance).setID(id);
            }
            instance.customDeserialize(this, inStream);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(getString("serialization.can.not.create.object.of.class", clazz.toString()), e);
        }
    }

    // Read-only helpers available on client
    public <T> T readObject(DataInputStream inStream) throws IOException {
        return BaseUtils.readObject(inStream);
    }

    public AppImage readImageIcon(DataInputStream inStream) throws IOException {
        return IOUtils.readAppImage(inStream);
    }

    public String readString(DataInputStream inStream) throws IOException {
        return SerializationUtil.readString(inStream);
    }

    public boolean readBoolean(DataInputStream inStream) throws IOException {
        return SerializationUtil.readBoolean(inStream);
    }

    public Integer readInt(DataInputStream inStream) throws IOException {
        return SerializationUtil.readInt(inStream);
    }

    public Long readLong(DataInputStream inStream) throws IOException {
        return SerializationUtil.readLong(inStream);
    }
}
