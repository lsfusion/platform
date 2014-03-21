package lsfusion.server.logics;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetStaticValue;
import lsfusion.server.caches.hash.HashValues;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.session.SessionChanges;

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

    public boolean calcTwins(TwinImmutableObject o) {
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

    public ObjectValue refresh(SessionChanges session, ValueClass upClass) {
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
