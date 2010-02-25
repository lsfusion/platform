package platform.server.data.expr;

import platform.server.classes.ConcreteClass;
import platform.server.classes.LogicalClass;
import platform.server.classes.IntegralClass;
import platform.server.data.query.*;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.Type;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;


public class ValueExpr extends StaticClassExpr {

    public final Object object;
    public final ConcreteClass objectClass;

    public ValueExpr(Object object, ConcreteClass objectClass) {
        this.object = object;
        this.objectClass = objectClass;

        assert !(this.objectClass instanceof LogicalClass && !this.object.equals(true));
    }

    public static ValueExpr TRUE = new ValueExpr(true,LogicalClass.instance);
    public static Expr get(Where where) {
        return TRUE.and(where);
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

    public void enumerate(SourceEnumerator enumerator) {
        enumerator.add(this);
    }

    public Type getType(Where where) {
        return objectClass.getType();
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    // возвращает Where без следствий
    public Where calculateWhere() {
        return Where.TRUE;
    }

    public boolean twins(AbstractSourceJoin o) {
        return object.equals(((ValueExpr)o).object) && objectClass.equals(((ValueExpr)o).objectClass);
    }

    @Override
    public int hashCode() {
        return object.hashCode()*31+objectClass.hashCode();
    }

    public int hashContext(HashContext hashContext) {
        return hashContext.hash(this);
    }

    // нельзя потому как при трансляции значения потеряются
/*    @Override
    public ValueExpr scale(int mult) {
        return new ValueExpr(((IntegralClass)objectClass).multiply((Number) object,mult),objectClass);
    }*/

    public ValueExpr translateQuery(QueryTranslator translator) {
        return translator.translate(this);
    }

    public BaseExpr translateDirect(KeyTranslator translator) {
        return translator.translate(this);
    }

    public DataWhereSet getFollows() {
        return new DataWhereSet();
    }
}
