package platform.server.data.expr;

import platform.base.GlobalInteger;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MMap;
import platform.server.caches.ParamExpr;
import platform.server.data.expr.where.NotNullWhere;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.query.innerjoins.GroupJoinsWheres;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

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

