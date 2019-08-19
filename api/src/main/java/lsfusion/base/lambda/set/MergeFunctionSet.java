package lsfusion.base.lambda.set;

public class MergeFunctionSet<T> implements SFunctionSet<T> {

    private final FunctionSet<T> set1;
    private final FunctionSet<T> set2;

    public MergeFunctionSet(FunctionSet<T> set1, FunctionSet<T> set2) {
        assert !set1.isFull() && !set1.isEmpty() && !set2.isFull() && !set2.isEmpty();
//        assert !(set1 instanceof ImSet && set2 instanceof ImSet);
        this.set1 = set1;
        this.set2 = set2;
    }

    public boolean contains(T element) {
        return set1.contains(element) || set2.contains(element);
    }
}
