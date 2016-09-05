package lsfusion.server.data.expr;

import lsfusion.base.GlobalInteger;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.data.expr.query.StatType;
import lsfusion.server.data.expr.where.NotNullWhere;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.query.innerjoins.GroupJoinsWheres;
import lsfusion.server.data.query.innerjoins.UpWhere;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.where.DataWhere;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;

public class NullableKeyExpr extends ParamExpr implements NullableExprInterface {

    private final int ID;
    public NullableKeyExpr(int ID) {
        this.ID = ID;
    }

    @Override
    public void fillAndJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
        throw new RuntimeException("not supported");
    }

    @Override
    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return "I_" + ID;
        throw new RuntimeException("not supported");
    }

    public class NotNull extends NotNullWhere {

        protected BaseExpr getExpr() {
            return NullableKeyExpr.this;
        }

        public ClassExprWhere calculateClassWhere() {
            return ClassExprWhere.TRUE;
        }

        public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, StatType statType, KeyStat keyStat, ImOrderSet<Expr> orderTop, GroupJoinsWheres.Type type) {
            throw new RuntimeException("not supported");
        }

        @Override
        protected UpWhere getUpWhere() {
            throw new UnsupportedOperationException();
        }
    }

    public NotNull calculateNotNullWhere() {
        return new NotNull();
    }

    public void fillFollowSet(MSet<DataWhere> result) {
        NullableExpr.fillFollowSet(this, result);
    }

    public boolean hasNotNull() {
        return NullableExpr.hasNotNull(this);
    }

    // упрощенная копия аналогичного метода в NullableExpr
    public ImSet<NullableExprInterface> getExprFollows(boolean includeThis, boolean includeInnerWithoutNotNull, boolean recursive) {
        assert includeThis || recursive;
        if(includeThis) {
            return SetFact.<NullableExprInterface>singleton(this);
        }
        return SetFact.EMPTY();
    }

    @Override
    public boolean isIndexed() {
        throw new RuntimeException("not supported yet");
    }

    private final static GlobalInteger keyClass = new GlobalInteger(39316401);

    public GlobalInteger getKeyClass() {
        return keyClass;
    }
}

