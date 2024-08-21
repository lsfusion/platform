grammar LsfLogics;

@header {
	package lsfusion.server.language;

    import lsfusion.base.BaseUtils;
    import lsfusion.base.Pair;
    import lsfusion.base.col.MapFact;
    import lsfusion.base.col.heavy.OrderedMap;
    import lsfusion.base.col.interfaces.immutable.ImOrderSet;
    import lsfusion.interop.action.MessageClientType;
    import lsfusion.base.col.interfaces.immutable.ImList;
    import lsfusion.interop.action.ServerResponse;
    import lsfusion.interop.form.WindowFormType;
    import lsfusion.interop.form.ContainerWindowFormType;
    import lsfusion.interop.form.ModalityWindowFormType;
    import lsfusion.interop.base.view.FlexAlignment;
    import lsfusion.interop.form.event.FormChangeEvent;
    import lsfusion.interop.form.event.FormContainerEvent;
    import lsfusion.interop.form.event.FormScheduler;
    import lsfusion.interop.form.object.table.grid.ListViewType;
    import lsfusion.interop.form.property.ClassViewType;
    import lsfusion.interop.form.property.PivotOptions;
    import lsfusion.interop.form.property.PropertyEditType;
    import lsfusion.interop.form.property.PropertyGroupType;
    import lsfusion.interop.form.print.FormPrintType;
    import lsfusion.server.base.version.Version;
    import lsfusion.server.base.AppServerImage;
    import lsfusion.server.data.expr.formula.SQLSyntaxType;
    import lsfusion.server.data.expr.query.PartitionType;
    import lsfusion.server.data.table.IndexType;
    import lsfusion.server.language.ScriptParser;
    import lsfusion.server.language.ScriptingErrorLog;
    import lsfusion.server.language.ScriptingLogicsModule;
    import lsfusion.server.language.ScriptingLogicsModule.*;
    import lsfusion.server.language.action.ActionSettings;
    import lsfusion.server.language.action.LA;
    import lsfusion.server.language.form.FormPropertyOptions;
    import lsfusion.server.language.form.ScriptingFormEntity;
    import lsfusion.server.language.form.ScriptingFormEntity.RegularFilterInfo;
    import lsfusion.server.language.form.design.Bounds;
    import lsfusion.server.language.form.design.ScriptingFormView;
    import lsfusion.server.language.form.object.ScriptingGroupObject;
    import lsfusion.server.language.navigator.window.BorderPosition;
    import lsfusion.server.language.navigator.window.DockPosition;
    import lsfusion.server.language.navigator.window.NavigatorWindowOptions;
    import lsfusion.server.language.navigator.window.Orientation;
    import lsfusion.server.language.property.LP;
    import lsfusion.server.language.property.PropertySettings;
    import lsfusion.server.language.property.oraction.ActionOrPropertySettings;
    import lsfusion.server.language.property.oraction.LAP;
    import lsfusion.server.logics.action.flow.ChangeFlowActionType;
    import lsfusion.server.logics.action.flow.Inline;
    import lsfusion.server.logics.action.flow.ListCaseAction;
    import lsfusion.server.logics.action.session.LocalNestedType;
    import lsfusion.server.logics.action.session.changed.IncrementType;
    import lsfusion.server.logics.classes.ValueClass;
    import lsfusion.server.logics.classes.data.DataClass;
    import lsfusion.server.logics.classes.user.CustomClass;
    import lsfusion.server.logics.classes.user.set.ResolveClassSet;
    import lsfusion.server.logics.event.ChangeEvent;
    import lsfusion.server.logics.event.Event;
    import lsfusion.server.logics.event.SystemEvent;
    import lsfusion.server.logics.form.interactive.FormEventType;
    import lsfusion.server.logics.form.interactive.ManageSessionType;
    import lsfusion.server.logics.form.interactive.UpdateType;
    import lsfusion.server.logics.form.interactive.action.async.QuickAccess;
    import lsfusion.server.logics.form.interactive.action.async.QuickAccessMode;
    import lsfusion.server.logics.form.interactive.action.expand.ExpandCollapseType;
    import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
    import lsfusion.server.logics.form.interactive.design.ComponentView;
    import lsfusion.server.logics.form.interactive.design.filter.FilterView;
    import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
    import lsfusion.server.logics.form.interactive.event.UserEventObject;
    import lsfusion.server.logics.form.interactive.property.GroupObjectProp;
    import lsfusion.server.logics.form.open.MappedForm;
    import lsfusion.server.logics.form.stat.SelectTop;
    import lsfusion.server.logics.form.stat.struct.FormIntegrationType;
    import lsfusion.server.logics.form.struct.FormEntity;
    import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
    import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
    import lsfusion.server.logics.form.struct.object.ObjectEntity;
    import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
    import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
    import lsfusion.server.logics.navigator.NavigatorElement;
    import lsfusion.server.logics.property.cases.CaseUnionProperty;
    import lsfusion.server.logics.property.set.Cycle;
	import lsfusion.server.base.version.ComplexLocation;
    import lsfusion.server.physics.admin.reflection.ReflectionPropertyType;
    import lsfusion.server.physics.dev.debug.BooleanDebug;
    import lsfusion.server.physics.dev.debug.DebugInfo;
    import lsfusion.server.physics.dev.debug.PropertyFollowsDebug;
    import lsfusion.server.physics.dev.i18n.LocalizedString;
    import lsfusion.server.physics.dev.integration.internal.to.InternalFormat;
    import lsfusion.server.physics.dev.integration.external.to.ExternalFormat;
    import lsfusion.interop.session.ExternalHttpMethod;
    import org.antlr.runtime.BitSet;
    import org.antlr.runtime.*;
    
    import javax.mail.Message;
    import java.awt.*;
    import java.math.BigDecimal;
    import java.util.*;
    import java.util.function.*;
    import java.time.*;

    import static java.util.Arrays.asList;
    import static lsfusion.server.language.ScriptingLogicsModule.WindowType.*;
}

@lexer::header { 
	package lsfusion.server.language; 
	import lsfusion.server.language.ScriptingLogicsModule;
	import lsfusion.server.language.ScriptParser;
	import lsfusion.server.language.ScriptedStringUtils;
}

@lexer::members {
	public ScriptingLogicsModule self;
	public ScriptParser.State parseState;

	public boolean isFirstFullParse() {
		return parseState == ScriptParser.State.META_CLASS_TABLE;
	}

	@Override
	public void emitErrorMessage(String msg) {
		if (isFirstFullParse() || parseState == ScriptParser.State.PRE) { 
			self.getErrLog().write(msg + "\n");
		}
	}

	@Override
	public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
		self.getErrLog().displayRecognitionError(this, self.getParser(), "error", tokenNames, e);
	}
	
	private boolean ahead(String text) {
		for(int i = 0; i < text.length(); i++) {
			if(input.LA(i + 1) != text.charAt(i)) {
				return false;
			}
		}
		return true;
	}	

    private boolean isRawStringSpecialChar(int ch) {
    	return ScriptedStringUtils.isRawStringSpecialChar(ch);
    }
}

@members {
	public ScriptingLogicsModule self;
	public ScriptParser.State parseState;

	private boolean insideRecursion = false;
	
	public boolean inParseState(ScriptParser.State parseState) {
		return this.parseState == parseState;
	}

	public boolean inPreParseState() {
		return inParseState(ScriptParser.State.PRE);
	}

	public boolean inMetaClassTableParseState() {
		return inParseState(ScriptParser.State.META_CLASS_TABLE);
	}

	public boolean inMainParseState() {
		return inParseState(ScriptParser.State.MAIN);
	}

	public boolean inGenMetaParseState() {
		return inParseState(ScriptParser.State.GENMETA);
	}

	public boolean inMetaParseState() {
		return inGenMetaParseState() || inParseState(ScriptParser.State.METADECL);
	}

	public boolean isFirstFullParse() {
		return inMetaClassTableParseState();
	}

	public DebugInfo.DebugPoint getCurrentDebugPoint() {
		return getCurrentDebugPoint(false);
	}

	public DebugInfo.DebugPoint getCurrentDebugPoint(boolean previous) {
		if (!$propertyStatement.isEmpty()) {
			return self.getParser().getGlobalDebugPoint(self.getName(), self.getPath(), previous, $propertyStatement::topName, $propertyStatement::topCaption);
		}
		if (!$actionStatement.isEmpty()) {
			return self.getParser().getGlobalDebugPoint(self.getName(), self.getPath(), previous, $actionStatement::topName, $actionStatement::topCaption);
		}
		if (!$overridePropertyStatement.isEmpty()) {
			return self.getParser().getGlobalDebugPoint(self.getName(), self.getPath(), previous, $overridePropertyStatement::topName, null);
		}
		if (!$overrideActionStatement.isEmpty()) {
			return self.getParser().getGlobalDebugPoint(self.getName(), self.getPath(), previous, $overrideActionStatement::topName, null);
		}
		return self.getParser().getGlobalDebugPoint(self.getName(), self.getPath(), previous);
	}

	public DebugInfo.DebugPoint getEventDebugPoint() {
		return getCurrentDebugPoint();
	}

	public void setObjectProperty(Object propertyReceiver, String propertyName, Object propertyValue, Version version, Supplier<DebugInfo.DebugPoint> debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        $designStatement::design.setObjectProperty(propertyReceiver, propertyName, propertyValue, version, debugPoint);
	}

	public List<GroupObjectEntity> getGroupObjectsList(List<String> ids, Version version) throws ScriptingErrorLog.SemanticErrorException {
		if (inMainParseState()) {
			return $formStatement::form.getGroupObjectsList(ids, version);
		}
		return null;
	}

	public TypedParameter TP(String className, String paramName) throws ScriptingErrorLog.SemanticErrorException {
		return self.new TypedParameter(className, paramName);
	}

	public TypedParameter TP(String paramName) throws ScriptingErrorLog.SemanticErrorException {
		return self.new TypedParameter((ValueClass)null, paramName);
	}

	@Override
	public void emitErrorMessage(String msg) {
		if (isFirstFullParse() || inPreParseState()) { 
			self.getErrLog().write(msg + "\n");
		}
	}

	@Override
	public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
		self.getErrLog().displayRecognitionError(this, self.getParser(), "error", tokenNames, e);
	}
}

@rulecatch {
	catch(RecognitionException re) {
		if (re instanceof ScriptingErrorLog.SemanticErrorException) {
			throw re;
		} else {
			reportError(re);
			recover(input,re);
		}
	}
}

script	
	:	moduleHeader 
		statements 
		EOF
	;

statements
	:	statement*
	;

moduleHeader
@init {
	List<String> requiredModules = new ArrayList<>();
	List<String> namespacePriority = new ArrayList<>();
	String namespaceName = null;
}
@after {
	if (inPreParseState()) {
		self.initScriptingModule($name.text, namespaceName, requiredModules, namespacePriority);
	} else if (isFirstFullParse()) {
		self.initModulesAndNamespaces(requiredModules, namespacePriority);
	}
}
	:	'MODULE' name=ID ';'
		('REQUIRE' list=nonEmptyIdList ';' { requiredModules = $list.ids; })?
		('PRIORITY' list=nonEmptyIdList ';' { namespacePriority = $list.ids; })?
		('NAMESPACE' nname=ID ';' { namespaceName = $nname.text; })?
	;


statement
	:	(	classStatement
		|	extendClassStatement
		|	groupStatement
		|	propertyStatement
		|	actionStatement
		|	overridePropertyStatement
		|	overrideActionStatement
		|	constraintStatement
		|	followsStatement
		|	writeWhenStatement
		|	eventStatement
		|	globalEventStatement
		|	aspectStatement
		|	tableStatement
		|	indexStatement
		|	formStatement
		|	designStatement
		|	windowStatement
		|	navigatorStatement
		|	metaCodeDeclarationStatement
		|	metaCodeStatement 
		|	emptyStatement
		)
	;

metaCodeParsingStatement  // metacode parsing rule
	:
		statements
	;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// CLASS STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////


classStatement 
@init {
	List<String> classParents = new ArrayList<>();
	boolean isAbstract = false;
	boolean isNative = false;
	boolean isComplex = false;
	DebugInfo.DebugPoint point = getCurrentDebugPoint(); 
}
@after {
	if (inMetaClassTableParseState()) {
	    if (!isNative)
		    self.addScriptedClass($nameCaption.name, $nameCaption.caption, $image.image, isAbstract, $classData.names, $classData.captions, $classData.images, $classData.parents, isComplex, point);
	}
}
	:	'CLASS'
		('ABSTRACT' {isAbstract = true;} | 'NATIVE' {isNative = true;})?
		('COMPLEX' { isComplex = true; })?
		nameCaption=simpleNameWithCaption
		(image=imageOption)?
		classData=classInstancesAndParents
	;

extendClassStatement
@after {
	if (inMetaClassTableParseState()) {
		self.extendClass($className.sid, $classData.names, $classData.captions, $classData.images, $classData.parents);
	}
}
	:	'EXTEND' 'CLASS' 
		className=compoundID 
		classData=classInstancesAndParents 
	;

classInstancesAndParents returns [List<String> names, List<LocalizedString> captions, List<String> images, List<String> parents]
@init {
	$parents = new ArrayList<>();
	$names = new ArrayList<>();
	$captions = new ArrayList<>();
	$images = new ArrayList<>();
}
	:	(
			'{'
				(firstInstData=simpleNameWithCaption { $names.add($firstInstData.name); $captions.add($firstInstData.caption); }
				(firstInstImg = imageOption)? {$images.add($firstInstImg.image);}
				(',' nextInstData=simpleNameWithCaption { $names.add($nextInstData.name); $captions.add($nextInstData.caption); }
				(nextInstImg = imageOption)? {$images.add($nextInstImg.image); })*)?
			'}'
			(clist=classParentsList ';' { $parents = $clist.list; })?
		|
			(clist=classParentsList { $parents = $clist.list; })? ';'
		)
	; 

classParentsList returns [List<String> list]
	:	':' parentList=nonEmptyClassIdList { $list = $parentList.ids; }
	; 

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// GROUP STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

groupStatement
@init {
	String parent = null;
	boolean isNative = false;
}
@after {
	if (inMainParseState() && !isNative) {
		self.addScriptedGroup($groupNameCaption.name, $groupNameCaption.caption, $extID.val, parent, getCurrentDebugPoint(true));
	}
}
	:	'GROUP' ('NATIVE' { isNative = true; })?
		groupNameCaption=simpleNameWithCaption
		('EXTID' extID=stringLiteral)?
		(':' parentName=compoundID { parent = $parentName.sid; })?
		';'
	;


////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////// FORM STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

formStatement
scope {
	ScriptingFormEntity form;
}
@init {
	boolean initialDeclaration = false;
	DebugInfo.DebugPoint point = getCurrentDebugPoint();
}
@after {
	if (inMainParseState() && initialDeclaration) {
		self.finalizeScriptedForm($formStatement::form);
	}
}
	:	(	declaration=formDeclaration { $formStatement::form = $declaration.form; initialDeclaration = true; }
		|	extDecl=extendingFormDeclaration { $formStatement::form = $extDecl.form; }
		)
		(	formGroupObjectsList
		|	formTreeGroupObjectList
		|	formFiltersList
		|	formPropertiesList
		|	formHintsList
		|	formEventsList
		|	filterGroupDeclaration
		|	extendFilterGroupDeclaration
		|	userFiltersDeclaration
		|	formOrderByList
	    |   formPivotOptionsDeclaration
		|	dialogFormDeclaration
		|	editFormDeclaration
		|	reportFilesDeclaration
		|	reportDeclaration
		|   formExtIDDeclaration
		)*
		';'
	;

dialogFormDeclaration
	:	'LIST' cid=classId 'OBJECT' oid=ID
		{
			if (inMainParseState()) {
				$formStatement::form.setAsDialogForm($cid.sid, $oid.text, self.getVersion());
			}
		}
	;

editFormDeclaration
	:	'EDIT' cid=classId 'OBJECT' oid=ID
		{
			if (inMainParseState()) {
				$formStatement::form.setAsEditForm($cid.sid, $oid.text, self.getVersion());
			}
		}
	;
	
reportFilesDeclaration
	:	('REPORTS' | 'REPORTFILES') reportPath (',' reportPath)*
	;
	
reportPath
@init {
	GroupObjectEntity groupObject = null;
	PropertyObjectEntity property = null;
}
@after {
	if (inMainParseState()) {
		$formStatement::form.setReportPath(groupObject, property);	
	}
}
	:	(	'TOP' 
		| 	go = formGroupObjectEntity { groupObject = $go.groupObject; }
		) 
		prop = formPropertyObject { property = $prop.property; }
	;

reportDeclaration
@init {
	PropertyObjectEntity property = null;
}
@after {
	if (inMainParseState()) {
		$formStatement::form.setReportPath(property);
	}
}
	:	'REPORT' prop = formPropertyObject { property = $prop.property; }
	;

formExtIDDeclaration
@init {
	String formExtID = null;
}
@after {
	if (inMainParseState()) {
		$formStatement::form.setIntegrationSID(formExtID);
	}
}
	:	'FORMEXTID' id=stringLiteral { formExtID = $id.val; }
	;

formDeclaration returns [ScriptingFormEntity form]
@init {
	String image = null;
	String title = null;
	boolean localAsync = false;
	DebugInfo.DebugPoint point = getCurrentDebugPoint();
}
@after {
	if (inMainParseState()) {
		$form = self.createScriptedForm($formNameCaption.name, $formNameCaption.caption, point, $img.image, localAsync);
	}
}
	:	'FORM' 
		formNameCaption=simpleNameWithCaption
		(	img=imageOption
		|	('LOCALASYNC' { localAsync = true; })
		)*
	;


extendingFormDeclaration returns [ScriptingFormEntity form]
@after {
	if (inMainParseState()) {
		$form = self.getFormForExtending($formName.sid);
	}
}
	:	'EXTEND' 'FORM' formName=compoundID
	;

formGroupObjectsList
@init {
	List<ScriptingGroupObject> groups = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
		$formStatement::form.addScriptingGroupObjects(groups, self.getVersion(), getCurrentDebugPoint(true));
	}
}
	:	'OBJECTS'
		groupElement=formGroupObjectDeclaration { groups.add($groupElement.groupObject); }
		(',' groupElement=formGroupObjectDeclaration { groups.add($groupElement.groupObject); })*
	;

formTreeGroupObjectList
@init {
	String treeSID = null;
	List<ScriptingGroupObject> groups = new ArrayList<>();
	List<List<LP>> properties = new ArrayList<>();
	List<List<ImOrderSet<String>>> propertyMappings = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
		$formStatement::form.addScriptingTreeGroupObject(treeSID, $opts.location, groups, properties, propertyMappings, self.getVersion(), getCurrentDebugPoint(true));
	}
}
	:	'TREE'
		(id = ID { treeSID = $id.text; })?
		groupElement=formTreeGroupObject { groups.add($groupElement.groupObject); properties.add($groupElement.properties); propertyMappings.add($groupElement.propertyMappings); }
		(',' groupElement=formTreeGroupObject { groups.add($groupElement.groupObject); properties.add($groupElement.properties); propertyMappings.add($groupElement.propertyMappings); })*
	    opts = formTreeGroupObjectOptions
	;

formGroupObjectDeclaration returns [ScriptingGroupObject groupObject]
	:	object=formGroupObject { $groupObject = $object.groupObject; }
	    formGroupObjectOptions[$groupObject]
	;

formGroupObjectOptions[ScriptingGroupObject groupObject]
	:	(	viewType=formGroupObjectViewType { $groupObject.setViewType($viewType.type, $viewType.listType); $groupObject.setPivotOptions($viewType.options);
	                                           $groupObject.setCustomTypeRenderFunction($viewType.customRenderFunction); $groupObject.setCustomOptions($viewType.customOptions);
	                                           $groupObject.setMapTileProvider($viewType.mapTileProvider);}
		|	pageSize=formGroupObjectPageSize { $groupObject.setPageSize($pageSize.value); }
		|	update=formGroupObjectUpdate { $groupObject.setUpdateType($update.updateType); }
		|	relative=formGroupObjectRelativePosition { $groupObject.setLocation($relative.location); }
		|	group=formGroupObjectGroup { $groupObject.setPropertyGroupName($group.formObjectGroup); }
		|   extID=formExtID { $groupObject.setIntegrationSID($extID.extID); }
		|   formExtKey { $groupObject.setIntegrationKey(true); }
		|   formSubReport { $groupObject.setSubReport($formSubReport.pathProperty);  }
		|   background=formGroupObjectBackground { $groupObject.setBackground($background.background); }
		|   foreground=formGroupObjectForeground { $groupObject.setForeground($foreground.foreground); }
		)*
	;

formTreeGroupObjectOptions returns [ComplexLocation<GroupObjectEntity> location]
	:	(	relative=formGroupObjectRelativePosition { $location = $relative.location; }
		)*
	;

formGroupObject returns [ScriptingGroupObject groupObject]
	:	sdecl=formSingleGroupObjectDeclaration
		{
			$groupObject = new ScriptingGroupObject(null, asList($sdecl.name), asList($sdecl.className), asList($sdecl.caption), asList($sdecl.event), asList($sdecl.extID));
		}
	|	mdecl=formMultiGroupObjectDeclaration
		{
			$groupObject = new ScriptingGroupObject($mdecl.groupName, $mdecl.objectNames, $mdecl.classNames, $mdecl.captions, $mdecl.events, $mdecl.extIDs);
		}
	;

formTreeGroupObject returns [ScriptingGroupObject groupObject, List<LP> properties, List<ImOrderSet<String>> propertyMappings]
	:	( sdecl=formSingleGroupObjectDeclaration
		{
			$groupObject = new ScriptingGroupObject(null, asList($sdecl.name), asList($sdecl.className), asList($sdecl.caption), asList($sdecl.event), asList($sdecl.extID));
		}

		('PARENT' decl=formExprDeclaration { $properties = asList($decl.property); $propertyMappings = asList($decl.mapping); })? )

	|	mdecl=formMultiGroupObjectDeclaration
		{
			$groupObject = new ScriptingGroupObject($mdecl.groupName, $mdecl.objectNames, $mdecl.classNames, $mdecl.captions, $mdecl.events, $mdecl.extIDs);
		}

        ( '('
		'PARENT' {$properties = new ArrayList<>(); $propertyMappings = new ArrayList<>();} first=formExprDeclaration { $properties.add($first.property); $propertyMappings.add($first.mapping); }
        		(',' next=formExprDeclaration { $properties.add($next.property); $propertyMappings.add($next.mapping); })*
        ')' )?

	;

formGroupObjectViewType returns [ClassViewType type, ListViewType listType, PivotOptions options, String customRenderFunction, FormLPUsage customOptions, String mapTileProvider]
	:	viewType=groupObjectClassViewType { $type = $viewType.type; $listType = $viewType.listType; $options = $viewType.options;
	                                        $customRenderFunction = $viewType.customRenderFunction; $customOptions = $viewType.customOptions;
	                                        $mapTileProvider = $viewType.mapTileProvider;}
	;

groupObjectClassViewType returns [ClassViewType type, ListViewType listType, PivotOptions options, String customRenderFunction, FormLPUsage customOptions, String mapTileProvider]
	:   'PANEL' {$type = ClassViewType.PANEL;}
	|   'TOOLBAR' {$type = ClassViewType.TOOLBAR;}
	|   'POPUP' {$type = ClassViewType.POPUP;}
	|   'GRID' {$type = ClassViewType.LIST;}
    |	lType=listViewType { $listType = $lType.type; $options = $lType.options;
                                          $customRenderFunction = $lType.customRenderFunction; $customOptions = $lType.customOptions;
                                          $mapTileProvider = $lType.mapTileProvider;}
	;

propertyClassViewType returns [ClassViewType type]
	:   'PANEL' {$type = ClassViewType.PANEL;}
	|   'GRID' {$type = ClassViewType.LIST;}
	|   'TOOLBAR' {$type = ClassViewType.TOOLBAR;}
	|   'POPUP' {$type = ClassViewType.POPUP;}
	;

propertyCustomView returns [String customRenderFunction, String customEditorFunction, String selectFunction]
	:	('CUSTOM'
	            ((renderFun=stringLiteral { $customRenderFunction = $renderFun.val;})
	        |   ((renderFun=stringLiteral { $customRenderFunction = $renderFun.val;})?
	            pedt = propertyEditCustomView { $customEditorFunction = $pedt.customEditorFunction; })))
	    |
	    'SELECT' { $customRenderFunction = PropertyDrawEntity.SELECT + PropertyDrawEntity.AUTOSELECT; } ({ input.LA(1)!=ID }? ('AUTO' | renderFun=stringLiteral { $customRenderFunction = PropertyDrawEntity.SELECT + $renderFun.val;}))?
	    |
	    'NOSELECT' { $customRenderFunction = PropertyDrawEntity.NOSELECT; }
	;

propertyEditCustomView returns [String customEditorFunction]
    :
        // EDIT TEXT is a temporary fix for backward compatibility
        ('CHANGE' | ('EDIT' primitiveType)) { $customEditorFunction = "DEFAULT"; } (editFun=stringLiteral {$customEditorFunction = $editFun.val; })? // "DEFAULT" is hardcoded and used in GFormController.edit
    ;

listViewType returns [ListViewType type, PivotOptions options, String customRenderFunction, FormLPUsage customOptions, String mapTileProvider]
	:   'PIVOT' {$type = ListViewType.PIVOT;} opt = pivotOptions {$options = $opt.options; }
	|   'MAP' (tileProvider = stringLiteral)? {$type = ListViewType.MAP; $mapTileProvider = $tileProvider.val;}
	|   'CUSTOM' function=stringLiteral {$type = ListViewType.CUSTOM; $customRenderFunction = $function.val;} ('HEADER' decl=customOptionsGroupObjectContext { $customOptions = $decl.customOptions; })?
	|   'CALENDAR' {$type = ListViewType.CALENDAR;}
    ;

customOptionsGroupObjectContext returns [FormLPUsage customOptions]
	:	propObj=formLPUsage{ $customOptions = $propObj.propUsage; }
	;

propertyGroupType returns [PropertyGroupType type]
	: 	('SUM' {$type = PropertyGroupType.SUM;} | 'MAX' {$type = PropertyGroupType.MAX;} | 'MIN' {$type = PropertyGroupType.MIN;})
	;

propertyLastAggr returns [List<PropertyObjectEntity> properties = new ArrayList<>(), Boolean desc = false]
	: 	'LAST'
	    ('DESC' { $desc = true; } )?
	    '('
        prop=formPropertyObject { $properties.add($prop.property); }
        (',' prop=formPropertyObject { $properties.add($prop.property); })*
        ')'
	;

// temporary it's definitely shouldn't be an option
propertyFormula returns [String formula, List<PropertyDrawEntity> operands]
@init {
    $operands = new ArrayList<>();
}
	: 	'FORMULA' f=stringLiteral { $formula = $f.val; }
	    '('
	        (pd=formPropertyDraw { $operands.add($pd.property); }
	        (',' pd=formPropertyDraw { $operands.add($pd.property); })*)?
	    ')'
	;

formGroupObjectPageSize returns [Integer value = null]
	:	'PAGESIZE' size=intLiteral { $value = $size.val; }
	;

staticRelativePosition returns [ComplexLocation location]
	:	'FIRST' { $location = ComplexLocation.FIRST(); }
    |	'LAST' { $location = ComplexLocation.LAST(); }
    |	'DEFAULT' { $location = ComplexLocation.DEFAULT(); }
	;

formGroupObjectRelativePosition returns [ComplexLocation<GroupObjectEntity> location]
	:	'AFTER' go=formGroupObjectEntity { $location = ComplexLocation.AFTER($go.groupObject); }
	|	'BEFORE' go=formGroupObjectEntity { $location = ComplexLocation.BEFORE($go.groupObject); }
	|	st=staticRelativePosition { $location = $st.location; }
	;

formPropertyDrawRelativePosition returns [ComplexLocation<PropertyDrawEntity> location, String propText]
	:	'AFTER' pd=formPropertyDraw { $location = ComplexLocation.AFTER($pd.property); $propText = $pd.text; }
	|	'BEFORE' pd=formPropertyDraw { $location = ComplexLocation.BEFORE($pd.property); $propText = $pd.text; }
	|	st=staticRelativePosition { $location = $st.location; }
	;

componentRelativePosition returns [ComplexLocation<ComponentView> location]
	:	'AFTER' cm=componentSelector { $location = ComplexLocation.AFTER($cm.component); }
	|	'BEFORE' cm=componentSelector { $location = ComplexLocation.BEFORE($cm.component); }
    |	st=staticRelativePosition { $location = $st.location; }
	;

navigatorElementRelativePosition returns [ComplexLocation<NavigatorElement> location]
	:	'AFTER' ne=navigatorElementSelector { $location = ComplexLocation.AFTER($ne.element); }
	|	'BEFORE' ne=navigatorElementSelector { $location = ComplexLocation.BEFORE($ne.element); }
	|	st=staticRelativePosition { $location = $st.location; }
	;

formGroupObjectBackground returns [FormLPUsage background]
    :	'BACKGROUND' propObj=formLPUsage { background = $propObj.propUsage; }
    ;

formGroupObjectForeground returns [FormLPUsage foreground]
    :	'FOREGROUND' propObj=formLPUsage { foreground = $propObj.propUsage; }
    ;

formGroupObjectUpdate returns [UpdateType updateType]
@init {
}
	:	'FIRST' { $updateType = UpdateType.FIRST; }
	|	'LAST' { $updateType = UpdateType.LAST; }
	|   'PREV' { $updateType = UpdateType.PREV; }
	|   'NULL' { $updateType = UpdateType.NULL; }
	;

formGroupObjectGroup returns [String formObjectGroup]
	:	'IN' groupName=compoundID { $formObjectGroup = $groupName.sid; }
	;

formExtID returns [String extID]
	:	'EXTID' id=stringLiteral { $extID = $id.val; }
	;

formExtKey
	:	'EXTKEY'
	;

formSubReport returns [PropertyObjectEntity pathProperty]
	:	'SUBREPORT' (prop=formPropertyObject { pathProperty = $prop.property; })?
	;

formSingleGroupObjectDeclaration returns [String name, String className, LocalizedString caption, ActionObjectEntity event, String extID]
	:	foDecl=formObjectDeclaration { $name = $foDecl.name; $className = $foDecl.className; $caption = $foDecl.caption; $event = $foDecl.event; $extID = $foDecl.extID; }
	;

formMultiGroupObjectDeclaration returns [String groupName, List<String> objectNames, List<String> classNames, List<LocalizedString> captions, List<ActionObjectEntity> events, List<String> extIDs]
@init {
	$objectNames = new ArrayList<>();
	$classNames = new ArrayList<>();
	$captions = new ArrayList<>();
	$events = new ArrayList<>();
	$extIDs = new ArrayList<>();
}
	:	(gname=ID { $groupName = $gname.text; } EQ)?
		'('
			objDecl=formObjectDeclaration { $objectNames.add($objDecl.name); $classNames.add($objDecl.className); $captions.add($objDecl.caption); $events.add($objDecl.event); $extIDs.add($objDecl.extID); }
			(',' objDecl=formObjectDeclaration { $objectNames.add($objDecl.name); $classNames.add($objDecl.className); $captions.add($objDecl.caption); $events.add($objDecl.event); $extIDs.add($objDecl.extID); })*
		')'
	;


