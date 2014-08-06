package lsfusion.server.data.where;

import lsfusion.base.BaseUtils;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.StaticExpr;
import lsfusion.server.data.expr.ValueExpr;

public class Equal {
    public final BaseExpr[] exprs;
    public int size;
    public ValueExpr value;
    public StaticExpr staticValue;
    public boolean dropped = false;

    Equal(BaseExpr expr,int max) {
        exprs = new BaseExpr[max];
        exprs[0] = expr;
        size = 1;
        if(expr instanceof StaticExpr) {
            if(expr instanceof ValueExpr)
                value = (ValueExpr) expr;
            else
                staticValue = (StaticExpr)expr;
        }
    }

    public boolean contains(BaseExpr expr) {
        for(int i=0;i<size;i++)
            if(BaseUtils.hashEquals(exprs[i],expr))
                return true;
        return false;
    }
}
