package platform.server.form.entity;

import platform.base.BaseUtils;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.LongMutable;
import platform.base.col.interfaces.mutable.MOrderExclSet;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.base.identity.IdentityObject;
import platform.interop.ClassViewType;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.Instantiable;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GroupObjectEntity extends IdentityObject implements Instantiable<GroupObjectInstance>, ServerIdentitySerializable {

    public static int PAGE_SIZE_DEFAULT_VALUE = 50;

    private int ID;

    public PropertyDrawEntity filterProperty;

    public TreeGroupEntity treeGroup;

    public CalcPropertyObjectEntity<?> reportPathProp;

    public GroupObjectEntity() {
    }

    public GroupObjectEntity(int ID) {
        this(ID, (String)null);
    }

    public GroupObjectEntity(int ID, String sID) {
        this.ID = ID;
        this.sID = sID != null ? sID : "groupObj" + ID;
    }

    public int getID() {
        return ID;
    }

    public void setID(int iID) {
        ID = iID;
    }

    public ClassViewType initClassView = ClassViewType.GRID;
    public List<ClassViewType> banClassView = new ArrayList<ClassViewType>();
    public Integer pageSize;

    public CalcPropertyObjectEntity<?> propertyBackground;
    public CalcPropertyObjectEntity<?> propertyForeground;

    public GroupObjectInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, getOrderObjects().toJavaList());
        pool.writeInt(outStream, initClassView.ordinal());
        pool.writeObject(outStream, banClassView);
        pool.serializeObject(outStream, treeGroup);
        pool.serializeObject(outStream, propertyBackground);
        pool.serializeObject(outStream, propertyForeground);
        pool.serializeObject(outStream, filterProperty);
        outStream.writeBoolean(isParent != null);
        if (isParent != null) {
            pool.serializeMap(outStream, isParent.toJavaMap());
        }
        pool.writeObject(outStream, pageSize);
        pool.serializeObject(outStream, reportPathProp);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        setObjects(SetFact.fromJavaOrderSet(pool.<ObjectEntity>deserializeList(inStream)));
        initClassView = ClassViewType.values()[pool.readInt(inStream)];
        banClassView = pool.readObject(inStream);
        treeGroup = pool.deserializeObject(inStream);
        propertyBackground = pool.deserializeObject(inStream);
        propertyForeground = pool.deserializeObject(inStream);
        filterProperty = pool.deserializeObject(inStream);
        if (inStream.readBoolean()) {
            isParent = MapFact.fromJavaMap(pool.<ObjectEntity, CalcPropertyObjectEntity<?>>deserializeMap(inStream));
        }
        pageSize = pool.readObject(inStream);
        reportPathProp = pool.deserializeObject(inStream);
    }

    public ImMap<ObjectEntity, CalcPropertyObjectEntity<?>> isParent = null;

    public void setIsParents(final CalcPropertyObjectEntity... properties) {
        isParent = getOrderObjects().mapOrderValues(new GetIndex<CalcPropertyObjectEntity<?>>() {
            public CalcPropertyObjectEntity<?> getMapValue(int i) {
                return properties[i];
            }});
    }

    public void setInitClassView(ClassViewType type) {
        initClassView = type;
    }

    public void setSingleClassView(ClassViewType type) {
        setInitClassView(type);
        banClassView.addAll(BaseUtils.toList(ClassViewType.PANEL, ClassViewType.GRID, ClassViewType.HIDE));
        banClassView.remove(type);
    }

    public boolean isAllowedClassView(ClassViewType type) {
        return !banClassView.contains(type);
    }

    private boolean finalizedObjects;
    private Object objects = SetFact.mOrderExclSet();

    public ImSet<ObjectEntity> getObjects() {
        return getOrderObjects().getSet();
    }
    @LongMutable
    public ImOrderSet<ObjectEntity> getOrderObjects() {
        if(!finalizedObjects) {
            finalizedObjects = true;
            objects = ((MOrderExclSet<ObjectEntity>)objects).immutableOrder();
        }

        return (ImOrderSet<ObjectEntity>)objects;
    }

    public void add(ObjectEntity objectEntity) {
        assert !finalizedObjects;
        objectEntity.groupTo = this;
        ((MOrderExclSet<ObjectEntity>)objects).exclAdd(objectEntity);
    }
    public void setObjects(ImOrderSet<ObjectEntity> objects) {
        assert !finalizedObjects;
        finalizedObjects = true;
        this.objects = objects;
    }
    public GroupObjectEntity(int ID, ImOrderSet<ObjectEntity> objects) {
        this(ID, (String)null);

        setObjects(objects);
    }
}
