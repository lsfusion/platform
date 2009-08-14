package platform.server.view.form;

import platform.server.data.classes.CustomClass;
import platform.server.data.classes.DataClass;
import platform.server.data.classes.ValueClass;
import platform.server.data.classes.where.AndClassSet;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.types.Type;
import platform.server.logics.DataObject;
import platform.server.session.ChangesSession;

import java.sql.SQLException;
import java.util.Collection;

// ObjectImplement data'ы
public class DataObjectImplement extends ObjectImplement {

    DataClass dataClass;
    Object value;

    public DataObjectImplement(int iID, String sID, DataClass iDataClass, String iCaption) {
        super(iID,sID,iCaption);
        dataClass = iDataClass;
        value = dataClass.getDefaultValue();
    }

    public AndClassSet getClassSet(GroupObjectImplement classGroup) {
        return getObjectClass();
    }

    public ValueClass getGridClass() {
        return dataClass;
    }

    public DataClass getObjectClass() {
        return dataClass;
    }

    public DataClass getBaseClass() {
        return dataClass;
    }

    public void changeValue(ChangesSession session, Object changeValue) throws SQLException {
        if(changeValue==null) changeValue = dataClass.getDefaultValue();
        if(value.equals(changeValue)) return;

        value = changeValue;

        updated = updated | UPDATED_OBJECT;
        groupTo.updated = groupTo.updated | GroupObjectImplement.UPDATED_OBJECT;
    }

    public void changeValue(ChangesSession session, DataObject changeValue) throws SQLException {
        changeValue(session, changeValue.object);
    }

    public boolean classChanged(Collection<CustomClass> changedClasses) {
        return false;
    }
    public boolean classUpdated() {
        return false;
    }

    public DataObject getValue() {
        return new DataObject(value,dataClass);
    }

    protected SourceExpr getExpr() {
        return new ValueExpr(value,dataClass);
    }

    public Type getType() {
        return dataClass;
    }
}
