package platform.server.logics.properties;

import platform.base.BaseUtils;
import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.session.*;
import platform.server.where.WhereBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.interop.Compare;

import java.util.*;
import java.sql.SQLException;

public class JoinProperty<T extends PropertyInterface> extends FunctionProperty<JoinPropertyInterface> {
    public PropertyImplement<PropertyInterfaceImplement<JoinPropertyInterface>,T> implement;

    static Collection<JoinPropertyInterface> getInterfaces(int intNum) {
        Collection<JoinPropertyInterface> interfaces = new ArrayList<JoinPropertyInterface>();
        for(int i=0;i<intNum;i++)
            interfaces.add(new JoinPropertyInterface(i));
        return interfaces;
    }

    public JoinProperty(String sID, String caption, int intNum) {
        super(sID, caption, getInterfaces(intNum));
    }

    public SourceExpr calculateSourceExpr(Map<JoinPropertyInterface, ? extends SourceExpr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {

        // считаем новые SourceExpr'ы и классы
        Map<T, SourceExpr> implementExprs = new HashMap<T, SourceExpr>();
        for(Map.Entry<T,PropertyInterfaceImplement<JoinPropertyInterface>> interfaceImplement : implement.mapping.entrySet())
            implementExprs.put(interfaceImplement.getKey(),interfaceImplement.getValue().mapSourceExpr(joinImplement, modifier, changedWhere));
        return implement.property.getSourceExpr(implementExprs, modifier, changedWhere);
    }

    @Override
    public void fillDepends(Set<Property> depends) {
        fillDepends(depends,implement.mapping.values());
        depends.add(implement.property);       
    }

    @Override
    public ChangeProperty getChangeProperty(DataSession session, Map<JoinPropertyInterface, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier, ChangePropertySecurityPolicy securityPolicy, boolean externalID) throws SQLException {
        if(implement.mapping.size()==1)
            return BaseUtils.singleValue(implement.mapping).mapGetChangeProperty(session, interfaceValues, modifier, securityPolicy, externalID);
        if(implement.property instanceof CompareFormulaProperty && ((CompareFormulaProperty)implement.property).compare == Compare.EQUALS) { // если =
            assert implement.mapping.size()==2;
            Iterator<PropertyInterfaceImplement<JoinPropertyInterface>> i = implement.mapping.values().iterator();
            PropertyInterfaceImplement<JoinPropertyInterface> op1 = i.next();
            PropertyInterfaceImplement<JoinPropertyInterface> op2 = i.next();
            DataChangeProperty joinData;
            if((joinData=op1.mapGetJoinChangeProperty(session, interfaceValues, modifier, securityPolicy, externalID))!=null)
                return new EqualsChangeProperty(joinData, op2.read(session, interfaceValues, modifier));
            if((joinData=op2.mapGetJoinChangeProperty(session, interfaceValues, modifier, securityPolicy, externalID))!=null)
                return new EqualsChangeProperty(joinData, op1.read(session, interfaceValues, modifier));
        }
        return null;
    }

    @Override
    public DataChangeProperty getJoinChangeProperty(DataSession session, Map<JoinPropertyInterface, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier, ChangePropertySecurityPolicy securityPolicy, boolean externalID) throws SQLException {
        Map<T,DataObject> propertyImplement = new HashMap<T, DataObject>();
        for(Map.Entry<T,PropertyInterfaceImplement<JoinPropertyInterface>> interfaceImplement : implement.mapping.entrySet()) {
            ObjectValue implementValue = interfaceImplement.getValue().read(session, interfaceValues, modifier);
            if(implementValue instanceof DataObject)
                propertyImplement.put(interfaceImplement.getKey(), (DataObject) implementValue);
            else
                return null;
        }
        return implement.property.getJoinChangeProperty(session, propertyImplement, modifier, securityPolicy, externalID);
    }
}
