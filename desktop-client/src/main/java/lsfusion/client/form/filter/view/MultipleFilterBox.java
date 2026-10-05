package lsfusion.client.form.filter.view;

import lsfusion.client.base.view.ItemAdapter;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.design.view.widget.ComboBoxWidget;
import lsfusion.client.form.filter.ClientRegularFilter;
import lsfusion.client.form.filter.ClientRegularFilterGroup;
import lsfusion.client.form.filter.ClientRegularFilterWrapper;

import java.awt.event.ItemEvent;
import java.io.IOException;

import static lsfusion.client.ClientResourceBundle.getString;

public abstract class MultipleFilterBox extends ComboBoxWidget {
    private final ClientRegularFilterGroup filterGroup;
    // the filter the server has selected, as far as the client knows, null for none: selecting it sends nothing
    private ClientRegularFilter currentFilter;

    public MultipleFilterBox(ClientRegularFilterGroup filterGroup) {
        this.filterGroup = filterGroup;

        if(!filterGroup.noNull)
            addItem(new ClientRegularFilterWrapper(getString("form.all")));
        for (final ClientRegularFilter filter : filterGroup.filters)
            addItem(new ClientRegularFilterWrapper(filter));

        addItemListener(new ItemAdapter() {
            @Override
            public void itemSelected(final ItemEvent e) {
                ClientRegularFilter filter = ((ClientRegularFilterWrapper) e.getItem()).filter;
                if (filter == currentFilter)
                    return;
                currentFilter = filter;
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            selected(filter);
                        } catch (IOException ioe) {
                            throw new RuntimeException(getString("form.error.changing.regular.filter"), ioe);
                        }
                    }
                });
            }
        });
    }

    // shows the filter the server reports selected, null for none, which it has applied already: nothing is sent
    public void showSelected(ClientRegularFilter filter) {
        currentFilter = filter;
        setSelectedIndex(filterGroup.filters.indexOf(filter) + (filterGroup.noNull ? 0 : 1));
    }

    public abstract void selected(ClientRegularFilter filter) throws IOException;
}
