package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.server.classes.IntegralClass;
import platform.server.data.query.*;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.expr.query.OrderExpr;
import platform.server.data.where.Where;
import platform.server.data.sql.SQLSyntax;
import platform.server.caches.hash.HashContext;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collection;

public class LinearOperandMap extends HashMap<BaseExpr,Integer> {

    public IntegralClass getType() {
        assert size()>0;

        IntegralClass type = null;
        for(BaseExpr expr : keySet())
            if(!(expr instanceof KeyExpr)) {
                IntegralClass exprType = (IntegralClass) expr.getSelfType();
                if(type==null)
                    type = exprType;
                else
                    type = (IntegralClass)type.getCompatible(exprType);
            }
        return type;        
    }

    void add(LinearOperandMap map, int coeff) {
        for(Map.Entry<BaseExpr,Integer> addOperand : map.entrySet())
            add(addOperand.getKey(),addOperand.getValue()*coeff);
    }

    // !!!! он меняется при add'е, но конструктора пока нету так что все равно
    void add(BaseExpr expr,int coeff) {
        if(expr instanceof LinearExpr)
            add(((LinearExpr)expr).map,coeff);
        else {
            Integer prevCoeff = get(expr);
            if(prevCoeff!=null)
                coeff = coeff + prevCoeff;
            put(expr,coeff);
        }
    }

    public int hashContext(HashContext hashContext) {
        int result = 0;
        for(Map.Entry<BaseExpr,Integer> operand : entrySet())
            result += (operand.getValue()-1)*31 + operand.getKey().hashContext(hashContext);
        return result;
    }

    // возвращает Where на notNull
    public Where getWhere() {
        Where result = Where.FALSE;
        for(BaseExpr operand : keySet())
            result = result.or(operand.getWhere());
        return result;
    }

    public String getSource(CompileSource compile) {

        if(size()==1) {
            Map.Entry<BaseExpr,Integer> operand = BaseUtils.singleEntry(this);
            return addToString(true, operand.getKey().getSource(compile), operand.getValue());
        }

        String source = "";
        Where linearWhere = Where.FALSE;
        Collection<String> orderWhere = new ArrayList<String>();
        for(Map.Entry<BaseExpr,Integer> operand : entrySet()) {
            if(operand.getValue()!=0)
                source = source + addToString(source.length() == 0, compile.syntax.isNULL(operand.getKey().getSource(compile), "0", true), operand.getValue());
            if(operand.getKey() instanceof OrderExpr)
                orderWhere.add(operand.getKey().getSource(compile)+" IS NOT NULL");
            else
                linearWhere = linearWhere.or(operand.getKey().getWhere());
        }
        return "(CASE WHEN " + linearWhere.getSource(compile) + (orderWhere.size()==0?"":" OR "+BaseUtils.toString(orderWhere," OR ")) + " THEN " + (source.length()==0?"0":source) + " ELSE " + SQLSyntax.NULL + " END)";
    }

    public String toString() {
        String result = "";
        for(Map.Entry<BaseExpr,Integer> operand : entrySet())
            result = result + addToString(result.length() == 0, operand.getKey().toString(), operand.getValue());
        return "L(" + result + ")";
    }

    public void enumerate(SourceEnumerator enumerator) {
        enumerator.fill(keySet());
    }

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        for(BaseExpr operand : keySet()) // просто гоним по операндам
            operand.fillJoinWheres(joins, andWhere);
    }

    protected static String addToString(boolean first, String string, int coeff) {
        if(coeff==0) return (first?"0":"");
        if(coeff>0) return (first?"":"+") + (coeff==1?"":coeff+"*") + string;
        if(coeff==-1) return "-"+string;
        return coeff+"*"+string; // <0
    }

    public Where getSiblingsWhere(BaseExpr expr) {
        Where result = Where.FALSE;
        for(BaseExpr sibling : keySet())
            if(!BaseUtils.hashEquals(sibling,expr))
                result = result.or(sibling.getWhere());    
        return result;
    }

    public LinearOperandMap packFollowFalse(Where where) {
        LinearOperandMap followedMap = new LinearOperandMap();
        for(Map.Entry<BaseExpr,Integer> operand : entrySet()) {
            Where operandWhere = where;
            if(operand.getValue().equals(0)) // если коэффициент 0 то когда остальные не null нас тоже не интересует
                operandWhere = operandWhere.or(getSiblingsWhere(operand.getKey()));
            BaseExpr operandFollow = operand.getKey().andFollowFalse(operandWhere);
            if(operandFollow!=null) followedMap.add(operandFollow,operand.getValue());
        }
        return followedMap;
    }

    public BaseExpr getExpr() {
        if(size()==1) {
            Map.Entry<BaseExpr, Integer> entry = BaseUtils.singleEntry(this);
            if(entry.getValue().equals(1))
                return entry.getKey();
        }
        return new LinearExpr(this);
    }
}
