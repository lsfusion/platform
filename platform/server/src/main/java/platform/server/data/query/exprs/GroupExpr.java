package platform.server.data.query.exprs;

import net.jcip.annotations.Immutable;
import platform.base.BaseUtils;
import platform.base.Pair;
import platform.server.caches.*;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.CompileSource;
import platform.server.data.query.Context;
import platform.server.data.query.GroupJoin;
import platform.server.data.query.HashContext;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.InnerJoins;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.query.exprs.cases.ExprCaseList;
import platform.server.data.query.exprs.cases.MapCase;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.query.translators.QueryTranslator;
import platform.server.data.query.wheres.EqualsWhere;
import platform.server.data.types.Type;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

import java.util.*;

@Immutable
public abstract class GroupExpr<E extends SourceExpr,This extends GroupExpr<E,This>> extends MapExpr implements MapContext {

    public static final boolean inner = false;
    
    public final Where where;
    public final Map<AndExpr,AndExpr> group;
    public final E expr;

    final Context context;

    @TwinLazy
    public Where getJoinsWhere() {
        return getJoinsWhere(group);
    }

    // проталкивает "верхний" where внутрь
    private static Where pushWhere(Where where,Map<AndExpr,AndExpr> group) {
        Where result = where.and(getJoinsWhere(group)).getClassWhere().mapBack(group).and(getWhere(group.keySet()).getClassWhere()).getMeansWhere();
        assert result.means(getWhere(group.keySet())); // надо assert'ить чтобы не and'ить
        return result;
    }

    public abstract E packExpr(E expr, Where trueWhere);

    // можно использовать напрямую только заведомо зная что не null
    private GroupExpr(Map<AndExpr, AndExpr> group, Where<?> where, E expr, Where upWhere) {

        // PUSH VALUES
        Map<AndExpr,AndExpr> keepGroup = new HashMap<AndExpr, AndExpr>(); // проталкиваем values внутрь
        for(Map.Entry<AndExpr,AndExpr> groupExpr : group.entrySet())
            if (groupExpr.getValue() instanceof ValueExpr)
                where = where.and(new EqualsWhere(groupExpr.getKey(), (ValueExpr)groupExpr.getValue()));
            else
                keepGroup.put(groupExpr.getKey(),groupExpr.getValue());
        group = keepGroup;

        // PACK
        Where pushWhere = pushWhere(upWhere, keepGroup);
        expr = packExpr(expr,pushWhere.and(where)); // сначала pack'аем expr
        where = where.followFalse(pushWhere.and(expr.getWhere()).not()); // затем pack'аем where
        Where exprWhere = expr.getWhere().and(where); // теперь pack'аем group
        keepGroup = new HashMap<AndExpr, AndExpr>();
        for(Map.Entry<AndExpr,AndExpr> entry : group.entrySet()) // собсно будем паковать "общим" where исключив, одновременно за or not'им себя, чтобы собой не пакнуться
            keepGroup.put(entry.getKey().packFollowFalse(exprWhere.and(pushWhere.or(entry.getKey().getWhere().not()))),entry.getValue());
        group = keepGroup;

        // TRANSLATE KEYS
        Map<KeyExpr,AndExpr> keyExprs;
        while(!(keyExprs = where.getKeyExprs()).isEmpty()) {
            QueryTranslator translator = new QueryTranslator(keyExprs,new HashMap<ValueExpr, ValueExpr>());
            where = where.translateQuery(translator);
            expr = (E)expr.translateQuery(translator);

            keepGroup = new HashMap<AndExpr, AndExpr>();
            for(Map.Entry<AndExpr,AndExpr> groupExpr : group.entrySet())
                keepGroup.put((AndExpr)groupExpr.getKey().translateQuery(translator),groupExpr.getValue());
            group = keepGroup;
        }
        assert expr.getWhere().and(where).getKeyExprs().isEmpty();

        this.expr = expr;
        this.where = where;
        this.group = group;
        context = getContext(this.expr, this.group, this.where); // перечитаем

        assert checkExpr();
    }

