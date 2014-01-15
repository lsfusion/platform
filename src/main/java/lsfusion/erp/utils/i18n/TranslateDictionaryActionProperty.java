package lsfusion.erp.utils.i18n;

import lsfusion.server.classes.StringClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.StringTokenizer;

public class TranslateDictionaryActionProperty extends ScriptingActionProperty {
    public final ClassPropertyInterface dictionaryInterface;
    public final ClassPropertyInterface termInterface;

    public TranslateDictionaryActionProperty(ScriptingLogicsModule LM) {
        super(LM, new ValueClass[]{LM.getClassByName("Dictionary"), StringClass.get(50)});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        dictionaryInterface = i.next();
        termInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            DataObject dictionaryObject = context.getDataKeyValue(dictionaryInterface);
            DataObject termObject = context.getDataKeyValue(termInterface);

            if (dictionaryObject != null && termObject != null) {

                Boolean insensitive = LM.findLCPByCompoundOldName("insensitiveDictionary").read(context.getSession(), dictionaryObject) != null;

                String source = (String) termObject.object;
                if (insensitive)
                    source = source.toUpperCase();
                if (source != null) {
                    String delim = ", .:;%#$@/\\|<>=+-_)(*&?^!~{}[]\"1234567890'";
                    StringTokenizer st = new StringTokenizer(source, delim, true);
                    String result = "";
                    String fullLineTranslation = (String) (LM.findLCPByCompoundOldName(insensitive ? "insensitiveTranslationDictionaryEntryDictionaryTerm" : "translationDictionaryEntryDictionaryTerm").read(context, dictionaryObject, new DataObject(source, StringClass.get(50))));
                    if (fullLineTranslation != null) {
                        result = fullLineTranslation.trim();
                    } else {
                        while (st.hasMoreTokens()) {
                            String token = st.nextToken();
                            if (!delim.contains(token.subSequence(0, token.length()))) {
                                String translation = (String) (LM.findLCPByCompoundOldName(insensitive ? "insensitiveTranslationDictionaryEntryDictionaryTerm" : "translationDictionaryEntryDictionaryTerm").read(context, dictionaryObject, new DataObject(token, StringClass.get(50))));
                                if (translation != null) {
                                    token = translation.trim();
                                }
                            }
                            result += token;
                        }
                    }
                    LM.findLCPByCompoundOldName("translationResultString").change(result, context);
                }
            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
        }
    }
}
