package lsfusion.server.language.parserPostProcess;

import java.io.*;

public class LSFLogicsParserPostProcess {

    public static void main(String[] args) {

        File parserFile = new File("platform/server/src/main/java/lsfusion/server/language/LsfLogicsParser.java");
        File tokenNamesFile = new File(parserFile.getParent() + "/LsfLogicsParserTokenNames.java");

        String startLine = "public static final String[] tokenNames = new String[] {";
        String endLine = "};";

        String tokenNamesLine = "@Override public String[] getTokenNames() { return LsfLogicsParser.tokenNames; }";
        String tokenNamesReplaceLine = "@Override public String[] getTokenNames() { return LsfLogicsParserTokenNames.tokenNames; }";

        try {

            File tempParserFile = new File(parserFile.getParent()  + "/LsfLogicsParser.tmp");
            File tempTokenNamesFile = new File(parserFile.getParent() + "/LsfLogicsParserTokenNames.tmp");

            boolean found = false;
            try( BufferedReader reader = new BufferedReader(new FileReader(parserFile));
                 PrintWriter writer1 = new PrintWriter(new FileWriter(tempParserFile));
                 PrintWriter writer2 = new PrintWriter(new FileWriter(tempTokenNamesFile))) {

                boolean tokenNames = false;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (tokenNames) {
                        writer2.println(line);
                        if (line.trim().equals(endLine)) {
                            writer2.println("}");
                            tokenNames = false;
                        }
                    } else {
                        if (line.trim().equals(startLine)) {
                            found = true;
                            writer2.println("package lsfusion.server.language;\n\npublic class LsfLogicsParserTokenNames {");
                            writer2.println(line);
                            tokenNames = true;
                        } else if(line.trim().equals(tokenNamesLine)) {
                            writer1.println(tokenNamesReplaceLine);
                        } else {
                            writer1.println(line);
                        }
                    }
                }
            }

            if(found) {
                delete(parserFile);
                delete(tokenNamesFile);

                rename(tempTokenNamesFile, tokenNamesFile);
                rename(tempParserFile, parserFile);

            } else {
                delete(tempParserFile);
                delete(tempTokenNamesFile);
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
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
