package platform.server.logics.property;

import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.ServerResourceBundle;
import platform.server.session.PropertyChanges;

import java.util.*;

// выбирает объект по битам
public class AndFormulaProperty extends FormulaProperty<AndFormulaProperty.Interface> {

    public final ObjectInterface objectInterface;
    public final Collection<AndInterface> andInterfaces;

    public static abstract class Interface<P extends Interface<P>> extends PropertyInterface<P> {
        public Interface(int ID) {
            super(ID);
        }
    }

    public static class ObjectInterface extends Interface<ObjectInterface> {
        public ObjectInterface(int ID) {
            super(ID);
        }
    }

    public static class AndInterface extends Interface<AndInterface> {
        public AndInterface(int ID) {
            super(ID);
        }
    }

    static List<Interface> getInterfaces(int size) {
        List<Interface> result = new ArrayList<Interface>();
        result.add(new ObjectInterface(0));
        for(int i=0;i<size;i++)
            result.add(new AndInterface(i+1));
        return result;
    }

    public AndFormulaProperty(String sID, int size) {
        super(sID, ServerResourceBundle.getString("logics.property.if"), getInterfaces(size));
        andInterfaces = new ArrayList<AndInterface>();
        ObjectInterface objInterface = null;
        for(Interface propertyInterface : interfaces)
            if(propertyInterface instanceof ObjectInterface)
                objInterface = (ObjectInterface) propertyInterface;
            else
                andInterfaces.add((AndInterface) propertyInterface); 
        objectInterface = objInterface;

        finalizeInit();
    }

    public Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Where where = Where.TRUE;
        for(Interface propertyInterface : interfaces)
            if(propertyInterface!= objectInterface)
                where = where.and(joinImplement.get(propertyInterface).getWhere());
        return joinImplement.get(objectInterface).and(where);
    }

    @Override
    public Map<AndFormulaProperty.Interface, ValueClass> getInterfaceCommonClasses(ValueClass commonValue) {
        if(commonValue!=null)
            return Collections.singletonMap((AndFormulaProperty.Interface)objectInterface, commonValue);
        return super.getInterfaceCommonClasses(commonValue);
    }

}
