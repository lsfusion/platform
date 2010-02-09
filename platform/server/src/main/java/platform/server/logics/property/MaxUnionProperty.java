package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;
import platform.server.data.where.WhereBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class MaxUnionProperty extends UnionProperty {

    public Collection<PropertyMapImplement<?,Interface>> operands = new ArrayList<PropertyMapImplement<?, Interface>>();

    public Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {

        Expr result = null;
        for(PropertyMapImplement<?, Interface> operand : operands) {
            Expr operandExpr = operand.mapExpr(joinImplement, modifier, changedWhere);
            if(result==null)
                result = operandExpr;
            else
                result = result.max(operandExpr);
        }
        return result;
    }

    protected Collection<PropertyMapImplement<?, Interface>> getOperands() {
        return operands;
    }

    public MaxUnionProperty(String sID, String caption, int intNum) {
        super(sID, caption, intNum);
    }
}
