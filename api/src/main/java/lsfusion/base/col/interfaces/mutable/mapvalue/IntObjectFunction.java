package lsfusion.base.col.interfaces.mutable.mapvalue;

@FunctionalInterface
public interface IntObjectFunction<T, R> {
    
    R apply(int i, T value);
}
