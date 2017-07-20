package lsfusion.server.caches;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.Value;
import lsfusion.server.data.translator.MapTranslate;

public interface InnerContext<I extends InnerContext<I>> extends InnerHashContext, PackInterface<I>, ValuesContext<I> {

    I translateInner(MapTranslate translate);

    ImSet<Value> getInnerValues();

    BaseUtils.HashComponents<ParamExpr> getInnerComponents(boolean values);

    boolean equalsInner(I object); // проверка на соответствие если одинаковые контексты, на самом деле protected
}