formObjectDeclaration returns [String name, String className, LocalizedString caption, ActionObjectEntity event, String extID]
	:	((objectName=ID { $name = $objectName.text; })? (c=localizedStringLiteral { $caption = $c.val; })? EQ)?
		id=classId {
		    $className = $id.sid;
		    if (inMainParseState()) {
                $formStatement::form.addDeclaredTypedParameter(self.new TypedParameter($className, BaseUtils.nvl($name, $className)));
            }
		}
		(
		    'ON' 'CHANGE' faprop=formActionObject { $event = $faprop.action; }
		|   'EXTID' eid=stringLiteral { $extID = $eid.val; }
		)*
	; 
	
formPropertiesList
@init {
	List<? extends AbstractFormActionOrPropertyUsage> properties = new ArrayList<>();
	List<String> aliases = new ArrayList<>();
	List<LocalizedString> captions = new ArrayList<>();	
	List<DebugInfo.DebugPoint> points = new ArrayList<>();
	FormPropertyOptions commonOptions = null;
	List<FormPropertyOptions> options = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
		$formStatement::form.addScriptedPropertyDraws(properties, aliases, captions, commonOptions, options, self.getVersion(), points);
	}
}
	:	'PROPERTIES' '(' objects=idList ')' opts=formPropertyOptionsList list=formPropertyUList[$objects.ids]
		{
			commonOptions = $opts.options;
			properties = $list.properties;
			aliases = $list.aliases;
			captions = $list.captions;
			options = $list.options;
			points = $list.points;
		}
	|	'PROPERTIES' opts=formPropertyOptionsList mappedList=formMappedPropertiesList
		{
			commonOptions = $opts.options;
			properties = $mappedList.properties;
			aliases = $mappedList.aliases;
			captions = $mappedList.captions;
			options = $mappedList.options;
			points = $mappedList.points;
		}
	;	

// потенциально две проблемы с убиранием =pE -> (a=)?pe | pe решается простым lookahead, два pe подряд SHOWIF pe pe, факторится с ? так чтобы formPropertyOptionsList заканчивался на pe а дальше formMappedProperty | pe после чего formMappedProperty lookahead'ся 
formPropertyOptionsList returns [FormPropertyOptions options]
@init {
	$options = new FormPropertyOptions();
}
	:	(	editType = propertyEditTypeLiteral { $options.setEditType($editType.val); }
	    |   'SELECTOR' { $options.setSelector(true); }
		|	'HINTNOUPDATE' { $options.setHintNoUpdate(true); }
		|	'HINTTABLE' { $options.setHintTable(true); }
        |   fs = formSessionScopeClause { $options.setFormSessionScope($fs.result); }
		|	'OPTIMISTICASYNC' { $options.setOptimisticAsync(true); }
		|	'COLUMNS' (columnsName=stringLiteral)? '(' ids=nonEmptyIdList ')' { $options.setColumns($columnsName.text, getGroupObjectsList($ids.ids, self.getVersion())); }
		|	'SHOWIF' propObj=formPropertyObject { $options.setShowIf($propObj.property); }
		|	'DISABLEIF' propObj=formPropertyObject { $options.setDisableIf($propObj.property); }
		|	'READONLYIF' propObj=formPropertyObject { $options.setReadOnlyIf($propObj.property); }
		|	'CLASS' propObj=formPropertyObject { $options.setValueElementClass($propObj.property); }
		|	'BACKGROUND' propObj=formPropertyObject { $options.setBackground($propObj.property); }
		|	'FOREGROUND' propObj=formPropertyObject { $options.setForeground($propObj.property); }
		|	('IMAGE' ('AUTO' | propObj=formPropertyObject)? { $options.setImage($propObj.literal, $propObj.property); } | 'NOIMAGE' { $options.setImage(AppServerImage.NULL, null); } )
		|	'HEADER' propObj=formPropertyObject { $options.setHeader($propObj.property); }
		|	'FOOTER' propObj=formPropertyObject { $options.setFooter($propObj.property); }
		|	viewType=propertyClassViewType { $options.setViewType($viewType.type); }
		|	customView=propertyCustomView { $options.setCustomRenderFunction($customView.customRenderFunction); $options.setCustomEditorFunction($customView.customEditorFunction); }
		|	'PIVOT' pgt=propertyGroupType { $options.setAggrFunc($pgt.type); }
		|	'PIVOT' pla=propertyLastAggr { $options.setLastAggr($pla.properties, $pla.desc); }
		|	'PIVOT' pf=propertyFormula { $options.setFormula($pf.formula, $pf.operands); }
		|	'DRAW' toDraw=formGroupObjectEntity { $options.setToDraw($toDraw.groupObject); }
		|   pl=formPropertyDrawRelativePosition { $options.setLocation($pl.location, $pl.propText); }
		|	'QUICKFILTER' pdraw=formPropertyDraw { $options.setQuickFilterPropertyDraw($pdraw.property); }
		|	'ON' et=formEventType prop=formActionObject { $options.addEventAction($et.type, $et.before, $prop.action); }
		|	'ON' 'CONTEXTMENU' (c=localizedStringLiteralNoID)? prop=formActionObject { $options.addContextMenuAction($c.val, $prop.action); }
		|	'ON' 'KEYPRESS' key=stringLiteral prop=formActionObject { $options.addKeyPressAction($key.val, $prop.action); }
		|	'EVENTID' id=stringLiteral { $options.setEventId($id.val); }
		|	'ATTR' { $options.setAttr(true); }
		|   'IN' groupName=compoundID { $options.setGroupName($groupName.sid); }
		|   ('EXTID' id=stringLiteral { $options.setIntegrationSID($id.val); } | 'NOEXTID' { $options.setIntegrationSID("NOEXTID"); })
		|   'EXTNULL' { $options.setExtNull(true); }
		|   po=propertyDrawOrder { $options.setOrder($po.order); }
		|   'FILTER' { $options.setFilter(true); }
		|   'COLUMN' { $options.setPivotColumn(true); }
		|   'ROW' { $options.setPivotRow(true); }
		|   'MEASURE' { $options.setPivotMeasure(true); }
		|   st = stickyOption { $options.setSticky($st.sticky); }
		|   sync = syncTypeLiteral { $options.setSync($sync.val); }
		)*
	;

formPropertyDraw returns [PropertyDrawEntity property]
	:	id=ID              	{ if (inMainParseState()) $property = $formStatement::form.getPropertyDraw($id.text, self.getVersion()); }
	|	prop=mappedPropertyDraw { if (inMainParseState()) $property = $formStatement::form.getPropertyDraw($prop.name, $prop.mapping, self.getVersion()); }
	;

formMappedPropertiesList returns [List<String> aliases, List<LocalizedString> captions, List<AbstractFormActionOrPropertyUsage> properties, List<FormPropertyOptions> options, List<DebugInfo.DebugPoint> points]
@init {
	$aliases = new ArrayList<>();
	$captions = new ArrayList<>();
	$properties = new ArrayList<>();
	$options = new ArrayList<>();
	$points = new ArrayList<>(); 
	String alias = null;
	LocalizedString caption = null;
    AbstractFormActionOrPropertyUsage lpUsage = null;
}
	:	{ alias = null; caption = null; $points.add(getCurrentDebugPoint()); }
		(		
			mappedProp=formMappedProperty
			{
        		$properties.add($mappedProp.propUsage);
			} 
		| 	
		    (
		    	(id=simpleNameOrWithCaption { alias = $id.name; caption = $id.caption; })?
				EQ
				(   mappedProp = formMappedPredefinedOrAction // formMappedProperty without simple f(a,b) - formExprDeclaration will proceed this (otherwise x=f(a,b)*g(a,b) will be parsed as x=f(a,b))
                    { lpUsage = $mappedProp.propUsage; }                        
				|   expr=formExprOrTrivialLADeclaration
				    {
                        if(inMainParseState()) {
                            if($expr.fu != null)
                                lpUsage = $expr.fu;
                            else // we don't need checkPropertyIsNew because it is checked in makeActionOrPropertyPublic(FormLAPUsage)
                                lpUsage = new FormLPUsage($expr.property, $expr.mapping, $expr.signature);
                        }
				    }
				|	action=formActionDeclaration
				    {
                        if(inMainParseState()) {
                            lpUsage = new FormLAUsage($action.action, $action.mapping, $action.signature);
                        }
				    })
				)
				{
					$properties.add(lpUsage);
				} 
			)
		opts=formPropertyOptionsList
		{
			$aliases.add(alias);
			$captions.add(caption);
			$options.add($opts.options);
		}
		(','
            { alias = null; caption = null; $points.add(getCurrentDebugPoint()); }
            (		
                mappedProp=formMappedProperty
                {
                    $properties.add($mappedProp.propUsage);
                } 
            | 	
                (
                    (id=simpleNameOrWithCaption { alias = $id.name; caption = $id.caption; })?
                    EQ
                    (   mappedProp = formMappedPredefinedOrAction // formMappedProperty without simple f(a,b) - formExprDeclaration will proceed this (otherwise x=f(a,b)*g(a,b) will be parsed as x=f(a,b))
                        { lpUsage = $mappedProp.propUsage; }                        
                    |   expr=formExprOrTrivialLADeclaration
                        {
                            if(inMainParseState()) {
                                if($expr.fu != null)
                                    lpUsage = $expr.fu;
                                else
                                    lpUsage = new FormLPUsage($expr.property, $expr.mapping, $expr.signature);
                            }
                        }
                    |	action=formActionDeclaration
                        {
                            if(inMainParseState()) {
                                lpUsage = new FormLAUsage($action.action, $action.mapping, $action.signature);
                            }
                        })
                    )
                    {
                        $properties.add(lpUsage);
                    } 
                )
            opts=formPropertyOptionsList
            {
                $aliases.add(alias);
                $captions.add(caption);
                $options.add($opts.options);
            }
		)*
	;

formPropertyObject returns [PropertyObjectEntity property = null, Object literal]
	:   fd = designOrFormPropertyObject[null] { $property = $fd.property; $literal = $fd.literal; }
	;

designPropertyObject returns [PropertyObjectEntity property = null, Object literal]
	:   fd = designOrFormPropertyObject[$designStatement::design] { $property = $fd.property; $literal = $fd.literal; }
	;

// may be used in design
designOrFormPropertyObject[ScriptingFormView design] returns [PropertyObjectEntity property = null, Object literal]
@init {
    AbstractFormPropertyUsage propUsage = null;
}
	:	expr=designOrFormExprDeclaration[design] { propUsage = new FormLPUsage($expr.property, $expr.mapping); $literal = $expr.literal; }
		{
			if (inMainParseState()) {
			    if(design != null)
			        $property = design.addPropertyObject(propUsage);
                else
				    $property = $formStatement::form.addPropertyObject(propUsage);
			}
		}
	;

formLPUsage returns [FormLPUsage propUsage]
	:	expr=designOrFormExprDeclaration[null] { propUsage = new FormLPUsage($expr.property, $expr.mapping); }
	;

formActionObject returns [ActionObjectEntity action = null]
@init {
    AbstractFormActionUsage propUsage = null;
    ImOrderSet<String> mapping = null;
}
	:	(	mProperty=mappedPropertyObjectUsage { propUsage = new FormActionUsage($mProperty.propUsage, $mProperty.mapping); }
		|	mAction=formActionDeclaration { propUsage = new FormLAUsage($mAction.action, $mAction.mapping); }
		)
		{
			if (inMainParseState()) {
				$action = $formStatement::form.addActionObject(propUsage);
			}
		}
	;

formGroupObjectEntity returns [GroupObjectEntity groupObject]
	:	id = ID { 
			if (inMainParseState()) {
				$groupObject = $formStatement::form.getGroupObjectEntity($ID.text, self.getVersion());
			} 
		}
	;

formMappedProperty returns [BaseFormActionOrPropertyUsage propUsage]
@after {
    $propUsage.setMapping($objects.ids); // // need this because mapping is parsed after usage
}
	:	pu=formPropertyUsage[null] { $propUsage = $pu.propUsage; }
		'('
			objects=idList
		')'
	;

formMappedPredefinedOrAction returns [BaseFormActionOrPropertyUsage propUsage] // actually FormPredefinedUsage or FormActionUsage 
@after {
    $propUsage.setMapping($objects.ids); // // need this because mapping is parsed after usage
}
	:	pu=formPredefinedOrActionUsage[null] { $propUsage = $pu.propUsage; }
		'('
			objects=idList
		')'
	;

mappedPropertyObjectUsage returns [NamedPropertyUsage propUsage, List<String> mapping]
	:	pu=propertyUsage { $propUsage = $pu.propUsage; }
		'('
			objects=idList { $mapping = $objects.ids; }
		')'
	;

formPropertySelector[FormEntity form] returns [PropertyDrawEntity propertyDraw = null]
	:	pname=ID
		{
			if (inMainParseState()) {
				$propertyDraw = form == null ? null : ScriptingFormEntity.getPropertyDraw(self, form, $pname.text, self.getVersion());
			}
		}
	|	mappedProp=mappedPropertyDraw	
		{
			if (inMainParseState()) {
				$propertyDraw = ScriptingFormEntity.getPropertyDraw(self, form, $mappedProp.name, $mappedProp.mapping, self.getVersion());
			}
		}
	;

mappedPropertyDraw returns [String name, List<String> mapping]
	:	pDrawName=ID { $name = $pDrawName.text; }
		'('
		list=idList { $mapping = $list.ids; }
		')'
	;

formPropertyUList[List<String> mapping] returns [List<String> aliases, List<LocalizedString> captions, List<BaseFormActionOrPropertyUsage> properties, List<FormPropertyOptions> options, List<DebugInfo.DebugPoint> points]
@init {
	$aliases = new ArrayList<>();
	$captions = new ArrayList<>();
	$properties = new ArrayList<>();
	$options = new ArrayList<>();
	$points = new ArrayList<>();
	String alias = null;
	LocalizedString caption = null;
}
	:	{ alias = null; caption = null; $points.add(getCurrentDebugPoint()); }
		(id=simpleNameOrWithCaption EQ { alias = $id.name; caption = $id.caption; })?
		pu=formPropertyUsage[mapping] opts=formPropertyOptionsList
		{
			$aliases.add(alias);
			$captions.add(caption);
			$properties.add($pu.propUsage);
			$options.add($opts.options);
		}
		(','
			{ alias = null; caption = null; $points.add(getCurrentDebugPoint()); }
			(id=simpleNameOrWithCaption EQ { alias = $id.name; caption = $id.caption; })?
			pu=formPropertyUsage[mapping] opts=formPropertyOptionsList
			{
				$aliases.add(alias);
				$captions.add(caption);
				$properties.add($pu.propUsage);
				$options.add($opts.options);
			}
		)*
	;

formPropertyUsage[List<String> mapping] returns [BaseFormActionOrPropertyUsage propUsage]
@init {
   String systemName = null;
   List<String> signature = null;
}
	:	fpp = actionOrPropertyUsage { $propUsage = $fpp.propUsage.createFormUsage(mapping); } 
	    |	
	    fpd = formPredefinedUsage[mapping] { $propUsage = $fpd.propUsage; }
   ;
   
formPredefinedUsage[List<String> mapping] returns [FormPredefinedUsage propUsage]
@init {
   String systemName = null;
   List<String> signature = null;
}
@after {
	$propUsage = new FormPredefinedUsage(new NamedPropertyUsage(systemName, signature), mapping);
}
    :
        (
				(	cid='NEW'		{ systemName = $cid.text; }
				|	cid='NEWEDIT'	{ systemName = $cid.text; }
				|	cid='EDIT'		{ systemName = $cid.text; }
				)
				( '[' clId=compoundID ']'  { signature = Collections.singletonList($clId.sid); } )?
			)
		|	cid='VALUE'		{ systemName = $cid.text; }
		|	cid='INTERVAL'	{ systemName = $cid.text; }
		|	cid='DELETE'	{ systemName = $cid.text; }
;

formPredefinedOrActionUsage[List<String> mapping] returns [BaseFormActionOrPropertyUsage propUsage] // actually FormPredefinedUsage or FormActionUsage
	:	('ACTION' pu = propertyUsage { $propUsage = new ActionUsage($pu.propUsage).createFormUsage(mapping); }) 
	    |	
	    fpd = formPredefinedUsage[mapping] { $propUsage = $fpd.propUsage; }

;

nonEmptyActionOrPropertyUsageList returns [List<ActionOrPropertyUsage> propUsages]
@init {
	$propUsages = new ArrayList<>();
}
	:	first=actionOrPropertyUsage { $propUsages.add($first.propUsage); }
		(',' next=actionOrPropertyUsage { $propUsages.add($next.propUsage); })*
	;

actionOrPropertyUsage returns [ActionOrPropertyUsage propUsage]
@init {
   boolean action = false;
}
    :
        ('ACTION' { action = true; } )?
        pu=propertyUsage { $propUsage = action ? new ActionUsage($pu.propUsage) : new PropertyElseActionUsage($pu.propUsage); }    
    ;

formFiltersList
@init {
	List<LP> properties = new ArrayList<>();
	List<ImOrderSet<String>> propertyMappings = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
		$formStatement::form.addScriptedFilters(properties, propertyMappings, self.getVersion());
	}
}
	:	'FILTERS'
		decl=formExprDeclaration { properties.add($decl.property); propertyMappings.add($decl.mapping);}
	    (',' decl=formExprDeclaration { properties.add($decl.property); propertyMappings.add($decl.mapping);})*
	;

formHintsList
@init {
	boolean hintNoUpdate = true;
}
@after {
	if (inMainParseState()) {
		$formStatement::form.addScriptedHints(hintNoUpdate, $list.propUsages, self.getVersion());
	}
}
	:	(('HINTNOUPDATE') | ('HINTTABLE' { hintNoUpdate = false; })) 'LIST'
		list=nonEmptyPropertyUsageList
	;

formEventsList
@init {
	List<ActionObjectEntity> actions = new ArrayList<>();
	List<Object> types = new ArrayList<>();
	List<Boolean> replaces = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
		$formStatement::form.addScriptedFormEvents(actions, types, replaces, self.getVersion());
	}
}
	:	('EVENTS')?
		decl=formEventDeclaration { actions.add($decl.action); types.add($decl.type); replaces.add($decl.replace); }
		(',' decl=formEventDeclaration { actions.add($decl.action); types.add($decl.type); replaces.add($decl.replace); })*
	;


formEventDeclaration returns [ActionObjectEntity action, Object type, Boolean replace = null]
@init {
    Boolean before = null;
}
	:	'ON'
		(	'OK' ('BEFORE' { before = true; } | 'AFTER' { before = false; })? { $type = before == null ? FormEventType.OK : (before ? FormEventType.BEFOREOK : FormEventType.AFTEROK); }
		|	'APPLY' ('BEFORE' { before = true; } | 'AFTER' { before = false; })? { $type = before == null ? FormEventType.APPLY : (before ? FormEventType.BEFOREAPPLY : FormEventType.AFTERAPPLY); }
		|	'CLOSE'	 { $type = FormEventType.CLOSE; }
		|	'INIT'	 { $type = FormEventType.INIT; }
		|	'CANCEL' { $type = FormEventType.CANCEL; }
		|	'DROP'	 { $type = FormEventType.DROP; }
		|	'QUERYOK'	 { $type = FormEventType.QUERYOK; }
		|	'QUERYCLOSE'	 { $type = FormEventType.QUERYCLOSE; }
		| 	changeEvent = changeEventDeclaration { $type = $changeEvent.type; }
		| 	containerEvent=formContainerEventDeclaration { $type = new FormContainerEvent($containerEvent.sid, $containerEvent.collapse); }
		| 	schedule = scheduleFormEventDeclaration { $type = new FormScheduler($schedule.period, $schedule.fixed); }
		)
		('REPLACE' { $replace = true; } | 'NOREPLACE' { $replace = false; } )?
		faprop=formActionObject { $action = $faprop.action; }
	;

changeEventDeclaration returns [Object type]
@init {
    Boolean before = null;
}
    :
    'CHANGE' objectId=ID { $type = $objectId.text; }
    |
    'CHANGE'? (
        ('OBJECT' objectId=ID { $type = $objectId.text; }
        |  'FILTER' objectId=ID { $type = new UserEventObject($objectId.text, UserEventObject.Type.FILTER, false); }
        |  'ORDER' objectId=ID { $type = new UserEventObject($objectId.text, UserEventObject.Type.ORDER, false); }
        |  'FILTERS' objectId=ID { $type = new UserEventObject($objectId.text, UserEventObject.Type.FILTER, true); }
        |  'ORDERS' objectId=ID { $type = new UserEventObject($objectId.text, UserEventObject.Type.ORDER, true); }
        |  'PROPERTY' ('BEFORE' { before = true; } | 'AFTER' { before = false; })? prop=formPropertyDraw { $type = new FormChangeEvent($prop.property, before); }
        )
     )
    ;

formContainerEventDeclaration returns [String sid, boolean collapse = false]
    :   ('COLLAPSE' { $collapse = true; } | 'EXPAND')
        (   obj=ID { $sid = $obj.text; }
        |   comp=formContainersComponentSelector { $sid = $comp.sid; }
        )
    ;

scheduleFormEventDeclaration returns [int period, boolean fixed]
	:   'SCHEDULE' 'PERIOD' periodLiteral=intLiteral { $period = $periodLiteral.val; } ('FIXED' { $fixed = true; })?
	;

filterGroupDeclaration
@init {
	String filterGroupSID = null;
	List<RegularFilterInfo> filters = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
		$formStatement::form.addScriptedRegularFilterGroup(filterGroupSID, filters, self.getVersion());
	}
}
	:	'FILTERGROUP' sid=ID { filterGroupSID = $sid.text; }
		( rf=formRegularFilterDeclaration { filters.add($rf.filter); } )*
	;

extendFilterGroupDeclaration
@init {
	String filterGroupSID = null;
	List<RegularFilterInfo> filters = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
		$formStatement::form.extendScriptedRegularFilterGroup(filterGroupSID, filters, self.getVersion());
	}
}
	:	'EXTEND'	
		'FILTERGROUP' sid=ID { filterGroupSID = $sid.text; }
		( rf=formRegularFilterDeclaration { filters.add($rf.filter); } )+
	;
	
formRegularFilterDeclaration returns [RegularFilterInfo filter]
@init {
	String keyEvent = null;
	String mouseEvent = null;
	boolean isMouseEvent = false;
	boolean showKey = true;
	boolean showMouse = true;
}
    :   'FILTER' caption=localizedStringLiteral fd=formExprDeclaration (
            { isMouseEvent = false; showKey = true; showMouse = true; } ('KEY' | 'MOUSE' { isMouseEvent = true; })?
            key=stringLiteral {if(isMouseEvent) mouseEvent = $key.val; else keyEvent = $key.val;}
            ('SHOW' | 'HIDE' { if(isMouseEvent) showMouse = false; else showKey = false; })?
            )* setDefault=filterSetDefault
        {
            $filter = new RegularFilterInfo($caption.val, keyEvent, showKey, mouseEvent, showMouse, $fd.property, $fd.mapping, $setDefault.isDefault);
        }
    ;
	
formExprDeclaration returns [LP property, ImOrderSet<String> mapping, List<ResolveClassSet> signature]
    :   dfe = designOrFormExprDeclaration[null] { $property = $dfe.property; $mapping = $dfe.mapping; $signature = $dfe.signature; }
    ;

designOrFormExprDeclaration[ScriptingFormView design] returns [LP property, ImOrderSet<String> mapping, List<ResolveClassSet> signature, Object literal]
@init {
	List<TypedParameter> context = new ArrayList<>();
	if (inMainParseState()) {
	    if(design != null)
	        context = design.getTypedObjectsNames(self.getVersion());
	    else
		    context = $formStatement::form.getTypedObjectsNames(self.getVersion());
	}
}
@after {
	if (inMainParseState()) {
		if($expr.literal != null)
		    $literal = $expr.literal.value;

		$mapping = self.getUsedNames(context, $expr.property.usedParams);
		$signature = self.getUsedClasses(context, $expr.property.usedParams);
	}
}
	:	expr=propertyExpressionOrLiteral[context] { if (inMainParseState()) { $property = self.checkSingleParam($expr.property).getLP(); } }
	;

formExprOrTrivialLADeclaration returns [LP property, ImOrderSet<String> mapping, List<ResolveClassSet> signature, FormActionOrPropertyUsage fu]
@init {
	List<TypedParameter> context = new ArrayList<>();
	if (inMainParseState()) {
		context = $formStatement::form.getTypedObjectsNames(self.getVersion());
	}
}
@after {
	if (inMainParseState()) {
	    if($expr.la != null)
	        $fu = $expr.la.action;
	    else { 
	        $property = self.checkSingleParam($expr.property).getLP();
            $mapping = self.getUsedNames(context, $expr.property.usedParams);
            $signature = self.getUsedClasses(context, $expr.property.usedParams);
        }  
	}	
}
	:	expr=propertyExpressionOrTrivialLA[context, false]
	;

formActionDeclaration returns [LA action, ImOrderSet<String> mapping, List<ResolveClassSet> signature]
@init {
	List<TypedParameter> context = new ArrayList<>();
	if (inMainParseState()) {
		context = $formStatement::form.getTypedObjectsNames(self.getVersion());
	}
}
@after {
	if (inMainParseState()) {
		$mapping = self.getUsedNames(context, $aDB.action.usedParams);
		$signature = self.getUsedClasses(context, $aDB.action.usedParams);
	}
}
	:	aDB=listTopContextDependentActionDefinitionBody[context, false, false] { if (inMainParseState()) { $action = $aDB.action.getLP(); } }
	;
	
filterSetDefault returns [boolean isDefault = false]
	:	('DEFAULT' { $isDefault = true; })?
	;

userFiltersDeclaration
@init {
	List<PropertyDrawEntity> properties = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
		$formStatement::form.addScriptedUserFilters(properties, self.getVersion());
	}
}
	:	'USERFILTERS'
		prop=formPropertyDraw { properties.add($prop.property); }
		(',' prop=formPropertyDraw { properties.add($prop.property); } )*
	;

formOrderByList
@init {
	boolean ascending = true;
	List<PropertyDrawEntity> properties = new ArrayList<>();
	List<Boolean> orders = new ArrayList<>();
	boolean addFirst = false;
}
@after {
	if (inMainParseState()) {
		$formStatement::form.addScriptedDefaultOrder(properties, orders, addFirst, self.getVersion());
	}
}
	:	'ORDERS'
	    ('FIRST' { addFirst = true; })?
	    orderedProp=formPropertyDrawWithOrder { properties.add($orderedProp.property); orders.add($orderedProp.order); }
		(',' orderedProp=formPropertyDrawWithOrder { properties.add($orderedProp.property); orders.add($orderedProp.order); } )*
	;
	
formPropertyDrawWithOrder returns [PropertyDrawEntity property, boolean order = true]
	:	pDraw=formPropertyDraw { $property = $pDraw.property; } ('DESC' { $order = false; })?
	;

propertyDrawOrder returns [boolean order = true]
	:	'ORDER' ('DESC' { $order = false; })?
	;

formPivotOptionsDeclaration
@init {
	List<Pair<String, PivotOptions>> pivotOptions = new ArrayList<>();
	List<List<PropertyDrawEntity>> pivotColumns = new ArrayList<>();
	List<List<PropertyDrawEntity>> pivotRows = new ArrayList<>();
	List<PropertyDrawEntity> pivotMeasures = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
		$formStatement::form.addPivotOptions(pivotOptions, pivotColumns, pivotRows, pivotMeasures, self.getVersion());
	}
}
	:	'PIVOT'
	    (   (options=groupObjectPivotOptions { pivotOptions.add(Pair.create($options.groupObject, $options.options)); })
        |   ('COLUMNS' column=pivotPropertyDrawList { pivotColumns.add($column.props); } (',' column=pivotPropertyDrawList { pivotColumns.add($column.props); } )*)
        |   ('ROWS' row=pivotPropertyDrawList { pivotRows.add($row.props); } (',' row=pivotPropertyDrawList { pivotRows.add($row.props); } )*)
        |   ('MEASURES' measure=formPropertyDraw { pivotMeasures.add($measure.property); } (',' measure=formPropertyDraw { pivotMeasures.add($measure.property); } )*)
        )+
	;

groupObjectPivotOptions returns [String groupObject, PivotOptions options = new PivotOptions()]
    :   group=ID { $groupObject = $group.text; }
		opt = pivotOptions {$options = $opt.options; }
    ;

pivotOptions returns [PivotOptions options = new PivotOptions()]
    :
    (   t=stringLiteral { $options.setType($t.val); }
    |   a=propertyGroupType { $options.setAggregation($a.type); }
    |   ('SETTINGS'  { $options.setShowSettings(true); } | 'NOSETTINGS'  { $options.setShowSettings(false); })
    |   ('CONFIG'  sLiteral=stringLiteral { $options.setConfigFunction($sLiteral.val); })
    )*
    ;

pivotPropertyDrawList returns [List<PropertyDrawEntity> props = new ArrayList<>()]
	:	prop=formPropertyDraw { props.add($prop.property); }
	|   '(' prop=formPropertyDraw { props.add($prop.property); } (',' prop=formPropertyDraw { props.add($prop.property); } )* ')'
	;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// PROPERTY STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

propertyStatement
scope {
	String topName;
	LocalizedString topCaption;
}
@init {
	List<TypedParameter> context = new ArrayList<>();
	List<ResolveClassSet> signature = null; 
	boolean dynamic = true;
	DebugInfo.DebugPoint point = getCurrentDebugPoint();
	
	String propertyName = null;
	LocalizedString caption = null;
	LP property = null;
	PropertySettings ps = new PropertySettings();
}
@after {
	if (inMainParseState() && property != null) { // == null when native
    	self.addSettingsToProperty(property, propertyName, caption, context, signature, ps);
	    self.setPropertyScriptInfo(property, $text, point);
	}
}
	:	declaration=actionOrPropertyDeclaration { if ($declaration.params != null) { context = $declaration.params; dynamic = false; } }
		{
			$propertyStatement::topName = propertyName = $declaration.name;
			$propertyStatement::topCaption = caption = $declaration.caption;
		}
        EQ
        pdef=propertyDefinition[context, dynamic] { property = $pdef.property; signature = $pdef.signature; }
        { if (inMainParseState() && property != null) { property = self.checkPropertyIsNew(property); }}
        ((popt=propertyOptions[property, propertyName, caption, context, signature] { ps = $popt.ps; } ) | ';')
	;

actionStatement
scope {
	String topName;
	LocalizedString topCaption;
}
@init {
	List<TypedParameter> context = new ArrayList<>();
	List<ResolveClassSet> signature = null;
	boolean dynamic = true;
	DebugInfo.DebugPoint point = getCurrentDebugPoint();

	String actionName = null;
	LocalizedString caption = null;
	LA action = null;
	ActionSettings as = new ActionSettings();
}
@after {
	if (inMainParseState()) {
        self.addSettingsToAction(action, actionName, caption, context, signature, as);
		self.setPropertyScriptInfo(action, $text, point);
	}
}
	:	'ACTION'?
	    declaration=actionOrPropertyDeclaration { if ($declaration.params != null) { context = $declaration.params; dynamic = false; } }
		{
			$actionStatement::topName = actionName = $declaration.name;
			$actionStatement::topCaption = caption = $declaration.caption;
		}
        (
            (   ciADB=contextIndependentActionDB[context] { if(inMainParseState()) { action = $ciADB.action; signature = $ciADB.signature; } }
                ((aopt=actionOptions[action, actionName, caption, context, signature] { as = $aopt.as; } ) | ';')
            )
        |
            (   aDB=listTopContextDependentActionDefinitionBody[context, dynamic, true] { if (inMainParseState()) { action = $aDB.action.getLP(); signature = self.getClassesFromTypedParams(context); }}
                (aopt=actionOptions[action, actionName, caption, context, signature]  { as = $aopt.as; } )?
            )
        )
	;

propertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LP property, List<ResolveClassSet> signature]
	:	ciPD=contextIndependentPD[context, dynamic, false] { $property = $ciPD.property; $signature = $ciPD.signature; }
	|	exprOrCIPD=propertyExpressionOrContextIndependent[context, dynamic, true] { if($exprOrCIPD.ci != null) { $property = $exprOrCIPD.ci.property; $signature = $exprOrCIPD.ci.signature; } 
                                                    else { if (inMainParseState()) { $property = self.checkSingleParam($exprOrCIPD.property).getLP(); $signature = self.getClassesFromTypedParams(context); } }}
	|	'NATIVE' classId '(' clist=classIdList ')' { if (inMainParseState()) { $signature = self.createClassSetsFromClassNames($clist.ids); }}
	;


actionOrPropertyDeclaration returns [String name, LocalizedString caption, List<TypedParameter> params]
	:	nameCaption=simpleNameWithCaption { $name = $nameCaption.name; $caption = $nameCaption.caption; }
		('(' paramList=typedParameterList ')' { $params = $paramList.params; })? 
	;


propertyExpression[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
    :   exprOrCIPD=propertyExpressionOrContextIndependent[context, dynamic, false] { $property = $exprOrCIPD.property; }
        { if(inMainParseState()) { self.checkNotExprInExpr($exprOrCIPD.property, $exprOrCIPD.ci); } }
;

propertyExpressionOrContextIndependent[List<TypedParameter> context, boolean dynamic, boolean needFullContext] returns [LPWithParams property, LPContextIndependent ci]
    :   exprOrNotExpr=propertyExpressionOrNot[context, dynamic, needFullContext] { $property = $exprOrNotExpr.property;  }
        { if(inMainParseState()) { $ci = self.checkCIInExpr($exprOrNotExpr.property, $exprOrNotExpr.ci); } }
;

propertyExpressionOrTrivialLA[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPTrivialLA la]
    :   exprOrNotExpr=propertyExpressionOrNot[context, dynamic, false] { $property = $exprOrNotExpr.property;  }
        { if(inMainParseState()) { $la = self.checkTLAInExpr($exprOrNotExpr.property, $exprOrNotExpr.ci); } }
;

propertyExpressionOrLiteral[List<TypedParameter> context] returns [LPWithParams property, LPLiteral literal]
    :   exprOrNotExpr=propertyExpressionOrNot[context, false, false] { $property = $exprOrNotExpr.property;  }
        { if(inMainParseState()) { $literal = self.checkLiteralInExpr($exprOrNotExpr.property, $exprOrNotExpr.ci); } }
;

propertyExpressionOrCompoundID[List<TypedParameter> context] returns [LPWithParams property, LPCompoundID id]
    :   exprOrNotExpr=propertyExpressionOrNot[context, false, false] { $property = $exprOrNotExpr.property;  }
        { if(inMainParseState()) { $id = self.checkCompoundIDInExpr($exprOrNotExpr.property, $exprOrNotExpr.ci); } }
;

propertyExpressionOrNot[List<TypedParameter> context, boolean dynamic, boolean needFullContext] returns [LPWithParams property, LPNotExpr ci]
@init {
	DebugInfo.DebugPoint point = getCurrentDebugPoint();
}
@after{
    if (inMainParseState()) {
        LP propertyCreated = null;
        if($property != null) {
            $property = self.propertyExpressionCreated($property, context, needFullContext);
            propertyCreated = $property.getLP();
        } else if(!($ci instanceof LPTrivialLA))
            propertyCreated = ((LPContextIndependent)$ci).property;
        self.propertyDefinitionCreated(propertyCreated, point);
    }
}
	:	pe=ifPE[context, dynamic] { $property = $pe.property; $ci = $pe.ci; }
	;


ifPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPNotExpr ci]
@init {
	List<LPWithParams> props = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
		$property = self.addScriptedIfProp(props);
	}
} 
	:	firstExpr=orPE[context, dynamic] { props.add($firstExpr.property); $ci = $firstExpr.ci; }
        ( { if(inMainParseState()) { $ci = self.checkNotExprInExpr($firstExpr.property, $ci); } }
        'IF' nextExpr=orPE[context, dynamic] { props.add($nextExpr.property); }
        { if(inMainParseState()) { self.checkNotExprInExpr($nextExpr.property, $nextExpr.ci); } })*
	;

orPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPNotExpr ci]
@init {
	List<LPWithParams> props = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
		$property = self.addScriptedOrProp(props);
	}
} 
	:	firstExpr=xorPE[context, dynamic] { props.add($firstExpr.property); $ci = $firstExpr.ci; }
		( { if(inMainParseState()) { $ci = self.checkNotExprInExpr($firstExpr.property, $ci); } }
		'OR' nextExpr=xorPE[context, dynamic] { props.add($nextExpr.property); }
		 { if(inMainParseState()) { self.checkNotExprInExpr($nextExpr.property, $nextExpr.ci); } })*
	;

xorPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPNotExpr ci]
@init {
	List<LPWithParams> props = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
		$property = self.addScriptedXorProp(props);
	}
} 
	:	firstExpr=andPE[context, dynamic] { props.add($firstExpr.property); $ci = $firstExpr.ci; }
		( { if(inMainParseState()) { $ci = self.checkNotExprInExpr($firstExpr.property, $ci); } }
		'XOR' nextExpr=andPE[context, dynamic] { props.add($nextExpr.property); }
		{ if(inMainParseState()) { self.checkNotExprInExpr($nextExpr.property, $nextExpr.ci); } })*
	;

andPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPNotExpr ci]
@init {
	List<LPWithParams> props = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
		$property = self.addScriptedAndProp(props);				
	}
}
	:	firstExpr=notPE[context, dynamic] { props.add($firstExpr.property); $ci = $firstExpr.ci; }
		( { if(inMainParseState()) { $ci = self.checkNotExprInExpr($firstExpr.property, $ci); } }
		'AND' nextExpr=notPE[context, dynamic] { props.add($nextExpr.property); }
        { if(inMainParseState()) { self.checkNotExprInExpr($nextExpr.property, $nextExpr.ci); } })*
	;

notPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPNotExpr ci]
@init {
	boolean notWas = false;
}
@after {
	if (inMainParseState() && notWas) {
		$property = self.addScriptedNotProp($notExpr.property);  
	}
}
	:	'NOT' notExpr=notPE[context, dynamic] { notWas = true; } { if(inMainParseState()) { self.checkNotExprInExpr($notExpr.property, $notExpr.ci); } }
	|	expr=equalityPE[context, dynamic] { $property = $expr.property; $ci = $expr.ci; }
	;

equalityPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPNotExpr ci]
@init {
	LPWithParams leftProp = null, rightProp = null;
	String op = null;
}
@after {
	if (inMainParseState() && op != null) {
		$property = self.addScriptedEqualityProp(op, leftProp, rightProp, context);
	} else {
		$property = leftProp;
	}
}
	:	lhs=relationalPE[context, dynamic] { leftProp = $lhs.property; $ci = $lhs.ci; }
		( { if(inMainParseState()) { $ci = self.checkNotExprInExpr($lhs.property, $ci); } }
		(operand=EQ_OPERAND { op = $operand.text; } | operand=EQ { op = $operand.text; })
		rhs=relationalPE[context, dynamic] { rightProp = $rhs.property; }
		{ if(inMainParseState()) { self.checkNotExprInExpr($rhs.property, $rhs.ci); } })?
	;


relationalPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPNotExpr ci]
@init {
	LPWithParams leftProp = null, rightProp = null;
	LP mainProp = null;
	String op = null;
}
@after {
	if (inMainParseState())
	{
		if (op != null) {
			$property = self.addScriptedRelationalProp(op, leftProp, rightProp, context);
		} else {
			$property = leftProp;
		}
	}	
}
	:	lhs=likePE[context, dynamic] { leftProp = $lhs.property; $ci = $lhs.ci; }
		(
			(   { if(inMainParseState()) { $ci = self.checkNotExprInExpr($lhs.property, $ci); } }
			    operand=relOperand { op = $operand.text; }
			    rhs=likePE[context, dynamic] { rightProp = $rhs.property; }
			    { if(inMainParseState()) { self.checkNotExprInExpr($rhs.property, $rhs.ci); } }
			)
		)?
	;


likePE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPNotExpr ci]
@init {
	LPWithParams leftProp = null, rightProp = null;
	boolean match = false;
}
@after {
	if (inMainParseState()) {
	    if(rightProp != null)
		    $property = self.addScriptedLikeProp(match, leftProp, rightProp);
	    else
		    $property = leftProp;
	}
}
	:	lhs=additiveORPE[context, dynamic] { leftProp = $lhs.property; $ci = $lhs.ci; }
		( { if(inMainParseState()) { $ci = self.checkNotExprInExpr($lhs.property, $ci); } }
		('LIKE' | 'MATCH' { match = true; })
		rhs=additiveORPE[context, dynamic] { rightProp = $rhs.property; }
        { if(inMainParseState()) { self.checkNotExprInExpr($rhs.property, $rhs.ci); } })?
	;

additiveORPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPNotExpr ci]
@init {
	List<LPWithParams> props = new ArrayList<>();
	List<String> ops = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
		$property = self.addScriptedAdditiveOrProp(ops, props);
	}
}
	:	firstExpr=additivePE[context, dynamic] { props.add($firstExpr.property); $ci = $firstExpr.ci; }
		( { if(inMainParseState()) { $ci = self.checkNotExprInExpr($firstExpr.property, $ci); } }
		(operand=ADDOR_OPERAND nextExpr=additivePE[context, dynamic] { ops.add($operand.text); props.add($nextExpr.property); }
        { if(inMainParseState()) { self.checkNotExprInExpr($nextExpr.property, $nextExpr.ci); } }))*
	;
	
	
additivePE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPNotExpr ci]
@init {
	List<LPWithParams> props = new ArrayList<>();
	List<String> ops = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
		$property = self.addScriptedAdditiveProp(ops, props);				
	}
}
	:	firstExpr=multiplicativePE[context, dynamic] { props.add($firstExpr.property); $ci = $firstExpr.ci; }
		( { if(inMainParseState()) { $ci = self.checkNotExprInExpr($firstExpr.property, $ci); } }
		(operand=PLUS | operand=MINUS) { ops.add($operand.text); }
		nextExpr=multiplicativePE[context, dynamic] { props.add($nextExpr.property); }
		{ if(inMainParseState()) { self.checkNotExprInExpr($nextExpr.property, $nextExpr.ci); } })*
	;
		
	
multiplicativePE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPNotExpr ci]
@init {
	List<LPWithParams> props = new ArrayList<>();
	List<String> ops = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
		$property = self.addScriptedMultiplicativeProp(ops, props);				
	}
}
	:	firstExpr=unaryMinusPE[context, dynamic] { props.add($firstExpr.property); $ci = $firstExpr.ci; }
		( { if(inMainParseState()) { $ci = self.checkNotExprInExpr($firstExpr.property, $ci); } }
		operand=multOperand { ops.add($operand.text); }
		nextExpr=unaryMinusPE[context, dynamic] { props.add($nextExpr.property); }
		{ if(inMainParseState()) { self.checkNotExprInExpr($nextExpr.property, $nextExpr.ci); } })*
	;

unaryMinusPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPNotExpr ci] 
@init {
	boolean minusWas = false;
}
@after {
	if (inMainParseState() && minusWas) {
		$property = self.addScriptedUnaryMinusProp($expr.property);
	} 
}
	:	MINUS expr=unaryMinusPE[context, dynamic] { minusWas = true; } { if(inMainParseState()) { $ci = self.checkNumericLiteralInExpr($expr.property, $expr.ci); } }
	|	simpleExpr=postfixUnaryPE[context, dynamic] { $property = $simpleExpr.property; $ci = $simpleExpr.ci; }
	;

		 
postfixUnaryPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPNotExpr ci] 
@init {	
	boolean hasPostfix = false;
	Boolean type = null;
}
@after {
	if (inMainParseState()) {
	    if(hasPostfix)
    		$property = self.addScriptedDCCProp($expr.property, $index.val);
        else if(type != null)
            $property = self.addScriptedTypeProp($expr.property, $clsId.sid, type);
	} 
}
	:	expr=simplePE[context, dynamic] { $property = $expr.property; $ci = $expr.ci; }
		(
		    { if(inMainParseState()) { $ci = self.checkNotExprInExpr($expr.property, $ci); } }
		    (
			    '[' index=uintLiteral ']' { hasPostfix = true; }
                |
                ('IS' { type = true; } | 'AS' { type = false; } )
                clsId=classId
            )
		)?
	;		 

		 
simplePE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPNotExpr ci]
	:	'(' expr=propertyExpression[context, dynamic] ')' { $property = $expr.property; } 
	|	primitive=expressionPrimitive[context, dynamic] { $property = $primitive.property; $ci = $primitive.ci; } 
	;

	
expressionPrimitive[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPNotExpr ci]
	:	param=singleParameter[context, dynamic] { $property = $param.property; $ci = $param.ci; }
	|	expr=expressionFriendlyPD[context, dynamic] { $property = $expr.property; $ci = $expr.ci; }
	;

singleParameter[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPNotExpr ci]
@init {
	TypedParameter parameter = null;
}
@after {
	if (inMainParseState()) {
		Pair<LPWithParams, LPNotExpr> constantProp = self.addSingleParameter(parameter, $context, $dynamic, insideRecursion);
        $property = constantProp.first;
        $ci = constantProp.second;
	}
}
	:
	    tp = typedParameter { parameter = $tp.param; }
	    |
	    rp = RECURSIVE_PARAM { parameter = TP($rp.text); }
	;
	
expressionFriendlyPD[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPNotExpr ci]
@after {
	if (inMainParseState() && $ci == null) {
		self.checkPropertyValue($property.getLP());
	}
}
	:	joinDef=joinPropertyDefinition[context, dynamic] { $property = $joinDef.property; $ci = $joinDef.la; } 
	|	multiDef=multiPropertyDefinition[context, dynamic] { $property = $multiDef.property; }
	|	overDef=overridePropertyDefinition[context, dynamic] { $property = $overDef.property; }
	|	ifElseDef=ifElsePropertyDefinition[context, dynamic] { $property = $ifElseDef.property; }
	|	maxDef=maxPropertyDefinition[context, dynamic] { $property = $maxDef.property; }
	|	caseDef=casePropertyDefinition[context, dynamic] { $property = $caseDef.property; }
	|	partDef=partitionPropertyDefinition[context, dynamic] { $property = $partDef.property; }
	|	groupDef=groupCDPropertyDefinition[context, dynamic] { $property = $groupDef.property; $ci = $groupDef.ci; }
	|	recDef=recursivePropertyDefinition[context, dynamic] { $property = $recDef.property; } 
	|	structDef=structCreationPropertyDefinition[context, dynamic] { $property = $structDef.property; }
	|	concatDef=concatPropertyDefinition[context, dynamic] { $property = $concatDef.property; }
    |	jsonDef=jsonPropertyDefinition[context, dynamic] { $property = $jsonDef.property; }
    |	jsonFormDef=jsonFormPropertyDefinition[context, dynamic] { $property = $jsonFormDef.property; }
	|	castDef=castPropertyDefinition[context, dynamic] { $property = $castDef.property; }
	|	sessionDef=sessionPropertyDefinition[context, dynamic] { $property = $sessionDef.property; }
	|	signDef=signaturePropertyDefinition[context, dynamic] { $property = $signDef.property; }
	|	activeTabDef=activeTabPropertyDefinition[context, dynamic] { $property = $activeTabDef.property; }
	|	roundProp=roundPropertyDefinition[context, dynamic] { $property = $roundProp.property; }
	|	constDef=constantProperty[context, dynamic] { $property = $constDef.property; $ci = $constDef.ci; }
	|	oProp=objectPropertyDefinition { $property = $oProp.property; }
	;

contextIndependentPD[List<TypedParameter> context, boolean dynamic, boolean innerPD] returns [LP property, List<ResolveClassSet> signature, List<Integer> usedContext = Collections.emptyList()]
@init {
	DebugInfo.DebugPoint point = getCurrentDebugPoint();
}
@after{
	if (inMainParseState()) {
		self.propertyDefinitionCreated($property, point);
	}
}
	: 	dataDef=dataPropertyDefinition[context, innerPD] { $property = $dataDef.property; $signature = $dataDef.signature; }
	|	abstractDef=abstractPropertyDefinition[context, innerPD] { $property = $abstractDef.property; $signature = $abstractDef.signature; }
	|	formulaProp=formulaPropertyDefinition[context, innerPD] { $property = $formulaProp.property; $signature = $formulaProp.signature; }
	|	aggrDef=aggrPropertyDefinition[context, dynamic, innerPD] { $property = $aggrDef.property; $signature = $aggrDef.signature; $usedContext = $aggrDef.usedContext; }
	|	goProp=groupObjectPropertyDefinition { $property = $goProp.property; $signature = $goProp.signature; }
	|	reflectionDef=reflectionPropertyDefinition { $property = $reflectionDef.property; $signature = $reflectionDef.signature;  }
	;

joinPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPTrivialLA la]
@init {
	boolean isInline = false;
	boolean ci = false;
	List<Integer> usedContext = null;
}
@after {
	if (inMainParseState()) {
		if (isInline) {
			$property = self.addScriptedJProp(true, $iProp.property, $exprList.props, usedContext, ci);
		} else {
		    Pair<LPWithParams, LPTrivialLA> actionOrProperty = self.addScriptedJProp(true, $uProp.propUsage, $exprList.props, context);
            $property = actionOrProperty.first;
            $la = actionOrProperty.second;	
		}
	}
}
	:	('JOIN')? 
		(	uProp=propertyUsage
		|	iProp=inlineProperty[context] { isInline = true; usedContext = $iProp.usedContext; ci=$iProp.ci; }
		)
		'('
		exprList=propertyExpressionList[context, dynamic]
		')'
	;


aggrPropertyDefinition[List<TypedParameter> context, boolean dynamic, boolean innerPD] returns [LP property, List<ResolveClassSet> signature, List<Integer> usedContext]
@init {
    List<TypedParameter> groupContext = new ArrayList<>(context);
    DebugInfo.DebugPoint newDebugPoint = null, deleteDebugPoint = null;
    DebugInfo.DebugPoint aggrDebugPoint = getEventDebugPoint();
}
@after {
	if (inMainParseState()) {
		LPContextIndependent ci = self.addScriptedAGProp(context, $aggrClass.sid, $whereExpr.property, $et.event, aggrDebugPoint, $newEv.event, newDebugPoint, $deleteEv.event, deleteDebugPoint, innerPD);
		$property = ci.property;
		$usedContext = ci.usedContext;		
		$signature = ci.signature;
	}
}
	:	'AGGR'
	    et=baseEventPE
	    aggrClass=classId
	    'WHERE'
	    whereExpr=propertyExpression[context, dynamic]
	    ( { newDebugPoint = getEventDebugPoint(); } 'NEW' newEv=baseEventNotPE)?
	    ( { deleteDebugPoint = getEventDebugPoint(); } 'DELETE' deleteEv=baseEventNotPE)?
	;
	
groupCDPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPContextIndependent ci]
@init {
	List<TypedParameter> groupContext = new ArrayList<>(context);
    DebugInfo.DebugPoint debugPoint = getEventDebugPoint();
}
@after {
	if (inMainParseState()) {
		Pair<LPWithParams, LPContextIndependent> peOrCI = self.addScriptedCDGProp(context.size(), $exprList.props, $gp.type, $gp.mainProps, $gp.orderProps, $gp.ascending, $gp.whereProp, groupContext, debugPoint);
		$property = peOrCI.first;
		$ci = peOrCI.second;
	}
}
	:	'GROUP'
	    gp=groupPropertyBodyDefinition[groupContext]
	    ('BY' exprList=nonEmptyPropertyExpressionList[groupContext, true])?
	;
	
groupPropertyBodyDefinition[List<TypedParameter> context] returns [GroupingType type, List<LPWithParams> mainProps = new ArrayList<>(), List<LPWithParams> orderProps = new ArrayList<>(), boolean ascending = true, LPWithParams whereProp = null]
	:
    	(
    	    gt=groupingType { $type = $gt.type; }
            mainList=nonEmptyPropertyExpressionList[context, true] { $mainProps = $mainList.props; }
        |
            gt=groupingTypeOrder { $type = $gt.type; }
            mainList=nonEmptyPropertyExpressionList[context, true] { $mainProps = $mainList.props; }
            ('ORDER' ('DESC' { $ascending = false; } )?
            orderList=nonEmptyPropertyExpressionList[context, true] { $orderProps = $orderList.props; })
        |
            { boolean setOrdered = false; }
            gct = aggrCustomType
            (
                mainList=nonEmptyPropertyExpressionList[context, true] { $mainProps = $mainList.props; }
                (('WITHIN' { setOrdered = true; })? 'ORDER' ('DESC' { $ascending = false; } )?
                orderList=nonEmptyPropertyExpressionList[context, true] { $orderProps = $orderList.props; })?
                |
                ('WITHIN' { setOrdered = true; })? 'ORDER' ('DESC' { $ascending = false; } )?
                orderList=nonEmptyPropertyExpressionList[context, true] { $orderProps = $orderList.props; }
            )
            { $type = new CustomGroupingType($gct.func, setOrdered, $gct.cls, $gct.valueNull); }
        )
        ('WHERE' whereExpr=propertyExpression[context, true] { $whereProp = $whereExpr.property; } )?
    ;

aggrCustomType returns [DataClass cls = null, String func = null, boolean valueNull = false]
    :
        'CUSTOM'
        ('NULL' { $valueNull = true; } )?
        (clsName = primitiveType { if(inMainParseState()) $cls = (DataClass)self.findClass($clsName.text); })?
        t = stringLiteral { $func = $t.val; }
    ;

groupingType returns [GroupingType type]
	:	'SUM' 	{ $type = GroupingType.SUM; }
	|	'MAX' 	{ $type = GroupingType.MAX; }
	|	'MIN' 	{ $type = GroupingType.MIN; }
	|	'AGGR' { $type = GroupingType.AGGR; }
	|	'NAGGR' { $type = GroupingType.NAGGR; }
	|	'EQUAL'	{ $type = GroupingType.EQUAL; }	
	;

groupingTypeOrder returns [GroupingType type]
	:	'CONCAT' { $type = GroupingType.CONCAT; }
	|	'LAST'	{ $type = GroupingType.LAST; }
	;


partitionPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> paramProps = new ArrayList<>();
	NamedPropertyUsage pUsage = null;
	PartitionType type = null;
	int exprCnt = 1;
	int groupExprCnt = 0;
	boolean strict = false;
	int precision = 0;
	boolean ascending = true;
	boolean useLast = true;
}
@after {
	if (inMainParseState()) {
		$property = self.addScriptedPartitionProp(type, pUsage, strict, precision, ascending, useLast, exprCnt, groupExprCnt, paramProps, context);
	}
}
	:	'PARTITION' (
            (
                (	'SUM'	{ type = PartitionType.sum(); }
                |	'PREV'	{ type = PartitionType.previous(); }
                )
            |	'UNGROUP'
                ungroupProp=propertyUsage { pUsage = $ungroupProp.propUsage; }
                (	'PROPORTION' { type = PartitionType.distrCumProportion(); }
                    ('STRICT' { strict = true; })?
                    'ROUND' '(' prec=intLiteral ')' { precision = $prec.val; }
                |	'LIMIT' { type = PartitionType.distrRestrict(); }
                    ('STRICT' { strict = true; })?
                )
            )
            expr=propertyExpression[context, dynamic] { paramProps.add($expr.property); }
            (	'ORDER' ('DESC' { ascending = false; } )?
                orderList=nonEmptyPropertyExpressionList[context, dynamic] { paramProps.addAll($orderList.props); }
            )?
            |
            gct = aggrCustomType { type = PartitionType.CUSTOM($gct.func, $gct.cls, $gct.valueNull); }
            (
                mainList=nonEmptyPropertyExpressionList[context, true] { paramProps.addAll($mainList.props); exprCnt = $mainList.props.size(); }
                ('ORDER' ('DESC' { ascending = false; } )?
                orderList=nonEmptyPropertyExpressionList[context, true] { paramProps.addAll($orderList.props); })?
                |
                { exprCnt = 0; }
                'ORDER' ('DESC' { ascending = false; } )?
                orderList=nonEmptyPropertyExpressionList[context, true] { paramProps.addAll($orderList.props); }
            )
        )
		('WINDOW' 'EXCEPTLAST' { useLast = false; })?
		(	'BY'
			exprList=nonEmptyPropertyExpressionList[context, dynamic] { paramProps.addAll(0, $exprList.props); }
    		{ groupExprCnt = $exprList.props.size(); }
		)?
	;


dataPropertyDefinition[List<TypedParameter> context, boolean innerPD] returns [LP property, List<ResolveClassSet> signature]
@init {
	boolean localProp = false;
	LocalNestedType nestedType = null;

}
@after {
	if (inMainParseState()) {
	    ImList<ValueClass> paramClasses = self.findClasses($paramClassNames.ids, context);
		$property = self.addScriptedDProp($returnClass.sid, paramClasses, localProp, innerPD, false, nestedType);
		$signature = self.getParamClasses($property, paramClasses, innerPD);
	}
}
	:	'DATA'
		('LOCAL' nlm=nestedLocalModifier { localProp = true; nestedType = $nlm.nestedType; })?
		returnClass=classId
		('('
			paramClassNames=classIdList
		')')?
	;

nestedLocalModifier returns[LocalNestedType nestedType = null]
	:	('NESTED' { $nestedType = LocalNestedType.ALL; }
	        (   'MANAGESESSION' { $nestedType = LocalNestedType.MANAGESESSION; }
	        |   'NOMANAGESESSION' { $nestedType = LocalNestedType.NOMANAGESESSION; }
	        )?
        )?
	;

abstractPropertyDefinition[List<TypedParameter> context, boolean innerPD] returns [LP property, List<ResolveClassSet> signature]
@init {
	boolean isExclusive = true;
	boolean isLast = false;
	boolean isChecked = false;
	CaseUnionProperty.Type type = CaseUnionProperty.Type.MULTI;	
}
@after {
	if (inMainParseState()) {
        ImList<ValueClass> paramClasses = self.findClasses($paramClassNames.ids, context);
        $property = self.addScriptedAbstractProp(type, $returnClass.sid, paramClasses, isExclusive, isChecked, isLast, innerPD);
        $signature = self.getParamClasses($property, paramClasses, innerPD);
	}
}
	:	'ABSTRACT'
		(
			(	'CASE' { type = CaseUnionProperty.Type.CASE; isExclusive = false; }
			|	'MULTI'	{ type = CaseUnionProperty.Type.MULTI; isExclusive = true; } 
			|   'VALUE' { type = CaseUnionProperty.Type.VALUE; isExclusive = false; } 
			)
			(opt=abstractExclusiveOverrideOption { isExclusive = $opt.isExclusive; if($opt.isLast != null) isLast = $opt.isLast;})?
		)?
		('FULL' { isChecked = true; })?
		returnClass=classId
		('('
			paramClassNames=classIdList
		')')?
	;

abstractActionDefinition[List<TypedParameter> context] returns [LA action, List<ResolveClassSet> signature]
@init {
	boolean isExclusive = true;
	boolean isLast = false;
	boolean isChecked = false;
	ListCaseAction.AbstractType type = ListCaseAction.AbstractType.MULTI;
}
@after {
	if (inMainParseState()) {
        ImList<ValueClass> paramClasses = self.findClasses($paramClassNames.ids, context);
		$action = self.addScriptedAbstractAction(type, paramClasses, isExclusive, isChecked, isLast);
        $signature = self.getParamClasses($action, paramClasses, false);
	}
}
	:	'ABSTRACT'
		(
			(	
				(	'CASE' { type = ListCaseAction.AbstractType.CASE; isExclusive = false; }
			 	|	'MULTI'	{ type = ListCaseAction.AbstractType.MULTI; isExclusive = true; }
			 	) (opt=abstractExclusiveOverrideOption { isExclusive = $opt.isExclusive; if ($opt.isLast!=null) isLast = $opt.isLast;})?
			)
		|	('LIST' { type = ListCaseAction.AbstractType.LIST; isLast = true; } (acopt=abstractCaseAddOption { isLast = $acopt.isLast; } )?
		)
		)?
		('FULL' { isChecked = true; })?
		('('
			paramClassNames=classIdList
		')')?
	;
	
overridePropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean isExclusive = false;
}
@after {
	if (inMainParseState()) {
		$property = self.addScriptedOverrideProp($exprList.props, isExclusive);
	}
}
	:	(('OVERRIDE') | ('EXCLUSIVE' { isExclusive = true; })) 
		exprList=nonEmptyPropertyExpressionList[context, dynamic] 
	;


ifElsePropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	LPWithParams elseProp = null;
}
@after {
	if (inMainParseState()) {
		$property = self.addScriptedIfElseUProp($ifExpr.property, $thenExpr.property, elseProp);
	}
}
	:	'IF' ifExpr=propertyExpression[context, dynamic]
		'THEN' thenExpr=propertyExpression[context, dynamic]
		('ELSE' elseExpr=propertyExpression[context, dynamic] { elseProp = $elseExpr.property; })?
	;


maxPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean isMin = true;
}
@after {
	if (inMainParseState()) {
		$property = self.addScriptedMaxProp($exprList.props, isMin);
	}
}
	:	(('MAX') { isMin = false; } | ('MIN'))
		exprList=nonEmptyPropertyExpressionList[context, dynamic]	
	;


casePropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> whenProps = new ArrayList<>();
	List<LPWithParams> thenProps = new ArrayList<>();
	LPWithParams elseProp = null;
	boolean isExclusive = false;
}
@after {
	if (inMainParseState()) {
		$property = self.addScriptedCaseUProp(whenProps, thenProps, elseProp, isExclusive);
	}
}
	:	'CASE' (opt=exclusiveOverrideOption { isExclusive = $opt.isExclusive; })?
			( branch=caseBranchBody[context, dynamic] { whenProps.add($branch.whenProperty); thenProps.add($branch.thenProperty); } )+
			('ELSE' elseExpr=propertyExpression[context, dynamic] { elseProp = $elseExpr.property; })?
	;
	
	
caseBranchBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams whenProperty, LPWithParams thenProperty]
	:	'WHEN' whenExpr=propertyExpression[context, dynamic] { $whenProperty = $whenExpr.property; }
		'THEN' thenExpr=propertyExpression[context, dynamic] { $thenProperty = $thenExpr.property; }
	;

multiPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean isExclusive = true;
}
@after {
	if (inMainParseState()) {
		$property = self.addScriptedMultiProp($exprList.props, isExclusive);
	}
}
	:	'MULTI' 
		exprList=nonEmptyPropertyExpressionList[context, dynamic] 
		(opt=exclusiveOverrideOption { isExclusive = $opt.isExclusive; })? // нельзя наверх так как есть оператор OVERRIDE
	;

recursivePropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	Cycle cycleType = Cycle.NO;
	List<TypedParameter> recursiveContext = null;
	if (inMainParseState() && insideRecursion) {
		self.getErrLog().emitNestedRecursionError(self.getParser());
	}
    DebugInfo.DebugPoint debugPoint = getEventDebugPoint();
}
@after {
	if (inMainParseState()) {
		$property = self.addScriptedRProp(recursiveContext, $zeroStep.property, $nextStep.property, cycleType, debugPoint);
	}
	insideRecursion = false;
}
	:	'RECURSION'
		zeroStep=propertyExpression[context, dynamic]
		'STEP'
		{ 
			insideRecursion = true; 
		  	recursiveContext = new ArrayList<TypedParameter>(context);
		}
		nextStep=propertyExpression[recursiveContext, dynamic]
		('CYCLES' 
			(	'YES' { cycleType = Cycle.YES; }
			|	'NO' { cycleType = Cycle.NO; } 
			|	'IMPOSSIBLE' { cycleType = Cycle.IMPOSSIBLE; }
			)
		)?
	;

structCreationPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property] 
@after {
	if (inMainParseState()) {
		$property = self.addScriptedCCProp($list.props);
	}
}
	:	'STRUCT'
		'('
		list=nonEmptyPropertyExpressionList[context, dynamic]
		')' 
	;

castPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inMainParseState()) {
		$property = self.addScriptedCastProp($ptype.text, $expr.property);
	}
}
	:   ptype=primitiveType '(' expr=propertyExpression[context, dynamic] ')'
	;

concatPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inMainParseState()) {
		$property = self.addScriptedConcatProp($separator.val, $list.props);
	}
}
	:   'CONCAT' separator=stringLiteral ',' list=nonEmptyPropertyExpressionList[context, dynamic]
	;

jsonFormPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, FormEntity form, MappedForm mapped]
@init {
    List<TypedParameter> objectsContext = null;
    List<LPWithParams> contextFilters = new ArrayList<>();
    boolean returnString = false;
}
@after {
	if (inMainParseState()) {
	    $property = self.addScriptedJSONFormProp($mf.mapped, $mf.props, objectsContext, contextFilters, context, returnString);
	}
}
	:   ('JSON' | 'JSONTEXT' { returnString = true; }) '(' mf=mappedForm[context, null, dynamic] {
                if(inMainParseState())
                    objectsContext = self.getTypedObjectsNames($mf.mapped);
            }
            (cf = contextFiltersClause[context, objectsContext] { contextFilters.addAll($cf.contextFilters); })?
        ')'
//        'ENDJSONX'
	;

jsonPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<TypedParameter> newContext = new ArrayList<>(context);
    List<LPWithParams> orderProperties = new ArrayList<>();
    List<Boolean> orderDirections = new ArrayList<>();
    boolean returnString = false;
}
@after {
	if (inMainParseState()) {
		$property = self.addScriptedJSONProperty(context, $plist.aliases, $plist.literals, $plist.properties, $plist.propUsages,
		 $whereExpr.property, orderProperties, orderDirections, returnString);
	}
}
	:	('JSON' | 'JSONTEXT' { returnString = true; })
		'FROM' plist=nonEmptyAliasedPropertyExpressionList[newContext, true]
		('WHERE' whereExpr=propertyExpression[newContext, true])?
		('ORDER' orderedProp=propertyExpressionWithOrder[newContext, true] { orderProperties.add($orderedProp.property); orderDirections.add($orderedProp.order); }
        	(',' orderedProp=propertyExpressionWithOrder[newContext, true] { orderProperties.add($orderedProp.property); orderDirections.add($orderedProp.order); } )*
        )?
	;

sessionPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	IncrementType type = null; 
}
@after {
	if (inMainParseState()) {
		$property = self.addScriptedSessionProp(type, $expr.property);
	}
}
	:	(	'PREV' { type = null; } 
		| 	'CHANGED' { type = IncrementType.CHANGED; }
		| 	'SET' { type = IncrementType.SET; }
		| 	'DROPPED' { type = IncrementType.DROP; }
		| 	'SETCHANGED' { type = IncrementType.SETCHANGED; }
		|	'DROPCHANGED' { type = IncrementType.DROPCHANGED; }
		| 	'SETDROPPED' { type = IncrementType.DROPSET; }
		)
		'('
		expr=propertyExpression[context, dynamic] 
		')'
	;

signaturePropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property] 	
@after {
	if (inMainParseState()) {
		$property = self.addScriptedSignatureProp($expr.property);
	}
} 
	: 	'ISCLASS' '(' expr=propertyExpression[context, dynamic] ')'
	;

activeTabPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inMainParseState()) {
		$property = self.addScriptedActiveTabProp($fc.component);
	}
}
	: 	'ACTIVE' 'TAB' fc = formComponentID
	;

roundPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inMainParseState()) {
		property = self.addScriptedRoundProp($expr.property, $scaleExpr.property);
	}
}
	:	'ROUND' '(' expr=propertyExpression[context, dynamic] (',' scaleExpr = propertyExpression[context, dynamic] )? ')'
	;

formulaPropertyDefinition[List<TypedParameter> context, boolean innerPD] returns [LP property, List<ResolveClassSet> signature]
@init {
	String className = null;
	boolean valueNull = false;
	boolean paramsNull = false;
}
@after {
	if (inMainParseState()) {
        ImList<ValueClass> paramClasses = self.findClasses($params.classNames, context);
        List<String> paramNames = self.getParamNamesFromTypedParams($params.ids, context, innerPD);
		$property = self.addScriptedSFProp($value.className, $value.id, paramClasses, paramNames, $synt.types, $synt.strings, valueNull, paramsNull);
        $signature = self.getParamClasses($property, paramClasses, innerPD);
	}
}
	:	'FORMULA'
		('NULL' { valueNull = true; })?
	    (value = typedIdOrStringLiteral)?
		synt=formulaPropertySyntaxList
        ('('
            params=typedIdOrStringLiteralList
        ')')?
		('NULL' { paramsNull = true; })?
	;

idOrStringLiteral returns [String id, boolean literal]
    :
        pid=ID { $id = $pid.text; $literal = false; }
    |	sLiteral=stringLiteral { $id = $sLiteral.val; $literal = true; }
;

typedIdOrStringLiteral returns [String className, String id]
    :
    clsName=classId { $className = $clsName.sid; }
    (exid = idOrStringLiteral { $id = $exid.id; } )?
;

typedIdOrStringLiteralList returns [List<String> classNames, List<String> ids]
@init {
	$classNames = new ArrayList<>();
	$ids = new ArrayList<>();
}
	:	(neList=nonEmptyTypedIdOrStringLiteralList { $classNames = $neList.classNames; $ids = $neList.ids; })?
	;

nonEmptyTypedIdOrStringLiteralList returns [List<String> classNames, List<String> ids]
@init {
	$classNames = new ArrayList<>();
	$ids = new ArrayList<>();
}
	:	firstIsl=typedIdOrStringLiteral { $classNames.add($firstIsl.className); $ids.add($firstIsl.id); }
		(',' isl=typedIdOrStringLiteral { $classNames.add($isl.className); $ids.add($isl.id); })*
	;

formulaPropertySyntaxList returns [List<SQLSyntaxType> types = new ArrayList<>(), List<String> strings = new ArrayList<>()]
	:	firstType=formulaPropertySyntaxType firstText=stringLiteral { $types.add($firstType.type); $strings.add($firstText.val); }
		(',' nextType=formulaPropertySyntaxType nextText=stringLiteral { $types.add($nextType.type); $strings.add($nextText.val); })*
	;

formulaPropertySyntaxType returns [SQLSyntaxType type = null]
	:	('PG' { $type = SQLSyntaxType.POSTGRES; } | 'MS' { $type = SQLSyntaxType.MSSQL; })? 
	;

groupObjectPropertyDefinition returns [LP property, List<ResolveClassSet> signature]
@init {
	GroupObjectProp prop = null;
}
@after {
	if (inMainParseState()) {
		$signature = new ArrayList<>();	
		$property = self.addScriptedGroupObjectProp($gobj.sid, prop, $signature);
	}
}
	:	('FILTER' { prop = GroupObjectProp.FILTER; } | 'ORDER' { prop = GroupObjectProp.ORDER; } | 'VIEW' { prop = GroupObjectProp.VIEW; } )
		gobj=formGroupObjectID
	;

objectPropertyDefinition returns [LPWithParams property]
@init {
	GroupObjectProp prop = null;
}
@after {
	if (inMainParseState()) {
		$property = self.addScriptedValueObjectProp($gobj.sid);
	}
}
	:	'VALUE'
		gobj=formObjectID
	;
	
reflectionPropertyDefinition returns [LP property, List<ResolveClassSet> signature]
@init {
	ReflectionPropertyType type = null;
	ActionOrPropertyUsage propertyUsage = null;
}
@after{
	if (inMainParseState()) {
		$signature = new ArrayList<>();	
		$property = self.addScriptedReflectionProperty(type, propertyUsage, $signature);
	}
}
	:	'REFLECTION' t=reflectionPropertyType { type = $t.type; } pu=actionOrPropertyUsage { propertyUsage = $pu.propUsage; }
	;
	
reflectionPropertyType returns [ReflectionPropertyType type]
	:	'CANONICALNAME' { $type = ReflectionPropertyType.CANONICAL_NAME; }
	;

readActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
    boolean clientAction = false;
    boolean dialog = false;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedReadAction($expr.property, $pUsage.propUsage, context, clientAction, dialog);
	}
}
	:	'READ' ('CLIENT' { clientAction = true; } ('DIALOG' { dialog = true; })? )? expr=propertyExpression[context, dynamic] ('TO' pUsage=propertyUsage)?
	;

writeActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
    boolean clientAction = false;
	boolean dialog = false;
	boolean append = false;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedWriteAction($fromExpr.property, $expr.property, context, clientAction, dialog, append);
	}
}
	:	'WRITE' ('CLIENT' { clientAction = true; } ('DIALOG' { dialog = true; })? )? fromExpr=propertyExpression[context, dynamic]
	    'TO' expr=propertyExpression[context, dynamic] ('APPEND' { append = true; })?

	;

importActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
    List<TypedParameter> newContext = new ArrayList<TypedParameter>(context);

	FormIntegrationType format = null;
	LPWithParams sheet = null;
	boolean sheetAll = false;
	LPWithParams memo = null;
	LPWithParams where = null;
	String separator = null;
	boolean hasHeader = false;
	boolean noEscape = false;
	String charset = null;
	LPWithParams root = null;
	boolean attr = false;
    List<TypedParameter> fieldParams = null;
    List<String> toParams = null;

	List<String> ids = null;
	List<Boolean> literals = null;
}
@after {
	if (inMainParseState()) {
        $action = self.addScriptedImportAction(format, $expr.property, ids, literals, $plist.propUsages, $pflist.nulls, $dDB.action, $dDB.elseAction, context, newContext, $wherePropertyUsage.propUsage, sheet, sheetAll, separator, !hasHeader, noEscape, charset, root, fieldParams, toParams, attr, where, memo);
	}
} 
	:	'IMPORT' 
		(type = importSourceFormat [context, dynamic] { format = $type.format; sheet = $type.sheet; sheetAll = $type.sheetAll; memo = $type.memo; where = $type.where; separator = $type.separator;
		        hasHeader = $type.hasHeader; noEscape = $type.noEscape; root = $type.root; attr = $type.attr; charset = $type.charset; })?
		'FROM' expr=propertyExpression[context, dynamic] { if (inMainParseState()) self.getChecks().checkImportFromFileExpression($expr.property); }
		(
            'FIELDS' ('(' list=typedParameterList { if(inMainParseState()) { fieldParams = list; } } ')')?
            {
                if(inMainParseState()) {
                    if(fieldParams == null)
                        fieldParams = Arrays.asList(TP("INTEGER", "row"));
                    self.getParamIndices(fieldParams, newContext, true, insideRecursion);
                }
            }
            pflist = nonEmptyImportFieldDefinitions[newContext] { ids = $pflist.ids; literals = $pflist.literals; }
            dDB=doInputBody[context, newContext]
            |
		    'TO' ('(' paramClassNames=classIdList { if(inMainParseState()) { toParams = $paramClassNames.ids; } } ')')?
             {
                 if(inMainParseState() && toParams == null) {
                     toParams = Collections.singletonList("INTEGER");
                 }
             }
		    plist = nonEmptyPropertyUsageListWithIds { ids = $plist.ids; literals = $plist.literals; }
		    ('WHERE' wherePropertyUsage=propertyUsage)?
		)
	;

nonEmptyImportFieldDefinitions[List<TypedParameter> newContext] returns [List<String> ids, List<Boolean> literals, List<Boolean> nulls]
@init {
	$ids = new ArrayList<String>();
	$literals = new ArrayList<Boolean>();
	$nulls = new ArrayList<Boolean>();
}
	:	field = importFieldDefinition[newContext] { $ids.add($field.id); $literals.add($field.literal); $nulls.add($field.nulls); }
		(',' field = importFieldDefinition[newContext] { $ids.add($field.id); $literals.add($field.literal); $nulls.add($field.nulls); })*
	;

importFieldDefinition[List<TypedParameter> newContext] returns [String id, Boolean literal, boolean nulls = false]
@init {
    DataClass dataClass = null;
}
    :
        ptype=primitiveType { if(inMainParseState()) dataClass = (DataClass)self.findClass($ptype.text); }
        (varID=ID EQ)?
        exid = idOrStringLiteral { $id = $exid.id; $literal = $exid.literal; }
        ('NULL' { $nulls = true; } )?
        {
        	if(inMainParseState())
                self.getParamIndex(self.new TypedParameter(dataClass, $varID.text != null ? $varID.text : $id), newContext, true, insideRecursion);
        }
    ;

exportActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
	List<TypedParameter> newContext = new ArrayList<>(context); 

    FormIntegrationType format = null;
    List<LPWithParams> orderProperties = new ArrayList<>();
    List<Boolean> orderDirections = new ArrayList<>();
	String separator = null;
	Boolean hasHeader = null;
	boolean noEscape = false;
	String charset = null;
	LPWithParams sheetName = null;
	boolean attr = false;
	LPWithParams root = null;
	LPWithParams tag = null;

}
@after {
	if (inMainParseState()) {
			$action = self.addScriptedExportAction(context, format, $plist.aliases, $plist.literals, $plist.properties, $plist.propUsages, $whereExpr.property, $pUsage.propUsage,
			                                                 sheetName, root, tag, separator, hasHeader, noEscape, new SelectTop($selectTop.property), charset, attr, orderProperties, orderDirections);
	}
} 
	:	'EXPORT'
	    (type = exportSourceFormat [context, dynamic] { format = $type.format; separator = $type.separator; hasHeader = $type.hasHeader; noEscape = $type.noEscape;
	                                                    sheetName = $type.sheetName; charset = $type.charset; root = $type.root; tag = $type.tag; attr = $type.attr; })?
		('TOP' selectTop = propertyExpression[context, dynamic])?
		'FROM' plist=nonEmptyAliasedPropertyExpressionList[newContext, true]
		('WHERE' whereExpr=propertyExpression[newContext, true])?
		('ORDER' orderedProp=propertyExpressionWithOrder[newContext, true] { orderProperties.add($orderedProp.property); orderDirections.add($orderedProp.order); }
        	(',' orderedProp=propertyExpressionWithOrder[newContext, true] { orderProperties.add($orderedProp.property); orderDirections.add($orderedProp.order); } )*
        )?
		('TO' pUsage=propertyUsage)?
	;

propertyExpressionWithOrder[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, boolean order = true]
	:	pDraw=propertyExpression[context, dynamic] { $property = $pDraw.property; } ('DESC' { $order = false; })?
	;

nonEmptyAliasedPropertyExpressionList[List<TypedParameter> context, boolean dynamic] returns [List<String> aliases = new ArrayList<>(), List<Boolean> literals = new ArrayList<>(), List<LPWithParams> properties = new ArrayList<>(), List<LPTrivialLA> propUsages = new ArrayList<>()]
@init {
    String alias;
}
    :
        expr=exportAliasedPropertyExpression[context, dynamic] { $aliases.add($expr.alias); $literals.add($expr.literal); $properties.add($expr.property); $propUsages.add($expr.propUsage); }
		(',' expr=exportAliasedPropertyExpression[context, dynamic] { $aliases.add($expr.alias); $literals.add($expr.literal); $properties.add($expr.property); $propUsages.add($expr.propUsage); } )*
	;

exportAliasedPropertyExpression[List<TypedParameter> context, boolean dynamic] returns [String alias = null, Boolean literal = null, LPWithParams property, LPTrivialLA propUsage]
    :
        ( { (input.LA(1)==ID || input.LA(1)==STRING_LITERAL) && input.LA(2)==EQ }?
          exid = idOrStringLiteral { $alias = $exid.id; $literal = $exid.literal; }
          EQ
        )?
        expr=propertyExpressionOrTrivialLA[context, dynamic] { $property = $expr.property; $propUsage = $expr.la; }
    ;

importFormActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
    FormIntegrationType format = null;
	LPWithParams sheet = null;
	boolean sheetAll = false;
	LPWithParams memo = null;
	LPWithParams where = null;
	String separator = null;
	boolean hasHeader = false;
	boolean noEscape = false;
	String charset = null;
	LPWithParams root = null;
	boolean attr = false;
    FormEntity form = null;
}
@after {
	if (inMainParseState()) {
	    $action = self.addScriptedImportFormAction(format, context, $fileExprs.property, $fileExprs.properties, form, sheet, sheetAll, !hasHeader, noEscape, attr, charset, separator, root, where, memo);
	}
}
	:	'IMPORT'
	    (namespace=ID '.')? formSName=ID { if (inMainParseState()) { form = self.findForm(($namespace == null ? "" : $namespace.text + ".") + $formSName.text); }}
	    (type = importSourceFormat [context, dynamic] { format = $type.format; sheet = $type.sheet; sheetAll = $type.sheetAll; where = $type.where; memo = $type.memo; separator = $type.separator;
               hasHeader = $type.hasHeader; noEscape = $type.noEscape; root = $type.root; attr = $type.attr; charset = $type.charset;   })?
	    ('FROM' fileExprs=importFormPropertyExpressions[context, dynamic, form])?
	;

importFormPropertyExpressions[List<TypedParameter> context, boolean dynamic, FormEntity formEntity] returns [LPWithParams property, OrderedMap<GroupObjectEntity, LPWithParams> properties]
@init {
	$properties = new OrderedMap<>();
	GroupObjectEntity go = null;
}
	:  aliasedPE=importAliasedPropertyExpression[context, dynamic] { if(inMainParseState()) { if($aliasedPE.alias == null) { $property = $aliasedPE.property; } else { $properties.put(self.findGroupObjectEntity(formEntity, $aliasedPE.alias), $aliasedPE.property); } } }
		(',' nextGroupObject=ID { if(inMainParseState()) { go=self.findGroupObjectEntity(formEntity, $nextGroupObject.text); } } EQ nextPropertyExpression = propertyExpression[context, dynamic] { if(inMainParseState()) { $properties.put(go, $nextPropertyExpression.property); } } )*
	;

importAliasedPropertyExpression[List<TypedParameter> context, boolean dynamic] returns [String alias = null, LPWithParams property]
    :
        ( { input.LA(1)==ID && input.LA(2)==EQ }?
          ( simpleName=ID { $alias = $simpleName.text; } )
          EQ
        )?
        expr=propertyExpression[context, dynamic] { $property = $expr.property; }
    ;

newThreadActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
	List<LPWithParams> props = new ArrayList<>();
	List<LP> localProps = new ArrayList<LP>();
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedNewThreadAction($aDB.action, $connExpr.property, $periodExpr.property, $delayExpr.property, $pUsage.propUsage);
	}
}
	:	'NEWTHREAD' aDB=keepContextFlowActionDefinitionBody[context, dynamic]
	    (
	    	(   'CONNECTION' connExpr=propertyExpression[context, dynamic]
		    |   'SCHEDULE' ('PERIOD' periodExpr=propertyExpression[context, dynamic])? ('DELAY' delayExpr=propertyExpression[context, dynamic])?
		    |   'TO' pUsage=propertyUsage
    	    )
    	    ';'
        )?
	;

newExecutorActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
	List<LPWithParams> props = new ArrayList<>();
	List<LP> localProps = new ArrayList<LP>();
	Boolean syncType = null;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedNewExecutorAction($aDB.action, $threadsExpr.property, syncType);
	}
}
	:	'NEWEXECUTOR' aDB=keepContextFlowActionDefinitionBody[context, dynamic]
	        'THREADS' threadsExpr=propertyExpression[context, dynamic]
	         (sync = syncTypeLiteral { syncType = $sync.val; })? ';'
	;

newSessionActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
	List<NamedPropertyUsage> migrateSessionProps = Collections.emptyList();
	boolean migrateClasses = false;
	boolean migrateAllSessionProps = false;
	boolean isNested = false;
	boolean singleApply = false;
	boolean newSQL = false;
	List<String> ids = null;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedNewSessionAProp($aDB.action, migrateSessionProps, migrateAllSessionProps, migrateClasses, isNested, singleApply, newSQL, ids);
	}
}
	:	(	'NEWSESSION' ('NEWSQL' { newSQL = true; })?
	        ('FORMS' (neIdList=nonEmptyCompoundIdList { ids = $neIdList.ids; }) )?
	        (mps=nestedPropertiesSelector { migrateAllSessionProps = $mps.all; migrateSessionProps = $mps.props;
	            migrateClasses = $mps.classes; })?

		|	'NESTEDSESSION' { isNested = true; }
		)
		('SINGLE' { singleApply = true; })?
		aDB=keepContextFlowActionDefinitionBody[context, dynamic]
	;

nonEmptyPropertyUsageListWithIds returns [List<String> ids, List<Boolean> literals, List<NamedPropertyUsage> propUsages]
@init {
	$ids = new ArrayList<String>();
	$literals = new ArrayList<Boolean>();
	$propUsages = new ArrayList<NamedPropertyUsage>();
}
	:	usage = propertyUsageWithId { $ids.add($usage.id); $literals.add($usage.literal); $propUsages.add($usage.propUsage); }
		(',' usage = propertyUsageWithId { $ids.add($usage.id); $literals.add($usage.literal); $propUsages.add($usage.propUsage); })*
	;

propertyUsageWithId returns [String id = null, Boolean literal = null, NamedPropertyUsage propUsage]
	:	pu=propertyUsage { $propUsage = $pu.propUsage; }
		(	EQ
            exid = idOrStringLiteral { $id = $exid.id; $literal = $exid.literal; }
		)?
	;

importSourceFormat [List<TypedParameter> context, boolean dynamic] returns [FormIntegrationType format, LPWithParams sheet, boolean sheetAll, LPWithParams memo, LPWithParams where, String separator, boolean hasHeader, boolean noEscape, String charset, LPWithParams root, boolean attr]
	:	'CSV'	{ $format = FormIntegrationType.CSV; } (
	            (separatorVal = stringLiteral { $separator = $separatorVal.val; })?
	            (hasHeaderVal = hasHeaderOption { $hasHeader = $hasHeaderVal.hasHeader; })?
	            (noEscapeVal = noEscapeOption { $noEscape = $noEscapeVal.noEscape; })?
	            ('WHERE' whereProperty = propertyExpression[context, dynamic] {$where = $whereProperty.property; })?
	            ('CHARSET' charsetVal = stringLiteral { $charset = $charsetVal.val; })?
	            )
    |	'DBF'	{ $format = FormIntegrationType.DBF; } (
                ('MEMO' memoProperty = propertyExpression[context, dynamic] {$memo = $memoProperty.property; })?
                ('WHERE' whereProperty = propertyExpression[context, dynamic] {$where = $whereProperty.property; })?
                ('CHARSET' charsetVal = stringLiteral { $charset = $charsetVal.val; })?
                )
    |   'XLS' 	{ $format = FormIntegrationType.XLS; } (
                (hasHeaderVal = hasHeaderOption { $hasHeader = $hasHeaderVal.hasHeader; })?
                ('SHEET' ((sheetProperty = propertyExpression[context, dynamic] { $sheet = $sheetProperty.property; }) | ('ALL' {$sheetAll = true; })) )?
                ('WHERE' whereProperty = propertyExpression[context, dynamic] {$where = $whereProperty.property; })?
                )
	|	'JSON'	{ $format = FormIntegrationType.JSON; } (
	            ('ROOT' rootProperty = propertyExpression[context, dynamic] {$root = $rootProperty.property; })?
	            ('WHERE' whereProperty = propertyExpression[context, dynamic] {$where = $whereProperty.property; })?
	            ('CHARSET' charsetVal = stringLiteral { $charset = $charsetVal.val; })?
	            )
	|	'XML'	{ $format = FormIntegrationType.XML; } (
	            ('ROOT' rootProperty = propertyExpression[context, dynamic] {$root = $rootProperty.property; })?
	            ('ATTR' { $attr = true; })?
	            ('WHERE' whereProperty = propertyExpression[context, dynamic] {$where = $whereProperty.property; })?
	            )
	|	'TABLE'	{ $format = FormIntegrationType.TABLE; } (
	            ('WHERE' whereProperty = propertyExpression[context, dynamic] {$where = $whereProperty.property; })?
	            )
	;

propertyUsage returns [String name, NamedPropertyUsage propUsage]
@init {
	List<String> classList = null;
}
@after {
	$propUsage = new NamedPropertyUsage($pname.name, classList);
}
	:	pname=propertyName { $name = $pname.name; } ('[' cidList=signatureClassList ']' { classList = $cidList.ids; })?
	;

inlineProperty[List<TypedParameter> context] returns [LP property, List<Integer> usedContext, boolean ci]
@init {
	List<TypedParameter> newContext = new ArrayList<>(context);
	List<ResolveClassSet> signature = null;
}
@after {
	if (inMainParseState()) { // not native
		$property.setExplicitClasses(signature);
	}
}
	:	'[' 	(	ciPD=contextIndependentPD[context, true, true] { $property = $ciPD.property; signature = $ciPD.signature; $usedContext = $ciPD.usedContext; $ci = true; }
				|   exprOrCIPD=propertyExpressionOrContextIndependent[newContext, true, false] { if($exprOrCIPD.ci != null) { $property = $exprOrCIPD.ci.property; signature = $exprOrCIPD.ci.signature; $usedContext = $exprOrCIPD.ci.usedContext; $ci = true; }
                                                                    else { if (inMainParseState()) { $property = self.checkSingleParam($exprOrCIPD.property).getLP(); $usedContext = self.getResultInterfaces(context.size(), $exprOrCIPD.property); signature = self.getClassesFromTypedParams(context.size(), $usedContext, newContext);} }}
				)
		']'
	;

propertyName returns [String name]
	:	id=compoundID { $name = $id.sid; }
	;

propertyOptions[LP property, String propertyName, LocalizedString caption, List<TypedParameter> context, List<ResolveClassSet> signature] returns [PropertySettings ps = new PropertySettings()]
	:	recursivePropertyOptions[property, propertyName, caption, $ps, context]
	;

recursivePropertyOptions[LP property, String propertyName, LocalizedString caption, PropertySettings ps, List<TypedParameter> context]
	:	semiPropertyOption[property, propertyName, caption, ps, context] (';' | recursivePropertyOptions[property, propertyName, caption, ps, context])
	|	nonSemiPropertyOption[property, propertyName, caption, ps, context] recursivePropertyOptions[property, propertyName, caption, ps, context]?
	;

actionOptions[LA action, String actionName, LocalizedString caption, List<TypedParameter> context, List<ResolveClassSet> signature] returns [ActionSettings as = new ActionSettings()]
	:	recursiveActionOptions[action, actionName, caption, $as, context]
	;

recursiveActionOptions[LA action, String actionName, LocalizedString caption, ActionSettings as, List<TypedParameter> context]
	:	semiActionOption[action, actionName, caption, as, context] (';' | recursiveActionOptions[action, actionName, caption, as, context])
	|	nonSemiActionOption[action, actionName, caption, as, context] recursiveActionOptions[action, actionName, caption, as, context]?
	;

semiActionOrPropertyOption[LAP property, String propertyName, LocalizedString caption, ActionOrPropertySettings ps, List<TypedParameter> context]
    :	inSetting [ps]
	|	viewTypeSetting [property]
	|	customViewSetting [property]
	|	flexCharWidthSetting [property]
	|	charWidthSetting [property]
	|	changeKeySetting [property]
	|	changeMouseSetting [property]
	|	stickySetting [property]
	|	syncSetting [property]
	|   imageSetting [property]
	|   '@@' ann = ID { ps.annotation = $ann.text; }
    ;

semiPropertyOption[LP property, String propertyName, LocalizedString caption, PropertySettings ps, List<TypedParameter> context]
    :	semiActionOrPropertyOption[property, propertyName, caption, ps, context]
    |   materializedSetting [ps]
    |	indexedSetting [ps]
	|	complexSetting [ps]
	|	prereadSetting [ps]
	|	hintSettings [ps]
	|	tableSetting [ps]
	|   defaultCompareSetting [property]
	|	autosetSetting [property]
	|	patternSetting [property]
	|	regexpSetting [property]
	|	echoSymbolsSetting [property]
	|	setNotNullSetting [ps]
	|	aggrSetting [property]
	|	eventIdSetting [property]
    ;

semiActionOption[LA action, String actionName, LocalizedString caption, ActionSettings ps, List<TypedParameter> context]
    :	semiActionOrPropertyOption[action, actionName, caption, ps, context]
	|	shortcutSetting [action, caption != null ? caption : LocalizedString.create(actionName)]
	|	asonEventActionSetting [action]
	|	confirmSetting [action]
    ;

nonSemiActionOrPropertyOption[LAP property, String propertyName, LocalizedString caption, ActionOrPropertySettings ps, List<TypedParameter> context]
    :	onEditEventSetting [property, context]
    |	onContextMenuEventSetting [property, context]
    |	onKeyPressEventSetting [property, context]
    ;

nonSemiPropertyOption[LP property, String propertyName, LocalizedString caption, PropertySettings ps, List<TypedParameter> context]
    :   nonSemiActionOrPropertyOption[property, propertyName, caption, ps, context]
    ;

nonSemiActionOption[LA action, String actionName, LocalizedString caption, ActionSettings as, List<TypedParameter> context]
    :   nonSemiActionOrPropertyOption[action, actionName, caption, as, context]
    ;

inSetting [ActionOrPropertySettings ps]
	:	'IN' name=compoundID { ps.groupName = $name.sid; }
	;

materializedSetting [PropertySettings ps]
	:	'MATERIALIZED' (name=stringLiteral)? { ps.isMaterialized = true; ps.field = $name.val; }
	;

indexedSetting [PropertySettings ps]
	:	'INDEXED' { ps.indexType = IndexType.DEFAULT; } (dbName=stringLiteral { ps.indexName = $dbName.val; })?
	        (('LIKE' { ps.indexType = IndexType.LIKE; }) | ('MATCH' { ps.indexType = IndexType.MATCH; }))?
	;

complexSetting [PropertySettings ps]
	:	('COMPLEX' { ps.isComplex = true; } | 'NOCOMPLEX' { ps.isComplex = false; } )
	;

