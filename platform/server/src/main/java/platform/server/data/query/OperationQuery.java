package platform.server.data.query;

import platform.interop.Compare;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.query.exprs.LinearExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.wheres.CompareWhere;
import platform.server.data.query.wheres.JoinWhere;
import platform.server.data.Union;

import java.util.Collection;
import java.util.List;

// пока сделаем так что у UnionQuery одинаковые ключи
public class OperationQuery<K,V> extends UnionQuery<K,V> {

    public OperationQuery(Collection<? extends K> iKeys, Union iDefaultOperator) {
        super(iKeys);
        defaultOperator = iDefaultOperator;
    }

    // как в List 0 - MAX, 1 - SUM, 2 - NVL, плюс 3 - если есть в Source
    Union defaultOperator;

    public static SourceExpr getExpr(List<SourceExpr> operands, Union operator) {
        SourceExpr result = operands.get(0);
        for(int i=1;i<operands.size();i++)
            result = getUnionExpr(result,operands.get(i),operator);
        return result;
    }

    static SourceExpr getUnionExpr(SourceExpr prevExpr, SourceExpr expr, Union operator) {
        if(prevExpr ==null) return expr;

        SourceExpr result;
        switch(operator) {
            case MAX: // MAX CE(New.notNull AND !(Prev>New),New,Prev)
                result = new CaseExpr(new CompareWhere(prevExpr,expr, Compare.GREATER).or(expr.getWhere().not()), prevExpr, expr);
                break;
            case SUM: // SUM CE(Prev.null,New,New.null,Prev,true,New+Prev)
//                Result = new CaseExpr(Expr.getWhere().not(),PrevExpr,new CaseExpr(PrevExpr.getWhere().not(),Expr,new FormulaExpr(PrevExpr,Expr,true)));
                result = new LinearExpr(expr, prevExpr,true);
                break;
            case OVERRIDE: // NVL CE(New.notNull,New,Prev)
                result = new CaseExpr(expr.getWhere(), expr, prevExpr);
                break;
            default:
                throw new RuntimeException("не может быть такого оператора");
        }
        return result;
    }

    SourceExpr getUnionExpr(SourceExpr prevExpr, SourceExpr expr, JoinWhere inJoin) {
        return getUnionExpr(prevExpr,expr,defaultOperator);
    }
}
