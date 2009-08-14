package platform.server.data.query.exprs;

import platform.server.data.classes.BaseClass;
import platform.server.data.classes.where.AndClassSet;
import platform.server.data.query.*;
import platform.server.data.query.translators.QueryTranslator;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.query.wheres.IsClassWhere;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.types.Type;
import platform.server.where.Where;
import platform.server.where.DataWhereSet;

public class KeyExpr extends VariableClassExpr {

    final String name;
    @Override
    public String toString() {
        return name;
    }

    public KeyExpr(String iName) {
        name = iName;
    }

    public String getSource(CompileSource compile) {
        return compile.keySelect.get(this);
    }

    public void fillContext(Context context) {
        context.keys.add(this);
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    public Type getType(Where where) {
        return where.getClassWhere().getType(this);
    }

    // возвращает Where без следствий
    protected Where calculateWhere() {
        return Where.TRUE;
    }

    public SourceExpr translateQuery(QueryTranslator translator) {
        return translator.translate(this);
    }

    public KeyExpr translateDirect(KeyTranslator translator) {
        return translator.translate(this);
    }

    public DataWhereSet getFollows() {
        return new DataWhereSet();
    }

    private IsClassExpr classExpr;
    public IsClassExpr getClassExpr(BaseClass baseClass) {
        if(classExpr==null)
            classExpr = new IsClassExpr(this,baseClass);
        return classExpr;
    }

    public Where getIsClassWhere(AndClassSet set) {
        if(set.isEmpty())
            return Where.FALSE;
        else
            return new IsClassWhere(this,set);
    }


    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    public int hashContext(HashContext hashContext) {
        return hashContext.hash(this);
    }
}
