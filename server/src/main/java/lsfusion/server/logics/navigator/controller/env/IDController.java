package lsfusion.server.logics.navigator.controller.env;

import lsfusion.base.Pair;

public interface IDController {

    long generateID();
    Pair<Long, Long>[] generateIDs(long count);
}
