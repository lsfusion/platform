package lsfusion.server.data.stat;

import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.classes.IsClassType;
import lsfusion.server.data.expr.classes.VariableSingleClassExpr;
import lsfusion.server.data.expr.join.classes.ObjectClassField;
import lsfusion.server.data.expr.join.inner.InnerBaseJoin;
import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.query.compile.FJData;
import lsfusion.server.data.translator.ExprTranslator;
import lsfusion.server.data.translator.JoinExprTranslator;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.ValueClassSet;
import lsfusion.server.logics.classes.user.set.AndClassSet;

// нужно только для определения статистики для создания связи ключ-ключ
public class KeyJoinExpr extends BaseExpr implements InnerBaseJoin<Object> {
    
    private final BaseExpr expr;

    public KeyJoinExpr(BaseExpr expr) {
        this.expr = expr;
    }
    
    public BaseExpr getBaseExpr() {
        return expr;
    }

    @Override
    protected boolean calcTwins(TwinImmutableObject o) {
        return expr.equals(((KeyJoinExpr)o).expr);
    }

    @Override
    public int immutableHashCode() {
        return expr.hashCode() + 13;
    }

    @Override
    protected Expr translate(ExprTranslator translator) {
        assert translator instanceof JoinExprTranslator;
        return expr.translateExpr(translator);
    }

    @Override
    public Stat getTypeStat(KeyStat keyStat, boolean forJoin) {
        return expr.getTypeStat(keyStat, forJoin);
    }

    @Override
    public Type getType(KeyType keyType) {
        assert false;
        return expr.getType(keyType);
    }

    @Override
    public PropStat getStatValue(KeyStat keyStat, StatType type) {
        return PropStat.ALOT; // expr.getStatValue(keyStat, type);
    }

    // JOIN'ы - собственно ради этого все и делается
    @Override
    public KeyJoinExpr getBaseJoin() {
        return this;
    }

    @Override
    public ImMap<Object, BaseExpr> getJoins() {
        return MapFact.singleton((Object)0, expr);
    }

    @Override
    public StatKeys<Object> getStatKeys(KeyStat keyStat, StatType type, boolean oldMech) {
        return new StatKeys<>(SetFact.singleton((Object)0), Stat.ALOT);
    }

    @Override
    public Cost getPushedCost(KeyStat keyStat, StatType type, Cost pushCost, Stat pushStat, ImMap<Object, Stat> pushKeys, ImMap<Object, Stat> pushNotNullKeys, ImMap<BaseExpr, Stat> pushProps, Result<ImSet<Object>> rPushedKeys, Result<ImSet<BaseExpr>> rPushedProps) {
        if(pushProps.isEmpty() && pushKeys.isEmpty())
            return Cost.ALOT;
        return pushCost;
    }

    // КОМПИЛЯЦИЯ / КЭШИРОВАНИЕ

    @Override
    public int hash(HashContext hash) {
        assert false;
        return expr.hashOuter(hash);
    }

    @Override
    protected BaseExpr translate(MapTranslate translator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSource(CompileSource compile, boolean needValue) {
        assert compile instanceof ToString;
        return "KJ - " + expr.getSource(compile, needValue);
    }

    @Override
    public void fillAndJoinWheres(MMap<FJData, Where> joins, Where andWhere) {
        assert false;
        expr.fillAndJoinWheres(joins, andWhere);
    }

    // КЛАССЫ
    
    @Override
    public Expr classExpr(ImSet<ObjectClassField> classes, IsClassType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassExprWhere getClassWhere(AndClassSet classes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Where isClass(ValueClassSet set, boolean inconsistent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AndClassSet getAndClassSet(ImMap<VariableSingleClassExpr, AndClassSet> and) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAndClassSet(MMap<VariableSingleClassExpr, AndClassSet> and, AndClassSet add) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConcreteClass getStaticClass() {
        throw new UnsupportedOperationException();
    }
}
