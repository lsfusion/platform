package platform.server.caches;

import platform.server.data.expr.ValueExpr;
import platform.base.MapIterable;
import platform.base.Pairs;

import java.util.Map;
import java.util.Collection;
import java.util.Iterator;

public class ValuePairs extends MapIterable<Map<ValueExpr,ValueExpr>,Map<ValueExpr,ValueExpr>> {
    private final Collection<ValueExpr> values1;
    private final Collection<ValueExpr> values2;

    public ValuePairs(Collection<ValueExpr> values1, Collection<ValueExpr> values2) {
        this.values1 = values1;
        this.values2 = values2;
    }

    protected Map<ValueExpr, ValueExpr> map(Map<ValueExpr, ValueExpr> mapValues) {
        for(Map.Entry<ValueExpr,ValueExpr> mapValue : mapValues.entrySet())
            if(!(mapValue.getKey().objectClass.equals(mapValue.getValue().objectClass)))
                return null;
        return mapValues;
    }

    protected Iterator<Map<ValueExpr, ValueExpr>> mapIterator() {
        return new Pairs<ValueExpr,ValueExpr>(values1, values2).iterator();
    }
}
