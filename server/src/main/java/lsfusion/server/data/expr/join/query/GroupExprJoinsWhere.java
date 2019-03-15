package lsfusion.server.data.expr.join.query;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.stat.StatType;
import lsfusion.server.data.expr.join.where.GroupJoinsWhere;
import lsfusion.server.data.translator.ExprTranslator;

public class GroupExprJoinsWhere<K extends Expr> {

    public final ImMap<K, BaseExpr> mapExprs;
    public final GroupJoinsWhere joinsWhere;

    public GroupExprJoinsWhere(ImMap<K, BaseExpr> mapExprs, GroupJoinsWhere joinsWhere) {
        this.mapExprs = mapExprs;
        this.joinsWhere = joinsWhere;
    }
    
    public static <K extends Expr> ImCol<GroupExprJoinsWhere<K>> create(ImCol<GroupJoinsWhere> joinsWheres, final ImMap<K, BaseExpr> mapExprs, StatType statType, boolean forcePackReduce) {
        MCol<GroupExprJoinsWhere<K>> mResult = ListFact.mCol();
        for(int i=0,size=joinsWheres.size();i<size;i++) {
            GroupJoinsWhere joinsWhere = joinsWheres.get(i);
            if(joinsWhere.keyEqual.isEmpty())
                mResult.add(new GroupExprJoinsWhere<K>(mapExprs, joinsWhere));
            else {
                ExprTranslator translator = joinsWhere.keyEqual.getTranslator();
                ImMap<K, Expr> transMapExprs = translator.translate(mapExprs);
                ImMap<K, BaseExpr> transMapBaseExprs = BaseExpr.onlyBaseExprs(transMapExprs);
                if(transMapBaseExprs != null)
                    mResult.add(new GroupExprJoinsWhere<K>(transMapBaseExprs, joinsWhere));
                else
                    mResult.addAll(joinsWhere.getFullWhere().getGroupExprJoinsWheres(transMapExprs, statType, forcePackReduce));
            }
        }
        return mResult.immutableCol();
//
//        return joinsWheres.mapColValues(new GetValue<GroupExprJoinsWhere<K>, GroupJoinsWhere>() {
//            public GroupExprJoinsWhere<K> getMapValue(GroupJoinsWhere value) {
//                return new GroupExprJoinsWhere<>(mapExprs, value);
//            }
//        });
    }
}