prereadSetting [PropertySettings ps]
	:	'PREREAD' { ps.isPreread = true; }
	;

hintSettings [PropertySettings ps]
	:	('HINT' { ps.isHint = true; } | 'NOHINT' { ps.isHint = false; } )
	;

tableSetting [PropertySettings ps]
	:	'TABLE' tbl = compoundID { ps.table = $tbl.sid; }
	;

aggrSetting [LP property]
@after {
	if (inMainParseState()) {
		self.setAggr(property);
	}
}
    :
        'AGGR'
    ;

setNotNullSetting [PropertySettings ps]
    :   s=notNullSetting {
							ps.notNull = new BooleanDebug($s.debugPoint);
							ps.notNullResolve = $s.toResolve;
							ps.notNullEvent = $s.event;
							ps.notNullResolveEvent = $s.resolveEvent;
						 }
    ;

notNullSetting returns [DebugInfo.DebugPoint debugPoint, BooleanDebug toResolve = null, Event event, Event resolveEvent]
@init {
    $debugPoint = getEventDebugPoint();
}
	:	'NONULL'
	    et=baseEventNotPE { $event = $et.event; }
	    (dt = notNullDeleteSetting { $toResolve = new BooleanDebug($dt.debugPoint); $resolveEvent = $dt.event; })?
	;


shortcutSetting [LA property, LocalizedString caption]
@after {
	if (inMainParseState()) {
		self.addToContextMenuFor(property, $c.val != null ? $c.val : caption, $usage.propUsage);
	}
}
	:	'ASON' 'CONTEXTMENU' (c=localizedStringLiteralNoID)? usage = actionOrPropertyUsage
	;

asonEventActionSetting [LA property]
@init {
	String eventActionSID = null;
}
@after {
	if (inMainParseState()) {
		self.setAsEventActionFor(property, $et.type, $et.before, $usage.propUsage);
	}
}
	:	'ASON' et=formEventType usage=actionOrPropertyUsage
	;

viewTypeSetting [LAP property]
@after {
	if (inMainParseState()) {
		self.setViewType(property, $viewType.type);
	}
}
	:	viewType=propertyClassViewType
	;

customViewSetting [LAP property]
@after {
	if (inMainParseState()) {
		self.setCustomRenderFunction(property, $customView.customRenderFunction);
		self.setCustomEditorFunction(property, $customView.customEditorFunction);
	}
}
	:	customView=propertyCustomView
	;

flexCharWidthSetting [LAP property]
@init {
	Boolean flex = null;
}
@after {
	if (inMainParseState()) {
		self.setFlexCharWidth(property, $width.val, flex);
	}
}
	:	'CHARWIDTH' width = intLiteral
	    (	('FLEX' { flex = true; })
        |	('NOFLEX' { flex = false; })
        )
	;

charWidthSetting [LAP property]
@after {
	if (inMainParseState()) {
		self.setCharWidth(property, $width.val);
	}
}
	:	'CHARWIDTH' width = intLiteral
	;

imageSetting [LAP property]
@after {
	if (inMainParseState()) {
		self.setImage(property, $img.image);
	}
}
	:   img=imageOption
	;

defaultCompareSetting [LAP property]
@after {
	if (inMainParseState()) {
		self.setDefaultCompare(property, $defaultCompare.val);
	}
}
	:	'DEFAULTCOMPARE' defaultCompare = stringLiteral
	;


changeKeySetting [LAP property]
@init {
	Boolean show = null;
}
@after {
	if (inMainParseState()) {
		self.setChangeKey(property, $key.val, show);
	}
}
	:	'CHANGEKEY' key = stringLiteral
		(	('SHOW' { show = true; })
		|	('HIDE' { show = false; })
		)?
	;

changeMouseSetting [LAP property]
@init {
	Boolean show = null;
}
@after {
	if (inMainParseState()) {
		self.setChangeMouse(property, $key.val, show);
	}
}
	:	'CHANGEMOUSE' key = stringLiteral
		(	('SHOW' { show = true; })
		|	('HIDE' { show = false; })
		)?
	;

autosetSetting [LP property]
@init {
	boolean autoset = false;
}
@after {
	if (inMainParseState()) {
		self.setAutoset(property, autoset);
	}
}
	:	'AUTOSET' { autoset = true; }
	;

confirmSetting [LAP property]
@init {
	boolean askConfirm = false;
}
@after {
	if (inMainParseState()) {
		self.setAskConfirm(property, askConfirm);
	}
}
	:	'CONFIRM' { askConfirm = true; }
	;

patternSetting [LAP property]
@after {
	if (inMainParseState()) {
		self.setPattern(property, $exp.val);
	}
}
	:	'PATTERN' exp = localizedStringLiteral
	;

regexpSetting [LAP property]
@after {
	if (inMainParseState()) {
		self.setRegexp(property, $exp.val, $mess.val);
	}
}
	:	'REGEXP' exp = localizedStringLiteral
		(mess = localizedStringLiteral)?
	;

echoSymbolsSetting [LAP property]
@after {
	if (inMainParseState()) {
		self.setEchoSymbols(property);
	}
}
	:	'ECHO'
	;

notNullDeleteSetting returns [DebugInfo.DebugPoint debugPoint, Event event]
@init {
    $debugPoint = getEventDebugPoint();
}
    :   'DELETE'
        et=baseEventNotPE { $event = $et.event; }
	;

onEditEventSetting [LAP property, List<TypedParameter> context]
@after {
	if (inMainParseState()) {
		self.setScriptedEventAction(property, $et.type, $et.before, $aDB.action);
	}
}
	:	'ON' et=formEventType
		aDB=listTopContextDependentActionDefinitionBody[context, false, false]
	;

formEventType returns [String type, Boolean before]
	:	'CHANGE' { $type = ServerResponse.CHANGE; } ('BEFORE' { $before = true; } | 'AFTER' { $before = false; })?
	|	'CHANGEWYS' { $type = ServerResponse.CHANGE_WYS; }
	|	'EDIT' { $type = ServerResponse.EDIT_OBJECT; }
	|	'GROUPCHANGE' { $type = ServerResponse.GROUP_CHANGE; }
	;

onContextMenuEventSetting [LAP property, List<TypedParameter> context]
@after {
	if (inMainParseState()) {
		self.setScriptedContextMenuAction(property, $c.val, $action.action);
	}
}
	:	'ON' 'CONTEXTMENU' (c=localizedStringLiteralNoID)?
		action=listTopContextDependentActionDefinitionBody[context, false, false]
	;

onKeyPressEventSetting [LAP property, List<TypedParameter> context]
@after {
	if (inMainParseState()) {
		self.setScriptedKeyPressAction(property, $key.val, $action.action);
	}
}
	: 'ON' 'KEYPRESS' key=stringLiteral action=listTopContextDependentActionDefinitionBody[context, false, false]
	;

eventIdSetting [LAP property]
@after {
	if (inMainParseState()) {
		self.setEventId(property, $id.val);
	}
}
	:	'EVENTID' id=stringLiteral
	;

stickySetting [LAP property]
@init {
	boolean sticky = false;
}
@after {
	if (inMainParseState()) {
		self.setSticky(property, sticky);
	}
}
    :
        st = stickyOption { sticky = $st.sticky; }
    ;

stickyOption returns[boolean sticky = false]
	:	'STICKY' { sticky = true; } | 'NOSTICKY' { sticky = false; }
	;

syncSetting [LAP property]
@init {
	Boolean sync = null;
}
@after {
	if (inMainParseState()) {
		self.setSync(property, sync);
	}
}
    :
        s = syncTypeLiteral { sync = $s.val; }
    ;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// ACTION PROPERTIES ///////////////////////////
////////////////////////////////////////////////////////////////////////////////

// "multiple inheritance" of topContextDependentActionDefinitionBody
listTopContextDependentActionDefinitionBody[List<TypedParameter> context, boolean dynamic, boolean needFullContext] returns [LAWithParams action]
@init {
	DebugInfo.DebugPoint point = getCurrentDebugPoint();
}
@after {
    if (inMainParseState()) {
        $action = self.modifyContextFlowActionDefinitionBodyCreated($action, context, new ArrayList<TypedParameter>(), needFullContext);

		DebugInfo.DebugPoint endPoint = getCurrentDebugPoint(true);
		self.actionDefinitionBodyCreated($action, point, endPoint, true, null);

        self.topContextActionDefinitionBodyCreated($action);
    }
}
    :   aDB=listActionDefinitionBody[context, dynamic] { if(inMainParseState()) { $action = $aDB.action; } }
	;

endDeclTopContextDependentActionDefinitionBody[List<TypedParameter> context, boolean dynamic, boolean needFullContext] returns [LAWithParams action]
    :   aDB=topContextDependentActionDefinitionBody[context, dynamic, needFullContext] { $action = $aDB.action; }
	;

// top level, not recursive
topContextDependentActionDefinitionBody[List<TypedParameter> context, boolean dynamic, boolean needFullContext] returns [LAWithParams action]
@after{
    if (inMainParseState()) {
        self.topContextActionDefinitionBodyCreated($action);
    }
}
    :   aDB=modifyContextFlowActionDefinitionBody[new ArrayList<TypedParameter>(), context, dynamic, needFullContext, false] { $action = $aDB.action; }
	;

// modifies context + is flow action (uses another actions)
modifyContextFlowActionDefinitionBody[List<TypedParameter> oldContext, List<TypedParameter> newContext, boolean dynamic, boolean needFullContext, boolean explicitCreated] returns [LAWithParams action]
@after{
    if (inMainParseState() && !explicitCreated) {
        $action = self.modifyContextFlowActionDefinitionBodyCreated($action, newContext, $oldContext, needFullContext);
    }
}
    :	aDB=actionDefinitionBody[newContext, dynamic, true] { $action = $aDB.action; }
	;

keepContextFlowActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
    :	aDB=actionDefinitionBody[context, dynamic, false] { $action = $aDB.action; }
	;

actionDefinitionBody[List<TypedParameter> context, boolean dynamic, boolean modifyContext] returns [LAWithParams action]
@init {
	DebugInfo.DebugPoint point = getCurrentDebugPoint();
}
@after{
	if (inMainParseState()) {
		DebugInfo.DebugPoint endPoint = getCurrentDebugPoint(true);
		self.actionDefinitionBodyCreated($action, point, endPoint, modifyContext, null);
	}
}
	:	(   recDB=recursiveContextActionDB[context, dynamic]	{ $action = $recDB.action; }
	    |	leafDB=leafContextActionDB[context, dynamic]	{ $action = $leafDB.action; }
	    )
	;

// recursive or mixed (in mixed rule there can be semi, but not necessary)
recursiveContextActionDB[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
	:	(   extDB=recursiveExtendContextActionDB[context, dynamic]	{ $action = $extDB.action; }
	    |	keepDB=recursiveKeepContextActionDB[context, dynamic]	{ $action = $keepDB.action; }
	    )
;

recursiveExtendContextActionDB[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
	if (inMainParseState() && dynamic) {
		self.getErrLog().emitExtendActionContextError(self.getParser());
	}
}
	:	forADB=forActionDefinitionBody[context] { $action = $forADB.action; }
	|	dialogADB=dialogActionDefinitionBody[context] { $action = $dialogADB.action; } // mixed, input
	|	inputADB=inputActionDefinitionBody[context] { $action = $inputADB.action; } // mixed, input
	|	newADB=newActionDefinitionBody[context] { $action = $newADB.action; }
	|	recalculateADB=recalculateActionDefinitionBody[context] { $action = $recalculateADB.action; }
	;

recursiveKeepContextActionDB[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
	:	listADB=listActionDefinitionBody[context, dynamic] { $action = $listADB.action; }
	|	confirmADB=confirmActionDefinitionBody[context] { $action = $confirmADB.action; } // mixed, input
	|	importADB=importActionDefinitionBody[context, dynamic] { $action = $importADB.action; } // mixed
	|	newSessionADB=newSessionActionDefinitionBody[context, dynamic] { $action = $newSessionADB.action; }
	|	requestADB=requestActionDefinitionBody[context, dynamic] { $action = $requestADB.action; }
	|	tryADB=tryActionDefinitionBody[context, dynamic] { $action = $tryADB.action; } // mixed
	|	ifADB=ifActionDefinitionBody[context, dynamic] { $action = $ifADB.action; }
	|	caseADB=caseActionDefinitionBody[context, dynamic] { $action = $caseADB.action; }
	|	multiADB=multiActionDefinitionBody[context, dynamic] { $action = $multiADB.action; }
	|	applyADB=applyActionDefinitionBody[context, dynamic] { $action = $applyADB.action; }
    |   newThreadADB=newThreadActionDefinitionBody[context, dynamic] { $action = $newThreadADB.action; } // mixed
	|	newExecutorADB=newExecutorActionDefinitionBody[context, dynamic] { $action = $newExecutorADB.action; } // mixed, recursive but always semi
;

// always semi in the end
leafContextActionDB[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
	:	(   extDB=leafExtendContextActionDB[context, dynamic]	{ $action = $extDB.action; }
	    |	keepDB=leafKeepContextActionDB[context, dynamic]	{ $action = $keepDB.action; }
	    ) ';'
;

leafExtendContextActionDB[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
    boolean isKeepContext = false; // hack for execActionDefinitionBody
}
@after {
	if (inMainParseState() && dynamic && !isKeepContext) {
		self.getErrLog().emitExtendActionContextError(self.getParser());
	}
}
	// actually exec is keepContextActionDB but to make grammar LL* we need to combine it with change (do a left-factoring) 
	:	setADB=changeOrExecActionDefinitionBody[context, dynamic] { $action = $setADB.action; isKeepContext = $setADB.isKeepContext; }
	|	classADB=changeClassActionDefinitionBody[context] { $action = $classADB.action; }
	|	delADB=deleteActionDefinitionBody[context] { $action = $delADB.action; }
	|	addADB=newWhereActionDefinitionBody[context] { $action = $addADB.action; }
	;

leafKeepContextActionDB[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
	:	termADB=terminalFlowActionDefinitionBody { $action = $termADB.action; }
	|  	cancelPDB=cancelActionDefinitionBody[context, dynamic] { $action = $cancelPDB.action; }
	|	formADB=formActionDefinitionBody[context, dynamic] { $action = $formADB.action; }
	|	printADB=printActionDefinitionBody[context, dynamic] { $action = $printADB.action; }
	|	exportFormADB=exportFormActionDefinitionBody[context, dynamic] { $action = $exportFormADB.action; }
	|	exportADB=exportActionDefinitionBody[context, dynamic] { $action = $exportADB.action; }
	|	msgADB=messageActionDefinitionBody[context, dynamic] { $action = $msgADB.action; }
	|	asyncADB=asyncUpdateActionDefinitionBody[context, dynamic] { $action = $asyncADB.action; }
	|	seekADB=seekObjectActionDefinitionBody[context, dynamic] { $action = $seekADB.action; }
	|	expandADB=expandGroupObjectActionDefinitionBody[context, dynamic] { $action = $expandADB.action; }
	|	collapseADB=collapseGroupObjectActionDefinitionBody[context, dynamic] { $action = $collapseADB.action; }
	|   orderADB=orderActionDefinitionBody[context, dynamic] { $action = $orderADB.action; }
	|   readOrdersADB=readOrdersActionDefinitionBody[context, dynamic] { $action = $readOrdersADB.action; }
	|   filterADB=filterActionDefinitionBody[context, dynamic] { $action = $filterADB.action; }
	|   readFiltersADB=readFiltersActionDefinitionBody[context, dynamic] { $action = $readFiltersADB.action; }
	|   filterGroupADB=filterGroupActionDefinitionBody[context, dynamic] { $action = $filterGroupADB.action; }
	|   readFilterGroupsADB=readFilterGroupsActionDefinitionBody[context, dynamic] { $action = $readFilterGroupsADB.action; }
    |   filterPropertyADB=filterPropertyActionDefinitionBody[context, dynamic] { $action = $filterPropertyADB.action; }
	|   readFiltersPropertyADB=readFiltersPropertyActionDefinitionBody[context, dynamic] { $action = $readFiltersPropertyADB.action; }
	|	mailADB=emailActionDefinitionBody[context, dynamic] { $action = $mailADB.action; }
	|	evalADB=evalActionDefinitionBody[context, dynamic] { $action = $evalADB.action; }
	|	readADB=readActionDefinitionBody[context, dynamic] { $action = $readADB.action; }
	|	writeADB=writeActionDefinitionBody[context, dynamic] { $action = $writeADB.action; }
	|	importFormADB=importFormActionDefinitionBody[context, dynamic] { $action = $importFormADB.action; }
	|	activeFormADB=activeFormActionDefinitionBody[context, dynamic] { $action = $activeFormADB.action; }
	|	activateADB=activateActionDefinitionBody[context, dynamic] { $action = $activateADB.action; }
	|	closeFormADB=closeFormActionDefinitionBody[context, dynamic] { $action = $closeFormADB.action; }
	|	expandCollapseADB=expandCollapseActionDefinitionBody[context, dynamic] { $action = $expandCollapseADB.action; }
    |   internalADB=internalContextActionDefinitionBody[context, dynamic] { $action = $internalADB.action;}
    |   externalADB=externalActionDefinitionBody[context, dynamic] { $action = $externalADB.action;}
    |   showRecDepADB=showRecDepActionDefinitionBody[context, dynamic] { $action = $showRecDepADB.action;}
	|	emptyADB=emptyActionDefinitionBody[context, dynamic] { $action = $emptyADB.action; }
	;

contextIndependentActionDB[List<TypedParameter> context] returns [LA action, List<ResolveClassSet> signature]
@init {
	DebugInfo.DebugPoint point = getCurrentDebugPoint();
	Boolean needToCreateDelegate = null;
}
@after{
	if (inMainParseState()) {
	    LAWithParams laWithParams = new LAWithParams($action, new ArrayList<Integer>());
		DebugInfo.DebugPoint endPoint = getCurrentDebugPoint(true);
		self.actionDefinitionBodyCreated(laWithParams, point, endPoint, false, needToCreateDelegate);

        self.topContextActionDefinitionBodyCreated(laWithParams);
	}
}
	:	internalADB=internalActionDefinitionBody[context] { $action = $internalADB.action; $signature = $internalADB.signature; }
    |	abstractActionDef=abstractActionDefinition[context] { $action = $abstractActionDef.action; $signature = $abstractActionDef.signature; needToCreateDelegate = false; } // to debug into implementation immediately, without stepping on abstract declaration
	;

mappedForm[List<TypedParameter> context, List<TypedParameter> newContext, boolean dynamic] returns [MappedForm mapped, List<FormActionProps> props = new ArrayList<>(), FormEntity form]
@init {

    CustomClass mappedCls = null;
    boolean edit = false;
}
	:
	(
		(	formName=compoundID { if(inMainParseState()) { $form = self.findForm($formName.sid); } }
			('OBJECTS' list=formActionObjectList[$form, context, newContext, dynamic] { $props = $list.props; })?
			{
				if(inMainParseState())
					$mapped = MappedForm.create($form, $list.objects != null ? $list.objects : new ArrayList<ObjectEntity>());
			}
		)
	    |
	    (	('LIST' | ('EDIT' { edit = true; } ))
			cls = classId { if(inMainParseState()) { mappedCls = (CustomClass)self.findClass($cls.sid); } }
			(object=formActionProps["object", mappedCls, context, newContext, dynamic] { $props = Collections.singletonList($object.props); })
			{
				if(inMainParseState())
					$mapped = MappedForm.create(mappedCls, edit);
			}
		)
	)
;


emptyActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@after {
    if (inMainParseState()) {
        $action = new LAWithParams(self.baseLM.getEmpty(), new ArrayList<Integer>());
    }
}
    :
    ;

formActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {

	Boolean syncType = null;
	WindowFormType windowType = null;

    List<TypedParameter> objectsContext = null;
    List<LPWithParams> contextFilters = new ArrayList<>();

    ManageSessionType manageSession = ManageSessionType.AUTO;
	Boolean noCancel = FormEntity.DEFAULT_NOCANCEL; // temporary, should be NULL
	FormSessionScope formSessionScope = FormSessionScope.OLDSESSION;

	boolean readOnly = false;
	boolean checkOnOk = false;

	String formId = null;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedShowFAProp($mf.mapped, $mf.props, syncType, windowType, manageSession, formSessionScope, checkOnOk, noCancel, readOnly,
		                                     objectsContext, contextFilters, context, formId);
	}
}
	:	'SHOW' (formIdVal = stringLiteral { formId = $formIdVal.val; } '=' )? mf=mappedForm[context, null, dynamic]
	    {
	        if(inMainParseState())
                objectsContext = self.getTypedObjectsNames($mf.mapped);
	    }
		(
		    cf = contextFiltersClause[context, objectsContext] { contextFilters.addAll($cf.contextFilters); }
		|   sync = syncTypeLiteral { syncType = $sync.val; }
		|   window = windowTypeLiteral { windowType = $window.val; }

        |	ms=manageSessionClause { manageSession = $ms.result; }
		|	nc=noCancelClause { noCancel = $nc.result; }
		|	fs=formSessionScopeClause { formSessionScope = $fs.result; }

		|	'READONLY' { readOnly = true; }
		|	'CHECK' { checkOnOk = true; }
		)*
	;

dialogActionDefinitionBody[List<TypedParameter> context] returns [LAWithParams action]
@init {
	WindowFormType windowType = null;

	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context);
	
	List<TypedParameter> objectsContext = null;
	List<LPWithParams> contextFilters = new ArrayList<>();

	ManageSessionType manageSession = ManageSessionType.AUTO;
	Boolean noCancel = FormEntity.DEFAULT_NOCANCEL; // temporary, should be NULL
	FormSessionScope formSessionScope = FormSessionScope.OLDSESSION;

	boolean checkOnOk = false;

	boolean readOnly = false;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedDialogFAProp($mf.mapped, $mf.props, windowType, manageSession, formSessionScope, checkOnOk, noCancel, readOnly, $dDB.action, $dDB.elseAction, objectsContext, contextFilters, context, newContext);
	}
}
	:	'DIALOG' mf=mappedForm[context, newContext, false]
	    {
            if(inMainParseState())
        	    objectsContext = self.getTypedObjectsNames($mf.mapped); 
        }
		(   cf = contextFiltersClause[context, objectsContext] { contextFilters.addAll($cf.contextFilters); }
		|   window = windowTypeLiteral { windowType = $window.val; }
		|	ms=manageSessionClause { manageSession = $ms.result; }
		|	nc=noCancelClause { noCancel = $nc.result; }
		|	fs=formSessionScopeClause { formSessionScope = $fs.result; }

		|	'READONLY' { readOnly = true; }
		|	'CHECK' { checkOnOk = true; }
		)*
		dDB=doInputBody[context, newContext]
	;

manageSessionClause returns [ManageSessionType result]
    :	'MANAGESESSION' { $result = ManageSessionType.MANAGESESSION; }
	|	'NOMANAGESESSION' { $result = ManageSessionType.NOMANAGESESSION; }
    ;

formSessionScopeClause returns [FormSessionScope result]
    :	'NEWSESSION' { $result = FormSessionScope.NEWSESSION; }
	|	'NESTEDSESSION' { $result = FormSessionScope.NESTEDSESSION; }
	|   'THISSESSION' { $result = FormSessionScope.OLDSESSION; }
    ;

noCancelClause returns [boolean result]
    :	'CANCEL' { $result = false; }
	|	'NOCANCEL' { $result = true; }
    ;

doInputBody[List<TypedParameter> oldContext, List<TypedParameter> newContext] returns [LAWithParams action, LAWithParams elseAction]
        // used explicit modifyContextFlowActionDefinitionBodyCreated to support CHANGE clauses inside extendDoParams, but need modifyContext flag in actionDefinitionBody to get right DelegationType
    :	(('DO' dDB=modifyContextFlowActionDefinitionBody[oldContext, newContext, false, false, true] { $action = $dDB.action; } ) ('ELSE' eDB=keepContextFlowActionDefinitionBody[newContext, false] { $elseAction = $eDB.action; } )?)
	|	';'
;

syncTypeLiteral returns [boolean val]
	:	'WAIT' { $val = true; }
	|	'NOWAIT' { $val = false; }
	;

windowTypeLiteral returns [WindowFormType val]
	:	'FLOAT' { $val = ModalityWindowFormType.FLOAT; }
	|	'DOCKED' { $val = ModalityWindowFormType.DOCKED; }
	|	'EMBEDDED' { $val = ModalityWindowFormType.EMBEDDED; }
	|	'POPUP' { $val = ModalityWindowFormType.POPUP; }
	|   'IN' fc = formComponentID {
	        if(inMainParseState()) {
                self.getChecks().checkComponentIsContainer($fc.component);
                $val = new ContainerWindowFormType($fc.component.getID());
	        }
	     }
	;

printActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
    List<TypedParameter> objectsContext = null;
    List<LPWithParams> contextFilters = new ArrayList<>();

	FormPrintType printType = null;
    Boolean syncType = null;
    MessageClientType messageType = MessageClientType.DEFAULT;
    LPWithParams printerProperty = null;
    LPWithParams sheetNameProperty = null;
    LPWithParams passwordProperty = null;
    boolean server = false;
    boolean autoPrint = false;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedPrintFAProp($mf.mapped, $mf.props, printType, server, autoPrint, $pUsage.propUsage, syncType, messageType, new SelectTop($selectTop.property, $selectTops.selectTops != null ? MapFact.fromJavaOrderMap($selectTops.selectTops) : null), printerProperty, sheetNameProperty, passwordProperty,
		                                      objectsContext, contextFilters, context);
	}
}
	:	'PRINT' ('CLIENT' | 'SERVER' { server = true; })?
	    mf=mappedForm[context, null, dynamic] {
            if(inMainParseState())
                 objectsContext = self.getTypedObjectsNames($mf.mapped);
        }
        (cf = contextFiltersClause[context, objectsContext] { contextFilters.addAll($cf.contextFilters); })?
        (
            ( // static - rest
                'MESSAGE' { printType = FormPrintType.MESSAGE; }
                (
                    sync = syncTypeLiteral { syncType = $sync.val; }
                |   mt = messageTypeLiteral { messageType = $mt.val; }
                )*
                ('TOP' ({ input.LA(1)==ID && input.LA(2)==EQ }? selectTops=groupObjectSelectTopMap[$mf.form, context, dynamic] | selectTop = propertyExpression[context, dynamic]))?
            )
            |
            ( // static - interactive
                { printType = FormPrintType.PRINT; }
                ( // static - jasper
                    type = printType [context, dynamic] { printType = $type.printType; sheetNameProperty = $type.sheetName; passwordProperty = $type.passwordProperty;}
                    ('TO' pUsage=propertyUsage)?
                )?
                ( 'PREVIEW' | 'NOPREVIEW' { autoPrint = true; } )?
                (sync = syncTypeLiteral { syncType = $sync.val; })?
                ('TO' pe = propertyExpression[context, dynamic] { printerProperty = $pe.property; })?
            )
        )
	;

printType [List<TypedParameter> context, boolean dynamic] returns [FormPrintType printType, LPWithParams sheetName, LPWithParams passwordProperty]
        :    'XLS'  { $printType = FormPrintType.XLS; } (sheet = sheetExpression[context, dynamic] { $sheetName = $sheet.sheetName; })? ('PASSWORD' pwd = propertyExpression[context, dynamic] { $passwordProperty = $pwd.property; })?
        |	'XLSX' { $printType = FormPrintType.XLSX; } (sheet = sheetExpression[context, dynamic] { $sheetName = $sheet.sheetName; })? ('PASSWORD' pwd = propertyExpression[context, dynamic] { $passwordProperty = $pwd.property; })?
        |	'PDF' { $printType = FormPrintType.PDF; }
        |	'DOC'  { $printType = FormPrintType.DOC; }
        |	'DOCX' { $printType = FormPrintType.DOCX; }
        |	'RTF' { $printType = FormPrintType.RTF; }
        |	'HTML' { $printType = FormPrintType.HTML; }
        ;

exportFormActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
    List<TypedParameter> objectsContext = null;
    List<LPWithParams> contextFilters = new ArrayList<>();

    FormIntegrationType format = null;
	String separator = null;
	Boolean hasHeader = null;
	boolean noEscape = false;
	String charset = null;
	boolean attr = false;
	LPWithParams sheetName = null;
	LPWithParams root = null;
	LPWithParams tag = null;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedExportFAProp($mf.mapped, $mf.props, format, sheetName, root, tag, attr, hasHeader, separator, noEscape,
		                                       new SelectTop($selectTop.property, $selectTops.selectTops != null ? MapFact.fromJavaOrderMap($selectTops.selectTops) : null),
		                                       charset, $pUsage.propUsage, $pUsages.pUsages,
		                                       objectsContext, contextFilters, context);
	}
}
	:	'EXPORT' mf=mappedForm[context, null, dynamic] {
	        if(inMainParseState())
                objectsContext = self.getTypedObjectsNames($mf.mapped);
	    }
	    (cf = contextFiltersClause[context, objectsContext] { contextFilters.addAll($cf.contextFilters); })?
		(type = exportSourceFormat [context, dynamic] { format = $type.format; separator = $type.separator; hasHeader = $type.hasHeader; noEscape = $type.noEscape;
        	                                                    charset = $type.charset; sheetName = $type.sheetName; root = $type.root; tag = $type.tag; attr = $type.attr; })?
		('TOP' ({ input.LA(1)==ID && input.LA(2)==EQ }? selectTops=groupObjectSelectTopMap[$mf.form, context, dynamic] | selectTop = propertyExpression[context, dynamic]))?
		('TO' (pUsages=groupObjectPropertyUsageMap[$mf.form] | pUsage=propertyUsage))?
	;

contextFiltersClause[List<TypedParameter> oldContext, List<TypedParameter> objectsContext] returns [List<LPWithParams> contextFilters = new ArrayList<>()]
@init {
    List<TypedParameter> context = new ArrayList<>();
}
    :   'FILTERS' {
            if(inMainParseState()) {
                context.addAll(oldContext);
                context.addAll(objectsContext);
            }
        }
        decl=propertyExpression[context, true] { contextFilters.add($decl.property); }
        (',' decl=propertyExpression[context, true] { contextFilters.add($decl.property); })*
    ;

exportSourceFormat [List<TypedParameter> context, boolean dynamic] returns [FormIntegrationType format, String separator, Boolean hasHeader, boolean noEscape, String charset, LPWithParams sheetName, LPWithParams root, LPWithParams tag, boolean attr]
	:	'CSV' { $format = FormIntegrationType.CSV; } (separatorVal = stringLiteral { $separator = $separatorVal.val; })? (hasHeaderVal = hasHeaderOption { $hasHeader = $hasHeaderVal.hasHeader; })? (noEscapeVal = noEscapeOption { $noEscape = $noEscapeVal.noEscape; })? ('CHARSET' charsetVal = stringLiteral { $charset = $charsetVal.val; })?
    |	'DBF' { $format = FormIntegrationType.DBF; } ('CHARSET' charsetVal = stringLiteral { $charset = $charsetVal.val; })?
    |   'XLS' { $format = FormIntegrationType.XLS; } (sheet = sheetExpression[context, dynamic] { $sheetName = $sheet.sheetName; })? (hasHeaderVal = hasHeaderOption { $hasHeader = $hasHeaderVal.hasHeader; })?
    |   'XLSX' { $format = FormIntegrationType.XLSX; } (sheet = sheetExpression[context, dynamic] { $sheetName = $sheet.sheetName; })? (hasHeaderVal = hasHeaderOption { $hasHeader = $hasHeaderVal.hasHeader; })?
	|	'JSON' { $format = FormIntegrationType.JSON; } ('CHARSET' charsetVal = stringLiteral { $charset = $charsetVal.val; })?
	|	'XML' { $format = FormIntegrationType.XML; } (hasHeaderVal = hasHeaderOption { $hasHeader = $hasHeaderVal.hasHeader; })? ('ROOT' rootProperty = propertyExpression[context, dynamic] {$root = $rootProperty.property; })?
	                                                 ('TAG' tagProperty = propertyExpression[context, dynamic] {$tag = $tagProperty.property; })? ('ATTR' { $attr = true; })? ('CHARSET' charsetVal = stringLiteral { $charset = $charsetVal.val; })?
	|	'TABLE' { $format = FormIntegrationType.TABLE; }
	;

