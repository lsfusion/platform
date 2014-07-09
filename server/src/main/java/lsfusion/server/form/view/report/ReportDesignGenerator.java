package lsfusion.server.form.view.report;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.interop.FontInfo;
import lsfusion.interop.form.ColumnUserPreferences;
import lsfusion.interop.form.FormUserPreferences;
import lsfusion.interop.form.GroupObjectUserPreferences;
import lsfusion.interop.form.ReportConstants;
import lsfusion.server.ServerLoggers;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.GroupObjectHierarchy;
import lsfusion.server.form.entity.PropertyObjectEntity;
import lsfusion.server.form.view.FormView;
import lsfusion.server.form.view.GroupObjectView;
import lsfusion.server.form.view.ObjectView;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.session.DataSession;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import net.sf.jasperreports.engine.type.OrientationEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.StretchTypeEnum;

import java.awt.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

import static lsfusion.interop.form.ReportConstants.footerSuffix;
import static lsfusion.interop.form.ReportConstants.headerSuffix;
import static lsfusion.interop.form.ReportConstants.objectSuffix;
import static lsfusion.server.form.entity.GroupObjectHierarchy.ReportNode;

/**
 * User: DAle
 * Date: 09.08.2010
 * Time: 18:07:13
 */

public class ReportDesignGenerator {
    private GroupObjectHierarchy.ReportHierarchy hierarchy;
    private FormView formView;
    private Set<Integer> hiddenGroupsId;
    private FormUserPreferences userPreferences;
    private boolean toExcel;

    private Map<String, LinkedHashSet<List<Object>>> columnGroupObjects;
            
    private static final int defaultPageWidth = 842;  //
    private static final int defaultPageHeight = 595; // эти константы есть в JasperReports Ultimate Guide

    private static final int defaultPageMargin = 20;

    private int pageWidth;
    private int pageUsableWidth;
    private static final int neighboursGap = 5;

    private int rowHeight = 18;
    private int charWidth = 8;
    private boolean toStretch = true; 
    
    private Map<String, JasperDesign> designs = new HashMap<String, JasperDesign>();

    public ReportDesignGenerator(FormView formView, GroupObjectHierarchy.ReportHierarchy hierarchy, Set<Integer> hiddenGroupsId, FormUserPreferences userPreferences, boolean toExcel, Map<String, LinkedHashSet<List<Object>>> columnGroupObjects) {
        this.formView = formView;
        this.hierarchy = hierarchy;
        this.hiddenGroupsId = hiddenGroupsId;
        this.userPreferences = userPreferences;
        this.toExcel = toExcel;
        
        this.columnGroupObjects = columnGroupObjects;

        pageWidth = calculatePageWidth(toExcel);
        pageUsableWidth = pageWidth - defaultPageMargin * 2;
    }

    private int calculatePageWidth(boolean toExcel) {
        if (formView.overridePageWidth != null) {
            return formView.overridePageWidth;
        } else if (!toExcel) {
            return defaultPageWidth;
        } else {
            int maxGroupWidth = defaultPageWidth;
            for (ReportNode reportNode : hierarchy.getAllNodes()) {
                for (GroupObjectEntity group : reportNode.getGroupList()) {
                    if (!hiddenGroupsId.contains(group.getID())) {
                        maxGroupWidth = Math.max(maxGroupWidth, calculateGroupPreferredWidth(group));
                    }
                }
            }
            return maxGroupWidth;
        }
    }

    public Map<String, JasperDesign> generate() throws JRException {
        try {
            BaseLogicsModule baseLM = ThreadLocalContext.getBusinessLogics().LM;
            DataSession session = ThreadLocalContext.getDbManager().createSession();
            charWidth = (Integer) baseLM.reportCharWidth.read(session);
            rowHeight = (Integer) baseLM.reportRowHeight.read(session);
            toStretch = BaseUtils.nvl((Boolean) baseLM.reportToStretch.read(session), false);
            session.close();
        } catch (SQLException e) {
            ServerLoggers.systemLogger.warn("Error when reading report parameters", e);
        } catch (SQLHandledException e) {
            ServerLoggers.systemLogger.warn("Error when reading report parameters", e);
        }
    
        JasperDesign rootDesign = createJasperDesignObject(GroupObjectHierarchy.rootNodeName, true, false);

        iterateChildReports(rootDesign, null, 0);

        return designs;
    }

