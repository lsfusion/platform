package lsfusion.server.data.expr.key;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.comb.map.GlobalInteger;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.query.compile.FJData;
import lsfusion.server.data.where.Where;
import lsfusion.server.physics.admin.SystemProperties;

import java.util.function.Function;
import java.util.function.IntFunction;

public class KeyExpr extends ParamExpr {

    private static final Function<Object, KeyExpr> genStringKeys = value -> new KeyExpr(value.toString());
    private static final IntFunction<KeyExpr> genIndexKeys = KeyExpr::new;
    public static <T> ImRevMap<T, KeyExpr> getMapKeys(ImSet<T> objects) {
        if (SystemProperties.inDevMode)
            return objects.mapRevValues((Function<T, KeyExpr>) genStringKeys);

        return objects.mapRevValues(genIndexKeys);
    }

    private final Object name;
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

    public String getSource(CompileSource compile, boolean needValue) {
        String source = compile.getSource(this);
        assert source!=null;
        return source;
    }

    public void fillAndJoinWheres(MMap<FJData, Where> joins, Where andWhere) {
    }

    public boolean isIndexed() {
        return true;
    }

    private final static GlobalInteger keyClass = new GlobalInteger(39916801);

    public GlobalInteger getKeyClass() {
        return keyClass;
    }
}
