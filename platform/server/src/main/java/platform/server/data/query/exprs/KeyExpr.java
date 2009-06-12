package platform.server.data.query.exprs;

import platform.server.data.query.*;
import platform.server.data.query.translators.DirectTranslator;
import platform.server.data.query.translators.Translator;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.query.wheres.IsClassWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.data.classes.BaseClass;
import platform.server.data.classes.where.ClassSet;
import platform.server.where.Where;

import java.util.Map;

public class KeyExpr extends VariableClassExpr implements QueryData {

    final String name;
    @Override
    public String toString() {
        return name;
    }

    public KeyExpr(String iName) {
        name = iName;
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return queryData.get(this);
    }

    public int fillContext(Context context, boolean compile) {
        context.keys.add(this);
        return -1;
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

    // для кэша
    public boolean equals(SourceExpr expr, MapContext mapContext) {
        return mapContext.keys.get(this).equals(expr);
    }

    public SourceExpr translate(Translator translator) {
        return translator.translate(this);
    }

    public KeyExpr translateAnd(DirectTranslator translator) {
        return translator.translate(this);
    }

    private IsClassExpr classExpr;
    public IsClassExpr getClassExpr(BaseClass baseClass) {
        if(classExpr==null)
            classExpr = new IsClassExpr(this,baseClass);
        return classExpr;
    }

    public Where getIsClassWhere(ClassSet set) {
        if(set.isEmpty())
            return Where.FALSE;
        else
            return new IsClassWhere(this,set);
    }

}
