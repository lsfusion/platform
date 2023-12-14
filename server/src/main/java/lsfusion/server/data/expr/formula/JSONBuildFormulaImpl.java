package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.file.JSONClass;
import lsfusion.server.logics.classes.data.file.JSONStringClass;

public class JSONBuildFormulaImpl extends AbstractFormulaImpl implements FormulaUnionImpl {

    private final ImList<String> fieldNames;
    private final boolean returnString;

    public JSONBuildFormulaImpl(ImList<String> fieldNames, boolean returnString) {
        this.fieldNames = fieldNames;
        this.returnString = returnString;
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
        String fields = fieldNames.toString((i, value) -> {
            String valueSource = source.getSource(i);
            Type type = source.getType(i);
            if(type != null)
                valueSource = type.formatJSONSource(valueSource, source.getSyntax());
            return "'" + value + "'," + valueSource;
        }, ",");

        return returnString ? "notEmpty(json_strip_nulls( json_build_object(" + fields + ")))" :
                "notEmpty(jsonb_strip_nulls( jsonb_build_object(" + fields + ")))";
    }

    @Override
    public int hashCode() {
        return fieldNames.hashCode() + (returnString ? 1 : 0);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof JSONBuildFormulaImpl && fieldNames.equals(((JSONBuildFormulaImpl) o).fieldNames) && returnString == ((JSONBuildFormulaImpl) o).returnString;
    }

    public Type getType(ExprType source) {
        return returnString ? JSONStringClass.instance : JSONClass.instance;
    }
}
