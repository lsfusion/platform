package platform.server.logics.classes;

import java.util.Random;
import java.util.List;
import java.util.Map;
import java.sql.SQLException;

import platform.server.data.types.Type;
import platform.server.logics.data.TableFactory;
import platform.server.logics.session.DataSession;

public class BitClass extends IntegralClass {
    BitClass(Integer iID, String caption) {super(iID, caption);}

    public Type getType() {
        return Type.bit;
    }

    public Object getRandomObject(DataSession session, TableFactory tableFactory, Integer diap, Random randomizer) throws SQLException {
        return randomizer.nextBoolean();
    }

    public Object getRandomObject(Map<DataClass, List<Integer>> objects, Random randomizer, Integer diap) throws SQLException {
        return randomizer.nextBoolean();
    }
}
