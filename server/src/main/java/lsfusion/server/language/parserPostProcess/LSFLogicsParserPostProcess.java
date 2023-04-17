package lsfusion.server.language.parserPostProcess;

import java.io.*;

public class LSFLogicsParserPostProcess {

    public static void main(String[] args) {
        String baseDir = args[0];
        File parserFile = new File(baseDir + "/src/main/java/lsfusion/server/language/LsfLogicsParser.java");

        File tokenNamesFile = new File(parserFile.getParent() + "/LsfLogicsParserTokenNames.java");
        String tokenNameStartLine = "public static final String[] tokenNames = new String[] {";
        String tokenNameEndLine = "};";
        String tokenNamesLine = "@Override public String[] getTokenNames() { return LsfLogicsParser.tokenNames; }";
        String tokenNamesReplaceLine = "@Override public String[] getTokenNames() { return LsfLogicsParserTokenNames.tokenNames; }";

        File bitsetFollowFile = new File(parserFile.getParent() + "/LsfLogicsParserBitsetFollow.java");
        String bitsetFollowStartLine = "public static final BitSet FOLLOW_";
        String bitsetFollowEndLine = "}";
        String bitsetImportLine = "import org.antlr.runtime.BitSet;";
        String bitsetImportReplaceLine = "import static lsfusion.server.language.LsfLogicsParserBitsetFollow.*;";

        try {
            File tempParserFile = new File(parserFile.getParent()  + "/LsfLogicsParser.tmp");
            File tempTokenNamesFile = new File(parserFile.getParent() + "/LsfLogicsParserTokenNames.tmp");
            File tempBitsetFollowFile = new File(parserFile.getParent() + "/LsfLogicsParserBitsetFollow.tmp");

            boolean foundTokenNames = false;
            boolean foundBitsetFollow = false;

            try( BufferedReader reader = new BufferedReader(new FileReader(parserFile));
                 PrintWriter writer1 = new PrintWriter(new FileWriter(tempParserFile));
                 PrintWriter writer2 = new PrintWriter(new FileWriter(tempTokenNamesFile));
                 PrintWriter writer3 = new PrintWriter(new FileWriter(tempBitsetFollowFile))) {

                boolean tokenNames = false;
                boolean bitset = false;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (tokenNames) {
                        writer2.println(line);
                        if (line.trim().equals(tokenNameEndLine)) {
                            writer2.println("}");
                            tokenNames = false;
                        }
                    } else if (bitset) {
                        writer3.println(line);
                        if (line.trim().equals(bitsetFollowEndLine)) {
                            writer1.println(line); // The LsfLogicsParser ends after the last BitSet
                            bitset = false;
                        }
                    } else {
                        if (line.trim().equals(tokenNameStartLine)) {
                            foundTokenNames = true;
                            writer2.println("package lsfusion.server.language;\n\npublic class LsfLogicsParserTokenNames {");
                            writer2.println(line);
                            tokenNames = true;
                        } else if(line.trim().equals(tokenNamesLine)) {
                            writer1.println(tokenNamesReplaceLine);
                        } else if (line.trim().equals(bitsetImportLine)) {
                            writer1.println(bitsetImportReplaceLine);
                        } else if (line.trim().startsWith(bitsetFollowStartLine)) {
                            foundBitsetFollow = true;
                            writer3.println("package lsfusion.server.language;\n\nimport org.antlr.runtime.BitSet;\n\npublic class LsfLogicsParserBitsetFollow {");
                            writer3.println(line);
                            bitset = true;
                        } else {
                            writer1.println(line);
                        }
                    }
                }
            }

            if (foundTokenNames || foundBitsetFollow) {
                delete(parserFile);

                replaceChangedFile(foundTokenNames, tokenNamesFile, tempTokenNamesFile);
                replaceChangedFile(foundBitsetFollow, bitsetFollowFile, tempBitsetFollowFile);

                rename(tempParserFile, parserFile);
            } else {
                clear(tempParserFile, tempTokenNamesFile, tempBitsetFollowFile);
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }

    private static void clear(File... tmpFiles) {
        for (File tmpFile : tmpFiles) {
            delete(tmpFile);
        }
    }

    private static void replaceChangedFile(boolean fileChanged, File originalFile, File tmpFile) {
        if (fileChanged) {
            delete(originalFile);
            rename(tmpFile, originalFile);
        }
    }

    private static void delete(File f) {
        if (f.exists() && !f.delete()) {
            throw new RuntimeException("Could not delete file " + f.getAbsolutePath());
        }
    }

    private static void rename(File from, File to) {
        if (!from.renameTo(to)) {
            throw new RuntimeException("Could not rename file " + from.getAbsolutePath());
        }
    }
}
