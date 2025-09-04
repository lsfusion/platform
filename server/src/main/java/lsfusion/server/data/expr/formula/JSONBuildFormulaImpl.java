package lsfusion.server.data.expr.formula;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.file.JSONClass;
import lsfusion.server.logics.classes.data.file.JSONTextClass;

public class JSONBuildFormulaImpl extends AbstractFormulaImpl implements FormulaUnionImpl {

    private final ImOrderMap<String, Boolean> fieldNames;
    private final boolean returnString;

    public JSONBuildFormulaImpl(ImOrderMap<String, Boolean> fieldNames, boolean returnString) {
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
        MList<String> currentGroup = ListFact.mList();
        Boolean currentStrip = null;
        for (int i = 0; i < fieldNames.size(); i++) {
            String value = fieldNames.getKey(i);
            String valueSource = source.getSource(i);
            boolean strip = fieldNames.getValue(i);
            if (currentStrip != null && currentStrip != strip && currentGroup.size() > 0) {
                result.add(getFields(currentGroup, currentStrip));
                currentGroup = ListFact.mList();
            }
            currentGroup.add("'" + value + "'," + valueSource);
            currentStrip = strip;
        }

        if (currentGroup.size() > 0) //last group
            result.add(getFields(currentGroup, currentStrip));

        return "notEmpty(" + result.immutableList().toString(" || ") + ")";
    }

    private String getFields(MList<String> group, Boolean stripNull) {
        String fields = group.immutableList().toString(",");
        if (stripNull != null && stripNull)
            return returnString
                    ? "json_strip_nulls(json_build_object(" + fields + "))"
                    : "jsonb_strip_nulls(jsonb_build_object(" + fields + "))";
        else
            return returnString
                    ? "json_build_object(" + fields + ")"
                    : "jsonb_build_object(" + fields + ")";
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
