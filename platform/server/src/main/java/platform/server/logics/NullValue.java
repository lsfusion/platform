package platform.server.logics;

import platform.base.QuickSet;
import platform.base.TwinImmutableInterface;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.expr.Expr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.instance.ObjectInstance;
import platform.server.session.SessionChanges;

import java.util.*;

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

    public boolean twins(TwinImmutableInterface o) {
        return true;
    }

    protected int hash(HashValues hashValues) {
        return 0;
    }

    public QuickSet<Value> getValues() {
        return QuickSet.EMPTY();
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

    public Collection<ObjectInstance> getObjectInstances() {
        return new ArrayList<ObjectInstance>();
    }

    public static <K> Map<K,ObjectValue> getMap(Collection<K> keys) {
        Map<K,ObjectValue> result = new HashMap<K, ObjectValue>();
        for(K object : keys)
            result.put(object, NullValue.instance);
        return result;
    }

    public <K> ClassWhere<K> getClassWhere(K key) {
        return ClassWhere.STATIC(false);
    }
}
