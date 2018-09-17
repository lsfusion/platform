package lsfusion.server.form.entity;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.instance.PropertyType;
import lsfusion.server.profiler.ProfiledObject;

public interface PropertyReaderEntity extends ProfiledObject {

    byte getTypeID();

    int getID(); // ID в рамках Type
    
    String getSID(); // ID в рамках Type
    
    CalcPropertyObjectEntity getPropertyObjectEntity();
    
    PropertyType getPropertyType(FormEntity formEntity);

    String getReportSID();
    
    ImOrderSet<GroupObjectEntity> getColumnGroupObjects();
}
