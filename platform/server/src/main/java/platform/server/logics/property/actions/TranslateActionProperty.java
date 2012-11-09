package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.server.classes.StringClass;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class TranslateActionProperty extends UserActionProperty {
    private LCP sourceProperty;
    private LCP targetProperty;
    private LCP translationDictionaryTerm;
    private LCP insensitiveTranslationDictionaryTerm;
    private LCP insensitiveDictionary;
    
    public TranslateActionProperty(String sID, String caption, LCP translationDictionaryTerm, LCP insensitiveTranslationDictionaryTerm,
                                   LCP insensitiveDictionary, LCP sourceProperty, LCP targetProperty, ValueClass dictionary) {
        super(sID, caption, getValueClasses(sourceProperty, dictionary));

        this.translationDictionaryTerm = translationDictionaryTerm;
        this.insensitiveTranslationDictionaryTerm = insensitiveTranslationDictionaryTerm;
        this.insensitiveDictionary = insensitiveDictionary;
        this.sourceProperty = sourceProperty;
        this.targetProperty = targetProperty;
    }

    private static ValueClass[] getValueClasses(LCP sourceProperty, ValueClass dictionary) {
        List<ValueClass> result = new ArrayList<ValueClass>();
        result.add(dictionary);
        result.addAll(BaseUtils.toList(sourceProperty.getInterfaceClasses()));
        return result.toArray(new ValueClass[result.size()]);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        List<ClassPropertyInterface> interfacesList = new ArrayList<ClassPropertyInterface>(interfaces);
        DataObject dictionary = context.getKeyValue(interfacesList.remove(0));
        Boolean insensitive = insensitiveDictionary.read(context.getSession(), dictionary) != null;
        List<DataObject> inputObjects = BaseUtils.mapList(interfacesList, context.getKeys());

        String source = (String) sourceProperty.read(context, inputObjects.toArray(new DataObject[inputObjects.size()]));
        if(insensitive)
            source = source.toUpperCase();
        if (source != null) {
            String delim = ", .:;%#$@/\\|<>=+-_)(*&?^!~{}[]\"1234567890'";
            StringTokenizer st = new StringTokenizer(source, delim, true);
            String result = "";
            String fullLineTranslation = (String) (insensitive ? insensitiveTranslationDictionaryTerm : translationDictionaryTerm).read(context, dictionary, new DataObject(source, StringClass.get(50)));
            if (fullLineTranslation != null) {
                result = fullLineTranslation.trim();
            } else {
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    if (!delim.contains(token.subSequence(0, token.length()))) {
                        String translation = (String) (insensitive? insensitiveTranslationDictionaryTerm : translationDictionaryTerm).read(context, dictionary, new DataObject(token, StringClass.get(50)));
                        if (translation != null) {
                            token = translation.trim();
                        }
                    }
                    result += token;
                }
            }
            targetProperty.change(result, context, inputObjects.toArray(new DataObject[inputObjects.size()]));
        }
    }
}
