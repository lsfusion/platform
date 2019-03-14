package lsfusion.server.logics.form.stat.integration.hierarchy;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.form.struct.group.AbstractGroup;
import lsfusion.server.logics.form.struct.object.ObjectEntity;

public class PropertyGroupParseNode extends GroupParseNode {
    private final AbstractGroup group;

    protected String getKey() {
        return group.getIntegrationSID();
    }

    public PropertyGroupParseNode(ImOrderSet<ParseNode> children, AbstractGroup group) {
        super(children);
        this.group = group;
    }

    @Override
    public <T extends Node<T>> void importNode(T node, ImMap<ObjectEntity, Object> upValues, ImportData importData) {
        T childNode = node.getNode(getKey());
        if(childNode != null)
            importChildrenNodes(childNode, upValues, importData);
    }

    @Override
    public <T extends Node<T>> void exportNode(T node, ImMap<ObjectEntity, Object> upValues, ExportData exportData) {
        T newNode = node.createNode();
        boolean upDown = node.isUpDown();
        if(!upDown)
            exportChildrenNodes(newNode, upValues, exportData);
        node.addNode(node, getKey(), newNode);
        if(upDown)
            exportChildrenNodes(newNode, upValues, exportData);
    }
}
