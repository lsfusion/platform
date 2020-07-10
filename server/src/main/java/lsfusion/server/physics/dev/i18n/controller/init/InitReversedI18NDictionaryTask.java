package lsfusion.server.physics.dev.i18n.controller.init;

import lsfusion.server.logics.controller.init.SimpleBLTask;
import lsfusion.server.physics.dev.i18n.ReversedI18NDictionary;
import org.apache.log4j.Logger;

public class InitReversedI18NDictionaryTask extends SimpleBLTask {
    @Override
    public String getCaption() {
        return "Initializing string literals data";
    }

    @Override
    public void run(Logger logger) {
        String lang = getBL().getLsfStrLiteralsLanguage();
        String country = getBL().getLsfStrLiteralsCountry();
        ReversedI18NDictionary dict;
        if (lang != null) {
            dict = new ReversedI18NDictionary(lang, country);
        } else {
            dict = new ReversedI18NDictionary();
        }
        getBL().setReversedI18nDictionary(dict);
    }
}
