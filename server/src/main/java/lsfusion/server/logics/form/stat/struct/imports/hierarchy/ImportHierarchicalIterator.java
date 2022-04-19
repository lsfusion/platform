package lsfusion.server.logics.form.stat.struct.imports.hierarchy;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.integral.NumericClass;
import lsfusion.server.logics.classes.data.time.DateClass;
import lsfusion.server.logics.classes.data.time.DateTimeClass;
import lsfusion.server.logics.classes.data.time.TimeClass;
import lsfusion.server.logics.form.stat.struct.hierarchy.ChildParseNode;
import lsfusion.server.logics.form.stat.struct.hierarchy.Node;
import lsfusion.server.logics.form.stat.struct.hierarchy.ParseNode;
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

                    PropertyParseNode parseNode = null;
                    Object fieldValue = null;
                    for (ParseNode child : children) {
                        if(child instanceof PropertyParseNode) {
                            if(((PropertyParseNode) child).getKey().equals(where.field)) {
                                parseNode = (PropertyParseNode) child;
                                fieldValue = parseNode.getValue(node);
                                break;
                            }
                        }
                    }
                    if(parseNode == null) {
                        throw Throwables.propagate(new RuntimeException(String.format("Incorrect WHERE in IMPORT: no such column '%s'", where.field)));
                    }

                    if (fieldValue != null) {
                        Type fieldType = parseNode.getType();
                        if (fieldType == DateClass.instance || fieldType == TimeClass.instance || fieldType == DateTimeClass.instance || fieldType instanceof NumericClass) {
                            conditionResult = ignoreRowCondition(where.not, fieldValue, where.sign, fieldType.parseString(where.value));
                        } else {
                            conditionResult = ignoreRowCondition(where.not, String.valueOf(fieldValue), where.sign, where.value);
                        }
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