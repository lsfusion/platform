package platform.server.data.expr.where.cases;

import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.Expr;
import platform.server.data.translator.MapTranslate;

public class ExprCaseList extends CaseList<Expr, Expr, ExprCase> {

    public ExprCaseList(ImList<ExprCase> exprCases) {
        super(exprCases);
    }

    public ExprCaseList(ImSet<ExprCase> exprCases) {
        super(exprCases);
    }

    public int hashOuter(HashContext hashContext) {
        int hash = exclusive ? 1 : 0;
        for(ExprCase exprCase : this)
            hash = 31*hash + exprCase.hashOuter(hashContext);
        return hash;
    }

    public long getComplexity(boolean outer) {
        long complexity = 0;
        for(ExprCase exprCase : this)
            complexity += exprCase.getComplexity(outer);
        return complexity;
    }

    public ExprCaseList translateOuter(final MapTranslate translate) {
        GetValue<ExprCase, ExprCase> transCase = new GetValue<ExprCase, ExprCase>() {
            public ExprCase getMapValue(ExprCase exprCase) {
                return new ExprCase(exprCase.where.translateOuter(translate), exprCase.data.translateOuter(translate));
            }};

        if(exclusive)
            return new ExprCaseList(((ImSet<ExprCase>)list).mapSetValues(transCase));
        else
            return new ExprCaseList(((ImList<ExprCase>)list).mapListValues(transCase));
    }

    public <K> MapCaseList<K> mapValues(GetValue<MapCase<K>, ExprCase> mapValue) {
        if(exclusive)
            return new MapCaseList<K>(((ImSet<ExprCase>)list).mapSetValues(mapValue));
        else
            return new MapCaseList<K>(((ImList<ExprCase>)list).mapListValues(mapValue));
    }
}
