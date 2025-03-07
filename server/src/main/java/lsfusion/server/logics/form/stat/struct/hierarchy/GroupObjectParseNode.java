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
import lsfusion.server.logics.form.stat.SelectTop;
import lsfusion.server.logics.form.stat.struct.export.hierarchy.json.FormPropertyDataInterface;
import lsfusion.server.logics.form.stat.struct.imports.hierarchy.ImportHierarchicalIterator;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

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
    public <T extends Node<T>> void importNode(T node, ImMap<ObjectEntity, Object> upValues, ImportData importData, ImportHierarchicalIterator iterator) {
        boolean isIndex = isIndex();
        ObjectEntity object = getSingleObject();

        for (Pair<Object, T> data : node.getMap(getKey(), isIndex)) {
            if(iterator == null || !iterator.ignoreRow(children, data.second)) {
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
                importChildrenNodes(data.second, newUpValues, importData, null);
            }
        }
    }

    @Override
    public <T extends Node<T>> boolean exportNode(T node, ImMap<ObjectEntity, Object> upValues, ExportData exportData) {
        boolean isIndex = isIndex();
        boolean upDown = node.isUpDown();

        int i=0;
        ImList<ImMap<ObjectEntity, Object>> objects = exportData.getObjects(group, upValues);
        MList<Pair<Pair<Object, DataClass>, T>> mMap = ListFact.mList(objects.size());
        for (ImMap<ObjectEntity, Object> data : objects) {
            T newNode = node.createNode();
            if(!upDown)
                exportChildrenNodes(newNode, data, exportData);

            // getting object value
            Object objectValue;
            DataClass objectClass;
            if (isIndex) {
                objectValue = i++;
                objectClass = null;
            } else {
                ObjectEntity object = getSingleObject();
                objectValue = data.get(object);
                objectClass = (DataClass) object.baseClass;
            }

            mMap.add(new Pair<>(new Pair<>(objectValue, objectClass), newNode));
        }
        ImList<Pair<Pair<Object, DataClass>, T>> map = mMap.immutableList();
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
    public <X extends PropertyInterface, P extends PropertyInterface> PropertyMapImplement<?, X> getJSONProperty(FormPropertyDataInterface<P> form, ImRevMap<P, X> mapValues, ImRevMap<ObjectEntity, X> mapObjects, boolean returnString) {

        // we could generate new interfaces here, but not sure that it makes sense, so we'll use the existing ones
        ImSet<PropertyInterface> outerInterfaces = (ImSet<PropertyInterface>) mapValues.valuesSet().addExcl(mapObjects.valuesSet());
        ImRevMap<P, PropertyInterface> outerValues = BaseUtils.immutableCast(mapValues);

        ImRevMap<ObjectEntity, PropertyInterface> mapInnerObjects = MapFact.addRevExcl(mapObjects, group.getObjects().mapRevValues(() -> new PropertyInterface()));

        PropertyMapImplement<?, PropertyInterface> group = PropertyFact.createAnd(getChildrenJSONProperties(form, outerValues, mapInnerObjects, true, returnString), form.getWhere(this.group, outerValues, mapInnerObjects));

        ImOrderMap<PropertyInterfaceImplement<PropertyInterface>, Boolean> orders = form.getOrders(this.group, mapInnerObjects);

        ImSet<PropertyInterface> usedInnerInterfaces = PropertyFact.getUsedInterfaces(group).merge(PropertyFact.getUsedInterfaces(orders.keys()));

        SelectTop<PropertyInterface> selectTop = form.getSelectTop(this.group, outerValues);
        if(!selectTop.isEmpty()) {
            ImSet<PropertyInterface> selectTopParams = selectTop.getParamsSet();
            usedInnerInterfaces = usedInnerInterfaces.addExcl(selectTopParams);
            assert outerInterfaces.containsAll(selectTopParams);
        }

        // actually outerInterfaces are X interfaces, so in the end there will be only X interfaces

        return (PropertyMapImplement<?, X>) PropertyFact.createGProp(GroupType.JSON_CONCAT, usedInnerInterfaces, outerInterfaces.filter(usedInnerInterfaces), ListFact.singleton(group), orders, false, selectTop);
    }
}
