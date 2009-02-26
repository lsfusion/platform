package platform.server.view.form;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.types.Type;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.logics.session.DataSession;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class ObjectValueLink extends ValueLink {

    public ObjectValueLink(DataInputStream inStream, RemoteForm form) throws IOException {
        super(inStream, form);
        object = form.getObjectImplement(inStream.readInt());
    }

    public ObjectValueLink(ObjectImplement iObject) {
        object =iObject;
    }

    public ObjectImplement object;

    @Override
    ClassSet getValueClass(GroupObjectImplement ClassGroup) {
        if(object.objectClass ==null)
            return new ClassSet();
        else
            return new ClassSet(object.objectClass);
    }

    @Override
    boolean ClassUpdated(GroupObjectImplement ClassGroup) {
        return ((object.updated & ObjectImplement.UPDATED_CLASS)!=0);
    }

    @Override
    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {
        return ((object.updated & ObjectImplement.UPDATED_OBJECT)!=0);
    }

    public SourceExpr getValueExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, ? extends SourceExpr> ClassSource, DataSession Session, Type DBType) {
        return object.getSourceExpr(ClassGroup,ClassSource);
    }
}
