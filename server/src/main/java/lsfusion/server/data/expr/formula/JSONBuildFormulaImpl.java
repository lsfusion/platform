package lsfusion.server.data.expr.formula;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.file.JSONClass;
import lsfusion.server.logics.classes.data.file.JSONTextClass;

import static lsfusion.base.BaseUtils.nullEquals;

public class JSONBuildFormulaImpl extends AbstractFormulaImpl implements FormulaUnionImpl {

    private final ImList<JSONField> fields;
    private final boolean returnString;

    public JSONBuildFormulaImpl(ImList<JSONField> fields, boolean returnString) {
        this.fields = fields;
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
        Object currentShowIf = null;

        int showIfIndex = fields.size();
        for (int i = 0; i < fields.size(); i++) {
            JSONField field = fields.get(i);
            String value = field.name;
            String valueSource = source.getSource(i);
            FieldShowIf fieldShowIf = field.showIf;
            Object showIf = fieldShowIf == FieldShowIf.SHOWIF ? source.getSource(showIfIndex++) : fieldShowIf == FieldShowIf.EXTNULL;
            if(i > 0 && !nullEquals(currentShowIf, showIf)) {
                result.add(new JSONEntry(currentGroup.immutableList(), currentShowIf));
                currentGroup = ListFact.mList();

            }
            currentGroup.add("'" + value + "'," + valueSource);
            currentShowIf = showIf;
        }

        if(currentGroup.size() > 0) //last group
            result.add(new JSONEntry(currentGroup.immutableList(), currentShowIf));

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
        return fields.hashCode() + (returnString ? 1 : 0);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof JSONBuildFormulaImpl && fields.equals(((JSONBuildFormulaImpl) o).fields) && returnString == ((JSONBuildFormulaImpl) o).returnString;
    }

    public Type getType(ExprType source) {
        return returnString ? JSONTextClass.instance : JSONClass.instance;
    }

    private class JSONEntry {
        private final ImList<String> sources;
        private final Object showIf;

        public JSONEntry(ImList<String> sources, Object showIf) {
            this.sources = sources;
            this.showIf = showIf;
        }

        public String getJSON(boolean convertToJsonb) {
            String showIfSource = showIf instanceof String ? (String) showIf : null;
            boolean extNull = showIf instanceof Boolean && (Boolean) showIf;

            String jsonBuildObject = (returnString ? "json_build_object" : "jsonb_build_object") + "(" + sources.toString(",") + ")";
            String field = showIfSource != null || extNull ? jsonBuildObject : jsonStripNulls(jsonBuildObject);
            String result = showIfSource != null ? ("CASE WHEN " + showIfSource + " IS NOT NULL THEN " + field +
                    " ELSE '{}'::" + (returnString ? "json" : "jsonb") + " END") : field;
            return convertToJsonb ? toJsonb(result) : result;
        }

        private String jsonStripNulls(String value) {
            return returnString ? ("json_strip_nulls(" + value + ")") : ("jsonb_strip_nulls(" + value + ")");
        }
    }
}
