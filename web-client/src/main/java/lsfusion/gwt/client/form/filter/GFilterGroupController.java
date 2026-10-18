package lsfusion.gwt.client.form.filter;

import com.google.gwt.user.client.ui.Widget;

// the form's controller of a filter group - FILTERGROUP(fg) -, of the kind of whoever draws it, chosen once
// (GReactFormData.createFilterGroupController): the platform's check box or list (GPlatformFilterGroupController), or
// a view's part of the group (GReactFormData.FilterGroupPart). The server applies the filter selected and reports it,
// and each kind shows it - and sends what is selected in it, a filter's key as well
public interface GFilterGroupController {
    // the filter selected - null: none -, as the server reports it, or as it has just been asked for
    void showFilter(GRegularFilter filter);
    // a filter's BINDING - a key or a mouse one -, bound once for every filter group by the form, whoever draws it
    // (GFormController.initializeBindings): the widget it is taken with - while it is on the page -, null where it is
    // not now, and the filter selected, as the binding of the platform's widget selects it
    Widget getBindingWidget();
    void onBinding(GRegularFilter filter);
}
