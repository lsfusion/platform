package lsfusion.server.logics.form.interactive.action;

// which open form a statement means - ['label' =] [form] [WINDOW window], at least one part, with the names already
// resolved. ACTIVATE FORM takes the first form it names, CLOSE FORM takes every one.
// An omitted part does not narrow the address: with no form it names that label in any form, with no label it names
// every instance of the form whatever label it was opened with, and with no window it looks in all of them
public class FormAddress {

    public final String formId;
    public final String formCanonicalName;
    public final String windowCanonicalName;

    public FormAddress(String formId, String formCanonicalName, String windowCanonicalName) {
        this.formId = formId;
        this.formCanonicalName = formCanonicalName;
        this.windowCanonicalName = windowCanonicalName;
    }
}
