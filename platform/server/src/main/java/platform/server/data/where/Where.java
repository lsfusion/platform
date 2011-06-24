package platform.server.data.where;

import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.KeyType;
import platform.server.data.query.SourceJoin;
import platform.server.data.query.innerjoins.*;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.where.classes.MeanClassWheres;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface Where extends SourceJoin<Where>, OuterContext<Where>, KeyType, CheckWhere {

    Where followFalse(Where falseWhere);
    Where followFalse(Where falseWhere, boolean packExprs);
    Where followFalse(CheckWhere falseWhere, boolean pack, FollowChange change); // protected

    static enum FollowType { // protected
        WIDE,NARROW,DIFF,EQUALS;

        public FollowType or(FollowType or) {
            if(this==FollowType.DIFF || this==or || or==FollowType.EQUALS)
                return this;
            if(or==FollowType.DIFF || this==FollowType.EQUALS)
                return or;
            return FollowType.DIFF;
        }
    }

    static class FollowChange { // protected

        public FollowType type = FollowType.EQUALS;
        void not() {
            switch(type) {
                case WIDE:
                    type=FollowType.NARROW;
                    break;
                case NARROW:
                    type=FollowType.WIDE;
                    break;
            }
        }
    }

    
    Where pack();

    <K> Map<K, Expr> followTrue(Map<K,? extends Expr> map);

    Where not();

    Where and(Where where);
    Where and(Where where, boolean packExprs);
    Where or(Where where);
    Where or(Where where, boolean packExprs);

    Where xor(Where where);

    OrObjectWhere[] getOr(); // protected

    Map<BaseExpr,BaseExpr> getExprValues();
    Map<BaseExpr,BaseExpr> getNotExprValues();

    static String TRUE_STRING = "1=1";
    static String FALSE_STRING = "1<>1";

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    Collection<InnerSelectJoin> getInnerJoins(boolean notExclusive);
    Collection<InnerGroupJoin<? extends GroupJoinSet>> getInnerJoins(boolean notExclusive, boolean noJoins, Set<KeyExpr> keys);
    ObjectJoinSets groupObjectJoinSets(); // protected
    KeyEquals getKeyEquals();
    MeanClassWheres groupMeanClassWheres();

    abstract public ClassExprWhere getClassWhere();

    int hashOuter(HashContext hashContext);

    int getHeight();

    static Where TRUE = new AndWhere();
    static Where FALSE = new OrWhere();

    Where translateQuery(QueryTranslator translator);

    Where translateOuter(MapTranslate translator);

    Where map(Map<KeyExpr,? extends Expr> map);

    Where getKeepWhere(KeyExpr expr);
}
