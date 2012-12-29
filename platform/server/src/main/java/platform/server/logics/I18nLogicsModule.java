package platform.server.logics;

import org.antlr.runtime.RecognitionException;
import platform.server.classes.*;
import platform.server.logics.linear.LCP;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;

import static platform.server.logics.ServerResourceBundle.getString;

public class I18nLogicsModule extends ScriptingLogicsModule {

    public ConcreteCustomClass dictionary;
    public ConcreteCustomClass dictionaryEntry;

    protected LCP termDictionary;
    protected LCP translationDictionary;
    public LCP insensitiveDictionary;
    public LCP insensitiveTermDictionary;
    protected LCP entryDictionary;
    protected LCP nameEntryDictionary;
    public LCP translationDictionaryEntryDictionaryTerm;
    public LCP insensitiveTranslationDictionaryEntryDictionaryTerm;

    public I18nLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(I18nLogicsModule.class.getResourceAsStream("/scripts/I18n.lsf"), baseLM, BL);
        setBaseLogicsModule(baseLM);
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();

        dictionary = (ConcreteCustomClass) getClassByName("dictionary");
        dictionaryEntry = (ConcreteCustomClass) getClassByName("dictionaryEntry");

    }

    @Override
    public void initProperties() throws RecognitionException {
        super.initProperties();

        insensitiveDictionary = getLCPByName("insensitiveDictionary");
        entryDictionary = getLCPByName("entryDictionary");
        termDictionary = getLCPByName("termDictionary");
        insensitiveTermDictionary = getLCPByName("insensitiveTerm");
        translationDictionary = getLCPByName("translationDictionary");
        translationDictionaryEntryDictionaryTerm = getLCPByName("translationDictionaryEntryDictionaryTerm");
        nameEntryDictionary = getLCPByName("nameEntryDictionary");
        insensitiveTranslationDictionaryEntryDictionaryTerm = getLCPByName("insensitiveTranslationDictionaryEntryDictionaryTerm");


    }
}
