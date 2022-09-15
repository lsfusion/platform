package lsfusion.client.form.filter.user.view;

public interface FiltersHandler {
    boolean hasFiltersContainer();
    
    void addCondition();
    
    void applyFilters(boolean focusFirstComponent);
    
    void resetConditions();
}
