package platform.server.logics.properties;

import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.session.*;
import platform.server.where.WhereBuilder;
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

    public SourceExpr calculateSourceExpr(Map<PropertyInterface, ? extends SourceExpr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {

        SourceExpr result = null;
        for(PropertyMapImplement<PropertyInterface, PropertyInterface> operand : operands) {
            SourceExpr operandExpr = operand.mapSourceExpr(joinImplement, modifier, changedWhere);
            if(result==null)
                result = operandExpr;
            else
                result = operandExpr.nvl(result);
        }
        return result;
    }

    @Override
    public ChangeProperty getChangeProperty(DataSession session, Map<PropertyInterface, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier, ChangePropertySecurityPolicy securityPolicy, boolean externalID) throws SQLException {
        for(PropertyMapImplement<PropertyInterface, PropertyInterface> operand : operands) {
            ChangeProperty operandChange = operand.mapGetChangeProperty(session, interfaceValues, modifier, securityPolicy, externalID);
            if(operandChange!=null) return operandChange;
        }
        return null;
    }
}
