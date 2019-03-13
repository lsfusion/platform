package lsfusion.server.form.entity;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.type.Type;
import lsfusion.server.physics.admin.profiler.ProfiledObject;

public interface PropertyReaderEntity extends ProfiledObject {

    byte getTypeID();
    
    Type getType();

    int getID(); // ID в рамках Type
    
    String getSID(); // ID в рамках Type
    
    CalcPropertyObjectEntity getPropertyObjectEntity();

    String getReportSID();
    
    ImOrderSet<GroupObjectEntity> getColumnGroupObjects();
}
