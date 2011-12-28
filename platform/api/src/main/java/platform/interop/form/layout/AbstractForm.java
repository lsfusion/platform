package platform.interop.form.layout;

public interface AbstractForm<C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>, F extends AbstractFunction<C, T>> {
    C getMainContainer();

    F getPrintFunction();
    F getEditFunction();
    F getXlsFunction();
    F getNullFunction();
    F getRefreshFunction();
    F getApplyFunction();
    F getCancelFunction();
    F getOkFunction();
    F getCloseFunction();
}
