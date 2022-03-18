package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.file.JSONClass;

import static lsfusion.base.BaseUtils.nullEquals;
import static lsfusion.base.BaseUtils.nullHash;

public class JSONBuildFormulaImpl extends AbstractFormulaImpl implements FormulaUnionImpl {

    private final ImList<String> fieldNames;

    public JSONBuildFormulaImpl(ImList<String> fieldNames) {
        this.fieldNames = fieldNames;
    }

    @Override
    public boolean supportRemoveNull() {
        return false;
    }

    @Override
    public boolean supportSingleSimplify() {
        return false;
    }

    @Override
    public boolean supportNeedValue() {
        return true;
    }

    @Override
    public String getSource(ExprSource source) {
        return "notEmpty(jsonb_strip_nulls(jsonb_build_object(" + fieldNames.toString((i, value) -> "'" + value + "'," + source.getSource(i), ",") + ")))";
    }

    @Override
    public int hashCode() {
        return fieldNames.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof JSONBuildFormulaImpl && fieldNames.equals(((JSONBuildFormulaImpl) o).fieldNames);
    }

    public Type getType(ExprType source) {
        return JSONClass.instance;
    }
}
