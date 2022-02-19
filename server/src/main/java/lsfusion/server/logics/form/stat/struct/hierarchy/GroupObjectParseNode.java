package lsfusion.server.logics.form.stat.struct.hierarchy;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.form.stat.struct.export.hierarchy.json.FormPropertyDataInterface;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

import static org.apache.commons.compress.compressors.CompressorStreamFactory.Z;

public class GroupObjectParseNode extends GroupParseNode implements ChildParseNode {
    private final GroupObjectEntity group;

    public String getKey() {
        return group.getIntegrationSID();
    }

    public GroupObjectParseNode(ImOrderSet<ChildParseNode> children, GroupObjectEntity group) {
        super(children);
        this.group = group;
    }

    private ObjectEntity getSingleObject() {
        return group.getObjects().single();
    }

    private boolean isIndex() {
        return group.isIndex();
    }
    
    @Override
    public <T extends Node<T>> void importNode(T node, ImMap<ObjectEntity, Object> upValues, ImportData importData) {
        boolean isIndex = isIndex();
        ObjectEntity object = getSingleObject();

        for (Pair<Object, T> data : node.getMap(getKey(), isIndex)) {
            // getting object value
            Object objectValue;
            try {
                if (isIndex)
                    objectValue = importData.genObject(object);
                else
                    objectValue = ((DataClass) object.baseClass).parseString((String) data.first);
            } catch (SQLException | ParseException e) {
                throw Throwables.propagate(e);
            }

            ImMap<ObjectEntity, Object> newUpValues = upValues.addExcl(object, objectValue);

            importData.addObject(group, newUpValues, isIndex);
            importChildrenNodes(data.second, newUpValues, importData);
        }
    }

    @Override
    public <T extends Node<T>> boolean exportNode(T node, ImMap<ObjectEntity, Object> upValues, ExportData exportData) {
        boolean isIndex = isIndex();
        boolean upDown = node.isUpDown();

        MList<Pair<Object, T>> mMap = ListFact.mList();
        int i=0;
        ImList<ImMap<ObjectEntity, Object>> objects = exportData.getObjects(group, upValues);
        for (ImMap<ObjectEntity, Object> data : objects) {
            T newNode = node.createNode();
            if(!upDown)
                exportChildrenNodes(newNode, data, exportData);

            // getting object value
            Object objectValue;
            if (isIndex)
                objectValue = i++;
            else {
                ObjectEntity object = getSingleObject();
                objectValue = ((DataClass) object.baseClass).formatString(data.get(object));
            }

            mMap.add(new Pair<>(objectValue, newNode));
        }
        ImList<Pair<Object, T>> map = mMap.immutableList();
        boolean isNotEmpty = node.addMap(node, getKey(), isIndex, map);

        if(upDown) {
            for(int j=0,size=map.size();j<size;j++) {
                ImMap<ObjectEntity, Object> data = objects.get(j);
                exportChildrenNodes(map.get(j).second, data, exportData);
            }
        }
        
        return isNotEmpty;
    }

    @Override
    public <X extends PropertyInterface, P extends PropertyInterface> PropertyMapImplement<?, X> getJSONProperty(FormPropertyDataInterface<P> form, ImRevMap<P, X> mapValues, ImRevMap<ObjectEntity, X> mapObjects) {
        ImRevMap<ObjectEntity, PropertyInterface> mapGroupObjects = group.getObjects().mapRevValues(() -> new PropertyInterface());

        // we could generate new interfaces here, but not sure that it makes sense, so we'll use the existing ones
        ImSet<PropertyInterface> outerInterfaces = (ImSet<PropertyInterface>) mapValues.valuesSet().addExcl(mapObjects.valuesSet());

        ImRevMap<ObjectEntity, PropertyInterface> mapInnerObjects = MapFact.addRevExcl(mapObjects, mapGroupObjects);

        PropertyMapImplement<?, PropertyInterface> group = PropertyFact.createAnd(getChildrenJSONProperties(form, BaseUtils.immutableCast(mapValues), mapInnerObjects), form.getWhere(this.group, BaseUtils.immutableCast(mapValues), mapInnerObjects));

        ImOrderMap<PropertyInterfaceImplement<PropertyInterface>, Boolean> orders = form.getOrders(this.group, mapInnerObjects);

        ImSet<PropertyInterface> usedInnerInterfaces = PropertyFact.getUsedInterfaces(group).merge(PropertyFact.getUsedInterfaces(orders.keys()));

        // actually outerInterfaces are X interfaces, so in the end there will be only X interfaces
        return (PropertyMapImplement<?, X>) PropertyFact.createGProp(GroupType.JSON_CONCAT, usedInnerInterfaces, outerInterfaces.filter(usedInnerInterfaces), ListFact.singleton(group), orders, false);
    }
}
