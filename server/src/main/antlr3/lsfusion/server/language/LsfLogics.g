grammar LsfLogics;

@header {
	package lsfusion.server.language;

    import lsfusion.base.Pair;
    import lsfusion.base.col.heavy.OrderedMap;
    import lsfusion.base.col.interfaces.immutable.ImOrderSet;
    import lsfusion.interop.action.ServerResponse;
    import lsfusion.interop.form.ModalityType;
    import lsfusion.interop.form.WindowFormType;
    import lsfusion.interop.form.event.FormEventType;
    import lsfusion.interop.form.design.Alignment;
    import lsfusion.interop.form.design.ContainerType;
    import lsfusion.interop.base.view.FlexAlignment;
    import lsfusion.interop.form.object.table.grid.ListViewType;
    import lsfusion.interop.form.property.ClassViewType;
    import lsfusion.interop.form.property.PivotOptions;
    import lsfusion.interop.form.property.PropertyEditType;
    import lsfusion.interop.form.property.PropertyGroupType;
    import lsfusion.interop.form.print.FormPrintType;
    import lsfusion.server.base.version.Version;
    import lsfusion.server.data.expr.formula.SQLSyntaxType;
    import lsfusion.server.data.expr.query.PartitionType;
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
    import lsfusion.server.logics.form.interactive.ManageSessionType;
    import lsfusion.server.logics.form.interactive.UpdateType;
    import lsfusion.server.logics.form.interactive.action.expand.ExpandCollapseType;
    import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
    import lsfusion.server.logics.form.interactive.design.ComponentView;
    import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
    import lsfusion.server.logics.form.interactive.property.GroupObjectProp;
    import lsfusion.server.logics.form.open.MappedForm;
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
	import lsfusion.server.logics.LogicsModule.InsertType;
    import lsfusion.server.physics.admin.reflection.ReflectionPropertyType;
    import lsfusion.server.physics.dev.debug.BooleanDebug;
    import lsfusion.server.physics.dev.debug.DebugInfo;
    import lsfusion.server.physics.dev.debug.PropertyFollowsDebug;
    import lsfusion.server.physics.dev.i18n.LocalizedString;
    import lsfusion.server.physics.dev.integration.external.to.ExternalFormat;
    import lsfusion.interop.session.ExternalHttpMethod;
    import lsfusion.server.physics.dev.integration.external.to.mail.AttachmentFormat;
    import org.antlr.runtime.BitSet;
    import org.antlr.runtime.*;
    
    import javax.mail.Message;
    import java.awt.*;
    import java.util.List;
    import java.util.*;
    import java.time.*;

    import static java.util.Arrays.asList;
    import static lsfusion.server.language.ScriptingLogicsModule.WindowType.*;
}

@lexer::header { 
	package lsfusion.server.language; 
	import lsfusion.server.language.ScriptingLogicsModule;
	import lsfusion.server.language.ScriptParser;
}

