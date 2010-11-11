package platform.server.caches;

import platform.base.Result;
import platform.server.data.expr.ValueExpr;
import platform.server.data.translator.MapTranslate;

import java.util.Set;

public abstract class InnerContext<I extends InnerContext<I>> extends InnerHashContext {

    public abstract Set<ValueExpr> getValues();
    
    public abstract I translateInner(MapTranslate translate);
    // проверка на соответствие если одинаковые контексты
    public abstract boolean equalsInner(I object);

    public MapTranslate mapInner(I object, boolean values) {
        Result<MapTranslate> mapTranslate = new Result<MapTranslate>();
        if(mapInner(object, values, mapTranslate)!=null)
            return mapTranslate.result;
        else
            return null;
    }

    public I mapInner(I object, boolean values, Result<MapTranslate> mapTranslate) {
        for(MapTranslate translator : new MapParamsIterable(this, object, values)) {
            I transContext = translateInner(translator);
            if(transContext.equalsInner(object)) {
                mapTranslate.set(translator);
                return transContext;
            }
        }
        return null;
    }
}
