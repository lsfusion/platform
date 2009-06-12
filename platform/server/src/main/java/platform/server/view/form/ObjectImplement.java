package platform.server.view.form;

import platform.server.data.classes.ConcreteClass;
import platform.server.data.classes.CustomClass;
import platform.server.data.classes.ValueClass;
import platform.server.data.classes.where.ClassSet;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.types.Type;
import platform.server.logics.DataObject;
import platform.server.session.TableChanges;
import platform.server.session.ChangesSession;
import platform.server.where.Where;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

// на самом деле нужен collection но при extend'е нужна конкретная реализация
public abstract class ObjectImplement {

    public static Map<ObjectImplement, KeyExpr> getMapKeys(Collection<ObjectImplement> objects) {
        Map<ObjectImplement,KeyExpr> result = new HashMap<ObjectImplement, KeyExpr>();
        for(ObjectImplement object : objects)
            result.put(object,new KeyExpr(object.caption));
        return result;
    }

    // идентификатор (в рамках формы)
    public final int ID;

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    public final String sID;
    
    public ObjectImplement(int iID, String iSID, String iCaption) {
        ID = iID;
        sID = iSID;
        caption = iCaption;
    }

    // 0 !!! - изменился объект, 1 !!! - класс объекта, 3 !!! - класса, 4 - классовый вид

    public final static int UPDATED_OBJECT = (1);
    public final static int UPDATED_CLASS = (1 << 1);
    public final static int UPDATED_GRIDCLASS = (1 << 3);

    protected int updated = UPDATED_GRIDCLASS;

    public GroupObjectImplement groupTo;

    public String caption = "";

    public String toString() {
        return caption;
    }

    SourceExpr getSourceExpr(Set<GroupObjectImplement> classGroup, Map<ObjectImplement, ? extends SourceExpr> classSource) {
        return (classGroup!=null && classGroup.contains(groupTo)?classSource.get(this):getValue().getExpr());
    }

    public abstract ValueClass getBaseClass();

    public abstract DataObject getValue();

    // может и null передаваться
    public abstract ClassSet getClassSet(GroupObjectImplement classGroup);
    public abstract ConcreteClass getObjectClass();

    public abstract ValueClass getGridClass();

    public abstract void changeValue(ChangesSession session, DataObject changeValue) throws SQLException;

    public abstract void changeValue(ChangesSession session, Object changeValue) throws SQLException;

    public abstract boolean classChanged(Collection<CustomClass> changedClasses);
    public abstract boolean classUpdated();

    public abstract Type getType();

    public boolean objectUpdated() {
        return (updated & UPDATED_OBJECT)!=0;
    }
}
