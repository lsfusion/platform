package platform.server.view.form;

import platform.server.data.classes.ConcreteClass;
import platform.server.data.classes.CustomClass;
import platform.server.data.classes.ValueClass;
import platform.server.data.classes.where.AndClassSet;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.types.Type;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.properties.Property;
import platform.server.session.ChangesSession;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// на самом деле нужен collection но при extend'е нужна конкретная реализация
public abstract class ObjectImplement extends CellView implements PropertyObjectInterface {

    public ObjectImplement(int iID, String iSID, String iCaption) {
        super(iID,iSID);
        caption = iCaption;
    }

    // 0 !!! - изменился объект, 1 !!! - класс объекта, 3 !!! - класса, 4 - классовый вид

    public final static int UPDATED_OBJECT = (1);
    public final static int UPDATED_CLASS = (1 << 1);
    public final static int UPDATED_GRIDCLASS = (1 << 3);

    protected int updated = UPDATED_CLASS | UPDATED_GRIDCLASS;

    public GroupObjectImplement groupTo;

    public String caption = "";

    public String toString() {
        return caption;
    }

    protected abstract SourceExpr getExpr();
    public SourceExpr getSourceExpr(Set<GroupObjectImplement> classGroup, Map<ObjectImplement, ? extends SourceExpr> classSource) {
        return (classGroup!=null && classGroup.contains(groupTo)?classSource.get(this):getExpr());
    }

    public abstract ValueClass getBaseClass();

    public abstract ObjectValue getObjectValue();
    public DataObject getDataObject() {
        return (DataObject)getObjectValue();
    }

    public abstract ValueClass getGridClass();

    public abstract void changeValue(ChangesSession session, Object changeValue) throws SQLException;
    public abstract void changeValue(ChangesSession session, ObjectValue changeValue) throws SQLException;

    public abstract boolean classChanged(Collection<CustomClass> changedClasses);

    public abstract Type getType();

    public boolean objectUpdated(GroupObjectImplement classGroup) { return groupTo!=classGroup && (updated & UPDATED_OBJECT)!=0; }
    public boolean dataUpdated(Collection<Property> changedProps) { return false; }
    public void fillProperties(Set<Property> properties) { }
    public SourceExpr getSourceExpr(Set<GroupObjectImplement> classGroup, Map<ObjectImplement, ? extends SourceExpr> classSource, TableModifier<? extends TableChanges> modifier) {
        return getSourceExpr(classGroup, classSource);
    }

    public GroupObjectImplement getApplyObject() {
        return groupTo;
    }
}
