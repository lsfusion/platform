package lsfusion.server.logics.form.stat.struct.imports.hierarchy;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.form.stat.struct.hierarchy.ChildParseNode;
import lsfusion.server.logics.form.stat.struct.hierarchy.Node;
import lsfusion.server.logics.form.stat.struct.hierarchy.PropertyParseNode;
import lsfusion.server.logics.form.stat.struct.imports.ImportIterator;

public class ImportHierarchicalIterator extends ImportIterator {

    public ImportHierarchicalIterator(String wheres) {
        super(wheres);
    }

    private int rowIndex;

    public <T extends Node<T>> boolean ignoreRow(ImOrderSet<ChildParseNode> children, T node) {
        boolean ignoreRow = false;
        try {
            for (Where where : wheresList) {

                boolean conditionResult;
                if(where.isRow()) {
                    conditionResult = ignoreRowIndexCondition(where.not, rowIndex, where.sign, where.value);
                } else {

                    boolean attr = false;
                    /*for (ChildParseNode child : children) {
                        if (child instanceof PropertyParseNode && child.getKey().equals(where.field)) {
                            attr = ((PropertyParseNode) child).isAttr();
                            break;
                        }
                    }*/

                    Object fieldValue = node.getValue(where.field, attr, StringClass.text);
                    if (fieldValue != null) {
                        conditionResult = ignoreRowCondition(where.not, String.valueOf(fieldValue), where.sign, where.value);
                    } else {
                        conditionResult = true;
                    }
                }

                ignoreRow = where.isAnd() ? (ignoreRow | conditionResult) : where.isOr() ? (ignoreRow & conditionResult) : conditionResult;
            }
            rowIndex++;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        return ignoreRow;
    }
}