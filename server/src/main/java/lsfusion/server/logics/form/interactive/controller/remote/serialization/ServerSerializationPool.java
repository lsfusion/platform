package lsfusion.server.logics.form.interactive.controller.remote.serialization;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.file.AppImage;
import lsfusion.base.file.IOUtils;
import lsfusion.interop.form.remote.serialization.SerializationUtil;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.filter.FilterControlsView;
import lsfusion.server.logics.form.interactive.design.filter.FilterView;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterGroupView;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterView;
import lsfusion.server.logics.form.interactive.design.object.*;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.struct.property.PivotColumn;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.base.ApiResourceBundle.getString;

public class ServerSerializationPool {
    private final Map<Class<? extends ServerCustomSerializable>, Integer> classToId = new HashMap<>();
    private final Map<Long, ServerCustomSerializable> objects = new HashMap<>();

    public FormInstanceContext context;
    private static final int NULL_REF_CLASS = -1;

    public ServerSerializationPool(FormInstanceContext context) {
        this.context = context;

        // порядок добавления должен соответствовать порядку в ClientSerializationPool

        addMapping(FormView.class);
        addMapping(ComponentView.class);
        addMapping(ContainerView.class);
        addMapping(GroupObjectView.class);
        addMapping(TreeGroupView.class);
        addMapping(GridView.class);
        addMapping(ToolbarView.class);
        addMapping(FilterView.class);
        addMapping(FilterControlsView.class);
        addMapping(CalculationsView.class);
        addMapping(ObjectView.class);
        addMapping(PropertyDrawView.class);
        addMapping(RegularFilterView.class);
        addMapping(RegularFilterGroupView.class);
        addMapping(PivotColumn.class);
    }

    protected void addMapping(Class<? extends ServerCustomSerializable> clazz) {
        int classId = classToId.size();
        classToId.put(clazz, classId);
    }

    public ServerCustomSerializable get(int classId, int id) {
        return objects.get(getLongId(classId, id));
    }

    public void put(int classId, int id, ServerCustomSerializable value) {
        objects.put(getLongId(classId, id), value);
    }

    private long getLongId(int classId, int id) {
        return (((long) classId) << 32L) | id;
    }

    public int getClassId(Class<?> clazz) {
        // делаем поиск вверх по иерархии...
        while (clazz != null && !classToId.containsKey(clazz)) {
            clazz = clazz.getSuperclass();
        }

        if (clazz == null || !classToId.containsKey(clazz)) {
            throw new IllegalArgumentException(getString("serialization.unknown.class", clazz));
        }

        return classToId.get((Class<? extends ServerCustomSerializable>) clazz);
    }

    public <T extends ServerCustomSerializable> void serializeObject(DataOutputStream outStream, T object) throws IOException {
        if (object == null) {
            outStream.writeInt(NULL_REF_CLASS);
            return;
        }

        int classId = getClassId(object.getClass());
        outStream.writeInt(classId);

        if (object instanceof ServerIdentitySerializable) {
            ServerIdentitySerializable identityObj = (ServerIdentitySerializable) object;
            int id = identityObj.getID();
            outStream.writeInt(id);

            if (get(classId, id) == null) {
                put(classId, id, object);
                object.customSerialize(this, outStream);
            }
        } else {
            object.customSerialize(this, outStream);
        }
    }

    public <T extends ServerCustomSerializable> void serializeCollection(DataOutputStream outStream, Collection<T> list) throws IOException {
        outStream.writeInt(list.size());
        for (T element : list) {
            serializeObject(outStream, element);
        }
    }

    public <T extends ServerCustomSerializable> void serializeCollection(DataOutputStream outStream, ImList<T> list) throws IOException {
        outStream.writeInt(list.size());
        for (T element : list) {
            serializeObject(outStream, element);
        }
    }

    public <T extends ServerCustomSerializable> void serializeCollection(DataOutputStream outStream, ImSet<T> list) throws IOException {
        outStream.writeInt(list.size());
        for (T element : list) {
            serializeObject(outStream, element);
        }
    }

    public <K extends ServerCustomSerializable, V extends ServerCustomSerializable> void serializeMap(DataOutputStream outStream, Map<K, V> map) throws IOException {
        outStream.writeInt(map.size());
        for (Map.Entry<K, V> entry : map.entrySet()) {
            serializeObject(outStream, entry.getKey());
            serializeObject(outStream, entry.getValue());
        }
    }

    // Write-only helpers available on server
    public void writeObject(DataOutputStream outStream, Object object) throws IOException {
        BaseUtils.writeObject(outStream, object);
    }

    public void writeImageIcon(DataOutputStream outStream, AppImage appImage) throws IOException {
        IOUtils.writeAppImage(outStream, appImage);
    }

    public void writeString(DataOutputStream outStream, String str) throws IOException {
        SerializationUtil.writeString(outStream, str);
    }

    public void writeBoolean(DataOutputStream outStream, boolean bool) throws IOException {
        SerializationUtil.writeBoolean(outStream, bool);
    }

    public void writeInt(DataOutputStream outStream, Integer integer) throws IOException {
        SerializationUtil.writeInt(outStream, integer);
    }

    public void writeLong(DataOutputStream outStream, Long n) throws IOException {
        SerializationUtil.writeLong(outStream, n);
    }
}
