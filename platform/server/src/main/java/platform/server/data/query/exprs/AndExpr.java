package platform.server.data.query.exprs;

import platform.interop.Compare;
import platform.server.data.classes.where.AndClassSet;
import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.query.JoinData;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.query.exprs.cases.CaseWhereInterface;
import platform.server.data.query.exprs.cases.ExprCaseList;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.query.wheres.EqualsWhere;
import platform.server.data.query.wheres.GreaterWhere;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.types.Reader;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;


public abstract class AndExpr extends SourceExpr {

    public static SourceExpr create(AndExpr expr) {
        if(expr.getWhere().getClassWhere().isFalse())
            return CaseExpr.NULL;
        else
            return expr;
    }

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

    public abstract AndExpr translateDirect(KeyTranslator translator);

    public abstract void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere);

    public SourceExpr followFalse(Where where) {
        AndExpr andFollow = andFollowFalse(where);
        if(andFollow==null)
            return CaseExpr.NULL;
        else
            return andFollow;
    }

    // возвращает null если по определению не верно
    public AndExpr andFollowFalse(Where where) {
        if(getWhere().means(where))
            return null;
        else
            return packFollowFalse(where);
    }

    // для linear'ов делает followFalse, известно что не means(where)
    public AndExpr packFollowFalse(Where where) {
        return this;
    }

    public abstract ClassExprWhere getClassWhere(AndClassSet classes);

    public Where compare(final SourceExpr expr, final Compare compare) {
        if(expr instanceof AndExpr) {
            switch(compare) {
                case EQUALS:
                    return EqualsWhere.create(this,(AndExpr)expr);
                case GREATER:
                    return GreaterWhere.create(this,(AndExpr)expr);
                case GREATER_EQUALS:
                    return GreaterWhere.create(this,(AndExpr)expr).or(EqualsWhere.create(this,(AndExpr)expr));
                case LESS:
                    return GreaterWhere.create((AndExpr)expr,this);
                case LESS_EQUALS:
                    return GreaterWhere.create((AndExpr)expr,this).or(EqualsWhere.create(this,(AndExpr)expr));
                case NOT_EQUALS: // оба заданы и не равно
                    return getWhere().and(expr.getWhere()).and(EqualsWhere.create(this,(AndExpr)expr).not());
            }
            throw new RuntimeException("should not be");
        } else {
            return expr.getCases().getWhere(new CaseWhereInterface<AndExpr>() {
                public Where getWhere(AndExpr cCase) {
                    return compare(cCase,compare);
                }
/*                @Override
                public Where getElse() {
                    if(compare==Compare.EQUALS || compare==Compare.NOT_EQUALS)
                        return Where.FALSE;
                    else // если не equals то нас устроит и просто не null
                        return AndExpr.this.getWhere();
                }*/
            });
        }
    }

    public AndExpr scale(int coeff) {
        if(coeff==1) return this;

        LinearOperandMap map = new LinearOperandMap();
        map.add(this,coeff);
        return map.getExpr();
    }

    public SourceExpr sum(AndExpr expr) {
        if(getWhere().means(expr.getWhere().not())) // если не пересекаются то возвращаем case
            return nvl(expr);
        
        LinearOperandMap map = new LinearOperandMap();
        map.add(this,1);
        map.add(expr,1);
        return map.getExpr();
    }

    public SourceExpr sum(SourceExpr expr) {
        if(expr instanceof AndExpr)
            return sum((AndExpr)expr);
        else
            return expr.sum(this);
    }
}