    private void iterateChildReports(JasperDesign design, ReportNode node, int treeGroupLevel) throws JRException {
        List<ReportNode> children = (node == null ? hierarchy.getRootNodes() : hierarchy.getChildNodes(node));
        for (ReportNode childNode : children) {
            JRDesignBand detail = new JRDesignBand();

            if (node == null) {
                treeGroupLevel = childNode.getGroupLevel();
            }
            String sid = childNode.getID();

            boolean hasTopMargin = (childNode != children.get(0));
            JasperDesign childDesign = createJasperDesignObject(sid, false, hasTopMargin);
            createDesignGroups(childDesign, childNode, node, treeGroupLevel);
            iterateChildReports(childDesign, childNode, treeGroupLevel);

            addParametersToDesign(design, sid);

            JRDesignSubreport subreportElement = new JRDesignSubreport(designs.get(sid));
            setExpressionsToSubreportElement(subreportElement, sid);
            subreportElement.setStretchType(StretchTypeEnum.RELATIVE_TO_BAND_HEIGHT);
            detail.addElement(subreportElement);

            ((JRDesignSection)design.getDetailSection()).addBand(detail);
        }
    }

    private List<ReportDrawField> getAllowedGroupDrawFields(GroupObjectEntity group) {
        List<ReportDrawField> fields = new ArrayList<ReportDrawField>();
        PropertyObjectEntity backgroundProp = group.propertyBackground;

        GroupObjectUserPreferences groupObjectPreferences = null;
        if (userPreferences != null) {
            groupObjectPreferences = userPreferences.getUsedPreferences(group.getSID());
        }
        
        List<Pair<PropertyDrawView, ColumnUserPreferences>> properties = new ArrayList<Pair<PropertyDrawView, ColumnUserPreferences>>();
        
        for (PropertyDrawView property : formView.getPropertiesList()) {
            ColumnUserPreferences columnUserPreferences = null;
            if (groupObjectPreferences != null && groupObjectPreferences.getColumnUserPreferences().containsKey(property.getSID())) {
                columnUserPreferences = groupObjectPreferences.getColumnUserPreferences().get(property.getSID());
            }
            
            properties.add(new Pair<PropertyDrawView, ColumnUserPreferences>(property, columnUserPreferences));
            
        }
        
        Collections.sort(properties, new Comparator<Pair<PropertyDrawView, ColumnUserPreferences>>() {
            @Override
            public int compare(Pair<PropertyDrawView, ColumnUserPreferences> o1, Pair<PropertyDrawView, ColumnUserPreferences> o2) {
                if (o1.second == null || o1.second.userOrder == null)
                    return o2.second == null || o2.second.userOrder == null ? 0 : 1;
                else
                    return o2.second == null || o2.second.userOrder == null ? -1 : (o1.second.userOrder - o2.second.userOrder);
            }
        });
        
        for (Pair<PropertyDrawView, ColumnUserPreferences> prop : properties) {
            GroupObjectEntity applyGroup = prop.first.entity.propertyObject.getApplyObject(formView.entity.getGroupsList());
            GroupObjectEntity drawGroup = prop.first.entity.getToDraw(formView.entity);
            
            boolean hidden = prop.second != null && prop.second.userHide != null && prop.second.userHide;
            
            if (group.equals(drawGroup) && (applyGroup == null || applyGroup == drawGroup) && !hidden) {
                int scale = 1;
                LinkedHashSet<List<Object>> objects;
                if(columnGroupObjects != null && (objects = columnGroupObjects.get(prop.first.getSID())) != null) // на не null на всякий случай проверка
                    scale = objects.size();
                
                ReportDrawField reportField = prop.first.getReportDrawField(charWidth, scale);
                if (reportField != null && (backgroundProp == null || backgroundProp.property != prop.first.entity.propertyObject.property)) {
                    Integer widthUser = prop.second == null ? null : prop.second.userWidth;
                    if (widthUser != null) {
                        reportField.setWidthUser(widthUser);
                    }
                    fields.add(reportField);
                }
            }
        }
        
        return fields;
    }

    private int calculateGroupPreferredWidth(GroupObjectEntity group) {
        int width = 0;
        for (ReportDrawField field : getAllowedGroupDrawFields(group)) {
            width += field.getPreferredWidth();
        }
        return width;
    }

