package lsfusion.server.physics.dev.i18n;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ResourceBundleGenerator {
    private final File bundleFile;
    private final Set<String> alreadyAdded;

    private static final String russianAlphabet = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя";
    private static final String[] translitaration = {"a", "b", "v", "g", "d", "e", "e", "zh", "z", "i", "i", "k", "l", "m", 
        "n", "o", "p", "r", "s", "t", "u", "f", "kh", "ts", "ch", "sh", "shch", "ie", "y", "", "e", "iu", "ia"};
    
    private static final Map<Character, String> transliterationMap = new HashMap<>();
    static {
        for (int i = 0; i < russianAlphabet.length(); ++i) {
            transliterationMap.put(russianAlphabet.charAt(i), translitaration[i]);            
        }
    }
    
    public ResourceBundleGenerator(String filename) {
        bundleFile = new File(System.getProperty("java.io.tmpdir") + "/" + filename + ".properties");
        FileWriter writer;
        try {
            writer = new FileWriter(bundleFile);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        alreadyAdded = new HashSet<>();
    }
    
    synchronized public void appendEntry(String s) {
        if (!alreadyAdded.contains(s)) {
            try {
                try (FileWriter writer = new FileWriter(bundleFile, true)) {
                    writer.write(createBundleEntry(s) + '\n');
                }
                alreadyAdded.add(s);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private String createBundleEntry(String s) {
        String key = createBundleKey(s);
        return key + " = " + s;
    }
    
    private String createBundleKey(String value) {
        StringBuilder builder = new StringBuilder();
        boolean isStart = true;
        for (int i = 0; i < value.length(); ++i) {
            char ch = Character.toLowerCase(value.charAt(i));
            if (ch >= 'a' && ch <= 'z' || ch == '_' || Character.isDigit(ch) && builder.length() > 0) {
                builder.append(ch);
                isStart = false;
            } else if (Character.UnicodeBlock.of(ch).equals(Character.UnicodeBlock.CYRILLIC)) {
                builder.append(transliterationMap.get(ch));
                isStart = false;
            } else if (!isStart) {
                isStart = true;
                builder.append('_');
            }
        }
        return builder.toString();
    }
}
