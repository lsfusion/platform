package platform.server.data.query.translators;

import platform.server.data.query.Context;
import platform.server.where.Where;

public class PackTranslator extends DirectJoinTranslator {

    public PackTranslator(Context packContext, Where where) {
        context = new Context(packContext,where,this);
    }
}
