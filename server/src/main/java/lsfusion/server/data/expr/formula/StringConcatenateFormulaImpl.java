package lsfusion.server.data.expr.formula;

import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.TextClass;
import lsfusion.server.physics.admin.Settings;

public class StringConcatenateFormulaImpl extends AbstractFormulaImpl implements FormulaUnionImpl {

    public StringConcatenateFormulaImpl() {
    }

    protected String getExprSource(ExprSource source, StringClass selfType, int i) {
        Type exprType = source.getType(i);
        String exprSource = source.getSource(i);
        return SumFormulaImpl.castToVarString(exprSource, selfType, exprType, source.getSyntax(), source.getMEnv());
    }

    @Override
    public StringClass getType(ExprType source) {
        ExtInt length = ExtInt.ZERO;
        boolean caseInsensitive = false;
        boolean blankPadded = true;
        boolean isText = false;
        String sid = null;

        Type separatorType = source.getType(0);

        for (int i = 1, size = source.getExprCount(); i < size; i++) {
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

            if (i > 1) {
                length = length.sum(separatorType.getCharLength());
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
        if (exprCount < 2) {
            return "";
        }

        StringClass type = getType(source);
        SQLSyntax syntax = source.getSyntax();

        String separator = getExprSource(source, type, 0);
        String result = getExprSource(source, type, 1);
        for (int i = 2; i < exprCount; i++) {
            String exprSource = getExprSource(source, type, i);

            if(Settings.get().isUseSafeStringAgg()) {
                result = "CASE WHEN " + result + " IS NOT NULL" +
                        " THEN " + result + " " + syntax.getStringConcatenate() + " (CASE WHEN " + exprSource + " IS NOT NULL THEN " + separator + " " + syntax.getStringConcatenate() + " " + exprSource + " ELSE '' END)" +
                        " ELSE " + exprSource + " END";
            } else {
                result = syntax.getStringCFunc() + "(" + result + "," + exprSource + "," + separator + ")";
            }
        }
        return type.getCast("(" + result + ")", syntax, source.getMEnv());
    }
}
