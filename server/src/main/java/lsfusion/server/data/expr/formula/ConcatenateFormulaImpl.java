package lsfusion.server.data.expr.formula;

import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.TextClass;
import lsfusion.server.logics.classes.data.file.JSONClass;
import lsfusion.server.physics.admin.Settings;

public class ConcatenateFormulaImpl extends AbstractFormulaImpl implements FormulaUnionImpl {
    protected final String separator;

    public ConcatenateFormulaImpl(String separator) {
        this.separator = separator;
    }

    protected String getExprSource(ExprSource source, StringClass selfType, int i) {
        Type exprType = source.getType(i);
        String exprSource = source.getSource(i);
        return SumFormulaImpl.castToVarString(exprSource, selfType, exprType, source.getSyntax(), source.getMEnv());
    }

    @Override
    public Type getType(ExprType source) {
        if(separator != null) {
            int separatorLength = separator.length();

            ExtInt length = ExtInt.ZERO;
            boolean caseInsensitive = false;
            boolean blankPadded = true;
            boolean isText = false;
            String sid = null;
            for (int i = 0, size = source.getExprCount(); i < size; i++) {
                Type exprType = source.getType(i);

                length = length.sum(exprType != null ? exprType.getCharLength() : ExtInt.ZERO);
                if (exprType instanceof StringClass) {
                    StringClass stringType = (StringClass) exprType;
                    caseInsensitive = caseInsensitive || stringType.caseInsensitive;
                    blankPadded = blankPadded && stringType.blankPadded;
                    if(exprType instanceof TextClass) {
                        isText = true;
                        sid = exprType.getSID();
                    }
                }

                if (i > 0) {
                    length = length.sum(new ExtInt(separatorLength));
                }
            }

            if(isText)
                return TextClass.getInstance(sid);
            return StringClass.get(blankPadded, caseInsensitive, length);
        } else {
            return super.getType(source);
        }
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
        if (separator != null) {
            if (exprCount == 0) {
                return "";
            }

            StringClass type = (StringClass) getType(source);
            SQLSyntax syntax = source.getSyntax();

            String result = getExprSource(source, type, 0);

            for (int i = 1; i < exprCount; i++) {
                String exprSource = getExprSource(source, type, i);

                if (Settings.get().isUseSafeStringAgg()) {
                    result = "CASE WHEN " + result + " IS NOT NULL" +
                            " THEN " + result + " " + syntax.getStringConcatenate() + " (CASE WHEN " + exprSource + " IS NOT NULL THEN '" + separator + "' " + syntax.getStringConcatenate() + " " + exprSource + " ELSE '' END)" +
                            " ELSE " + exprSource + " END";
                } else {
                    result = syntax.getStringCFunc() + "(" + result + "," + exprSource + ",'" + separator + "')";
                }
            }
            return type.getCast("(" + result + ")", syntax, source.getMEnv());
        } else {
            if (exprCount <= 1) {
                return "";
            }
            String result = source.getSource(0);
            for (int i = 1; i < exprCount; i++) {
                result = "jsonb_recursive_merge(" + result + "::jsonb," + source.getSource(i) + "::jsonb)";
            }
            return JSONClass.instance.getCast("(" + result + ")", source.getSyntax(), source.getMEnv());
        }
    }
}
