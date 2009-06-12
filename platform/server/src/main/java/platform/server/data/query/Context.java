package platform.server.data.query;

import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.translators.MergeTranslator;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.query.translators.PackTranslator;
import platform.server.where.Where;
import platform.base.BaseUtils;
import platform.base.Pairs;
import platform.base.MapIterable;

import java.util.*;

public class Context extends ArrayList<Collection<DataJoin>> {

    public Context() {
        keys = new HashSet<KeyExpr>();
        values = new HashSet<ValueExpr>();
    }

    public int hash() {
        return keys.size()+values.size()*31; 
    }

    public <J> int add(DataJoin<J,?> join, boolean compile) {
        for(int i=0;i<size();i++)
            if(get(i).contains(join))
                return i;

        int level = fill(join.joins.values(),compile)+1;
        assert (level<=size());
        if(level==size())
            add(new ArrayList<DataJoin>());
        get(level).add(join);

        values.addAll(join.source.getValues());

        return level;
    }

    public final Set<KeyExpr> keys;
    public final Set<ValueExpr> values;

    public int fill(Collection<? extends SourceJoin> exprs,boolean compile) {
        int level = -1;
        for(SourceJoin expr : exprs)
            level = BaseUtils.max(expr.fillContext(this, compile),level);
        return level;
    }

    static class MergedJoin<J> extends ArrayList<MergedJoin.Entry> {
        static class Entry<J,U> {
            final DataJoin<J,U> join;
            final Map<U,Object> mapProps;

            Entry(DataJoin<J, U> iJoin, Map<U,Object> iMapProps) {
                join = iJoin;
                mapProps = iMapProps;
            }

            <MJ> void retranslate(MergeTranslator translator,DataJoin<MJ,Object> merged) {
                translator.retranslate(join, merged, mapProps);
            }
        }

        private DataJoin<J,Object> merged;

        <U> MergedJoin(DataJoin<J,U> transJoin,DataJoin<J,U> join) {
            merged = DataJoin.<J,Object,U>immutableCast(transJoin);
            add(new Entry<J,U>(join,new HashMap<U,Object>(BaseUtils.toMap(join.source.getProperties()))));
        }

        <MJ,MU> boolean merge(DataJoin<MJ,MU> transJoin,DataJoin<MJ,MU> join) {
            Map<MU,Object> mergeProps = new HashMap<MU, Object>();
            DataJoin<J,Object> joinMerged = merged.merge(transJoin,mergeProps);
            if(joinMerged!=null) {
                merged = joinMerged;
                add(new Entry<MJ,MU>(join,mergeProps));
                return true;
            } else
                return false;
        }

        public DataJoin<J,Object> retranslate(MergeTranslator translator) {
            for(Entry<?,?> entry : this)
                entry.retranslate(translator, merged);
            return merged;
        }
    }

    private static <J,U> void mergeJoin(DataJoin<J,U> join, MergeTranslator translator,Collection<MergedJoin> mergedJoins) {
        DataJoin<J,U> transJoin = new DataJoin<J, U>(join.source, translator.translateAnd(join.joins));
        for(MergedJoin<?> mergedJoin : mergedJoins)
            if(mergedJoin.merge(transJoin,join)) return;
        mergedJoins.add(new MergedJoin<J>(transJoin,join));
    }

    // конструктор слияния (merge)
    public Context(Context context, MergeTranslator translated) {
        // бежим по каждому уровню, транслируя предыдущий уровень и сливая
        for(Collection<DataJoin> level : context) {
            Collection<MergedJoin> mergedJoins = new ArrayList<MergedJoin>(); // сольем все join'ы
            for(DataJoin<?,?> join : level)
                mergeJoin(join,translated,mergedJoins);

            Collection<DataJoin> levelJoins = new ArrayList<DataJoin>();
            for(MergedJoin mergedJoin : mergedJoins)
                levelJoins.add(mergedJoin.retranslate(translated));
            add(levelJoins);
        }

        keys = context.keys;
        values = context.values;
    }

    // конструктор трансляции (translate)
    public Context(Context context, KeyTranslator translated) {
        for(Collection<DataJoin> level : context) {
            Collection<DataJoin> transLevel = new ArrayList<DataJoin>();
            for (DataJoin join : level) // здесь по сути надо перетранслировать ValueExpr'ы а также GroupQuery перебить на новые Values
                transLevel.add(join.translate(translated));
            add(transLevel);
        }

        keys = new HashSet<KeyExpr>(translated.keys.values());
        values = new HashSet<ValueExpr>(translated.values.values());
    }

    public Context(Context context) {
        for(Collection<DataJoin> level : context)
            add(new ArrayList<DataJoin>(level));
        keys = new HashSet<KeyExpr>(context.keys);
        values = new HashSet<ValueExpr>(context.values);
    }

