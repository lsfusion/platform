package lsfusion.server.logics.form.stat.struct.hierarchy;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;

import java.sql.SQLException;

public class GroupObjectParseNode extends GroupParseNode {
    private final GroupObjectEntity group;

    protected String getKey() {
        return group.getIntegrationSID();
    }

    public GroupObjectParseNode(ImOrderSet<ParseNode> children, GroupObjectEntity group) {
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

        int i=0;
        ImList<ImMap<ObjectEntity, Object>> objects = exportData.getObjects(group, upValues);
        MList<Pair<Object, T>> mMap = ListFact.mList(objects.size());
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
}
