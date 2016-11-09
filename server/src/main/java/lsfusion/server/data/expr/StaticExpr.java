package lsfusion.server.data.expr;

import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.data.ParseValue;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.query.stat.InnerBaseJoin;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.ValueJoin;
import lsfusion.server.data.type.FunctionType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;

public abstract class StaticExpr<C extends ConcreteClass> extends StaticClassExpr {

    public final C objectClass;

    public StaticExpr(C objectClass) {
        this.objectClass = objectClass;
    }

    public ConcreteClass getStaticClass() {
        return objectClass;
    }

    public Type getType(KeyType keyType) {
        return objectClass.getType();
    }
    public Stat getTypeStat(KeyStat keyStat, boolean forJoin) {
        return objectClass.getTypeStat(forJoin);
    }

    public void fillAndJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
    }

    public PropStat getStatValue(KeyStat keyStat) {
        return PropStat.ONE;
    }
    public InnerBaseJoin<?> getBaseJoin() {
        return ValueJoin.instance;
    }

    public Type getType() {
        return objectClass.getType();
    }
    
    public FunctionType getFunctionType() {
        return getType();
    }
}
