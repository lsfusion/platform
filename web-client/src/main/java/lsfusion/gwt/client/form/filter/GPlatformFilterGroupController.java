package lsfusion.gwt.client.form.filter;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.FormCheckBox;
import lsfusion.gwt.client.form.controller.GFormController;

import java.util.ArrayList;

// the platform's filter group - FILTERGROUP(fg) where the platform draws it, the platform's kind of the controller of
// a filter group (GFilterGroupController): a check box for one filter, a list for several, with "(All)" unless one
// must be selected - what the user selects in it, or a filter's key, sent at once, and the filter the server reports
// selected shown
public final class GPlatformFilterGroupController implements GFilterGroupController {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    private final GFormController form;
    private final GRegularFilterGroup filterGroup;
    private final Widget view;

    public GPlatformFilterGroupController(GFormController form, GRegularFilterGroup filterGroup) {
        this.form = form;
        this.filterGroup = filterGroup;
        view = filterGroup.filters.size() == 1 ? createSingleFilterComponent(filterGroup.filters.get(0)) : createMultipleFilterComponent();
        form.addFilterView(filterGroup, view);
    }

    public void showFilter(GRegularFilter filter) {
        if (view instanceof CheckBox)
            ((CheckBox) view).setValue(filter != null, false);
        else
            ((ListBox) view).setSelectedIndex(filterGroup.filters.indexOf(filter) + (filterGroup.noNull ? 0 : 1));
    }

    // a filter's binding, bound by the form (GFormController.initializeBindings): taken with the check box or the list,
    // it toggles the check box, or selects the filter in the list
    public Widget getBindingWidget() {
        return view;
    }
    public void onBinding(GRegularFilter filter) {
        if (view instanceof CheckBox) {
            CheckBox filterCheck = (CheckBox) view;
            filterCheck.setValue(!filterCheck.getValue(), true);
        } else
            setRegularFilter((ListBox) view, filterGroup.filters.indexOf(filter));
    }

    private CheckBox createSingleFilterComponent(final GRegularFilter filter) {
        final CheckBox filterCheck = new FormCheckBox(filter.getFullCaption());
        filterCheck.setValue(false);
        filterCheck.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> e) {
                form.setRemoteRegularFilter(filterGroup, e.getValue() != null && e.getValue() ? filter : null);
            }
        });
        GwtClientUtils.addClassName(filterCheck, "filter-group-check");

        GFormController.setBindingGroupObject(filterCheck, filterGroup.groupObject);
        return filterCheck;
    }

    private ListBox createMultipleFilterComponent() {
        final ListBox filterBox = new ListBox();
        filterBox.setMultipleSelect(false);
        if (!filterGroup.noNull)
            filterBox.addItem("(" + messages.multipleFilterComponentAll() + ")", "-1");

        ArrayList<GRegularFilter> filters = filterGroup.filters;
        for (int i = 0; i < filters.size(); i++) {
            final GRegularFilter filter = filters.get(i);
            filterBox.addItem(filter.getFullCaption(), "" + i);

            GFormController.setBindingGroupObject(filterBox, filterGroup.groupObject);
        }

        filterBox.addChangeHandler(event -> setRemoteRegularFilter(filterBox.getSelectedIndex() - (filterGroup.noNull ? 0 : 1)));

        GwtClientUtils.addClassName(filterBox, "filter-group-select");
        GwtClientUtils.addClassName(filterBox, "form-select");

        return filterBox;
    }

    // the list shows the filter at once, as when the user selects it there: the answer reports nothing if the server
    // has it already
    private void setRegularFilter(ListBox filterBox, int filterIndex) {
        filterBox.setSelectedIndex(filterIndex + (filterGroup.noNull ? 0 : 1));
        setRemoteRegularFilter(filterIndex);
    }

    private void setRemoteRegularFilter(int index) {
        form.setRemoteRegularFilter(filterGroup, index == -1 ? null : filterGroup.filters.get(index));
    }
}