    private void createDesignGroups(JasperDesign design, ReportNode node, ReportNode parent, int treeGroupLevel) throws JRException {
        List<GroupObjectEntity> groups = node.getGroupList();
        for (GroupObjectEntity group : groups) {
            boolean hiddenGroup = hiddenGroupsId.contains(group.getID());
            GroupObjectView groupView = formView.getGroupObject(group);
            PropertyObjectEntity backgroundProp = group.propertyBackground;

            boolean hasColumnGroupProperty = false;
            List<ReportDrawField> drawFields = getAllowedGroupDrawFields(group);
            for (ReportDrawField field : drawFields) {
                hasColumnGroupProperty = hasColumnGroupProperty || field.hasColumnGroupObjects;
                if (field.hasCaptionProperty) {
                    String fieldId = field.sID + headerSuffix;
                    addDesignField(design, fieldId, field.captionClass.getName());
                }
                if (field.hasFooterProperty) {
                    String fieldId = field.sID + footerSuffix;
                    addDesignField(design, fieldId, field.footerClass.getName());
                }
            }

            // todo [dale]: Здесь создается ReportDrawField для propertyBackground свойства, нет проверок на уникальность имени получившегося Field, может нам вообще не нужен этот функционал? 
            String backgroundPropertySID = null;
            if (backgroundProp != null) {
                ReportDrawField reportField = new ReportDrawField(backgroundProp.property.getSID(), "", charWidth);
                backgroundProp.property.getType().fillReportDrawField(reportField);
                addDesignField(design, reportField);
                backgroundPropertySID = reportField.sID;
            }
            
            GroupObjectUserPreferences groupPreferences = userPreferences != null ? userPreferences.getUsedPreferences(group.getSID()) : null;
            FontInfo font = groupPreferences != null ? groupPreferences.fontInfo : null;
            if (font == null) {
                font = formView.get(group).getGrid().design.getFont();
            }

            if (!hiddenGroup) {
                boolean detail = hierarchy.isLeaf(node) && (group == groups.get(groups.size()-1));
                ReportLayout reportLayout;
                
                if (detail) {
                    reportLayout = new ReportDetailLayout(design, rowHeight);
                } else {

                    int captionWidth = 0, preferredWidth = 0;
                    for (ReportDrawField reportField : drawFields) {
                        captionWidth += reportField.getCaptionWidth();
                        preferredWidth += reportField.getPreferredWidth();
                    }

                    if (captionWidth + preferredWidth <= pageUsableWidth && !hasColumnGroupProperty) {
                        JRDesignGroup designGroup = addDesignGroup(design, groupView, "designGroup" + group.getID());
                        reportLayout = new ReportGroupRowLayout(designGroup, rowHeight);
                    } else {
                        JRDesignGroup captionGroup = addDesignGroup(design, groupView, "captionGroup" + group.getID());
                        JRDesignGroup textGroup = addDesignGroup(design, groupView, "textGroup" + group.getID());
                        reportLayout = new ReportGroupColumnLayout(captionGroup, textGroup, rowHeight);
                    }
                }

                int groupsCnt = groups.size();
                int minGroupLevel = node.getGroupLevel() - groupsCnt;

                JRDesignStyle groupCellStyle;
                if (parent != null) {
                    int minParentGroupLevel = parent.getGroupLevel()-parent.getGroupList().size();
                    groupCellStyle = DesignStyles.getGroupStyle(groups.indexOf(group), groupsCnt, minGroupLevel, minParentGroupLevel, treeGroupLevel);
                } else {
                    groupCellStyle = DesignStyles.getGroupStyle(groups.indexOf(group), groupsCnt, minGroupLevel, treeGroupLevel, treeGroupLevel);
                }
                
                if (font != null) {
                    groupCellStyle.setFontSize(font.fontSize);
                    groupCellStyle.setBold(font.isBold());
                    groupCellStyle.setItalic(font.isItalic());
                }
                
                design.addStyle(groupCellStyle);
                JRDesignStyle groupCaptionStyle = groupCellStyle;
                if (backgroundPropertySID != null) {
                    if (detail) {
                        groupCaptionStyle = (JRDesignStyle) groupCellStyle.clone();
                        groupCaptionStyle.setName(groupCellStyle.getName() + "_caption");
                        design.addStyle(groupCaptionStyle);
                    }
                    JRDesignConditionalStyle condStyle = new JRDesignConditionalStyle();
                    condStyle.setParentStyle(groupCellStyle);
                    Color oldColor = condStyle.getBackcolor();
                    condStyle.setBackcolor(new Color(oldColor.getRed(), oldColor.getGreen(), 0));
                    JRDesignExpression expr =
                            ReportUtils.createExpression("new Boolean($F{" + backgroundPropertySID + "} != null)", java.lang.Boolean.class);
                    condStyle.setConditionExpression(expr);
                    groupCellStyle.addConditionalStyle(condStyle);
                }

                for(ReportDrawField reportField : drawFields) {
                    addReportFieldToLayout(reportLayout, reportField, groupCaptionStyle, groupCellStyle);
                }
                reportLayout.doLayout(pageUsableWidth);
            }

            for (ObjectView view : groupView) {
                ReportDrawField objField = new ReportDrawField(view.entity.getSID() + objectSuffix, "", charWidth);
                view.entity.baseClass.getType().fillReportDrawField(objField);
                addDesignField(design, objField);
            }

            for (ReportDrawField propertyField : drawFields) {
                addDesignField(design, propertyField);
            }
        }
    }

