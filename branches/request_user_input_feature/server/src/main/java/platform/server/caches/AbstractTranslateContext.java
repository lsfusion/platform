package platform.server.caches;

import platform.base.OrderedMap;
import platform.base.QuickSet;
import platform.base.TwinImmutableObject;
import platform.server.caches.hash.HashCodeKeys;
import platform.server.caches.hash.HashCodeValues;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashObject;
import platform.server.data.Value;
import platform.server.data.translator.MapObject;

import java.lang.ref.WeakReference;
import java.util.*;

// assert что T instanceof AbstractTranslateContext но заколебешься его протаскивать
public abstract class AbstractTranslateContext<T, M extends MapObject, H extends HashObject> extends TwinImmutableObject implements PackInterface<T> {

    // аспекты транслирования
    private WeakReference<T> from;
    private WeakReference<M> translator;
    public T getFrom() {
        if(from==null)
            return null;
        else
            return from.get();
    }
    public M getTranslator() {
        if(translator==null)
            return null;
        else
            return translator.get();
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

    protected QuickSet<Value> values;
    @ManualLazy
    protected QuickSet<Value> aspectGetValues() {
        if(values==null)
            values = getValues();
        return values;
    }
    public abstract QuickSet<Value> getValues(); // по сути protected

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
    
    public static <T extends PackInterface<T>> List<T> pack(List<T> exprs) {
        List<T> result = new ArrayList<T>();
        for(T expr : exprs)
            result.add(expr.pack());
        return result;
    }

    public static <T extends PackInterface<T>> Set<T> pack(Set<T> exprs) {
        Set<T> result = new HashSet<T>();
        for(T expr : exprs)
            result.add(expr.pack());
        return result;
    }

    public static <T extends PackInterface<T>> OrderedMap<T, Boolean> pack(OrderedMap<T, Boolean> map) {
        OrderedMap<T, Boolean> result = new OrderedMap<T, Boolean>();
        for(Map.Entry<T, Boolean> entry : map.entrySet())
            result.put(entry.getKey().pack(), entry.getValue());
        return result;
    }
}
