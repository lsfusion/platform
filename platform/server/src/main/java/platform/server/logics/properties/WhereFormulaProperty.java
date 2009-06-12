package platform.server.logics.properties;

import platform.server.data.classes.BitClass;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.types.Type;
import platform.server.where.Where;
import platform.server.where.WhereBuilder;
import platform.server.session.TableChanges;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

abstract class WhereFormulaProperty extends ValueFormulaProperty<FormulaPropertyInterface> {

    static Collection<FormulaPropertyInterface> getInterfaces(int paramCount) {
        Collection<FormulaPropertyInterface> interfaces = new ArrayList<FormulaPropertyInterface>();
        for(int i=0;i<paramCount;i++)
            interfaces.add(new FormulaPropertyInterface(i));
        return interfaces;
    }

    protected WhereFormulaProperty(String iSID, int paramCount) {
        super(iSID, getInterfaces(paramCount), BitClass.instance);
    }

    public SourceExpr calculateSourceExpr(Map<FormulaPropertyInterface, ? extends SourceExpr> joinImplement, TableChanges session, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps, WhereBuilder changedWhere) {
        return new CaseExpr(getWhere(joinImplement), new ValueExpr(true, BitClass.instance));
    }

    abstract Where getWhere(Map<FormulaPropertyInterface, ? extends SourceExpr> joinImplement);
}
