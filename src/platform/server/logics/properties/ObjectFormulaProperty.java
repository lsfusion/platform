package platform.server.logics.properties;

import platform.server.logics.data.TableFactory;
import platform.server.logics.classes.ObjectClass;
import platform.server.logics.classes.DataClass;
import platform.server.logics.classes.sets.InterfaceClass;
import platform.server.logics.classes.sets.InterfaceClassSet;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.logics.classes.sets.ValueClassSet;
import platform.server.data.query.exprs.CaseExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.where.Where;

import java.util.Map;

// выбирает объект по битам
public class ObjectFormulaProperty extends FormulaProperty<FormulaPropertyInterface> {

    public FormulaPropertyInterface objectInterface;
    ClassSet ObjectClass;

    public ObjectFormulaProperty(TableFactory iTableFactory, ObjectClass iObjectClass) {
        super(iTableFactory);
        ObjectClass = ClassSet.getUp(iObjectClass);
        objectInterface = new FormulaPropertyInterface(0);
        interfaces.add(objectInterface);
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
            Result.put(Interface,Interface== objectInterface ?ReqValue:new ClassSet(DataClass.bit));
        return Result;
    }

    ValueClassSet<FormulaPropertyInterface> calculateValueClassSet() {
        throw new RuntimeException("у этого св-ва этот метод слишком сложный, поэтому надо решать верхними частными случаям");
    }

}
