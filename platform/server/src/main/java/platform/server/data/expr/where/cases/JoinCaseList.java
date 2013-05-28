package platform.server.data.expr.where.cases;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.data.expr.Expr;
import platform.server.data.query.Join;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;

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
        this(SetFact.<JoinCase<U>>singleton(new JoinCase<U>(where, join)), join.getProperties());
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
                return new JoinCase<U>(exprCase.where.translateOuter(translator), exprCase.data.translateRemoveValues(translate));
            }
        };

        if(exclusive)
            return new JoinCaseList<U>(((ImSet<JoinCase<U>>)list).mapSetValues(transCase), properties);
        else
            return new JoinCaseList<U>(((ImList<JoinCase<U>>)list).mapListValues(transCase), properties);
    }
}
