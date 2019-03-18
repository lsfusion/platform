package lsfusion.server.data.expr.value;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.comb.map.GlobalObject;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.classes.VariableSingleClassExpr;
import lsfusion.server.data.expr.join.base.ValueJoin;
import lsfusion.server.data.expr.join.inner.InnerBaseJoin;
import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.query.compile.FJData;
import lsfusion.server.data.stat.KeyStat;
import lsfusion.server.data.stat.PropStat;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.stat.StatType;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.type.FunctionType;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeObject;
import lsfusion.server.data.type.exec.EnsureTypeEnvironment;
import lsfusion.server.data.type.parse.ParseInterface;
import lsfusion.server.data.value.Value;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.classes.user.ConcreteObjectClass;

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
    public String getSource(CompileSource compile, boolean needValue) {
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
    public int hash(HashContext hash) {
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
    public void fillAndJoinWheres(MMap<FJData, Where> joins, Where andWhere) {
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
    public String toDebugString() {
        return toString();
    }

    @Override
    public ParseInterface getParseInterface(QueryEnvironment env, EnsureTypeEnvironment typeEnv) {
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
