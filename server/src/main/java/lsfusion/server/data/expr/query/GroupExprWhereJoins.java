package lsfusion.server.data.expr.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.AbstractOuterContext;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.innerjoins.GroupJoinsWhere;
import lsfusion.server.data.query.innerjoins.KeyEqual;
import lsfusion.server.data.query.stat.*;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.translator.MapTranslate;

public class GroupExprWhereJoins<K extends Expr> extends AbstractOuterContext<GroupExprWhereJoins<K>> {

    public static class Node<K extends Expr> extends AbstractOuterContext<Node<K>>  {
        public final ImMap<K, BaseExpr> mapExprs;
        public final KeyEqual keyEqual;
        public final WhereJoins joins;

        public Node(ImMap<K, BaseExpr> mapExprs, KeyEqual keyEqual, WhereJoins joins) {
            this.mapExprs = mapExprs;
            this.keyEqual = keyEqual;
            this.joins = joins;
        }

        public StatKeys<KeyExpr> getPartitionStatKeys(KeyStat keyStat, StatType type, StatKeys<KeyExpr> statKeys, ImSet<KeyExpr> allKeys, boolean useWhere) {
            keyStat = keyEqual.getKeyStat(keyStat);

            ImSet<BaseExpr> group = mapExprs.values().toSet();
            StatKeys<BaseExpr> partitionStats = (useWhere ? joins : WhereJoins.EMPTY).pushStatKeys(statKeys).getStatKeys(group, keyStat, type, keyEqual); // joins
            return joins.pushStatKeys(partitionStats).getStatKeys(allKeys, keyStat, type, keyEqual);
        }

        public StatKeys<K> getStatKeys(KeyStat keyStat, StatType type, StatKeys<K> statKeys) {
            keyStat = keyEqual.getKeyStat(keyStat);

            WhereJoins adjJoins = joins;
            if(statKeys != StatKeys.<K>NOPUSH()) {
                Result<ImRevMap<K, BaseExpr>> revMap = new Result<>();
                StatKeys<K> revStatKeys = statKeys.toRevMap(mapExprs.filterIncl(statKeys.getKeys()), revMap);
                adjJoins = adjJoins.pushStatKeys(revStatKeys.mapBack(revMap.result.reverse()));
            }
            return adjJoins.getStatKeys(mapExprs.values().toSet(), keyStat, type, keyEqual).mapBack(mapExprs);
        }

        protected ImSet<OuterContext> calculateOuterDepends() {
            return SetFact.mergeSet(mapExprs.keys(), BaseUtils.<ImSet<OuterContext>>immutableCast(mapExprs.values().toSet())).merge(keyEqual).merge(joins);
        }

        protected Node<K> translate(MapTranslate translator) {
            return new Node<>(translator.translateMap(mapExprs), keyEqual.translateOuter(translator), joins.translateOuter(translator));
        }

        protected int hash(HashContext hash) {
            return 31 * (31 * AbstractOuterContext.hashMapOuter(mapExprs, hash) + keyEqual.hashOuter(hash)) + joins.hashOuter(hash);
        }

        protected boolean calcTwins(TwinImmutableObject o) {
            return mapExprs.equals(((Node<K>)o).mapExprs) && keyEqual.equals(((Node<K>)o).keyEqual) && joins.equals(((Node<K>)o).joins);
        }
    }

    private ImSet<Node<K>> nodes;

    public GroupExprWhereJoins(ImSet<Node<K>> nodes) {
        this.nodes = nodes;
    }

    public StatKeys<KeyExpr> getPartitionStatKeys(final KeyStat keyStat, final StatType type, final StatKeys<KeyExpr> statKeys, final boolean useWhere, final ImSet<KeyExpr> allKeys) {
        return StatKeys.or(nodes, new GetValue<StatKeys<KeyExpr>, Node<K>>() {
            public StatKeys<KeyExpr> getMapValue(Node<K> value) {
                return value.getPartitionStatKeys(keyStat, type, statKeys, allKeys, useWhere);
            }
        }, allKeys);
    }

    public StatKeys<K> getStatKeys(final KeyStat keyStat, final StatType type, final StatKeys<K> statKeys, ImSet<K> allKeys) {
        return StatKeys.or(nodes, new GetValue<StatKeys<K>, Node<K>>() {
            public StatKeys<K> getMapValue(Node<K> value) {
                return value.getStatKeys(keyStat, type, statKeys);
            }
        }, allKeys);
    }

    // GroupJoinsWhere может и всегда приходит без Where
    public static <K extends Expr> GroupExprWhereJoins<K> create(ImCol<GroupJoinsWhere> whereJoins, final ImMap<K, BaseExpr> mapExprs) {
        return new GroupExprWhereJoins<>(whereJoins.mapMergeSetValues(new GetValue<Node<K>, GroupJoinsWhere>() {
            public Node<K> getMapValue(GroupJoinsWhere value) {
                return new Node<>(mapExprs, value.keyEqual, value.joins);
            }
        }));
    }

    private static final GroupExprWhereJoins EMPTY = new GroupExprWhereJoins(SetFact.EMPTY());
    public static <K extends Expr> GroupExprWhereJoins<K> EMPTY() {
        return EMPTY;
    }

    public GroupExprWhereJoins<K> merge(GroupExprWhereJoins<K> joins) {
        return new GroupExprWhereJoins<>(nodes.merge(joins.nodes));
    }

    @Override
    protected ImSet<OuterContext> calculateOuterDepends() {
        return BaseUtils.immutableCast(nodes);
    }

    @Override
    protected GroupExprWhereJoins<K> translate(MapTranslate translator) {
        return new GroupExprWhereJoins<>(translator.translateSet(nodes));
    }

    @Override
    protected int hash(HashContext hash) {
        return AbstractOuterContext.hashOuter(nodes, hash);
    }

    @Override
    protected boolean calcTwins(TwinImmutableObject o) {
        return nodes.equals(((GroupExprWhereJoins<K>)o).nodes);
    }
}
