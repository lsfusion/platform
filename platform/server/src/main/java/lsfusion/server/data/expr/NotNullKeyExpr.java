package lsfusion.server.data.expr;

import lsfusion.base.GlobalInteger;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.data.expr.where.NotNullWhere;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.query.innerjoins.GroupJoinsWheres;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;

public class NotNullKeyExpr extends ParamExpr {

    private final int ID;
    public NotNullKeyExpr(int ID) {
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
            return NotNullKeyExpr.this;
        }

        public ClassExprWhere calculateClassWhere() {
            return ClassExprWhere.TRUE;
        }

        public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, KeyStat keyStat, ImOrderSet<Expr> orderTop, boolean noWhere) {
            throw new RuntimeException("not supported");
        }
    }

    public NotNull calculateNotNullWhere() {
        return new NotNull();
    }

    @Override
    public boolean isTableIndexed() {
        throw new RuntimeException("not supported yet");
    }

    private final static GlobalInteger keyClass = new GlobalInteger(39316401);

    public GlobalInteger getKeyClass() {
        return keyClass;
    }
}

