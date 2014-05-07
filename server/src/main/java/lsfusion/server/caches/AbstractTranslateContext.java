package lsfusion.server.caches;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.lru.LRUWVWSMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.col.lru.LRUWSVSMap;
import lsfusion.server.caches.hash.HashObject;
import lsfusion.server.data.Value;
import lsfusion.server.data.translator.MapObject;

import java.lang.ref.WeakReference;

// assert что T instanceof AbstractTranslateContext но заколебешься его протаскивать
public abstract class AbstractTranslateContext<T, M extends MapObject, H extends HashObject> extends AbstractHashContext<H> implements PackInterface<T> {

    // аспекты транслирования
//    private WeakReference<T> from;
//    private WeakReference<M> translator;
//    public T getFrom() {
//        if(from==null) {
//            translator = null;
//            return null;
//        } else {
//            T result = from.get();
//            if(result==null) {
//                from = null;
//                translator = null;
//            }
//            return result;
//        }
//    }
//    public M getTranslator() {
//        if(translator==null) {
//            from = null;
//            return null;
//        } else {
//            M result = translator.get();
//            if(result==null) {
//                from = null;
//                translator = null;
//            }
//            return result;
//        }
//    }
//    public void initTranslate(T from, M translator) {
//        assert from!=this; // identity должен отрубать
//        this.from = new WeakReference<T>(from);
//        this.translator = new WeakReference<M>(translator);
//    }

    boolean translated;
    // аспекты транслирования
    public LRUWVWSMap.Value<M, T> getFromValue() {
        if(translated) {
            LRUWVWSMap.Value<M, T> result = BaseUtils.<LRUWVWSMap.Value<M, T>>immutableCast(transFrom.get(this));
            if(result==null) {
                translated = false;
                return LRUWVWSMap.notFound(); 
            }
            return result;
        }
        return LRUWVWSMap.notFound();
    }

    public void initTranslate(T from, M translator) {
        assert from!=this; // identity должен отрубать
        transFrom.put(this, translator, (AbstractTranslateContext) from);
        translated = true;
    }

    private final static LRUWVWSMap<AbstractTranslateContext, MapObject, AbstractTranslateContext> transFrom = new LRUWVWSMap<AbstractTranslateContext, MapObject, AbstractTranslateContext>(LRUUtil.L2);
    private final static LRUWSVSMap<MapObject, AbstractTranslateContext, AbstractTranslateContext> transCache = new LRUWSVSMap<MapObject, AbstractTranslateContext, AbstractTranslateContext>(LRUUtil.L2);
    
    protected abstract T aspectContextTranslate(M translator);
    @ManualLazy
    protected T aspectTranslate(M translator) {
        if(isComplex()) {
            AbstractTranslateContext<T, M, H> cacheResult;
            
            cacheResult = transCache.get(translator, this);
            if(cacheResult==null) {
                cacheResult = (AbstractTranslateContext<T, M, H>) aspectContextTranslate(translator);
                if(cacheResult!=this)
                    cacheResult.initTranslate((T) this, translator);
                transCache.put(translator, this, cacheResult);
            }
            return (T) cacheResult;
        } else
            return translate(translator);
    }
    protected abstract T translate(M translator);

    protected ImSet<Value> values;
    @ManualLazy
    protected ImSet<Value> aspectGetValues() {
        if(values==null)
            values = getValues();
        return values;
    }
    public abstract ImSet<Value> getValues(); // по сути protected

    public T calculatePack() {
        throw new RuntimeException("not supported yet");
    }
    public T packed;
    @ManualLazy
    public T pack() {
        if(packed==null) {
            T calcPacked = calculatePack();
            ((AbstractTranslateContext)calcPacked).packed = calcPacked;
            packed = calcPacked;
        }
        return packed;
    }

    private Long outerComplexity;
    private Long complexity;
    @ManualLazy
    public long getComplexity(boolean outer) {
        if(outer) {
            if(outerComplexity == null)
                outerComplexity = calculateComplexity(outer);
            return outerComplexity;
        } else {
            if(complexity == null)
                complexity = calculateComplexity(outer);
            return complexity;
        }
    }
    
    protected long calculateComplexity(boolean outer) {
        throw new RuntimeException("not supported yet");
    }

    private final static GetValue<Object, PackInterface<Object>> packValue = new GetValue<Object, PackInterface<Object>>() {
        public Object getMapValue(PackInterface<Object> value) {
            return value.pack();
        }};
    private static <T extends PackInterface<T>> GetValue<T, T> packValue() {
        return BaseUtils.immutableCast(packValue);
    }

    public static <K, T extends PackInterface<T>> ImMap<K, T> pack(ImMap<K, T> map) {
        return map.mapValues(AbstractTranslateContext.<T>packValue());
    }

    public static <T extends PackInterface<T>> ImList<T> pack(ImList<T> exprs) {
        return exprs.mapListValues(AbstractTranslateContext.<T>packValue());
    }

    public static <T extends PackInterface<T>> ImSet<T> pack(ImSet<T> exprs) {
        return exprs.mapMergeSetValues(AbstractTranslateContext.<T>packValue());
    }

    public static <T extends PackInterface<T>> ImOrderMap<T, Boolean> pack(ImOrderMap<T, Boolean> map) {
        return map.mapMergeOrderKeys(AbstractTranslateContext.<T>packValue());
    }
}
