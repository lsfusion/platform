package lsfusion.server.logics;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetStaticValue;
import lsfusion.server.caches.hash.HashValues;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.Field;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.type.ParseInterface;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.session.SessionChanges;

import java.sql.PreparedStatement;
import java.sql.SQLException;

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

    private static class Parse implements ParseInterface {
        
        private final Type type;

        private Parse(Type type) {
            this.type = type;
        }

        public boolean isSafeString() {
            return true;
        }

        public String getString(SQLSyntax syntax, StringBuilder envString, boolean usedRecursion) {
            return SQLSyntax.NULL;
        }

        public void writeParam(PreparedStatement statement, SQLSession.ParamNum paramNum, SQLSyntax syntax) throws SQLException {
            type.writeNullParam(statement, paramNum, syntax);
        }

        public boolean isSafeType() {
            return false;
        }

        public Type getType() {
            return type;
        }

        public void checkSessionTable(SQLSession sql) {
        }
    }
    public ParseInterface getParse(Field field, SQLSyntax syntax) {
        return new Parse(field.type);
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public String getShortName() {
        return toString();
    }
}
