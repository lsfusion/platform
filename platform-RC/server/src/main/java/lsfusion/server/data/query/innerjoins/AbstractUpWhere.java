package lsfusion.server.data.query.innerjoins;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;
import lsfusion.server.data.query.stat.WhereJoin;
import lsfusion.server.data.where.Where;

public abstract class AbstractUpWhere<T extends AbstractUpWhere<T>> extends TwinImmutableObject<T> implements UpWhere {

    private static class Not extends AbstractUpWhere<Not> {

        private final UpWhere upWhere;

        public Not(UpWhere upWhere) {
            this.upWhere = upWhere;
        }

        @Override
        protected boolean calcTwins(TwinImmutableObject o) {
            return upWhere.equals(((Not)o).upWhere);
        }

        @Override
        public int immutableHashCode() {
            return upWhere.hashCode();
        }

        @Override
        public Where getWhere() {
            return upWhere.getWhere().not();
        }
    }

    public UpWhere not() {
        return new Not(this);
    }

    private static class Op extends AbstractUpWhere<Not> {

        private final UpWhere upWhere1;
        private final UpWhere upWhere2;
        private final boolean and;

        public Op(UpWhere upWhere1, UpWhere upWhere2, boolean and) {
            this.upWhere1 = upWhere1;
            this.upWhere2 = upWhere2;
            this.and = and;
        }

        @Override
        protected boolean calcTwins(TwinImmutableObject o) {
            return ((upWhere1.equals(((Op)o).upWhere1) && upWhere2.equals(((Op)o).upWhere2)) ||
                    (upWhere1.equals(((Op)o).upWhere2) && upWhere2.equals(((Op)o).upWhere1))) && and == ((Op)o).and;
        }

        @Override
        public int immutableHashCode() {
            return upWhere1.hashCode() + upWhere2.hashCode() + (and ? 1 : 0);
        }

        @Override
        public Where getWhere() {
            Where where1 = upWhere1.getWhere();
            Where where2 = upWhere2.getWhere();
            if(and)
                return where1.and(where2);
            else
                return where1.or(where2);
        }
    }

    @Override
    public UpWhere or(UpWhere upWhere) {
        return new Op(this, upWhere, false);
    }

    @Override
    public UpWhere and(UpWhere upWhere) {
        return new Op(this, upWhere, true);
    }

    private static SymmAddValue<Object, UpWhere> andInterface = new SymmAddValue<Object, UpWhere>() {
        @Override
        public UpWhere addValue(Object key, UpWhere prevValue, UpWhere newValue) {
            return prevValue.and(newValue);
        }
    };

    public static <T> SymmAddValue<T, UpWhere> and() {
        return (SymmAddValue<T, UpWhere>) andInterface;
    }

}
