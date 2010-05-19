package platform.server.view.form;

import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.NullValue;
import platform.server.logics.property.Property;
import platform.server.session.ChangesSession;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

// на самом деле нужен collection но при extend'е нужна конкретная реализация
public abstract class ObjectImplement extends CellView implements PropertyObjectInterface {

    public ObjectImplement(int ID, String sID, String caption) {
        super(ID,sID);
        this.caption = caption;
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

    protected abstract Expr getExpr();
    public Expr getExpr(Set<GroupObjectImplement> classGroup, Map<ObjectImplement, ? extends Expr> classSource) {
        return (classGroup!=null && classGroup.contains(groupTo)?classSource.get(this):getExpr());
    }

    public abstract ValueClass getBaseClass();

    public abstract ObjectValue getObjectValue();
    public DataObject getDataObject() {
        return (DataObject)getObjectValue();
    }

    public boolean isNull() {
        return getObjectValue() instanceof NullValue;
    }

    public abstract ValueClass getGridClass();

    public abstract void changeValue(ChangesSession session, Object changeValue) throws SQLException;
    public abstract void changeValue(ChangesSession session, ObjectValue changeValue) throws SQLException;

    public abstract boolean classChanged(Collection<CustomClass> changedClasses);
    public abstract boolean classUpdated();

    public abstract Type getType();

    public boolean objectUpdated(GroupObjectImplement classGroup) { return groupTo!=classGroup && (updated & UPDATED_OBJECT)!=0; }
    public boolean dataUpdated(Collection<Property> changedProps) { return false; }
    public void fillProperties(Set<Property> properties) { }
    public Expr getExpr(Set<GroupObjectImplement> classGroup, Map<ObjectImplement, ? extends Expr> classSource, Modifier<? extends Changes> modifier) {
        return getExpr(classGroup, classSource);
    }

    public GroupObjectImplement getApplyObject() {
        return groupTo;
    }
}
