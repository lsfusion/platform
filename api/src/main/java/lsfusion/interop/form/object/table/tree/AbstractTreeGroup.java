package lsfusion.interop.form.object.table.tree;

import lsfusion.interop.form.design.AbstractComponent;

public interface AbstractTreeGroup<T extends AbstractComponent> extends AbstractComponent {

    int getID();
    String getSID();

    T getToolbarSystem();
    T getFiltersContainer();
    T getFilterControls();
}
