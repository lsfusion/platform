package platform.server.data.where;

import platform.server.data.translator.QueryTranslator;
import platform.server.data.translator.MapTranslate;

public interface CheckWhere {

    boolean isTrue();
    boolean isFalse();
    
    boolean checkTrue();
    boolean directMeansFrom(AndObjectWhere where);

    boolean means(CheckWhere where);
    
    CheckWhere andCheck(CheckWhere where); // чисто для means
    CheckWhere orCheck(CheckWhere where); // чисто для means

    AndObjectWhere[] getAnd(); // protected

    CheckWhere translate(MapTranslate translator);

    CheckWhere not();
}
