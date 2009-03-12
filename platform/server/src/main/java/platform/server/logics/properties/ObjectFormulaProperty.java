package platform.server.logics.properties;

import platform.server.data.query.exprs.CaseExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.logics.classes.ObjectClass;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.logics.classes.sets.InterfaceClass;
import platform.server.logics.classes.sets.InterfaceClassSet;
import platform.server.logics.classes.sets.ValueClassSet;
import platform.server.logics.data.TableFactory;
import platform.server.where.Where;

import java.util.Map;
import java.util.Collections;
import java.util.Collection;
import java.util.ArrayList;

// выбирает объект по битам
public class ObjectFormulaProperty extends FormulaProperty<FormulaPropertyInterface> {

    public FormulaPropertyInterface objectInterface;
    ClassSet objectClass;

    static Collection<FormulaPropertyInterface> getInterfaces(int bitCount) {
        Collection<FormulaPropertyInterface> result = new ArrayList<FormulaPropertyInterface>();
        for(int i=0;i<bitCount+1;i++)
            result.add(new FormulaPropertyInterface(i));
        return result;
    }

    public ObjectFormulaProperty(String iSID, int bitCount, TableFactory iTableFactory, ObjectClass iObjectClass) {
        super(iSID, getInterfaces(bitCount),iTableFactory);
        objectInterface = interfaces.iterator().next();
        objectClass = ClassSet.getUp(iObjectClass);
    }

    SourceExpr calculateSourceExpr(Map<FormulaPropertyInterface,? extends SourceExpr> joinImplement, InterfaceClassSet<FormulaPropertyInterface> joinClasses) {
        Where where = Where.TRUE;
        for(FormulaPropertyInterface Interface : interfaces)
            if(Interface!= objectInterface)
                where = where.and(joinImplement.get(Interface).getWhere());

        return new CaseExpr(where, joinImplement.get(objectInterface));
    }

    ClassSet calculateValueClass(InterfaceClass<FormulaPropertyInterface> interfaceImplement) {
        return interfaceImplement.get(objectInterface);
    }

    InterfaceClassSet<FormulaPropertyInterface> calculateClassSet(ClassSet reqValue) {
        if(!reqValue.isEmpty())
            return new InterfaceClassSet<FormulaPropertyInterface>(getInterfaceClass(reqValue));
        else
            return new InterfaceClassSet<FormulaPropertyInterface>();
    }

    InterfaceClass<FormulaPropertyInterface> getInterfaceClass(ClassSet ReqValue) {
        InterfaceClass<FormulaPropertyInterface> Result = new InterfaceClass<FormulaPropertyInterface>();
        for(FormulaPropertyInterface Interface : interfaces)
            Result.put(Interface,Interface== objectInterface ?ReqValue:new ClassSet(RemoteClass.bit));
        return Result;
    }

    ValueClassSet<FormulaPropertyInterface> calculateValueClassSet() {
        throw new RuntimeException("у этого св-ва этот метод слишком сложный, поэтому надо решать верхними частными случаям");
    }

}
