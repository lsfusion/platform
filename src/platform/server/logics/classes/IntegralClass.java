package platform.server.logics.classes;

import java.util.Random;
import java.util.List;
import java.util.Map;
import java.sql.SQLException;

import platform.server.logics.session.DataSession;
import platform.server.logics.data.TableFactory;
import platform.server.data.types.Type;

// класс который можно сравнивать
public class IntegralClass extends DataClass {

    IntegralClass(Integer iID, String caption) {super(iID, caption);}

    public Object getRandomObject(DataSession session, TableFactory tableFactory, Integer diap, Random randomizer) throws SQLException {
        return randomizer.nextInt(diap * diap +1);
    }

    public Object getRandomObject(Map<DataClass, List<Integer>> objects, Random randomizer, Integer diap) throws SQLException {
        return randomizer.nextInt(diap);
    }

    public Type getType() {
        return Type.integer;
    }
}
