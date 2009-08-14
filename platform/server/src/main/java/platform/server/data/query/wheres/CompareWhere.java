package platform.server.data.query.wheres;

import platform.interop.Compare;
import platform.server.data.classes.StringClass;
import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.query.*;
import platform.server.data.query.exprs.*;
import platform.server.data.query.translators.Translator;
import platform.server.where.DataWhere;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;
import platform.server.where.OrWhere;
import platform.server.caches.ParamLazy;


public abstract class CompareWhere extends DataWhere {

    public final AndExpr operator1;
    public final AndExpr operator2;

    protected static Where create(CompareWhere where) {
        if(where.getClassWhere().isFalse())
            return Where.FALSE;
        else
            return where;
    }

    protected CompareWhere(AndExpr iOperator1, AndExpr iOperator2) {
        operator1 = iOperator1;
        operator2 = iOperator2;
    }

    public void fillContext(Context context) {
        operator1.fillContext(context);
        operator2.fillContext(context);
    }

    public void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        operator1.fillJoinWheres(joins,andWhere);
        operator2.fillJoinWheres(joins,andWhere);
    }

    public boolean checkTrue(Where where) {
        // A>B = !(A=B) AND !(B>A) AND A AND B
        // A=B = !(A>B) AND !(B>A) AND A AND B
        return  GreaterWhere.create(operator2, operator1).means(where) &&
                OrWhere.orTrue(operator1.getWhere(),where) &&
                OrWhere.orTrue(operator2.getWhere(),where) &&
                (this instanceof GreaterWhere?EqualsWhere.create(operator1, operator2):GreaterWhere.create(operator1, operator2)).means(where);
//                OrWhere.orTrue(operator1.getWhere().orMeans(operator2.getWhere()),where);
    }
}
