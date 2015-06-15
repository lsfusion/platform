package lsfusion.server.data.expr;

import lsfusion.base.GlobalInteger;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.SystemProperties;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.where.Where;

public class KeyExpr extends ParamExpr {

    private static final GetValue<KeyExpr, Object> genStringKeys = new GetValue<KeyExpr, Object>() {
        public KeyExpr getMapValue(Object value) {
            return new KeyExpr(value.toString());
        }
    };
    private static final GetIndex<KeyExpr> genIndexKeys = new GetIndex<KeyExpr>() {
        public KeyExpr getMapValue(int i) {
            return new KeyExpr(i);
        }
    };
    public static <T> ImRevMap<T, KeyExpr> getMapKeys(ImSet<T> objects) {
        if (SystemProperties.isDebug)
            return objects.mapRevValues((GetValue<KeyExpr, T>) genStringKeys);

        return objects.mapRevValues(genIndexKeys);
    }

    final Object name;
    @Override
    public String toString() {
        return name.toString();
    }

    public KeyExpr(String name) {
        this.name = name;
    }

    public KeyExpr(int id) {
        this.name = id;
    }

    public String getSource(CompileSource compile) {
        String source = compile.getSource(this);
        assert source!=null;
        return source;
    }

    public void fillAndJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
    }

    public boolean isIndexed() {
        return true;
    }

    private final static GlobalInteger keyClass = new GlobalInteger(39916801);

    public GlobalInteger getKeyClass() {
        return keyClass;
    }
}
