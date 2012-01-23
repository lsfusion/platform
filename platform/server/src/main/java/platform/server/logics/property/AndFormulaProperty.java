package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.ServerResourceBundle;
import platform.server.session.PropertyChanges;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
        public boolean not;

        public AndInterface(int ID, boolean not) {
            super(ID);
            this.not = not;
        }
    }

    static List<Interface> getInterfaces(boolean... nots) {
        List<Interface> result = new ArrayList<Interface>();
        result.add(new ObjectInterface(0));
        for(int i=0;i<nots.length;i++)
            result.add(new AndInterface(i+1,nots[i]));
        return result;
    }

    public AndFormulaProperty(String sID, boolean... nots) {
        super(sID, ServerResourceBundle.getString("logics.property.if"), getInterfaces(nots));
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

    public Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Where where = Where.TRUE;
        for(Interface propertyInterface : interfaces)
            if(propertyInterface!= objectInterface) {
                Where interfaceWhere = joinImplement.get(propertyInterface).getWhere();
                if(((AndInterface)propertyInterface).not)
                    interfaceWhere = interfaceWhere.not();
                where = where.and(interfaceWhere);
            }
        return joinImplement.get(objectInterface).and(where);
    }
}
