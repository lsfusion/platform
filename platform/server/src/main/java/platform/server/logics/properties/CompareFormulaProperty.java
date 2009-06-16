package platform.server.logics.properties;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.classes.LogicalClass;
import platform.server.where.WhereBuilder;
import platform.server.session.TableChanges;

import java.util.Iterator;
import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;

public class CompareFormulaProperty extends ValueFormulaProperty<FormulaPropertyInterface> {

    int compare;
    public FormulaPropertyInterface operator1;
    public FormulaPropertyInterface operator2;

    public CompareFormulaProperty(String sID, int iCompare) {
        super(sID, getInterfaces(2), LogicalClass.instance);

        compare = iCompare;
        Iterator<FormulaPropertyInterface> i = interfaces.iterator();
        operator1 = i.next();
        operator2 = i.next();
    }

    static Collection<FormulaPropertyInterface> getInterfaces(int paramCount) {
        Collection<FormulaPropertyInterface> interfaces = new ArrayList<FormulaPropertyInterface>();
        for(int i=0;i<paramCount;i++)
            interfaces.add(new FormulaPropertyInterface(i));
        return interfaces;
    }

    public SourceExpr calculateSourceExpr(Map<FormulaPropertyInterface, ? extends SourceExpr> joinImplement, TableChanges session, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps, WhereBuilder changedWhere) {
        return new CaseExpr(joinImplement.get(operator1).compare(joinImplement.get(operator2), compare), new ValueExpr(true, LogicalClass.instance));
    }
}
