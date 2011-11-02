package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.base.TwinImmutableInterface;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ParamLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.IntegralClass;
import platform.server.data.translator.*;
import platform.server.data.query.CompileSource;
import platform.server.data.where.Where;

import java.util.*;

// среднее что-то между CaseExpr и FormulaExpr - для того чтобы не плодить экспоненциальные case'ы
// придется делать BaseExpr
@TranslateExprLazy
public class LinearExpr extends UnionExpr {

    final LinearOperandMap map;

    public LinearExpr(LinearOperandMap map) {
        this.map = map;
        assert (map.size()>0);
        assert !(map.size()==1 && BaseUtils.singleValue(map).equals(1));
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return map.toString();
        else
            return map.getSource(compile);
    }

    @Override
    protected Set<Expr> getParams() {
        return map.keySet();
    }

    @Override
    public boolean equals(Object obj) {
        if(map.size()==1) {
            Map.Entry<Expr, Integer> singleEntry = BaseUtils.singleEntry(map);
            if(singleEntry.getValue().equals(1)) return singleEntry.getKey().equals(obj);
        }
        return super.equals(obj);
    }

    public boolean twins(TwinImmutableInterface obj) {
        return map.equals(((LinearExpr)obj).map);
    }

    @HashOuterLazy
    public int hashOuter(HashContext hashContext) {
        return map.hashOuter(hashContext) * 5;
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        Expr result = null;
        for(Map.Entry<Expr, Integer> operand : map.entrySet()) {
            Expr transOperand = operand.getKey().translateQuery(translator).scale(operand.getValue());
            if(result==null)
                result = transOperand;
            else
                result = result.sum(transOperand);
        }
        return result;
    }

    // транслирует выражение/ также дополнительно вытаскивает ExprCase'ы
    @ParamLazy
    public BaseExpr translateOuter(MapTranslate translator) {
        LinearOperandMap transMap = new LinearOperandMap();
        for(Map.Entry<Expr, Integer> operand : map.entrySet())
            transMap.put(operand.getKey().translateOuter(translator),operand.getValue());
        return new LinearExpr(transMap);
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
