package platform.server.view.form;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.types.Type;
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

abstract public class ValueLink {

//    ClassSet getValueClass(GroupObjectImplement ClassGroup) {return null;}

    boolean classUpdated(GroupObjectImplement ClassGroup) {return false;}

    boolean objectUpdated(GroupObjectImplement ClassGroup) {return false;}

    public abstract SourceExpr getValueExpr(Set<GroupObjectImplement> classGroup, Map<ObjectImplement, ? extends SourceExpr> classSource, TableChanges session, Type DBType, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps) throws SQLException;

    protected ValueLink() {
    }

    protected ValueLink(DataInputStream inStream,RemoteForm form,Type DBType) {
    }

    public static ValueLink deserialize(DataInputStream inStream, RemoteForm form, Type DBType) throws IOException, SQLException {
        byte type = inStream.readByte();
        if(type==0) return new UserValueLink(inStream,form,DBType);
        if(type==1) return new ObjectValueLink(inStream,form,DBType);
        if(type==2) return new PropertyValueLink(inStream,form,DBType);

        throw new IOException();
    }

}
