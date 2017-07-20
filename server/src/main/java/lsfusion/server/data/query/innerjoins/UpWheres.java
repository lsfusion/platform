package lsfusion.server.data.query.innerjoins;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.WrapMap;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.query.stat.WhereJoin;
import lsfusion.server.data.where.Where;

public class UpWheres<J extends WhereJoin> extends WrapMap<J, UpWhere> {

    public UpWheres(ImMap<? extends J, ? extends UpWhere> map) {
        super(map);
    }

    public UpWheres(J key, UpWhere value) {
        super(key, value);
    }

    public UpWheres<J> filterUp(ImSet<J> joins) {
        return new UpWheres<J>(filterIncl(joins));
    }

    private static UpWheres EMPTY = new UpWheres(MapFact.EMPTY());
    public static <J extends WhereJoin> UpWheres<J> EMPTY() {
        return EMPTY;
    }

    public static final UpWhere TRUE = new UpWhere() {
        public UpWhere or(UpWhere upWhere) {
            return this;
        }

        public UpWhere and(UpWhere upWhere) {
            return upWhere;
        }

        public UpWhere not() {
            return FALSE;
        }

        public Where getWhere() {
            return Where.TRUE;
        }
    };
    public static final UpWhere FALSE = new UpWhere() {
        public UpWhere or(UpWhere upWhere) {
            return upWhere;
        }

        public UpWhere and(UpWhere upWhere) {
            return this;
        }

        public UpWhere not() {
            return TRUE;
        }

        public Where getWhere() {
            return Where.FALSE;
        }
    };
}
