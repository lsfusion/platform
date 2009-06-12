package platform.server.data.query.exprs;

import platform.server.data.classes.IntegralClass;
import platform.server.data.query.*;
import platform.server.data.query.translators.DirectTranslator;
import platform.server.data.query.translators.Translator;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

import java.util.Map;

// среднее что-то между CaseExpr и FormulaExpr - для того чтобы не плодить экспоненциальные case'ы
// придется делать AndExpr
public class LinearExpr extends StaticClassExpr {

    final LinearOperandMap map;

    private static String addToString(boolean first, String string, int coeff) {
        if(coeff==0) return (first?"0":"");
        if(coeff>0) return (first?"":"+") + (coeff==1?"":coeff+"*") + string;
        if(coeff==-1) return "-"+string;
        return coeff+"*"+string; // <0
    }

    public LinearExpr(LinearOperandMap iMap) {
        map = iMap;
        map.removeZeros();
        // повырезаем все смежные getWhere с нулевыми коэффицентами
        assert (map.size()>0);
    }

    // при translate'ы вырезает LinearExpr'ы

    public Type getType(Where where) {
        // бежим по всем операндам
        IntegralClass result = null;
        for(AndExpr operand : map.keySet()) {
            IntegralClass operandClass = (IntegralClass) operand.getType(where);
            if(result==null)
                result = operandClass;
            else
                result = (IntegralClass)result.getCompatible(operandClass);
        }
        return result;
    }

    // возвращает Where на notNull
    protected Where calculateWhere() {
        Where result = Where.FALSE;
        for(AndExpr operand : map.keySet())
            result = result.or(operand.getWhere());
        return result;
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {

        if(map.size()==1) {
            Map.Entry<AndExpr,Integer> operand = map.entrySet().iterator().next();
            return addToString(true, operand.getKey().getSource(queryData, syntax), operand.getValue());
        }

        String source = "";
        for(Map.Entry<AndExpr,Integer> operand : map.entrySet())
            if(operand.getValue()!=0)
                source = source + addToString(source.length() == 0, syntax.isNULL(operand.getKey().getSource(queryData, syntax), "0", true), operand.getValue());
        return "(CASE WHEN " + getWhere().getSource(queryData, syntax) + " THEN " + (source.length()==0?"0":source) + " ELSE NULL END)";
    }

    public String toString() {
        String result = "";
        for(Map.Entry<AndExpr,Integer> operand : map.entrySet())
            result = result + addToString(result.length() == 0, operand.getKey().toString(), operand.getValue());
        return "L(" + result + ")";
    }

    public int fillContext(Context context, boolean compile) {
        return context.fill(map.keySet(),compile);
    }

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        for(AndExpr operand : map.keySet()) // просто гоним по операндам
            operand.fillJoinWheres(joins, andWhere);
    }

    // мы и так перегрузили fillJoinWheres
    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    // для кэша
    public boolean equals(SourceExpr expr, MapContext mapContext) {
        return expr instanceof LinearExpr && map.equals(((LinearExpr) expr).map, mapContext);
    }

    protected int getHash() {
        return map.hash();
    }

    public boolean equals(Object obj) {
        if(map.size()==1) {
            Map.Entry<AndExpr, Integer> singleEntry = map.entrySet().iterator().next();
            if(singleEntry.getValue().equals(1)) return singleEntry.getKey().equals(obj);
        }
        return this==obj || obj instanceof LinearExpr && map.equals(((LinearExpr)obj).map);
    }

    protected int getHashCode() {
        return map.hashCode();
    }

    public SourceExpr translate(Translator translator) {
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
    public AndExpr translateAnd(DirectTranslator translator) {
        LinearOperandMap transMap = new LinearOperandMap();
        for(Map.Entry<AndExpr,Integer> operand : map.entrySet())
            transMap.add(operand.getKey().translateAnd(translator),operand.getValue());
        return new LinearExpr(transMap);
    }

    public AndExpr linearFollowFalse(Where where) {
        LinearOperandMap followedMap = new LinearOperandMap();
        for(Map.Entry<AndExpr,Integer> operand : map.entrySet()) {
            AndExpr operandFollow = operand.getKey().andFollowFalse(where);
            if(operandFollow!=null) followedMap.add(operandFollow,operand.getValue());
        }
        return new LinearExpr(followedMap);
    }

    public DataWhereSet getFollows() {
        if(map.size()==1) return map.keySet().iterator().next().getFollows();
        DataWhereSet[] follows = new DataWhereSet[map.size()] ; int num = 0;
        for(AndExpr expr : map.keySet())
            follows[num++] = expr.getFollows();
        return new DataWhereSet(follows);
    }

    public IntegralClass getStaticClass() {
        return (IntegralClass) getType(getWhere());
    }
}