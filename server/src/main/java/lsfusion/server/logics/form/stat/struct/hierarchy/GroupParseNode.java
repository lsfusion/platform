package lsfusion.server.logics.form.stat.struct.hierarchy;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MOrderSet;
import lsfusion.server.data.expr.formula.FieldShowIf;
import lsfusion.server.data.expr.formula.JSONBuildFormulaImpl;
import lsfusion.server.logics.form.stat.struct.export.hierarchy.json.FormPropertyDataInterface;
import lsfusion.server.logics.form.stat.struct.imports.hierarchy.ImportHierarchicalIterator;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyReaderEntity;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public abstract class GroupParseNode implements ParseNode {
    public final ImOrderSet<ChildParseNode> children;

    public GroupParseNode(ImOrderSet<ChildParseNode> children) {
        this.children = children;
    }

    protected <T extends Node<T>> void importChildrenNodes(T node, ImMap<ObjectEntity, Object> upValues, ImportData importData, ImportHierarchicalIterator iterator){
        for (ParseNode child : children) {
            child.importNode(node, upValues, importData, iterator);
        }
    }
    protected <T extends Node<T>> boolean exportChildrenNodes(T node, ImMap<ObjectEntity, Object> upValues, ExportData importData) {
        boolean hasNotEmptyChild = false;
        for(ParseNode child : children) {
            hasNotEmptyChild = child.exportNode(node, upValues, importData) || hasNotEmptyChild;
        }
        return hasNotEmptyChild;
    }

    public <X extends PropertyInterface, P extends PropertyInterface> PropertyMapImplement<?, X> getChildrenJSONProperties(FormPropertyDataInterface<P> form, ImRevMap<P, X> mapValues, ImRevMap<ObjectEntity, X> mapObjects, boolean convertValue, boolean returnString) {
        MList<String> fieldNames = ListFact.mList();
        MList<FieldShowIf> fieldShowIfs = ListFact.mList();
        MOrderSet<PropertyMapImplement<?, X>> childrenProps = SetFact.mOrderSet();

        for (ChildParseNode child : children) {
            fieldNames.add(child.getKey());
            fieldShowIfs.add(child.getFieldShowIf());
            childrenProps.add(child.getJSONProperty(form, mapValues, mapObjects, returnString));
            if (child instanceof PropertyParseNode) {
                PropertyReaderEntity property = ((PropertyParseNode) child).getProperty();
                if (property instanceof PropertyDrawEntity) {
                    PropertyReaderEntity showIfProp = ((PropertyDrawEntity<?>) property).getShowIfProp();
                    if(showIfProp != null) {
                        childrenProps.add(showIfProp.getReaderProperty().getImplement(mapObjects));
                    }
                }
            }
        }

        if(convertValue && children.size() == 1 && children.single().getKey().equals("value"))
            return childrenProps.immutableOrder().single();

        // json_build_object - getKey() + getProperty
        return PropertyFact.createFormulaUnion(new JSONBuildFormulaImpl(
                fieldNames.immutableList(), fieldShowIfs.immutableList(), returnString), childrenProps.immutableOrder());
    }
}
