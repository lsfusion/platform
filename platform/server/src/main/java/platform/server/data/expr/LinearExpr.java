package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.server.caches.ParamLazy;
import platform.server.caches.HashContext;
import platform.server.classes.IntegralClass;
import platform.server.data.query.*;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.translator.TranslateExprLazy;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.type.Type;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;

import java.util.Map;

// среднее что-то между CaseExpr и FormulaExpr - для того чтобы не плодить экспоненциальные case'ы
// придется делать BaseExpr
@TranslateExprLazy
public class LinearExpr extends StaticClassExpr {

    final LinearOperandMap map;

    public LinearExpr(LinearOperandMap iMap) {
        map = iMap;
        // повырезаем все смежные getWhere с нулевыми коэффицентами
        assert (map.size()>0);
        assert !(map.size()==1 && BaseUtils.singleValue(map).equals(1));
    }

    public Type getType(Where where) {
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

    public void enumerate(SourceEnumerator enumerator) {
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
            Map.Entry<BaseExpr, Integer> singleEntry = BaseUtils.singleEntry(map);
            if(singleEntry.getValue().equals(1)) return singleEntry.getKey().equals(obj);
        }
        return super.equals(obj);
    }

    public boolean twins(AbstractSourceJoin obj) {
        return map.equals(((LinearExpr)obj).map);
    }

    public int hashContext(HashContext hashContext) {
        return map.hashContext(hashContext);
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        Expr result = null;
        for(Map.Entry<BaseExpr,Integer> operand : map.entrySet()) {
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
    public BaseExpr translateDirect(KeyTranslator translator) {
        LinearOperandMap transMap = new LinearOperandMap();
        for(Map.Entry<BaseExpr,Integer> operand : map.entrySet())
            transMap.put(operand.getKey().translateDirect(translator),operand.getValue());
        return new LinearExpr(transMap);
    }

    public BaseExpr packFollowFalse(Where where) {
        return map.packFollowFalse(where).getExpr();
    }

    public DataWhereSet getFollows() {
        DataWhereSet[] follows = new DataWhereSet[map.size()] ; int num = 0;
        for(BaseExpr expr : map.keySet())
            follows[num++] = expr.getFollows();
        return new DataWhereSet(follows);
    }

    public IntegralClass getStaticClass() {
        return map.getType();
    }
}