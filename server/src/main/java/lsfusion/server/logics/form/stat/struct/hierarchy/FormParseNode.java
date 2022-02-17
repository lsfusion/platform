package lsfusion.server.logics.form.stat.struct.hierarchy;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.form.stat.struct.export.hierarchy.json.FormPropertyDataInterface;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class FormParseNode extends GroupParseNode {

    public FormParseNode(ImOrderSet<ChildParseNode> children) {
        super(children);
    }

    @Override
    public <T extends Node<T>> void importNode(T node, ImMap<ObjectEntity, Object> upValues, ImportData importData) {
        importChildrenNodes(node, upValues, importData);
    }

    @Override
    public <T extends Node<T>> boolean exportNode(T node, ImMap<ObjectEntity, Object> upValues, ExportData exportData) {
        return exportChildrenNodes(node, upValues, exportData);
    }

    @Override
    public <X extends PropertyInterface, P extends PropertyInterface> PropertyMapImplement<?, X> getJSONProperty(FormPropertyDataInterface<P> form, ImRevMap<P, X> mapValues, ImRevMap<ObjectEntity, X> mapObjects) {
        return getChildrenJSONProperties(form, mapValues, mapObjects);
    }
}
