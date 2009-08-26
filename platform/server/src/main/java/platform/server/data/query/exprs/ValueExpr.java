package platform.server.data.query.exprs;

import platform.server.data.classes.ConcreteClass;
import platform.server.data.classes.LogicalClass;
import platform.server.data.query.*;
import platform.server.data.query.translators.QueryTranslator;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.logics.DataObject;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;


public class ValueExpr extends StaticClassExpr {

    public final Object object;
    public final ConcreteClass objectClass;

    public ValueExpr(Object iObject, ConcreteClass iObjectClass) {
        object = iObject;
        objectClass = iObjectClass;

        assert !(objectClass instanceof LogicalClass && !object.equals(true));
    }

    public ConcreteClass getStaticClass() {
        return objectClass;
    }

    public String getSource(CompileSource compile) {
        String source = compile.params.get(this);
        if(source==null) source = getString(compile.syntax);
        return source;
    }

    public String getString(SQLSyntax syntax) {
        return objectClass.getType().getString(object, syntax);
    }

    @Override
    public String toString() {
        return object + " - " + objectClass;
    }

    public void fillContext(Context context) {
        context.values.add(this);
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

    @Override
    public int hashCode() {
        return object.hashCode()*31+objectClass.hashCode();
    }

    public int hashContext(HashContext hashContext) {
        return hashContext.hash(this);
    }
    
    public ValueExpr scale(int mult) {
        return new ValueExpr(((Integer)object)*mult,objectClass);
    }

    public ValueExpr translateQuery(QueryTranslator translator) {
        return translator.translate(this);
    }

    public AndExpr translateDirect(KeyTranslator translator) {
        return translator.translate(this);
    }

    public DataWhereSet getFollows() {
        return new DataWhereSet();
    }
}
