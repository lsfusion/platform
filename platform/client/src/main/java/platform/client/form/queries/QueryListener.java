package platform.client.form.queries;

import platform.client.logics.filter.ClientPropertyFilter;

interface QueryListener {

    public void applyPressed();
    public void addConditionPressed(boolean replace);
    public void conditionRemoved(ClientPropertyFilter filter);
    public void allConditionsRemoved();
    public void conditionsUpdated();
}
