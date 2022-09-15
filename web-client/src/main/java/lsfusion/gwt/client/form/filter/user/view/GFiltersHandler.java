package lsfusion.gwt.client.form.filter.user.view;

public interface GFiltersHandler {
    boolean hasFiltersContainer();
    
    void addCondition();

    void applyFilters();

    void resetConditions();
}
