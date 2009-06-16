package platform.server.logics.properties;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.where.Where;
import platform.server.where.WhereBuilder;
import platform.server.session.TableChanges;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

// выбирает объект по битам
public class AndFormulaProperty extends FormulaProperty<FormulaPropertyInterface> {

    public FormulaPropertyInterface objectInterface;

    static Collection<FormulaPropertyInterface> getInterfaces(boolean... nots) {
        Collection<FormulaPropertyInterface> result = new ArrayList<FormulaPropertyInterface>();
        result.add(new FormulaPropertyInterface(0));
        for(int i=0;i<nots.length;i++)
            result.add(new AndPropertyInterface(i+1,nots[i]));
        return result;
    }

    public AndFormulaProperty(String iSID, boolean... nots) {
        super(iSID, getInterfaces(nots));
        objectInterface = interfaces.iterator().next();
    }

    public SourceExpr calculateSourceExpr(Map<FormulaPropertyInterface, ? extends SourceExpr> joinImplement, TableChanges session, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps, WhereBuilder changedWhere) {
        Where where = Where.TRUE;
        for(FormulaPropertyInterface propertyInterface : interfaces)
            if(propertyInterface!= objectInterface) {
                Where interfaceWhere = joinImplement.get(propertyInterface).getWhere();
                if(((AndPropertyInterface)propertyInterface).not)
                    interfaceWhere = interfaceWhere.not();
                where = where.and(interfaceWhere);
            }
        return new CaseExpr(where, joinImplement.get(objectInterface));
    }
}