@lexer::members {
	public ScriptingLogicsModule self;
	public ScriptParser.State parseState;
	
	@Override
	public void emitErrorMessage(String msg) {
		if (isFirstFullParse() || parseState == ScriptParser.State.PRE) { 
			self.getErrLog().write(msg + "\n");
		}
	}
	
	public boolean isFirstFullParse() {
		return parseState == ScriptParser.State.META_CLASS_TABLE;
	}
	
	@Override
	public String getErrorMessage(RecognitionException e, String[] tokenNames) {
		return self.getErrLog().getErrorMessage(this, super.getErrorMessage(e, tokenNames), e);
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

	public boolean isFirstFullParse() {
		return inMetaClassTableParseState();
	}

	public DebugInfo.DebugPoint getCurrentDebugPoint() {
		return getCurrentDebugPoint(false);
	}

	public DebugInfo.DebugPoint getCurrentDebugPoint(boolean previous) {
		if (!$propertyStatement.isEmpty()) {
			return self.getParser().getGlobalDebugPoint(self.getName(), previous, $propertyStatement::topName, $propertyStatement::topCaption);
		}
		if (!$actionStatement.isEmpty()) {
			return self.getParser().getGlobalDebugPoint(self.getName(), previous, $actionStatement::topName, $actionStatement::topCaption);
		}
		if (!$overridePropertyStatement.isEmpty()) {
			return self.getParser().getGlobalDebugPoint(self.getName(), previous, $overridePropertyStatement::topName, null);
		}
		if (!$overrideActionStatement.isEmpty()) {
			return self.getParser().getGlobalDebugPoint(self.getName(), previous, $overrideActionStatement::topName, null);
		}
		return self.getParser().getGlobalDebugPoint(self.getName(), previous);
	}

	public DebugInfo.DebugPoint getEventDebugPoint() {
		return getCurrentDebugPoint();
	}

	public void setObjectProperty(Object propertyReceiver, String propertyName, Object propertyValue) throws ScriptingErrorLog.SemanticErrorException {
		if (inMainParseState()) {
			$designStatement::design.setObjectProperty(propertyReceiver, propertyName, propertyValue);
		}
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

	@Override
	public void emitErrorMessage(String msg) {
		if (isFirstFullParse() || inPreParseState()) { 
			self.getErrLog().write(msg + "\n");
		}
	}

	@Override
	public String getErrorMessage(RecognitionException e, String[] tokenNames) {
		return self.getErrLog().getErrorMessage(this, super.getErrorMessage(e, tokenNames), e);
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
		|	showDepStatement
		|	globalEventStatement
		|	aspectStatement
		|	tableStatement
		|	loggableStatement
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
	:	'META' ID '(' idList ')'
		statements
		'END'
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
		    self.addScriptedClass($nameCaption.name, $nameCaption.caption, isAbstract, $classData.names, $classData.captions, $classData.parents, isComplex, point);
	}
}
	:	'CLASS'
		('ABSTRACT' {isAbstract = true;} | 'NATIVE' {isNative = true;})?
		('COMPLEX' { isComplex = true; })?
		nameCaption=simpleNameWithCaption
		classData=classInstancesAndParents
	;

extendClassStatement
@after {
	if (inMetaClassTableParseState()) {
		self.extendClass($className.sid, $classData.names, $classData.captions, $classData.parents);
	}
}
	:	'EXTEND' 'CLASS' 
		className=compoundID 
		classData=classInstancesAndParents 
	;

classInstancesAndParents returns [List<String> names, List<LocalizedString> captions, List<String> parents] 
@init {
	$parents = new ArrayList<>();
	$names = new ArrayList<>();
	$captions = new ArrayList<>();
}
	:	(
			'{'
				(firstInstData=simpleNameWithCaption { $names.add($firstInstData.name); $captions.add($firstInstData.caption); }
				(',' nextInstData=simpleNameWithCaption { $names.add($nextInstData.name); $captions.add($nextInstData.caption); })*)?
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
		self.addScriptedGroup($groupNameCaption.name, $groupNameCaption.caption, $extID.val, parent);
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
	:	'REPORTFILES' reportPath (',' reportPath)*
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
	ModalityType modalityType = null;
	int autoRefresh = 0;
	String image = null;
	String title = null;
	DebugInfo.DebugPoint point = getCurrentDebugPoint();
}
@after {
	if (inMainParseState()) {
		$form = self.createScriptedForm($formNameCaption.name, $formNameCaption.caption, point, image, modalityType, autoRefresh);
	}
}
	:	'FORM' 
		formNameCaption=simpleNameWithCaption
		(	('IMAGE' img=stringLiteral { image = $img.val; })
		|	('AUTOREFRESH' refresh=intLiteral { autoRefresh = $refresh.val; })
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
		$formStatement::form.addScriptingGroupObjects(groups, self.getVersion(), getCurrentDebugPoint());
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
		$formStatement::form.addScriptingTreeGroupObject(treeSID, $opts.neighbourObject, $opts.insertType, groups, properties, propertyMappings, self.getVersion(), getCurrentDebugPoint());
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
	:	(	viewType=formGroupObjectViewType { $groupObject.setViewType($viewType.type, $viewType.listType); $groupObject.setPivotOptions($viewType.options); $groupObject.setCustomTypeRenderFunction($viewType.customRenderFunction); }
		|	pageSize=formGroupObjectPageSize { $groupObject.setPageSize($pageSize.value); }
		|	update=formGroupObjectUpdate { $groupObject.setUpdateType($update.updateType); }
		|	relative=formGroupObjectRelativePosition { $groupObject.setNeighbourGroupObject($relative.groupObject, $relative.insertType); }
		|	background=formGroupObjectBackground { $groupObject.setBackground($background.background); }
		|	foreground=formGroupObjectForeground { $groupObject.setForeground($foreground.foreground); }
		|	group=formGroupObjectGroup { $groupObject.setPropertyGroupName($group.formObjectGroup); }
		|   extID=formExtID { $groupObject.setIntegrationSID($extID.extID); }
		|   formExtKey { $groupObject.setIntegrationKey(true); }
		|   formSubReport { $groupObject.setSubReport($formSubReport.pathProperty);  }
		)*
	;
	
formTreeGroupObjectOptions returns [GroupObjectEntity neighbourObject, InsertType insertType]
	:	(	relative=formGroupObjectRelativePosition { $neighbourObject = $relative.groupObject; $insertType = $relative.insertType; }
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
@init {
	List<TypedParameter> extraContext = new ArrayList<>();
}
	:	( sdecl=formSingleGroupObjectDeclaration
		{
		    extraContext.add(self.new TypedParameter($sdecl.className, $sdecl.name));
			$groupObject = new ScriptingGroupObject(null, asList($sdecl.name), asList($sdecl.className), asList($sdecl.caption), asList($sdecl.event), asList($sdecl.extID));
		}

		('PARENT' decl=formExprDeclaration[extraContext] { $properties = asList($decl.property); $propertyMappings = asList($decl.mapping); })? )

	|	mdecl=formMultiGroupObjectDeclaration
		{
			$groupObject = new ScriptingGroupObject($mdecl.groupName, $mdecl.objectNames, $mdecl.classNames, $mdecl.captions, $mdecl.events, $mdecl.extIDs);
		}

        ( '('
		'PARENT' {$properties = new ArrayList<>(); $propertyMappings = new ArrayList<>();} first=formExprDeclaration[extraContext] { $properties.add($first.property); $propertyMappings.add($first.mapping); }
        		(',' next=formExprDeclaration[extraContext] { $properties.add($next.property); $propertyMappings.add($next.mapping); })*
        ')' )?

	;

formGroupObjectViewType returns [ClassViewType type, ListViewType listType, PivotOptions options, String customRenderFunction]
	:	viewType=groupObjectClassViewType { $type = $viewType.type; $listType = $viewType.listType; $options = $viewType.options; $customRenderFunction = $viewType.customRenderFunction; }
	;

groupObjectClassViewType returns [ClassViewType type, ListViewType listType, PivotOptions options, String customRenderFunction]
	:   'PANEL' {$type = ClassViewType.PANEL;}
	|   'TOOLBAR' {$type = ClassViewType.TOOLBAR;}
	|   'GRID' {$type = ClassViewType.LIST;}
    |	lType=listViewType { $listType = $lType.type; $options = $lType.options; $customRenderFunction = $lType.customRenderFunction;}
	;

propertyClassViewType returns [ClassViewType type]
	:   'PANEL' {$type = ClassViewType.PANEL;}
	|   'GRID' {$type = ClassViewType.LIST;}
	|   'TOOLBAR' {$type = ClassViewType.TOOLBAR;}
	;

listViewType returns [ListViewType type, PivotOptions options, String customRenderFunction]
	:   'PIVOT' {$type = ListViewType.PIVOT;} ('DEFAULT' | 'NODEFAULT' {$type = null;})? opt = pivotOptions {$options = $opt.options; }
	|   'MAP' {$type = ListViewType.MAP;}
	|   'CUSTOM' function=stringLiteral {$type = ListViewType.CUSTOM; $customRenderFunction = $function.val;}
	|   'CALENDAR' {$type = ListViewType.CALENDAR;}
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
	
formGroupObjectRelativePosition returns [GroupObjectEntity groupObject, InsertType insertType = null]
	:	'AFTER' go=formGroupObjectEntity { $groupObject = $go.groupObject; $insertType = InsertType.AFTER; }
	|	'BEFORE' go=formGroupObjectEntity { $groupObject = $go.groupObject; $insertType = InsertType.BEFORE; }
	|	'FIRST' { $insertType = InsertType.FIRST; }
	;

formGroupObjectBackground returns [PropertyObjectEntity background]
    :	'BACKGROUND' propObj=formPropertyObject { background = $propObj.property; }
    ;

formGroupObjectForeground returns [PropertyObjectEntity foreground]
    :	'FOREGROUND' propObj=formPropertyObject { foreground = $propObj.property; }
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
		id=classId { $className = $id.sid; }
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
        |   (('NEWSESSION' | 'NESTEDSESSION' { $options.setNested(true); } ) { $options.setNewSession(true); })
		|	'OPTIMISTICASYNC' { $options.setOptimisticAsync(true); }
		|	'COLUMNS' (columnsName=stringLiteral)? '(' ids=nonEmptyIdList ')' { $options.setColumns($columnsName.text, getGroupObjectsList($ids.ids, self.getVersion())); }
		|	'SHOWIF' propObj=formPropertyObject { $options.setShowIf($propObj.property); }
		|	'READONLYIF' propObj=formPropertyObject { $options.setReadOnlyIf($propObj.property); }
		|	'BACKGROUND' propObj=formPropertyObject { $options.setBackground($propObj.property); }
		|	'FOREGROUND' propObj=formPropertyObject { $options.setForeground($propObj.property); }
		|	'IMAGE' propObj=formPropertyObject { $options.setImage($propObj.property); }
		|	'HEADER' propObj=formPropertyObject { $options.setHeader($propObj.property); }
		|	'FOOTER' propObj=formPropertyObject { $options.setFooter($propObj.property); }
		|	viewType=propertyClassViewType { $options.setViewType($viewType.type); }
		|	pgt=propertyGroupType { $options.setAggrFunc($pgt.type); }
		|	pla=propertyLastAggr { $options.setLastAggr($pla.properties, $pla.desc); }
		|	pf=propertyFormula { $options.setFormula($pf.formula, $pf.operands); }
		|	'DRAW' toDraw=formGroupObjectEntity { $options.setToDraw($toDraw.groupObject); }
		|	'BEFORE' pdraw=formPropertyDraw { $options.setNeighbourPropertyDraw($pdraw.property, $pdraw.text); $options.setInsertType(InsertType.BEFORE); }
		|	'AFTER'  pdraw=formPropertyDraw { $options.setNeighbourPropertyDraw($pdraw.property, $pdraw.text); $options.setInsertType(InsertType.AFTER); }
		|	'FIRST' { $options.setInsertType(InsertType.FIRST); }
		|	'QUICKFILTER' pdraw=formPropertyDraw { $options.setQuickFilterPropertyDraw($pdraw.property); }
		|	'ON' et=formEventType prop=formActionObject { $options.addEventAction($et.type, $prop.action); }
		|	'ON' 'CONTEXTMENU' (c=localizedStringLiteral)? prop=formActionObject { $options.addContextMenuAction($c.val, $prop.action); }
		|	'ON' 'KEYPRESS' key=stringLiteral prop=formActionObject { $options.addKeyPressAction($key.val, $prop.action); }
		|	'EVENTID' id=stringLiteral { $options.setEventId($id.val); }
		|	'ATTR' { $options.setAttr(true); }
		|   'IN' groupName=compoundID { $options.setGroupName($groupName.sid); }
		|   'EXTID' id=stringLiteral { $options.setIntegrationSID($id.val); }
		|   po=propertyDrawOrder { $options.setOrder($po.order); }
		|   'FILTER' { $options.setFilter(true); }
		|   'COLUMN' { $options.setPivotColumn(true); }
		|   'ROW' { $options.setPivotRow(true); }
		|   'MEASURE' { $options.setPivotMeasure(true); }
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

formPropertyObject returns [PropertyObjectEntity property = null]
	:   fd = designOrFormPropertyObject[null] { $property = $fd.property; }	
	;

designPropertyObject returns [PropertyObjectEntity property = null]
	:   fd = designOrFormPropertyObject[$designStatement::design] { $property = $fd.property; }
	;

// may be used in design
designOrFormPropertyObject[ScriptingFormView design] returns [PropertyObjectEntity property = null]
@init {
    AbstractFormPropertyUsage propUsage = null;
}
	:	expr=designOrFormExprDeclaration[design, null] { propUsage = new FormLPUsage($expr.property, $expr.mapping); }
		{
			if (inMainParseState()) {
			    if(design != null)
			        $property = design.addPropertyObject(propUsage);
                else
				    $property = $formStatement::form.addPropertyObject(propUsage);
			}
		}
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
		|	cid='DELETE'	{ systemName = $cid.text; }
;

formPredefinedOrActionUsage[List<String> mapping] returns [BaseFormActionOrPropertyUsage propUsage] // actually FormPredefinedUsage or FormActionUsage
	:	('ACTION' pu = propertyUsage { $propUsage = new ActionUsage($pu.propUsage).createFormUsage(mapping); }) 
	    |	
	    fpd = formPredefinedUsage[mapping] { $propUsage = $fpd.propUsage; }

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
		decl=formExprDeclaration[null] { properties.add($decl.property); propertyMappings.add($decl.mapping);}
	    (',' decl=formExprDeclaration[null] { properties.add($decl.property); propertyMappings.add($decl.mapping);})*
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
}
@after {
	if (inMainParseState()) {
		$formStatement::form.addScriptedFormEvents(actions, types, self.getVersion());
	}
}
	:	'EVENTS'
		decl=formEventDeclaration { actions.add($decl.action); types.add($decl.type); }
		(',' decl=formEventDeclaration { actions.add($decl.action); types.add($decl.type); })*
	;


formEventDeclaration returns [ActionObjectEntity action, Object type]
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
		|	'QUERYCLOSE'	 { $type = FormEventType.QUERYCLOSE; }
		| 	'CHANGE' objectId=ID { $type = $objectId.text; }
		)
		faprop=formActionObject { $action = $faprop.action; }
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
	String key = null;
}
    :   'FILTER' caption=localizedStringLiteral fd=formExprDeclaration[null] (keystroke=stringLiteral {key = $keystroke.val;})? setDefault=filterSetDefault
        {
            $filter = new RegularFilterInfo($caption.val, key, $fd.property, $fd.mapping, $setDefault.isDefault);
        }
    ;
	
formExprDeclaration[List<TypedParameter> extraContext] returns [LP property, ImOrderSet<String> mapping, List<ResolveClassSet> signature]
    :   dfe = designOrFormExprDeclaration[null, extraContext] { $property = $dfe.property; $mapping = $dfe.mapping; $signature = $dfe.signature; }
    ;

designOrFormExprDeclaration[ScriptingFormView design, List<TypedParameter> extraContext] returns [LP property, ImOrderSet<String> mapping, List<ResolveClassSet> signature]
@init {
	List<TypedParameter> context = new ArrayList<>();
	if (inMainParseState()) {
	    if(design != null)
	        context = design.getTypedObjectsNames(self.getVersion());
	    else
		    context = $formStatement::form.getTypedObjectsNames(self.getVersion());
	}
	if(extraContext != null)
	    context.addAll(extraContext);
}
@after {
	if (inMainParseState()) {
		$mapping = self.getUsedNames(context, $expr.property.usedParams);
		$signature = self.getUsedClasses(context, $expr.property.usedParams);
	}	
}
	:	expr=propertyExpression[context, false] { if (inMainParseState()) { $property = self.checkSingleParam($expr.property).getLP(); } }
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
	:	expr=propertyExpressionOrTrivialLA[context]
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

formOrderByList
@init {
	boolean ascending = true;
	List<PropertyDrawEntity> properties = new ArrayList<>();
	List<Boolean> orders = new ArrayList<>();
}
@after {
	if (inMainParseState()) {
		$formStatement::form.addScriptedDefaultOrder(properties, orders, self.getVersion());
	}
}
	:	'ORDERS' orderedProp=formPropertyDrawWithOrder { properties.add($orderedProp.property); orders.add($orderedProp.order); }
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
        { if(inMainParseState()) { $ci = self.checkTLAInExpr($exprOrNotExpr.property, $exprOrNotExpr.ci); } }
;

propertyExpressionOrTrivialLA[List<TypedParameter> context] returns [LPWithParams property, LPTrivialLA la]
    :   exprOrNotExpr=propertyExpressionOrNot[context, false, false] { $property = $exprOrNotExpr.property;  }
        { if(inMainParseState()) { $la = self.checkCIInExpr($exprOrNotExpr.ci); } }
;

propertyExpressionOrNot[List<TypedParameter> context, boolean dynamic, boolean needFullContext] returns [LPWithParams property, LPNotExpr ci]
@init {
	DebugInfo.DebugPoint point = getCurrentDebugPoint();
}
@after{
    if (inMainParseState()) {
        if($ci == null)
            $property = self.propertyExpressionCreated($property, context, needFullContext);

        LP propertyCreated = null;
        if($property != null)
            propertyCreated = $property.getLP();
        else if(!($ci instanceof LPTrivialLA))
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
}
@after {
	if (inMainParseState()) {
	    if(rightProp != null)
		    $property = self.addScriptedLikeProp(leftProp, rightProp);
	    else
		    $property = leftProp;
	}
}
	:	lhs=additiveORPE[context, dynamic] { leftProp = $lhs.property; $ci = $lhs.ci; }
		( { if(inMainParseState()) { $ci = self.checkNotExprInExpr($lhs.property, $ci); } }
		'LIKE'
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
	:	MINUS expr=unaryMinusPE[context, dynamic] { minusWas = true; } { if(inMainParseState()) { self.checkNotExprInExpr($expr.property, $expr.ci); } }
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
	:	param=singleParameter[context, dynamic] { $property = $param.property; }
	|	expr=expressionFriendlyPD[context, dynamic] { $property = $expr.property; $ci = $expr.ci; }
	;

singleParameter[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	String className = null;
}
@after {
	if (inMainParseState()) {
		$property = new LPWithParams(null, self.getParamIndex(TP(className, $paramName.text), $context, $dynamic, insideRecursion));
	}
}
	:	(clsId=classId { className = $clsId.sid; })? paramName=parameter
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
	|	castDef=castPropertyDefinition[context, dynamic] { $property = $castDef.property; }
	|	sessionDef=sessionPropertyDefinition[context, dynamic] { $property = $sessionDef.property; }
	|	signDef=signaturePropertyDefinition[context, dynamic] { $property = $signDef.property; }
	|	activeTabDef=activeTabPropertyDefinition[context, dynamic] { $property = $activeTabDef.property; }
	|	constDef=constantProperty { $property = new LPWithParams($constDef.property); }
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
	|	formulaProp=formulaPropertyDefinition { $property = $formulaProp.property; $signature = $formulaProp.signature; }
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
    DebugInfo.DebugPoint classDebugPoint, exprDebugPoint;
}
@after {
	if (inMainParseState()) {
		LPContextIndependent ci = self.addScriptedAGProp(context, $aggrClass.sid, $whereExpr.property, classDebugPoint, exprDebugPoint, innerPD);
		$property = ci.property;
		$usedContext = ci.usedContext;		
		$signature = ci.signature;
	}
}
	:	'AGGR'
	    { classDebugPoint = getEventDebugPoint(); }
	    aggrClass=classId
	    'WHERE'
	    { exprDebugPoint = getEventDebugPoint(); }
	    whereExpr=propertyExpression[context, dynamic]
	;
	
groupCDPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, LPContextIndependent ci]
@init {
	List<TypedParameter> groupContext = new ArrayList<>(context);
}
@after {
	if (inMainParseState()) {
		Pair<LPWithParams, LPContextIndependent> peOrCI = self.addScriptedCDGProp(context.size(), $exprList.props, $gp.type, $gp.mainProps, $gp.orderProps, $gp.ascending, $gp.whereProp, groupContext);
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
    	gt=groupingType { $type = $gt.type; }
        mainList=nonEmptyPropertyExpressionList[context, true] { $mainProps = $mainList.props; }
        ('ORDER' ('DESC' { $ascending = false; } )?
        orderList=nonEmptyPropertyExpressionList[context, true] { $orderProps = $orderList.props; })?
        ('WHERE' whereExpr=propertyExpression[context, true] { $whereProp = $whereExpr.property; } )?
    ;


groupingType returns [GroupingType type]
	:	'SUM' 	{ $type = GroupingType.SUM; }
	|	'MAX' 	{ $type = GroupingType.MAX; }
	|	'MIN' 	{ $type = GroupingType.MIN; }
	|	'CONCAT' { $type = GroupingType.CONCAT; }
	|	'AGGR' { $type = GroupingType.AGGR; }
	|	'NAGGR' { $type = GroupingType.NAGGR; }
	|	'EQUAL'	{ $type = GroupingType.EQUAL; }	
	|	'LAST'	{ $type = GroupingType.LAST; }
	;


partitionPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> paramProps = new ArrayList<>();
	NamedPropertyUsage pUsage = null;
	PartitionType type = null;
	int groupExprCnt = 0;
	boolean strict = false;
	int precision = 0;
	boolean ascending = true;
	boolean useLast = true;
}
@after {
	if (inMainParseState()) {
		$property = self.addScriptedPartitionProp(type, pUsage, strict, precision, ascending, useLast, groupExprCnt, paramProps, context);
	}
}
	:	'PARTITION' 
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
	    $signature = $paramClassNames.ids == null ? self.getClassesFromTypedParams(context) : self.createClassSetsFromClassNames($paramClassNames.ids);
		$property = self.addScriptedDProp($returnClass.sid, $paramClassNames.ids, $signature, localProp, innerPD, false, nestedType);
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
        $signature = $paramClassNames.ids == null ? self.getClassesFromTypedParams(context) : self.createClassSetsFromClassNames($paramClassNames.ids);
        $property = self.addScriptedAbstractProp(type, $returnClass.sid, $paramClassNames.ids, $signature, isExclusive, isChecked, isLast, innerPD);
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
		$signature = $paramClassNames.ids == null ? self.getClassesFromTypedParams(context) : self.createClassSetsFromClassNames($paramClassNames.ids);
		$action = self.addScriptedAbstractAction(type, $paramClassNames.ids, $signature, isExclusive, isChecked, isLast);
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
}
@after {
	if (inMainParseState()) {
		$property = self.addScriptedRProp(recursiveContext, $zeroStep.property, $nextStep.property, cycleType);			
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
	:   ptype=PRIMITIVE_TYPE '(' expr=propertyExpression[context, dynamic] ')'
	;

concatPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inMainParseState()) {
		$property = self.addScriptedConcatProp($separator.val, $list.props);
	}
}
	:   'CONCAT' separator=stringLiteral ',' list=nonEmptyPropertyExpressionList[context, dynamic]
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
	: 	'CLASS' '(' expr=propertyExpression[context, dynamic] ')'
	;

activeTabPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inMainParseState()) {
		$property = self.addScriptedActiveTabProp($fc.component);
	}
}
	: 	'ACTIVE' 'TAB' fc = formComponentID
	;

