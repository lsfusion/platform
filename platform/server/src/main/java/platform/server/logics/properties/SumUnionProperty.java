package platform.server.logics.properties;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.session.TableChanges;
import platform.server.where.WhereBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SumUnionProperty extends UnionProperty {

    public SumUnionProperty(String iSID, int intNum) {
        super(iSID, intNum);
    }

    public Map<PropertyMapImplement<PropertyInterface,PropertyInterface>,Integer> operands = new HashMap<PropertyMapImplement<PropertyInterface, PropertyInterface>, Integer>();

    protected Collection<PropertyMapImplement<PropertyInterface, PropertyInterface>> getOperands() {
        return operands.keySet();
    }

    public SourceExpr calculateSourceExpr(Map<PropertyInterface, ? extends SourceExpr> joinImplement, TableChanges session, Collection<DataProperty> usedDefault, TableDepends<? extends TableUsedChanges> depends, WhereBuilder changedWhere) {

        SourceExpr result = null;        
        for(Map.Entry<PropertyMapImplement<PropertyInterface,PropertyInterface>,Integer> operandCoeff : operands.entrySet()) {
            SourceExpr operandExpr = operandCoeff.getKey().mapSourceExpr(joinImplement, session, usedDefault, depends, changedWhere).scale(operandCoeff.getValue());
            if(result==null)
                result = operandExpr;
            else
                result = result.sum(operandExpr);
        }
        return result;
    }

}
