package platform.server.data.query.exprs;

import platform.server.data.classes.IntegralClass;
import platform.server.data.query.*;
import platform.server.data.query.wheres.MapWhere;
import platform.server.where.Where;

import java.util.HashMap;
import java.util.Map;

public class LinearOperandMap extends HashMap<AndExpr,Integer> {

    public IntegralClass getType() {
        assert size()>0;

        IntegralClass type = null;
        for(AndExpr expr : keySet()) {
            IntegralClass exprType = (IntegralClass) expr.getType(expr.getWhere());
            if(type==null)
                type = exprType;
            else
                type = (IntegralClass)type.getCompatible(exprType);
        }
        return type;        
    }

    void add(LinearOperandMap map, int coeff) {
        for(Map.Entry<AndExpr,Integer> addOperand : map.entrySet())
            add(addOperand.getKey(),addOperand.getValue()*coeff);
    }

    // !!!! он меняется при add'е, но конструктора пока нету так что все равно
    void add(AndExpr expr,int coeff) {
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
        for(Map.Entry<AndExpr,Integer> operand : entrySet())
            result += (operand.getValue()-1)*31 + operand.getKey().hashContext(hashContext);
        return result;
    }

    // возвращает Where на notNull
    public Where getWhere() {
        Where result = Where.FALSE;
        for(AndExpr operand : keySet())
            result = result.or(operand.getWhere());
        return result;
    }

    public String getSource(CompileSource compile) {

        if(size()==1) {
            Map.Entry<AndExpr,Integer> operand = entrySet().iterator().next();
            return addToString(true, operand.getKey().getSource(compile), operand.getValue());
        }

        String source = "";
        for(Map.Entry<AndExpr,Integer> operand : entrySet())
            if(operand.getValue()!=0)
                source = source + addToString(source.length() == 0, compile.syntax.isNULL(operand.getKey().getSource(compile), "0", true), operand.getValue());
        return "(CASE WHEN " + getWhere().getSource(compile) + " THEN " + (source.length()==0?"0":source) + " ELSE NULL END)";
    }

    public String toString() {
        String result = "";
        for(Map.Entry<AndExpr,Integer> operand : entrySet())
            result = result + addToString(result.length() == 0, operand.getKey().toString(), operand.getValue());
        return "L(" + result + ")";
    }

    public void fillContext(Context context) {
        context.fill(keySet());
    }

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        for(AndExpr operand : keySet()) // просто гоним по операндам
            operand.fillJoinWheres(joins, andWhere);
    }

    protected static String addToString(boolean first, String string, int coeff) {
        if(coeff==0) return (first?"0":"");
        if(coeff>0) return (first?"":"+") + (coeff==1?"":coeff+"*") + string;
        if(coeff==-1) return "-"+string;
        return coeff+"*"+string; // <0
    }

    private Where getSiblingsWhere(AndExpr expr) {
        Where result = Where.FALSE;
        for(AndExpr sibling : keySet())
            if(!sibling.equals(expr))
                result = result.or(sibling.getWhere());    
        return result;
    }

    public LinearOperandMap packFollowFalse(Where where) {
        LinearOperandMap followedMap = new LinearOperandMap();
        for(Map.Entry<AndExpr,Integer> operand : entrySet()) {
            Where operandWhere = where;
            if(operand.getValue().equals(0)) // если коэффициент 0 то когда остальные не null нас тоже не интересует
                operandWhere = operandWhere.or(getSiblingsWhere(operand.getKey()));
            AndExpr operandFollow = operand.getKey().andFollowFalse(operandWhere);
            if(operandFollow!=null) followedMap.add(operandFollow,operand.getValue());
        }
        return followedMap;
    }

    public AndExpr getExpr() {
        if(size()==1) {
            Map.Entry<AndExpr, Integer> entry = entrySet().iterator().next();
            if(entry.getValue().equals(1))
                return entry.getKey();
        }
        return new LinearExpr(this);
    }
}
