package lsfusion.server.logics.form.struct.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.physics.admin.profiler.ProfiledObject;

public interface PropertyReaderEntity extends ProfiledObject {

    Type getType();

    int getID(); // ID в рамках Type
    
    String getSID(); // ID в рамках Type
    
    PropertyObjectEntity getPropertyObjectEntity();

    String getReportSID();
    
    ImOrderSet<GroupObjectEntity> getColumnGroupObjects();
}
