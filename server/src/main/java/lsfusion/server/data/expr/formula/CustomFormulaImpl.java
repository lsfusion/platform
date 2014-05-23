package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.type.Type;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lsfusion.base.BaseUtils.nullEquals;
import static lsfusion.base.BaseUtils.nullHash;

public class CustomFormulaImpl extends AbstractFormulaImpl implements FormulaJoinImpl {

    public final String formula;

    public final Pattern paramsPattern;
    public ImMap<String, Integer> mapParams;

    public final FormulaClass valueClass;
    
    private final boolean hasNotNull;

    public boolean hasNotNull() {
        return hasNotNull;
    }

    public CustomFormulaImpl(String formula, ImMap<String, Integer> mapParams, FormulaClass valueClass, boolean hasNotNull) {
        this.formula = formula;
        this.mapParams = mapParams;
        this.valueClass = valueClass;
        this.paramsPattern = Pattern.compile(mapParams.keys().toString("|"));
        this.hasNotNull = hasNotNull;
    }

    @Override
    public String getSource(final ExprSource source) {
        ImMap<String, String> exprSource = mapParams.mapValues(new GetValue<String, Integer>() {
            @Override
            public String getMapValue(Integer exprInd) {
                return source.getSource(exprInd);
            }
        });

        Matcher m = paramsPattern.matcher(formula);
        StringBuffer result = new StringBuffer("(");
        while (m.find()) {
            String param = m.group();
            m.appendReplacement(result, Matcher.quoteReplacement(exprSource.get(param)));
        }
        m.appendTail(result);
        result.append(")");

        String sourceString = result.toString();
        sourceString = sourceString.replace("||", source.getSyntax().getStringConcatenate()); // используется в BaseLogicsModule.toDateTime, StringAggUnionProperty тоже
        return "("+sourceString+")"; // type.getCast(sourceString, compile.syntax, false)
    }

    @Override
    public Type getType(ExprType source) {
        return valueClass != null ? valueClass.getType() : super.getType(source);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * nullHash(valueClass) + mapParams.hashCode()) + formula.hashCode() + (hasNotNull?1:0);
    }

    @Override
    public boolean equals(Object o) {
        return formula.equals(((CustomFormulaImpl) o).formula) && mapParams.equals(((CustomFormulaImpl) o).mapParams) && nullEquals(valueClass, ((CustomFormulaImpl) o).valueClass) && hasNotNull==((CustomFormulaImpl) o).hasNotNull;
    }
}
