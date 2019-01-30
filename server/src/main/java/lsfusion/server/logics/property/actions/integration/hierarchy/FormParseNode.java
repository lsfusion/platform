package lsfusion.server.logics.property.actions.integration.hierarchy;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.form.entity.ObjectEntity;

public class FormParseNode extends GroupParseNode {

    public FormParseNode(ImOrderSet<ParseNode> children) {
        super(children);
    }

    @Override
    public <T extends Node<T>> void importNode(T node, ImMap<ObjectEntity, Object> upValues, ImportData importData) {
        importChildrenNodes(node, upValues, importData);
    }

    @Override
    public <T extends Node<T>> void exportNode(T node, ImMap<ObjectEntity, Object> upValues, ExportData exportData) {
        exportChildrenNodes(node, upValues, exportData);
    }
}
