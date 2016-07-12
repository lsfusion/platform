package lsfusion.server.session;

import lsfusion.base.Pair;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.AbstractOuterContext;
import lsfusion.server.caches.AbstractValuesContext;
import lsfusion.server.caches.hash.HashCodeKeys;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.caches.hash.HashValues;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.PropertyInterface;

public class PrereadRows<P extends PropertyInterface> extends AbstractValuesContext<PrereadRows<P>> {

    private final static PrereadRows<PropertyInterface> EMPTY = new PrereadRows<PropertyInterface>(MapFact.<Expr, ObjectValue>EMPTY(), MapFact.<ImMap<PropertyInterface, Expr>, Pair<ObjectValue, Boolean>>EMPTY());
    public static <P extends PropertyInterface> PrereadRows<P> EMPTY() {
        return (PrereadRows<P>) EMPTY;
    }

    public final ImMap<ImMap<P, Expr>, Pair<ObjectValue, Boolean>> readValues;
    public final ImMap<Expr, ObjectValue> readParams;

    public PrereadRows(ImMap<Expr, ObjectValue> readParams, ImMap<ImMap<P, Expr>, Pair<ObjectValue, Boolean>> readValues) {
        this.readParams = readParams;
        this.readValues = readValues;
    }

    public boolean isEmpty() {
        return readParams.isEmpty() && readValues.isEmpty();
    }

    protected PrereadRows<P> translate(final MapValuesTranslate translator) {
        final MapTranslate mapTranslate = translator.mapKeys();
        return new PrereadRows<P>(readParams.mapKeyValues(new GetValue<Expr, Expr>() {
            public Expr getMapValue(Expr value) {
                return value.translateOuter(mapTranslate);
            }}, new GetValue<ObjectValue, ObjectValue>() {
              public ObjectValue getMapValue(ObjectValue value) {
                  return ((ObjectValue<?>)value).translateValues(translator);
              }}),
                readValues.mapKeyValues(new GetValue<ImMap<P, Expr>, ImMap<P, Expr>>() {
            public ImMap<P, Expr> getMapValue(ImMap<P, Expr> value) {
                return mapTranslate.translate(value);
            }}, new GetValue<Pair<ObjectValue, Boolean>, Pair<ObjectValue, Boolean>>() {
            public Pair<ObjectValue, Boolean> getMapValue(Pair<ObjectValue, Boolean> value) {
                return new Pair<ObjectValue, Boolean>((ObjectValue) value.first.translateValues(translator), value.second);
            }}));
    }

    public ImSet<Value> getValues() {
        MSet<Value> mResult = SetFact.mSet();
        for(int i=0,size=readValues.size();i<size;i++) {
            ImMap<P, Expr> keys = readValues.getKey(i);
            for(int j=0,sizeJ=keys.size();j<sizeJ;j++)
                mResult.addAll(keys.getValue(j).getOuterValues());
            mResult.addAll(readValues.getValue(i).first.getContextValues());
        }
        for(int i=0,size=readParams.size();i<size;i++) {
            mResult.addAll(readParams.getKey(i).getOuterValues());
            mResult.addAll(readParams.getValue(i).getContextValues());
        }
        return mResult.immutable();
    }

    protected int hash(HashValues hashValues) {
        if(isEmpty()) // оптимизация
            return 0;

        int hash = 0;
        HashContext hashContext = HashContext.create(HashCodeKeys.instance, hashValues);
        for(int i=0,size=readValues.size();i<size;i++) {
            Pair<ObjectValue, Boolean> value = readValues.getValue(i);
            hash += AbstractOuterContext.hashOuter(readValues.getKey(i), hashContext) ^ (31 * value.first.hashValues(hashValues) + (value.second ? 1 : 0));
        }
        hash = hash * 31;
        for(int i=0,size=readParams.size();i<size;i++)
            hash += readParams.getKey(i).hashOuter(hashContext) ^ readParams.getValue(i).hashValues(hashValues);
        return hash;
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return readParams.equals(((PrereadRows<P>)o).readParams) && readValues.equals(((PrereadRows<P>)o).readValues);
    }

    public PrereadRows<P> add(PrereadRows<P> rows) {
        if(isEmpty()) // оптимизация
            return rows;
        if(rows.isEmpty()) // оптимизация
            return this;
        return new PrereadRows<P>(readParams.override(rows.readParams), readValues.override(rows.readValues));
    }

    public PrereadRows<P> addExcl(PrereadRows<P> rows) {
        if(isEmpty()) // оптимизация
            return rows;
        if(rows.isEmpty()) // оптимизация
            return this;
        return new PrereadRows<P>(readParams.addExcl(rows.readParams), readValues.addExcl(rows.readValues));
    }

    @Override
    public String toString() {
        return readValues + ", " + readParams;
    }
}
