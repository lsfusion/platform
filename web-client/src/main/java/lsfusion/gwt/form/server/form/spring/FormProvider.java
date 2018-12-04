package lsfusion.gwt.form.server.form.spring;

public interface FormProvider {

    String addFormSessionObject(FormSessionObject formSessionObject);
    FormSessionObject getFormSessionObjectOrNull(String formSessionID);
    FormSessionObject getFormSessionObject(String formSessionID);
    void removeFormSessionObject(String formSessionID);
    void removeFormSessionObjects(String tabSID);  
}
