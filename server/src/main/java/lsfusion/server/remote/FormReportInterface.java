package lsfusion.server.remote;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.ClassViewType;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.CalcPropertyObjectEntity;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.FormSourceInterface;
import lsfusion.server.form.instance.PropertyType;
import lsfusion.server.logics.BusinessLogics;

import java.sql.SQLException;

public interface FormReportInterface<PD extends PRI, GO, PO, CPO extends OI, OI, O extends OI, PRI> extends FormSourceInterface<PD, GO, PO, CPO, OI, O, PRI> {

    BusinessLogics getBL();
    FormEntity getEntity();

    ImSet<GO> getGroups();

    int getGroupID(GO go); // interface
    int getObjectID(O o); // interface
    String getObjectSID(O o); // interface
    ClassViewType getGroupViewType(GO go); // interface

    byte getTypeID(PRI pri);
    PropertyType getPropertyType(PRI pri);

    GO getGroupByID(int id);

    Object read(CalcPropertyObjectEntity reportPathProp) throws SQLException, SQLHandledException;
    FormInstance getFormInstance(); // хак, но иначе придется в ReportDesignGenerator тоже протягивать generics'ы
}
