package lsfusion.server.data.expr.formula;

import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.file.JSONClass;
import lsfusion.server.logics.classes.data.file.JSONTextClass;

public class JSONBuildFormulaImpl extends AbstractFormulaImpl implements FormulaUnionImpl {

    private final ImOrderMap<String, Pair<Boolean, Boolean>> fieldNames;
    private final boolean returnString;

    public JSONBuildFormulaImpl(ImOrderMap<String, Pair<Boolean, Boolean>> fieldNames, boolean returnString) {
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
        MList<String> result = ListFact.mList();
        for (int i = 0; i < fieldNames.size(); i++) {
            String value = fieldNames.getKey(i);
            String valueSource = source.getSource(i);
            Pair<Boolean, Boolean> options = fieldNames.getValue(i);
            if(options != null) { //options == null - showIf field, skip
                Boolean showIf = options.first;
                Boolean stripNulls = options.second;
                String showIfSource = showIf ? source.getSource(i + 1) : null;
                result.add(getField("'" + value + "'," + valueSource, showIfSource, stripNulls));
            }
        }

        return "notEmpty(" + result.immutableList().toString(" || ") + ")";
    }

    private String getField(String source, String showIfSource, Boolean stripNulls) {
        String jsonBuildObject = jsonBuildObject(source);
        String field = stripNulls != null && stripNulls ? jsonStripNulls(jsonBuildObject) : jsonBuildObject;
        return showIfSource != null ? ("CASE WHEN " + showIfSource + " IS NOT NULL THEN " + field + " ELSE " + empty() + " END") : field;
    }

    private String jsonBuildObject(String value) {
        return (returnString ? "json_build_object" : "jsonb_build_object") + "(" + value + ")";
    }

    private String jsonStripNulls(String value) {
        return (returnString ? "json_strip_nulls" : "jsonb_strip_nulls") + "(" + value + ")";
    }

    private String empty() {
        return (returnString ? "'{}'::json" : "'{}'::jsonb");
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
        return returnString ? JSONTextClass.instance : JSONClass.instance;
    }
}
