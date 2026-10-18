package lsfusion.server.language.proxy;

import lsfusion.server.logics.form.interactive.design.filter.FilterView;

public class FilterViewProxy extends ComponentViewProxy<FilterView> {
    public FilterViewProxy(FilterView target) {
        super(target);
    }

    // a user filter's condition is no component a React view of its own draws: React draws what a FILTERS box holds,
    // its conditions one part of its group (FormView.filtersScope), so the custom is the box's - NULL alone is taken
    @Override
    public void setCustom(Object custom) {
        if (!isExplicitNull(custom))
            throw new UnsupportedOperationException("custom of a user filter is NULL, the standard view: a React view draws"
                    + " the conditions of the FILTERS box they are in, which takes the custom");
        super.setCustom(custom);
    }
}
