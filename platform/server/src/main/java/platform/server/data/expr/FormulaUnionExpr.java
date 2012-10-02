package platform.server.data.expr;

import platform.base.TwinImmutableInterface;
import platform.server.caches.hash.HashContext;
import platform.server.classes.DataClass;
import platform.server.data.query.CompileSource;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.Where;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FormulaUnionExpr extends UnionExpr {

    private final String formula;
    private final DataClass dataClass;
    private final Map<String, Expr> params;

    public FormulaUnionExpr(String formula, DataClass dataClass, Map<String, Expr> params) {
        this.formula = formula;
        this.dataClass = dataClass;
        this.params = params;
    }

    public DataClass getStaticClass() {
        return dataClass;
    }

    protected Set<Expr> getParams() {
        return new HashSet<Expr>(params.values());
    }

    protected VariableClassExpr translate(MapTranslate translator) {
        return new FormulaUnionExpr(formula, dataClass, translator.translate(params));
    }

    public Expr translateQuery(QueryTranslator translator) {
        return new FormulaUnionExpr(formula, dataClass, translator.translate(params));
    }

    public String getSource(CompileSource compile) {
        return FormulaExpr.getSource(formula, params, dataClass, compile);
    }

    protected int hash(HashContext hashContext) {
        return (formula.hashCode() * 31 + dataClass.hashCode()) * 31 + hashOuter(params, hashContext);
    }

    public boolean twins(TwinImmutableInterface o) {
        return formula.equals(((FormulaUnionExpr)o).formula) && dataClass.equals(((FormulaUnionExpr)o).dataClass) && params.equals(((FormulaUnionExpr)o).params);
    }
}
