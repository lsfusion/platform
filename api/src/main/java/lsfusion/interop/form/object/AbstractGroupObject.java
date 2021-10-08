package lsfusion.interop.form.object;

import lsfusion.interop.form.design.AbstractComponent;

public interface AbstractGroupObject<T extends AbstractComponent, Str> {

    Str getCaption();
    int getID();
    String getSID();

    T getGrid();
    T getToolbarSystem();
    T getFiltersContainer();
    T getCalculations();
}
