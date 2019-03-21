package lsfusion.server.logics.form.stat;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.interop.form.filter.user.Compare;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;

import java.sql.SQLException;

public abstract class AbstractFormDataInterface implements FormDataInterface {

    protected abstract ObjectValue getValueObject(ObjectEntity object);

    @Override
    public ImMap<ObjectEntity, ObjectValue> getObjectValues(ImSet<GroupObjectEntity> valueGroups) {
        MExclMap<ObjectEntity, ObjectValue> mResult = MapFact.mExclMap();
        for(GroupObjectEntity group : valueGroups)
            for(ObjectEntity object : group.getObjects())
                mResult.exclAdd(object, getValueObject(object));
        return mResult.immutable();
    }

    @Override
    public Where getValueWhere(GroupObjectEntity groupObject, ImSet<GroupObjectEntity> valueGroups, ImMap<ObjectEntity, Expr> mapExprs) throws SQLException, SQLHandledException {
        Where where = Where.TRUE;
        if(!valueGroups.contains(groupObject)) { // if wasn't remove from hierarchy, then use it as regular filter (otherwise will be used in getValueObjects)
            for(ObjectEntity object : getValueObjects())
                if(object.groupTo == groupObject)
                    where = where.and(mapExprs.get(object).compare(getValueObject(object).getExpr(), Compare.EQUALS));
        }            
        return where;
    }

    protected abstract ImSet<ObjectEntity> getValueObjects();
    
    // group objects, that will be removed from hierarchy
    private ImSet<GroupObjectEntity> getValueGroupObjects() {
        return getValueObjects().group(new BaseUtils.Group<GroupObjectEntity, ObjectEntity>() {
            public GroupObjectEntity group(ObjectEntity key) {
                return key.groupTo;
            }
        }).filterFn(new GetKeyValue<Boolean, GroupObjectEntity, ImSet<ObjectEntity>>() {
            public Boolean getMapValue(GroupObjectEntity key, ImSet<ObjectEntity> value) {
                return key.getObjects().size() == value.size(); // only groups with all objects
            }
        }).keys();
    }

    protected GetKeyValue<ImOrderSet<PropertyDrawEntity>, GroupObjectEntity, ImOrderSet<PropertyDrawEntity>> getUserVisible() { // with user visible
        return null;
    }

    @Override
    public StaticDataGenerator.Hierarchy getHierarchy(boolean isReport) {
        // for reports its important to keep hierarchy the same (because there are templates for post processing)
        ImSet<GroupObjectEntity> valueGroups = isReport ? SetFact.<GroupObjectEntity>EMPTY() : getValueGroupObjects();
        return getFormEntity().getStaticHierarchy(isReport, valueGroups, getUserVisible());
    }
}
