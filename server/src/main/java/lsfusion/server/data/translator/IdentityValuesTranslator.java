package lsfusion.server.data.translator;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.hash.HashCodeValues;
import lsfusion.server.caches.hash.HashValues;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.ValueExpr;

public class IdentityValuesTranslator extends MapValuesTranslator {

    private final ImSet<Value> values;

    public IdentityValuesTranslator(ImSet<Value> values) {
        this.values = values;
    }

    public int hash(HashValues hashValues) {
        int hash = 0;
        for(int i=0,size=values.size();i<size;i++) {
            Value value = values.get(i);
            hash += value.hashCode() ^ hashValues.hash(value);
        }
        return hash;
    }

    public ImSet<Value> getValues() {
        return values;
    }

    public <V extends Value> V translate(V expr) {
        return expr;
    }

    public MapValuesTranslator onlyKeys() {
        return this;
    }

    public boolean identityValues(ImSet<? extends Value> values) {
        assert this.values.containsAll(values);
        return true;
    }

    public MapValuesTranslate mapTrans(MapValuesTranslate map) {
        return map;
    }

    public MapValuesTranslate filter(ImSet<? extends Value> values) {
        assert this.values.containsAll(values);
        return new IdentityValuesTranslator((ImSet<Value>) values);
    }

    public boolean assertValuesContains(ImSet<? extends Value> values) {
        return ValueExpr.noStaticContains(this.values, values);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return values.equals(((IdentityValuesTranslator)o).values);
    }

    public int immutableHashCode() {
        return values.hashCode();
    }

    public MapValuesTranslate reverse() {
        return this;
    }

    public MapTranslate reverseMap() {
        return this;
    }

    public HashValues getHashValues() {
        return HashCodeValues.instance;
    }

}
