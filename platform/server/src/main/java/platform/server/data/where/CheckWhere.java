package platform.server.data.where;

import platform.server.caches.TranslateContext;
import platform.server.data.translator.MapTranslate;

public interface CheckWhere<T extends TranslateContext<T>> extends TranslateContext<T> {

    boolean isTrue();
    boolean isFalse();
    
    boolean checkTrue();
    boolean directMeansFrom(AndObjectWhere where);

    boolean means(CheckWhere where);
    
    CheckWhere andCheck(CheckWhere where); // чисто для means
    CheckWhere orCheck(CheckWhere where); // чисто для means

    AndObjectWhere[] getAnd(); // protected

    CheckWhere not();
}
