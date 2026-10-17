package lsfusion.gwt.client.form.filter;

// the form's controller of a filter group - FILTERGROUP(fg) -, of the kind of whoever draws it, chosen once
// (GReactFormData.createFilterGroupController): the platform's check box or list (GPlatformFilterGroupController), or
// a view's part of the group (GReactFormData.FilterGroupPart). The server applies the filter selected and reports it,
// and each kind shows it - and sends what is selected in it, a filter's key as well
public interface GFilterGroupController {
    // the filter selected - null: none -, as the server reports it, or as it has just been asked for
    void showFilter(GRegularFilter filter);
}
