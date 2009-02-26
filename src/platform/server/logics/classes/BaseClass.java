package platform.server.logics.classes;

import platform.server.data.types.Type;
import platform.server.logics.data.TableFactory;
import platform.server.logics.session.DataSession;

import java.sql.SQLException;
import java.text.Format;
import java.util.List;
import java.util.Map;
import java.util.Random;

class BaseClass extends RemoteClass {

    BaseClass(Integer iID, String iCaption) {
        super(iID, iCaption);
    }

    public Type getType() {
        return Type.integer;
    }// получает рандомный объект

    public Object getRandomObject(DataSession session, TableFactory tableFactory, Integer diap, Random randomizer) throws SQLException {
        return null;
    }

    public Object getRandomObject(Map<RemoteClass, List<Integer>> objects, Random randomizer, Integer diap) throws SQLException {
        return null;
    }

    public Class getJavaClass() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Format getDefaultFormat() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte getTypeID() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
