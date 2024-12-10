package lsfusion.server.data.expr.formula;

import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.TextClass;
import lsfusion.server.logics.classes.data.file.AJSONClass;
import lsfusion.server.physics.admin.Settings;

public class StringConcatenateFormulaImpl extends AbstractFormulaImpl implements FormulaUnionImpl {
    protected final String separator;

    public StringConcatenateFormulaImpl(String separator) {
        this.separator = separator;
    }

    protected String getExprSource(ExprSource source, StringClass selfType, int i) {
        Type exprType = source.getType(i);
        String exprSource = source.getSource(i);
        return SumFormulaImpl.castToVarString(exprSource, selfType, exprType, source.getSyntax(), source.getMEnv());
    }

    @Override
    public Type getType(ExprType source) {

        //if separator == null, then it's first param
        boolean staticSeparator = separator != null;

        int separatorLength = staticSeparator ? separator.length() : source.getType(0) != null ? ((StringClass)source.getType(0)).length.getValue() : 0;

        ExtInt length = ExtInt.ZERO;
        boolean caseInsensitive = false;
        boolean blankPadded = true;
        boolean isText = false;
        String sid = null;
        for (int i = staticSeparator ? 0 : 1, size = source.getExprCount(); i < size; i++) {
            Type exprType = source.getType(i);

            length = length.sum(exprType != null ? exprType.getCharLength() : ExtInt.ZERO);
            if (exprType instanceof StringClass) {
                StringClass stringType = (StringClass) exprType;
                caseInsensitive = caseInsensitive || stringType.caseInsensitive;
                blankPadded = blankPadded && stringType.blankPadded;
                if (exprType instanceof TextClass) {
                    isText = true;
                    sid = exprType.getSID();
                }
            } else if (exprType instanceof AJSONClass) {
                isText = true;
                sid = TextClass.instance.getSID();
            }

            if (i > 0) {
                length = length.sum(new ExtInt(separatorLength));
            }
        }

        if(isText)
            return TextClass.getInstance(sid);
        return StringClass.get(blankPadded, caseInsensitive, length);
    }

    public boolean supportRemoveNull() {
        return true;
    }

    public boolean supportSingleSimplify() {
        return false; // because it will break implicit cast'ing (CONCAT '', f(), 5 will return 5 when f() is null)
    }

    public boolean supportNeedValue() {
        return true;
    }

    @Override
    public String getSource(ExprSource source) {
        int exprCount = source.getExprCount();
        if (exprCount == 0) {
            return "";
        }

        StringClass type = (StringClass) getType(source);
        SQLSyntax syntax = source.getSyntax();

        boolean staticSeparator = separator != null;

        String separatorSource = staticSeparator ? ("'" + separator + "'") : getExprSource(source, type, 0);

        String result = getExprSource(source, type, staticSeparator ? 0 : 1);

        for (int i = staticSeparator ? 1 : 2; i < exprCount; i++) {
            String exprSource = getExprSource(source, type, i);

            if (Settings.get().isUseSafeStringAgg()) {
                result = "CASE WHEN " + result + " IS NOT NULL" +
                        " THEN " + result + " " + syntax.getStringConcatenate() + " (CASE WHEN " + exprSource + " IS NOT NULL THEN " + separatorSource + " " + syntax.getStringConcatenate() + " " + exprSource + " ELSE '' END)" +
                        " ELSE " + exprSource + " END";
            } else {
                result = syntax.getStringCFunc() + "(" + result + "," + exprSource + "," + separatorSource + ")";
            }
        }
        return type.getCast("(" + result + ")", syntax, source.getMEnv());
    }
}
