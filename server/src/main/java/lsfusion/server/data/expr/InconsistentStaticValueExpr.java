package lsfusion.server.data.expr;

import lsfusion.base.GlobalObject;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.ConcreteObjectClass;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.query.StatType;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.query.stat.InnerBaseJoin;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.ValueJoin;
import lsfusion.server.data.translator.ExprTranslator;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.type.*;
import lsfusion.server.data.where.Where;

// вообще надо по хорошему через множественное наследование связать с ValueExpr, но т.к. это локальный case пока делать не будем
// класс нужен так как classExpr работает только с VariableClassExpr
public class InconsistentStaticValueExpr extends VariableSingleClassExpr implements Value, StaticExprInterface {
    
    public final ConcreteObjectClass objectClass;
    private final Object object;

    public InconsistentStaticValueExpr(ConcreteObjectClass objectClass, Object object) {
        this.objectClass = objectClass;
        this.object = object;
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return object.equals(((InconsistentStaticValueExpr)o).object) && objectClass.equals(((InconsistentStaticValueExpr)o).objectClass);
    }

    @Override
    public String getSource(CompileSource compile) {
        return compile.params.get(this);
    }

    @Override
    protected VariableSingleClassExpr translate(MapTranslate translator) {
        return translator.translate(this);
    }

    @Override
    public Type getType(KeyType keyType) {
        return ObjectType.instance;
    }

    @Override
    protected int hash(HashContext hash) {
        return hash.values.hash(this);
    }

    @Override
    public int immutableHashCode() {
        return object.hashCode()+objectClass.hashCode() * 31;
    }

    @Override
    protected Expr translate(ExprTranslator translator) {
        return this;
    }

    @Override
    public void fillAndJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
    }

    @Override
    public PropStat getStatValue(KeyStat keyStat, StatType type) {
        return PropStat.ONE;
    }

    @Override
    public InnerBaseJoin<?> getBaseJoin() {
        return ValueJoin.instance(this);
    }

    @Override
    public Stat getTypeStat(KeyStat keyStat, boolean forJoin) {
        return ObjectType.instance.getTypeStat(forJoin);
    }

    @Override
    public Value removeBig(MAddSet<Value> usedValues) {
        return null;
    }

    @Override
    public ParseInterface getParseInterface(QueryEnvironment env) {
        return new TypeObject(object, objectClass.getType());
    }

    @Override
    public ImSet<Value> getValues() {
        return SetFact.<Value>singleton(this);
    }

    @Override
    public GlobalObject getValueClass() {
        return objectClass;
    }

    @Override
    public boolean isAlwaysSafeString() {
        return true;
    }

    @Override
    public FunctionType getFunctionType() {
        return ObjectType.instance;
    }
}
