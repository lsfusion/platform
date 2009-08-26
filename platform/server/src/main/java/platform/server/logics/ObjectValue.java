package platform.server.logics;

import platform.server.data.classes.ConcreteClass;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.sql.SQLSyntax;
import platform.server.view.form.filter.CompareValue;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.ObjectImplement;
import platform.server.session.TableChanges;
import platform.server.logics.properties.Property;
import platform.server.where.Where;
import platform.interop.Compare;

import java.util.Set;
import java.util.Map;
import java.util.Collection;
import java.sql.SQLException;

public abstract class ObjectValue implements CompareValue {

    public abstract String getString(SQLSyntax syntax);

    public abstract boolean isString(SQLSyntax syntax);

    public abstract SourceExpr getExpr();

    public abstract Object getValue();

    public static ObjectValue getValue(Object value, ConcreteClass objectClass) {
        if(value==null)
            return NullValue.instance;
        else
            return new DataObject(value, objectClass);
    }

    public SourceExpr getSourceExpr(Set<GroupObjectImplement> classGroup, Map<ObjectImplement, ? extends SourceExpr> classSource, TableChanges session, Property.TableDepends<? extends Property.TableUsedChanges> depends) throws SQLException {
        return getExpr();
    }
    
    public boolean classUpdated(GroupObjectImplement classGroup) {return false;}
    public boolean objectUpdated(GroupObjectImplement classGroup) {return false;}
    public boolean dataUpdated(Collection<Property> changedProps) {return false;}
    public void fillProperties(Set<Property> properties) {}
    public boolean isInInterface(GroupObjectImplement classGroup) {return true;}

    public abstract Where order(SourceExpr expr, boolean desc, Where orderWhere);

}