formulaPropertyDefinition returns [LP property, List<ResolveClassSet> signature]
@init {
	String className = null;
	boolean hasNotNullCondition = false;
}
@after {
	if (inMainParseState()) {
		$property = self.addScriptedSFProp(className, $synt.types, $synt.strings, hasNotNullCondition);
		$signature = Collections.<ResolveClassSet>nCopies($property.listInterfaces.size(), null);
	}
}
	:	'FORMULA'
		('NULL' { hasNotNullCondition = true; })?
		(clsName=classId { className = $clsName.sid; })?
		synt=formulaPropertySyntaxList
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
	String className = null;
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
        ptype=PRIMITIVE_TYPE { if(inMainParseState()) dataClass = (DataClass)self.findClass($ptype.text); }
        (varID=ID EQ)?
        (   pid=ID { $id = $pid.text; $literal = false; }
        |	sLiteral=stringLiteral { $id = $sLiteral.val; $literal = true; }
        )
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
	boolean hasHeader = false;
	boolean noEscape = false;
	String charset = null;
	boolean attr = false;
	LPWithParams root = null;
	LPWithParams tag = null;

}
@after {
	if (inMainParseState()) {
			$action = self.addScriptedExportAction(context, format, $plist.aliases, $plist.literals, $plist.properties, $whereExpr.property, $pUsage.propUsage,
			                                                 root, tag, separator, !hasHeader, noEscape, selectTop, charset, attr, orderProperties, orderDirections);
	}
} 
	:	'EXPORT'
	    (type = exportSourceFormat [context, dynamic] { format = $type.format; separator = $type.separator; hasHeader = $type.hasHeader; noEscape = $type.noEscape;
	                                                    charset = $type.charset; root = $type.root; tag = $type.tag; attr = $type.attr; })?
		('TOP' selectTop = intLiteral)?
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

