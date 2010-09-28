package platform.server.form.instance;

import platform.server.data.expr.Expr;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.sql.SQLException;

public interface PropertyReadInstance {

    public PropertyObjectInstance getPropertyObject();

    public byte getTypeID();

    public int getID(); // ID в рамках Type

    public List<ObjectInstance> getSerializeList(Set<PropertyDrawInstance> panelProperties);
}