hasHeaderOption returns [boolean hasHeader]
    :	'HEADER' { $hasHeader = true; }
    |	'NOHEADER'{ $hasHeader = false; }
	;

noEscapeOption returns [boolean noEscape]
    :	'NOESCAPE' { $noEscape = true; }
    |	'ESCAPE'{ $noEscape = false; }
	;

sheetExpression[List<TypedParameter> context, boolean dynamic] returns [LPWithParams sheetName]
        :   'SHEET' name = propertyExpression[context, dynamic] { $sheetName = $name.property; }
        ;

groupObjectSelectTopMap[FormEntity formEntity, List<TypedParameter> context, boolean dynamic] returns [OrderedMap<GroupObjectEntity, LPWithParams> selectTops]
@init {
	$selectTops = new OrderedMap<>();
	GroupObjectEntity go = null;
}
	:	firstGroupObject=ID { if(inMainParseState()) { go=self.findGroupObjectEntity(formEntity, $firstGroupObject.text); } }  EQ firstSelectTop=propertyExpression[context, dynamic] { if(inMainParseState()) { $selectTops.put(go, $firstSelectTop.property); } }
		(',' nextGroupObject=ID { if(inMainParseState()) { go=self.findGroupObjectEntity(formEntity, $nextGroupObject.text); } } EQ nextSelectTop=propertyExpression[context, dynamic] { if(inMainParseState()) { $selectTops.put(go, $nextSelectTop.property); } } )*
	;

groupObjectPropertyUsageMap[FormEntity formEntity] returns [OrderedMap<GroupObjectEntity, NamedPropertyUsage> pUsages]
@init {
	$pUsages = new OrderedMap<>();
	GroupObjectEntity go = null;
}
	:	firstGroupObject=ID { if(inMainParseState()) { go=self.findGroupObjectEntity(formEntity, $firstGroupObject.text); } }  EQ firstPropertyUsage=propertyUsage { if(inMainParseState()) { $pUsages.put(go, $firstPropertyUsage.propUsage); } }
		(',' nextGroupObject=ID { if(inMainParseState()) { go=self.findGroupObjectEntity(formEntity, $nextGroupObject.text); } } EQ nextPropertyUsage = propertyUsage { if(inMainParseState()) { $pUsages.put(go, $nextPropertyUsage.propUsage); } } )*
	;

formActionObjectList[FormEntity formEntity, List<TypedParameter> context, List<TypedParameter> newContext, boolean dynamic] returns [List<ObjectEntity> objects = new ArrayList<>(), List<FormActionProps> props = new ArrayList<>() ]
@init {
    ObjectEntity object = null;
}
	:	id=ID { if(inMainParseState()) { object=self.findObjectEntity(formEntity, $id.text); $objects.add(object); } } fap=formActionProps[$id.text, object != null ? object.baseClass : null, context, newContext, dynamic] { $props.add($fap.props); }
		(',' id=ID { if(inMainParseState()) { object=self.findObjectEntity(formEntity, $id.text); $objects.add(object); } } fap=formActionProps[$id.text, object != null ? object.baseClass : null, context, newContext, dynamic] { $props.add($fap.props); })*
	;

formActionProps[String objectName, ValueClass objectClass, List<TypedParameter> context, List<TypedParameter> newContext, boolean dynamic] returns [FormActionProps props]
@init {
    LPWithParams in = null;
    Boolean inNull = false;
    boolean out = false;
    Integer outParamNum = null;
    Boolean outNull = false;
    NamedPropertyUsage outProp = null;

    LPWithParams listProp = null;

    LPWithParams changeProp = null;

    boolean assign = false;
    boolean constraintFilter = false;

    DebugInfo.DebugPoint assignDebugPoint = null;
}
@after {
    $props = new FormActionProps(in, inNull, out, outParamNum, outNull, outProp, constraintFilter, assign, listProp, changeProp, assignDebugPoint);
}
    :   (EQ expr=propertyExpression[context, dynamic] { in = $expr.property; } ('NULL' { inNull = true; } )? )?
        (
            (   'INPUT'
                |
                (
                { assignDebugPoint = getCurrentDebugPoint(); }
                'CHANGE' { assign = true; outNull = true; constraintFilter = true; }
                (EQ consExpr=propertyExpression[context, dynamic])? { changeProp = $consExpr.property; }
                ('NOCONSTRAINTFILTER' { constraintFilter = false; } )?
                ('NOCHANGE' { assign = false; assignDebugPoint = null; } )?
                )
            )
            { out = true; inNull = true; }
            varID=ID?
            { if(newContext!=null && inMainParseState()) { outParamNum = self.getParamIndex(self.new TypedParameter(objectClass, $varID.text != null ? $varID.text : objectName), newContext, true, insideRecursion); } }
            ('NULL' { outNull = true; })? 
            ('TO' pUsage=propertyUsage { outProp = $pUsage.propUsage; } )?
            (('CONSTRAINTFILTER' { constraintFilter = true; } ) (EQ consExpr=propertyExpression[context, dynamic] { changeProp = $consExpr.property; } )?)?
            ('LIST' listExpr=propertyExpression[newContext, dynamic] { listProp = $listExpr.property; } )?
        )?
    ;

idEqualPEList[List<TypedParameter> context, boolean dynamic] returns [List<String> ids = new ArrayList<>(), List<LPWithParams> exprs = new ArrayList<>(), List<Boolean> nulls = new ArrayList<>()]
@init {
	boolean allowNulls = false;
}
	:	id=ID { $ids.add($id.text); } EQ expr=propertyExpression[context, dynamic] { $exprs.add($expr.property); } { allowNulls = false; } ('NULL' { allowNulls = true; })? { $nulls.add(allowNulls); }
		(',' id=ID { $ids.add($id.text); } EQ expr=propertyExpression[context, dynamic] { $exprs.add($expr.property); } { allowNulls = false; } ('NULL' { allowNulls = true; })? { $nulls.add(allowNulls); })*
	;

internalActionDefinitionBody[List<TypedParameter> context] returns [LA action, List<ResolveClassSet> signature]
@init {
	boolean allowNullValue = false;
	List<String> classes = null;
	boolean clientAction = false;
    boolean syncType = false;
}
@after {
	if (inMainParseState()) {
        ImList<ValueClass> paramClasses = self.findClasses(classes, context);
		$signature = classes == null ? self.getClassesFromTypedParams(context) : self.createClassSetsFromClassNames(classes);
        if(clientAction)
            $action = self.addScriptedInternalClientAction($classN.val, paramClasses, syncType);
        else if($code.val == null)
	        $action = self.addScriptedInternalAction($classN.val, paramClasses, allowNullValue);
	    else
		    $action = self.addScriptedInternalAction($code.val, allowNullValue);
        $signature = self.getParamClasses($action, paramClasses, false);
	}
}

	:	'INTERNAL'
	    ('CLIENT' { clientAction = true; } )?
        (sync = syncTypeLiteral { syncType = $sync.val; })?
        (
            classN = stringLiteral ('(' cls=classIdList ')' { classes = $cls.ids; })?
		|   code = codeLiteral
        )
	    ('NULL' { allowNullValue = true; })?
	;

internalContextActionDefinitionBody [List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
    InternalFormat format = null;
    List<LPWithParams> params = new ArrayList<>();
    List<NamedPropertyUsage> toList = new ArrayList<>();
    boolean syncType = false;
}
@after {
	if (inMainParseState()) {
      if(format == InternalFormat.DB) {
        $action = self.addScriptedInternalDBAction($execProp.property, params, context, toList);
      } else if(format == InternalFormat.CLIENT) {
        $action = self.addScriptedInternalClientAction($execProp.property, params, context, toList, syncType);
      }
	}
}
	:	'INTERNAL'
	    (
	        ( 'DB' { format = InternalFormat.DB; } )
	    |
	        ( 'CLIENT' { format = InternalFormat.CLIENT; } (sync = syncTypeLiteral { syncType = $sync.val; })? )
	    )
        execProp = propertyExpression[context, dynamic]
        ('PARAMS' exprList=propertyExpressionList[context, dynamic] { params = $exprList.props; } )?
        ('TO' tl = nonEmptyPropertyUsageList { toList = $tl.propUsages; } )?
	;

externalActionDefinitionBody [List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
    List<LPWithParams> params = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
      if($type.format == ExternalFormat.DB) {
        $action = self.addScriptedExternalDBAction($type.conStr, $type.exec, params, context, $tl.propUsages);
      } else if($type.format == ExternalFormat.DBF) {
        $action = self.addScriptedExternalDBFAction($type.conStr, $type.charset, params, context, $tl.propUsages);
      } else if($type.format == ExternalFormat.JAVA) {
        $action = self.addScriptedExternalJavaAction(params, context, $tl.propUsages);
      } else if($type.format == ExternalFormat.TCP) {
        $action = self.addScriptedExternalTCPAction($type.clientAction, $type.conStr, params, context);
      } else if($type.format == ExternalFormat.UDP) {
        $action = self.addScriptedExternalUDPAction($type.clientAction, $type.conStr, params, context);
      } else if($type.format == ExternalFormat.HTTP) {
        $action = self.addScriptedExternalHTTPAction($type.clientAction, $type.method, $type.conStr, $type.bodyUrl, $type.bodyParamNames, $type.bodyParamHeadersList,
            $type.headers, $type.cookies, $type.headersTo, $type.cookiesTo, params, context, $tl.propUsages);
      } else if($type.format == ExternalFormat.LSF) {
        $action = self.addScriptedExternalLSFAction($type.conStr, $type.exec, $type.eval, $type.action, params, context, $tl.propUsages);
      }
	}
}
	:	'EXTERNAL'
	    type = externalFormat[context, dynamic]
	    ('PARAMS' exprList=propertyExpressionList[context, dynamic] { params = $exprList.props; } )?
	    ('TO' tl = nonEmptyPropertyUsageList)?
	;

externalFormat [List<TypedParameter> context, boolean dynamic] returns [ExternalFormat format, ExternalHttpMethod method, boolean clientAction, LPWithParams conStr, LPWithParams bodyUrl, LPWithParams exec, List<LPWithParams> bodyParamNames = new ArrayList<>(), List<NamedPropertyUsage> bodyParamHeadersList = new ArrayList<>(), NamedPropertyUsage headers, NamedPropertyUsage cookies, NamedPropertyUsage headersTo, NamedPropertyUsage cookiesTo, boolean eval = false, boolean action = false, String charset]
	:	'SQL'	{ $format = ExternalFormat.DB; } conStrVal = propertyExpression[context, dynamic] { $conStr = $conStrVal.property; } 'EXEC' execVal = propertyExpression[context, dynamic] { $exec = $execVal.property; }
    |	'TCP'	{ $format = ExternalFormat.TCP; } ('CLIENT' { $clientAction = true; })?
                conStrVal = propertyExpression[context, dynamic] { $conStr = $conStrVal.property; }
	|	'UDP'	{ $format = ExternalFormat.UDP; } ('CLIENT' { $clientAction = true; })?
	            conStrVal = propertyExpression[context, dynamic] { $conStr = $conStrVal.property; }
	|	'HTTP'	{ $format = ExternalFormat.HTTP; } ('CLIENT' { $clientAction = true; })?
	            (methodVal = externalHttpMethod { $method = $methodVal.method; })? conStrVal = propertyExpression[context, dynamic] { $conStr = $conStrVal.property; }
	            ('BODYURL' bodyUrlVal = propertyExpression[context, dynamic] { $bodyUrl = $bodyUrlVal.property; })?
	            ('BODYPARAMNAMES' firstName=propertyExpression[context, dynamic] { $bodyParamNames.add($firstName.property); } (',' nextName=propertyExpression[context, dynamic] { $bodyParamNames.add($nextName.property); })*)?
                ('BODYPARAMHEADERS' firstProp = propertyUsage { $bodyParamHeadersList.add($firstProp.propUsage); } (',' nextProp = propertyUsage { $bodyParamHeadersList.add($nextProp.propUsage); })*)?
	            ('HEADERS' headersVal = propertyUsage { $headers = $headersVal.propUsage; })?
	            ('COOKIES' cookiesVal = propertyUsage { $cookies = $cookiesVal.propUsage; })?
	            ('HEADERSTO' headersToVal = propertyUsage { $headersTo = $headersToVal.propUsage; })?
	            ('COOKIESTO' cookiesToVal = propertyUsage { $cookiesTo = $cookiesToVal.propUsage; })?
	|	'DBF'	{ $format = ExternalFormat.DBF; } conStrVal = propertyExpression[context, dynamic] { $conStr = $conStrVal.property; } 'APPEND' ('CHARSET' charsetVal = stringLiteral { $charset = $charsetVal.val; })?
	|	'LSF'	{ $format = ExternalFormat.LSF; } conStrVal = propertyExpression[context, dynamic] { $conStr = $conStrVal.property; } ('EXEC' | ('EVAL' { $eval = true; } ('ACTION' { $action = true; })? )) execVal = propertyExpression[context, dynamic] { $exec = $execVal.property; }
	|   'JAVA' 	{ $format = ExternalFormat.JAVA; } conStrVal = propertyExpression[context, dynamic] { $conStr = $conStrVal.property; }
	;

externalHttpMethod returns [ExternalHttpMethod method]
	:	'DELETE' { $method = ExternalHttpMethod.DELETE; }
	|	'GET'    { $method = ExternalHttpMethod.GET; }
	|	'PATCH'    { $method = ExternalHttpMethod.PATCH; }
	|	'POST'	 { $method = ExternalHttpMethod.POST; }
	|	'PUT'    { $method = ExternalHttpMethod.PUT; }
	;

newWhereActionDefinitionBody[List<TypedParameter> context] returns [LAWithParams action]
@init {
	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context);
	LPWithParams condition = null;
	NamedPropertyUsage toPropUsage = null;
	List<LPWithParams> toPropMapping = null;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedAddObjProp(context, $cid.sid, toPropUsage, toPropMapping, condition, newContext);
	}
}
	:	'NEW' cid=classId
		'WHERE' pe=propertyExpression[newContext, true] { condition = $pe.property; }
		('TO' toProp=propertyUsage '(' params=singleParameterList[newContext, false] ')' { toPropUsage = $toProp.propUsage; toPropMapping = $params.props; } )?
	;

newActionDefinitionBody[List<TypedParameter> context] returns [LAWithParams action]
@init {
	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context);

	String varName = "added";
}
@after {
	if (inMainParseState()) {
        $action = self.addScriptedNewAProp(context, $actDB.action, $addObj.paramCnt, $addObj.className, $addObj.autoset);
	}
}
	:
	    addObj=forAddObjClause[newContext]
//        ('TO' pUsage=propertyUsage)?
   		actDB=modifyContextFlowActionDefinitionBody[context, newContext, false, false, false]
	;

emailActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
    Boolean syncType = null;
	LPWithParams fromProp = null;
	LPWithParams subjProp = null;
	LPWithParams bodyProp = null;

	List<Message.RecipientType> recipTypes = new ArrayList<>();
	List<LPWithParams> recipProps = new ArrayList<>();

	List<LPWithParams> attachFileNames = new ArrayList<>();
	List<LPWithParams> attachFiles = new ArrayList<>();

    List<NamedPropertyUsage> attachFileNameProps = new ArrayList<>();
    List<NamedPropertyUsage> attachFileProps = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedEmailProp(fromProp, subjProp, bodyProp, recipTypes, recipProps, attachFileNames, attachFiles,
		                                    attachFileNameProps, attachFileProps, syncType);
	}
}
	:	'EMAIL'
		('FROM' fromExpr=propertyExpression[context, dynamic] { fromProp = $fromExpr.property; } )?
		('SUBJECT' subjExpr=propertyExpression[context, dynamic] { subjProp = $subjExpr.property; })?
		(
			recipType=emailRecipientTypeLiteral { recipTypes.add($recipType.val); }
			recipExpr=propertyExpression[context, dynamic] { recipProps.add($recipExpr.property); }
		)+
		('BODY' bodyExpr=propertyExpression[context, dynamic] { bodyProp = $bodyExpr.property; })?
		(   'ATTACH'
		    (
                (attachFile=propertyExpression[context, dynamic] { attachFiles.add($attachFile.property); }
                { LPWithParams attachFileName = null;}
                ('NAME' attachFileNameExpr=propertyExpression[context, dynamic] { attachFileName = $attachFileNameExpr.property; } )?
                { attachFileNames.add(attachFileName); })
            |
                ('LIST'
                attachFileProp = propertyUsage { attachFileProps.add($attachFileProp.propUsage); }
                { NamedPropertyUsage attachFileNamesProp = null; }
                ('NAME' attachFileNameProp = propertyUsage { attachFileNameProps.add($attachFileNameProp.propUsage); })?)
            )
		)*
		(sync = syncTypeLiteral{ syncType = $sync.val; })?
	;

confirmActionDefinitionBody[List<TypedParameter> context] returns [LAWithParams action]
@init {
    List<TypedParameter> newContext;
    boolean yesNo = false;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedConfirmProp($mpe.property, $hpe.property, $dDB.action, $dDB.elseAction, yesNo, context, newContext);
	}
}
	:	'ASK'
        mpe=propertyExpression[context, false]
        ('HEADER' hpe=propertyExpression[context, false])?
        { newContext = new ArrayList<TypedParameter>(context); }
	    ((varID=ID { if (inMainParseState()) { self.getParamIndex(self.new TypedParameter("BOOLEAN", $varID.text), newContext, true, insideRecursion); } } EQ)? 'YESNO' { yesNo = true;} )?
        dDB=doInputBody[context, newContext]
	;
		
messageActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
    boolean syncType = true;
    MessageClientType messageType = MessageClientType.DEFAULT;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedMessageProp($mpe.property, $hpe.property, !syncType, messageType);
	}
}
	:	'MESSAGE'
	    mpe=propertyExpression[context, dynamic]
	    ('HEADER' hpe=propertyExpression[context, dynamic])?
	    (
	        sync = syncTypeLiteral { syncType = $sync.val; }
	    |   mt = messageTypeLiteral { messageType = $mt.val; }
        )*
	;

messageTypeLiteral returns [MessageClientType val]
	:	'LOG' { $val = MessageClientType.LOG; }
	|	'INFO' { $val = MessageClientType.INFO; }
	|   'SUCCESS' { $val = MessageClientType.SUCCESS; }
	|	'WARN' { $val = MessageClientType.WARN; }
	|	'ERROR' { $val = MessageClientType.ERROR; }
	|	'DEFAULT' { $val = MessageClientType.DEFAULT; }
	;

asyncUpdateActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@after {
	if (inMainParseState()) {
		$action = self.addScriptedAsyncUpdateProp($pe.property);
	}
}
	:	'ASYNCUPDATE' pe=propertyExpression[context, dynamic]
	;

seekObjectActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
	UpdateType type = null;
	List<String> objNames = new ArrayList<>();
	List<LPWithParams> lps = new ArrayList<>(); 
}
@after {
	if (inMainParseState()) {
		$action = obj != null ? self.addScriptedObjectSeekProp($obj.sid, $pe.property, type)
		                      : self.addScriptedGroupObjectSeekProp($gobj.sid, objNames, lps, type);
	}
}
	:	'SEEK' ('FIRST' { type = UpdateType.FIRST; } | 'LAST' { type = UpdateType.LAST; } | 'NULL' { type = UpdateType.NULL; })?
		(	obj=formObjectID EQ pe=propertyExpression[context, dynamic]
		|	gobj=formGroupObjectID ('OBJECTS' list=seekObjectsList[context, dynamic] { objNames = $list.objects; lps = $list.values; })?
		)
	;

seekObjectsList[List<TypedParameter> context, boolean dynamic] returns [List<String> objects, List<LPWithParams> values] 
	:	list=idEqualPEList[context, dynamic] { $objects = $list.ids; $values = $list.exprs; }
	;

expandGroupObjectActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
	ExpandCollapseType type = ExpandCollapseType.DOWN;
	List<String> objNames = new ArrayList<>();
	List<LPWithParams> lps = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedGroupObjectExpandProp($gobj.sid, objNames, lps, type, true);
	}
}
	:	'EXPAND' ('DOWN' { type = ExpandCollapseType.DOWN; } | 'UP' { type = ExpandCollapseType.UP; } | ('ALL' { type = ExpandCollapseType.ALL; } ('TOP' { type = ExpandCollapseType.ALLTOP; })?) )?
		gobj=formGroupObjectID ('OBJECTS' list=expandCollapseObjectsList[context, dynamic] { objNames = $list.objects; lps = $list.values; })?
	;

collapseGroupObjectActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
	ExpandCollapseType type = ExpandCollapseType.DOWN;
	List<String> objNames = new ArrayList<>();
	List<LPWithParams> lps = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedGroupObjectExpandProp($gobj.sid, objNames, lps, type, false);
	}
}
	:	'COLLAPSE' ('DOWN' { type = ExpandCollapseType.DOWN; } | ('ALL' { type = ExpandCollapseType.ALL; } ('TOP' { type = ExpandCollapseType.ALLTOP; })?) )?
		gobj=formGroupObjectID ('OBJECTS' list=expandCollapseObjectsList[context, dynamic] { objNames = $list.objects; lps = $list.values; })?
	;

expandCollapseObjectsList[List<TypedParameter> context, boolean dynamic] returns [List<String> objects, List<LPWithParams> values]
	:	list=idEqualPEList[context, dynamic] { $objects = $list.ids; $values = $list.exprs; }
	;

orderActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@after {
	if (inMainParseState()) {
		$action = self.addScriptedOrderProp($gobj.sid, $expr.property);
	}
}
    :   'ORDER'
        gobj=formGroupObjectID
        ('FROM' expr=propertyExpression[context, dynamic])?
    ;

readOrdersActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@after {
	if (inMainParseState()) {
		$action = self.addScriptedReadOrdersProp($gobj.sid, $pu.propUsage);
	}
}
    :   'ORDERS'
        gobj=formGroupObjectID
        ('TO' pu=propertyUsage)?
    ;

 filterActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
 @after {
 	if (inMainParseState()) {
 		$action = self.addScriptedFilterProp($gobj.sid, $expr.property);
 	}
 }
     :   'FILTER'
         gobj=formGroupObjectID
         ('FROM' expr=propertyExpression[context, dynamic])?
     ;

readFiltersActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@after {
	if (inMainParseState()) {
		$action = self.addScriptedReadFiltersProp($gobj.sid, $pu.propUsage);
	}
}
    :   'FILTERS'
        gobj=formGroupObjectID
        ('TO' pu=propertyUsage)?
    ;

 filterGroupActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
 @after {
 	if (inMainParseState()) {
 		$action = self.addScriptedFilterGroupProp($fg.sid, $expr.property);
 	}
 }

     :   'FILTERGROUP'
         fg=formFilterGroupID
         ('FROM' expr=propertyExpression[context, dynamic])?
     ;

readFilterGroupsActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@after {
	if (inMainParseState()) {
		$action = self.addScriptedReadFilterGroupsProp($fg.sid, $pu.propUsage);
	}
}
    :   'FILTERGROUPS'
        fg=formFilterGroupID
        ('TO' pu=propertyUsage)?
    ;

 filterPropertyActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
 @init {
     PropertyDrawEntity propertyDraw = null;
 }
 @after {
 	if (inMainParseState()) {
 		$action = self.addScriptedFilterPropertyProp(propertyDraw, $expr.property);
 	}
 }

     :   'FILTER' 'PROPERTY'
         fp=formPropertyID { propertyDraw = $fp.propertyDraw; }
         ('FROM' expr=propertyExpression[context, dynamic])?
     ;

readFiltersPropertyActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
 @init {
     PropertyDrawEntity propertyDraw = null;
 }
@after {
	if (inMainParseState()) {
		$action = self.addScriptedReadFiltersPropertyProp(propertyDraw, $pu.propUsage);
	}
}
    :   'FILTERS' 'PROPERTY'
        fp=formPropertyID { propertyDraw = $fp.propertyDraw; }
        ('TO' pu=propertyUsage)?
    ;


changeClassActionDefinitionBody[List<TypedParameter> context] returns [LAWithParams action]
@init {
	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context);
	LPWithParams condition = null;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedChangeClassAProp(context.size(), newContext, $param.property, $className.sid, condition);	
	}
}
	:	'CHANGECLASS' param=propertyExpression[newContext, true] 'TO' className=classId 
		('WHERE' pe=propertyExpression[newContext, false] { condition = $pe.property; })?
	;  

deleteActionDefinitionBody[List<TypedParameter> context] returns [LAWithParams action]
@init {
	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context);
	LPWithParams condition = null;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedDeleteAProp(context.size(), newContext, $param.property, condition);	
	}
}
	:	'DELETE' param=propertyExpression[newContext, true] 
		('WHERE' pe=propertyExpression[newContext, false] { condition = $pe.property; })?
	;  

evalActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
	boolean isAction = false;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedEvalAction($expr.property, $exprList.props, context, isAction);
	}
}
	:	'EVAL' ('ACTION' { isAction = true; })? expr=propertyExpression[context, dynamic] ('PARAMS' exprList=propertyExpressionList[context, dynamic])?
	;

requestActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@after {
	if (inMainParseState()) {
		$action = self.addScriptedRequestAProp($aDB.action, $dDB.action, $eDB.action);
	}
}
	:	'REQUEST' aDB=keepContextFlowActionDefinitionBody[context, dynamic] 'DO' dDB=keepContextFlowActionDefinitionBody[context, dynamic]
	    ('ELSE' eDB=keepContextFlowActionDefinitionBody[context, dynamic])?
	;

inputActionDefinitionBody[List<TypedParameter> context] returns [LAWithParams action]
@init {
	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context);
	boolean assign = false;
	boolean constraintFilter = false;
	DebugInfo.DebugPoint assignDebugPoint = null;

    NamedPropertyUsage outProp = null;
    LPWithParams changeProp = null;
    LAPWithParams listProp = null;
    LPWithParams whereProp = null;

    List<String> actionImages = new ArrayList<>();
    List<String> keyPresses = new ArrayList<>();
    List<List<QuickAccess>> quickAccesses = new ArrayList<>();
    List<LAWithParams> actions = new ArrayList<>();
    String customEditorFunction = null;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedInputAProp($in.valueClass, $in.initValue, outProp, $dDB.action, $dDB.elseAction, context, newContext,
		 assign, constraintFilter, changeProp, listProp, whereProp, actionImages, keyPresses, quickAccesses, actions, assignDebugPoint,
		 $fs.result, customEditorFunction);
	}
}
	:	'INPUT'
	    in=mappedInput[newContext]
        ( { assignDebugPoint = getCurrentDebugPoint(); }// copy paste of 'CHANGE' in formActionProps
            'CHANGE' { assign = true; constraintFilter = true; }
            (EQ consExpr=propertyExpression[context, false])? { changeProp = $consExpr.property; }
            ('NOCONSTRAINTFILTER' { constraintFilter = false; } )?
            ('NOCHANGE' { assign = false; assignDebugPoint = null; } )?
        )?
        {
            List<TypedParameter> newListContext;
            boolean listDynamic;
            if($in.valueClass instanceof DataClass) {
                newListContext = new ArrayList<TypedParameter>(context);
                listDynamic = true;
            } else {
                newListContext = newContext;
                listDynamic = false;
            }

            List<TypedParameter> newActionsContext = new ArrayList<TypedParameter>(newContext);
        }
        ('CUSTOM' editFun=stringLiteral {customEditorFunction = $editFun.val;})?
	    ('LIST'
	        (
	            listExpr=propertyExpression[newListContext, listDynamic] {
                    listProp = $listExpr.property;
                    if(!listDynamic && listProp != null) {
                        newActionsContext.set(newActionsContext.size() - 1, self.new TypedParameter($listExpr.property.getLP().property.getType().getSID(), newActionsContext.get(newContext.size() - 1).paramName));
                    }
                }
                |
                actDB=listActionDefinitionBody[newActionsContext, false] {
                    // assert listDynamic
                    listProp = $actDB.action;
                }
            )
        )?
        ('WHERE' whereExpr=propertyExpression[newListContext, listDynamic] { whereProp = $whereExpr.property; })?
        (acts = contextActions[newActionsContext] { actionImages = $acts.actionImages; keyPresses = $acts.keyPresses; quickAccesses = $acts.quickAccesses; actions = $acts.actions; })?
        fs=formSessionScopeClause?
		('TO' pUsage=propertyUsage { outProp = $pUsage.propUsage; } )?
        dDB=doInputBody[context, newContext]
	;

contextActions[List<TypedParameter> context] returns [List<String> actionImages = new ArrayList<>(), List<String> keyPresses = new ArrayList<>(), List<List<QuickAccess>> quickAccesses = new ArrayList<>(), List<LAWithParams> actions = new ArrayList<>()]
	:
	'ACTIONS' act = contextAction[context] { $actionImages.add($act.actionImage); $keyPresses.add($act.keyPress); $quickAccesses.add($act.quickAccess); $actions.add($act.action); }
	(',' nextAct = contextAction[context] { $actionImages.add($nextAct.actionImage); $keyPresses.add($nextAct.keyPress); $quickAccesses.add($nextAct.quickAccess); $actions.add($nextAct.action); })*
	;

contextAction[List<TypedParameter> context] returns [String actionImage, String keyPress = "", List<QuickAccess> quickAccess = new ArrayList<>(), LAWithParams action]
	:
	image=stringLiteral { $actionImage = $image.val; } ('KEYPRESS' kp=stringLiteral { $keyPress = $kp.val; })?
	          ('TOOLBAR' (quickAccess { $quickAccess.add(new QuickAccess($quickAccess.mode, $quickAccess.hover)); })*)? actDB=listActionDefinitionBody[context, false] { $action = $actDB.action; }
	;

quickAccess returns [QuickAccessMode mode, Boolean hover = false]
	:
	('ALL' { $mode = QuickAccessMode.ALL; } | 'SELECTED' { $mode = QuickAccessMode.SELECTED; } | 'FOCUSED' { $mode = QuickAccessMode.FOCUSED; }) ('HOVER' { $hover = true; })?
	;

mappedInput[List<TypedParameter> context] returns [ValueClass valueClass, LPWithParams initValue = null]
@init {
    String varName = "object"; // for INPUT =f() CHANGE and INPUT LONG;
}
@after {
	if (inMainParseState()) {
		$valueClass = self.getInputValueClass(varName, context, $pe.id != null ? $pe.id.name : $ptype.text, $pe.property, insideRecursion);
		if($pe.id == null)
    		$initValue = $pe.property;
	}
}
    :    
    (
        (varID=ID EQ { varName = $varID.text; } )?
        ptype=primitiveType
    )
    |	
    ( 
        (varID=ID { varName = $varID.text; } )?
        EQ pe=propertyExpressionOrCompoundID[context]
    )
