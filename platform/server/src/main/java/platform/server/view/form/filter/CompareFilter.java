package platform.server.view.form.filter;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.types.Type;
import platform.server.logics.properties.Property;
import platform.server.logics.properties.PropertyInterface;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.ObjectImplement;
import platform.server.view.form.PropertyObjectImplement;
import platform.server.view.form.RemoteForm;
import platform.server.where.Where;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class CompareFilter<P extends PropertyInterface> extends PropertyFilter<P> {

    public Compare compare;
    public CompareValue value;

    public CompareFilter(PropertyObjectImplement<P> iProperty,Compare iCompare, CompareValue iValue) {
        super(iProperty);
        compare = iCompare;
        value = iValue;
    }

    public CompareFilter(DataInputStream inStream, RemoteForm form) throws IOException, SQLException {
        super(inStream,form);
        compare = Compare.deserialize(inStream);
        value = deserializeCompare(inStream, form, property.getType());
    }

    private static CompareValue deserializeCompare(DataInputStream inStream, RemoteForm form, Type DBType) throws IOException, SQLException {
        byte type = inStream.readByte();
        switch(type) {
            case 0:
                return form.session.getObjectValue(BaseUtils.deserializeObject(inStream),DBType);
            case 1:
                return form.getObjectImplement(inStream.readInt());
            case 2:
                return form.getPropertyView(inStream.readInt()).view;
        }

        throw new IOException();
    }

    @Override
    public boolean dataUpdated(Collection<Property> changedProps) {
        return super.dataUpdated(changedProps) || value.dataUpdated(changedProps);
    }

    @Override
    public boolean classUpdated(GroupObjectImplement classGroup) {
        return super.classUpdated(classGroup) || value.classUpdated(classGroup);
    }

    @Override
    public boolean objectUpdated(GroupObjectImplement classGroup) {
        return super.objectUpdated(classGroup) || value.objectUpdated(classGroup);
    }

    public Where getWhere(Map<ObjectImplement, KeyExpr> mapKeys, Set<GroupObjectImplement> classGroup, TableModifier<? extends TableChanges> modifier) throws SQLException {
        return property.getSourceExpr(classGroup, mapKeys, modifier).compare(value.getSourceExpr(classGroup, mapKeys, modifier), compare);
    }

    @Override
    public void fillProperties(Set<Property> properties) {
        super.fillProperties(properties);
        value.fillProperties(properties);
    }

    @Override
    public boolean isInInterface(GroupObjectImplement classGroup) {
        return super.isInInterface(classGroup) && value.isInInterface(classGroup);
    }
}
