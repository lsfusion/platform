package platform.server.logics.properties;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.where.WhereBuilder;
import platform.server.session.TableChanges;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class MaxUnionProperty extends UnionProperty {

    public Collection<PropertyMapImplement<PropertyInterface,PropertyInterface>> operands = new ArrayList<PropertyMapImplement<PropertyInterface, PropertyInterface>>();

    public SourceExpr calculateSourceExpr(Map<PropertyInterface, ? extends SourceExpr> joinImplement, TableChanges session, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps, WhereBuilder changedWhere) {

        SourceExpr result = null;
        for(PropertyMapImplement<PropertyInterface, PropertyInterface> operand : operands) {
            SourceExpr operandExpr = operand.mapSourceExpr(joinImplement, session, defaultProps, changedWhere, noUpdateProps);
            if(result==null)
                result = operandExpr;
            else
                result = new CaseExpr(result.greater(operandExpr), result, operandExpr);
        }
        return result;
    }

    protected Collection<PropertyMapImplement<PropertyInterface, PropertyInterface>> getOperands() {
        return operands;
    }

    public MaxUnionProperty(String iSID, int intNum) {
        super(iSID, intNum);
    }
}
