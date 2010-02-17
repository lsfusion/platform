package platform.server.data.query;

import platform.server.data.where.DataWhereSet;
import platform.server.data.translator.KeyTranslator;

public interface InnerJoin {
    DataWhereSet getJoinFollows();

    int hashContext(HashContext hashContext);

    InnerJoin translateDirect(KeyTranslator translator);

    boolean isIn(DataWhereSet set);
}