    private SourceExpr packSingle() {
        Map<AndExpr,AndExpr> compares = new HashMap<AndExpr, AndExpr>();
        Map<KeyExpr,AndExpr> groupKeys = BaseUtils.splitKeys(group,context.keys,compares);
        if(groupKeys.size()==context.keys.size()) {
            QueryTranslator translator = new QueryTranslator(groupKeys,new HashMap<ValueExpr, ValueExpr>());
            Where transWhere = where.translateQuery(translator);
            for(Map.Entry<AndExpr,AndExpr> compare : compares.entrySet()) // оставшиеся
                transWhere = transWhere.and(EqualsWhere.create((AndExpr) compare.getKey().translateQuery(translator),compare.getValue()));
            return expr.translateQuery(translator).and(transWhere);
        } else
            return this;
    }

    // проверяем что keyExpr'ы все в контексте
    protected static SourceExpr create(GroupExpr<?,?> expr) {
        return expr.packSingle();
    }

    public GroupExpr(Where where, Map<AndExpr, AndExpr> group, E expr) {
        this(group, where, expr, Where.TRUE);
    }

    private static Map<AndExpr,AndExpr> pushValues(Map<AndExpr,AndExpr> group,Where<?> trueWhere) {
        Map<AndExpr,ValueExpr> exprValues = trueWhere.getExprValues();
        Map<AndExpr,AndExpr> result = new HashMap<AndExpr, AndExpr>(); ValueExpr pushValue; // проталкиваем values внутрь
        for(Map.Entry<AndExpr,AndExpr> groupExpr : group.entrySet())
            result.put(groupExpr.getKey(),((pushValue=exprValues.get(groupExpr.getValue()))==null?groupExpr.getValue():pushValue));
        return result;
    }

    // assertion когда не надо push'ать
    public boolean assertNoPush(Where<?> trueWhere) { // убрал trueWhere.and(getJoinsWhere())
        return Collections.disjoint(group.values(),trueWhere.getExprValues().keySet()) && getFullWhere().getClassWhere().means(trueWhere.getClassWhere().mapBack(group));
    }

    public GroupExpr(This groupExpr,Where<?> falseWhere) { // в отличии от joins, expr нельзя проталкивать потому как повлияет на результат !!!
        this(pushValues(groupExpr.group,falseWhere.not()), groupExpr.where, groupExpr.expr,falseWhere.not());

        assert !getFullWhere().isFalse();
    }

    // трансляция
    public GroupExpr(This groupExpr,KeyTranslator translator) {
        // надо еще транслировать "внутренние" values
        Map<ValueExpr, ValueExpr> mapValues = BaseUtils.filterKeys(translator.values, groupExpr.context.values);

        if(BaseUtils.identity(mapValues)) { // если все совпадает то и не перетранслируем внутри ничего 
            expr = groupExpr.expr;
            where = groupExpr.where;
            group = translator.translateDirect(groupExpr.group);
        } else { // еще values перетранслируем
            KeyTranslator valueTranslator = new KeyTranslator(BaseUtils.toMap(groupExpr.context.keys), mapValues);
            expr = (E) groupExpr.expr.translateDirect(valueTranslator);
            where = groupExpr.where.translateDirect(valueTranslator);
            group = new HashMap<AndExpr, AndExpr>();
            for(Map.Entry<AndExpr,AndExpr> groupJoin : groupExpr.group.entrySet())
                group.put(groupJoin.getKey().translateDirect(valueTranslator),groupJoin.getValue().translateDirect(translator));
        }

        context = new Context(new HashSet<KeyExpr>(groupExpr.context.keys),new HashSet<ValueExpr>(mapValues.values()));

        assert checkExpr();
    }

    private boolean checkExpr() {
        assert getContext(expr,group,where).equals(context);

        for(Map.Entry<AndExpr,AndExpr> groupExpr : group.entrySet())
            assert !(groupExpr.getValue() instanceof ValueExpr);

        return true;
    }

    protected abstract SourceExpr createThis(Where iWhere,Map<AndExpr,AndExpr> iGroup,E iExpr);

    // трансляция не прямая
    @ParamLazy
    public SourceExpr translateQuery(QueryTranslator translator) {
        ExprCaseList result = new ExprCaseList();
        for(MapCase<AndExpr> mapCase : CaseExpr.pullCases(translator.translate(group)))
            result.add(mapCase.where,createThis(where,mapCase.data,expr));
        return result.getExpr();
    }

