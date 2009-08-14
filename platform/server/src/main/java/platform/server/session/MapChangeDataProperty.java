package platform.server.session;

import platform.base.BaseUtils;
import platform.server.logics.DataObject;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.DataPropertyInterface;
import platform.server.logics.properties.Property;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class MapChangeDataProperty<K> {
    final DataProperty property;
    final Map<DataPropertyInterface,K> mapping;
    final boolean reRead;

    public MapChangeDataProperty(DataProperty iProperty, Map<DataPropertyInterface, K> iMapping, boolean iReRead) {
        property = iProperty;
        mapping = iMapping;
        reRead = iReRead;
    }

    public <V> MapChangeDataProperty(MapChangeDataProperty<V> change, Map<V,K> map, boolean iReRead) {
        property = change.property;
        mapping = BaseUtils.join(change.mapping,map);
        reRead = change.reRead || iReRead;
    }

    public void change(ChangesSession session, Map<K, DataObject> keys, Object newValue, boolean externalID) throws SQLException {
        session.changeProperty(property, BaseUtils.join(mapping, keys), newValue, externalID);
    }

    public void serializeChange(DataOutputStream outStream, DataSession session, Map<K, DataObject> keys, Property.TableDepends<? extends Property.TableUsedChanges> depends) throws IOException, SQLException {
        if(reRead) {
            outStream.writeByte(0);
            BaseUtils.serializeObject(outStream,property.read(session, BaseUtils.join(mapping, keys), depends));
            outStream.writeInt(1);
        } else {
            outStream.writeByte(1);
            property.value.serialize(outStream);
            outStream.writeInt(1);
        }
    }
}