nonEmptyAliasedPropertyExpressionList[List<TypedParameter> context, boolean dynamic] returns [List<String> aliases = new ArrayList<>(), List<Boolean> literals = new ArrayList<>(), List<LPWithParams> properties = new ArrayList<>()]
@init {
    String alias;
}
    :
        expr=exportAliasedPropertyExpression[context, dynamic] { $aliases.add($expr.alias); $literals.add($expr.literal); $properties.add($expr.property); }
		(',' expr=exportAliasedPropertyExpression[context, dynamic] { $aliases.add($expr.alias); $literals.add($expr.literal); $properties.add($expr.property); } )*
	;

exportAliasedPropertyExpression[List<TypedParameter> context, boolean dynamic] returns [String alias = null, Boolean literal = null, LPWithParams property]
    :
        ( { (input.LA(1)==ID || input.LA(1)==STRING_LITERAL) && input.LA(2)==EQ }?
          (   simpleName=ID { $alias = $simpleName.text; $literal = false; }
          |	  sLiteral=stringLiteral { $alias = $sLiteral.val; $literal = true; }
          )
          EQ
        )?
        expr=propertyExpression[context, dynamic] { $property = $expr.property; }
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
		$action = self.addScriptedNewThreadAction($aDB.action, $connExpr.property, $periodExpr.property, $delayExpr.property);
	}
}
	:	'NEWTHREAD' aDB=keepContextFlowActionDefinitionBody[context, dynamic]
	    (
	    	(   'CONNECTION' connExpr=propertyExpression[context, dynamic]
		    |   'SCHEDULE' ('PERIOD' periodExpr=propertyExpression[context, dynamic])? ('DELAY' delayExpr=propertyExpression[context, dynamic])?
    	    )
    	    ';'
        )?
	;

newExecutorActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
	List<LPWithParams> props = new ArrayList<>();
	List<LP> localProps = new ArrayList<LP>();
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedNewExecutorAction($aDB.action, $threadsExpr.property);
	}
}
	:	'NEWEXECUTOR' aDB=keepContextFlowActionDefinitionBody[context, dynamic] 'THREADS' threadsExpr=propertyExpression[context, dynamic] ';'
	;

newSessionActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
	List<NamedPropertyUsage> migrateSessionProps = Collections.emptyList();
	boolean migrateAllSessionProps = false;
	boolean isNested = false;
	boolean singleApply = false;
	boolean newSQL = false;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedNewSessionAProp($aDB.action, migrateSessionProps, migrateAllSessionProps, isNested, singleApply, newSQL);
	}
}
	:	(	'NEWSESSION' ('NEWSQL' { newSQL = true; })? (mps=nestedPropertiesSelector { migrateAllSessionProps = $mps.all; migrateSessionProps = $mps.props; })?
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
			(	pid=ID { $id = $pid.text; $literal = false; }
			|	sLiteral=stringLiteral { $id = $sLiteral.val; $literal = false; }
			)
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
	            ('CHARSET' charsetVal = stringLiteral { $charset = $charsetVal.val; })?
	            )
	|	'XML'	{ $format = FormIntegrationType.XML; } (
	            ('ROOT' rootProperty = propertyExpression[context, dynamic] {$root = $rootProperty.property; })?
	            ('ATTR' { $attr = true; })?
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
	|	flexCharWidthSetting [property]
	|	charWidthSetting [property]
	|	changeKeySetting [property]
	|	changeMouseSetting [property]
	|   '@@' ann = ID { ps.annotation = $ann.text; }
    ;

semiPropertyOption[LP property, String propertyName, LocalizedString caption, PropertySettings ps, List<TypedParameter> context]
    :	semiActionOrPropertyOption[property, propertyName, caption, ps, context]
    |   persistentSetting [ps]
	|	complexSetting [ps]
	|	prereadSetting [ps]
	|	noHintSetting [ps]
	|	tableSetting [ps]
	|   defaultCompareSetting [property]
	|	autosetSetting [property]
	|	regexpSetting [property]
	|	loggableSetting [ps]
	|	echoSymbolsSetting [property]
	|	indexSetting [property]
	|	setNotNullSetting [ps]
	|	aggrSetting [property]
	|	eventIdSetting [property]
    ;

semiActionOption[LA action, String actionName, LocalizedString caption, ActionSettings ps, List<TypedParameter> context]
    :	semiActionOrPropertyOption[action, actionName, caption, ps, context]
    |   imageSetting [action]
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

persistentSetting [PropertySettings ps]
	:	'MATERIALIZED' { ps.isPersistent = true; }
	;

complexSetting [PropertySettings ps]
	:	('COMPLEX' { ps.isComplex = true; } | 'NOCOMPLEX' { ps.isComplex = false; } )
	;

prereadSetting [PropertySettings ps]
	:	'PREREAD' { ps.isPreread = true; }
	;

noHintSetting [PropertySettings ps]
	:	'NOHINT' { ps.noHint = true; }
	;

tableSetting [PropertySettings ps]
	:	'TABLE' tbl = compoundID { ps.table = $tbl.sid; }
	;

loggableSetting [PropertySettings ps]
	:	'LOGGABLE'  { ps.isLoggable = true; }
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
						 }
    ;
annotationSetting [PropertySettings ps]
	:
	    '@@' ann = ID { ps.annotation = $ann.text; }
	;

notNullSetting returns [DebugInfo.DebugPoint debugPoint, BooleanDebug toResolve = null, Event event]
@init {
    $debugPoint = getEventDebugPoint();
}
	:	'NONULL'
	    (dt = notNullDeleteSetting { $toResolve = new BooleanDebug($dt.debugPoint); })?
	    et=baseEvent { $event = $et.event; }
	;


shortcutSetting [LA property, LocalizedString caption]
@after {
	if (inMainParseState()) {
		self.addToContextMenuFor(property, $c.val != null ? $c.val : caption, $usage.propUsage);
	}
}
	:	'ASON' 'CONTEXTMENU' (c=localizedStringLiteral)? usage = actionOrPropertyUsage
	;

asonEventActionSetting [LA property]
@init {
	String eventActionSID = null;
}
@after {
	if (inMainParseState()) {
		self.setAsEventActionFor(property, $et.type, $usage.propUsage);
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
		self.setImage(property, $path.val);
	}
}
	:	'IMAGE' path = stringLiteral
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

regexpSetting [LAP property]
@init {
	String message = null;
}
@after {
	if (inMainParseState()) {
		self.setRegexp(property, $exp.val, message);
	}
}
	:	'REGEXP' exp = stringLiteral
		(mess = stringLiteral { message = $mess.val; })?
	;

echoSymbolsSetting [LAP property]
@after {
	if (inMainParseState()) {
		self.setEchoSymbols(property);
	}
}
	:	'ECHO'
	;

indexSetting [LP property]
@after {
	if (inMainParseState()) {
		self.addScriptedIndex(property);
	}
}
	:	'INDEXED'
	;

notNullDeleteSetting returns [DebugInfo.DebugPoint debugPoint]
@init {
    $debugPoint = getEventDebugPoint();
}
    :   'DELETE'
	;

onEditEventSetting [LAP property, List<TypedParameter> context]
@after {
	if (inMainParseState()) {
		self.setScriptedEventAction(property, $et.type, $aDB.action);
	}
}
	:	'ON' et=formEventType
		aDB=listTopContextDependentActionDefinitionBody[context, false, false]
	;

