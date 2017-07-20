package lsfusion.server.data.expr.where.cases;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.where.Where;

public class JoinCaseList<U> extends CaseList<Join<U>, Join<U>,JoinCase<U>> {

    public ImSet<U> properties;
    public JoinCaseList(ImList<JoinCase<U>> joinCases, ImSet<U> properties) {
        super(joinCases);
        this.properties = properties;
    }

    public JoinCaseList(ImSet<JoinCase<U>> joinCases, ImSet<U> properties) {
        super(joinCases);
        this.properties = properties;
    }

    public Expr getExpr(U property) {
        MExprCaseList result = new MExprCaseList(exclusive);
        for(JoinCase<U> joinCase : this)
            result.add(joinCase.where,joinCase.data.getExpr(property));
        return result.getFinal();
    }

    public JoinCaseList(Where where,Join<U> join) {
        this(SetFact.<JoinCase<U>>singleton(new JoinCase<>(where, join)), join.getProperties());
    }

    public Where getWhere() {
        return getWhere(new GetValue<Where, Join<U>>() {
            public Where getMapValue(Join<U> cCase) {
                return cCase.getWhere();
           }
        });
    }

    public JoinCaseList<U> translateRemoveValues(final MapValuesTranslate translate) {
        final MapTranslate translator = translate.mapKeys();
        GetValue<JoinCase<U>, JoinCase<U>> transCase = new GetValue<JoinCase<U>, JoinCase<U>>() {
            public JoinCase<U> getMapValue(JoinCase<U> exprCase) {
                return new JoinCase<>(exprCase.where.translateOuter(translator), exprCase.data.translateRemoveValues(translate));
            }
        };

        if(exclusive)
            return new JoinCaseList<>(((ImSet<JoinCase<U>>) list).mapSetValues(transCase), properties);
        else
            return new JoinCaseList<>(((ImList<JoinCase<U>>) list).mapListValues(transCase), properties);
    }
}
