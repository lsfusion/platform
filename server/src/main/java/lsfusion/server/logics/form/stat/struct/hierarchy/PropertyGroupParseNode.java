package lsfusion.server.logics.form.stat.struct.hierarchy;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.form.stat.struct.export.hierarchy.json.FormPropertyDataInterface;
import lsfusion.server.logics.form.stat.struct.imports.hierarchy.ImportHierarchicalIterator;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class PropertyGroupParseNode extends GroupParseNode implements ChildParseNode {
    private final Group group;

    public String getKey() {
        return group.getIntegrationSID();
    }

    public PropertyGroupParseNode(ImOrderSet<ChildParseNode> children, Group group) {
        super(children);
        this.group = group;
    }

    @Override
    public <T extends Node<T>> void importNode(T node, ImMap<ObjectEntity, Object> upValues, ImportData importData, ImportHierarchicalIterator iterator) {
        T childNode = node.getNode(getKey());
        if(childNode != null)
            importChildrenNodes(childNode, upValues, importData, iterator);
    }

    @Override
    public <T extends Node<T>> boolean exportNode(T node, ImMap<ObjectEntity, Object> upValues, ExportData exportData) {
        T newNode = node.createNode();
        boolean hasNotEmptyChild = true;
        boolean upDown = node.isUpDown();
        if(!upDown)
            hasNotEmptyChild = exportChildrenNodes(newNode, upValues, exportData);
        if(hasNotEmptyChild)
            node.addNode(node, getKey(), newNode);
        if(upDown) {
            hasNotEmptyChild = exportChildrenNodes(newNode, upValues, exportData);
            if(!hasNotEmptyChild)
                node.removeNode(node, newNode);
        }
        return hasNotEmptyChild;
    }

    @Override
    public <X extends PropertyInterface, P extends PropertyInterface> PropertyMapImplement<?, X> getJSONProperty(FormPropertyDataInterface<P> form, ImRevMap<P, X> mapValues, ImRevMap<ObjectEntity, X> mapObjects) {
        return getChildrenJSONProperties(form, mapValues, mapObjects, false);
    }
}
