package lsfusion.server.logics.property.actions.integration.hierarchy;

import lsfusion.base.ExtInt;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.classes.StringClass;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;

public class GroupObjectParseNode extends GroupParseNode {
    private final GroupObjectEntity group;

    protected String getKey() {
        return group.getSID();
    }

    public GroupObjectParseNode(ImSet<ParseNode> children, GroupObjectEntity group) {
        super(children);
        this.group = group;
    }

    private ObjectEntity getSingleObject() {
        return group.getObjects().single();
    }

    private boolean isIndex() {
        if(group.getObjects().size() != 1)
            return true;

        ObjectEntity singleObject = getSingleObject();
        if(singleObject.noClasses())
            return true;

        return !singleObject.baseClass.isCompatibleParent(StringClass.get(ExtInt.UNLIMITED));
    }
    
    @Override
    public <T extends Node<T>> void importNode(T node, ImMap<ObjectEntity, Object> upValues, ImportData importData) {
        boolean isIndex = isIndex();

        ObjectEntity object = getSingleObject();
        Integer index = null;
        Integer count = null;
        if(isIndex) {
            index = importData.getIndex(object);
            count = 0;
        }
        for (Pair<Object, T> data : node.getMap(getKey(), isIndex)) {
            Object objectValue = data.first;
            if(isIndex) {
                objectValue = ((Integer)objectValue) + index;
                count++;
            }
            ImMap<ObjectEntity, Object> newUpValues = upValues.addExcl(object, objectValue);

            importData.addObject(group, newUpValues);
            importChildrenNodes(data.second, newUpValues, importData);
        }
        if(isIndex)
            importData.shiftIndex(object, count);
    }

    @Override
    public <T extends Node<T>> void exportNode(T node, ImMap<ObjectEntity, Object> upValues, ExportData exportData) {
        boolean isIndex = isIndex();

        MList<Pair<Object, T>> mMap = ListFact.mList();
        int i=0;
        for (ImMap<ObjectEntity, Object> data : exportData.getObjects(group, upValues)) {
            T newNode = node.createNode();
            exportChildrenNodes(newNode, data, exportData);
            
            mMap.add(new Pair<Object, T>(isIndex?i++:(String)data.get(getSingleObject()), newNode));
        }
        node.addMap(node, getKey(), isIndex, mMap.immutableList());
    }
}
