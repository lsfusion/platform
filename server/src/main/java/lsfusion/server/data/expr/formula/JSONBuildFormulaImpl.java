package lsfusion.server.data.expr.formula;

import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.file.JSONClass;
import lsfusion.server.logics.classes.data.file.JSONTextClass;

import static lsfusion.base.BaseUtils.nullEquals;

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
        MList<JSONEntry> result = ListFact.mList();
        MList<String> currentGroup = ListFact.mList();
        String currentShowIfSource = null;
        Boolean currentStripNulls = null;

        for (int i = 0; i < fieldNames.size(); i++) {
            String value = fieldNames.getKey(i);
            String valueSource = source.getSource(i);
            Pair<Boolean, Boolean> options = fieldNames.getValue(i);
            if(options != null) { //options == null - showIf field, skip
                Boolean showIf = options.first;
                Boolean stripNulls = options.second;
                String showIfSource = showIf ? source.getSource(i + 1) : null;
                if(i > 0 && !nullEquals(currentStripNulls, stripNulls) || (showIfSource != null || currentShowIfSource != null)) {
                    result.add(new JSONEntry(currentGroup.immutableList(), currentShowIfSource, currentStripNulls));
                    currentGroup = ListFact.mList();

                }
                currentGroup.add("'" + value + "'," + valueSource);
                currentShowIfSource = showIfSource;
                currentStripNulls = stripNulls;
            }
        }

        if(currentGroup.size() > 0) //last group
            result.add(new JSONEntry(currentGroup.immutableList(), currentShowIfSource, currentStripNulls));

        return getJSON(result);
    }

    private String getJSON(MList<JSONEntry> list) {
        boolean convertToJsonb = list.size() > 1;
        String result = list.immutableList().toString(jsonEntry -> jsonEntry.getJSON(convertToJsonb), " || ");
        return "notEmpty(" + (returnString && convertToJsonb ? toJson(result) : result) + ")";
    }

    private String toJson(String value) {
        return returnString ? "(" + value + ")::json" : value;
    }

    private String toJsonb(String value) {
        return returnString ? "(" + value + ")::jsonb" : value;
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

    private class JSONEntry {
        private final ImList<String> sources;
        private final String showIfSource;
        private final Boolean stripNulls;

        public JSONEntry(ImList<String> sources, String showIfSource, Boolean stripNulls) {
            this.sources = sources;
            this.showIfSource = showIfSource;
            this.stripNulls = stripNulls;
        }

        public String getJSON(boolean convertToJsonb) {
            String jsonBuildObject = (returnString ? "json_build_object" : "jsonb_build_object") + "(" + sources.toString(",") + ")";
            String field = stripNulls != null && stripNulls ? jsonStripNulls(jsonBuildObject) : jsonBuildObject;
            String result = showIfSource != null ? ("CASE WHEN " + showIfSource + " IS NOT NULL THEN " + field +
                    " ELSE '{}'::" + (returnString ? "json" : "jsonb") + " END") : field;
            return convertToJsonb ? toJsonb(result) : result;
        }

        private String jsonStripNulls(String value) {
            return returnString ? ("json_strip_nulls(" + value + ")") : ("jsonb_strip_nulls(" + value + ")");
        }
    }
}
