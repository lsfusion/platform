package platform.server.data.expr;

import platform.base.TwinImmutableObject;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ParamLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.IntegralClass;
import platform.server.data.query.CompileSource;
import platform.server.data.query.innerjoins.GroupJoinsWheres;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

// среднее что-то между CaseExpr и FormulaExpr - для того чтобы не плодить экспоненциальные case'ы
// придется делать BaseExpr
public class LinearExpr extends UnionExpr {

    final LinearOperandMap map;

    public LinearExpr(LinearOperandMap map) {
        this.map = map;
        assert (map.size()>0);
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return map.toString();
        else
            return map.getSource(compile);
    }

    @Override
    protected ImSet<Expr> getParams() {
        return map.keys();
    }

    @IdentityLazy
    public Where getCommonWhere() {
        return getWhere(getBaseJoin().getJoins());
    }
    
    public class NotNull extends NotNullExpr.NotNull {

        public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, KeyStat keyStat, ImOrderSet<Expr> orderTop, boolean noWhere) {
            return getCommonWhere().groupJoinsWheres(keepStat, keyStat, orderTop, noWhere).and(new GroupJoinsWheres(this, noWhere));
        }

        public ClassExprWhere calculateClassWhere() {
            return getCommonWhere().getClassWhere();
        }
    }

    public Where calculateNotNullWhere() {
        return new NotNull();
    }

    /*    @Override
    public boolean equals(Object obj) {
        if(map.size()==1) {
            Map.Entry<Expr, Integer> singleEntry = BaseUtils.singleEntry(map);
            if(singleEntry.getValue().equals(1)) return singleEntry.getKey().equals(obj);
        }
        return super.equals(obj);
    }*/

    public boolean twins(TwinImmutableObject obj) {
        return map.equals(((LinearExpr)obj).map);
    }

    protected int hash(HashContext hashContext) {
        return map.hashOuter(hashContext) * 5;
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        Expr result = null;
        for(int i=0,size=map.size();i<size;i++) {
            Expr transOperand = map.getKey(i).translateQuery(translator).scale(map.getValue(i));
            if(result==null)
                result = transOperand;
            else
                result = result.sum(transOperand);
        }
        return result;
    }

    // транслирует выражение/ также дополнительно вытаскивает ExprCase'ы
    protected LinearExpr translate(MapTranslate translator) {
        return new LinearExpr(map.translateOuter(translator));
    }

    @Override
    public Expr packFollowFalse(Where where) {
        return map.packFollowFalse(where);
    }

    @IdentityLazy
    public IntegralClass getStaticClass() {
        return map.getType();
    }
}
