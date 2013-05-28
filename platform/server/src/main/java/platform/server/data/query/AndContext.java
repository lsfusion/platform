package platform.server.data.query;

import platform.server.data.where.Where;

public interface AndContext<T extends AndContext<T>> {

    T and(Where where);
}
