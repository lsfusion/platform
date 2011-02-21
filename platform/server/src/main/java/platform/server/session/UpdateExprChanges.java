package platform.server.session;

import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.data.expr.Expr;
import platform.server.data.query.Join;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.Value;
import platform.server.classes.ValueClass;
import platform.server.classes.BaseClass;
import platform.server.logics.property.DataProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.caches.hash.HashValues;
import platform.server.caches.IdentityLazy;
import platform.server.caches.AbstractMapValues;
import platform.base.BaseUtils;
import platform.base.TwinImmutableInterface;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

public class UpdateExprChanges implements ExprChanges {
    private final SimpleChanges changes;

    public UpdateExprChanges(SimpleChanges changes) {
        this.changes = changes;
    }

    // предполагается что все до getUsedChanges вызывается только для IncrementUpdate в FormInstance при проходе через MaxChange и Cycle
    // проблема в том что кэшингу мешает
    public Where getIsClassWhere(Expr expr, ValueClass isClass, WhereBuilder changedWheres) {
        return expr.isClass(isClass.getUpSet());
    }

    public Expr getIsClassExpr(Expr expr, BaseClass baseClass, WhereBuilder changedWheres) {
        return expr.classExpr(baseClass);
    }

    public Join<String> getDataChange(DataProperty property, Map<ClassPropertyInterface, ? extends Expr> joinImplement) {
        return null;
    }

    public Where getRemoveWhere(ValueClass valueClass, Expr expr) {
        return Where.FALSE;
    }

    private static class UpdateMapValues extends AbstractMapValues<UpdateMapValues> {

        public boolean twins(TwinImmutableInterface o) {
            return true;
        }

        public int hashValues(HashValues hashValues) {
            return 23323;
        }

        public Set<Value> getValues() {
            return new HashSet<Value>();
        }

        public UpdateMapValues translate(MapValuesTranslate mapValues) {
            return this;
        }
    }
    private static final UpdateMapValues update = new UpdateMapValues();

    @IdentityLazy
    public SimpleChanges getUsedChanges() {
        return new SimpleChanges(changes, update);
    }
}
