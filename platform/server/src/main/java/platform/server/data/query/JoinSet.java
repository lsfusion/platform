package platform.server.data.query;

import platform.base.AddSet;
import platform.base.BaseUtils;
import platform.base.Result;
import platform.server.caches.IdentityLazy;
import platform.server.caches.hash.HashContext;
import platform.server.data.Table;
import platform.server.data.query.innerjoins.GroupJoinSet;
import platform.server.data.expr.VariableExprSet;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.VariableClassExpr;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.query.GroupJoin;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.DNFWheres;

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

public class JoinSet extends AddSet<InnerJoin, JoinSet> implements DNFWheres.Interface<JoinSet>, GroupJoinSet<JoinSet> {

    public JoinSet() {
    }

    public JoinSet(InnerJoin[] iWheres) {
        super(iWheres);
    }

    public JoinSet(InnerJoin where) {
        super(where);
    }

    protected JoinSet createThis(InnerJoin[] wheres) {
        return new JoinSet(wheres);
    }

    protected InnerJoin[] newArray(int size) {
        return new InnerJoin[size];
    }

    protected boolean containsAll(InnerJoin who, InnerJoin what) {
        return BaseUtils.hashEquals(who,what) || what.isIn(who.getJoinFollows());
    }

    public JoinSet and(JoinSet set) {
        return add(set);
    }

    boolean means(InnerJoin inner) {
        return means(new JoinSet(inner));
    }

    public boolean means(JoinSet set) {
        return equals(and(set));
    }

    void fillJoins(Collection<Table.Join> tables, Collection<GroupJoin> groups) {
        for(InnerJoin where : wheres)
            if (where instanceof Table.Join)
                tables.add((Table.Join) where);
            else
                groups.add((GroupJoin) where);
    }

    @IdentityLazy
    public int hashOuter(HashContext hashContext) {
        int hash = 0;
        for(InnerJoin where : wheres)
            hash += where.hashOuter(hashContext);
        return hash;
    }

    public JoinSet translateOuter(MapTranslate translator) {
        InnerJoin[] transJoins = new InnerJoin[wheres.length];
        for(int i=0;i<wheres.length;i++)
            transJoins[i] = wheres[i].translateOuter(translator);
        return new JoinSet(transJoins);
    }

    public SourceJoin[] getEnum() {
        throw new RuntimeException("not supported");
    }

    public VariableExprSet getJoinFollows() {
        VariableExprSet exprs = new VariableExprSet();
        for (InnerJoin where : wheres)
            exprs.addAll(where.getJoinFollows());
        return exprs;
    }

    @IdentityLazy
    public Set<InnerJoin> getAllJoins() {
        Set<InnerJoin> result = new HashSet<InnerJoin>();
        for (InnerJoin where : wheres) {
            result.add(where);

            VariableExprSet joinFollows = where.getJoinFollows();
            for(int i=0;i<joinFollows.size;i++) {
                VariableClassExpr followExpr = joinFollows.get(i);
                InnerJoin innerJoin = null;
                if(followExpr instanceof GroupExpr)
                    innerJoin = ((GroupExpr)followExpr).getGroupJoin();
                if(followExpr instanceof Table.Join.Expr)
                    innerJoin = ((Table.Join.Expr)followExpr).getJoin();
                if(innerJoin!=null)
                    result.add(innerJoin);
            }
        }
        return result;
    }

    // получает подможнство join'ов которое дает keys, пропуская skipJoin. тут же алгоритм по определению достаточных ключей
    public Set<KeyExpr> insufficientKeys(Set<KeyExpr> keys, InnerJoin skipJoin, Result<JoinSet> result) {
        Set<KeyExpr> foundedKeys = new HashSet<KeyExpr>();
        Set<KeyExpr> neededKeys = new HashSet<KeyExpr>(keys);

        JoinSet joinSet = new JoinSet();

        for(InnerJoin innerJoin : getAllJoins()) {
            if(!(skipJoin!=null && (BaseUtils.hashEquals(skipJoin, innerJoin) || skipJoin.isIn(innerJoin.getJoinFollows())))) {
                Map<Object, BaseExpr> joinExprs = innerJoin.getJoins();
                Set<Object> insufKeys = innerJoin.insufficientKeys();
                
                boolean hasNeeded = false;
                for(Map.Entry<Object,BaseExpr> joinExpr : joinExprs.entrySet()) // бежим по достаточным ключам
                    if(joinExpr.getValue() instanceof KeyExpr && !insufKeys.contains(joinExpr.getKey())) {
                        hasNeeded = neededKeys.remove((KeyExpr)joinExpr.getValue()) || hasNeeded;
                        if(hasNeeded && result!=null)
                            foundedKeys.add((KeyExpr) joinExpr.getValue());
                    }
                if(hasNeeded && result!=null) { // интересует, только если есть необходимые ключи
                    joinSet = joinSet.and(new JoinSet(innerJoin));
                    // добавляем недостающие ключи в neededKeys
                    neededKeys.addAll(BaseUtils.remove(AbstractSourceJoin.enumKeys(BaseUtils.filterNotKeys(joinExprs, insufKeys).values()), foundedKeys));
                }                    
            }

            if(neededKeys.isEmpty())
                break;
        }

        if(result!=null)
            result.set(joinSet);

        return neededKeys;
    }

    public Set<KeyExpr> insufficientKeys(Set<KeyExpr> keys) {
        return insufficientKeys(keys, null, null);
    }
}
