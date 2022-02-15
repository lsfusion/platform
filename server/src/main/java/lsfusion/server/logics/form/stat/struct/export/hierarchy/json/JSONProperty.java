package lsfusion.server.logics.form.stat.struct.export.hierarchy.json;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.simple.SingletonSet;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.form.open.MappedForm;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.stat.struct.hierarchy.*;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JSONProperty {

    public <O extends ObjectSelector> ScriptingLogicsModule.LPWithParams addScriptedJSONProp(MappedForm<O> mapped/*, final List<String> ids, List<Boolean> literals, List<LPWithParams> exprs*/) throws ScriptingErrorLog.SemanticErrorException {
        FormParseNode parseNode = (FormParseNode) mapped.form.getNFStaticForm().getStaticHierarchy(false, SetFact.EMPTY(), null).getIntegrationHierarchy();
        return addScriptedJSONPropRecursive(parseNode);
    }

    public <O extends ObjectSelector> ScriptingLogicsModule.LPWithParams addScriptedJSONPropRecursive(ParseNode parseNode) throws ScriptingErrorLog.SemanticErrorException {

        if(parseNode instanceof FormParseNode) {
            List<ScriptingLogicsModule.LPWithParams> params = new ArrayList<>();
            List<String> integrationSIDs = new ArrayList<>();
            for (ParseNode child : ((FormParseNode)parseNode).children) {
                params.add(addScriptedJSONPropRecursive(child));
                integrationSIDs.add(getIntegrationSID(child));
            }
            return addScriptedJProp(addJFUProp(integrationSIDs, params.size()), params);

        } else if(parseNode instanceof GroupObjectParseNode) {
            GroupObjectParseNode groupNode = (GroupObjectParseNode) parseNode;
            List<ScriptingLogicsModule.LPWithParams> params = new ArrayList<>();
            List<String> integrationSIDs = new ArrayList<>();
            for(ParseNode c : groupNode.children) {
                params.add(addScriptedJSONPropRecursive(c));
                integrationSIDs.add(getIntegrationSID(c));
            }

            ScriptingLogicsModule.LPWithParams concatProp = addScriptedJProp(addJFUProp(integrationSIDs, params.size()), params);

            List<ScriptingLogicsModule.TypedParameter> newContext = Arrays.asList(new ScriptingLogicsModule.TypedParameter(((ObjectEntity) (((SingletonSet) groupNode.group.objects).get(0))).baseClass, groupNode.group.getSID()));
            ScriptingLogicsModule.LPContextIndependent ci = addScriptedCDIGProp(0/*oldContextSize*/, new ArrayList<>(), ScriptingLogicsModule.GroupingType.JCONCAT, Arrays.asList(concatProp), Arrays.asList(new ScriptingLogicsModule.LPWithParams(null, Arrays.asList(0))), true, null, newContext);
            return new ScriptingLogicsModule.LPWithParams(ci.property, ci.usedContext);

        } else if(parseNode instanceof PropertyGroupParseNode) {
            return null;//todo

            /*PropertyGroupParseNode groupNode = (PropertyGroupParseNode) parseNode;
            List<LPWithParams> params = new ArrayList<>();
            List<String> integrationSIDs = new ArrayList<>();
            for(ParseNode c : groupNode.children) {
                params.add(addScriptedJSONPropRecursive(c));
                integrationSIDs.add(getIntegrationSID(c));
            }

            LPWithParams concatProp = addScriptedJProp(addJFUProp(integrationSIDs, params.size()), params);

            List<TypedParameter> newContext = Arrays.asList(new TypedParameter((ValueClass) null*//*((ObjectEntity) (((SingletonSet) groupNode.group.objects).get(0))).baseClass*//*, groupNode.group.getIntegrationSID()));
            LPContextIndependent ci = addScriptedCDIGProp(0*//*oldContextSize*//*, new ArrayList<>(), GroupingType.JCONCAT, Arrays.asList(concatProp), Arrays.asList(new LPWithParams(null, Arrays.asList(0))), true, null, newContext);
            return new LPWithParams(ci.property, ci.usedContext);*/

        } else {
            assert parseNode instanceof PropertyParseNode;
            Property property = ((PropertyParseNode) parseNode).getProperty();
            return new ScriptingLogicsModule.LPWithParams(new LP(property), IntStream.range(0, property.getInterfaceCount()).boxed().collect(Collectors.toList()));
        }
    }

    private String getIntegrationSID(ParseNode node) {
        if (node instanceof GroupObjectParseNode) {
            return ((GroupObjectParseNode) node).getKey();
        } else if (node instanceof PropertyGroupParseNode) {
            return ((PropertyGroupParseNode) node).getKey();
        } else {
            assert node instanceof PropertyParseNode;
            return ((PropertyParseNode) node).getKey();
        }
    }

    private ScriptingLogicsModule.LPWithParams groupObjectParseNodeToGroupConcatProp(GroupObjectParseNode child) throws ScriptingErrorLog.SemanticErrorException {

        List<ScriptingLogicsModule.LPWithParams> params = new ArrayList<>();
        List<String> integrationSIDs = new ArrayList<>();
        for(ParseNode c : child.children) {
            params.add(new ScriptingLogicsModule.LPWithParams(new LP(((PropertyParseNode) c).getProperty()), 0));
            integrationSIDs.add(((PropertyParseNode)c).getKey());
        }

        ScriptingLogicsModule.LPWithParams concatProp = addScriptedJProp(addJFUProp(integrationSIDs, params.size()), params);

        List<ScriptingLogicsModule.TypedParameter> newContext = Arrays.asList(new ScriptingLogicsModule.TypedParameter(((ObjectEntity) (((SingletonSet) child.group.objects).get(0))).baseClass, child.group.getSID()));
        ScriptingLogicsModule.LPContextIndependent ci = addScriptedCDIGProp(0/*oldContextSize*/, new ArrayList<>(), ScriptingLogicsModule.GroupingType.JCONCAT, Arrays.asList(concatProp), Arrays.asList(new ScriptingLogicsModule.LPWithParams(null, Arrays.asList(0))), true, null, newContext);
        return new ScriptingLogicsModule.LPWithParams(ci.property, ci.usedContext);

    }

    private ScriptingLogicsModule.LPWithParams propertyToLPWithParams(Property property, List<Integer> usedParams) {
        return new ScriptingLogicsModule.LPWithParams(new LP(property), usedParams);
    }



}