    public Context(Context context,Collection<ValueExpr> addValues) {
        addAll(context);
        keys = context.keys;
        values = new HashSet<ValueExpr>(context.values);
        values.addAll(addValues);
    }

    // конструктор объединения (add)
    public Context(Context context,Context mergeContext) {
        this(context);
        add(mergeContext);
    }

    public void add(Context context) {
        for(int i=0;i<BaseUtils.min(size(),context.size());i++)
            get(i).addAll(context.get(i));
        if(context.size()>size())
            addAll(context.subList(size(),context.size()));
        keys.addAll(context.keys);
        values.addAll(context.values);
    }

    // конструктор упаковки (pack)
    public Context(Context context, Where where, PackTranslator translated) {

        for(Collection<DataJoin> level : context) {
            Collection<DataJoin> packedJoins = new ArrayList<DataJoin>();
            for(DataJoin join : level)
                packedJoins.add(join.pack(where, translated));
            add(packedJoins);
        }

        keys = context.keys;
        values = context.values;
    }

    class DoublePairs {
        Map<ValueExpr,ValueExpr> values;
        Map<KeyExpr,KeyExpr> keys;

        DoublePairs(Map<ValueExpr, ValueExpr> iValues, Map<KeyExpr, KeyExpr> iKeys) {
            values = iValues;
            keys = iKeys;
        }
    }

    class DoublePairsIterator implements Iterator<DoublePairs> {

        Pairs<KeyExpr,KeyExpr> keyPairs;
        
        Iterator<Map<ValueExpr,ValueExpr>> valueIterator;
        Iterator<Map<KeyExpr,KeyExpr>> keysIterator;

        Context context;

        DoublePairsIterator(Context context) {
            valueIterator = new Pairs<ValueExpr,ValueExpr>(values,context.values).iterator();
            keyPairs = new Pairs<KeyExpr,KeyExpr>(keys,context.keys);
            keysIterator = keyPairs.iterator();

            mapValues = valueIterator.next();
        }

        public boolean hasNext() {
            return keysIterator.hasNext() || valueIterator.hasNext();
        }

        Map<ValueExpr,ValueExpr> mapValues;

        public DoublePairs next() {
            Map<KeyExpr,KeyExpr> mapKeys;
            if(keysIterator.hasNext())
                mapKeys = keysIterator.next();
            else {
                mapValues = valueIterator.next();
                keysIterator = keyPairs.iterator();
                mapKeys = keysIterator.next();
            }
            return new DoublePairs(mapValues, mapKeys);
        }

        public void remove() {
            throw new RuntimeException("not supported");
        }
    }

    public Iterable<MapContext> map(final Context context) {
        if(keys.size()!=context.keys.size() || values.size()!=context.values.size() || size()!=context.size()) return new ArrayList<MapContext>();

        return new MapIterable<DoublePairs,MapContext>() {
            protected MapContext map(DoublePairs pairs) {
                for(Map.Entry<ValueExpr,ValueExpr> mapValue : pairs.values.entrySet())
                    if(!mapValue.getKey().objectClass.equals(mapValue.getValue().objectClass))
                        return null;

                MapContext result = new MapContext(pairs.values,pairs.keys);
                for(int i=0;i<size();i++) {
                    // строим hash
                    Map<Integer,Collection<DataJoin>> hashQueryJoins = new HashMap<Integer,Collection<DataJoin>>();
                    for(DataJoin join : context.get(i)) {
                        Integer hash = join.hash();
                        Collection<DataJoin> hashJoins = hashQueryJoins.get(hash);
                        if(hashJoins==null) {
                            hashJoins = new ArrayList<DataJoin>();
                            hashQueryJoins.put(hash,hashJoins);
                        }
                        hashJoins.add(join);
                    }

                    for(DataJoin<?,?> join : get(i)) {
                        Collection<DataJoin> hashJoins = hashQueryJoins.get(join.hash());
                        if(hashJoins==null) return null;
                        boolean atLeastOneEquals = false;
                        for(DataJoin<?,?> hashJoin : hashJoins)
                            atLeastOneEquals = join.equals(hashJoin, result) || atLeastOneEquals;
                        if(!atLeastOneEquals) return null;
                    }
                }
                return result;
            }

            protected Iterator<DoublePairs> mapIterator() {
                return new DoublePairsIterator(context);
            }
        };
    }

    public List<Set<DataJoin>> forCheckEqual() {
        List<Set<DataJoin>> result = new ArrayList<Set<DataJoin>>();
        for(Collection<DataJoin> level : this)
            result.add(new HashSet<DataJoin>(level));
        return result;
    }

    public void checkFirstLevel() {
        if(size()>0) {
            for(DataJoin<?,?> join : get(0)) {
                for(AndExpr expr : join.joins.values())
                    if(!(expr instanceof ValueExpr) && !(expr instanceof KeyExpr))
                        expr = expr;
            }
        }
    }
}
