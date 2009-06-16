package platform.server.data.query.exprs;

import platform.server.data.classes.ConcreteClass;
import platform.server.data.classes.LogicalClass;
import platform.server.data.query.*;
import platform.server.data.query.translators.DirectTranslator;
import platform.server.data.query.translators.Translator;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.logics.DataObject;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

import java.util.Map;


public class ValueExpr extends StaticClassExpr implements QueryData {

    public final Object object;
    public final ConcreteClass objectClass;

    public ValueExpr(Object iObject, ConcreteClass iObjectClass) {
        object = iObject;
        objectClass = iObjectClass;

        assert !(objectClass instanceof LogicalClass && !object.equals(true));
    }

    public ValueExpr(DataObject value) {
        this(value.object,value.objectClass);
    }

    public ConcreteClass getStaticClass() {
        return objectClass;
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        String source = queryData.get(this);
        if(source==null) source = getString(syntax);
        return source;
//        return getString(Syntax);
    }

    public String getString(SQLSyntax syntax) {
        return objectClass.getType().getString(object, syntax);
    }

    public String toString() {
        return object + " - " + objectClass;
    }

    public int fillContext(Context context, boolean compile) {
        context.values.add(this);
        return -1;
    }

    public Type getType(Where where) {
        return objectClass.getType();
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    // возвращает Where без следствий
    protected Where calculateWhere() {
        return Where.TRUE;
    }

    public boolean equals(Object o) {
        return this==o || o instanceof ValueExpr && object.equals(((ValueExpr)o).object) && objectClass.equals(((ValueExpr)o).objectClass);
    }

    protected int getHashCode() {
        return object.hashCode()*31+objectClass.hashCode();
    }

    // для кэша
    public boolean equals(SourceExpr expr, MapContext mapContext) {
        return mapContext.values.get(this).equals(expr);
    }

    public ValueExpr scale(int mult) {
        return new ValueExpr(((Integer)object)*mult,objectClass);
    }

    public ValueExpr translate(Translator translator) {
        return translator.translate(this);
    }

    public AndExpr translateAnd(DirectTranslator translator) {
        return translator.translate(this);
    }

    public DataWhereSet getFollows() {
        return new DataWhereSet();
    }

    protected int getHash() {
        return 1;
    }
}
