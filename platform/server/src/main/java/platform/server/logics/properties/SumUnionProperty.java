package platform.server.logics.properties;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;
import platform.server.where.WhereBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SumUnionProperty extends UnionProperty {

    public SumUnionProperty(String sID, String caption, int intNum) {
        super(sID, caption, intNum);
    }

    public Map<PropertyMapImplement<PropertyInterface,PropertyInterface>,Integer> operands = new HashMap<PropertyMapImplement<PropertyInterface, PropertyInterface>, Integer>();

    protected Collection<PropertyMapImplement<PropertyInterface, PropertyInterface>> getOperands() {
        return operands.keySet();
    }

    public SourceExpr calculateSourceExpr(Map<PropertyInterface, ? extends SourceExpr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {

        SourceExpr result = null;        
        for(Map.Entry<PropertyMapImplement<PropertyInterface,PropertyInterface>,Integer> operandCoeff : operands.entrySet()) {
            SourceExpr operandExpr = operandCoeff.getKey().mapSourceExpr(joinImplement, modifier, changedWhere).scale(operandCoeff.getValue());
            if(result==null)
                result = operandExpr;
            else
                result = result.sum(operandExpr);
        }
        return result;
    }

}
