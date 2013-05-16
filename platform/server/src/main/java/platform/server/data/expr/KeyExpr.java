package platform.server.data.expr;

import platform.base.GlobalInteger;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MMap;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.SystemProperties;
import platform.server.caches.ParamExpr;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.where.Where;

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

    public boolean isTableIndexed() {
        return true;
    }

    private final static GlobalInteger keyClass = new GlobalInteger(39916801);

    public GlobalInteger getKeyClass() {
        return keyClass;
    }
}
