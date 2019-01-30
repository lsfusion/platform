package lsfusion.server.logics.property.actions.integration.hierarchy;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.classes.DataClass;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;

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
    public <T extends Node<T>> void exportNode(T node, ImMap<ObjectEntity, Object> upValues, ExportData exportData) {
        boolean isIndex = isIndex();

        MList<Pair<Object, T>> mMap = ListFact.mList();
        int i=0;
        for (ImMap<ObjectEntity, Object> data : exportData.getObjects(group, upValues)) {
            T newNode = node.createNode();
            exportChildrenNodes(newNode, data, exportData);

            // getting object value
            Object objectValue;
            if (isIndex)
                objectValue = i++;
            else {
                ObjectEntity object = getSingleObject();
                objectValue = ((DataClass) object.baseClass).formatString(data.get(object));
            }

            mMap.add(new Pair<Object, T>(objectValue, newNode));
        }
        node.addMap(node, getKey(), isIndex, mMap.immutableList());
    }
}
