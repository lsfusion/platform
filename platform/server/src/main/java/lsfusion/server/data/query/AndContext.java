package lsfusion.server.data.query;

import lsfusion.server.data.where.Where;

public interface AndContext<T extends AndContext<T>> {

    T and(Where where);
}
