package lsfusion.server.data.expr;

import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.data.expr.query.stat.PropStat;
import lsfusion.server.data.expr.query.stat.Stat;
import lsfusion.server.data.expr.query.stat.StatType;
import lsfusion.server.data.query.compile.FJData;
import lsfusion.server.data.expr.join.stat.InnerBaseJoin;
import lsfusion.server.data.expr.join.stat.KeyStat;
import lsfusion.server.data.expr.join.stat.ValueJoin;
import lsfusion.server.data.translator.ExprTranslator;
import lsfusion.server.data.type.FunctionType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.classes.ConcreteClass;

public abstract class StaticExpr<C extends ConcreteClass> extends StaticClassExpr implements StaticExprInterface {

    public final C objectClass;

    public StaticExpr(C objectClass) {
        this.objectClass = objectClass;
    }

    public ConcreteClass getStaticClass() {
        return objectClass;
    }

    public ConcreteClass getStaticClass(KeyType keyType) {
        return getStaticClass();
    }

    public Type getType(KeyType keyType) {
        return objectClass.getType();
    }
    public Stat getTypeStat(KeyStat keyStat, boolean forJoin) {
        return objectClass.getTypeStat(forJoin);
    }

    public void fillAndJoinWheres(MMap<FJData, Where> joins, Where andWhere) {
    }

    public PropStat getStatValue(KeyStat keyStat, StatType type) {
        return PropStat.ONE;
    }
    public InnerBaseJoin<?> getBaseJoin() {
        return ValueJoin.instance(this);
    }

    public Type getType() {
        return objectClass.getType();
    }
    
    public FunctionType getFunctionType() {
        return getType();
    }

    @Override
    public Expr translate(ExprTranslator translator) {
        return this;
    }
}