;

activeFormActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@after {
	if (inMainParseState()) {
		$action = self.addScriptedActiveFormAProp($name.sid);
	}
}
	:	'ACTIVE' 'FORM' name=compoundID 
	;

activateActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
    FormEntity form = null;
    ComponentView component = null;
    PropertyDrawEntity propertyDraw = null;
}
@after {
	if (inMainParseState()) {
	    if(form != null)
		    $action = self.addScriptedActivateAProp(form, component);
        else
            $action = self.addScriptedFocusAction(propertyDraw);
	}
}
	:	'ACTIVATE'
		(	'FORM' fName=compoundID { if (inMainParseState()) { form = self.findForm($fName.sid); } }
		|	'TAB' fc = formComponentID { form = $fc.form; component = $fc.component; }
		|   'PROPERTY' fp = formPropertyID { propertyDraw = $fp.propertyDraw; }
		)
	;

closeFormActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
    String formId = null;
}
@after {
	if (inMainParseState()) {
        $action = self.addScriptedCloseFormAProp(formId);
	}
}
	:	'CLOSE' 'FORM' formIdVal = stringLiteral { formId = $formIdVal.val; }
	;

expandCollapseActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
    ComponentView component = null;
    boolean collapse = true;
}
@after {
	if (inMainParseState()) {
		 $action = self.addScriptedCollapseExpandAProp(component, collapse);
	}
}
	:	(	'COLLAPSE'
		|	'EXPAND' { collapse = false; }
		) 
		'CONTAINER'
		fc = formComponentID { component = $fc.component; }
	;

listActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
	List<LAWithParams> props = new ArrayList<>();
	List<LP> localProps = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedListAProp(props, localProps);
	}
}
	:	'{'
			(	(aDB=keepContextFlowActionDefinitionBody[context, dynamic] { props.add($aDB.action); })
			|	def=localDataPropertyDefinition ';' { if (inMainParseState()) localProps.addAll($def.properties); }
			)*
		'}'
	;

nestedPropertiesSelector returns[boolean all = false, List<NamedPropertyUsage> props = new ArrayList<>(), boolean classes = false]
    :   'NESTED'
            (   'LOCAL' { $all = true; }
            |   (
            	'(' list=nonEmptyPropertyUsageList { $props = $list.propUsages; } ')'
            	)
            )?
            ('CLASSES' { $classes = true; })?
    ;
	
localDataPropertyDefinition returns [List<LP<?>> properties]
@init {
	DebugInfo.DebugPoint point = getCurrentDebugPoint();
}
@after {
	if (inMainParseState()) {
		$properties = self.addLocalDataProperty($propNames.ids, $returnClass.sid, $paramClasses.ids, $nlm.nestedType, point);
	}
}
	:	'LOCAL'
		nlm = nestedLocalModifier
		propNames=nonEmptyIdList
		EQ returnClass=classId
		'('
			paramClasses=classIdList
		')'
	;

changeOrExecActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action, boolean isKeepContext = false]
@init {
	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context); 
	LPWithParams condition = null;
	boolean isChange = false;
}
@after {
	if (inMainParseState()) {
	    if(isChange)
    		$action = self.addScriptedChangePropertyAProp(context, $propUsage.propUsage, $params.props, $expr.property, condition, newContext);
        else {
            if(!dynamic)
                self.checkNoExtendContext(context.size(), newContext);
			$action = self.addScriptedJoinAProp($propUsage.propUsage, $params.props, context);
			$isKeepContext = true;
        }
	}
}
	:	('CHANGE' | 'EXEC')?
		propUsage=propertyUsage
		'(' params=propertyExpressionList[newContext, true] ')'
		('<-' { isChange = true; }
		expr=propertyExpression[newContext, false] //no need to use dynamic context, because params should be either on global context or used in the left side
		('WHERE'
		whereExpr=propertyExpression[newContext, false] { condition = $whereExpr.property; })?)?
	;

recalculateActionDefinitionBody[List<TypedParameter> context] returns [LAWithParams action]
@init {
	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context);
	LPWithParams condition = null;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedRecalculatePropertyAProp(context, $propUsage.propUsage, $params.props, condition, newContext);
	}
}
	:	'RECALCULATE'
		propUsage=propertyUsage
		'(' params=propertyExpressionList[newContext, true] ')'
		('WHERE'
		whereExpr=propertyExpression[newContext, false] { condition = $whereExpr.property; })?
	;

tryActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@after {
	if (inMainParseState()) {
		$action = self.addScriptedTryAProp($tryADB.action, $catchADB.action, $finallyADB.action);
	}
}
	:	'TRY' tryADB=keepContextFlowActionDefinitionBody[context, dynamic]
	    ( 'CATCH' catchADB=keepContextFlowActionDefinitionBody[context, dynamic] )?
		( 'FINALLY' finallyADB=keepContextFlowActionDefinitionBody[context, dynamic] )?
	;

ifActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@after {
	if (inMainParseState()) {
		$action = self.addScriptedIfAProp($expr.property, $thenADB.action, $elseADB.action);
	}
}
	:	'IF' expr=propertyExpression[context, dynamic] 
		'THEN' thenADB=keepContextFlowActionDefinitionBody[context, dynamic]
		('ELSE' elseADB=keepContextFlowActionDefinitionBody[context, dynamic])?
	;

caseActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action] 
@init {
	List<LPWithParams> whenProps = new ArrayList<>();
	List<LAWithParams> thenActions = new ArrayList<>();
	LAWithParams elseAction = null;
	boolean isExclusive = false;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedCaseAProp(whenProps, thenActions, elseAction, isExclusive); 
	}
}
	:	'CASE' (opt=exclusiveOverrideOption { isExclusive = $opt.isExclusive; })?
			( branch=actionCaseBranchBody[context, dynamic] { whenProps.add($branch.whenProperty); thenActions.add($branch.thenAction); } )+
			('ELSE' elseAct=keepContextFlowActionDefinitionBody[context, dynamic] { elseAction = $elseAct.action; })?
	;

actionCaseBranchBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams whenProperty, LAWithParams thenAction]
	:	'WHEN' whenExpr=propertyExpression[context, dynamic] { $whenProperty = $whenExpr.property; }
		'THEN' thenAct=keepContextFlowActionDefinitionBody[context, dynamic] { $thenAction = $thenAct.action; }
	;

applyActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
	boolean single = false;
	List<NamedPropertyUsage> keepSessionProps = Collections.emptyList();
	boolean keepAllSessionProps = false;
	boolean serializable = false;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedApplyAProp($applyADB.action, single, keepSessionProps, keepAllSessionProps, serializable);
	}
}
	:	'APPLY' 
        (mps=nestedPropertiesSelector { keepAllSessionProps = $mps.all; keepSessionProps = $mps.props; })?
        ('SINGLE' { single = true; })?
        ('SERIALIZABLE' { serializable = true; })?
        applyADB=keepContextFlowActionDefinitionBody[context, dynamic]
	;

cancelActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
	List<NamedPropertyUsage> keepSessionProps = Collections.emptyList();
	boolean keepAllSessionProps = false;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedCancelAProp(keepSessionProps, keepAllSessionProps);
	}
}
	:	'CANCEL'
        (mps=nestedPropertiesSelector { keepAllSessionProps = $mps.all; keepSessionProps = $mps.props; })?
	;

multiActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action] 
@init {
	boolean isExclusive = true;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedMultiAProp($actList.actions, isExclusive); 
	}
}
	:	'MULTI' (opt=exclusiveOverrideOption { isExclusive = $opt.isExclusive; })?
		actList=nonEmptyActionPDBList[context, dynamic]
	;

forAddObjClause[List<TypedParameter> context] returns [Integer paramCnt, String className, Boolean autoset = false]
@init {
	String varName = "added";
}
@after {
	if (inMainParseState()) {
		$paramCnt = self.getParamIndex(self.new TypedParameter($className, varName), context, true, insideRecursion);
	}
}
	:	'NEW'
		(varID=ID EQ {varName = $varID.text;})?
		addClass=classId { $className = $addClass.sid; }
        ('AUTOSET' { $autoset = true; } )?
	;

forActionDefinitionBody[List<TypedParameter> context] returns [LAWithParams action]
@init {
	boolean recursive = false;
	boolean descending = false;
	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context);
	List<LPWithParams> orders = new ArrayList<>();
	Inline inline = null;
	
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedForAProp(context, $expr.property, orders, $actDB.action, $elseActDB.action, $addObj.paramCnt, $addObj.className, $addObj.autoset, recursive, descending, $in.noInline, $in.forceInline);
	}	
}
	:	(	'FOR' 
		| 	'WHILE' { recursive = true; }
		)
		expr=propertyExpression[newContext, true]
		('ORDER'
			('DESC' { descending = true; } )? 
			ordExprs=nonEmptyPropertyExpressionList[newContext, true] { orders = $ordExprs.props; }
		)?
		in = inlineStatement[newContext]
		(addObj=forAddObjClause[newContext])?
		'DO' actDB=modifyContextFlowActionDefinitionBody[context, newContext, false, false, false]
		( {!recursive}?=> 'ELSE' elseActDB=keepContextFlowActionDefinitionBody[context, false])?
	;

terminalFlowActionDefinitionBody returns [LAWithParams action]
@init {
	ChangeFlowActionType type = null;
}
@after {
	if (inMainParseState()) {
		$action = self.getTerminalFlowAction(type);
	}
}
	:	'BREAK' { type = ChangeFlowActionType.BREAK; }
	|   'CONTINUE' { type = ChangeFlowActionType.CONTINUE; }
	|	'RETURN' { type = ChangeFlowActionType.RETURN; }
	;


////////////////////////////////////////////////////////////////////////////////
/////////////////////////////OVERRIDE STATEMENT/////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

overridePropertyStatement
scope {
	String topName;
}
@init {
	List<TypedParameter> context = new ArrayList<>();
	LPWithParams property = null;
	LPWithParams when = null;
}
@after {
	if (inMainParseState()) {
        self.addImplementationToAbstractProp($prop.propUsage, $list.params, property, when);
	}
}
	:	prop=propertyUsage 
	{
		$overridePropertyStatement::topName = $prop.name;
	}
		'(' list=typedParameterList ')' { context = $list.params; }
        '+='
        ('WHEN' whenExpr=propertyExpression[context, false] 'THEN' { when = $whenExpr.property; })?
        expr=propertyExpressionOrContextIndependent[context, false, when == null] // for abstract VALUE will also support patch / explicit classes params (because param classes are also explicitly set in property definition, for WHEN will keep as it is)  
        { 
            property = $expr.property;
            if(inMainParseState()) {
                self.checkNotExprInExpr($expr.property,$expr.ci);
                if(when == null)
                    property = self.checkAndSetExplicitClasses(property, self.getClassesFromTypedParams(context)); // just like in property declaration we need explicit classes (that will add implicit IF paramater IS class even if there is no parameter usage in expression)
            }
        } ';'
	;

overrideActionStatement
scope {
	String topName;
}
@init {
	List<TypedParameter> context = new ArrayList<>();
	LAWithParams action = null;
	LPWithParams when = null;

	boolean optimisticAsync = false;
}
@after {
	if (inMainParseState()) {
        self.addImplementationToAbstractAction($prop.propUsage, $list.params, action, when, optimisticAsync);
	}
}
	:	'ACTION'?
	    prop=propertyUsage
	{
		$overrideActionStatement::topName = $prop.name;
	}
		'(' list=typedParameterList ')' { context = $list.params; }
        '+'
        ('WHEN' whenExpr=propertyExpression[context, false] 'THEN' { when = $whenExpr.property; })?
        actionDB=listTopContextDependentActionDefinitionBody[context, false, when == null] // for abstract LIST will also support patch / explicit classes params (because param classes are also explicitly set in property definition, for WHEN will keep as it is)
        { 
            action = $actionDB.action; 
            if(inMainParseState()) {
                if(when == null)
                    action.getLP().setExplicitClasses(self.getClassesFromTypedParams(context)); // just like in action declaration we need full context, and explicit classes (that will add implicit IF paramater IS class even if there is no parameter usage in body)
            }             
        }
        ('OPTIMISTICASYNC' { optimisticAsync = true; } )?
	;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// CONSTRAINT STATEMENT //////////////////////////
////////////////////////////////////////////////////////////////////////////////

constraintStatement 
@init {
    List<TypedParameter> context = new ArrayList<>();
	boolean checked = false;
	LP<?> property = null;
	List<NamedPropertyUsage> propUsages = null;
	List<LPWithParams> properties = new ArrayList<>();
	DebugInfo.DebugPoint debugPoint = getEventDebugPoint();
}
@after {
	if (inMainParseState()) {
		self.addScriptedConstraint(property, $et.event, checked, propUsages, $message.property.getLP(), properties, debugPoint);
	}
}
	:	'CONSTRAINT'
		et=baseEventPE
		{
			if (inMainParseState()) {
				self.setPrevScope($et.event);
			}
		}
		expr=propertyExpression[context, true] { if (inMainParseState()) property = self.checkSingleParam($expr.property).getLP(); }
		('CHECKED' { checked = true; }
			('BY' list=nonEmptyPropertyUsageList { propUsages = $list.propUsages; })?
		)?
		'MESSAGE' message=propertyExpression[new ArrayList<TypedParameter>(), false]
		{
			if (inMainParseState()) {
				self.dropPrevScope($et.event);
			}
		}
		('PROPERTIES' propExprs=nonEmptyPropertyExpressionList[context, true] { properties = $propExprs.props; })?
		';'
	;


////////////////////////////////////////////////////////////////////////////////
///////////////////////////////// FOLLOWS STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

followsStatement
@init {
	List<TypedParameter> context;
	NamedPropertyUsage mainProp;
	Event event = Event.APPLY;
}
@after {
	if (inMainParseState()) {
		self.addScriptedFollows(mainProp, context, $fcl.pfollows, $fcl.prop, $fcl.event, $fcl.debug);
	}
}
	:	prop=mappedProperty { mainProp = $prop.propUsage; context = $prop.mapping; }
		'=>'
		fcl=followsClause[context] 
		';'
;
	
followsClause[List<TypedParameter> context] returns [LPWithParams prop, Event event = Event.APPLY, DebugInfo.DebugPoint debug, List<PropertyFollowsDebug> pfollows = new ArrayList<>()] 
@init {
    $debug = getEventDebugPoint();
}
    :	
        et=baseEventPE { $event = $et.event; }
        {
            if (inMainParseState()) {
                self.setPrevScope($et.event);
            }
        }
        expr = propertyExpression[context, false]
		('RESOLVE' 
			('LEFT' {$pfollows.add(new PropertyFollowsDebug(null, true, false, getEventDebugPoint(), null));})?
			('RIGHT' {$pfollows.add(new PropertyFollowsDebug(null, false, false, getEventDebugPoint(), null));})?
		)? { $prop = $expr.property; }
        {
            if (inMainParseState()) {
                self.dropPrevScope($et.event);
            }
        }
;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// WRITE STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

writeWhenStatement
@init {
    boolean action = false;
}
@after {
	if (inMainParseState()) {
		self.addScriptedWriteWhen($mainProp.propUsage, $mainProp.mapping, $valueExpr.property, $whenExpr.property, action);
	}
}
	:	mainProp=mappedProperty 
		'<-'
		{
			if (inMainParseState()) {
				self.setPrevScope(ChangeEvent.scope);
			}
		}
		valueExpr=propertyExpression[$mainProp.mapping, false]
		'WHEN'
		('DO' { action = true; })? // DO - undocumented syntax
		whenExpr=propertyExpression[$mainProp.mapping, false]
		{
			if (inMainParseState()) {
				self.dropPrevScope(ChangeEvent.scope);
			}
		}
		';'
	;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// EVENT STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

eventStatement
@init {
	List<TypedParameter> context = new ArrayList<>();
	List<LPWithParams> orderProps = new ArrayList<>();
	boolean descending = false;
	DebugInfo.DebugPoint debug = getEventDebugPoint();
}
@after {
	if (inMainParseState()) {
		self.addScriptedWhen($whenExpr.property, $action.action, orderProps, descending, $et.event, $in.noInline, $in.forceInline, debug, null);
	} 
}
	:	'WHEN'
		et=baseEventPE
		{
			if (inMainParseState()) {
				self.setPrevScope($et.event);
			}
		}
		whenExpr=propertyExpression[context, true]
		(	'ORDER' ('DESC' { descending = true; })?
			orderList=nonEmptyPropertyExpressionList[context, false] { orderProps.addAll($orderList.props); }
		)?
		in=inlineStatement[context]
		'DO'
		action=endDeclTopContextDependentActionDefinitionBody[context, false, false]
		{
			if (inMainParseState()) {
				self.dropPrevScope($et.event);
			}
		}
	;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////// GLOBAL EVENT STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

globalEventStatement
@init {
	boolean single = false;
}
@after {
	if (inMainParseState()) {
		self.addScriptedGlobalEvent($action.action, $et.event, single);
	}
}
	:	'ON' 
		et=baseEventNotPE
		{
			if (inMainParseState()) {
				self.setPrevScope($et.event);
			}
		}
		('SINGLE' { single = true; })?
		action=endDeclTopContextDependentActionDefinitionBody[new ArrayList<TypedParameter>(), false, false]
		{
			if (inMainParseState()) {
				self.dropPrevScope($et.event);
			}
		}
	;

baseEventNotPE returns [Event event]
@init {
	SystemEvent baseEvent = SystemEvent.APPLY;
	List<String> ids = null;
	List<ActionOrPropertyUsage> puAfters = null;
}
@after {
	if (inMainParseState()) {
		$event = self.createScriptedEvent($name.text, baseEvent, ids, puAfters);
	}
}
	:
	    (name=ID)?
	    ('GLOBAL' { baseEvent = SystemEvent.APPLY; } | 'LOCAL' { baseEvent = SystemEvent.SESSION; })?
		('FORMS' (neIdList=nonEmptyCompoundIdList { ids = $neIdList.ids; }) )?
		(('GOAFTER' | 'AFTER') (nePropList=nonEmptyActionOrPropertyUsageList { puAfters = $nePropList.propUsages; }) )?
	;

baseEventPE returns [Event event]
@init {
	SystemEvent baseEvent = SystemEvent.APPLY;
	List<String> ids = null;
	List<ActionOrPropertyUsage> puAfters = null;
}
@after {
	if (inMainParseState()) {
		$event = self.createScriptedEvent($name.text, baseEvent, ids, puAfters);
	}
}
	:	('GLOBAL' { baseEvent = SystemEvent.APPLY; } | 'LOCAL' { baseEvent = SystemEvent.SESSION; })?
		('FORMS' (neIdList=nonEmptyCompoundIdList { ids = $neIdList.ids; }) )?
		(('GOAFTER' | 'AFTER') (nePropList=nonEmptyActionOrPropertyUsageList { puAfters = $nePropList.propUsages; }) )?
        ( { input.LA(1)==ID && input.LA(2)==EQ }?
          name=ID EQ
        )?
	;

showRecDepActionDefinitionBody [List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
    boolean showRec = false;
    boolean global = true;
}
@after {
	if (inMainParseState()) {
        $action = self.addScriptedShowRecDepAction($nePropList.propUsages, showRec, global);
	}
}
	:	(   'SHOWREC' { showRec = true; }
	        |
	        'SHOWDEP'
            ('GLOBAL' | 'LOCAL' { global = false; })?
        )
        (nePropList=nonEmptyActionOrPropertyUsageList)?
	;

inlineStatement[List<TypedParameter> context] returns [List<LPWithParams> noInline = new ArrayList<>(), boolean forceInline = false]
	:   ('NOINLINE' { $noInline = null; } ( '(' params=singleParameterList[context, false] { $noInline = $params.props; } ')' )? )?
	    ('INLINE' { $forceInline = true; })?
	;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// ASPECT STATEMENT //////////////////////////////
////////////////////////////////////////////////////////////////////////////////

aspectStatement
@init {
	List<TypedParameter> context = new ArrayList<>();
	boolean before = true;
}
@after {
	if (inMainParseState()) {
		self.addScriptedAspect($mainProp.propUsage, $mainProp.mapping, $action.action, before);
	}
}
	:	(	'BEFORE' 
		| 	'AFTER' { before = false; }
		)
		mainProp=mappedProperty
		'DO' action=endDeclTopContextDependentActionDefinitionBody[$mainProp.mapping, false, false]
	;


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// TABLE STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

tableStatement 
@init {
	boolean isFull = false;
	boolean isNoDefault = false;
}
@after {
	if (inMetaClassTableParseState()) {
		self.addScriptedTable($name.text, $dbName.val, $list.ids, isFull, isNoDefault);
	}
}
	:	'TABLE' name=ID (dbName = stringLiteral)? '(' list=classIdList ')' ('FULL' {isFull = true;} | 'NODEFAULT' { isNoDefault = true; } )? ';';

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// INDEX STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

mappedPropertyOrSimpleParam[List<TypedParameter> context] returns [LPWithParams property]
    :   (   toProp=propertyUsage '(' params=singleParameterList[context, true] ')' { if(inMainParseState()) { $property = self.findIndexProp($toProp.propUsage, $params.props, context); } }
        |   param=singleParameter[context, true] { $property = $param.property; }
        )
;

nonEmptyMappedPropertyOrSimpleParamList[List<TypedParameter> context] returns [List<LPWithParams> props]
@init {
	$props = new ArrayList<>();
}
	:	first=mappedPropertyOrSimpleParam[context] { $props.add($first.property); }
		(',' next=mappedPropertyOrSimpleParam[context] { $props.add($next.property); })*
	;

indexStatement
@init {
	List<TypedParameter> context = new ArrayList<>();
	IndexType indexType = IndexType.DEFAULT;
}
@after {
	if (inMainParseState()) {
		self.addScriptedIndex($dbName.val, context, $list.props, indexType);
	}	
}
	:	'INDEX' (dbName=stringLiteralNoID)? ('LIKE' { indexType = IndexType.LIKE; } | 'MATCH' { indexType = IndexType.MATCH; })? list=nonEmptyMappedPropertyOrSimpleParamList[context] ';'
	;


////////////////////////////////////////////////////////////////////////////////
/////////////////////////////// WINDOW STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

windowStatement
	:	windowCreateStatement
	|	windowHideStatement
	;

windowCreateStatement
@init {
	boolean isNative = false;
}
@after {
	if (inMainParseState()) {
		self.addScriptedWindow(isNative, $name.name, $name.caption, $opts.options);
	}
}
    //'TOOLBAR' is backward compatibility in 6.0, will be removed in 7.0
	:	'WINDOW' name=simpleNameWithCaption ('NATIVE' { isNative = true; })? 'TOOLBAR'? opts=windowOptions  ';'
	;

windowHideStatement
	:	'HIDE' 'WINDOW' wid=compoundID ';'
		{
			if (inMainParseState()) {
				self.hideWindow($wid.sid);
			}
		}
	;

windowOptions returns [NavigatorWindowOptions options]
@init {
	$options = new NavigatorWindowOptions();
}
	:	(	'HIDETITLE' { $options.setDrawTitle(false); }
		|	'HIDESCROLLBARS' { $options.setDrawScrollBars(false); }
		|	o=orientation { $options.setOrientation($o.val); }
		|	dp=dockPosition { $options.setDockPosition($dp.val); }
		|	bp=borderPosition { $options.setBorderPosition($bp.val); }
		|	'HALIGN' '(' ha=flexAlignmentLiteral ')' { $options.setHAlign($ha.val); }
		|	'VALIGN' '(' va=flexAlignmentLiteral ')' { $options.setVAlign($va.val); }
		|	'TEXTHALIGN' '(' tha=flexAlignmentLiteral ')' { $options.setTextHAlign($tha.val); }
		|	'TEXTVALIGN' '(' tva=flexAlignmentLiteral ')' { $options.setTextVAlign($tva.val); }
        |	'CLASS' aclass=propertyExpressionOrLiteral[null] {
                if (inMainParseState()) {
                    if($aclass.literal != null && $aclass.literal.value instanceof LocalizedString) {
                        $options.elementClass = ((LocalizedString) $aclass.literal.value).toString();
                    } else {
                        $options.elementClassProperty = $aclass.property;
                    }
                }
            }
		)*
	;

borderPosition returns [BorderPosition val]
	:	'LEFT'		{ $val = BorderPosition.LEFT; }
	|	'RIGHT'		{ $val = BorderPosition.RIGHT; }
	|	'TOP'		{ $val = BorderPosition.TOP; }
	|	'BOTTOM'	{ $val = BorderPosition.BOTTOM; }
	;

dockPosition returns [DockPosition val]
	:	'POSITION' '(' x=intLiteral ',' y=intLiteral ',' w=intLiteral ',' h=intLiteral ')' { $val = new DockPosition($x.val, $y.val, $w.val, $h.val); }
	;

orientation returns [Orientation val]
	:	'VERTICAL'		{ $val = Orientation.VERTICAL; }
	|	'HORIZONTAL'	{ $val = Orientation.HORIZONTAL; }
	;


////////////////////////////////////////////////////////////////////////////////
/////////////////////////////// NAVIGATOR STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

navigatorStatement
	:	'NAVIGATOR' navigatorElementStatementBody[self.baseLM.root]
	;

navigatorElementStatementBody[NavigatorElement parentElement]
	:	'{'
			(	moveNavigatorElementStatement[parentElement]
			|	newNavigatorElementStatement[parentElement]
			|	editNavigatorElementStatement[parentElement]
			|	emptyStatement
			)*
		'}'
	|	emptyStatement
	;

moveNavigatorElementStatement[NavigatorElement parentElement]
	:	'MOVE' elem=navigatorElementSelector (caption=localizedStringLiteral)? opts=navigatorElementOptions
		{
			if (inMainParseState()) {
				self.setupNavigatorElement($elem.element, $caption.val, $parentElement, $opts.options, false);
			}
		}
		navigatorElementStatementBody[$elem.element]
	;

newNavigatorElementStatement[NavigatorElement parentElement]
@init {
	NavigatorElement newElement = null;
}
	:	'NEW' b=navigatorElementDescription opts=navigatorElementOptions
		{
			if (inMainParseState()) {
				self.setupNavigatorElement($b.element, null, $parentElement, $opts.options, false);
			}
		}
		navigatorElementStatementBody[$b.element]
	;

navigatorElementDescription returns [NavigatorElement element]
@init {
	boolean isAction = false;
}
@after {
	if (inMainParseState()) {
 		$element = self.createScriptedNavigatorElement($name.text, $caption.val, getCurrentDebugPoint(true), $pu.propUsage, $formName.sid, isAction);
 	}	
}
	:	'FOLDER' name=ID (caption=localizedStringLiteral)?
	|	'FORM' ((name=ID)? (caption=localizedStringLiteral)? '=')? formName=compoundID
	|	('ACTION' { isAction = true; } )? ((name=ID)? (caption=localizedStringLiteral)? '=')? pu=propertyUsage
	;

navigatorElementOptions returns [NavigatorElementOptions options] 
@init {
	$options = new NavigatorElementOptions();
}
	:
	(	('WINDOW' wid=compoundID { $options.windowName = $wid.sid; } ('PARENT' { $options.parentWindow = true; })? )
	|	pos=navigatorElementRelativePosition { $options.location = $pos.location; }
	|	('IMAGE' (image=propertyExpressionOrLiteral[null])? {
	        if (inMainParseState()) {
	            if($image.literal != null && $image.literal.value instanceof LocalizedString) {
	                $options.imageOption = new ImageOption(((LocalizedString) $image.literal.value).toString());
	            } else if($image.property != null) {
	                $options.imageOption = new ImageOption($image.property);
	            } else
	                $options.imageOption = new ImageOption(true);
	        }
	    } | 'NOIMAGE' { $options.imageOption = new ImageOption(false); } )
	|	'CLASS' aclass = propertyExpressionOrLiteral[null] {
	        if (inMainParseState()) {
	            if($aclass.literal != null && $aclass.literal.value instanceof LocalizedString) {
	                $options.elementClass = ((LocalizedString) $aclass.literal.value).toString();
	            } else {
	                $options.elementClassProperty = $aclass.property;
	            }
	        }
	    }
	|   'HEADER' headerExpr = propertyExpression[null, false] { $options.headerProperty = $headerExpr.property; }
	)*
	;

editNavigatorElementStatement[NavigatorElement parentElement]
	:	elem=navigatorElementSelector (caption=localizedStringLiteral)? opts=navigatorElementOptions
		{
			if (inMainParseState()) {
				self.setupNavigatorElement($elem.element, $caption.val, $parentElement, $opts.options, true);
			}
		}
		navigatorElementStatementBody[$elem.element]
	;
	
navigatorElementSelector returns [NavigatorElement element]
	:	cid=compoundID
		{
			if (inMainParseState()) {
				$element = self.findNavigatorElement($cid.sid);
			}
		}
	;



////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// DESIGN STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

designStatement
scope {
	ScriptingFormView design;
}
@init {
	ScriptingFormView formView = null;
	boolean applyDefault = false;
}
	:	header=designHeader	{ $designStatement::design = formView = $header.view; }
		componentStatementBody[formView == null ? null : formView.getMainContainer()]
	;

designHeader returns [ScriptingFormView view]
@init {
	boolean customDesign = false;
	LocalizedString caption = null;
}
@after {
	if (inMainParseState()) {
		$view = self.getFormDesign($cid.sid, caption, customDesign);
	}
}
	:	'DESIGN' cid=compoundID (s=localizedStringLiteral { caption = $s.val; })? ('CUSTOM' { customDesign = true; })?
	;

componentStatementBody [ComponentView parentComponent]
	:	'{'
		(	setObjectPropertyStatement[parentComponent]
		|	setupComponentStatement
		|	newComponentStatement[parentComponent]
		|	moveComponentStatement[parentComponent]
		|	removeComponentStatement
		|	emptyStatement
		)*
		'}'
	|	emptyStatement
	;

setupComponentStatement
	:	comp=componentSelector componentStatementBody[$comp.component]
	;

newComponentStatement[ComponentView parentComponent]
@init {
	ComponentView newComp = null;
}
	:	'NEW' cid=ID (insPosition=componentRelativePosition)?
		{
			if (inMainParseState()) {
				newComp = $designStatement::design.createNewComponent($cid.text, parentComponent, $insPosition.location, self.getVersion(), getCurrentDebugPoint());
			}
		}
		componentStatementBody[newComp]
	;
	
moveComponentStatement[ComponentView parentComponent]
@init {
	ComponentView insComp = null;
}
	:	'MOVE' insSelector=componentSelector { insComp = $insSelector.component; } (insPosition=componentRelativePosition)?
		{
			if (inMainParseState()) {
				$designStatement::design.moveComponent(insComp, parentComponent, $insPosition.location, self.getVersion());
			}
		}
		componentStatementBody[insComp]
	;

removeComponentStatement
	:	'REMOVE' compSelector=componentSelector ';'
		{
			if (inMainParseState()) {
				$designStatement::design.removeComponent($compSelector.component, self.getVersion());
			}
		}
	;

componentSelector returns [ComponentView component]
    :
        exc=formComponentSelector[$designStatement::design] { $component = $exc.component; }
    ;
    
