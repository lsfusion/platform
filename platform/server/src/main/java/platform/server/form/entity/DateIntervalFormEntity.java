package platform.server.form.entity;

import platform.interop.ClassViewType;
import platform.server.classes.DateClass;
import platform.server.form.navigator.NavigatorElement;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.BusinessLogics;

public class DateIntervalFormEntity<T extends BusinessLogics<T>> extends FormEntity<T> {

    public ObjectEntity objDateFrom;
    public ObjectEntity objDateTo;

    public DateIntervalFormEntity(BaseLogicsModule<?> LM, NavigatorElement<T> parent, String sID, String caption) {
        super(parent, sID, caption);

        addIntervalGroupObject(LM);
    }

    public DateIntervalFormEntity(BaseLogicsModule<?> LM, NavigatorElement<T> parent, String sID, String caption, boolean iisPrintForm) {
        super(parent, sID, caption, null, iisPrintForm);

        addIntervalGroupObject(LM);
    }

    private void addIntervalGroupObject(BaseLogicsModule<?> LM) {
        GroupObjectEntity gobjDates = new GroupObjectEntity(1, "date");
        objDateFrom = new ObjectEntity(2, "dateFrom", DateClass.instance, "Дата (с)");
        objDateTo = new ObjectEntity(3, "dateTo", DateClass.instance, "Дата (по)");
        gobjDates.add(objDateFrom);
        gobjDates.add(objDateTo);

        addGroupObject(gobjDates);
        gobjDates.setSingleClassView(ClassViewType.PANEL);

        addPropertyDraw(objDateFrom, LM.objectValue);
        addPropertyDraw(objDateTo, LM.objectValue);
    }
}
