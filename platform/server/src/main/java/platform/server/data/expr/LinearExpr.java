package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.base.TwinImmutableInterface;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ParamLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.IntegralClass;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.query.CompileSource;
import platform.server.data.query.ExprEnumerator;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.translator.TranslateExprLazy;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

import java.util.Map;

// среднее что-то между CaseExpr и FormulaExpr - для того чтобы не плодить экспоненциальные case'ы
// придется делать BaseExpr
@TranslateExprLazy
public class LinearExpr extends StaticClassExpr {

    final LinearOperandMap map;

    public LinearExpr(LinearOperandMap map) {
        this.map = map;
        assert (map.size()>0);
        assert !(map.size()==1 && BaseUtils.singleValue(map).equals(1));
    }

    public Type getType(KeyType keyType) {
        return getStaticClass();
    }

    // возвращает Where на notNull
    public Where calculateWhere() {
        return map.getWhere();
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return map.toString();
        else
            return map.getSource(compile);
    }

    public void enumDepends(ExprEnumerator enumerator) {
        map.enumerate(enumerator);
    }

    @Override
    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        map.fillJoinWheres(joins, andWhere);
    }

    // мы и так перегрузили fillJoinWheres
    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
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

    @IdentityLazy
    public int hashOuter(HashContext hashContext) {
        return map.hashContext(hashContext) * 5;
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

    public VariableExprSet calculateExprFollows() {
        VariableExprSet[] follows = new VariableExprSet[map.size()] ; int num = 0;
        for(Expr expr : map.keySet())
            follows[num++] = expr.getExprFollows();
        return new VariableExprSet(follows);
    }

    @IdentityLazy
    public IntegralClass getStaticClass() {
        return map.getType();
    }

    public long calculateComplexity() {
        return map.getComplexity();
    }
}