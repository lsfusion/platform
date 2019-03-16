package lsfusion.server.data.expr.value;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.comb.map.GlobalObject;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.query.exec.materialize.NotMaterializable;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.type.FunctionType;
import lsfusion.server.data.type.TypeObject;
import lsfusion.server.data.type.exec.EnsureTypeEnvironment;
import lsfusion.server.data.type.parse.ParseInterface;
import lsfusion.server.data.value.Value;
import lsfusion.server.logics.classes.user.set.AndClassSet;

// по факту не nullable, но тут есть архитектурный нюанс, если не возвращать NotNull непонятно как класс заведомо определенный делать
// можно конечно смешать каким-то образом с StaticClassExpr, но не совсем понятно как это будет работать (тот же IsClassExpr все же возвращает конкретный класс)
public class StaticValueNullableExpr extends StaticNullableExpr implements Value {

    private final String object;
    private final Level level;

    // нужен собственно для предотвращения материализации + пересечения имен 
    public static class Level implements NotMaterializable {
        private final int level;

        public Level(int level) {
            this.level = level;
        }

        public boolean equals(Object o) {
            return this == o || o instanceof Level && level == (((Level) o).level);
        }

        public int hashCode() {
            return level + 5;
        }

    }  

    public StaticValueNullableExpr(AndClassSet paramClass, String object, Level level) {
        super(paramClass);
        this.object = object;
        this.level = level;
    }

    private static class ValueClass implements GlobalObject {
        private final AndClassSet paramClass;
        private final Level level;

        public ValueClass(AndClassSet paramClass, Level level) {
            this.paramClass = paramClass;
            this.level = level;
        }

        public boolean equals(Object o) {
            return this == o || o instanceof ValueClass && paramClass.equals(((ValueClass) o).paramClass) && level.equals(((ValueClass) o).level);
        }

        public int hashCode() {
            return 31 * level.hashCode() + paramClass.hashCode() + 5;
        }
    }
    
    private ValueClass valueClass = null;
    
    @Override
    public GlobalObject getValueClass() {
        if(valueClass == null)
            valueClass = new ValueClass(paramClass, level);
        return valueClass;
    }

    @Override
    public int getStaticEqualClass() {
        return -1; // так как используется для эмуляции значения из верхнего контекста, а значит разные StaticValueNullableExpr могут давать одинаковые значения
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return object.equals(((StaticValueNullableExpr)o).object) && getValueClass().equals(((StaticValueNullableExpr)o).getValueClass());
    }
    
    @Override
    public String getSource(CompileSource compile, boolean needValue) {
        compile.env.addNotMaterializable(level);
        return compile.params.get(this);
    }

    @Override
    public String toString() {
        return object + " - " + valueClass;
    }

    @Override
    protected StaticValueNullableExpr translate(MapTranslate translator) {
        return translator.translate(this);
    }

    @Override
    public int hash(HashContext hash) {
        return hash.values.hash(this);
    }

    @Override
    public int immutableHashCode() {
        return object.hashCode()+getValueClass().hashCode() * 31;
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
        assert false; // по идее не должен выполняться в текущих использованиях
        return new TypeObject(object, paramClass.getType());
    }

    @Override
    public ImSet<Value> getValues() {
        return SetFact.<Value>singleton(this);
    }

    @Override
    public boolean isAlwaysSafeString() {        
        return true;
    }

    @Override
    public FunctionType getFunctionType() {
        return paramClass.getType();
    }
}