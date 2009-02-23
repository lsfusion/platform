package platform.server.logics.classes;

import java.util.Random;
import java.util.List;
import java.util.Map;
import java.sql.SQLException;

import platform.server.data.types.Type;
import platform.server.logics.session.DataSession;
import platform.server.logics.data.TableFactory;

class BaseClass extends DataClass {

    BaseClass(Integer iID, String iCaption) {
        super(iID, iCaption);
    }

    public Type getType() {
        return Type.integer;
    }// получает рандомный объект

    public Object getRandomObject(DataSession session, TableFactory tableFactory, Integer diap, Random randomizer) throws SQLException {
        return null;
    }

    public Object getRandomObject(Map<DataClass, List<Integer>> objects, Random randomizer, Integer diap) throws SQLException {
        return null;
    }
}
