package platform.server.data.expr.formula;

import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.classes.ConcreteClass;
import platform.server.data.expr.KeyType;
import platform.server.data.query.CompileSource;
import platform.server.data.type.Type;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static platform.base.BaseUtils.nullEquals;
import static platform.base.BaseUtils.nullHash;

public class CustomFormulaImpl extends AbstractFormulaImpl {

    public final String formula;

    public final Pattern paramsPattern;
    public ImMap<String, Integer> mapParams;

    public final ConcreteClass valueClass;

    public CustomFormulaImpl(String formula, ImMap<String, Integer> mapParams, ConcreteClass valueClass) {
        this.formula = formula;
        this.mapParams = mapParams;
        this.valueClass = valueClass;
        this.paramsPattern = Pattern.compile(mapParams.keys().toString("|"));
    }

    @Override
    public String getSource(final CompileSource compile, final ExprSource source) {
        ImMap<String, String> exprSource = mapParams.mapValues(new GetValue<String, Integer>() {
            @Override
            public String getMapValue(Integer exprInd) {
                return source.getSource(exprInd, compile);
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
    public ConcreteClass getStaticClass(ExprSource source) {
        return valueClass != null ? valueClass : super.getStaticClass(source);
    }

    @Override
    public Type getType(ExprSource source, KeyType keyType) {
        ConcreteClass staticClass = getStaticClass(source);
        if (staticClass == null) {
            return null;
        }
        return staticClass.getType();
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
