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

    public CustomFormulaImpl(String formula, ImMap<String, Integer> mapParams, FormulaClass valueClass) {
        this.formula = formula;
        this.mapParams = mapParams;
        this.valueClass = valueClass;
        this.paramsPattern = Pattern.compile(mapParams.keys().toString("|"));
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

        return result.toString();
    }

    @Override
    public Type getType(ExprType source) {
        return valueClass != null ? valueClass.getType() : super.getType(source);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * nullHash(valueClass) + mapParams.hashCode()) + formula.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return formula.equals(((CustomFormulaImpl) o).formula) && mapParams.equals(((CustomFormulaImpl) o).mapParams) && nullEquals(valueClass, ((CustomFormulaImpl) o).valueClass);
    }
}
