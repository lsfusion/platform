package platform.server.data.expr;

import platform.base.Result;
import platform.server.caches.ManualLazy;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.InnerJoins;
import platform.server.data.query.SourceJoin;
import platform.server.data.query.innerjoins.GroupJoinsWheres;
import platform.server.data.query.stat.*;
import platform.server.data.query.stat.InnerBaseJoin;
import platform.server.data.query.stat.BaseJoin;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.MapWhere;
import platform.server.data.query.JoinData;
import platform.server.data.translator.TranslateExprLazy;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@TranslateExprLazy
public abstract class InnerExpr extends NotNullExpr implements JoinData {

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        joins.add(this, andWhere);
    }

    public InnerJoin<?> getFJGroup() {
        return getInnerJoin();
    }

    public Expr getFJExpr() {
        return this;
    }

    public String getFJString(String exprFJ) {
        return exprFJ;
    }

    public abstract class NotNull extends NotNullExpr.NotNull {

        public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(Set<K> keepStat, KeyStat keyStat) {
            return new GroupJoinsWheres(InnerExpr.this.getInnerJoin(), this);
        }

        protected DataWhereSet calculateFollows() {
            return new DataWhereSet(getExprFollows(false, true));
        }
    }

    private InnerExprSet exprThisFollows = null;
    @ManualLazy
    public InnerExprSet getExprFollows(boolean includeThis, boolean recursive) {
        assert includeThis || recursive;
        if(recursive) {
            if(includeThis) {
                if(exprThisFollows==null) {
                    exprThisFollows = new InnerExprSet(super.getExprFollows(true, true));
                    exprThisFollows.add(this);
                }
                return exprThisFollows;
            } else
                return super.getExprFollows(false, true);
        } else // не кэшируем так как редко используется
            return new InnerExprSet(this);
    }

    // множественное наследование
    public static <K> InnerExprSet getExprFollows(BaseJoin<K> join, boolean recursive) { // куда-то надо же положить
        return new InnerExprSet(join.getJoins().values(), recursive);
    }

    // множественное наследование
    public static InnerJoins getInnerJoins(InnerJoin join) { // куда-то надо же положить
        return new InnerJoins(join);
    }

    // множественное наследование
    public static InnerJoins getFollowJoins(WhereJoin<?, ?> join, Result<Map<InnerJoin, Where>> upWheres) { // куда-то надо же положить
        InnerJoins result = new InnerJoins();
        Map<InnerJoin, Where> upResult = new HashMap<InnerJoin, Where>();
        InnerExprSet innerExprs = join.getExprFollows(false);
        for(int i=0;i<innerExprs.size;i++) {
            InnerExpr innerExpr = innerExprs.get(i); InnerJoin innerJoin = innerExpr.getInnerJoin();
            result = result.and(new InnerJoins(innerJoin));
            upResult = result.andUpWheres(upResult, Collections.singletonMap(innerJoin,  innerExpr.getWhere()));
        }
        upWheres.set(upResult);
        return result;
    }

    public abstract InnerJoin<?> getInnerJoin();
    public InnerBaseJoin<?> getBaseJoin() {
        return getInnerJoin();
    }

    public abstract InnerExpr translateOuter(MapTranslate translator);
}
