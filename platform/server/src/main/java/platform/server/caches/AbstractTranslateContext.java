package platform.server.caches;

import platform.base.BaseUtils;
import platform.base.TwinImmutableObject;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.hash.HashCodeKeys;
import platform.server.caches.hash.HashCodeValues;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashObject;
import platform.server.data.Value;
import platform.server.data.translator.MapObject;

import java.lang.ref.WeakReference;

// assert что T instanceof AbstractTranslateContext но заколебешься его протаскивать
public abstract class AbstractTranslateContext<T, M extends MapObject, H extends HashObject> extends TwinImmutableObject implements PackInterface<T> {

    // аспекты транслирования
    private WeakReference<T> from;
    private WeakReference<M> translator;
    public T getFrom() {
        if(from==null) {
            translator = null;
            return null;
        } else {
            T result = from.get();
            if(result==null) {
                from = null;
                translator = null;
            }
            return result;
        }
    }
    public M getTranslator() {
        if(translator==null) {
            from = null;
            return null;
        } else {
            M result = translator.get();
            if(result==null) {
                from = null;
                translator = null;
            }
            return result;
        }
    }
    public void initTranslate(T from, M translator) {
        assert from!=this; // identity должен отрубать
        this.from = new WeakReference<T>(from);
        this.translator = new WeakReference<M>(translator);
    }

    protected boolean isComplex() { // замена аннотации
        return false;
    }

    protected abstract T aspectContextTranslate(M translator);
    @ManualLazy
    protected T aspectTranslate(M translator) {
        if(isComplex()) {
            AbstractTranslateContext<T, M, H> cacheResult = translator.aspectGetCache(this);
            if(cacheResult==null) {
                cacheResult = (AbstractTranslateContext<T, M, H>) aspectContextTranslate(translator);
                if(cacheResult!=this)
                    cacheResult.initTranslate((T) this, translator);
                translator.aspectSetCache(this, cacheResult);
            }
            return (T) cacheResult;
        } else
            return translate(translator);
    }
    protected abstract T translate(M translator);

    protected abstract H reverseTranslate(H hash, M translator);
    protected abstract H aspectContextHash(H hash);
    private Integer hashes; //Map<H, Integer>
    @ManualLazy
    protected int aspectHash(H hash) {
        if(isComplex()) {
            if(hash.isGlobal()) {
                // сделал isGlobal только HashCode*, так как getKeys, getValues и фильтрация достаточно много жрут
                assert hash == HashCodeValues.instance || (hash instanceof HashContext && ((HashContext)hash).keys==HashCodeKeys.instance && ((HashContext)hash).values==HashCodeValues.instance);
                if(hashes==null)
                    hashes = hash(hash);
                return hashes;

/*                if(hashes==null)
                     hashes = new HashMap<H, Integer>();

                hash = aspectContextHash(hash);
                Integer result = hashes.get(hash);
                if(result==null) {
                    AbstractTranslateContext<T, M, H> from = (AbstractTranslateContext<T, M, H>) getFrom();
                    H reversed;
                    if(from!=null && (reversed = from.reverseTranslate(hash, getTranslator()))!=null)
                        result = from.aspectHash(reversed);
                    else
                        result = hash(hash);
                    hashes.put(hash, result);
                }
                return result;*/
            } else {
                Integer cacheResult = hash.aspectGetCache(this);
                if(cacheResult==null) {
                    cacheResult = hash(hash);
                    hash.aspectSetCache(this, cacheResult);
                }
                return cacheResult;
            }
        } else
            return hash(hash);
    }
    protected abstract int hash(H hash); // по сути protected

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
            packed = calculatePack();
            ((AbstractTranslateContext)packed).packed = packed;
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
