package platform.server.data.where;

import platform.base.BaseUtils;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.ValueExpr;

public class Equal {
    public BaseExpr[] exprs;
    public int size;
    ValueExpr value;
    public boolean dropped = false;

    Equal(BaseExpr expr,int max) {
        exprs = new BaseExpr[max];
        exprs[0] = expr;
        size = 1;
        if(expr instanceof ValueExpr)
            value = (ValueExpr) expr;
    }

    public boolean contains(BaseExpr expr) {
        for(int i=0;i<size;i++)
            if(BaseUtils.hashEquals(exprs[i],expr))
                return true;
        return false;
    }
}
