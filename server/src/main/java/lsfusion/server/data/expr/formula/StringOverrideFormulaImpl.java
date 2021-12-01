package lsfusion.server.data.expr.formula;

import lsfusion.base.col.ListFact;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.TextClass;

public class StringOverrideFormulaImpl extends AbstractFormulaImpl implements FormulaUnionImpl {
    public final static StringOverrideFormulaImpl instance = new StringOverrideFormulaImpl();

    private StringOverrideFormulaImpl() {
        super(null);
    }

    public boolean supportRemoveNull() {
        return true;
    }

    public boolean supportNeedValue() {
        return true;
    }

    public boolean supportSingleSimplify() {
        return true;
    }

    public static String castToVarString(String source, StringClass resultType, Type operandType, SQLSyntax syntax, TypeEnvironment typeEnv) {
        if(!(operandType instanceof StringClass))
            source = resultType.toVar().getCast(source, syntax, typeEnv, operandType);
        return source;
    }

    protected String getExprSource(ExprSource source, StringClass selfType, int i) {
        Type exprType = source.getType(i);
        String exprSource = source.getSource(i);
        return castToVarString(exprSource, selfType, exprType, source.getSyntax(), source.getMEnv());
    }

    @Override
    public StringClass getType(ExprType source) {
        ExtInt length = ExtInt.ZERO;
        boolean caseInsensitive = false;
        boolean blankPadded = true;
        boolean isText = false;
        String sid = null;
        for (int i = 0, size = source.getExprCount(); i < size; i++) {
            Type exprType = source.getType(i);

            length = length.max(exprType != null ? exprType.getCharLength() : ExtInt.ZERO);
            if (exprType instanceof StringClass) {
                StringClass stringType = (StringClass) exprType;
                caseInsensitive = caseInsensitive || stringType.caseInsensitive;
                blankPadded = blankPadded && stringType.blankPadded;
                if(exprType instanceof TextClass) {
                    isText = true;
                    sid = exprType.getSID();
                }
            }
        }

        if(isText)
            return TextClass.getInstance(sid);
        return StringClass.get(blankPadded, caseInsensitive, length);
    }

    @Override
    public String getSource(ExprSource source) {
        int exprCount = source.getExprCount();
        assert exprCount > 0;
        if (exprCount == 0) {
            return "";
        }

        StringClass type = getType(source);
        SQLSyntax syntax = source.getSyntax();

        String string = syntax.isNULL(ListFact.toList(exprCount, value -> getExprSource(source, type, value)).toString(","), false);
        return type.getCast("(" + string + ")", syntax, source.getMEnv());
//        return result;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
