package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lsfusion.base.BaseUtils.nullEquals;
import static lsfusion.base.BaseUtils.nullHash;

public class CustomFormulaImpl extends AbstractFormulaImpl {

    public final CustomFormulaSyntax formula;

    public ImRevMap<String, Integer> mapParams;

    public final FormulaClass valueClass;
    
    public CustomFormulaImpl(CustomFormulaSyntax formula, ImRevMap<String, Integer> mapParams, FormulaClass valueClass) {
        this.formula = formula;
        this.mapParams = mapParams;
        this.valueClass = valueClass;
    }

    @Override
    public String getSource(final ExprSource source) {
        return "("+ formula.getSource(source.getSyntax(), mapParams.mapValues(source::getSource)) +")"; // type.getCast(sourceString, compile.syntax, false)
    }

    @Override
    public Type getType(ExprType source) {
        return valueClass != null ? valueClass.getType() : super.getType(source);
    }

    @Override
    public int hashCode() {
        return (31 * nullHash(valueClass) + mapParams.hashCode()) + formula.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof CustomFormulaImpl && formula.equals(((CustomFormulaImpl) o).formula) && mapParams.equals(((CustomFormulaImpl) o).mapParams) && nullEquals(valueClass, ((CustomFormulaImpl) o).valueClass);
    }
}
