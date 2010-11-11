package platform.interop.form.layout;

public interface FunctionFactory<F extends AbstractFunction> {

    F createFunction();
}
