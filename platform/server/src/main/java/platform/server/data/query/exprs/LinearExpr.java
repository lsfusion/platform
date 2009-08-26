package platform.server.data.query.exprs;

import platform.server.data.classes.IntegralClass;
import platform.server.data.query.*;
import platform.server.data.query.translators.QueryTranslator;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.types.Type;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;
import platform.server.caches.ParamLazy;
import platform.base.BaseUtils;

import java.util.Map;

// среднее что-то между CaseExpr и FormulaExpr - для того чтобы не плодить экспоненциальные case'ы
// придется делать AndExpr
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
    protected Where calculateWhere() {
        return map.getWhere();
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return map.toString();
        else
            return map.getSource(compile);
    }

    public void fillContext(Context context) {
        map.fillContext(context);
    }

    @Override
    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        map.fillJoinWheres(joins, andWhere);
    }

    // мы и так перегрузили fillJoinWheres
    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    public boolean equals(Object obj) {
        if(map.size()==1) {
            Map.Entry<AndExpr, Integer> singleEntry = BaseUtils.singleEntry(map);
            if(singleEntry.getValue().equals(1)) return singleEntry.getKey().equals(obj);
        }
        return this==obj || obj instanceof LinearExpr && map.equals(((LinearExpr)obj).map);
    }

    public int hashContext(HashContext hashContext) {
        return map.hashContext(hashContext);
    }

    @ParamLazy
    public SourceExpr translateQuery(QueryTranslator translator) {
        SourceExpr result = null;
        for(Map.Entry<AndExpr,Integer> operand : map.entrySet()) {
            SourceExpr transOperand = operand.getKey().translate(translator).scale(operand.getValue());
            if(result==null)
                result = transOperand;
            else
                result = result.sum(transOperand);
        }
        return result;
    }

    // транслирует выражение/ также дополнительно вытаскивает ExprCase'ы
    @ParamLazy
    public AndExpr translateDirect(KeyTranslator translator) {
        LinearOperandMap transMap = new LinearOperandMap();
        for(Map.Entry<AndExpr,Integer> operand : map.entrySet())
            transMap.add(operand.getKey().translateDirect(translator),operand.getValue());
        return transMap.getExpr();
    }

    public AndExpr packFollowFalse(Where where) {
        return map.packFollowFalse(where).getExpr();
    }

    public DataWhereSet getFollows() {
        DataWhereSet[] follows = new DataWhereSet[map.size()] ; int num = 0;
        for(AndExpr expr : map.keySet())
            follows[num++] = expr.getFollows();
        return new DataWhereSet(follows);
    }

    public IntegralClass getStaticClass() {
        return map.getType();
    }
}