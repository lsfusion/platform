package platform.server.data.query.exprs.cases;

import platform.server.data.types.Type;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.where.Where;

public class ExprCaseList extends CaseList<SourceExpr,ExprCase> {

    public ExprCaseList() {
    }
    public ExprCaseList(SourceExpr data) {
        super(data);
    }
    public ExprCaseList(Where where, SourceExpr data) {
        super(where, data);
    }
    public ExprCaseList(Where where, SourceExpr exprTrue, SourceExpr exprFalse) {
        super(where, exprTrue, exprFalse);
    }
    public ExprCaseList(Where falseWhere) {
        super(falseWhere);
    }

    ExprCase create(Where where, SourceExpr data) {
        return new ExprCase(where,data);
    }

    protected SourceExpr followWhere(Where where, SourceExpr data, Where upWhere) {
        return data.followFalse(upWhere.or(where.not()));
    }

    // возвращает CaseExpr
    public SourceExpr getExpr(Type type) {

        ExprCase lastCase;
        // срезаем null'ы
        while(size()>0) {
            lastCase = get(size()-1);
            Where nullWhere = lastCase.data.getWhere().not();
            lastCase.where = lastCase.where.followFalse(nullWhere);
            if(lastCase.where.isFalse()) {
                remove(size()-1);
            } else
                break;
        }

        // если не осталось элементов вернем просто NULL
        if(isEmpty())
            return type.getExpr(null);

        // если остался один его и возвращаем
        if(size()==1 && get(0).where.isTrue())
            return get(0).data;

        return new CaseExpr(this);
    }
}
