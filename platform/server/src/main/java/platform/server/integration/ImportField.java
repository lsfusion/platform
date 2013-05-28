package platform.server.integration;

import platform.base.col.interfaces.immutable.ImMap;
import platform.server.classes.DataClass;
import platform.server.data.expr.Expr;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.CalcProperty;
import platform.server.session.Modifier;
import platform.server.session.SinglePropertyTableUsage;

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
