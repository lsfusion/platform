package platform.client.form.queries;

import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.filter.ClientPropertyFilter;

import java.util.ArrayList;
import java.util.List;

abstract class QueryController implements QueryListener {

    private final QueryView view;

    public QueryView getView() {
        return view;
    }

    private final List<ClientPropertyFilter> conditions = new ArrayList<ClientPropertyFilter>();

    public List<ClientPropertyFilter> getConditions() {
        return conditions;
    }

    private final GroupObjectLogicsSupplier logicsSupplier;

    QueryController(GroupObjectLogicsSupplier logicsSupplier) {
        this.logicsSupplier = logicsSupplier;

        view = createView();
        view.setListener(this);
    }

    protected abstract QueryView createView();

    // Здесь слушаем наш View
    public void applyPressed() {
        if (queryChanged()) {
            view.queryApplied();
        }
    }

    public void addConditionPressed(boolean replace) {
        addConditionPressed(replace, null);
    }

    public void addConditionPressed(boolean replace, ClientPropertyDraw propertyDraw) {
        ClientPropertyDraw filterProperty = propertyDraw != null ? propertyDraw : logicsSupplier.getDefaultProperty();
        if (filterProperty == null) {
            //не добавляем, если нет ни одного свойства
            return;
        }

        if (replace) {
            // считаем, что в таком случае просто нажали сначала все удалить, а затем - добавить
            allConditionsRemoved();
        }

        ClientPropertyFilter filter = new ClientPropertyFilter();
        filter.property = filterProperty;

        conditions.add(filter);

        view.addConditionView(filter, logicsSupplier);
    }

    public void conditionRemoved(ClientPropertyFilter condition) {
        conditions.remove(condition);

        view.removeCondition(condition);

        if (conditions.isEmpty()) {
            applyPressed();
        }
    }

    public void allConditionsRemoved() {
        conditions.clear();
        view.removeAllConditions();
    }

    protected abstract boolean queryChanged();

    public void quickEditFilter(ClientPropertyDraw propertyDraw) {
        addConditionPressed(true, propertyDraw);
        view.startEditing(propertyDraw);
    }

    public boolean hasActiveFilter() {
        return conditions.size() > 0;
    }
}
