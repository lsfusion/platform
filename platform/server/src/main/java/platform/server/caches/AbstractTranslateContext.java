package platform.server.caches;

import platform.server.caches.hash.HashCodeContext;

public abstract class AbstractTranslateContext<This extends TranslateContext> implements TranslateContext<This> {

    boolean hashCoded = false;
    int hashCode;
    @Override
    public int hashCode() {
        if(!hashCoded) {
            hashCode = hashContext(HashCodeContext.instance);
            hashCoded = true;
        }
        return hashCode;
    }
}
