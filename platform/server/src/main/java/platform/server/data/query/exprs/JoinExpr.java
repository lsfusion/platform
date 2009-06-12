package platform.server.data.query.exprs;

import platform.server.data.classes.where.*;
import platform.server.data.classes.BaseClass;
import platform.server.data.classes.ConcreteObjectClass;
import platform.server.data.query.*;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.query.translators.DirectTranslator;
import platform.server.data.query.translators.Translator;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.query.wheres.NotNullWhere;
import platform.server.data.query.wheres.IsClassWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

import java.util.Map;


public class JoinExpr<J,U> extends VariableClassExpr implements JoinData {
    public final U property;
    public final DataJoin<J,U> from;
    private final NotNullWhere notNull;

    public JoinExpr(DataJoin<J,U> iFrom,U iProperty) {
        from = iFrom;
        property = iProperty;
        notNull = new NotNullWhere(this);
    }

    public int fillContext(Context context, boolean compile) {
        return context.add(from,compile);
    }

    public Join getJoin() {
        return from;
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        joins.add(this, andWhere);
    }

    // для fillSingleSelect'а
    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return queryData.get(this);
    }

    public String toString() {
        return from.toString() + "." + property;
    }

    public Type getType(Where where) {
        return from.source.getType(property);
    }

    // возвращает Where без следствий
    protected Where calculateWhere() {
        return notNull;
    }

    public DataWhereSet getFollows() {
        return notNull.getFollows();
    }

    public SourceExpr getFJExpr() {
        return this;
    }

    public String getFJString(String exprFJ) {
        return exprFJ;
    }

    protected int getHash() {
        return from.hash()*31+ from.source.hashProperty(property);
    }

    // для кэша
    public boolean equals(SourceExpr expr, MapContext mapContext) {
        return expr instanceof JoinExpr && mapContext.equals(this, (JoinExpr) expr);
    }

    public ClassExprWhere joinClassWhere;
    public ClassExprWhere getClassWhere() {
        return joinClassWhere;
    }

    public SourceExpr translate(Translator translator) {
        return translator.translate(this);
    }

    public AndExpr translateAnd(DirectTranslator translator) {
        return translator.translate(this);
    }

    private OrObjectClassSet getSet() {
        OrObjectClassSet result = OrObjectClassSet.FALSE;
        for(AndClassExprWhere where : getWhere().getClassWhere().wheres)
            result = result.or(where.get(this).getOr());
        return result;
    }
    
    private SourceExpr classExpr;
    public SourceExpr getClassExpr(BaseClass baseClass) {
        if(classExpr==null) {
            ConcreteObjectClass singleClass = getSet().getSingleClass(baseClass);
            if(singleClass!=null)
                classExpr = new CaseExpr(getWhere(),singleClass.getIDExpr());
            else
                classExpr = new IsClassExpr(this,baseClass);
        }
        return classExpr;
    }

    private boolean intersect(ClassSet set) {
        for(AndClassExprWhere where : getWhere().getClassWhere().wheres)
            if(!where.get(this).and(set).isEmpty()) return true;
        return false;
    }

    public Where getIsClassWhere(ClassSet set) {
        // в принципе можно было бы проand'ить но нарушит инварианты конструирования внутри IsClassExpr(baseClass+ joinExpr)
        if(!intersect(set)) { // если не пересекается то false
            if(1==1) throw new RuntimeException("to test");
            return Where.FALSE;
        }
        if(getSet().containsAll(set.getOr())) // если set содержит все элементы, то достаточно просто что не null
            return getWhere();
        return new IsClassWhere(this,set);
    }

}
