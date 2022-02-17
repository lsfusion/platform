package lsfusion.server.logics.form.stat.struct.hierarchy;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.form.stat.struct.export.hierarchy.json.FormPropertyDataInterface;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public abstract class GroupParseNode implements ParseNode {
    public final ImOrderSet<ChildParseNode> children;

    public GroupParseNode(ImOrderSet<ChildParseNode> children) {
        this.children = children;
    }
    
    protected <T extends Node<T>> void importChildrenNodes(T node, ImMap<ObjectEntity, Object> upValues, ImportData importData){
        for(ParseNode child : children) {
            child.importNode(node, upValues, importData);
        }
    }
    protected <T extends Node<T>> boolean exportChildrenNodes(T node, ImMap<ObjectEntity, Object> upValues, ExportData importData) {
        boolean hasNotEmptyChild = false;
        for(ParseNode child : children) {
            hasNotEmptyChild = child.exportNode(node, upValues, importData) || hasNotEmptyChild;
        }
        return hasNotEmptyChild;
    }

    public <X extends PropertyInterface, P extends PropertyInterface> PropertyMapImplement<?, X> getChildrenJSONProperties(FormPropertyDataInterface<P> form, ImRevMap<P, X> mapValues, ImRevMap<ObjectEntity, X> mapObjects) {
        // json_build_object по getKey() + getProperty
        for(ChildParseNode child : children) {
            child.getKey() + child.getJSONProperty(form, mapValues, mapObjects);dsds
        }


        return null;
    }
}
