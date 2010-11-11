package platform.interop.form.layout;

public interface AbstractForm<C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>, F extends AbstractFunction<C, T>> {

    void setMainContainer(C mainContainer);

    void setPrintFunction(F printFunction);
    void setXlsFunction(F xlsFunction);
    void setNullFunction(F nullFunction);
    void setRefreshFunction(F refreshFunction);
    void setApplyFunction(F applyFunction);
    void setCancelFunction(F cancelFunction);
    void setOkFunction(F okFunction);
    void setCloseFunction(F closeFunction);
}
