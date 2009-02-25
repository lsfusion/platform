package platform.server.logics.properties;

import platform.server.logics.data.TableFactory;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.classes.sets.InterfaceClassSet;
import platform.server.data.types.Type;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.CaseExpr;
import platform.server.where.Where;

import java.util.Map;

abstract class WhereFormulaProperty extends ValueFormulaProperty<FormulaPropertyInterface> {

    WhereFormulaProperty(TableFactory iTableFactory) {
        super(iTableFactory, RemoteClass.bit);
    }

    RemoteClass getOperandClass() {
        return RemoteClass.base;
    }

    SourceExpr calculateSourceExpr(Map<FormulaPropertyInterface, ? extends SourceExpr> joinImplement, InterfaceClassSet<FormulaPropertyInterface> joinClasses) {
        return new CaseExpr(getWhere(joinImplement), Type.bit.getExpr(true));
    }

    abstract Where getWhere(Map<FormulaPropertyInterface, ? extends SourceExpr> JoinImplement);
}
