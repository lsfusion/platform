package platform.server.logics;

import platform.base.TwinImmutableObject;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.mapvalue.GetStaticValue;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.expr.Expr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.instance.ObjectInstance;
import platform.server.session.SessionChanges;

public class NullValue extends ObjectValue<NullValue> {

    private NullValue() {
    }
    public static NullValue instance = new NullValue();

    public String getString(SQLSyntax syntax) {
        return SQLSyntax.NULL;
    }

    public boolean isString(SQLSyntax syntax) {
        return true;
    }

    public Expr getExpr() {
        return Expr.NULL;
    }
    public Expr getStaticExpr() {
        return Expr.NULL;
    }

    public Object getValue() {
        return null;
    }

    public Where order(Expr expr, boolean desc, Where orderWhere) {
        Where greater = expr.getWhere();
        if(desc)
            return greater.not().and(orderWhere);
        else
            return greater.or(orderWhere);
    }

    public boolean twins(TwinImmutableObject o) {
        return true;
    }

    protected int hash(HashValues hashValues) {
        return 0;
    }

    public ImSet<Value> getValues() {
        return SetFact.EMPTY();
    }

    protected NullValue translate(MapValuesTranslate mapValues) {
        return this;
    }

    public ObjectValue refresh(SessionChanges session) {
        return this;
    }

    @Override
    public boolean isNull() {
        return true;
    }

    public ImSet<ObjectInstance> getObjectInstances() {
        return SetFact.EMPTY();
    }

    public static <K> ImMap<K,ObjectValue> getMap(ImSet<K> keys) {
        return keys.mapValues(new GetStaticValue<ObjectValue>() {
            public ObjectValue getMapValue() {
                return NullValue.instance;
            }});
    }

    public <K> ClassWhere<K> getClassWhere(K key) {
        return ClassWhere.FALSE();
    }
}