formEventType returns [String type]
	:	'CHANGE' { $type = ServerResponse.CHANGE; }
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
	:	'ON' 'CONTEXTMENU' (c=localizedStringLiteral)?
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
	|	mailADB=emailActionDefinitionBody[context, dynamic] { $action = $mailADB.action; }
	|	evalADB=evalActionDefinitionBody[context, dynamic] { $action = $evalADB.action; }
	|	drillDownADB=drillDownActionDefinitionBody[context, dynamic] { $action = $drillDownADB.action; }
	|	readADB=readActionDefinitionBody[context, dynamic] { $action = $readADB.action; }
	|	writeADB=writeActionDefinitionBody[context, dynamic] { $action = $writeADB.action; }
	|	importFormADB=importFormActionDefinitionBody[context, dynamic] { $action = $importFormADB.action; }
	|	activeFormADB=activeFormActionDefinitionBody[context, dynamic] { $action = $activeFormADB.action; }
	|	activateADB=activateActionDefinitionBody[context, dynamic] { $action = $activateADB.action; }
    |   externalADB=externalActionDefinitionBody[context, dynamic] { $action = $externalADB.action;}
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
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedShowFAProp($mf.mapped, $mf.props, syncType, windowType, manageSession, formSessionScope, checkOnOk, noCancel, readOnly,
		                                     objectsContext, contextFilters, context);
	}
}
	:	'SHOW' mf=mappedForm[context, null, dynamic]
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
	:	'FLOAT' { $val = WindowFormType.FLOAT; }
	|	'DOCKED' { $val = WindowFormType.DOCKED; }
	;

printActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
    List<TypedParameter> objectsContext = null;
    List<LPWithParams> contextFilters = new ArrayList<>();

	FormPrintType printType = null;
    Boolean syncType = null;
    Integer selectTop = null;
    LPWithParams printerProperty = null;
    LPWithParams sheetNameProperty = null;
    LPWithParams passwordProperty = null;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedPrintFAProp($mf.mapped, $mf.props, printType, $pUsage.propUsage, syncType, selectTop, printerProperty, sheetNameProperty, passwordProperty,
		                                      objectsContext, contextFilters, context);
	}
}
	:	'PRINT' mf=mappedForm[context, null, dynamic] {
            if(inMainParseState())
                 objectsContext = self.getTypedObjectsNames($mf.mapped);
        }
        (cf = contextFiltersClause[context, objectsContext] { contextFilters.addAll($cf.contextFilters); })?
		(   ( // static - jasper
            (   'XLS'  { printType = FormPrintType.XLS; } ('SHEET' sheet = propertyExpression[context, dynamic] { sheetNameProperty = $sheet.property; })? ('PASSWORD' pwd = propertyExpression[context, dynamic] { passwordProperty = $pwd.property; })?
            |	'XLSX' { printType = FormPrintType.XLSX; } ('SHEET' sheet = propertyExpression[context, dynamic] { sheetNameProperty = $sheet.property; })? ('PASSWORD' pwd = propertyExpression[context, dynamic] { passwordProperty = $pwd.property; })?
            |	'PDF' { printType = FormPrintType.PDF; }
            |	'DOC'  { printType = FormPrintType.DOC; }
            |	'DOCX' { printType = FormPrintType.DOCX; }
            |	'RTF' { printType = FormPrintType.RTF; }
            |	'HTML' { printType = FormPrintType.HTML; }
            )
            ('TO' pUsage=propertyUsage)?
            )
        |   ( // static - rest
                'MESSAGE' { printType = FormPrintType.MESSAGE; }
                (sync = syncTypeLiteral { syncType = $sync.val; })?
                ('TOP' top = intLiteral { selectTop = $top.val; } )?
            )
        |   ( // static - interactive
            { printType = FormPrintType.PRINT; }
            (   'PREVIEW'
            |   'NOPREVIEW' { printType = FormPrintType.AUTO; }
            )?
		    (sync = syncTypeLiteral { syncType = $sync.val; })?
            ('TO' pe = propertyExpression[context, dynamic] { printerProperty = $pe.property; })?
            )
        )
	;

exportFormActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
    List<TypedParameter> objectsContext = null;
    List<LPWithParams> contextFilters = new ArrayList<>();

    FormIntegrationType format = null;
	String separator = null;
	boolean hasHeader = false;
	boolean noEscape = false;
	String charset = null;
	boolean attr = false;
	LPWithParams root = null;
	LPWithParams tag = null;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedExportFAProp($mf.mapped, $mf.props, format, root, tag, attr, !hasHeader, separator, noEscape, selectTop, charset, $pUsage.propUsage, $pUsages.pUsages,
		                                       objectsContext, contextFilters, context);
	}
}
	:	'EXPORT' mf=mappedForm[context, null, dynamic] {
	        if(inMainParseState())
                objectsContext = self.getTypedObjectsNames($mf.mapped);
	    }
	    (cf = contextFiltersClause[context, objectsContext] { contextFilters.addAll($cf.contextFilters); })?
		(type = exportSourceFormat [context, dynamic] { format = $type.format; separator = $type.separator; hasHeader = $type.hasHeader; noEscape = $type.noEscape;
        	                                                    charset = $type.charset; root = $type.root; tag = $type.tag; attr = $type.attr; })?
		('TOP' selectTop = intLiteral)?
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

exportSourceFormat [List<TypedParameter> context, boolean dynamic] returns [FormIntegrationType format, String separator, boolean hasHeader, boolean noEscape, String charset, LPWithParams root, LPWithParams tag, boolean attr]
	:	'CSV' { $format = FormIntegrationType.CSV; } (separatorVal = stringLiteral { $separator = $separatorVal.val; })? (hasHeaderVal = hasHeaderOption { $hasHeader = $hasHeaderVal.hasHeader; })? (noEscapeVal = noEscapeOption { $noEscape = $noEscapeVal.noEscape; })? ('CHARSET' charsetVal = stringLiteral { $charset = $charsetVal.val; })?
    |	'DBF' { $format = FormIntegrationType.DBF; } ('CHARSET' charsetVal = stringLiteral { $charset = $charsetVal.val; })?
    |   'XLS' { $format = FormIntegrationType.XLS; } (hasHeaderVal = hasHeaderOption { $hasHeader = $hasHeaderVal.hasHeader; })?
    |   'XLSX' { $format = FormIntegrationType.XLSX; } (hasHeaderVal = hasHeaderOption { $hasHeader = $hasHeaderVal.hasHeader; })?
	|	'JSON' { $format = FormIntegrationType.JSON; } ('CHARSET' charsetVal = stringLiteral { $charset = $charsetVal.val; })?
	|	'XML' { $format = FormIntegrationType.XML; } ('ROOT' rootProperty = propertyExpression[context, dynamic] {$root = $rootProperty.property; })? ('TAG' tagProperty = propertyExpression[context, dynamic] {$tag = $tagProperty.property; })? ('ATTR' { $attr = true; })? ('CHARSET' charsetVal = stringLiteral { $charset = $charsetVal.val; })?
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

    LPWithParams changeProp = null;

    boolean assign = false;
    boolean constraintFilter = false;

    DebugInfo.DebugPoint assignDebugPoint = null;
}
@after {
    $props = new FormActionProps(in, inNull, out, outParamNum, outNull, outProp, constraintFilter, assign, changeProp, assignDebugPoint);
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
//            ('TO' pUsage=propertyUsage { outProp = $pUsage.propUsage; } )?
            (('CONSTRAINTFILTER' { constraintFilter = true; } ) (EQ consExpr=propertyExpression[context, dynamic] { changeProp = $consExpr.property; } )?)?
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
}
@after {
	if (inMainParseState()) {

	    List<ResolveClassSet> contextParams = self.getClassesFromTypedParams(context);

	    if($code.val == null)
	        $action = self.addScriptedInternalAction($classN.val, classes, contextParams, allowNullValue);
	    else
		    $action = self.addScriptedInternalAction($code.val, allowNullValue);
		$signature = classes == null ? (contextParams.isEmpty() ? Collections.<ResolveClassSet>nCopies($action.listInterfaces.size(), null) : contextParams) : self.createClassSetsFromClassNames(classes);
	}
}
	:	'INTERNAL'
        (   
            classN = stringLiteral ('(' cls=classIdList ')' { classes = $cls.ids; })? 
		|   code = codeLiteral
        )
	    ('NULL' { allowNullValue = true; })?
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
      } else if($type.format == ExternalFormat.HTTP) {
        $action = self.addScriptedExternalHTTPAction($type.clientAction, $type.method, $type.conStr, $type.bodyUrl, $type.headers, $type.cookies, $type.headersTo, $type.cookiesTo, params, context, $tl.propUsages);
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

externalFormat [List<TypedParameter> context, boolean dynamic] returns [ExternalFormat format, ExternalHttpMethod method, boolean clientAction, LPWithParams conStr, LPWithParams bodyUrl, LPWithParams exec, NamedPropertyUsage headers, NamedPropertyUsage cookies, NamedPropertyUsage headersTo, NamedPropertyUsage cookiesTo, boolean eval = false, boolean action = false, String charset]
	:	'SQL'	{ $format = ExternalFormat.DB; } conStrVal = propertyExpression[context, dynamic] { $conStr = $conStrVal.property; } 'EXEC' execVal = propertyExpression[context, dynamic] { $exec = $execVal.property; }
	|	'HTTP'	{ $format = ExternalFormat.HTTP; } ('CLIENT' { $clientAction = true; })?
	            (methodVal = externalHttpMethod { $method = $methodVal.method; })? conStrVal = propertyExpression[context, dynamic] { $conStr = $conStrVal.property; }
	            ('BODYURL' bodyUrlVal = propertyExpression[context, dynamic] { $bodyUrl = $bodyUrlVal.property; })?
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
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedEmailProp(fromProp, subjProp, bodyProp, recipTypes, recipProps, attachFileNames, attachFiles, syncType);
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
		(	(	'ATTACH' attachFile=propertyExpression[context, dynamic] { attachFiles.add($attachFile.property); }
				{ LPWithParams attachFileName = null;}
                ('NAME' attachFileNameExpr=propertyExpression[context, dynamic] { attachFileName = $attachFileNameExpr.property; } )?
                { attachFileNames.add(attachFileName); }
			)
		)*
		(sync = syncTypeLiteral{ syncType = $sync.val; })?
	;
	
emailActionFormObjects[List<TypedParameter> context, boolean dynamic] returns [OrderedMap<String, LPWithParams> mapObjects]
@init {
	$mapObjects = new OrderedMap<>();
}

	:	(	'OBJECTS'
			obj=ID EQ objValueExpr=propertyExpression[context, dynamic] { $mapObjects.put($obj.text, $objValueExpr.property); }
			(',' obj=ID EQ objValueExpr=propertyExpression[context, dynamic] { $mapObjects.put($obj.text, $objValueExpr.property); })*
		)?
	;

confirmActionDefinitionBody[List<TypedParameter> context] returns [LAWithParams action]
@init {
    List<TypedParameter> newContext;
    boolean yesNo = false;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedConfirmProp($pe.property, $dDB.action, $dDB.elseAction, yesNo, context, newContext);
	}
}
	:	'ASK'
        pe=propertyExpression[context, false]
        { newContext = new ArrayList<TypedParameter>(context); }
	    ((varID=ID { if (inMainParseState()) { self.getParamIndex(self.new TypedParameter("BOOLEAN", $varID.text), newContext, true, insideRecursion); } } EQ)? 'YESNO' { yesNo = true;} )?
        dDB=doInputBody[context, newContext]
	;
		
messageActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@init {
    boolean noWait = false;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedMessageProp($pe.property, noWait);
	}
}
	:	'MESSAGE'
	    pe=propertyExpression[context, dynamic]
	    (sync = syncTypeLiteral { noWait = !$sync.val; })?
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
	UpdateType type = UpdateType.FIRST;
	List<String> objNames = new ArrayList<>();
	List<LPWithParams> lps = new ArrayList<>(); 
}
@after {
	if (inMainParseState()) {
		$action = obj != null ? self.addScriptedObjectSeekProp($obj.sid, $pe.property, type)
		                      : self.addScriptedGroupObjectSeekProp($gobj.sid, objNames, lps, type);
	}
}
	:	'SEEK' ('FIRST' | 'LAST' { type = UpdateType.LAST; } | 'NULL' { type = UpdateType.NULL; })?
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
	
drillDownActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LAWithParams action]
@after {
	if (inMainParseState()) {
		$action = self.addScriptedDrillDownAction($expr.property);
	}
}
	:	'DRILLDOWN' expr=propertyExpression[context, dynamic]
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
	DebugInfo.DebugPoint assignDebugPoint = null;

    NamedPropertyUsage outProp = null;
    LPWithParams changeProp = null;
}
@after {
	if (inMainParseState()) {
		$action = self.addScriptedInputAProp($in.dataClass, $in.initValue, outProp, $dDB.action, $dDB.elseAction, context, newContext, assign, changeProp, assignDebugPoint);
	}
}
	:	'INPUT'
	    in=mappedInput[newContext]
        ( { assignDebugPoint = getCurrentDebugPoint(); } 
            'CHANGE' { assign = true; }
            (EQ consExpr=propertyExpression[context, false])? { changeProp = $consExpr.property; }
        )?
//		('TO' pUsage=propertyUsage { outProp = $pUsage.propUsage; } )?
        dDB=doInputBody[context, newContext]
	;
	
mappedInput[List<TypedParameter> context] returns [DataClass dataClass, LPWithParams initValue]
@init {
    String varName = "object"; // for INPUT =f() CHANGE and INPUT LONG;
}
@after {
	if (inMainParseState()) {
		$dataClass = self.getInputDataClass(varName, context, $ptype.text, $pe.property, insideRecursion);
		$initValue = $pe.property;
	}
}
    :    
    (
        (varID=ID EQ { varName = $varID.text; } )?
        ptype=PRIMITIVE_TYPE
    )
    |	
    ( 
        (varID=ID { varName = $varID.text; } )?
        EQ pe=propertyExpression[context, false]
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
		(	'FORM' fName=compoundID { form = self.findForm($fName.sid); }
		|	'TAB' fc = formComponentID { form = $fc.form; component = $fc.component; }
		|   'PROPERTY' fp = formPropertyID { propertyDraw = $fp.propertyDraw; }
		)
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

nestedPropertiesSelector returns[boolean all = false, List<NamedPropertyUsage> props = new ArrayList<>()]
    :   'NESTED'
            (   'LOCAL' { $all = true; }
            |   (
            	'(' list=nonEmptyPropertyUsageList { $props = $list.propUsages; } ')'
            	)
            )
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
	boolean isBreak = true;
}
@after {
	if (inMainParseState()) {
		$action = self.getTerminalFlowAction(isBreak);
	}
}
	:	'BREAK'
	|	'RETURN' { isBreak = false; }
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
}
@after {
	if (inMainParseState()) {
        self.addImplementationToAbstractAction($prop.propUsage, $list.params, action, when);
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
	DebugInfo.DebugPoint debugPoint = null; 
	if (inMainParseState()) {
		debugPoint = getEventDebugPoint();
	}
}
@after {
	if (inMainParseState()) {
		self.addScriptedConstraint(property, $et.event, checked, propUsages, $message.property.getLP(), properties, debugPoint);
	}
}
	:	'CONSTRAINT'
		et=baseEvent	
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
        et=baseEvent { $event = $et.event; }
        {
            if (inMainParseState()) {
                self.setPrevScope($et.event);
            }
        }
        expr = propertyExpression[context, false]
		('RESOLVE' 
			('LEFT' {$pfollows.add(new PropertyFollowsDebug(true, getEventDebugPoint()));})?
			('RIGHT' {$pfollows.add(new PropertyFollowsDebug(false, getEventDebugPoint()));})?
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
	DebugInfo.DebugPoint debug = null;
	
	if (inMainParseState()) {
		debug = getEventDebugPoint(); 
	}
}
@after {
	if (inMainParseState()) {
		self.addScriptedEvent($whenExpr.property, $action.action, orderProps, descending, $et.event, $in.noInline, $in.forceInline, debug);
	} 
}
	:	'WHEN'
		et=baseEvent
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
		self.addScriptedGlobalEvent($action.action, $et.event, single, $property.propUsage);
	}
}
	:	'ON' 
		et=baseEvent
		{
			if (inMainParseState()) {
				self.setPrevScope($et.event);
			}
		}
		('SINGLE' { single = true; })?
		('SHOWDEP' property=actionOrPropertyUsage)?
		action=endDeclTopContextDependentActionDefinitionBody[new ArrayList<TypedParameter>(), false, false]
		{
			if (inMainParseState()) {
				self.dropPrevScope($et.event);
			}
		}
	;

baseEvent returns [Event event]
@init {
	SystemEvent baseEvent = SystemEvent.APPLY;
	List<String> ids = null;
	List<NamedPropertyUsage> puAfters = null;
}
@after {
	if (inMainParseState()) {
		$event = self.createScriptedEvent(baseEvent, ids, puAfters);
	}
}
	:	('GLOBAL' { baseEvent = SystemEvent.APPLY; } | 'LOCAL' { baseEvent = SystemEvent.SESSION; })?
		('FORMS' (neIdList=nonEmptyCompoundIdList { ids = $neIdList.ids; }) )?
		('GOAFTER' (nePropList=nonEmptyPropertyUsageList { puAfters = $nePropList.propUsages; }) )?
	;

inlineStatement[List<TypedParameter> context] returns [List<LPWithParams> noInline = new ArrayList<>(), boolean forceInline = false]
	:   ('NOINLINE' { $noInline = null; } ( '(' params=singleParameterList[context, false] { $noInline = $params.props; } ')' )? )?
	    ('INLINE' { $forceInline = true; })?
	;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// SHOWDEP STATEMENT //////////////////////////////
////////////////////////////////////////////////////////////////////////////////

showDepStatement
@after {
    if (inMainParseState()) {
        self.addScriptedShowDep($property.propUsage, $propFrom.propUsage);
    }
}
    :	'SHOWDEP'
        property=actionOrPropertyUsage
        'FROM'
        propFrom=actionOrPropertyUsage
        ';'
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
		self.addScriptedTable($name.text, $list.ids, isFull, isNoDefault);
	}
}
	:	'TABLE' name=ID '(' list=classIdList ')' ('FULL' {isFull = true;} | 'NODEFAULT' { isNoDefault = true; } )? ';';

////////////////////////////////////////////////////////////////////////////////
/////////////////////////////// LOGGABLE STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

loggableStatement
@after {
	if (inMainParseState()) {
		self.addScriptedLoggable($list.propUsages);
	}	
}
	:	'LOGGABLE' list=nonEmptyPropertyUsageList ';'
	;

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
}
@after {
	if (inMainParseState()) {
		self.addScriptedIndex(context, $list.props);
	}	
}
	:	'INDEX' list=nonEmptyMappedPropertyOrSimpleParamList[context] ';'
	;