    private static Context getContext(SourceExpr expr, Map<AndExpr, AndExpr> group, Where where) {
        // перечитаем joins, потому как из-за merge'а могут поуходить join'ы
        Context context = new Context();
        context.fill(group.keySet());
        expr.fillContext(context);
        where.fillContext(context);
        return context;
    }

    @Lazy
    public Where getFullWhere() {
        return expr.getWhere().and(getWhere(group.keySet())).and(where);
    }

    public Type getType(Where where) {
        return expr.getType(getFullWhere());
    }

    public abstract class NotNull extends MapExpr.NotNull {

        protected DataWhereSet getExprFollows() {
            return MapExpr.getExprFollows(group);
        }

        public InnerJoins getInnerJoins() {
            // здесь что-то типа GroupJoin'а быть должно
            return new InnerJoins(getGroupJoin(),this);
        }

        public int hashContext(HashContext hashContext) {
            return GroupExpr.this.hashContext(hashContext);
        }

        @Override
        public Where packFollowFalse(Where falseWhere) {
            return GroupExpr.this.packFollowFalse(falseWhere).getWhere();
        }

        public GroupJoin getGroupJoin() {
            return GroupExpr.this.getGroupJoin();
        }
    }

    public Context getContext() {
        return context;
    }

    class ImplementHashes implements MapContext {
        final Map<AndExpr,Integer> groupHashes = new HashMap<AndExpr, Integer>();

        ImplementHashes(HashContext hashContext) {
            for(Map.Entry<AndExpr,AndExpr> groupExpr : group.entrySet())
                groupHashes.put(groupExpr.getKey(),groupExpr.getValue().hashContext(hashContext));
        }

        ImplementHashes() {
            for(Map.Entry<AndExpr,AndExpr> groupExpr : group.entrySet())
                groupHashes.put(groupExpr.getKey(),groupExpr.getValue().hashCode());
        }

        public Context getContext() {
            return GroupExpr.this.getContext();
        }

        public int hash(HashContext hashContext) {
            int hash = 0;
            for(Map.Entry<AndExpr,Integer> groupHash : groupHashes.entrySet())
                hash += groupHash.getKey().hashContext(hashContext) ^ groupHash.getValue();
            return (where.hashContext(hashContext) * 31 + expr.hashContext(hashContext)) * 31 + hash;
        }
    }

    // hash'и "внешнего" контекста, там пойдет внутренняя трансляция values поэтому hash по values надо "протолкнуть" внутрь
    public int hashContext(final HashContext hashContext) {
        return new ImplementHashes(hashContext).hash(new HashContext() {
            public int hash(KeyExpr expr) {
                return 1;
            }
            public int hash(ValueExpr expr) {
                return hashContext.hash(expr);
            }
        });
    }

    // hash'и "внутреннего" контекста
    public int hash(HashContext hashContext) {
        return new ImplementHashes().hash(hashContext);
    }

    public boolean twins(AbstractSourceJoin obj) {
        GroupExpr<?,?> groupExpr = (GroupExpr)obj;

        assert hashCode()==groupExpr.hashCode();

        for(KeyTranslator translator : new MapHashIterable(this, groupExpr, false))
            if(where.translateDirect(translator).equals(groupExpr.where) && expr.translateDirect(translator).equals(groupExpr.expr) &&
                    translator.translateDirect(BaseUtils.reverse(group)).equals(BaseUtils.reverse(groupExpr.group)))
                return true;
        return false;
    }

    public InnerJoin getFJGroup() {
        return getGroupJoin();
//        return this;
    }

    static <K,V> Collection<InnerJoins.Entry> getInnerJoins(Map<K,AndExpr> groupKeys,SourceExpr expr,Where where) {
        throw new RuntimeException("not supported");
    }

    public void fillContext(Context context) {
        context.fill(group);
        context.values.addAll(this.context.values);
    }

    @Lazy
    public GroupJoin getGroupJoin() {
        return new GroupJoin(where, group, context.keys);  
    }

    public abstract boolean isMax();

    public String getSource(CompileSource compile) {
        return compile.getSource(this);
    }

    @Override
    public String toString() {
        int hash = hashCode();
        hash = hash>0?hash:-hash;
        int tobt = 0;
        for(int i=0;i<4;i++) {
            tobt += hash % 255;
            hash = hash/255;
        }
        return "G"+tobt;
    }
}

