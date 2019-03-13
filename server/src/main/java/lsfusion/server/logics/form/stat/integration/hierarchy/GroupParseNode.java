package lsfusion.server.logics.form.stat.integration.hierarchy;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.form.struct.object.ObjectEntity;

public abstract class GroupParseNode extends ParseNode {
    public final ImOrderSet<ParseNode> children;

    public GroupParseNode(ImOrderSet<ParseNode> children) {
        this.children = children;
    }
    
    protected <T extends Node<T>> void importChildrenNodes(T node, ImMap<ObjectEntity, Object> upValues, ImportData importData){
        for(ParseNode child : children) {
            child.importNode(node, upValues, importData);
        }
    }
    protected <T extends Node<T>> void exportChildrenNodes(T node, ImMap<ObjectEntity, Object> upValues, ExportData importData) {
        for(ParseNode child : children) {
            child.exportNode(node, upValues, importData);
        }
    }
}
