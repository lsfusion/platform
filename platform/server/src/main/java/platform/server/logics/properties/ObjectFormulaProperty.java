package platform.server.logics.properties;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.types.Type;
import platform.server.data.classes.BitClass;
import platform.server.where.Where;
import platform.server.where.WhereBuilder;
import platform.server.session.TableChanges;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

// выбирает объект по битам
public class ObjectFormulaProperty extends FormulaProperty<FormulaPropertyInterface> {

    public FormulaPropertyInterface objectInterface;

    static Collection<FormulaPropertyInterface> getInterfaces(int bitCount) {
        Collection<FormulaPropertyInterface> result = new ArrayList<FormulaPropertyInterface>();
        for(int i=0;i<bitCount+1;i++)
            result.add(new FormulaPropertyInterface(i));
        return result;
    }

    public ObjectFormulaProperty(String iSID, int bitCount) {
        super(iSID, getInterfaces(bitCount));
        objectInterface = interfaces.iterator().next();
    }

    public SourceExpr calculateSourceExpr(Map<FormulaPropertyInterface, ? extends SourceExpr> joinImplement, TableChanges session, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps, WhereBuilder changedWhere) {
        Where where = Where.TRUE;
        for(FormulaPropertyInterface Interface : interfaces)
            if(Interface!= objectInterface)
                where = where.and(joinImplement.get(Interface).getWhere());
        return new CaseExpr(where, joinImplement.get(objectInterface));
    }
}