////////////////////////////////////////////////////////////////////////////////
/////////////////////////////// WINDOW STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

windowStatement
	:	windowCreateStatement
	|	windowHideStatement
	;

windowCreateStatement
@after {
	if (inMainParseState()) {
		self.addScriptedWindow($type.type, $name.name, $name.caption, $opts.options);
	}
}
	:	'WINDOW' name=simpleNameWithCaption type=windowType opts=windowOptions  ';'
	;

windowHideStatement
	:	'HIDE' 'WINDOW' wid=compoundID ';'
		{
			if (inMainParseState()) {
				self.hideWindow($wid.sid);
			}
		}
	;

windowType returns [WindowType type]
	:	'MENU'		{ $type = MENU; }
	|	'PANEL'		{ $type = PANEL; }
	|	'TOOLBAR'	{ $type = TOOLBAR; }
	|	'TREE'		{ $type = TREE; }
	;

windowOptions returns [NavigatorWindowOptions options]
@init {
	$options = new NavigatorWindowOptions();
}
	:	(	'HIDETITLE' { $options.setDrawTitle(false); }
		|	'DRAWROOT' { $options.setDrawRoot(true); }
		|	'HIDESCROLLBARS' { $options.setDrawScrollBars(false); }
		|	o=orientation { $options.setOrientation($o.val); }
		|	dp=dockPosition { $options.setDockPosition($dp.val); }
		|	bp=borderPosition { $options.setBorderPosition($bp.val); }
		|	'HALIGN' '(' ha=alignmentLiteral ')' { $options.setHAlign($ha.val); }
		|	'VALIGN' '(' va=alignmentLiteral ')' { $options.setVAlign($va.val); }
		|	'TEXTHALIGN' '(' tha=alignmentLiteral ')' { $options.setTextHAlign($tha.val); }
		|	'TEXTVALIGN' '(' tva=alignmentLiteral ')' { $options.setTextVAlign($tva.val); }
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
 		$element = self.createScriptedNavigatorElement($name.text, $caption.val, getCurrentDebugPoint(), $pu.propUsage, $formName.sid, isAction);
 	}	
}
	:	'FOLDER' name=ID (caption=localizedStringLiteral)? 
	|	'FORM' ((name=ID)? (caption=localizedStringLiteral)? '=')? formName=compoundID 
	|	('ACTION' { isAction = true; } )? ((name=ID)? (caption=localizedStringLiteral)? '=')? pu=propertyUsage 
	;

navigatorElementOptions returns [NavigatorElementOptions options] 
@init {
	$options = new NavigatorElementOptions();
	$options.position = InsertType.IN;
}
	:	
	(	'WINDOW' wid=compoundID { $options.windowName = $wid.sid; }
	|	pos=navigatorElementInsertPosition { $options.position = $pos.position; $options.anchor = $pos.anchor; }
	|	'IMAGE' path=stringLiteral { $options.imagePath = $path.val; }	
	)*
	;
	
navigatorElementInsertPosition returns [InsertType position, NavigatorElement anchor]
@init {
	$anchor = null;
}
	:	pos=insertRelativePositionLiteral { $position = $pos.val; } elem=navigatorElementSelector { $anchor = $elem.element; }
	|	'FIRST' { $position = InsertType.FIRST; }
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
	:	'NEW' cid=ID insPosition=componentInsertPosition
		{
			if (inMainParseState()) {
				newComp = $designStatement::design.createNewComponent($cid.text, parentComponent, $insPosition.position, $insPosition.anchor, self.getVersion());
			}
		}
		componentStatementBody[newComp]
	;
	
moveComponentStatement[ComponentView parentComponent]
@init {
	ComponentView insComp = null;
}
	:	'MOVE' insSelector=componentSelector { insComp = $insSelector.component; } insPosition=componentInsertPosition
		{
			if (inMainParseState()) {
				$designStatement::design.moveComponent(insComp, parentComponent, $insPosition.position, $insPosition.anchor, self.getVersion());
			}
		}
		componentStatementBody[insComp]
	;
	
componentInsertPosition returns [InsertType position, ComponentView anchor]
@init {
	$position = InsertType.IN;
	$anchor = null;
}
	:	(	(pos=insertRelativePositionLiteral { $position = $pos.val; } comp=componentSelector { $anchor = $comp.component; })
		|	'FIRST' { $position = InsertType.FIRST; }
		)?
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
    	'BOX' | 'OBJECTS' | 'TOOLBARBOX' | 'TOOLBARLEFT' | 'TOOLBARRIGHT' | 'TOOLBAR' | 'PANEL'
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
        ( cst=componentSingleSelectorType { result = $cst.text; } | gost=groupObjectTreeComponentSelectorType { result = $gost.text; } )
        '(' gots = groupObjectTreeSelector ')'
        {
            $sid = result + "(" + $gots.sid + ")";
        }
    ;

groupObjectTreeComponentSelectorType
    :
    	'TOOLBARSYSTEM' | 'FILTERGROUPS' | 'USERFILTER' | 'GRIDBOX' | 'CLASSCHOOSER' | 'GRID'
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

setObjectPropertyStatement[Object propertyReceiver] returns [String id, Object value]
	:	ID EQ componentPropertyValue ';'  { setObjectProperty($propertyReceiver, $ID.text, $componentPropertyValue.value); }
	;

componentPropertyValue returns [Object value]
	:   c=colorLiteral { $value = $c.val; }
	|   s=localizedStringLiteral { $value = $s.val; }
	|   i=intLiteral { $value = $i.val; }
	|   l=longLiteral { $value = $l.val; }
	|   d=doubleLiteral { $value = $d.val; }
	|   dim=dimensionLiteral { $value = $dim.val; }
	|   b=booleanLiteral { $value = $b.val; }
	|   intB=boundsIntLiteral { $value = $intB.val; }
	|   doubleB=boundsDoubleLiteral { $value = $doubleB.val; }
	|   contType=containerTypeLiteral { $value = $contType.val; }
	|   alignment=flexAlignmentLiteral { $value = $alignment.val; }
	|   prop=designPropertyObject { $value = $prop.property; }
	;


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// META STATEMENT //////////////////////////////
////////////////////////////////////////////////////////////////////////////////

metaCodeDeclarationStatement
@init {
	String code;
	List<String> tokens;
	List<Pair<Integer, Boolean>> metaTokens;
	int lineNumber = self.getParser().getCurrentParserLineNumber();
}
@after {
	if (inMetaClassTableParseState()) {
		self.addScriptedMetaCodeFragment($id.text, $list.ids, tokens, metaTokens, $text, lineNumber);
	}
}
	
	:	'META' id=ID '(' list=idList ')'
		{
			Pair<List<String>, List<Pair<Integer, Boolean>>> tokensAndMeta = self.grabMetaCode($id.text);
			tokens = tokensAndMeta.first;
			metaTokens = tokensAndMeta.second;
		}
		'END'
	;


metaCodeStatement
@init {
	int lineNumber = self.getParser().getCurrentParserLineNumber();
	ScriptParser.State oldState = null; 
	boolean enabledMeta = false;
}
@after {
	self.runMetaCode($id.sid, $list.ids, lineNumber, enabledMeta);
}
	:	'@' id=compoundID '(' list=metaCodeIdList ')'
		('{' 	
		{ 	enabledMeta = true; 
			if (self.getParser().enterGeneratedMetaState()) {  
				oldState = parseState;
				parseState = ScriptParser.State.GENMETA; 
			}
		}
		statements 
		{ 	if (oldState != null) {
				self.getParser().leaveGeneratedMetaState(); 
				parseState = oldState;
			}
		} 
		'}')? // for intellij plugin
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
	|	ptype=PRIMITIVE_TYPE	{ $sid = $ptype.text; } 
	|	lit=metaCodeLiteral 	{ $sid = $lit.text; }
	|				{ $sid = ""; }
	;

metaCodeLiteral
	:	STRING_LITERAL 
	| 	UINT_LITERAL
	|	UNUMERIC_LITERAL
	|	UDOUBLE_LITERAL
	|	ULONG_LITERAL
	|	LOGICAL_LITERAL
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

parameter
	:	ID | RECURSIVE_PARAM
	;

typedParameter returns [TypedParameter param]
@after {
	if (inMainParseState()) {
		$param = self.new TypedParameter($cname.sid, $pname.text);
	}
}
	:	(cname=classId)? pname=ID
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
	ids = new ArrayList<>();
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
	ids = new ArrayList<>();
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
	params = new ArrayList<>();
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


compoundIdList returns [List<String> ids] 
@init {
	ids = new ArrayList<>();	
} 
	:	(neIdList=nonEmptyCompoundIdList { ids = $neIdList.ids; })?
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

actionPDBList[List<TypedParameter> context, boolean dynamic] returns [List<LAWithParams> actions] 
@init {
	$actions = new ArrayList<>();
}
	:	(neList=nonEmptyActionPDBList[context, dynamic] { $actions = $neList.actions; })?
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
	
constantProperty returns [LP property]
@init {
	ScriptingLogicsModule.ConstType cls = null;
	Object value = null;
}
@after {
	if (inMainParseState()) {
		$property = self.addConstantProp(cls, value);	
	}
}
	:	lit = literal { cls = $lit.cls; value = $lit.value; }
	;