    private void addReportFieldToLayout(ReportLayout layout, ReportDrawField reportField, JRDesignStyle captionStyle, JRDesignStyle style) {
        String designCaptionText;
        if (reportField.hasCaptionProperty) {
            designCaptionText = ReportUtils.createFieldString(reportField.sID + headerSuffix);
        } else {
            designCaptionText = '"' + reportField.caption + '"';
        }
        JRDesignExpression captionExpr = ReportUtils.createExpression(designCaptionText, reportField.captionClass);
        JRDesignTextField captionField = ReportUtils.createTextField(captionStyle, captionExpr, toStretch);
        captionField.setHorizontalAlignment(HorizontalAlignEnum.CENTER);

        JRDesignExpression dataExpr = ReportUtils.createExpression(ReportUtils.createFieldString(reportField.sID), reportField.valueClass);
        JRDesignTextField dataField = ReportUtils.createTextField(style, dataExpr, toStretch);
        dataField.setHorizontalAlignment(HorizontalAlignEnum.getByValue(reportField.alignment));
        dataField.setPositionType(PositionTypeEnum.FLOAT);
        dataField.setBlankWhenNull(true);

        if (!toExcel || reportField.valueClass != Double.class) {
            dataField.setPattern(reportField.pattern);
        }

        layout.add(reportField, captionField, dataField);
    }

    private JRDesignGroup addDesignGroup(JasperDesign design, GroupObjectView group, String groupName) throws JRException {

        JRDesignGroup designGroup = new JRDesignGroup();
        designGroup.setName(groupName);

        String groupString = "";
        for (ObjectView object : group) {
            groupString = (groupString.length()==0?"":groupString+"+\" \"+")+"String.valueOf($F{"+object.entity.getSID()+".object})";
        }
        JRDesignExpression groupExpr = ReportUtils.createExpression(groupString, java.lang.String.class);
        designGroup.setExpression(groupExpr);

        design.addGroup(designGroup);

        return designGroup;
    }

    private JRDesignField addDesignField(JasperDesign design, ReportDrawField reportField) throws JRException {
        return addDesignField(design, reportField.sID, reportField.valueClass.getName());
    }

    private JRDesignField addDesignField(JasperDesign design, String id, String className) throws JRException {
        JRDesignField designField = ReportUtils.createField(id, className);
        design.addField(designField);
        return designField;
    }

    private static void setExpressionsToSubreportElement(JRDesignSubreport subreport, String sid) {
        JRDesignExpression subreportExpr =
                ReportUtils.createExpression(ReportUtils.createParamString(sid + ReportConstants.reportSuffix), JasperReport.class);
        subreport.setExpression(subreportExpr);

        JRDesignExpression sourceExpr =
                ReportUtils.createExpression(ReportUtils.createParamString(sid + ReportConstants.sourceSuffix), JRDataSource.class);
        subreport.setDataSourceExpression(sourceExpr);

        JRDesignExpression paramsExpr =
                ReportUtils.createExpression(ReportUtils.createParamString(sid + ReportConstants.paramsSuffix), Map.class);
        subreport.setParametersMapExpression(paramsExpr);
    }

    private static void addParametersToDesign(JasperDesign design, String sid) throws JRException {
        ReportUtils.addParameter(design, sid + ReportConstants.reportSuffix, JasperReport.class);
        ReportUtils.addParameter(design, sid + ReportConstants.sourceSuffix, JRDataSource.class);
        ReportUtils.addParameter(design, sid + ReportConstants.paramsSuffix, Map.class);
    }

    private JasperDesign createJasperDesignObject(String name, boolean isMain, boolean hasTopMargin) throws JRException {
        JasperDesign design = new JasperDesign();
        if (name.equals(GroupObjectHierarchy.rootNodeName)) {
            design.setName(formView.caption);
        } else {
            design.setName(name);
        }

        design.setPageWidth(pageWidth);
        design.setPageHeight(defaultPageHeight);

        if (!isMain) {
            design.setTopMargin(hasTopMargin ? neighboursGap : 0);
            design.setBottomMargin(0);
            design.setLeftMargin(0);
            design.setRightMargin(0);
        }

        design.setOrientation(OrientationEnum.LANDSCAPE);

        design.addStyle(DesignStyles.getDefaultStyle());
        designs.put(name, design);
        return design;
    }
}
