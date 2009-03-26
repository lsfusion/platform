package platform.server.view.form;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.types.Type;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.session.DataSession;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

abstract public class ValueLink {

    ClassSet getValueClass(GroupObjectImplement ClassGroup) {return null;}

    boolean ClassUpdated(GroupObjectImplement ClassGroup) {return false;}

    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {return false;}

    public abstract SourceExpr getValueExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, ? extends SourceExpr> ClassSource, DataSession Session, Type DBType);

    protected ValueLink() {
    }

    protected ValueLink(DataInputStream inStream,RemoteForm form) {
    }

    public static ValueLink deserialize(DataInputStream inStream,RemoteForm form) throws IOException {
        byte type = inStream.readByte();
        if(type==0) return new UserValueLink(inStream,form);
        if(type==1) return new ObjectValueLink(inStream,form);
        if(type==2) return new PropertyValueLink(inStream,form);

        throw new IOException();
    }

}
