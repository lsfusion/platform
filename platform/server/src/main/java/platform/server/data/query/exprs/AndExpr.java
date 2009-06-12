package platform.server.data.query.exprs;

import platform.interop.Compare;
import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.classes.where.ClassSet;
import platform.server.data.classes.where.OrClassSet;
import platform.server.data.classes.where.ClassWhere;
import platform.server.data.query.JoinData;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.query.exprs.cases.ExprCaseList;
import platform.server.data.query.translators.DirectTranslator;
import platform.server.data.query.wheres.CompareWhere;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.types.Reader;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

import java.util.Collections;


public abstract class AndExpr extends SourceExpr {

    // получает список ExprCase'ов
    public ExprCaseList getCases() {
        return new ExprCaseList(this);
    }

    public abstract DataWhereSet getFollows();

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        fillAndJoinWheres(joins, andWhere.and(getWhere()));
    }

    public Reader getReader(Where where) {
        return getType(where); // assert'ится что не null
    }

    public abstract AndExpr translateAnd(DirectTranslator translator);

    public abstract void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere);

    public SourceExpr followFalse(Where where) {
        AndExpr andFollow = andFollowFalse(where);
        if(andFollow==null)
            return new CaseExpr();
        else
            return this;
    }

    // возвращает null если по определению не верно
    public AndExpr andFollowFalse(Where where) {
        if(getWhere().means(where))
            return null;
        else
            return linearFollowFalse(where);
    }

    // для linear'ов делает followFalse
    public AndExpr linearFollowFalse(Where where) {
        return this;
    }

    public abstract ClassExprWhere getClassWhere(ClassSet classes);

    public Where compare(SourceExpr expr, int compare) {
        if(expr instanceof AndExpr)
            return new CompareWhere(this,(AndExpr)expr,compare);
        else
            return expr.compare(this, Compare.reverse(compare));
    }

    public AndExpr scale(int coeff) {
        if(coeff==1) return this;

        LinearOperandMap map = new LinearOperandMap();
        map.add(this,coeff);
        return new LinearExpr(map);
    }

    public AndExpr sum(AndExpr expr) {
        LinearOperandMap map = new LinearOperandMap();
        map.add(this,1);
        map.add(expr,1);
        return new LinearExpr(map);
    }

    public SourceExpr sum(SourceExpr expr) {
        if(expr instanceof AndExpr)
            return sum((AndExpr)expr);
        else
            return expr.sum(this);
    }
}
