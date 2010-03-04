package platform.server.data.expr;

import platform.interop.Compare;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.query.JoinData;
import platform.server.data.expr.cases.CaseWhereInterface;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.expr.where.EqualsWhere;
import platform.server.data.expr.where.GreaterWhere;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.type.Reader;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;


public abstract class BaseExpr extends Expr {

    public static Expr create(BaseExpr expr) {
        if(expr.getWhere().getClassWhere().isFalse())
            return NULL;
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

    public abstract BaseExpr translateDirect(KeyTranslator translator);

    public abstract void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere);

    public Expr followFalse(Where where) {
        BaseExpr andFollow = andFollowFalse(where);
        if(andFollow==null)
            return NULL;
        else
            return andFollow;
    }

    // возвращает null если по определению не верно
    public BaseExpr andFollowFalse(Where where) {
        if(getWhere().means(where))
            return null;
        else
            return packFollowFalse(where);
    }

    // для linear'ов делает followFalse, известно что не means(where)
    public BaseExpr packFollowFalse(Where where) {
        return this;
    }

    public abstract ClassExprWhere getClassWhere(AndClassSet classes);

    public Where compare(final Expr expr, final Compare compare) {
        if(expr instanceof BaseExpr) {
            switch(compare) {
                case EQUALS:
                    return EqualsWhere.create(this,(BaseExpr)expr);
                case GREATER:
                    return GreaterWhere.create(this,(BaseExpr)expr);
                case GREATER_EQUALS:
                    return GreaterWhere.create(this,(BaseExpr)expr).or(EqualsWhere.create(this,(BaseExpr)expr));
                case LESS:
                    return GreaterWhere.create((BaseExpr)expr,this);
                case LESS_EQUALS:
                    return GreaterWhere.create((BaseExpr)expr,this).or(EqualsWhere.create(this,(BaseExpr)expr));
                case NOT_EQUALS: // оба заданы и не равно
                    return getWhere().and(expr.getWhere()).and(EqualsWhere.create(this,(BaseExpr)expr).not());
            }
            throw new RuntimeException("should not be");
        } else {
            return expr.getCases().getWhere(new CaseWhereInterface<BaseExpr>() {
                public Where getWhere(BaseExpr cCase) {
                    return compare(cCase,compare);
                }
/*                @Override
                public Where getElse() {
                    if(compare==Compare.EQUALS || compare==Compare.NOT_EQUALS)
                        return Where.FALSE;
                    else // если не equals то нас устроит и просто не null
                        return BaseExpr.this.getWhere();
                }*/
            });
        }
    }

    public BaseExpr scale(int coeff) {
        if(coeff==1) return this;

        LinearOperandMap map = new LinearOperandMap();
        map.add(this,coeff);
        return map.getExpr();
    }

    public Expr sum(BaseExpr expr) {
        if(getWhere().means(expr.getWhere().not())) // если не пересекаются то возвращаем case
            return nvl(expr);
        
        LinearOperandMap map = new LinearOperandMap();
        map.add(this,1);
        map.add(expr,1);
        return map.getExpr();
    }

    public Expr sum(Expr expr) {
        if(expr instanceof BaseExpr)
            return sum((BaseExpr)expr);
        else
            return expr.sum(this);
    }

    // проверка на статичность, временно потом более сложный алгоритм надо будет
    public boolean isValue() {
        return enumKeys(this).isEmpty();
    }

    public boolean hasKey(KeyExpr key) {
        return enumKeys(this).contains(key);
    }
}
