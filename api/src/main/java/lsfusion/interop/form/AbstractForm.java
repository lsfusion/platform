package lsfusion.interop.form;

import lsfusion.interop.form.design.AbstractComponent;
import lsfusion.interop.form.design.AbstractContainer;

public interface AbstractForm<C extends AbstractContainer<T, Str>, T extends AbstractComponent, Str> {
    C getMainContainer();
}