literal returns [ScriptingLogicsModule.ConstType cls, Object value]
	: 	vint=uintLiteral	{ $cls = ScriptingLogicsModule.ConstType.INT; $value = $vint.val; }
	|	vlong=ulongLiteral	{ $cls = ScriptingLogicsModule.ConstType.LONG; $value = $vlong.val; }
	|	vnum=UNUMERIC_LITERAL	{ $cls = ScriptingLogicsModule.ConstType.NUMERIC; $value = $vnum.text; }
	|	vdouble=udoubleLiteral { $cls = ScriptingLogicsModule.ConstType.REAL; $value = $vdouble.val; }
	|	vstr=localizedStringLiteral	{ $cls = ScriptingLogicsModule.ConstType.STRING; $value = $vstr.val; }  
	|	vbool=booleanLiteral	{ $cls = ScriptingLogicsModule.ConstType.LOGICAL; $value = $vbool.val;  { if (inMainParseState()) self.getChecks().checkBooleanUsage($vbool.val); }}
	|	vdate=dateLiteral	{ $cls = ScriptingLogicsModule.ConstType.DATE; $value = $vdate.val; }
	|	vdatetime=dateTimeLiteral { $cls = ScriptingLogicsModule.ConstType.DATETIME; $value = $vdatetime.val; }
	|	vtime=timeLiteral 	{ $cls = ScriptingLogicsModule.ConstType.TIME; $value = $vtime.val; }
	|	vsobj=staticObjectID { $cls = ScriptingLogicsModule.ConstType.STATIC; $value = $vsobj.sid; }
	|	vnull=NULL_LITERAL 	{ $cls = ScriptingLogicsModule.ConstType.NULL; }
	|	vcolor=colorLiteral { $cls = ScriptingLogicsModule.ConstType.COLOR; $value = $vcolor.val; }		
	;

classId returns [String sid]
	:	id=compoundID { $sid = $id.sid; }
	|	pid=PRIMITIVE_TYPE { $sid = $pid.text; }
	;

signatureClass returns [String sid]
	:	cid=classId { $sid = $cid.sid; }
	|	uc=unknownClass { $sid = $uc.text; }	
	; 

unknownClass 
	:	'?'
	;

typeId returns [String sid]
	:	pid=PRIMITIVE_TYPE { $sid = $pid.text; }
	|	obj='OBJECT' { $sid = $obj.text; }
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

multiCompoundID returns [String sid]
	:	id=ID { $sid = $id.text; } ('.' cid=ID { $sid = $sid + '.' + $cid.text; } )*
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

stringLiteral returns [String val]
	:	s=STRING_LITERAL { $val = self.transformStringLiteral($s.text); }
	;

localizedStringLiteral returns [LocalizedString val]
	:	s=STRING_LITERAL { $val = self.transformLocalizedStringLiteral($s.text); } 
	;

intLiteral returns [int val]
@init {
	boolean isMinus = false;
}
	:	(MINUS {isMinus=true;})?
		ui=uintLiteral  { $val = isMinus ? -$ui.val : $ui.val; }
	;

longLiteral returns [long val]
@init {
	boolean isMinus = false;
} 
	:	(MINUS {isMinus = true;})?
		ul=ulongLiteral { $val = isMinus ? -$ul.val : $ul.val; } 
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

insertRelativePositionLiteral returns [InsertType val]
	:	'BEFORE' { $val = InsertType.BEFORE; }
	|	'AFTER' { $val = InsertType.AFTER; }
	;

containerTypeLiteral returns [ContainerType val]
	:	'CONTAINERV' { $val = ContainerType.CONTAINERV; }	
	|	'CONTAINERH' { $val = ContainerType.CONTAINERH; }	
	|	'COLUMNS' { $val = ContainerType.COLUMNS; }
	|	'TABBED' { $val = ContainerType.TABBED_PANE; }
	|	'SPLITH' { $val = ContainerType.HORIZONTAL_SPLIT_PANE; }
	|	'SPLITV' { $val = ContainerType.VERTICAL_SPLIT_PANE; }
	|   'SCROLL' { $val = ContainerType.SCROLL; }
	;

alignmentLiteral returns [Alignment val]
    :   'START' { $val = Alignment.START; }
    |   'CENTER' { $val = Alignment.CENTER; }
    |   'END' { $val = Alignment.END; }
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
	;

emailRecipientTypeLiteral returns [Message.RecipientType val]
	:	'TO'	{ $val = Message.RecipientType.TO; }
	|	'CC'	{ $val = Message.RecipientType.CC; }
	|	'BCC'	{ $val = Message.RecipientType.BCC; }
	;
	
emailAttachFormat returns [AttachmentFormat val]
	:	'PDF'	{ $val = AttachmentFormat.PDF; }
	|	'DOCX'	{ $val = AttachmentFormat.DOCX; }
	|	'HTML'	{ $val = AttachmentFormat.HTML; }
	|	'RTF'	{ $val = AttachmentFormat.RTF; }
	|	'XLSX'	{ $val = AttachmentFormat.XLSX; }
	|	'DBF'	{ $val = AttachmentFormat.DBF; }
	;

udoubleLiteral returns [double val]
	:	d=UDOUBLE_LITERAL { $val = self.createScriptedDouble($d.text.substring(0, $d.text.length() - 1)); }
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
fragment STR_LITERAL_CHAR	: ('\\' ~('\n'|'\r')) | ~('\r'|'\n'|'\''|'\\');	 // overcomplicated due to bug in ANTLR Works
fragment DIGIT		:	'0'..'9';
fragment DIGITS		:	('0'..'9')+;
fragment EDIGITS	:	('0'..'9')*;
fragment HEX_DIGIT	: 	'0'..'9' | 'a'..'f' | 'A'..'F';
fragment FIRST_ID_LETTER	: ('a'..'z'|'A'..'Z');
fragment NEXT_ID_LETTER		: ('a'..'z'|'A'..'Z'|'_'|'0'..'9');
fragment OPEN_CODE_BRACKET	: '<{';
fragment CLOSE_CODE_BRACKET : '}>';

fragment STRING_LITERAL_FRAGMENT : '\'' STR_LITERAL_CHAR* '\'';
fragment ID_FRAGMENT : FIRST_ID_LETTER NEXT_ID_LETTER*;
fragment NEXTID_FRAGMENT : NEXT_ID_LETTER+;
fragment STRING_LITERAL_ID_FRAGMENT : ID_FRAGMENT | STRING_LITERAL_FRAGMENT;
fragment STRING_LITERAL_NEXTID_FRAGMENT : NEXTID_FRAGMENT | STRING_LITERAL_FRAGMENT;
fragment META_FRAGMENT : STRING_LITERAL_ID_FRAGMENT? (('##' | '###') STRING_LITERAL_NEXTID_FRAGMENT)+;

PRIMITIVE_TYPE  :	'INTEGER' | 'DOUBLE' | 'LONG' | 'BOOLEAN' | 'DATE' | 'DATETIME' | 'ZDATETIME' | 'YEAR'
                |   'TEXT' | 'RICHTEXT' | 'TIME' | 'WORDFILE' | 'IMAGEFILE' | 'PDFFILE' | 'RAWFILE'
				| 	'FILE' | 'EXCELFILE' | 'TEXTFILE' | 'CSVFILE' | 'HTMLFILE' | 'JSONFILE' | 'XMLFILE' | 'TABLEFILE'
				|   'WORDLINK' | 'IMAGELINK'
				|   'PDFLINK' | 'RAWLINK' | 'LINK' | 'EXCELLINK' | 'TEXTLINK' | 'CSVLINK' | 'HTMLLINK' | 'JSONLINK' | 'XMLLINK' | 'TABLELINK'
				|   ('BPSTRING' ('[' DIGITS ']')?) | ('BPISTRING' ('[' DIGITS ']')?)
				|	('STRING' ('[' DIGITS ']')?) | ('ISTRING' ('[' DIGITS ']')?) | 'NUMERIC' ('[' DIGITS ',' DIGITS ']')? | 'COLOR';
LOGICAL_LITERAL :	'TRUE' | 'FALSE';
NULL_LITERAL	:	'NULL';	
ID				:	ID_FRAGMENT;
STRING_LITERAL	:	STRING_LITERAL_FRAGMENT;
META_ID			:	META_FRAGMENT;
WS				:	(NEWLINE | SPACE) { $channel=HIDDEN; };
COLOR_LITERAL 	:	'#' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT;
COMMENTS		:	('//' .* '\n') { $channel=HIDDEN; };
UINT_LITERAL 	:	DIGITS;
ULONG_LITERAL	:	DIGITS('l'|'L');
UDOUBLE_LITERAL	:	DIGITS '.' EDIGITS('d'|'D');
UNUMERIC_LITERAL:	DIGITS '.' EDIGITS;	  
DATE_LITERAL	:	DIGIT DIGIT DIGIT DIGIT '_' DIGIT DIGIT '_' DIGIT DIGIT; 
DATETIME_LITERAL:	DIGIT DIGIT DIGIT DIGIT '_' DIGIT DIGIT '_' DIGIT DIGIT '_' DIGIT DIGIT ':' DIGIT DIGIT;	
TIME_LITERAL	:	DIGIT DIGIT ':' DIGIT DIGIT;
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