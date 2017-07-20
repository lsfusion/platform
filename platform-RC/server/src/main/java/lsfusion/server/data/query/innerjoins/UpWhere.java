package lsfusion.server.data.query.innerjoins;

import lsfusion.server.data.where.Where;

public interface UpWhere {

    UpWhere or(UpWhere upWhere);

    UpWhere and(UpWhere upWhere);

    UpWhere not();

    Where getWhere();

}
