package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lsfusion.base.BaseUtils.nullEquals;
import static lsfusion.base.BaseUtils.nullHash;

public class CustomFormulaImpl extends AbstractFormulaImpl implements FormulaJoinImpl {

    public final CustomFormulaSyntax formula;

    public final Pattern paramsPattern;
    public ImMap<String, Integer> mapParams;

    public final FormulaClass valueClass;
    
    private final boolean hasNotNull;

    public boolean hasNotNull() {
        return hasNotNull;
    }

    public CustomFormulaImpl(CustomFormulaSyntax formula, ImMap<String, Integer> mapParams, FormulaClass valueClass, boolean hasNotNull) {
        this.formula = formula;
        this.mapParams = mapParams;
        this.valueClass = valueClass;
        
        String patternString = mapParams.keys().toString("|");
        if (!patternString.isEmpty()) {
            // word boundaries added to be able to match prm10+
            patternString = "\\b(" + patternString + ")\\b"; 
        }
        this.paramsPattern = Pattern.compile(patternString);
        
        this.hasNotNull = hasNotNull;
    }

    @Override
    public String getSource(final ExprSource source) {
        ImMap<String, String> exprSource = mapParams.mapValues(source::getSource);

        SQLSyntax syntax = source.getSyntax();
        Matcher m = paramsPattern.matcher(formula.getFormula(syntax.getSyntaxType()));
        StringBuffer result = new StringBuffer("(");
        if (!paramsPattern.pattern().isEmpty()) {
            while (m.find()) {
                String param = m.group();
                m.appendReplacement(result, Matcher.quoteReplacement(exprSource.get(param)));
            }
        }
        m.appendTail(result);
        result.append(")");

        return "("+ result.toString() +")"; // type.getCast(sourceString, compile.syntax, false)
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
