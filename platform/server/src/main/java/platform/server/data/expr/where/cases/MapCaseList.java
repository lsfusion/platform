package platform.server.data.expr.where.cases;

import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.data.expr.Expr;

public class MapCaseList<K> extends CaseList<ImMap<K, Expr>, ImMap<K, Expr>,MapCase<K>> {

    public MapCaseList(ImList<MapCase<K>> mapCases) {
        super(mapCases);
    }

    public MapCaseList(ImSet<MapCase<K>> mapCases) {
        super(mapCases);
    }

}
