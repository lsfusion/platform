package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;

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

    public AndFormulaProperty(String sID, boolean... nots) {
        super(sID, "Если", getInterfaces(nots));
        objectInterface = interfaces.iterator().next();
    }

    public Expr calculateExpr(Map<FormulaPropertyInterface, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {
        Where where = Where.TRUE;
        for(FormulaPropertyInterface propertyInterface : interfaces)
            if(propertyInterface!= objectInterface) {
                Where interfaceWhere = joinImplement.get(propertyInterface).getWhere();
                if(((AndPropertyInterface)propertyInterface).not)
                    interfaceWhere = interfaceWhere.not();
                where = where.and(interfaceWhere);
            }
        return joinImplement.get(objectInterface).and(where);
    }
}
