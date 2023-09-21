package lsfusion.server.language.parserPostProcess;

import java.io.*;

public class LSFLogicsParserPostProcess {
    private static final int BITSET_FILE_LINES_LIMIT = 2000;

    public static void main(String[] args) {
        String baseDir = args[0];
        File parserFile = new File(baseDir + "/src/main/java/lsfusion/server/language/LsfLogicsParser.java");
        String parserDir = parserFile.getParent();

        File tokenNamesFile = new File(parserDir + "/LsfLogicsParserTokenNames.java");
        String tokenNameStartLine = "public static final String[] tokenNames = new String[] {";
        String tokenNameEndLine = "};";
        String tokenNamesLine = "@Override public String[] getTokenNames() { return LsfLogicsParser.tokenNames; }";
        String tokenNamesReplaceLine = "@Override public String[] getTokenNames() { return LsfLogicsParserTokenNames.tokenNames; }";

        int bitsetLines = 0;
        File bitsetFollowFile = null;
        String bitsetFollowStartLine = "public static final BitSet FOLLOW_";
        String bitsetFollowEndLine = "}";
        String bitsetImportLine = "import org.antlr.runtime.BitSet;";
        int bitsetFileParts = 0;

        try {
            File tempParserFile = new File(parserDir + "/LsfLogicsParser.tmp");
            File tempTokenNamesFile = new File(parserDir + "/LsfLogicsParserTokenNames.tmp");
            File tempBitsetFollowFile = null;

            boolean foundTokenNames = false;
            boolean foundBitsetFollow = false;

            PrintWriter writer3 = null;
            try( BufferedReader reader = new BufferedReader(new FileReader(parserFile));
                 PrintWriter writer1 = new PrintWriter(new FileWriter(tempParserFile));
                 PrintWriter writer2 = new PrintWriter(new FileWriter(tempTokenNamesFile));
            ) {
                boolean tokenNames = false;
                boolean bitset = false;
                String firstBitsetLine = null;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (tokenNames) {
                        writer2.println(line);
                        if (line.trim().equals(tokenNameEndLine)) {
                            writer2.println("}");
                            tokenNames = false;
                        }
                    } else if (bitset) {
                        if (writer3 == null) {
                            bitsetFileParts = bitsetLines / BITSET_FILE_LINES_LIMIT;
                            String filePart = bitsetFileParts == 0 ? "" : "" + bitsetFileParts;
                            tempBitsetFollowFile = new File(parserDir + "/LsfLogicsParserBitsetFollow" + filePart + ".tmp");
                            bitsetFollowFile = new File(parserDir + "/LsfLogicsParserBitsetFollow" + filePart + ".java");
                            writer3 = new PrintWriter(new FileWriter(tempBitsetFollowFile));
                            writer3.println("package lsfusion.server.language;\n\nimport org.antlr.runtime.BitSet;\n\npublic class LsfLogicsParserBitsetFollow" + filePart +" {");
                            if (firstBitsetLine != null) {
                                writer3.println(firstBitsetLine);
                                bitsetLines += 1;
                                firstBitsetLine = null;
                            }
                        }
                        
                        writer3.println(line);
                        bitsetLines += 1;
                        
                        if (bitsetLines % BITSET_FILE_LINES_LIMIT == 0) {
                            writer3.write(bitsetFollowEndLine);
                            writer3.close();
                            replaceChangedFile(true, bitsetFollowFile, tempBitsetFollowFile);
                            writer3 = null;
                        }

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
                        } else if (line.trim().startsWith(bitsetFollowStartLine)) {
                            foundBitsetFollow = true;
                            firstBitsetLine = line;
                            bitset = true;
                        } else {
                            writer1.println(line);
                        }
                    }
                }
            }

            if (foundBitsetFollow) {
                writer3.close();

                // second run just for bitset imports, as their number is unknown during first run
                File tempParserFile2 = new File(parserDir + "/LsfLogicsParser2.tmp");
                try (BufferedReader reader = new BufferedReader(new FileReader(tempParserFile));
                     PrintWriter writer = new PrintWriter(new FileWriter(tempParserFile2));
                ) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().equals(bitsetImportLine)) {
                            for (int i = 0; i <= bitsetFileParts; i++) {
                                writer.println("\timport static lsfusion.server.language.LsfLogicsParserBitsetFollow" + (i == 0 ? "" : i) + ".*;");
                            }
                        } else {
                            writer.println(line);
                        }
                    }
                }
                replaceChangedFile(true, tempParserFile, tempParserFile2);
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
        if (f != null && f.exists() && !f.delete()) {
            throw new RuntimeException("Could not delete file " + f.getAbsolutePath());
        }
    }

    private static void rename(File from, File to) {
        if (!from.renameTo(to)) {
            throw new RuntimeException("Could not rename file " + from.getAbsolutePath());
        }
    }
}
