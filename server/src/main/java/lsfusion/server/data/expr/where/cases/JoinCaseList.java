package lsfusion.server.data.expr.where.cases;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.translate.MapValuesTranslate;
import lsfusion.server.data.where.Where;

import java.util.function.Function;

public class JoinCaseList<U> extends CaseList<Join<U>, Join<U>,JoinCase<U>> {

    public JoinCaseList(ImList<JoinCase<U>> joinCases) {
        super(joinCases);
    }

    public JoinCaseList(ImSet<JoinCase<U>> joinCases) {
        super(joinCases);
    }

    public Expr getExpr(U property) {
        MExprCaseList result = new MExprCaseList(exclusive);
        for(JoinCase<U> joinCase : this)
            result.add(joinCase.where,joinCase.data.getExpr(property));
        return result.getFinal();
    }

    public JoinCaseList(Where where,Join<U> join) {
        this(SetFact.<JoinCase<U>>singleton(new JoinCase<>(where, join)));
    }

    public Where getWhere() {
        return getWhere(Join::getWhere);
    }

    public JoinCaseList<U> translateRemoveValues(final MapValuesTranslate translate) {
        final MapTranslate translator = translate.mapKeys();
        Function<JoinCase<U>, JoinCase<U>> transCase = exprCase -> new JoinCase<>(exprCase.where.translateOuter(translator), exprCase.data.translateRemoveValues(translate));

        if(exclusive)
            return new JoinCaseList<>(((ImSet<JoinCase<U>>) list).mapSetValues(transCase));
        else
            return new JoinCaseList<>(((ImList<JoinCase<U>>) list).mapListValues(transCase));
    }
}