formComponentSelector[ScriptingFormView formView] returns [ComponentView component]
	:	'PARENT' '(' child=componentSelector ')'
		{
			if (inMainParseState()) {
				formView.getParentContainer($child.component, self.getVersion());
			}
		}
	|	'PROPERTY' '(' prop=propertySelector[formView] ')' { $component = $prop.propertyView; }
	|   'FILTER' '(' filter=filterSelector[formView] ')' { $component = $filter.filterView; }
	|   exc=formContainersComponentSelector
	    {
			if (inMainParseState()) {
				$component = formView.getComponentBySID($exc.sid, self.getVersion());
			}
	    }
	|	mid=ID
		{
			if (inMainParseState()) {
				$component = formView.getComponentBySID($mid.text, self.getVersion());
			}
		}
	;
formContainersComponentSelector returns [String sid]
    :   gt = groupObjectTreeComponentSelector { $sid = $gt.sid; }
    |   gs = componentSingleSelectorType { $sid = $gs.text; }
    |   'GROUP' '(' (   ',' ggo = groupObjectTreeSelector { $sid = "GROUP(," + $ggo.sid + ")"; }
                    |   ggr = compoundID ',' ggo = groupObjectTreeSelector { if(inMainParseState()) $sid = "GROUP(" + self.findGroup($ggr.sid).getCanonicalName() + "," + $ggo.sid + ")"; }
                    |   ggr = compoundID { if(inMainParseState()) $sid = "GROUP(" + self.findGroup($ggr.sid).getCanonicalName() + ")"; }
                    |   { $sid = "GROUP()"; }
                    ) ')'
    |   'FILTERGROUP' '(' gfg = ID ')' { $sid = "FILTERGROUP(" + $gfg.text + ")"; }
    ;

componentSingleSelectorType
    :
    	'BOX' | 'OBJECTS' | 'TOOLBARBOX' | 'TOOLBARLEFT' | 'TOOLBARRIGHT' | 'TOOLBAR' | 'POPUP' | 'PANEL'
    ;

groupObjectTreeSelector returns [String sid]
    :
           'TREE' tg = ID { $sid = "TREE " + $tg.text; }
        |   go = ID { $sid = $go.text; }
    ;

groupObjectTreeComponentSelector returns [String sid]
@init {
	String result = null;
}
    :
        ( cst=componentSingleSelectorType { result = $cst.text; } 
        |   gost=groupObjectTreeComponentSelectorType 
            { 
                result = $gost.text;
            })
        '(' gots = groupObjectTreeSelector ')'
        {
            $sid = result + "(" + $gots.sid + ")";
        }
    ;

groupObjectTreeComponentSelectorType
    :
    	'TOOLBARSYSTEM' | 'FILTERGROUPS' | 'CLASSCHOOSER' | 'GRID' | 'FILTERBOX' | 'FILTERS' | 'FILTERCONTROLS'
    ;

propertySelector[ScriptingFormView formView] returns [PropertyDrawView propertyView = null]
	:	pname=ID
		{
			if (inMainParseState()) {
				$propertyView = formView.getPropertyView($pname.text, self.getVersion());
			}
		}
	|	mappedProp=mappedPropertyDraw	
		{
			if (inMainParseState()) {
				$propertyView = formView.getPropertyView($mappedProp.name, $mappedProp.mapping, self.getVersion());
			}
		}
	;
	
filterSelector[ScriptingFormView formView] returns [FilterView filterView = null]
	:	pname=ID
		{
			if (inMainParseState()) {
				$filterView = formView.getFilterView($pname.text, self.getVersion());
			}
		}
	|	mappedProp=mappedPropertyDraw	
		{
			if (inMainParseState()) {
				$filterView = formView.getFilterView($mappedProp.name, $mappedProp.mapping, self.getVersion());
			}
		}
	;

setObjectPropertyStatement[Object propertyReceiver] returns [String id, Object value]
	:	ID EQ componentPropertyValue ';'  {
            if(inMainParseState())
	            setObjectProperty($propertyReceiver, $ID.text, $componentPropertyValue.value, self.getVersion(), () -> getCurrentDebugPoint());
        }
	;

componentPropertyValue returns [Object value] //commented literals are in designPropertyObject
	:   //c=colorLiteral { $value = $c.val; }
    //|   s=localizedStringLiteralNoID { $value = $s.val; }
	//|   i=intLiteral { $value = $i.val; }
	//|   d=doubleLiteral { $value = $d.val; }
	|   dim=dimensionLiteral { $value = $dim.val; }
	|   b=booleanLiteral { $value = $b.val; }
	|   tb=tbooleanLiteral { $value = $tb.val; }
	|   intB=boundsIntLiteral { $value = $intB.val; }
	|   doubleB=boundsDoubleLiteral { $value = $doubleB.val; }
	|   alignment=flexAlignmentLiteral { $value = $alignment.val; }
	|   prop=designPropertyObject { $value = BaseUtils.nvl($prop.literal, $prop.property); }
	;


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// META STATEMENT //////////////////////////////
////////////////////////////////////////////////////////////////////////////////

metaCodeDeclarationStatement
@init {
	List<Pair<String, Boolean>> tokens;
	int lineNumber = self.getParser().getCurrentParserLineNumber();

    ScriptParser.State oldState = null;
}
@after {
	if (inMetaClassTableParseState()) {
		self.addScriptedMetaCodeFragment($id.text, $list.ids, tokens, lineNumber);
	}
}
	
	:	'META' id=ID '(' list=idList ')'
        {
            self.getParser().enterMetaDeclState();
            oldState = parseState;
            parseState = ScriptParser.State.METADECL;
        }
        statements
        {
            tokens = self.getParser().leaveMetaDeclState();

            parseState = oldState;
        }
		'END'
	;


metaCodeStatement
@init {
	int lineNumberBefore = self.getParser().getCurrentParserLineNumber();
	ScriptParser.State oldState = null; 
	boolean enabledMeta = false;
	int lineNumberAfter;
}
@after {
    if(!inMetaParseState()) // we don't want to run meta when we're in META declaration, or inside the @code generated by IDE
    	self.runMetaCode($id.sid, $list.ids, lineNumberBefore, lineNumberAfter, enabledMeta);
}
	:	'@' id=compoundID '(' list=metaCodeIdList ')' {
            lineNumberAfter = self.getParser().getCurrentParserLineNumber();
            }
		(
		{
		    enabledMeta = true;

            if(!inGenMetaParseState()) { // we want to skip generated statements, so grabbing only topmost rules
                self.getParser().grabMetaDeclCode();

                oldState = parseState;
                parseState = ScriptParser.State.GENMETA;
            }
		}
		'{'
		statements
		'}'
		{
            if(oldState != null)
                parseState = oldState;

            if(!inGenMetaParseState()) // we want to skip generated statements, so grabbing only topmost rules
                self.getParser().skipMetaDeclCode(); // we want to skip generated statements
		}
		)? // for intellij plugin
		';'	
	;


metaCodeIdList returns [List<String> ids]
@init {
	ids = new ArrayList<>();
}
	:	firstId=metaCodeId { ids.add($firstId.sid); }
		( ',' nextId=metaCodeId { ids.add($nextId.sid); })* 
	;


metaCodeId returns [String sid]
	:	id=compoundID 		{ $sid = $id.sid; }
	|	ptype=primitiveType	{ $sid = $ptype.text; } 
	|	lit=metaCodeLiteral { $sid = $lit.sid; }
	|	{ $sid = ""; }
	;

metaCodeLiteral returns [String sid]
	:	slit=metaCodeStringLiteral { $sid = $slit.val; }
	|	lit=metaCodeNonStringLiteral { $sid = $lit.text; }
	;

metaCodeStringLiteral returns [String val]
	:	slit=multilineStringLiteral { $val = $slit.val; }
	|   rslit=rawMultilineStringLiteral { $val = $rslit.val; }
	;

metaCodeNonStringLiteral
	:	UINT_LITERAL
	|	UNUMERIC_LITERAL
	|	UDOUBLE_LITERAL
	|	ULONG_LITERAL
	|	LOGICAL_LITERAL
	|	T_LOGICAL_LITERAL
	|	DATE_LITERAL
	|	DATETIME_LITERAL
	|	TIME_LITERAL
	|	NULL_LITERAL
	|	COLOR_LITERAL
	;


////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////// COMMON /////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

emptyStatement
	:	';'
	;

mappedProperty returns [NamedPropertyUsage propUsage, List<TypedParameter> mapping]
	:	propU=propertyUsage { $propUsage = $propU.propUsage; }
		'('
		list=typedParameterList { $mapping = $list.params; }
		')'
	;

typedParameter returns [TypedParameter param]
@after {
	if (inMainParseState()) {
		$param = self.new TypedParameter($cname.sid, $pname.text);
	}
}
	:	(cname=classId)? pname=ID
	;

imageOption returns [String image]
    :   ('IMAGE' (img=stringLiteral)? {$image = BaseUtils.nvl($img.val, AppServerImage.AUTO); } | 'NOIMAGE' { $image = AppServerImage.NULL; } )
    ;

simpleNameWithCaption returns [String name, LocalizedString caption]
	:	simpleName=ID { $name = $simpleName.text; }
		(captionStr=localizedStringLiteral { $caption = $captionStr.val; })?
	;

simpleNameOrWithCaption returns [String name, LocalizedString caption] 
	:	(   simpleName=ID { $name = $simpleName.text; }
		    (captionStr=localizedStringLiteral { $caption = $captionStr.val; })?
        )
        |
            (captionStr=localizedStringLiteral { $caption = $captionStr.val; })
	;
	
idList returns [List<String> ids] 
@init {
	ids = new ArrayList<>();	
} 
	:	(neIdList=nonEmptyIdList { ids = $neIdList.ids; })?
	;

classIdList returns [List<String> ids]
@init {
	$ids = new ArrayList<>();
}
	:	(neList=nonEmptyClassIdList { ids = $neList.ids; })?
	;

nonEmptyClassIdList returns [List<String> ids]
@init {
	ids = new ArrayList<>();
}
	:	firstClassName=classId { ids.add($firstClassName.sid); }
		(',' className=classId { ids.add($className.sid); })*
	;

signatureClassList returns [List<String> ids]
@init {
	$ids = new ArrayList<>();
}
	:	(neList=nonEmptySignatureClassList { ids = $neList.ids; })?
	;

nonEmptySignatureClassList returns [List<String> ids]
@init {
	ids = new ArrayList<>();
}
	:	firstClassName=signatureClass { ids.add($firstClassName.sid); }
		(',' className=signatureClass { ids.add($className.sid); })*
	;

typedParameterList returns [List<TypedParameter> params]
@init {
	$params = new ArrayList<>();
}
	:	(neList=nonEmptyTypedParameterList { $params = $neList.params; })?
	;

nonEmptyTypedParameterList returns [List<TypedParameter> params]
@init {
	params = new ArrayList<>();
}
	:	firstParam=typedParameter { params.add($firstParam.param); }
		(',' param=typedParameter { params.add($param.param); })*
	;

nonEmptyIdList returns [List<String> ids]
@init {
	ids = new ArrayList<>(); 
}
	:	firstId=ID	{ $ids.add($firstId.text); }
		(',' nextId=ID	{ $ids.add($nextId.text); })*
	;

nonEmptyCompoundIdList returns [List<String> ids]
@init {
	ids = new ArrayList<>();
}
	:	firstId=compoundID	{ $ids.add($firstId.sid); }
		(',' nextId=compoundID	{ $ids.add($nextId.sid); })*
	;

nonEmptyPropertyUsageList returns [List<NamedPropertyUsage> propUsages]
@init {
	$propUsages = new ArrayList<>();
}
	:	first=propertyUsage { $propUsages.add($first.propUsage); }
		(',' next=propertyUsage { $propUsages.add($next.propUsage); })* 
	; 

singleParameterList[List<TypedParameter> context, boolean dynamic] returns [List<LPWithParams> props]
@init {
	props = new ArrayList<>();
}
	:	(first=singleParameter[context, dynamic] { props.add($first.property); }
		(',' next=singleParameter[context, dynamic] { props.add($next.property); })*)?
	;

nonEmptyActionPDBList[List<TypedParameter> context, boolean dynamic] returns [List<LAWithParams> actions]
@init {
	$actions = new ArrayList<>();
}
	:	first=keepContextFlowActionDefinitionBody[context, dynamic] { $actions.add($first.action); }
		(',' next=keepContextFlowActionDefinitionBody[context, dynamic] { $actions.add($next.action); })*
	; 

propertyExpressionList[List<TypedParameter> context, boolean dynamic] returns [List<LPWithParams> props] 
@init {
	$props = new ArrayList<>();
}
	:	(neList=nonEmptyPropertyExpressionList[context, dynamic] { $props = $neList.props; })?
	;
	

nonEmptyPropertyExpressionList[List<TypedParameter> context, boolean dynamic] returns [List<LPWithParams> props]
@init {
	$props = new ArrayList<>();
}
	:	first=propertyExpression[context, dynamic] { $props.add($first.property); }
		(',' next=propertyExpression[context, dynamic] { $props.add($next.property); })* 
	;
	
constantProperty[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPNotExpr ci]
@init {
    int lineNumber = self.getParser().getCurrentParserLineNumber();
	ScriptingLogicsModule.ConstType cls = null;
	Object value = null;
}
@after {
	if (inMainParseState()) {
		Pair<LPWithParams, LPNotExpr> constantProp = self.addConstantProp(cls, value, lineNumber, context, dynamic);
		$property = constantProp.first;
		$ci = constantProp.second;
	}
}
	:	lit = expressionLiteral { cls = $lit.cls; value = $lit.value; }
	;

expressionLiteral returns [ScriptingLogicsModule.ConstType cls, Object value]
	:	cl=commonLiteral { $cls = $cl.cls; $value = $cl.value; } 	
	|	str=multilineStringLiteral { $cls = ScriptingLogicsModule.ConstType.STRING; $value = $str.val; }
	|   rstr=rawMultilineStringLiteral { $cls = ScriptingLogicsModule.ConstType.RSTRING; $value = $rstr.val; }
	;

commonLiteral returns [ScriptingLogicsModule.ConstType cls, Object value]
	: 	vint=uintLiteral	{ $cls = ScriptingLogicsModule.ConstType.INT; $value = $vint.val; }
	|	vlong=ulongLiteral	{ $cls = ScriptingLogicsModule.ConstType.LONG; $value = $vlong.val; }
	|	vnum=unumericLiteral   { $cls = ScriptingLogicsModule.ConstType.NUMERIC; $value = $vnum.val; }
	|	vdouble=udoubleLiteral { $cls = ScriptingLogicsModule.ConstType.REAL; $value = $vdouble.val; }
	|	vbool=booleanLiteral	{ $cls = ScriptingLogicsModule.ConstType.LOGICAL; $value = $vbool.val; { if (inMainParseState()) self.getChecks().checkBooleanUsage($vbool.val); }}
	|	vtbool=tbooleanLiteral	{ $cls = ScriptingLogicsModule.ConstType.TLOGICAL; $value = $vtbool.val; }
	|	vdate=dateLiteral	{ $cls = ScriptingLogicsModule.ConstType.DATE; $value = $vdate.val; }
	|	vdatetime=dateTimeLiteral { $cls = ScriptingLogicsModule.ConstType.DATETIME; $value = $vdatetime.val; }
	|	vtime=timeLiteral 	{ $cls = ScriptingLogicsModule.ConstType.TIME; $value = $vtime.val; }
	|	vsobj=staticObjectID { $cls = ScriptingLogicsModule.ConstType.STATIC; $value = $vsobj.sid; }
	|	vnull=NULL_LITERAL 	{ $cls = ScriptingLogicsModule.ConstType.NULL; }
	|	vcolor=colorLiteral { $cls = ScriptingLogicsModule.ConstType.COLOR; $value = $vcolor.val; }		
	;

classId returns [String sid]
	:	id=compoundID { $sid = $id.sid; }
	|	pid=primitiveType { $sid = $pid.text; }
	;

signatureClass returns [String sid]
	:	cid=classId { $sid = $cid.sid; }
	|	uc=unknownClass { $sid = $uc.text; }	
	; 

unknownClass 
	:	'?'
	;
	
compoundID returns [String sid]
	:	firstPart=ID { $sid = $firstPart.text; } ('.' secondPart=ID { $sid = $sid + '.' + $secondPart.text; })?
	;

staticObjectID returns [String sid]
	:	(namespacePart=ID '.')? classPart=ID '.' namePart=ID { $sid = ($namespacePart != null ? $namespacePart.text + '.' : "") + $classPart.text + '.' + $namePart.text; }
	;

formGroupObjectID returns [String sid]
    :	(namespacePart=ID '.')? formPart=ID '.' namePart=ID { $sid = ($namespacePart != null ? $namespacePart.text + '.' : "") + $formPart.text + '.' + $namePart.text; }
    ;

formFilterGroupID returns [String sid]
    :	(namespacePart=ID '.')? formPart=ID '.' namePart=ID { $sid = ($namespacePart != null ? $namespacePart.text + '.' : "") + $formPart.text + '.' + $namePart.text; }
    ;

formObjectID returns [String sid]
    :	(namespacePart=ID '.')? formPart=ID '.' namePart=ID { $sid = ($namespacePart != null ? $namespacePart.text + '.' : "") + $formPart.text + '.' + $namePart.text; }
    ;

formComponentID returns [FormEntity form, ComponentView component]
@init {
	ScriptingFormView formView = null;
}
    :
        (namespacePart=ID '.')? formPart=ID '.'
        {
            if(inMainParseState()) {
                formView = self.getFormDesign(($namespacePart != null ? $namespacePart.text + '.' : "") + $formPart.text, null, false);
            }
        }
        cs = formComponentSelector[formView] { $component = $cs.component; }
        {
            if(inMainParseState()) {
                $form = formView.getView().entity;
            }
        }
    ;

formPropertyID returns [PropertyDrawEntity propertyDraw]
@init {
	FormEntity form = null;
}
    :
        (namespace=ID '.')? formSName=ID '.'
        {
            if(inMainParseState()) {
                form = self.findForm(($namespace == null ? "" : $namespace.text + ".") + $formSName.text);
            }
        }
        prop=formPropertySelector[form]
        {
            $propertyDraw = $prop.propertyDraw;
        }
    ;

exclusiveOverrideOption returns [boolean isExclusive]
	:	'OVERRIDE' { $isExclusive = false; }
	|	'EXCLUSIVE'{ $isExclusive = true; } 
	;

abstractExclusiveOverrideOption returns [boolean isExclusive, Boolean isLast = null]
	:	('OVERRIDE' { $isExclusive = false; } (acopt = abstractCaseAddOption {$isLast = $acopt.isLast; } )? )
	|	'EXCLUSIVE'{ $isExclusive = true; }
	;

abstractCaseAddOption returns [boolean isLast]
	:	'FIRST' { $isLast = false; }
	|	'LAST'{ $isLast = true; }
	;

colorLiteral returns [Color val]
	:	c=COLOR_LITERAL { $val = Color.decode($c.text); }
	|	'RGB' '(' r=uintLiteral ',' g=uintLiteral ',' b=uintLiteral ')' { $val = self.createScriptedColor($r.val, $g.val, $b.val); } 
	;

multilineStringLiteral returns [String val]
	:	s=STRING_LITERAL { $val = self.removeCarriageReturn($s.text); }
	;

rawMultilineStringLiteral returns [String val]
	:   rs=RAW_STRING_LITERAL { $val = self.removeCarriageReturn($rs.text); }
	;

stringLiteral returns [String val]
	:	s=stringLiteralNoID { $val = $s.val; }
    |   id=ID { $val = null; }
	;

primitiveType returns [String val]
	:	p=PRIMITIVE_TYPE | JSON_TYPE | JSON_TEXT_TYPE | HTML_TYPE { $val = $p.text; }
	;

// there are some rules where ID is not desirable (see usages), where there is an ID
// it makes sense to be synchronized with noIDCheck in LSF.bnf in idea-plugin
localizedStringLiteralNoID returns [LocalizedString val]
	:	s=multilineStringLiteral { $val = self.transformLocalizedStringLiteral($s.val); }
	|   rs=rawMultilineStringLiteral { $val = self.getRawLocalizedStringLiteralText($rs.text); }
	;
	
stringLiteralNoID returns [String val]
	:	s=multilineStringLiteral { $val = self.transformStringLiteral($s.text); }
	|   rs=rawMultilineStringLiteral { $val = self.getRawStringLiteralText($rs.text); }
	;

localizedStringLiteral returns [LocalizedString val]
	:	ls=localizedStringLiteralNoID { $val = $ls.val; }
    |   s=ID { $val = null; }
	;

intLiteral returns [int val]
@init {
	boolean isMinus = false;
}
	:	(MINUS {isMinus=true;})?
		ui=uintLiteral  { $val = isMinus ? -$ui.val : $ui.val; }
	;

doubleLiteral returns [double val]
@init {
	boolean isMinus = false;
}
	:	(MINUS {isMinus=true;})?
		ud=UNUMERIC_LITERAL { $val = self.createScriptedDouble($ud.text); }
		{ if (isMinus) $val = -$val; }
	;

dateLiteral returns [LocalDate val]
	:	date=DATE_LITERAL { $val = self.dateLiteralToDate($date.text); }
	;

dateTimeLiteral returns [LocalDateTime val]
	:	time=DATETIME_LITERAL { $val = self.dateTimeLiteralToTimestamp($time.text); }
	;

timeLiteral returns [LocalTime val]
	:	time=TIME_LITERAL { $val = self.timeLiteralToTime($time.text); }
	;

booleanLiteral returns [boolean val]
	:	bool=LOGICAL_LITERAL { $val = Boolean.valueOf($bool.text); }
	;

tbooleanLiteral returns [boolean val]
	:	bool=T_LOGICAL_LITERAL { $val = self.tBooleanToBoolean($bool.text); }
	;

dimensionLiteral returns [Dimension val]
	:	'(' x=intLiteral ',' y=intLiteral ')' { $val = new Dimension($x.val, $y.val); }
	;

boundsIntLiteral returns [Insets val]
	:	'(' top=intLiteral ',' left=intLiteral ',' bottom=intLiteral ',' right=intLiteral ')' { $val = new Insets($top.val, $left.val, $bottom.val, $right.val); }
	;

boundsDoubleLiteral returns [Bounds val]
	:	'(' top=doubleLiteral ',' left=doubleLiteral ',' bottom=doubleLiteral ',' right=doubleLiteral ')' { $val = new Bounds($top.val, $left.val, $bottom.val, $right.val); }
	;

codeLiteral returns [String val]
	:	s=CODE_LITERAL { $val = $s.text; }
	;

flexAlignmentLiteral returns [FlexAlignment val]
    :   'START' { $val = FlexAlignment.START; }
    |   'CENTER' { $val = FlexAlignment.CENTER; }
    |   'END' { $val = FlexAlignment.END; }
    |   'STRETCH' { $val = FlexAlignment.STRETCH; }
    ;

propertyEditTypeLiteral returns [PropertyEditType val]
	:	'CHANGEABLE' { $val = PropertyEditType.EDITABLE; }
	|	'READONLY' { $val = PropertyEditType.READONLY; }
	|	'DISABLE' { $val = PropertyEditType.DISABLE; }
	;

emailRecipientTypeLiteral returns [Message.RecipientType val]
	:	'TO'	{ $val = Message.RecipientType.TO; }
	|	'CC'	{ $val = Message.RecipientType.CC; }
	|	'BCC'	{ $val = Message.RecipientType.BCC; }
	;

udoubleLiteral returns [double val]
	:	d=UDOUBLE_LITERAL { $val = self.createScriptedDouble($d.text.substring(0, $d.text.length() - 1)); }
	;	

unumericLiteral returns [BigDecimal val]
	:	u=UNUMERIC_LITERAL { $val = self.createScriptedNumeric($u.text); }
	;

uintLiteral returns [int val]
	:	u=UINT_LITERAL { $val = self.createScriptedInteger($u.text); }
	;		

ulongLiteral returns [long val]
	:	u=ULONG_LITERAL { $val = self.createScriptedLong($u.text.substring(0, $u.text.length() - 1)); }
	;

relOperand 
	:	RELEQ_OPERAND | LESS_OPERAND | GR_OPERAND	
	;
	
multOperand
	:	MULT | DIV
	;

/////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////// LEXER //////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////
	
fragment NEWLINE	:	'\r'?'\n'; 
fragment SPACE		:	(' '|'\t');
fragment DIGIT		:	'0'..'9';
fragment DIGITS		:	('0'..'9')+;
fragment EDIGITS	:	('0'..'9')*;
fragment HEX_DIGIT	: 	'0'..'9' | 'a'..'f' | 'A'..'F';
fragment FIRST_ID_LETTER	: ('a'..'z'|'A'..'Z');
fragment NEXT_ID_LETTER		: ('a'..'z'|'A'..'Z'|'_'|'0'..'9');
fragment OPEN_CODE_BRACKET	: '<{';
fragment CLOSE_CODE_BRACKET : '}>';

fragment STR_LITERAL_CHAR
	:	('\\'.)
	|	~('\''|'\\'|'$')
	| 	{input.LA(1) == '$' && input.LA(2) != '{'}?=> '$'
	;

fragment SIMPLE_RAW_STR_LITERAL_CHAR: ~('\'');
fragment RAW_STR_SPECIAL_CHAR: ~(NEXT_ID_LETTER|SPACE|'\n'|'\''|'+'|'*'|','|'='|'<'|'>'|'('|')'|'['|']'|'{'|'}'|'#');

fragment ESCAPED_STR_LITERAL_CHAR:	('\\'.) | ~('\\'|'{'|'}');
fragment BLOCK: '{' (BLOCK | ESCAPED_STR_LITERAL_CHAR)* '}';
fragment INTERPOLATION_BLOCK: '${' (BLOCK | ESCAPED_STR_LITERAL_CHAR)* '}';
fragment STRING_LITERAL_FRAGMENT:	'\'' (INTERPOLATION_BLOCK | STR_LITERAL_CHAR)* '\'';

fragment ID_FRAGMENT : FIRST_ID_LETTER NEXT_ID_LETTER*;
fragment NEXTID_FRAGMENT : NEXT_ID_LETTER+;


fragment ID_META_FRAGMENT : ('###' | '##')? ID_FRAGMENT (('###' | '##') NEXTID_FRAGMENT)*;

fragment STRING_META_SUFFIX_FRAGMENT : (('###' | '##') (NEXTID_FRAGMENT | STRING_LITERAL_FRAGMENT))*;
fragment STRING_META_FRAGMENT : ('###' | '##')? (NEXTID_FRAGMENT ('###' | '##'))* STRING_LITERAL_FRAGMENT STRING_META_SUFFIX_FRAGMENT;

fragment INTERVAL_TYPE : 'DATE' | 'DATETIME' | 'TIME' | 'ZDATETIME';

PRIMITIVE_TYPE  :	'INTEGER' | 'DOUBLE' | 'LONG' | 'BOOLEAN' | 'TBOOLEAN' | 'DATE' | ('DATETIME' ('[' '0'..'6' ']')?) | ('ZDATETIME' ('[' '0'..'6' ']')?) | 'YEAR'
                |   'TEXT' | 'RICHTEXT' | 'HTMLTEXT' | ('TIME' ('[' '0'..'6' ']')?) | 'WORDFILE' | 'IMAGEFILE' | 'PDFFILE' | 'VIDEOFILE' | 'DBFFILE' | 'RAWFILE'
				| 	'FILE' | 'EXCELFILE' | 'TEXTFILE' | 'CSVFILE' | 'HTMLFILE' | 'JSONFILE' | 'XMLFILE' | 'TABLEFILE' | 'NAMEDFILE'
				|   'WORDLINK' | 'IMAGELINK' | 'PDFLINK' | 'VIDEOLINK' | 'DBFLINK'
				|   'RAWLINK' | 'LINK' | 'EXCELLINK' | 'TEXTLINK' | 'CSVLINK' | 'HTMLLINK' | 'JSONLINK' | 'XMLLINK' | 'TABLELINK'
				|   ('BPSTRING' ('[' DIGITS ']')?) | ('BPISTRING' ('[' DIGITS ']')?)
				|	('STRING' ('[' DIGITS ']')?) | ('ISTRING' ('[' DIGITS ']')?) | 'NUMERIC' ('[' DIGITS ',' DIGITS ']')? | 'COLOR'
				|   ('INTERVAL' ('[' INTERVAL_TYPE ']'))
				|   'TSVECTOR' | 'TSQUERY';
JSON_TYPE       :   'JSON';
JSON_TEXT_TYPE  :   'JSONTEXT';
HTML_TYPE       :   'HTML';
LOGICAL_LITERAL :	'TRUE' | 'FALSE';
T_LOGICAL_LITERAL:	'TTRUE' | 'TFALSE';
NULL_LITERAL	:	'NULL';
ID				:	ID_META_FRAGMENT;
STRING_LITERAL	:	STRING_META_FRAGMENT;
WS				:	(NEWLINE | SPACE) { $channel=HIDDEN; };
COLOR_LITERAL 	:	'#' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT;
RAW_STRING_LITERAL:		('r'|'R') '\'' SIMPLE_RAW_STR_LITERAL_CHAR* '\''
				  |  	(   {(input.LA(1) == 'r' || input.LA(1) == 'R') && isRawStringSpecialChar(input.LA(2)) && input.LA(3) == '\''}?=>
				            ('r'|'R') c=RAW_STR_SPECIAL_CHAR '\'' { Character ch = $c.text.charAt(0); }
	                    	({input.LA(1) != '\'' || input.LA(2) != ch}?=> .)*
	                    	'\'' RAW_STR_SPECIAL_CHAR
	                    )
				  ;
COMMENTS		:	'//' ~('\n')* ('\n' | EOF) { $channel=HIDDEN; };
MULTILINE_COMMENTS	:	'/*' .* '*/' { $channel=HIDDEN; };	 
UINT_LITERAL 	:	DIGITS;
ULONG_LITERAL	:	DIGITS('l'|'L');
UDOUBLE_LITERAL	:	DIGITS '.' EDIGITS('d'|'D');
UNUMERIC_LITERAL:	DIGITS '.' EDIGITS;	  
DATE_LITERAL	:	DIGIT DIGIT DIGIT DIGIT '_' DIGIT DIGIT '_' DIGIT DIGIT; 
DATETIME_LITERAL:	DIGIT DIGIT DIGIT DIGIT '_' DIGIT DIGIT '_' DIGIT DIGIT '_' DIGIT DIGIT ':' DIGIT DIGIT (':' DIGIT DIGIT)?;
TIME_LITERAL	:	DIGIT DIGIT ':' DIGIT DIGIT (':' DIGIT DIGIT)?;
RECURSIVE_PARAM :	'$' FIRST_ID_LETTER NEXT_ID_LETTER*;	
EQ_OPERAND		:	('==') | ('!=');
EQ	            :	'=';
LESS_OPERAND	: 	('<');
GR_OPERAND		:	('>');
RELEQ_OPERAND	: 	('<=') | ('>=');
MINUS			:	'-';
PLUS			:	'+';
MULT			:	'*';
DIV				:	'/';
ADDOR_OPERAND	:	'(+)' | {ahead("(-)")}?=> '(-)';
CODE_LITERAL    : OPEN_CODE_BRACKET .* CLOSE_CODE_BRACKET;