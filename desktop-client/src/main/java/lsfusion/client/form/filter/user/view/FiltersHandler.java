package lsfusion.client.form.filter.user.view;

public interface FiltersHandler {
    boolean hasFiltersContainer();

    boolean addCondition();
    
    void applyFilters(boolean focusFirstComponent);
    
    void resetConditions();
}
