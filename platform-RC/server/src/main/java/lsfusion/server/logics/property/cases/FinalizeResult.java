package lsfusion.server.logics.property.cases;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.PropertyInterfaceImplement;
import lsfusion.server.logics.property.cases.graph.Graph;

/**
* Created by User on 18.09.2014.
*/
public class FinalizeResult<F extends Case> {
    public final ImList<F> cases;
    public final boolean isExclusive;
    public final Graph<F> graph;

    public FinalizeResult(ImList<F> cases, boolean isExclusive, Graph<F> graph) {
        this.cases = cases;
        this.isExclusive = isExclusive;
        this.graph = graph;
    }
}
