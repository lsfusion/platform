package platform.server.logics.property;

import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.data.expr.Expr;
import platform.server.session.*;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.DataObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.sql.SQLException;

public class OverrideUnionProperty extends UnionProperty {

    public OverrideUnionProperty(String sID, String caption, int intNum) {
        super(sID, caption, intNum);
    }

    public List<PropertyMapImplement<PropertyInterface,PropertyInterface>> operands = new ArrayList<PropertyMapImplement<PropertyInterface, PropertyInterface>>();

    protected Collection<PropertyMapImplement<PropertyInterface, PropertyInterface>> getOperands() {
        return operands;
    }

    public Expr calculateExpr(Map<PropertyInterface, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {

        Expr result = null;
        for(PropertyMapImplement<PropertyInterface, PropertyInterface> operand : operands) {
            Expr operandExpr = operand.mapExpr(joinImplement, modifier, changedWhere);
            if(result==null)
                result = operandExpr;
            else
                result = operandExpr.nvl(result);
        }
        return result;
    }

    @Override
    public DataChange getChangeProperty(DataSession session, Map<PropertyInterface, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier, ChangePropertySecurityPolicy securityPolicy, boolean externalID) throws SQLException {
        for(PropertyMapImplement<PropertyInterface, PropertyInterface> operand : operands) {
            DataChange operandChange = operand.mapGetChangeProperty(session, interfaceValues, modifier, securityPolicy, externalID);
            if(operandChange!=null) return operandChange;
        }
        return null;
    }
}
