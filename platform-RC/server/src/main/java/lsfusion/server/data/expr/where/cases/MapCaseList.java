package lsfusion.server.data.expr.where.cases;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.expr.Expr;

public class MapCaseList<K> extends CaseList<ImMap<K, Expr>, ImMap<K, Expr>,MapCase<K>> {

    public MapCaseList(ImList<MapCase<K>> mapCases) {
        super(mapCases);
    }

    public MapCaseList(ImSet<MapCase<K>> mapCases) {
        super(mapCases);
    }

}
