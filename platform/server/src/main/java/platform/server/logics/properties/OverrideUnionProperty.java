package platform.server.logics.properties;

import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.data.classes.ConcreteClass;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.session.MapChangeDataProperty;
import platform.server.session.TableChanges;
import platform.server.where.WhereBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OverrideUnionProperty extends UnionProperty {


    public OverrideUnionProperty(String iSID, int intNum) {
        super(iSID, intNum);
    }

    public List<PropertyMapImplement<PropertyInterface,PropertyInterface>> operands = new ArrayList<PropertyMapImplement<PropertyInterface, PropertyInterface>>();

    protected Collection<PropertyMapImplement<PropertyInterface, PropertyInterface>> getOperands() {
        return operands;
    }

    public SourceExpr calculateSourceExpr(Map<PropertyInterface, ? extends SourceExpr> joinImplement, TableChanges session, Collection<DataProperty> usedDefault, TableDepends<? extends TableUsedChanges> depends, WhereBuilder changedWhere) {

        SourceExpr result = null;
        for(PropertyMapImplement<PropertyInterface, PropertyInterface> operand : operands) {
            SourceExpr operandExpr = operand.mapSourceExpr(joinImplement, session, usedDefault, depends, changedWhere);
            if(result==null)
                result = operandExpr;
            else
                result = operandExpr.nvl(result);
        }
        return result;
    }

    @Override
    public MapChangeDataProperty<PropertyInterface> getChangeProperty(Map<PropertyInterface, ConcreteClass> interfaceClasses, ChangePropertySecurityPolicy securityPolicy, boolean externalID) {
        for(PropertyMapImplement<PropertyInterface, PropertyInterface> operand : operands) {
            MapChangeDataProperty<PropertyInterface> operandChange = operand.mapGetChangeProperty(interfaceClasses, securityPolicy, externalID);
            if(operandChange!=null) return operandChange;
        }
        return null;
    }
}
