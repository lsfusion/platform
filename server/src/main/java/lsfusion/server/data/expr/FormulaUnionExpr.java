package lsfusion.server.data.expr;

import lsfusion.base.SFunctionSet;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.DataClass;
import lsfusion.server.data.expr.formula.*;
import lsfusion.server.data.expr.formula.conversion.CompatibleTypeConversion;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;

public class FormulaUnionExpr extends UnionExpr {

    private final ImList<Expr> exprs;
    private final FormulaUnionImpl formula;

    private FormulaUnionExpr(FormulaUnionImpl formula, ImList<Expr> exprs) {
        this.formula = formula;
        this.exprs = exprs;
    }

    private final static SFunctionSet<Expr> notIsNull = new SFunctionSet<Expr>() {
        public boolean contains(Expr element) {
            return !element.isNull();
        }};
    public static Expr create(final FormulaUnionImpl formula, ImList<Expr> exprs) {
        Expr resolve = resolveObjectType(formula, exprs, null);
        if(resolve != null)
            return resolve;

        if(formula.supportRemoveNull())
            exprs = exprs.filterList(notIsNull);

        return create(new FormulaUnionExpr(formula, exprs));
    }

    public DataClass getStaticClass() {
        return (DataClass) formula.getType(new SelfListExprType(exprs));
    }

    protected ImCol<Expr> getParams() {
        return exprs.getCol();
    }

    protected VariableSingleClassExpr translate(MapTranslate translator) {
        return new FormulaUnionExpr(formula, translator.translate(exprs));
    }

    public Expr translateQuery(QueryTranslator translator) {
        return create(formula, translator.translate(exprs));
    }

    public String getSource(final CompileSource compile) {
        return formula.getSource(new ListExprSource(exprs) {
            public CompileSource getCompileSource() {
                return compile;
            }});
    }

    protected int hash(HashContext hashContext) {
        return 31*formula.hashCode() * 31 + hashOuter(exprs, hashContext);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return formula.equals(((FormulaUnionExpr)o).formula) && exprs.equals(((FormulaUnionExpr)o).exprs);
    }

    public static Expr resolveObjectType(FormulaImpl impl, ImList<Expr> exprs, final KeyType keyType) {
        if(impl instanceof MaxFormulaImpl && !((MaxFormulaImpl)impl).notObjectType) {
            Type compatibleType = AbstractFormulaImpl.getCompatibleType(keyType==null ? new SelfListExprType(exprs): new ContextListExprType(exprs) {
                public KeyType getKeyType() {
                    return keyType;
                }
            }, CompatibleTypeConversion.instance);
            if(compatibleType == null)
                return null;

            boolean isMin = ((MaxFormulaImpl) impl).isMin;
            if(compatibleType instanceof DataClass)
                return create(new MaxFormulaImpl(isMin, true), exprs);
            else
                return exprs.get(0).calcCompareExpr(exprs.get(1), isMin);
        }
        return null;
    }

    public Expr packFollowFalse(final Where where) {
        Expr expr = resolveObjectType(formula, exprs, where.not());
        if(expr!=null)
            return expr.followFalse(where, true);

        return create(formula, exprs.mapListValues(new GetValue<Expr, Expr>() {
            public Expr getMapValue(Expr value) {
                return value.followFalse(where, true);
            }}));
    }

    @Override
    public Where calculateNotNullWhere() {
        return Where.TRUE;
    }
}
