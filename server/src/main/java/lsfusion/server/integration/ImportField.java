package lsfusion.server.integration;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.DataClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.session.Modifier;
import lsfusion.server.session.SinglePropertyTableUsage;

/**
 * User: DAle
 * Date: 06.12.2010
 * Time: 14:27:14
 */

public class ImportField implements ImportFieldInterface, ImportKeyInterface {
    private DataClass fieldClass;

    public static final Type.Getter<ImportField> typeGetter = new Type.Getter<ImportField>() {
        public Type getType(ImportField key) {
            return key.getFieldClass();
        }};

    public ImportField(DataClass fieldClass) {
        this.fieldClass = fieldClass;
    }

    public ImportField(LCP property) {
        this((CalcProperty<?>) property.property);
    }

    public ImportField(CalcProperty<?> property) {
        this.fieldClass = (DataClass) property.getValueClass();
    }

    public DataClass getFieldClass() {
        return fieldClass;
    }

    public DataObject getDataObject(ImportTable.Row row) {
        if (row.getValue(this) != null) {
            return new DataObject(row.getValue(this), getFieldClass());
        } else {
            return null;
        }
    }

    public Expr getExpr(ImMap<ImportField, ? extends Expr> importKeys) {
        return importKeys.get(this);
    }

    public Expr getExpr(ImMap<ImportField, ? extends Expr> importKeys, ImMap<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys, Modifier modifier) {
        return getExpr(importKeys);
    }

    public Type getType() {
        return fieldClass;
    }
}
