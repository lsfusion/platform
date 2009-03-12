package platform.server.logics.properties;

import platform.server.data.query.exprs.CaseExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.types.Type;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.classes.sets.InterfaceClassSet;
import platform.server.logics.data.TableFactory;
import platform.server.where.Where;

import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;

abstract class WhereFormulaProperty extends ValueFormulaProperty<FormulaPropertyInterface> {

    static Collection<FormulaPropertyInterface> getInterfaces(int intNum) {
        Collection<FormulaPropertyInterface> interfaces = new ArrayList<FormulaPropertyInterface>();
        for(int i=0;i<intNum;i++)
            interfaces.add(new FormulaPropertyInterface(i));
        return interfaces;
    }

    protected WhereFormulaProperty(String iSID, int intNum, TableFactory iTableFactory) {
        super(iSID, getInterfaces(intNum), iTableFactory, RemoteClass.bit);
    }

    RemoteClass getOperandClass() {
        return RemoteClass.base;
    }

    SourceExpr calculateSourceExpr(Map<FormulaPropertyInterface, ? extends SourceExpr> joinImplement, InterfaceClassSet<FormulaPropertyInterface> joinClasses) {
        return new CaseExpr(getWhere(joinImplement), Type.bit.getExpr(true));
    }

    abstract Where getWhere(Map<FormulaPropertyInterface, ? extends SourceExpr> joinImplement);
}
