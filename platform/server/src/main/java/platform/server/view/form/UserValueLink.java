package platform.server.view.form;

import platform.base.BaseUtils;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.types.Type;
import platform.server.logics.ObjectValue;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.DefaultData;
import platform.server.logics.properties.Property;
import platform.server.session.TableChanges;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class UserValueLink extends ValueLink {

    public final ObjectValue value;

    public UserValueLink(ObjectValue iValue) {
        value = iValue;
    }

    public SourceExpr getValueExpr(Set<GroupObjectImplement> classGroup, Map<ObjectImplement, ? extends SourceExpr> classSource, TableChanges session, Type DBType, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps) throws SQLException {
        return value.getExpr();
    }

    public UserValueLink(DataInputStream inStream,RemoteForm form,Type DBType) throws IOException, SQLException {
        super(inStream,form,DBType);

        value = form.session.getObjectValue(BaseUtils.deserializeObject(inStream),DBType);
    }
}
