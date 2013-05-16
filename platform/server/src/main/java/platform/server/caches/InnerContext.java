package platform.server.caches;

import platform.base.BaseUtils;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.data.Value;
import platform.server.data.translator.MapTranslate;

public interface InnerContext<I extends InnerContext<I>> extends InnerHashContext, PackInterface<I>, ValuesContext<I> {

    I translateInner(MapTranslate translate);

    ImSet<Value> getInnerValues();

    BaseUtils.HashComponents<ParamExpr> getInnerComponents(boolean values);

    boolean equalsInner(I object); // проверка на соответствие если одинаковые контексты, на самом деле protected
}
