package platform.server.view.form;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.logics.ObjectValue;
import platform.server.logics.auth.ChangePropertySecurityPolicy;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.logics.classes.sets.InterfaceClass;
import platform.server.logics.classes.sets.InterfaceClassSet;
import platform.server.logics.properties.Property;
import platform.server.logics.properties.PropertyImplement;
import platform.server.logics.properties.PropertyInterface;
import platform.server.logics.session.ChangeValue;
import platform.server.logics.session.DataSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.sql.SQLException;

public class PropertyObjectImplement<P extends PropertyInterface> extends PropertyImplement<ObjectImplement,P> {

    PropertyObjectImplement(PropertyObjectImplement<P> iProperty) { super(iProperty); }
    public PropertyObjectImplement(Property<P> iProperty) {super(iProperty);}

    // получает Grid в котором рисоваться
    public GroupObjectImplement getApplyObject() {
        GroupObjectImplement ApplyObject=null;
        for(ObjectImplement IntObject : mapping.values())
            if(ApplyObject==null || IntObject.groupTo.order >ApplyObject.order) ApplyObject = IntObject.groupTo;

        return ApplyObject;
    }

    // получает класс значения
    ClassSet getValueClass(GroupObjectImplement ClassGroup) {
        InterfaceClass<P> ClassImplement = new InterfaceClass<P>();
        for(P Interface : property.interfaces) {
            ObjectImplement IntObject = mapping.get(Interface);
            ClassSet ImpClass;
            if(IntObject.groupTo ==ClassGroup)
                if(IntObject.gridClass ==null)
                    throw new RuntimeException("надо еще думать");
                else
                    ImpClass = new ClassSet(IntObject.gridClass);//ClassSet.getUp(IntObject.GridClass);
            else
                if(IntObject.objectClass ==null)
                    return new ClassSet();
                else
                    ImpClass = new ClassSet(IntObject.objectClass);
            ClassImplement.put(Interface,ImpClass);
        }

        return property.getValueClass(ClassImplement);
    }

    // в интерфейсе
    boolean isInInterface(GroupObjectImplement ClassGroup) {
        return !getValueClass(ClassGroup).isEmpty();
    }

    // проверяет на то что изменился верхний объект
    boolean objectUpdated(GroupObjectImplement classGroup) {
        for(ObjectImplement intObject : mapping.values())
            if(intObject.groupTo !=classGroup && ((intObject.updated & ObjectImplement.UPDATED_OBJECT)!=0)) return true;

        return false;
    }

    // изменился хоть один из классов интерфейса (могло повлиять на вхождение в интерфейс)
    boolean classUpdated(GroupObjectImplement classGroup) {
        for(ObjectImplement intObject : mapping.values())
            if(((intObject.updated & ((intObject.groupTo ==classGroup)? ObjectImplement.UPDATED_CLASS: ObjectImplement.UPDATED_OBJECT)))!=0) return true;

        return false;
    }

    public ChangeValue getChangeProperty(DataSession session, ChangePropertySecurityPolicy securityPolicy) throws SQLException {
        Map<P,ObjectValue> mapInterface = new HashMap<P,ObjectValue>();
        for(Map.Entry<P, ObjectImplement> Implement : mapping.entrySet())
            mapInterface.put(Implement.getKey(),new ObjectValue(Implement.getValue().idObject,Implement.getValue().objectClass));

        return property.getChangeProperty(session,mapInterface,1,securityPolicy);
    }

    SourceExpr getSourceExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, ? extends SourceExpr> ClassSource, DataSession Session) {

        Map<P, SourceExpr> JoinImplement = new HashMap<P,SourceExpr>();
        for(P Interface : property.interfaces)
            JoinImplement.put(Interface, mapping.get(Interface).getSourceExpr(ClassGroup,ClassSource));

        InterfaceClass<P> JoinClasses = new InterfaceClass<P>();
        for(Map.Entry<P, ObjectImplement> Implement : mapping.entrySet()) {
            ClassSet Classes;
            if(ClassGroup!=null && ClassGroup.contains(Implement.getValue().groupTo)) {
                RemoteClass ImplementClass = Implement.getValue().gridClass;
                Classes = ClassSet.getUp(ImplementClass);
                ClassSet AddClasses = Session.addChanges.get(ImplementClass);
                if(AddClasses!=null)
                    Classes.or(AddClasses);
            } else {
                RemoteClass ImplementClass = Session.baseClasses.get(Implement.getValue().idObject);
                if(ImplementClass==null) ImplementClass = Implement.getValue().objectClass;
                // чего не должно быть
                if(ImplementClass==null)
                    Classes = new ClassSet();
                else
                    Classes = new ClassSet(ImplementClass);
            }
            JoinClasses.put(Implement.getKey(),Classes);
        }

        // если есть не все интерфейсы и есть изменения надо с Full Join'ить старое с новым
        // иначе как и было
        return Session.getSourceExpr(property,JoinImplement,new InterfaceClassSet<P>(JoinClasses));
    }
}
