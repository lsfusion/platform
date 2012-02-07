package platform.server.caches;

import org.supercsv.cellprocessor.ParseInt;
import platform.base.*;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.expr.KeyExpr;
import platform.server.data.translator.MapTranslate;
import platform.server.data.type.ParseInterface;

import java.util.Map;
import java.util.Set;

public interface InnerContext<I extends InnerContext<I>> extends InnerHashContext, PackInterface<I>, ValuesContext<I>, TwinImmutableInterface {

    I translateInner(MapTranslate translate);

    QuickSet<Value> getInnerValues();

    BaseUtils.HashComponents<KeyExpr> getInnerComponents(boolean values);

    boolean equalsInner(I object); // проверка на соответствие если одинаковые контексты, на самом деле protected
}
