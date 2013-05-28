package platform.utils;

import org.springframework.util.PropertyPlaceholderHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import platform.base.IOUtils;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class AntBuildFileGenerator {
    private static final PropertyPlaceholderHelper stringResolver = new PropertyPlaceholderHelper("${", "}", ":", true);
    private static final String moduleTargetsTemplate =
            "    <target name=\"${moduleName}-recursiveCompile\">\n" +
            "        <antcall target=\"recursiveCompile\">\n" +
            "            <param name=\"artifactId\" value=\"${moduleName}\"/>\n" +
            "        </antcall>\n" +
            "    </target>\n" +
            "\n" +
            "    <target name=\"${moduleName}-quickCompile\">\n" +
            "        <antcall target=\"quickCompile\">\n" +
            "            <param name=\"artifactId\" value=\"${moduleName}\"/>\n" +
            "        </antcall>\n" +
            "    </target>\n" +
            "\n";

    private static final String webModuleWithGWTTargetTemplate =
            "    <target name=\"${moduleName}-recursiveCompileWithGwt\">\n" +
            "        <antcall target=\"recursiveCompileWithGwt\">\n" +
            "            <param name=\"artifactId\" value=\"${moduleName}\"/>\n" +
            "        </antcall>\n" +
            "    </target>\n" +
            "\n";

    private static Set<String> webModulesWithGWT = new HashSet<String>(Arrays.asList("web-client", "paas-web"));

    public static void main(String[] args) {
        try {
            Document document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new File("../pom.xml"));

            NodeList modulesNodes = document.getElementsByTagName("module");
            String modulesTargets = "";
            for (int i = 0; i < modulesNodes.getLength(); ++i) {
                Node item = modulesNodes.item(i);
                String moduleName = item.getTextContent();

                modulesTargets += resolveString(moduleTargetsTemplate, "moduleName", moduleName);
                if (webModulesWithGWT.contains(moduleName)) {
                    modulesTargets += resolveString(webModuleWithGWTTargetTemplate, "moduleName", moduleName);
                }
            }

            String templateContent = IOUtils.readStreamToString(new FileInputStream("../build.template.xml"));
            String buildFileContent = resolveString(templateContent, "modulesTargets", modulesTargets);

            PrintStream ps = new PrintStream("../build.xml");
            ps.print(buildFileContent);
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String resolveString(String template, String propName, String propValue) {
        Properties props = new Properties();
        props.put(propName, propValue);

        return stringResolver.replacePlaceholders(template, props);
    }
}
