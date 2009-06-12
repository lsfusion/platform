package platform.server.data.query.exprs;

import platform.base.BaseUtils;
import platform.server.where.Where;
import platform.server.data.query.MapContext;

import java.util.HashMap;
import java.util.Map;

public class LinearOperandMap extends HashMap<AndExpr,Integer> {

    // !!!! он меняется при add'е, но конструктора пока нету так что все равно
    void add(AndExpr expr,Integer coeff) {
        if(expr instanceof LinearExpr) {
            for(Map.Entry<AndExpr,Integer> addOperand : ((LinearExpr)expr).map.entrySet())
                add(addOperand.getKey(),addOperand.getValue()*coeff);
        } else {
            Integer prevCoeff = get(expr);
            if(prevCoeff==null) prevCoeff = 0;
            put(expr,coeff+prevCoeff);
        }
    }

    void removeZeros() {
        for(AndExpr zeroExpr : BaseUtils.filterValues(this,0)) {
            Where where = Where.FALSE;
            for(AndExpr expr : keySet())
                if(expr!=zeroExpr)
                    where = where.or(expr.getWhere());
            if(zeroExpr.getWhere().means(where)) // есл верно zeroExpr не null то и одно из выражений не null - то нафиг не нужен такой expr
                remove(zeroExpr);
        }
    }

    // для кэша
    public boolean equals(LinearOperandMap map, MapContext mapContext) {
        if(size()!=map.size()) return false;

        for(Map.Entry<AndExpr,Integer> exprOperand : map.entrySet()) {
            boolean found = false;
            for(Map.Entry<AndExpr,Integer> operand : entrySet())
                if(operand.getValue().equals(exprOperand.getValue()) && operand.getKey().hash() == exprOperand.getKey().hash() &&
                        operand.getKey().equals(exprOperand.getKey(), mapContext)) {
                    found = true;
                    break;
                }
            if(!found) return false;
        }

        return true;
    }

    protected int hash() {
        int result = 0;
        for(Map.Entry<AndExpr,Integer> operand : entrySet())
            result += (operand.getValue()-1)*31 + operand.getKey().hash();
        return result;
    }

}
