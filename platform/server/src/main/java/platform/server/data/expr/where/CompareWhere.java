package platform.server.data.expr.where;

import platform.server.data.query.Context;
import platform.server.data.query.JoinData;
import platform.server.data.expr.AndExpr;
import platform.server.data.where.DataWhere;
import platform.server.data.where.OrWhere;
import platform.server.data.where.Where;


public abstract class CompareWhere extends DataWhere {

    public final AndExpr operator1;
    public final AndExpr operator2;

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
        return  OrWhere.orTrue(operator1.getWhere(),where) &&
                OrWhere.orTrue(operator2.getWhere(),where) &&
                GreaterWhere.create(operator2, operator1).means(where) &&
                (this instanceof GreaterWhere?EqualsWhere.create(operator1, operator2):GreaterWhere.create(operator1, operator2)).means(where);
    }
}
