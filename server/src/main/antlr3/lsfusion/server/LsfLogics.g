grammar LsfLogics;

@header {
	package lsfusion.server;

	import lsfusion.base.OrderedMap;
	import lsfusion.interop.ClassViewType;
	import lsfusion.interop.PropertyEditType;
	import lsfusion.interop.form.layout.ContainerType;
	import lsfusion.interop.form.layout.FlexAlignment;
	import lsfusion.interop.form.layout.Alignment;
	import lsfusion.interop.form.ServerResponse;
	import lsfusion.interop.FormEventType;
	import lsfusion.interop.FormPrintType;
	import lsfusion.interop.FormExportType;
	import lsfusion.interop.ModalityType;
	import lsfusion.server.form.instance.FormSessionScope;
	import lsfusion.server.data.Union;
	import lsfusion.server.data.expr.query.PartitionType;
	import lsfusion.server.form.entity.*;
	import lsfusion.server.form.navigator.NavigatorElement;
	import lsfusion.server.form.view.ComponentView;
	import lsfusion.server.form.view.GroupObjectView;
	import lsfusion.server.form.view.PropertyDrawView;
	import lsfusion.server.classes.sets.ResolveClassSet;
	import lsfusion.server.logics.mutables.Version;
	import lsfusion.server.logics.linear.LP;
	import lsfusion.server.logics.linear.LCP;
	import lsfusion.server.logics.property.PropertyFollows;
	import lsfusion.server.logics.property.Cycle;
	import lsfusion.server.logics.property.ImportSourceFormat;
	import lsfusion.server.logics.scripted.*;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.WindowType;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.InsertPosition;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.GroupingType;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.LPWithParams;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.TypedParameter;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.PropertyUsage;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.NavigatorElementOptions;
	import lsfusion.server.logics.scripted.ScriptingFormEntity.RegularFilterInfo;
	import lsfusion.server.mail.SendEmailActionProperty.FormStorageType;
	import lsfusion.server.mail.AttachmentFormat;
	import lsfusion.server.logics.property.actions.file.FileActionType;
	import lsfusion.server.logics.property.actions.flow.Inline;
	import lsfusion.server.logics.property.actions.SystemEvent;
	import lsfusion.server.logics.property.Event;
	import lsfusion.server.logics.property.actions.flow.ListCaseActionProperty;
	import lsfusion.server.logics.property.CaseUnionProperty;
	import lsfusion.server.logics.property.IncrementType;
	import lsfusion.server.data.expr.formula.SQLSyntaxType;
    import lsfusion.server.logics.property.actions.ChangeEvent;
	import lsfusion.server.logics.property.BooleanDebug;
	import lsfusion.server.logics.property.PropertyFollowsDebug;
	import lsfusion.server.logics.debug.ActionDebugInfo;
	
	import javax.mail.Message;

	import lsfusion.server.form.entity.GroupObjectProp;

	import lsfusion.base.col.interfaces.immutable.ImSet;

	import java.util.*;
	import java.awt.*;
	import org.antlr.runtime.BitSet;
	import java.util.List;
	import java.sql.Date;

	import static java.util.Arrays.asList;
	import static lsfusion.server.logics.scripted.ScriptingLogicsModule.WindowType.*;
}

@lexer::header { 
	package lsfusion.server; 
	import lsfusion.server.logics.scripted.ScriptingLogicsModule;
	import lsfusion.server.logics.scripted.ScriptParser;
}

@lexer::members {
	public ScriptingLogicsModule self;
	public ScriptParser.State parseState;
	
	@Override
	public void emitErrorMessage(String msg) {
		if (parseState == ScriptParser.State.INIT) { 
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

	public boolean inInitParseState() {
		return inParseState(ScriptParser.State.INIT); 
	}

	public boolean inGroupParseState() {
		return inParseState(ScriptParser.State.GROUP);
	}

	public boolean inClassParseState() {
		return inParseState(ScriptParser.State.CLASS);
	}

	public boolean inPropParseState() {
		return inParseState(ScriptParser.State.PROP);
	}

	public boolean inTableParseState() {
		return inParseState(ScriptParser.State.TABLE);
	}

	public boolean inIndexParseState() {
		return inParseState(ScriptParser.State.INDEX);
	}

	public void setObjectProperty(Object propertyReceiver, String propertyName, Object propertyValue) throws ScriptingErrorLog.SemanticErrorException {
		if (inPropParseState()) {
			$designStatement::design.setObjectProperty(propertyReceiver, propertyName, propertyValue);
		}
	}

	public List<GroupObjectEntity> getGroupObjectsList(List<String> ids, Version version) throws ScriptingErrorLog.SemanticErrorException {
		if (inPropParseState()) {
			return $formStatement::form.getGroupObjectsList(ids, version);
		}
		return null;
	}

	public MappedProperty getPropertyWithMapping(PropertyUsage pUsage, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
		if (inPropParseState()) {
			return $formStatement::form.getPropertyWithMapping(pUsage, mapping);
		}
		return null;
	}

	public TypedParameter TP(String className, String paramName) throws ScriptingErrorLog.SemanticErrorException {
		return self.new TypedParameter(className, paramName);
	}

	@Override
	public void emitErrorMessage(String msg) {
		if (parseState == ScriptParser.State.INIT) { 
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
	List<String> requiredModules = new ArrayList<String>();
	List<String> namespacePriority = new ArrayList<String>();
	String namespaceName = null;
}
@after {
	if (inPreParseState()) {
		self.initScriptingModule($name.text, namespaceName, requiredModules, namespacePriority);
	} else if (inInitParseState()) {
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
		|	overrideStatement
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
	List<String> classParents = new ArrayList<String>();
	boolean isAbstract = false;
	boolean isNative = false;
	boolean isComplex = false;
	List<String> instanceNames = new ArrayList<String>();
	List<String> instanceCaptions = new ArrayList<String>();
}
@after {
	if (inClassParseState()) {
	    if(!isNative)
		    self.addScriptedClass($nameCaption.name, $nameCaption.caption, isAbstract, $classData.names, $classData.captions, $classData.parents, isComplex);
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
	if (inClassParseState()) {
		self.extendClass($className.sid, $classData.names, $classData.captions, $classData.parents);
	}
}
	:	'EXTEND' 'CLASS' 
		className=compoundID 
		classData=classInstancesAndParents 
	;

classInstancesAndParents returns [List<String> names, List<String> captions, List<String> parents] 
@init {
	$parents = new ArrayList<String>();
	$names = new ArrayList<String>();
	$captions = new ArrayList<String>();
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
}
@after {
	if (inGroupParseState()) {
		self.addScriptedGroup($groupNameCaption.name, $groupNameCaption.caption, parent);
	}
}
	:	'GROUP' groupNameCaption=simpleNameWithCaption
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
	int lineNumber = self.getParser().getCurrentParserLineNumber();
}
@after {
	if (inPropParseState() && initialDeclaration) {
		self.addScriptedForm($formStatement::form, lineNumber);
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
		|	dialogFormDeclaration
		|	editFormDeclaration
		|	reportFilesDeclaration
		)*
		';'
	;

dialogFormDeclaration
	:	'DIALOG' cid=classId 'OBJECT' oid=ID
		{
			if (inPropParseState()) {
				$formStatement::form.setAsDialogForm($cid.sid, $oid.text, self.getVersion());
			}
		}
	;

editFormDeclaration
	:	'EDIT' cid=classId 'OBJECT' oid=ID
		{
			if (inPropParseState()) {
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
	PropertyUsage propUsage = null;
	List<String> mapping = null;
}
@after {
	if (inPropParseState()) {
		$formStatement::form.setReportPath(groupObject, propUsage, mapping);	
	}
}
	:	(
			'TOP' 
		| 	go = formGroupObjectEntity { groupObject = $go.groupObject; }
		) 
		prop = formMappedProperty { propUsage = $prop.propUsage; mapping = $prop.mapping; }
	;

formDeclaration returns [ScriptingFormEntity form]
@init {
	ModalityType modalityType = null;
	int autoRefresh = 0;
	String image = null;
	String title = null;
}
@after {
	if (inPropParseState()) {
		$form = self.createScriptedForm($formNameCaption.name, $formNameCaption.caption, image, modalityType, autoRefresh);
	}
}
	:	'FORM' 
		formNameCaption=simpleNameWithCaption
		(	(modality=modalityTypeLiteral { modalityType = $modality.val; })
		|	('IMAGE' img=stringLiteral { image = $img.val; })
		|	('AUTOREFRESH' refresh=intLiteral { autoRefresh = $refresh.val; })
		)*
	;


extendingFormDeclaration returns [ScriptingFormEntity form]
@after {
	if (inPropParseState()) {
		$form = self.getFormForExtending($formName.sid);
	}
}
	:	'EXTEND' 'FORM' formName=compoundID
	;

formGroupObjectsList
@init {
	List<ScriptingGroupObject> groups = new ArrayList<ScriptingGroupObject>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptingGroupObjects(groups, self.getVersion());
	}
}
	:	'OBJECTS'
		groupElement=formGroupObjectDeclaration { groups.add($groupElement.groupObject); }
		(',' groupElement=formGroupObjectDeclaration { groups.add($groupElement.groupObject); })*
	;

formTreeGroupObjectList
@init {
	String treeSID = null;
	List<ScriptingGroupObject> groups = new ArrayList<ScriptingGroupObject>();
	List<List<PropertyUsage>> properties = new ArrayList<List<PropertyUsage>>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptingTreeGroupObject(treeSID, groups, properties, self.getVersion());
	}
}
	:	'TREE'
		(id = ID { treeSID = $id.text; })?
		groupElement=formTreeGroupObjectDeclaration { groups.add($groupElement.groupObject); properties.add($groupElement.properties); }
		(',' groupElement=formTreeGroupObjectDeclaration { groups.add($groupElement.groupObject); properties.add($groupElement.properties); })*
	;

formGroupObjectDeclaration returns [ScriptingGroupObject groupObject]
	:	object=formCommonGroupObject { $groupObject = $object.groupObject; } formGroupObjectOptions[$groupObject]		
	; 

formGroupObjectOptions[ScriptingGroupObject groupObject]
	:	(	viewType=formGroupObjectViewType { $groupObject.setViewType($viewType.type, $viewType.isInitType); }
		|	pageSize=formGroupObjectPageSize { $groupObject.setPageSize($pageSize.value); }
		|	update=formGroupObjectUpdate { $groupObject.setUpdateType($update.updateType); }
		|	relative=formGroupObjectRelativePosition { $groupObject.setNeighbourGroupObject($relative.groupObject, $relative.isRightNeighbour); }
		)*
	;

formTreeGroupObjectDeclaration returns [ScriptingGroupObject groupObject, List<PropertyUsage> properties]
	:	(object=formCommonGroupObject { $groupObject = $object.groupObject; })
		(parent=treeGroupParentDeclaration { $properties = $parent.properties; })?
	; 

treeGroupParentDeclaration returns [List<PropertyUsage> properties = new ArrayList<PropertyUsage>()]
	:	'PARENT'
		(	id = propertyUsage { $properties.add($id.propUsage); }
		|	'('
				list=nonEmptyPropertyUsageList { $properties.addAll($list.propUsages); }
			')'
		)		
	;

formCommonGroupObject returns [ScriptingGroupObject groupObject]
	:	sdecl=formSingleGroupObjectDeclaration
		{
			$groupObject = new ScriptingGroupObject(null, asList($sdecl.name), asList($sdecl.className), asList($sdecl.caption), asList($sdecl.event));
		}
	|	mdecl=formMultiGroupObjectDeclaration
		{
			$groupObject = new ScriptingGroupObject($mdecl.groupName, $mdecl.objectNames, $mdecl.classNames, $mdecl.captions, $mdecl.events);
		}
	;

formGroupObjectViewType returns [ClassViewType type, boolean isInitType]
	: 	('INIT' {$isInitType = true;} | 'FIXED' {$isInitType = false;})
		viewType=classViewType { $type = $viewType.type; }
	;

classViewType returns [ClassViewType type]
	: 	('PANEL' {$type = ClassViewType.PANEL;} | 'GRID' {$type = ClassViewType.GRID;})
	;

formGroupObjectPageSize returns [Integer value = null]
	:	'PAGESIZE' size=intLiteral { $value = $size.val; }
	;
	
formGroupObjectRelativePosition returns [GroupObjectEntity groupObject, boolean isRightNeighbour]
	:	'AFTER' go=formGroupObjectEntity { $groupObject = $go.groupObject; $isRightNeighbour = true; }
	|	'BEFORE' go=formGroupObjectEntity { $groupObject = $go.groupObject; $isRightNeighbour = false; }
	;

formGroupObjectUpdate returns [UpdateType updateType]
@init {
}
	:	'FIRST' { $updateType = UpdateType.FIRST; }
	|	'LAST' { $updateType = UpdateType.LAST; }
	|   'PREV' { $updateType = UpdateType.PREV; }
	;

formSingleGroupObjectDeclaration returns [String name, String className, String caption, ActionPropertyObjectEntity event] 
	:	foDecl=formObjectDeclaration { $name = $foDecl.name; $className = $foDecl.className; $caption = $foDecl.caption; $event = $foDecl.event; }
	;

formMultiGroupObjectDeclaration returns [String groupName, List<String> objectNames, List<String> classNames, List<String> captions, List<ActionPropertyObjectEntity> events]
@init {
	$objectNames = new ArrayList<String>();
	$classNames = new ArrayList<String>();
	$captions = new ArrayList<String>();
	$events = new ArrayList<ActionPropertyObjectEntity>();
}
	:	(gname=ID { $groupName = $gname.text; } '=')?
		'('
			objDecl=formObjectDeclaration { $objectNames.add($objDecl.name); $classNames.add($objDecl.className); $captions.add($objDecl.caption); $events.add($objDecl.event); }
			(',' objDecl=formObjectDeclaration { $objectNames.add($objDecl.name); $classNames.add($objDecl.className); $captions.add($objDecl.caption); $events.add($objDecl.event); })+
		')'
	;


formObjectDeclaration returns [String name, String className, String caption, ActionPropertyObjectEntity event] 
	:	(objectName=ID { $name = $objectName.text; } '=')?	
		id=classId { $className = $id.sid; }
		(c=stringLiteral { $caption = $c.val; })?
		('ON' 'CHANGE' faprop=formActionPropertyObject { $event = $faprop.action; })?
	; 
	
formPropertiesList
@init {
	List<PropertyUsage> properties = new ArrayList<PropertyUsage>();
	List<String> aliases = new ArrayList<String>();
	List<List<String>> mapping = new ArrayList<List<String>>();
	FormPropertyOptions commonOptions = null;
	List<FormPropertyOptions> options = new ArrayList<FormPropertyOptions>();
	int lineNumber = self.getParser().getCurrentParserLineNumber();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedPropertyDraws(properties, aliases, mapping, commonOptions, options, self.getVersion(), lineNumber);
	}
}
	:	'PROPERTIES' '(' objects=idList ')' opts=formPropertyOptionsList list=formPropertyUList
		{
			commonOptions = $opts.options;
			properties = $list.properties;
			aliases = $list.aliases;
			mapping = Collections.nCopies(properties.size(), $objects.ids);
			options = $list.options;
		}
	|	'PROPERTIES' opts=formPropertyOptionsList mappedList=formMappedPropertiesList
		{
			commonOptions = $opts.options;
			properties = $mappedList.properties;
			aliases = $mappedList.aliases;
			mapping = $mappedList.mapping;
			options = $mappedList.options;
		}
	;	

formPropertyOptionsList returns [FormPropertyOptions options]
@init {
	$options = new FormPropertyOptions();
}
	:	(	editType = propertyEditTypeLiteral { $options.setEditType($editType.val); }
		|	'HINTNOUPDATE' { $options.setHintNoUpdate(true); }
		|	'HINTTABLE' { $options.setHintTable(true); }
		|	'TOOLBAR' { $options.setDrawToToolbar(true); }
		|	'OPTIMISTICASYNC' { $options.setOptimisticAsync(true); }
		|	'COLUMNS' (columnsName=stringLiteral)? '(' ids=nonEmptyIdList ')' { $options.setColumns($columnsName.text, getGroupObjectsList($ids.ids, self.getVersion())); }
		|	'SHOWIF' propObj=formCalcPropertyObject { $options.setShowIf($propObj.property); }
		|	'READONLYIF' propObj=formCalcPropertyObject { $options.setReadOnlyIf($propObj.property); }
		|	'BACKGROUND' propObj=formCalcPropertyObject { $options.setBackground($propObj.property); }
		|	'FOREGROUND' propObj=formCalcPropertyObject { $options.setForeground($propObj.property); }
		|	'HEADER' propObj=formCalcPropertyObject { $options.setHeader($propObj.property); }
		|	'FOOTER' propObj=formCalcPropertyObject { $options.setFooter($propObj.property); }
		|	'FORCE' viewType=classViewType { $options.setForceViewType($viewType.type); }
		|	'TODRAW' toDraw=formGroupObjectEntity { $options.setToDraw($toDraw.groupObject); }
		|	'BEFORE' pdraw=formPropertyDraw { $options.setNeighbourPropertyDraw($pdraw.property, $pdraw.text); $options.setNeighbourType(false); }
		|	'AFTER'  pdraw=formPropertyDraw { $options.setNeighbourPropertyDraw($pdraw.property, $pdraw.text); $options.setNeighbourType(true); }
		|	'QUICKFILTER' pdraw=formPropertyDraw { $options.setQuickFilterPropertyDraw($pdraw.property); }
		|	'ON' 'EDIT' prop=formActionPropertyObject { $options.addEditAction(ServerResponse.EDIT_OBJECT, $prop.action); }
		|	'ON' 'CHANGE' prop=formActionPropertyObject { $options.addEditAction(ServerResponse.CHANGE, $prop.action); }
		|	'ON' 'CHANGEWYS' prop=formActionPropertyObject { $options.addEditAction(ServerResponse.CHANGE_WYS, $prop.action); }
		|	'ON' 'SHORTCUT' (c=stringLiteral)? prop=formActionPropertyObject { $options.addContextMenuEditAction($c.val, $prop.action); }
		|	'EVENTID' id=stringLiteral { $options.setEventId($id.val); }
		)*
	;

formPropertyDraw returns [PropertyDrawEntity property]
	:	id=ID              	{ if (inPropParseState()) $property = $formStatement::form.getPropertyDraw($id.text, self.getVersion()); }
	|	prop=mappedPropertyDraw { if (inPropParseState()) $property = $formStatement::form.getPropertyDraw($prop.name, $prop.mapping, self.getVersion()); }
	;

formMappedPropertiesList returns [List<String> aliases, List<PropertyUsage> properties, List<List<String>> mapping, List<FormPropertyOptions> options]
@init {
	$aliases = new ArrayList<String>();
	$properties = new ArrayList<PropertyUsage>();
	$mapping = new ArrayList<List<String>>();
	$options = new ArrayList<FormPropertyOptions>();
	String alias = null;
}
	:	{ alias = null; }
		(id=ID '=' { alias = $id.text; })?
		mappedProp=formMappedProperty opts=formPropertyOptionsList
		{
			$aliases.add(alias);
			$properties.add($mappedProp.propUsage);
			$mapping.add($mappedProp.mapping);
			$options.add($opts.options);
		}
		(','
			{ alias = null; }
			(id=ID '=' { alias = $id.text; })?
			mappedProp=formMappedProperty opts=formPropertyOptionsList
			{
				$aliases.add(alias);
				$properties.add($mappedProp.propUsage);
				$mapping.add($mappedProp.mapping);
				$options.add($opts.options);
			}
		)*
	;

formCalcPropertyObject returns [CalcPropertyObjectEntity property = null]
	:	mProperty=formMappedProperty
		{
			if (inPropParseState()) {
				$property = $formStatement::form.addCalcPropertyObject($mProperty.propUsage, $mProperty.mapping);
			}
		}
	;

formActionPropertyObject returns [ActionPropertyObjectEntity action = null]
	:	mProperty=formMappedProperty
		{
			if (inPropParseState()) {
				$action = $formStatement::form.addActionPropertyObject($mProperty.propUsage, $mProperty.mapping);
			}
		}
	;

formGroupObjectEntity returns [GroupObjectEntity groupObject]
	:	id = ID { 
			if (inPropParseState()) {
				$groupObject = $formStatement::form.getGroupObjectEntity($ID.text, self.getVersion());
			} 
		}
	;

formMappedProperty returns [PropertyUsage propUsage, List<String> mapping]
	:	pu=formPropertyUsage { $propUsage = $pu.propUsage; }
		'('
			objects=idList { $mapping = $objects.ids; }
		')'
	;

formPropertySelector[FormEntity form] returns [PropertyDrawEntity propertyDraw = null]
	:	pname=ID
		{
			if (inPropParseState()) {
				$propertyDraw = form == null ? null : ScriptingFormEntity.getPropertyDraw(self, form, $pname.text, self.getVersion());
			}
		}
	|	mappedProp=mappedPropertyDraw	
		{
			if (inPropParseState()) {
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

formPropertyUList returns [List<String> aliases, List<PropertyUsage> properties, List<FormPropertyOptions> options]
@init {
	$aliases = new ArrayList<String>();
	$properties = new ArrayList<PropertyUsage>();
	$options = new ArrayList<FormPropertyOptions>();
	String alias = null;
}
	:	{ alias = null; }
		(id=ID '=' { alias = $id.text; })?
		pu=formPropertyUsage opts=formPropertyOptionsList
		{
			$aliases.add(alias);
			$properties.add($pu.propUsage);
			$options.add($opts.options);
		}
		(','
			{ alias = null; }
			(id=ID '=' { alias = $id.text; })?
			pu=formPropertyUsage opts=formPropertyOptionsList
			{
				$aliases.add(alias);
				$properties.add($pu.propUsage);
				$options.add($opts.options);
			}
		)*
	;


formPropertyUsage returns [PropertyUsage propUsage]
	:	pu=propertyUsage	{ $propUsage = $pu.propUsage; }
	|	cid='OBJVALUE'		{ $propUsage = new PropertyUsage($cid.text); }
	|	cid='ADDOBJ'		{ $propUsage = new PropertyUsage($cid.text); }
	|	cid='ADDFORM'		{ $propUsage = new PropertyUsage($cid.text); }
	|	cid='ADDNESTEDFORM'	{ $propUsage = new PropertyUsage($cid.text); }
	|	cid='ADDSESSIONFORM'	{ $propUsage = new PropertyUsage($cid.text); }
	|	cid='EDITFORM'		{ $propUsage = new PropertyUsage($cid.text); }
	|	cid='EDITNESTEDFORM'	{ $propUsage = new PropertyUsage($cid.text); }
	|	cid='EDITSESSIONFORM'	{ $propUsage = new PropertyUsage($cid.text); }
	|	cid='DELETE'		{ $propUsage = new PropertyUsage($cid.text); }
	|	cid='DELETESESSION'	{ $propUsage = new PropertyUsage($cid.text); }
	;


formFiltersList
@init {
	List<LP> properties = new ArrayList<LP>();
	List<List<String>> propertyMappings = new ArrayList<List<String>>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedFilters(properties, propertyMappings, self.getVersion());
	}
}
	:	'FILTERS'
		decl=formFilterDeclaration { properties.add($decl.property); propertyMappings.add($decl.mapping);}
	    (',' decl=formFilterDeclaration { properties.add($decl.property); propertyMappings.add($decl.mapping);})*
	;

formHintsList
@init {
	boolean hintNoUpdate = true;
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedHints(hintNoUpdate, $list.propUsages, self.getVersion());
	}
}
	:	(('HINTNOUPDATE') | ('HINTTABLE' { hintNoUpdate = false; })) 'LIST'
		list=nonEmptyPropertyUsageList	
	;

formEventsList
@init {
	List<ActionPropertyObjectEntity> actions = new ArrayList<ActionPropertyObjectEntity>();
	List<Object> types = new ArrayList<Object>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedFormEvents(actions, types, self.getVersion());
	}
}
	:	'EVENTS'
		decl=formEventDeclaration { actions.add($decl.action); types.add($decl.type); }
		(',' decl=formEventDeclaration { actions.add($decl.action); types.add($decl.type); })*
	;


formEventDeclaration returns [ActionPropertyObjectEntity action, Object type]
@init {
    boolean before = false;
}
	:	'ON'
		(	'OK' 	 { $type = FormEventType.OK; }
		|	'APPLY' ('BEFORE' { before = true; } | 'AFTER' { before = true; } ) { $type = before ? FormEventType.BEFOREAPPLY : FormEventType.AFTERAPPLY; }
		|	'CLOSE'	 { $type = FormEventType.CLOSE; }
		|	'INIT'	 { $type = FormEventType.INIT; }
		|	'CANCEL' { $type = FormEventType.CANCEL; }
		|	'DROP'	 { $type = FormEventType.DROP; }
		|	'QUERYOK'	 { $type = FormEventType.QUERYOK; }
		|	'QUERYCLOSE'	 { $type = FormEventType.QUERYCLOSE; }
		| 	'CHANGE' objectId=ID { $type = $objectId.text; }
		)
		faprop=formActionPropertyObject { $action = $faprop.action; }
	;


filterGroupDeclaration
@init {
	String filterGroupSID = null;
	List<RegularFilterInfo> filters = new ArrayList<RegularFilterInfo>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedRegularFilterGroup(filterGroupSID, filters, self.getVersion());
	}
}
	:	'FILTERGROUP' sid=ID { filterGroupSID = $sid.text; }
		( rf=formRegularFilterDeclaration { filters.add($rf.filter); } )*
	;

extendFilterGroupDeclaration
@init {
	String filterGroupSID = null;
	List<RegularFilterInfo> filters = new ArrayList<RegularFilterInfo>();
}
@after {
	if (inPropParseState()) {
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
    :   'FILTER' caption=stringLiteral fd=formFilterDeclaration (keystroke=stringLiteral {key = $keystroke.val;})? setDefault=filterSetDefault
        {
            $filter = new RegularFilterInfo($caption.val, key, $fd.property, $fd.mapping, $setDefault.isDefault);
        }
    ;
	
formFilterDeclaration returns [LP property, List<String> mapping] 
@init {
	List<TypedParameter> context = null;
	if (inPropParseState()) {
		context = $formStatement::form.getTypedObjectsNames(self.getVersion());
	}
}
@after {
	if (inPropParseState()) {
		$mapping = self.getUsedNames(context, $expr.property.usedParams);
	}	
}
	:	expr=propertyExpression[context, false] { if (inPropParseState()) { self.checkNecessaryProperty($expr.property); $property = $expr.property.property; } }
	;
	
filterSetDefault returns [boolean isDefault = false]
	:	('DEFAULT' { $isDefault = true; })?
	;

formOrderByList
@init {
	boolean ascending = true;
	List<PropertyDrawEntity> properties = new ArrayList<PropertyDrawEntity>();
	List<Boolean> orders = new ArrayList<Boolean>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedDefaultOrder(properties, orders, self.getVersion());
	}
}
	:	'ORDER' 'BY' orderedProp=formPropertyDrawWithOrder { properties.add($orderedProp.property); orders.add($orderedProp.order); }
		(',' orderedProp=formPropertyDrawWithOrder { properties.add($orderedProp.property); orders.add($orderedProp.order); } )*
	;
	
formPropertyDrawWithOrder returns [PropertyDrawEntity property, boolean order = true]
	:	pDraw=formPropertyDraw { $property = $pDraw.property; } ('DESC' { $order = false; })?
	;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// PROPERTY STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

propertyStatement
@init {
	LP property = null;
	List<TypedParameter> context = new ArrayList<TypedParameter>();
	List<ResolveClassSet> signature = null; 
	boolean dynamic = true;
	int lineNumber = self.getParser().getCurrentParserLineNumber(); 
}
@after {
	if (inPropParseState()) {
	    if (property != null) // not native
		    self.setPropertyScriptInfo(property, $text, lineNumber);
	}
}
	:	declaration=propertyDeclaration { if ($declaration.params != null) { context = $declaration.params; dynamic = false; } }
		'=' 
		(	pdef=propertyDefinition[context, dynamic] { property = $pdef.property; signature = $pdef.signature; }
		|	adef=actionDefinition[context, dynamic] { property = $adef.property; signature = $adef.signature; }
		)
		propertyOptions[property, $declaration.name, $declaration.caption, context, signature]
		( {!self.semicolonNeeded()}?=>  | ';')
	;

propertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LP property, List<ResolveClassSet> signature]
	:	ciPD=contextIndependentPD[false] { $property = $ciPD.property; $signature = $ciPD.signature; }
	|	expr=propertyExpression[context, dynamic] { if (inPropParseState()) { self.checkNecessaryProperty($expr.property); $signature = self.getClassesFromTypedParams(context); $property = $expr.property.property; } }
	|	'NATIVE' classId '(' clist=classIdList ')' { if (inPropParseState()) { $signature = self.createClassSetsFromClassNames($clist.ids); }}
	;

actionDefinition[List<TypedParameter> context, boolean dynamic] returns [LP property, List<ResolveClassSet> signature]
	:	actD=concreteActionDefinition[context, dynamic] { if (inPropParseState()) { $property = $actD.property.property; $signature = $actD.signature; } }	
	|	abstractActionDef=abstractActionDefinition { $property = $abstractActionDef.property; $signature = $abstractActionDef.signature; }
	|	'ACTION' 'NATIVE' '(' clist=classIdList ')' { if (inPropParseState()) { $signature = self.createClassSetsFromClassNames($clist.ids); }}
	;

propertyDeclaration returns [String name, String caption, List<TypedParameter> params]
	:	propNameCaption=simpleNameWithCaption { $name = $propNameCaption.name; $caption = $propNameCaption.caption; }
		('(' paramList=typedParameterList ')' { $params = $paramList.params; })? 
	;


propertyExpression[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	int line = self.getParser().getGlobalCurrentLineNumber(); 
	int offset = self.getParser().getGlobalPositionInLine();
}
@after{
    if (inPropParseState()) {
        self.propertyDefinitionCreated($property.property, line, offset);
    }
}
	:	pe=ifPE[context, dynamic] { $property = $pe.property; }
	;


ifPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedIfProp(props);
	}
} 
	:	firstExpr=orPE[context, dynamic] { props.add($firstExpr.property); }
		('IF' nextExpr=orPE[context, dynamic] { props.add($nextExpr.property); })*
	;

orPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedOrProp(props);
	}
} 
	:	firstExpr=xorPE[context, dynamic] { props.add($firstExpr.property); }
		('OR' nextExpr=xorPE[context, dynamic] { props.add($nextExpr.property); })*
	;

xorPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedXorProp(props);
	}
} 
	:	firstExpr=andPE[context, dynamic] { props.add($firstExpr.property); }
		('XOR' nextExpr=andPE[context, dynamic] { props.add($nextExpr.property); })*
	;

andPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedAndProp(props);				
	}
}
	:	firstExpr=notPE[context, dynamic] { props.add($firstExpr.property); }
		('AND' nextExpr=notPE[context, dynamic] { props.add($nextExpr.property); })*
	;

notPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean notWas = false;
}
@after {
	if (inPropParseState() && notWas) { 
		$property = self.addScriptedNotProp($notExpr.property);  
	}
}
	:	'NOT' notExpr=notPE[context, dynamic] { notWas = true; } 
	|	expr=equalityPE[context, dynamic] { $property = $expr.property; } 
	;

equalityPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	LPWithParams leftProp = null, rightProp = null;
	String op = null;
}
@after {
	if (inPropParseState() && op != null) {
		$property = self.addScriptedEqualityProp(op, leftProp, rightProp);
	} else {
		$property = leftProp;
	}
}
	:	lhs=relationalPE[context, dynamic] { leftProp = $lhs.property; }
		(operand=EQ_OPERAND { op = $operand.text; }
		rhs=relationalPE[context, dynamic] { rightProp = $rhs.property; })?
	;


relationalPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	LPWithParams leftProp = null, rightProp = null;
	LP mainProp = null;
	String op = null;
}
@after {
	if (inPropParseState())
	{
		if (op != null) {
			$property = self.addScriptedRelationalProp(op, leftProp, rightProp);
		} else if (mainProp != null) {
			$property = leftProp;
			$property.property = self.addScriptedTypeExprProp(mainProp, leftProp);
		} else {
			$property = leftProp;
		}
	}	
}
	:	lhs=additiveORPE[context, dynamic] { leftProp = $lhs.property; }
		(
			(   operand=relOperand { op = $operand.text; }
			    rhs=additiveORPE[context, dynamic] { rightProp = $rhs.property; }
			)
		|	def=typePropertyDefinition { mainProp = $def.property; }
		)?
	;


additiveORPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
	List<String> ops = new ArrayList<String>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedAdditiveOrProp(ops, props);
	}
}
	:	firstExpr=additivePE[context, dynamic] { props.add($firstExpr.property); }
		(operand=ADDOR_OPERAND nextExpr=additivePE[context, dynamic] { ops.add($operand.text); props.add($nextExpr.property); })*
	;
	
	
additivePE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
	List<String> ops = new ArrayList<String>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedAdditiveProp(ops, props);				
	}
}
	:	firstExpr=multiplicativePE[context, dynamic] { props.add($firstExpr.property); }
		( (operand=PLUS | operand=MINUS) { ops.add($operand.text); }
		nextExpr=multiplicativePE[context, dynamic] { props.add($nextExpr.property); })*
	;
		
	
multiplicativePE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
	List<String> ops = new ArrayList<String>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedMultiplicativeProp(ops, props);				
	}
}
	:	firstExpr=unaryMinusPE[context, dynamic] { props.add($firstExpr.property); }
		(operand=multOperand { ops.add($operand.text); }
		nextExpr=unaryMinusPE[context, dynamic] { props.add($nextExpr.property); })*
	;

unaryMinusPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property] 
@init {
	boolean minusWas = false;
}
@after {
	if (inPropParseState() && minusWas) {
		$property = self.addScriptedUnaryMinusProp($expr.property);
	} 
}
	:	MINUS expr=unaryMinusPE[context, dynamic] { minusWas = true; } 
	|	simpleExpr=postfixUnaryPE[context, dynamic] { $property = $simpleExpr.property; }
	;

		 
postfixUnaryPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property] 
@init {	
	boolean hasPostfix = false;
}
@after {
	if (inPropParseState() && hasPostfix) {
		$property = self.addScriptedDCCProp($expr.property, $index.val);
	} 
}
	:	expr=simplePE[context, dynamic] { $property = $expr.property; }
		(
			'[' index=uintLiteral ']' { hasPostfix = true; }
		)?
	;		 

		 
simplePE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
	:	'(' expr=propertyExpression[context, dynamic] ')' { $property = $expr.property; } 
	|	primitive=expressionPrimitive[context, dynamic] { $property = $primitive.property; } 
	;

	
expressionPrimitive[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
	:	param=singleParameter[context, dynamic] { $property = $param.property; }
	|	expr=expressionFriendlyPD[context, dynamic] { $property = $expr.property; }
	;

singleParameter[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	String className = null;
}
@after {
	if (inPropParseState()) {
		$property = new LPWithParams(null, Collections.singletonList(self.getParamIndex(TP(className, $paramName.text), $context, $dynamic, insideRecursion)));
	}
}
	:	(clsId=classId { className = $clsId.sid; })? paramName=parameter
	;
	
expressionFriendlyPD[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		self.checkPropertyValue($property.property);
	}
}
	:	joinDef=joinPropertyDefinition[context, dynamic] { $property = $joinDef.property; } 
	|	multiDef=multiPropertyDefinition[context, dynamic] { $property = $multiDef.property; }
	|	overDef=overridePropertyDefinition[context, dynamic] { $property = $overDef.property; }
	|	ifElseDef=ifElsePropertyDefinition[context, dynamic] { $property = $ifElseDef.property; }
	|	maxDef=maxPropertyDefinition[context, dynamic] { $property = $maxDef.property; }
	|	caseDef=casePropertyDefinition[context, dynamic] { $property = $caseDef.property; }
	|	partDef=partitionPropertyDefinition[context, dynamic] { $property = $partDef.property; }
	|	recDef=recursivePropertyDefinition[context, dynamic] { $property = $recDef.property; } 
	|	structDef=structCreationPropertyDefinition[context, dynamic] { $property = $structDef.property; }
	|	concatDef=concatPropertyDefinition[context, dynamic] { $property = $concatDef.property; }
	|	castDef=castPropertyDefinition[context, dynamic] { $property = $castDef.property; }
	|	sessionDef=sessionPropertyDefinition[context, dynamic] { $property = $sessionDef.property; }
	|	signDef=signaturePropertyDefinition[context, dynamic] { $property = $signDef.property; }
	|	constDef=constantProperty { $property = new LPWithParams($constDef.property, new ArrayList<Integer>()); }
	;

contextIndependentPD[boolean innerPD] returns [LP property, List<ResolveClassSet> signature]
@init {
	int line = self.getParser().getGlobalCurrentLineNumber(); 
	int offset = self.getParser().getGlobalPositionInLine();
}
@after{
	if (inPropParseState()) {
		self.propertyDefinitionCreated($property, line, offset);
	}
}
	: 	dataDef=dataPropertyDefinition[innerPD] { $property = $dataDef.property; $signature = $dataDef.signature; }
	|	abstractDef=abstractPropertyDefinition { $property = $abstractDef.property; $signature = $abstractDef.signature; }
	|	formulaProp=formulaPropertyDefinition { $property = $formulaProp.property; $signature = $formulaProp.signature; }
	|	groupDef=groupPropertyDefinition { $property = $groupDef.property; $signature = $groupDef.signature; }
	|	filterProp=filterPropertyDefinition { $property = $filterProp.property; $signature = $filterProp.signature; }
	;

joinPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean isInline = false;
}
@after {
	if (inPropParseState()) {
		if (isInline) {
			$property = self.addScriptedJProp(true, $iProp.property, $exprList.props);
		} else {
			$property = self.addScriptedJProp(true, $uProp.propUsage, $exprList.props, context);	
		}
	}
}
	:	('JOIN')? 
		(	uProp=propertyUsage
		|	iProp=inlineProperty { isInline = true; }
		)
		'('
		exprList=propertyExpressionList[context, dynamic]
		')'
	;


groupPropertyDefinition returns [LP property, List<ResolveClassSet> signature]
@init {
	List<LPWithParams> orderProps = new ArrayList<LPWithParams>();
	List<LPWithParams> groupProps = new ArrayList<LPWithParams>();
	List<TypedParameter> groupContext = new ArrayList<TypedParameter>();
	boolean ascending = true;
}
@after {
	if (inPropParseState()) {
		$signature = self.getSignatureForGProp(groupProps, groupContext);
		$property = self.addScriptedGProp($type.type, $mainList.props, groupProps, orderProps, ascending, $whereExpr.property, groupContext);
	}
}
	:	'GROUP'
		type=groupingType
		mainList=nonEmptyPropertyExpressionList[groupContext, true]
		('BY' exprList=nonEmptyPropertyExpressionList[groupContext, true] { groupProps.addAll($exprList.props); })?
		('ORDER' ('DESC' { ascending = false; } )?
		orderList=nonEmptyPropertyExpressionList[groupContext, true] { orderProps.addAll($orderList.props); })?
		('WHERE' whereExpr=propertyExpression[groupContext, false])?
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
	List<LPWithParams> paramProps = new ArrayList<LPWithParams>();
	PropertyUsage pUsage = null;
	PartitionType type = null;
	int groupExprCnt;
	boolean strict = false;
	int precision = 0;
	boolean ascending = true;
	boolean useLast = true;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedPartitionProp(type, pUsage, strict, precision, ascending, useLast, groupExprCnt, paramProps, context);
	}
}
	:	'PARTITION' 
		(
			(	'SUM'	{ type = PartitionType.SUM; } 
			|	'PREV'	{ type = PartitionType.PREVIOUS; }
			)
		|	'UNGROUP'
			ungroupProp=propertyUsage { pUsage = $ungroupProp.propUsage; }
			(	'PROPORTION' { type = PartitionType.DISTR_CUM_PROPORTION; } 
				('STRICT' { strict = true; })? 
				'ROUND' '(' prec=intLiteral ')' { precision = $prec.val; }
			|	'LIMIT' { type = PartitionType.DISTR_RESTRICT; } 
				('STRICT' { strict = true; })? 
			)
		)
		expr=propertyExpression[context, dynamic] { paramProps.add($expr.property); }
		(	'BY'
			exprList=nonEmptyPropertyExpressionList[context, dynamic] { paramProps.addAll($exprList.props); }
		)?
		{ groupExprCnt = paramProps.size() - 1; }
		(	'ORDER' ('DESC' { ascending = false; } )?				
			orderList=nonEmptyPropertyExpressionList[context, dynamic] { paramProps.addAll($orderList.props); }
		)? 
		('WINDOW' 'EXCEPTLAST' { useLast = false; })?
	;


dataPropertyDefinition[boolean innerPD] returns [LP property, List<ResolveClassSet> signature]
@init {
	boolean localProp = false;
	boolean isNested = false;
}
@after {
	if (inPropParseState()) {
		$signature = self.createClassSetsFromClassNames($paramClassNames.ids); 
		$property = self.addScriptedDProp($returnClass.sid, $paramClassNames.ids, localProp, innerPD, false, isNested);
	}
}
	:	'DATA'
		('LOCAL' nlm=nestedLocalModifier { localProp = true; isNested = $nlm.isNested; })?
		returnClass=classId
		'('
			paramClassNames=classIdList
		')'
	;

nestedLocalModifier returns[boolean isNested = false]
	:	('NESTED' { $isNested = true; }	)?
	;

abstractPropertyDefinition returns [LP property, List<ResolveClassSet> signature]
@init {
	boolean isExclusive = true;
	boolean isLast = true;
	boolean isChecked = false;
	CaseUnionProperty.Type type = CaseUnionProperty.Type.MULTI;	
}
@after {
	if (inPropParseState()) {
		$signature = self.createClassSetsFromClassNames($paramClassNames.ids); 
		$property = self.addScriptedAbstractProp(type, $returnClass.sid, $paramClassNames.ids, isExclusive, isChecked, isLast);
	}
}
	:	'ABSTRACT'
		(
			(('CASE' { type = CaseUnionProperty.Type.CASE; isExclusive = false; }
			|	'MULTI'	{ type = CaseUnionProperty.Type.MULTI; isExclusive = true; }) (opt=abstractExclusiveOverrideOption { isExclusive = $opt.isExclusive; isLast = $opt.isLast;})?)
		|   ('OVERRIDE' { type = CaseUnionProperty.Type.VALUE; isExclusive = false; } (acopt=abstractCaseAddOption { isLast = $acopt.isLast; } )?)
		|	'EXCLUSIVE'{ type = CaseUnionProperty.Type.VALUE; isExclusive = true; }
		)?
		('CHECKED' { isChecked = true; })?
		returnClass=classId
		'('
			paramClassNames=classIdList
		')'
	;

abstractActionDefinition returns [LP property, List<ResolveClassSet> signature]
@init {
	boolean isExclusive = true;
    boolean isLast = true;
	boolean isChecked = false;
	ListCaseActionProperty.AbstractType type = ListCaseActionProperty.AbstractType.MULTI;
}
@after {
	if (inPropParseState()) {
		$signature = self.createClassSetsFromClassNames($paramClassNames.ids); 
		$property = self.addScriptedAbstractActionProp(type, $paramClassNames.ids, isExclusive, isChecked, isLast);
	}
}
	:	'ACTION' 'ABSTRACT' 
		(
			(('CASE' { type = ListCaseActionProperty.AbstractType.CASE; isExclusive = false; }
			|	'MULTI'	{ type = ListCaseActionProperty.AbstractType.MULTI; isExclusive = true; }) (opt=abstractExclusiveOverrideOption { isExclusive = $opt.isExclusive; isLast = $opt.isLast;})?)
		|	('LIST' { type = ListCaseActionProperty.AbstractType.LIST; } (acopt=abstractCaseAddOption { isLast = $acopt.isLast; } )?)
		)?
		('CHECKED' { isChecked = true; })?
		'(' 
			paramClassNames=classIdList
		')'	
	;
	
overridePropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean isExclusive = false;
}
@after {
	if (inPropParseState()) {
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
	if (inPropParseState()) {
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
	if (inPropParseState()) {
		$property = self.addScriptedMaxProp($exprList.props, isMin);
	}
}
	:	(('MAX') { isMin = false; } | ('MIN'))
		exprList=nonEmptyPropertyExpressionList[context, dynamic]	
	;


casePropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> whenProps = new ArrayList<LPWithParams>();
	List<LPWithParams> thenProps = new ArrayList<LPWithParams>();
	LPWithParams elseProp = null;
	boolean isExclusive = false;
}
@after {
	if (inPropParseState()) {
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
	if (inPropParseState()) {
		$property = self.addScriptedMultiProp($exprList.props, isExclusive);
	}
}
	:	'MULTI' 
		exprList=nonEmptyPropertyExpressionList[context, dynamic] 
		(opt=exclusiveOverrideOption { isExclusive = $opt.isExclusive; })?
	;

recursivePropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	Cycle cycleType = Cycle.NO;
	List<TypedParameter> recursiveContext = null;
	if (inPropParseState() && insideRecursion) {
		self.getErrLog().emitNestedRecursionError(self.getParser());
	}
}
@after {
	if (inPropParseState()) {
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
	if (inPropParseState()) {
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
	if (inPropParseState()) {
		$property = self.addScriptedCastProp($ptype.text, $expr.property);
	}
}
	:   ptype=PRIMITIVE_TYPE '(' expr=propertyExpression[context, dynamic] ')'
	;

concatPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
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
	if (inPropParseState()) {
		$property = self.addScriptedSessionProp(type, $expr.property);
	}
}
	:	(	'PREV' { type = null; } 
		| 	'CHANGED' { type = IncrementType.CHANGED; }
		| 	'SET' { type = IncrementType.SET; }
		| 	'DROPPED' { type = IncrementType.DROP; }
		| 	'SETCHANGED' { type = IncrementType.SETCHANGED; }
		|	'DROPCHANGED' { type = IncrementType.DROPCHANGED; }
		| 	'DROPSET' { type = IncrementType.DROPSET; }
		)
		'('
		expr=propertyExpression[context, dynamic] 
		')'
	;

signaturePropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property] 	
@after {
	if (inPropParseState()) {
		$property = self.addScriptedSignatureProp($expr.property);
	}
} 
	: 	'CLASS' '(' expr=propertyExpression[context, dynamic] ')'
	;

formulaPropertyDefinition returns [LP property, List<ResolveClassSet> signature]
@init {
	String className = null;
	boolean hasNotNullCondition = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedSFProp(className, $synt.types, $synt.strings, hasNotNullCondition);
		$signature = Collections.<ResolveClassSet>nCopies($property.listInterfaces.size(), null);
	}
}
	:	'FORMULA'
		('NULL' { hasNotNullCondition = true; })?
		(clsName=classId { className = $clsName.sid; })?
		synt=formulaPropertySyntaxList
	;

formulaPropertySyntaxList returns [List<SQLSyntaxType> types = new ArrayList<SQLSyntaxType>(), List<String> strings = new ArrayList<String>()]
	:	firstType=formulaPropertySyntaxType firstText=stringLiteral { $types.add($firstType.type); $strings.add($firstText.val); }
		(',' nextType=formulaPropertySyntaxType nextText=stringLiteral { $types.add($nextType.type); $strings.add($nextText.val); })*
	;

formulaPropertySyntaxType returns [SQLSyntaxType type = null]
	:	('PG' { $type = SQLSyntaxType.POSTGRES; } | 'MS' { $type = SQLSyntaxType.MSSQL; })? 
	;

filterPropertyDefinition returns [LP property, List<ResolveClassSet> signature]
@init {
	String className = null;
	GroupObjectProp prop = null;
}
@after {
	if (inPropParseState()) {
		$signature = new ArrayList<ResolveClassSet>();	
		$property = self.addScriptedGroupObjectProp($gobj.sid, prop, $signature);
	}
}
	:	('FILTER' { prop = GroupObjectProp.FILTER; } | 'ORDER' { prop = GroupObjectProp.ORDER; } | 'VIEW' { prop = GroupObjectProp.VIEW; } )
		gobj=groupObjectID
	;

readActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean delete = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedReadActionProperty($expr.property, $pUsage.propUsage, $moveExpr.property, delete);
	}
}
	:	'READ' expr=propertyExpression[context, dynamic] 'TO' pUsage=propertyUsage (('MOVE' moveExpr=propertyExpression[context, dynamic]) | ('DELETE' {delete = true; }))?
	;

writeActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedWriteActionProperty($expr.property, $pUsage.propUsage);
	}
}
	:	'WRITE' expr=propertyExpression[context, dynamic] 'FROM' pUsage=propertyUsage
	;

importActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	ImportSourceFormat format = null;
	LPWithParams sheet = null;
	String separator = null;
	boolean noHeader = false;
	String charset = null;
	boolean attr = false;

}
@after {
	if (inPropParseState()) {
		if($type.format == ImportSourceFormat.XLS || $type.format == ImportSourceFormat.XLSX)
			$property = self.addScriptedImportExcelActionProperty($type.format, $expr.property, $plist.ids, $plist.propUsages, sheet);
		else if($type.format == ImportSourceFormat.CSV)
        	$property = self.addScriptedImportCSVActionProperty($expr.property, $plist.ids, $plist.propUsages, separator, noHeader, charset);
        else if($type.format == ImportSourceFormat.XML)
        	$property = self.addScriptedImportXMLActionProperty($expr.property, $plist.ids, $plist.propUsages, attr);
		else
			$property = self.addScriptedImportActionProperty($type.format, $expr.property, $plist.ids, $plist.propUsages);
	}
} 
	:	'IMPORT' 
		type = importSourceFormat [context, dynamic] { format = $type.format; sheet = $type.sheet; separator = $type.separator; noHeader = $type.noHeader; attr = $type.attr; charset = $type.charset; }
		'TO' plist=nonEmptyPropertyUsageListWithIds 
		'FROM' expr=propertyExpression[context, dynamic]
	;

nonEmptyPropertyUsageListWithIds returns [List<String> ids, List<PropertyUsage> propUsages]
@init {
	$ids = new ArrayList<String>();
	$propUsages = new ArrayList<PropertyUsage>();
}
	:	usage = propertyUsageWithId { $ids.add($usage.id); $propUsages.add($usage.propUsage); }
		(',' usage = propertyUsageWithId { $ids.add($usage.id); $propUsages.add($usage.propUsage); })*
	;

propertyUsageWithId returns [String id = null, PropertyUsage propUsage]
	:	(
			pid=ID '=' { $id = $pid.text; }
		|	sLiteral = stringLiteral '=' { $id = $sLiteral.val; } 
		)? 
		pu=propertyUsage { $propUsage = $pu.propUsage; }
	;

importSourceFormat [List<TypedParameter> context, boolean dynamic] returns [ImportSourceFormat format, LPWithParams sheet, String separator, boolean noHeader, String charset, boolean attr]
	: 'XLS' { $format = ImportSourceFormat.XLS; } ('SHEET' sheetProperty = propertyExpression[context, dynamic] { $sheet = $sheetProperty.property; })?
	| 'XLSX' { $format = ImportSourceFormat.XLSX; } ('SHEET' sheetProperty = propertyExpression[context, dynamic] { $sheet = $sheetProperty.property; })?
	| 'DBF'  { $format = ImportSourceFormat.DBF; }
	| 'CSV'  { $format = ImportSourceFormat.CSV; } (separatorVal = stringLiteral { $separator = separatorVal.val; })? ('NOHEADER' { $noHeader = true; })? ('CHARSET' charsetVal = stringLiteral { $charset = charsetVal.val; })?
	| 'XML'  { $format = ImportSourceFormat.XML; } ('ATTR' { $attr = true; })?
	| 'JDBC' { $format = ImportSourceFormat.JDBC; }
	| 'MDB'  { $format = ImportSourceFormat.MDB; }
	;

typePropertyDefinition returns [LP property] 
@init {
	boolean bIs = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedTypeProp($clsId.sid, bIs);
	}	
}
	:	('IS' { bIs = true; } | 'AS')
		clsId=classId
	;


propertyUsage returns [PropertyUsage propUsage]  
@init {
	List<String> classList = null;
}
@after {
	$propUsage = new PropertyUsage($pname.name, classList);
}
	:	pname=propertyName ('[' cidList=signatureClassList ']' { classList = $cidList.ids; })? 
	;

inlineProperty returns [LP property]
@init {
	List<TypedParameter> newContext = new ArrayList<TypedParameter>();
	List<ResolveClassSet> signature = null;
}
@after {
	if (inPropParseState()) { // not native
		property.setExplicitClasses(signature);	
	}
}
	:	'[' '='	(	expr=propertyExpression[newContext, true] { if (inPropParseState()) { self.checkNecessaryProperty($expr.property); $property = $expr.property.property; signature = self.getClassesFromTypedParams(newContext); } }
				|	ciPD=contextIndependentPD[true] { $property = $ciPD.property; signature = $ciPD.signature; }
				|	cAD=concreteActionDefinition[newContext, true] { if (inPropParseState()) { $property = $cAD.property.property; signature = $cAD.signature; }}
				)
		']'
	;

propertyName returns [String name] 
	:	id=compoundID { $name = $id.sid; }
	;

propertyOptions[LP property, String propertyName, String caption, List<TypedParameter> context, List<ResolveClassSet> signature]
@init {
	String groupName = null;
	String table = null;
	boolean isPersistent = false;
	boolean isComplex = false;
	boolean noHint = false;
	Boolean isLoggable = null;
	BooleanDebug notNull = null;
	BooleanDebug notNullResolve = null;
	Event notNullEvent = null;
}
@after {
	if (inPropParseState() && property != null) { // not native
		self.addSettingsToProperty(property, propertyName, caption, context, signature, groupName, isPersistent, isComplex, noHint, table, notNull, notNullResolve, notNullEvent);
		self.makeLoggable(property, isLoggable);
	}
}
	: 	(	'IN' name=compoundID { groupName = $name.sid; }
		|	'PERSISTENT' { isPersistent = true; }
		|	'COMPLEX' { isComplex = true; }
		|	'NOHINT' { noHint = true; }
		|	'TABLE' tbl = compoundID { table = $tbl.sid; }
		|	shortcutSetting [property, caption != null ? caption : propertyName]
		|	asEditActionSetting [property]
		|	toolbarSetting [property]
		|	fixedCharWidthSetting [property]
		|	minCharWidthSetting [property]
		|	maxCharWidthSetting [property]
		|	prefCharWidthSetting [property]
		|	imageSetting [property]
		|	editKeySetting [property]
		|	autosetSetting [property]
		|	confirmSetting [property]
		|	regexpSetting [property]
		|	loggableSetting { isLoggable = true; }
		|	echoSymbolsSetting [property]
		|	indexSetting [property]
		|	aggPropSetting [property]
		|	s=notNullSetting { 
		    		notNull = new BooleanDebug($s.debugInfo);
		    		notNullResolve = $s.toResolve; 
		    		notNullEvent = $s.event; 
			}	
		|	onEditEventSetting [property, context]
		|	eventIdSetting [property]
		)*
	;


shortcutSetting [LP property, String caption]
@after {
	if (inPropParseState()) {
		self.addToContextMenuFor(property, caption, $usage.propUsage);
	}
}
	:	'SHORTCUT' usage = propertyUsage 
	;

asEditActionSetting [LP property]
@init {
	String editActionSID = null;
}
@after {
	if (inPropParseState()) {
		self.setAsEditActionFor(property, editActionSID, $usage.propUsage);
	}
}
	:	(   'ASONCHANGE' { editActionSID = ServerResponse.CHANGE; }
		|   'ASONCHANGEWYS' { editActionSID = ServerResponse.CHANGE_WYS; }
		|   'ASONEDIT' { editActionSID = ServerResponse.EDIT_OBJECT; }
        	)
	        usage = propertyUsage 
	;

toolbarSetting [LP property]
@after {
	if (inPropParseState()) {
		self.setDrawToToolbar(property);
	}
}
	:	'TOOLBAR'
	;

fixedCharWidthSetting [LP property]
@after {
	if (inPropParseState()) {
		self.setFixedCharWidth(property, $width.val);
	}
}
	:	'FIXEDCHARWIDTH' width = intLiteral
	;

minCharWidthSetting [LP property]
@after {
	if (inPropParseState()) {
		self.setMinCharWidth(property, $width.val);
	}
}
	:	'MINCHARWIDTH' width = intLiteral
	;

maxCharWidthSetting [LP property]
@after {
	if (inPropParseState()) {
		self.setMaxCharWidth(property, $width.val);
	}
}
	:	'MAXCHARWIDTH' width = intLiteral
	;

prefCharWidthSetting [LP property]
@after {
	if (inPropParseState()) {
		self.setPrefCharWidth(property, $width.val);
	}
}
	:	'PREFCHARWIDTH' width = intLiteral
	;

imageSetting [LP property]
@after {
	if (inPropParseState()) {
		self.setImage(property, $path.val);
	}
}
	:	'IMAGE' path = stringLiteral
	;

editKeySetting [LP property]
@init {
	Boolean show = null;
}
@after {
	if (inPropParseState()) {
		self.setEditKey(property, $key.val, show);
	}
}
	:	'EDITKEY' key = stringLiteral
		(	('SHOW' { show = true; })
		|	('HIDE' { show = false; })
		)?
	;

autosetSetting [LP property]
@init {
	boolean autoset = false;
}
@after {
	if (inPropParseState()) {
		self.setAutoset(property, autoset);
	}
}
	:	'AUTOSET' { autoset = true; }
	;

confirmSetting [LP property]
@init {
	boolean askConfirm = false;
}
@after {
	if (inPropParseState()) {
		self.setAskConfirm(property, askConfirm);
	}
}
	:	'CONFIRM' { askConfirm = true; }
	;

regexpSetting [LP property]
@init {
	String message = null;
}
@after {
	if (inPropParseState()) {
		self.setRegexp(property, $exp.val, message);
	}
}
	:	'REGEXP' exp = stringLiteral
		(mess = stringLiteral { message = $mess.val; })?
	;

loggableSetting
	:	'LOGGABLE'
	;

echoSymbolsSetting [LP property]
@after {
	if (inPropParseState()) {
		self.setEchoSymbols(property);
	}
}
	:	'ECHO'
	;

indexSetting [LP property]
@after {
	if (inPropParseState()) {
		self.addScriptedIndex(property);
	}
}
	:	'INDEXED'
	;

aggPropSetting [LP property]
@after {
	if (inPropParseState()) {
		self.setAggProp(property);
	}
}
	:	'AGGPROP'
	;

notNullSetting returns [ActionDebugInfo debugInfo, BooleanDebug toResolve = null, Event event]
@init {
    $debugInfo = self.getEventStackDebugInfo();
}
	:	'NOT' 'NULL' 
	    (dt = notNullDeleteSetting { $toResolve = new BooleanDebug($dt.debugInfo); })? 
	    et=baseEvent { $event = $et.event; }
	;

notNullDeleteSetting returns [ActionDebugInfo debugInfo]
@init {
    $debugInfo = self.getEventStackDebugInfo();
}
    :   'DELETE'
	;

onEditEventSetting [LP property, List<TypedParameter> context]
@init {
	String type = null;
}
@after {
	if (inPropParseState()) {
		self.setScriptedEditAction(property, type, $action.property);
	}
}
	:	'ON'
	    (   'CHANGE' { type = ServerResponse.CHANGE; }
	    |   'CHANGEWYS' { type = ServerResponse.CHANGE_WYS; }
	    |   'EDIT' { type = ServerResponse.EDIT_OBJECT; }
	    )
		action=topContextDependentActionDefinitionBody[context, false, false]
	;

eventIdSetting [LP property]
@after {
	if (inPropParseState()) {
		self.setEventId(property, $id.val);
	}
}
	:	'EVENTID' id=stringLiteral
	;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// ACTION PROPERTIES ///////////////////////////
////////////////////////////////////////////////////////////////////////////////

concreteActionDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, List<ResolveClassSet> signature]
	:	'ACTION'
		(	aDB=topContextDependentActionDefinitionBody[context, dynamic, true] { if (inPropParseState()) { $property = $aDB.property; $signature = $aDB.signature; }}
		|	ciADB=contextIndependentActionDB { $property = $ciADB.property; $signature = $ciADB.signature; }
		)
	;

// top level, not recursive
topContextDependentActionDefinitionBody[List<TypedParameter> context, boolean dynamic, boolean needFullContext] returns [LPWithParams property, List<ResolveClassSet> signature]
    :	aDB=modifyContextFlowActionDefinitionBody[new ArrayList<TypedParameter>(), context, dynamic, needFullContext] { $property = $aDB.property; $signature = $aDB.signature; }    
	;

// modifies context + is flow action (uses another actions)
modifyContextFlowActionDefinitionBody[List<TypedParameter> oldContext, List<TypedParameter> newContext, boolean dynamic, boolean needFullContext] returns [LPWithParams property, List<ResolveClassSet> signature]
@after{
    if (inPropParseState()) {
        $property = self.modifyContextFlowActionPropertyDefinitionBodyCreated($property, $newContext, $oldContext, $signature, needFullContext);
    }
}
    :	aDB=modifyContextActionDefinitionBody[newContext, dynamic, true] { $property = $aDB.property; $signature = $aDB.signature; }    
	;

innerActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, List<ResolveClassSet> signature]
    :	aDB=modifyContextActionDefinitionBody[context, dynamic, false] { $property = $aDB.property; $signature = $aDB.signature; }
	;

modifyContextActionDefinitionBody[List<TypedParameter> context, boolean dynamic, boolean modifyContext] returns [LPWithParams property, List<ResolveClassSet> signature]
@init {
	int line = self.getParser().getGlobalCurrentLineNumber(); 
	int offset = self.getParser().getGlobalPositionInLine();
}
@after{
	if (inPropParseState()) {
		self.actionPropertyDefinitionBodyCreated($property, line, offset, modifyContext);
	}
}
	:	extDB=extendContextActionDB[context, dynamic]	{ $property = $extDB.property; if (inPropParseState()) $signature = self.getClassesFromTypedParams(context); }
	|	keepDB=keepContextActionDB[context, dynamic]	{ $property = $keepDB.property; if (inPropParseState()) $signature = self.getClassesFromTypedParams(context); }
	;

extendContextActionDB[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init { 
	if (inPropParseState() && dynamic) {
		self.getErrLog().emitExtendActionContextError(self.getParser());
	}
}
	:	setADB=assignActionDefinitionBody[context] { $property = $setADB.property; }
	|	forADB=forActionPropertyDefinitionBody[context] { $property = $forADB.property; }
	|	classADB=changeClassActionDefinitionBody[context] { $property = $classADB.property; }
	|	delADB=deleteActionDefinitionBody[context] { $property = $delADB.property; }
	|	addADB=addObjectActionDefinitionBody[context] { $property = $addADB.property; }
	;
	
keepContextActionDB[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
	:	listADB=listActionDefinitionBody[context, dynamic] { $property = $listADB.property; }
	|	requestInputADB=requestInputActionDefinitionBody[context, dynamic] { $property = $requestInputADB.property; }
	|	execADB=execActionDefinitionBody[context, dynamic] { $property = $execADB.property; }	
	|	tryADB=tryActionDefinitionBody[context, dynamic] { $property = $tryADB.property; }
	|	ifADB=ifActionDefinitionBody[context, dynamic] { $property = $ifADB.property; }
	|	caseADB=caseActionDefinitionBody[context, dynamic] { $property = $caseADB.property; }
	|	multiADB=multiActionDefinitionBody[context, dynamic] { $property = $multiADB.property; }	
	|	termADB=terminalFlowActionDefinitionBody { $property = $termADB.property; }
	|  	applyADB=applyActionDefinitionBody[context, dynamic] { $property = $applyADB.property; }
	|	formADB=formActionDefinitionBody[context, dynamic] { $property = $formADB.property; }
	|	msgADB=messageActionDefinitionBody[context, dynamic] { $property = $msgADB.property; }
	|	asyncADB=asyncUpdateActionDefinitionBody[context, dynamic] { $property = $asyncADB.property; }
	|	seekADB=seekObjectActionDefinitionBody[context, dynamic] { $property = $seekADB.property; }
	|	confirmADB=confirmActionDefinitionBody[context, dynamic] { $property = $confirmADB.property; }
	|	mailADB=emailActionDefinitionBody[context, dynamic] { $property = $mailADB.property; }
	|	fileADB=fileActionDefinitionBody[context, dynamic] { $property = $fileADB.property; }
	|	evalADB=evalActionDefinitionBody[context, dynamic] { $property = $evalADB.property; }
	|	drillDownADB=drillDownActionDefinitionBody[context, dynamic] { $property = $drillDownADB.property; }
	|	focusADB=focusActionDefinitionBody[context, dynamic] { $property = $focusADB.property; }
	|	readADB=readActionDefinitionBody[context, dynamic] { $property = $readADB.property; }
	|	writeADB=writeActionDefinitionBody[context, dynamic] { $property = $writeADB.property; }
	|	importADB=importActionDefinitionBody[context, dynamic] { $property = $importADB.property; }
	;
	
contextIndependentActionDB returns [LPWithParams property, List<ResolveClassSet> signature]
@init {
	$property = new LPWithParams(null, new ArrayList<Integer>());
}
	:	addformADB=addFormActionDefinitionBody { $property.property = $addformADB.property; $signature = $addformADB.signature; }
	|	editformADB=editFormActionDefinitionBody { $property.property = $editformADB.property; $signature = $editformADB.signature; }
	|	customADB=customActionDefinitionBody { $property.property = $customADB.property; $signature = $customADB.signature; }
	;


formActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	FormSessionScope sessionScope = FormSessionScope.OLDSESSION;
	ModalityType modalityType = ModalityType.DOCKED;
	boolean checkOnOk = false;
	boolean showDrop = false;
	boolean noCancel = false;
	FormPrintType printType = null;
	FormExportType exportType = null;
	List<String> objects = new ArrayList<String>();
	List<LPWithParams> mapping = new ArrayList<LPWithParams>();
	String contextObjectName = null;
	LPWithParams contextProperty = null;
	String initFilterPropertyName = null;
	List<String> initFilterPropertyMapping = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedFAProp($formName.sid, objects, mapping, contextObjectName, contextProperty, initFilterPropertyName, initFilterPropertyMapping, modalityType, sessionScope, checkOnOk, showDrop, noCancel, printType, exportType);
	}
}
	:	'FORM' formName=compoundID 
		('OBJECTS' list=formActionObjectList[context, dynamic] { objects = $list.objects; mapping = $list.exprs; })?
		('CONTEXTFILTER' objName=ID '=' contextPropertyExpr=propertyExpression[context, dynamic] { contextObjectName = $objName.text; contextProperty = $contextPropertyExpr.property; })?
		(initFilter = initFilterDefinition { initFilterPropertyMapping = $initFilter.mapping; initFilterPropertyName = $initFilter.propName; })?
		(sessScope = formSessionScopeLiteral { sessionScope = $sessScope.val; })?
		(modality = modalityTypeLiteral { modalityType = $modality.val; })?
		('CHECK' { checkOnOk = true; })?
		('SHOWDROP' { showDrop = true; })?
		('NOCANCEL' { noCancel = true; })?
		(print = formPrintTypeLiteral { printType = $print.val; })?
		(export = formExportTypeLiteral { exportType = $export.val; })?
	;

initFilterDefinition returns [String propName, List<String> mapping]
	:	'INITFILTER'
		(  pname=ID { $propName = $pname.text; }
	    	|  mappedProp=mappedPropertyDraw { $propName = $mappedProp.name; $mapping = $mappedProp.mapping; }
		)
	;

formActionObjectList[List<TypedParameter> context, boolean dynamic] returns [List<String> objects = new ArrayList<String>(), List<LPWithParams> exprs = new ArrayList<LPWithParams>()]
	:	objName=ID { $objects.add($objName.text); } '=' expr=propertyExpression[context, dynamic] { $exprs.add($expr.property); } 
		(',' objName=ID { $objects.add($objName.text); } '=' expr=propertyExpression[context, dynamic] { $exprs.add($expr.property); })*
	;
	
customActionDefinitionBody returns [LP property, List<ResolveClassSet> signature]
@init {
	boolean allowNullValue = false;
	List<String> classes = null;
}
@after {
	if (inPropParseState()) {
	    if($code.val == null)
	        $property = self.addScriptedCustomActionProp($classN.val, classes, allowNullValue);
	    else
		    $property = self.addScriptedCustomActionProp($code.val, allowNullValue);
		$signature = (classes == null ? Collections.<ResolveClassSet>nCopies($property.listInterfaces.size(), null) : self.createClassSetsFromClassNames(classes)); 
	}
}
	:	'CUSTOM' 
		(classN = stringLiteral ('(' cls=classIdList ')' { classes = $cls.ids; })? | code = codeLiteral)
	    ('NULL' { allowNullValue = true; })?
	;


addFormActionDefinitionBody returns [LP property, List<ResolveClassSet> signature]
@init {
	FormSessionScope scope = FormSessionScope.NEWSESSION;	
}
@after {
	if (inPropParseState()) {
		$signature = new ArrayList<ResolveClassSet>(); 
		$property = self.addScriptedAddFormAction($cls.sid, scope);	
	}
}
	:	'ADDFORM'
		(   'SESSION' { scope = FormSessionScope.OLDSESSION; }
		|   'NESTED'  { scope = FormSessionScope.NESTEDSESSION; }
		)?
		cls=classId
	;

editFormActionDefinitionBody returns [LP property, List<ResolveClassSet> signature]
@init {
	FormSessionScope scope = FormSessionScope.NEWSESSION;	
}
@after {
	if (inPropParseState()) {
		$signature = self.createClassSetsFromClassNames(Collections.singletonList($cls.sid)); 
		$property = self.addScriptedEditFormAction($cls.sid, scope);	
	}
}
	:	'EDITFORM'
	    (   'SESSION' { scope = FormSessionScope.OLDSESSION; }
	    |   'NESTED'  { scope = FormSessionScope.NESTEDSESSION; }
        )?
        cls=classId
	;

addObjectActionDefinitionBody[List<TypedParameter> context] returns [LPWithParams property]
@init {
	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context);
	LPWithParams condition = null;
	PropertyUsage toPropUsage = null;
	List<LPWithParams> toPropMapping = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedAddObjProp(context, $cid.sid, toPropUsage, toPropMapping, condition, newContext);
	}
}
	:	'ADDOBJ' cid=classId
		('WHERE' pe=propertyExpression[newContext, true] { condition = $pe.property; })?
		('TO' toProp=propertyUsage '(' params=singleParameterList[newContext, false] ')' { toPropUsage = $toProp.propUsage; toPropMapping = $params.props; } )?
	;

emailActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	LPWithParams fromProp = null;
	LPWithParams subjProp = null;
	
	List<Message.RecipientType> recipTypes = new ArrayList<Message.RecipientType>();
	List<LPWithParams> recipProps = new ArrayList<LPWithParams>();

	List<String> forms = new ArrayList<String>();
	List<FormStorageType> formTypes = new ArrayList<FormStorageType>();
	List<OrderedMap<String, LPWithParams>> mapObjects = new ArrayList<OrderedMap<String, LPWithParams>>();
	List<LPWithParams> attachNames = new ArrayList<LPWithParams>();
	List<AttachmentFormat> attachFormats = new ArrayList<AttachmentFormat>();
	List<LPWithParams> attachFileNames = new ArrayList<LPWithParams>();
	List<LPWithParams> attachFiles = new ArrayList<LPWithParams>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedEmailProp(fromProp, subjProp, recipTypes, recipProps, forms, formTypes, mapObjects, attachNames, attachFormats, attachFileNames, attachFiles);
	}
}
	:	'EMAIL'
		('FROM' fromExpr=propertyExpression[context, dynamic] { fromProp = $fromExpr.property; } )?
		'SUBJECT' subjExpr=propertyExpression[context, dynamic] { subjProp = $subjExpr.property; }
		(
			recipType=emailRecipientTypeLiteral { recipTypes.add($recipType.val); }
			recipExpr=propertyExpression[context, dynamic] { recipProps.add($recipExpr.property); }
		)*
		(	(	'INLINE' { formTypes.add(FormStorageType.INLINE); }
				form=compoundID { forms.add($form.sid); attachFormats.add(null); attachNames.add(null); }
				objects=emailActionFormObjects[context, dynamic] { mapObjects.add($objects.mapObjects); }
			)
		|	(	'ATTACH' { formTypes.add(FormStorageType.ATTACH); }
				format=emailAttachFormat { attachFormats.add($format.val); }
				
				{ LPWithParams attachName = null;}
				('NAME' attachNameExpr=propertyExpression[context, dynamic] { attachName = $attachNameExpr.property; } )?
				{ attachNames.add(attachName); }
				
				form=compoundID { forms.add($form.sid); }
				objects=emailActionFormObjects[context, dynamic] { mapObjects.add($objects.mapObjects); }
			)
		|	(	'ATTACH' 'FILE'
				
				attachFile=propertyExpression[context, dynamic] { attachFiles.add($attachFile.property); }
				
				{ LPWithParams attachFileName = null;}
				('NAME' attachFileNameExpr=propertyExpression[context, dynamic] { attachFileName = $attachFileNameExpr.property; } )?
				{ attachFileNames.add(attachFileName); }
			)
		)*
	;
	
emailActionFormObjects[List<TypedParameter> context, boolean dynamic] returns [OrderedMap<String, LPWithParams> mapObjects]
@init {
	$mapObjects = new OrderedMap<String, LPWithParams>();
}

	:	(	'OBJECTS'
			obj=ID '=' objValueExpr=propertyExpression[context, dynamic] { $mapObjects.put($obj.text, $objValueExpr.property); }
			(',' obj=ID '=' objValueExpr=propertyExpression[context, dynamic] { $mapObjects.put($obj.text, $objValueExpr.property); })*
		)?
	;

confirmActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedConfirmProp($pe.property);
	}
}
	:	'CONFIRM' pe=propertyExpression[context, dynamic]
	;
		
messageActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
    boolean noWait = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedMessageProp($pe.property, noWait);
	}
}
	:	'MESSAGE'
	    ('NO WAIT' { noWait = true; } )?
	    pe=propertyExpression[context, dynamic]
	;

asyncUpdateActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedAsyncUpdateProp($pe.property);
	}
}
	:	'ASYNCUPDATE' pe=propertyExpression[context, dynamic]
	;

seekObjectActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedObjectSeekProp($gobj.sid, $pe.property);
	}
}
	:	'SEEK' gobj=groupObjectID pe=propertyExpression[context, dynamic]
	;

fileActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	FileActionType actionType = null;
	LPWithParams fileProp = null;
	LPWithParams fileNameProp = null;
	
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedFileAProp(actionType, fileProp, fileNameProp);
	}
}
	:	(
			'LOADFILE' { actionType = FileActionType.LOAD; } pe=propertyExpression[context, dynamic] { fileProp = $pe.property; } 
		| 	'OPENFILE' { actionType = FileActionType.OPEN; } pe=propertyExpression[context, dynamic] { fileProp = $pe.property; }
		|	'SAVEFILE' { actionType = FileActionType.SAVE; } pe=propertyExpression[context, dynamic] { fileProp = $pe.property; } ('NAME' npe=propertyExpression[context, dynamic] { fileNameProp = $npe.property; })?
		) 
	;

changeClassActionDefinitionBody[List<TypedParameter> context] returns [LPWithParams property]
@init {
	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context);
	LPWithParams condition = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedChangeClassAProp(context.size(), newContext, $param.property, $className.sid, condition);	
	}
}
	:	'CHANGECLASS' param=singleParameter[newContext, true] 'TO' className=classId 
		('WHERE' pe=propertyExpression[newContext, false] { condition = $pe.property; })?
	;  

deleteActionDefinitionBody[List<TypedParameter> context] returns [LPWithParams property]
@init {
	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context);
	LPWithParams condition = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedDeleteAProp(context.size(), newContext, $param.property, condition);	
	}
}
	:	'DELETE' param=singleParameter[newContext, true] 
		('WHERE' pe=propertyExpression[newContext, false] { condition = $pe.property; })?
	;  

evalActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedEvalActionProp($expr.property);
	}
}
	:	'EVAL' expr=propertyExpression[context, dynamic]
	;
	
drillDownActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedDrillDownActionProp($expr.property);
	}
}
	:	'DRILLDOWN' expr=propertyExpression[context, dynamic]
	;	

focusActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
    FormEntity form = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedFocusActionProp($prop.propertyDraw);
	}
}
	:	'FOCUS'
		(namespacePart=ID '.')? formPart=ID '.'
		{   
			if (inPropParseState()) {
				form = self.findForm(($namespacePart != null ? $namespacePart.text + '.' : "") + $formPart.text);
			}
		}
		prop=formPropertySelector[form]
	;	

requestInputActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedRequestUserInputAProp($tid.sid, $objID.text, $aDB.property);
	}
}
	:	'REQUEST' tid=typeId
		(	'INPUT'
		|	(objID=ID)? aDB=innerActionDefinitionBody[context, dynamic]
		)
	;

listActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
	List<LP> localProps = new ArrayList<LP>();
	boolean newSession = false;
	List<PropertyUsage> migrateSessionProps = Collections.emptyList();
	boolean migrateAllSessionProps = false;
	boolean isNested = false;
	boolean singleApply = false;
	boolean newThread = false;
	long ldelay = 0;
	Long lperiod = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedListAProp(newSession, migrateSessionProps, migrateAllSessionProps, isNested, singleApply, props, localProps, newThread, ldelay, lperiod);
	}
}
	:	(
			(	'NEWSESSION' (mps=nestedPropertiesSelector { migrateAllSessionProps = $mps.all; migrateSessionProps = $mps.props; })?
			|	'NESTEDSESSION' { isNested = true; }
			|	('NEWTHREAD' { newThread = true; } (period=intLiteral { lperiod = (long)$period.val; })? ('DELAY' delay=intLiteral { ldelay = $delay.val; })? ) 
			)	{ newSession = true; }
			('SINGLE' { singleApply = true; })?
		)?
		'{'
			(	(aDB=innerActionDefinitionBody[context, dynamic] { props.add($aDB.property); }
				( {!self.semicolonNeeded()}?=>  | ';'))
			|	def=localDataPropertyDefinition ';' { localProps.add($def.property); }
			|	emptyStatement
			)*
		'}'
	;

nestedPropertiesSelector returns[boolean all = false, List<PropertyUsage> props = new ArrayList<PropertyUsage>()]
	:	'NESTED'
		(	'LOCAL' { $all = true; }
		|	list=nonEmptyPropertyUsageList { $props = $list.propUsages; }
		)
	;
	
localDataPropertyDefinition returns [LP property]
@after {
	if (inPropParseState()) {
		$property = self.addLocalDataProperty($propName.text, $returnClass.sid, $paramClasses.ids, $nlm.isNested);
	}
}
	:	'LOCAL'
		nlm = nestedLocalModifier
		propName=ID
		'=' returnClass=classId
		'('
			paramClasses=classIdList
		')'
	;

execActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean isInline = false;
} 
@after {
	if (inPropParseState()) {
		if (isInline) {
			$property = self.addScriptedJoinAProp($iProp.property, $exprList.props);
		} else {
			$property = self.addScriptedJoinAProp($uProp.propUsage, $exprList.props, context);
		}
	}
}
	:	('EXEC')?
		(	uProp=propertyUsage
		|	iProp=inlineProperty { isInline = true; }
		)
		'('
		exprList=propertyExpressionList[context, dynamic]
		')'
	;

assignActionDefinitionBody[List<TypedParameter> context] returns [LPWithParams property]
@init {
	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context); 
	LPWithParams condition = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedAssignPropertyAProp(context, $propUsage.propUsage, $params.props, $expr.property, condition, newContext);
	}
}
	:	('ASSIGN')?
		propUsage=propertyUsage
		'(' params=singleParameterList[newContext, true] ')'
		'<-'
		expr=propertyExpression[newContext, false] //no need to use dynamic context, because params should be either on global context or used in the left side
		('WHERE'
		whereExpr=propertyExpression[newContext, false] { condition = $whereExpr.property; })?
	;

tryActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedTryAProp($tryADB.property, $finallyADB.property);
	}
}
	:	'TRY' tryADB=innerActionDefinitionBody[context, dynamic] 
		('FINALLY' finallyADB=innerActionDefinitionBody[context, dynamic])?
	;

ifActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedIfAProp($expr.property, $thenADB.property, $elseADB.property);
	}
}
	:	'IF' expr=propertyExpression[context, dynamic] 
		'THEN' thenADB=innerActionDefinitionBody[context, dynamic]
		('ELSE' elseADB=innerActionDefinitionBody[context, dynamic])?
	;

caseActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property] 
@init {
	List<LPWithParams> whenProps = new ArrayList<LPWithParams>();
	List<LPWithParams> thenActions = new ArrayList<LPWithParams>();
	LPWithParams elseAction = null;
	boolean isExclusive = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedCaseAProp(whenProps, thenActions, elseAction, isExclusive); 
	}
}
	:	'CASE' (opt=exclusiveOverrideOption { isExclusive = $opt.isExclusive; })?
			( branch=actionCaseBranchBody[context, dynamic] { whenProps.add($branch.whenProperty); thenActions.add($branch.thenAction); } )+
			('ELSE' elseAct=innerActionDefinitionBody[context, dynamic] { elseAction = $elseAct.property; })?
	;

actionCaseBranchBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams whenProperty, LPWithParams thenAction]
	:	'WHEN' whenExpr=propertyExpression[context, dynamic] { $whenProperty = $whenExpr.property; }
		'THEN' thenAct=innerActionDefinitionBody[context, dynamic] { $thenAction = $thenAct.property; }
	;

applyActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean single = false;
	List<PropertyUsage> keepSessionProps = Collections.emptyList();
	boolean keepAllSessionProps = false;
	boolean serializable = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedApplyAProp($applyADB.property, single, keepSessionProps, keepAllSessionProps, serializable);
	}
}
	:	'APPLY' 
        (mps=nestedPropertiesSelector { keepAllSessionProps = $mps.all; keepSessionProps = $mps.props; })?
        ('SINGLE' { single = true; })?
        ('SERIALIZABLE' { serializable = true; })?
        applyADB=innerActionDefinitionBody[context, dynamic]
	;

multiActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property] 
@init {
	boolean isExclusive = true;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedMultiAProp($actList.props, isExclusive); 
	}
}
	:	'MULTI' (opt=exclusiveOverrideOption { isExclusive = $opt.isExclusive; })?
		actList=nonEmptyActionPDBList[context, dynamic]
	;

forAddObjClause[List<TypedParameter> context] returns [Integer paramCnt, String className]
@init {
	String varName = "added";
}
@after {
	if (inPropParseState()) {
		$paramCnt = self.getParamIndex(self.new TypedParameter($className, varName), context, true, insideRecursion);
	}
}
	:	'ADDOBJ'
		(varID=ID '=' {varName = $varID.text;})?
		addClass=classId { $className = $addClass.sid; }
	;

forActionPropertyDefinitionBody[List<TypedParameter> context] returns [LPWithParams property]
@init {
	boolean recursive = false;
	boolean descending = false;
	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context);
	List<LPWithParams> orders = new ArrayList<LPWithParams>();
	Inline inline = null;
	
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedForAProp(context, $expr.property, orders, $actDB.property, $elseActDB.property, $addObj.paramCnt, $addObj.className, recursive, descending, $in.noInline, $in.forceInline, newContext);
	}	
}
	:	(	'FOR' 
		| 	'WHILE' { recursive = true; }
		)
		(expr=propertyExpression[newContext, true]
		('ORDER'
			('DESC' { descending = true; } )? 
			ordExprs=nonEmptyPropertyExpressionList[newContext, false] { orders = $ordExprs.props; }
		)?)?
		in = inlineStatement[newContext]
		(addObj=forAddObjClause[newContext])?
		'DO' actDB=modifyContextFlowActionDefinitionBody[context, newContext, false, false]
		( {!recursive}?=> 'ELSE' elseActDB=innerActionDefinitionBody[context, false])?
	;

terminalFlowActionDefinitionBody returns [LPWithParams property]
@init {
	boolean isBreak = true;
}
@after {
	if (inPropParseState()) {
		$property =	self.getTerminalFlowActionProperty(isBreak);
	}
}
	:	'BREAK'
	|	'RETURN' { isBreak = false; }
	;


////////////////////////////////////////////////////////////////////////////////
/////////////////////////////OVERRIDE STATEMENT/////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

overrideStatement
@init {
	List<TypedParameter> context = new ArrayList<TypedParameter>();
	boolean dynamic = true;
	LPWithParams property = null;
	LPWithParams when = null;
}
@after {
	if (inPropParseState()) {
		self.addImplementationToAbstract($prop.propUsage, $list.params, property, when);
	}
}
	:	prop=propertyUsage
		'(' list=typedParameterList ')' { context = $list.params; dynamic = false; }
		'+='
		('WHEN' whenExpr=propertyExpression[context, dynamic] 'THEN' { when = $whenExpr.property; })?
		(	expr=propertyExpression[context, dynamic] { property = $expr.property; }
		|	action=concreteActionDefinition[context, dynamic] { property = $action.property; }
		)
		( {!self.semicolonNeeded()}?=>  | ';')
	;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// CONSTRAINT STATEMENT //////////////////////////
////////////////////////////////////////////////////////////////////////////////

constraintStatement 
@init {
	boolean checked = false;
	List<PropertyUsage> propUsages = null;
	ActionDebugInfo debugInfo = null; 
	if (inPropParseState()) {
		debugInfo = self.getEventStackDebugInfo();
	}
}
@after {
	if (inPropParseState()) {
		self.addScriptedConstraint($expr.property.property, $et.event, checked, propUsages, $message.property.property, debugInfo);
	}
}
	:	'CONSTRAINT'
		et=baseEvent	
		{
			if (inPropParseState()) {
				self.setPrevScope($et.event);
			}
		}
		expr=propertyExpression[new ArrayList<TypedParameter>(), true] { if (inPropParseState()) self.checkNecessaryProperty($expr.property); }
		{
			if (inPropParseState()) {
				self.dropPrevScope($et.event);
			}
		}
		('CHECKED' { checked = true; }
			('BY' list=nonEmptyPropertyUsageList { propUsages = $list.propUsages; })? 
		)?
		'MESSAGE' message=propertyExpression[new ArrayList<TypedParameter>(), false]
		';'
	;


////////////////////////////////////////////////////////////////////////////////
///////////////////////////////// FOLLOWS STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

followsStatement
@init {
	List<TypedParameter> context;
	PropertyUsage mainProp;
	Event event = Event.APPLY;
}
@after {
	if (inPropParseState()) {
		self.addScriptedFollows(mainProp, context, $fcl.pfollows, $fcl.prop, $fcl.event, $fcl.debug);
	}
}
	:	prop=mappedProperty { mainProp = $prop.propUsage; context = $prop.mapping; }
		'=>'
		fcl=followsClause[context] 
		';'
;
	
followsClause[List<TypedParameter> context] returns [LPWithParams prop, Event event = Event.APPLY, ActionDebugInfo debug, List<PropertyFollowsDebug> pfollows = new ArrayList<PropertyFollowsDebug>()] 
@init {
    $debug = self.getEventStackDebugInfo();
}
    :   expr = propertyExpression[context, false] 
        ('RESOLVE' 
        	('LEFT' {$pfollows.add(new PropertyFollowsDebug(true, self.getEventStackDebugInfo()));})?
        	('RIGHT' {$pfollows.add(new PropertyFollowsDebug(false, self.getEventStackDebugInfo()));})?
                et=baseEvent { $event = $et.event; } 
        )? { $prop = $expr.property; }
;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// WRITE STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

writeWhenStatement
@init {
    boolean action = false;
}
@after {
	if (inPropParseState()) {
		self.addScriptedWriteWhen($mainProp.propUsage, $mainProp.mapping, $valueExpr.property, $whenExpr.property, action);
	}
}
	:	mainProp=mappedProperty 
		'<-'
		{
			if (inPropParseState()) {
				self.setPrevScope(ChangeEvent.scope);
			}
		}
		valueExpr=propertyExpression[$mainProp.mapping, false]
		'WHEN'
		('DO' { action = true; })? // DO - undocumented syntax
		whenExpr=propertyExpression[$mainProp.mapping, false]
		{
			if (inPropParseState()) {
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
	List<TypedParameter> context = new ArrayList<TypedParameter>();
	List<LPWithParams> orderProps = new ArrayList<LPWithParams>();
	boolean descending = false;
	ActionDebugInfo debug = null;
	
	if (inPropParseState()) {
		debug = self.getEventStackDebugInfo(); 
	}
}
@after {
	if (inPropParseState()) {
		self.addScriptedEvent($whenExpr.property, $action.property, orderProps, descending, $et.event, $in.noInline, $in.forceInline, debug);
	} 
}
	:	'WHEN'
		et=baseEvent
		{
			if (inPropParseState()) {
				self.setPrevScope($et.event);
			}
		}
		whenExpr=propertyExpression[context, true]
		{
			if (inPropParseState()) {
				self.dropPrevScope($et.event);
			}
		}
		in=inlineStatement[context]
		'DO'
		action=topContextDependentActionDefinitionBody[context, false, false]
		(	'ORDER' ('DESC' { descending = true; })?
			orderList=nonEmptyPropertyExpressionList[context, false] { orderProps.addAll($orderList.props); }
		)?
		( {!self.semicolonNeeded()}?=>  | ';')
	;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////// GLOBAL EVENT STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

globalEventStatement
@init {
	boolean single = false;
}
@after {
	if (inPropParseState()) {
		self.addScriptedGlobalEvent($action.property, $et.event, single, $property.propUsage);
	}
}
	:	'ON' 
		et=baseEvent
		('SINGLE' { single = true; })?
		('SHOWDEP' property=propertyUsage)?
		{
			if (inPropParseState()) {
				self.setPrevScope($et.event);
			}
		}
		action=topContextDependentActionDefinitionBody[new ArrayList<TypedParameter>(), false, false]
		{
			if (inPropParseState()) {
				self.dropPrevScope($et.event);
			}
		}
		( {!self.semicolonNeeded()}?=>  | ';')
	;

baseEvent returns [Event event]
@init {
	SystemEvent baseEvent = SystemEvent.APPLY;
	List<String> ids = null;
	List<PropertyUsage> puAfters = null;
}
@after {
	if (inPropParseState()) {
		$event = self.createScriptedEvent(baseEvent, ids, puAfters);
	}
}
	:	('GLOBAL' { baseEvent = SystemEvent.APPLY; } | 'SESSION' { baseEvent = SystemEvent.SESSION; })?
		('FORMS' (neIdList=nonEmptyCompoundIdList { ids = $neIdList.ids; }) )?
		('GOAFTER' (nePropList=nonEmptyPropertyUsageList { puAfters = $nePropList.propUsages; }) )?
	;

inlineStatement[List<TypedParameter> context] returns [List<LPWithParams> noInline = new ArrayList<LPWithParams>(), boolean forceInline = false]
	:   ('NOINLINE' { $noInline = null; } ( '(' params=singleParameterList[context, false] { $noInline = $params.props; } ')' )? )?
	    ('INLINE' { $forceInline = true; })?
	;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// SHOWDEP STATEMENT //////////////////////////////
////////////////////////////////////////////////////////////////////////////////

showDepStatement
@after {
    if (inPropParseState()) {
        self.addScriptedShowDep($property.propUsage, $propFrom.propUsage);
    }
}
    :	'SHOWDEP'
        property=propertyUsage
        'FROM'
        propFrom=propertyUsage
        ';'
    ;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// ASPECT STATEMENT //////////////////////////////
////////////////////////////////////////////////////////////////////////////////

aspectStatement
@init {
	List<TypedParameter> context = new ArrayList<TypedParameter>();
	boolean before = true;
}
@after {
	if (inPropParseState()) {
		self.addScriptedAspect($mainProp.propUsage, $mainProp.mapping, $action.property, before);
	}
}
	:	(	'BEFORE' 
		| 	'AFTER' { before = false; }
		)
		mainProp=mappedProperty 'DO' action=topContextDependentActionDefinitionBody[$mainProp.mapping, false, false]
		( {!self.semicolonNeeded()}?=>  | ';')
	;


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// TABLE STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

tableStatement 
@init {
	boolean isFull = false;
}
@after {
	if (inTableParseState()) {
		self.addScriptedTable($name.text, $list.ids, isFull);
	}
}
	:	'TABLE' name=ID '(' list=nonEmptyClassIdList ')' ('FULL' {isFull = true;})? ';';

////////////////////////////////////////////////////////////////////////////////
/////////////////////////////// LOGGABLE STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

loggableStatement
@after {
	if (inPropParseState()) {
		self.addScriptedLoggable($list.propUsages);
	}	
}
	:	'LOGGABLE' list=nonEmptyPropertyUsageList ';'
	;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// INDEX STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

// duplicate of singleParameter because of different parse states
singleParameterIndex[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	String className = null;
}
@after {
	if (inIndexParseState()) {
		$property = new LPWithParams(null, Collections.singletonList(self.getParamIndex(TP(className, $paramName.text), $context, $dynamic, insideRecursion)));
	}
}
	:	(clsId=classId { className = $clsId.sid; })? paramName=parameter
	;

singleParameterIndexList[List<TypedParameter> context, boolean dynamic] returns [List<LPWithParams> props]
    @init {
    	props = new ArrayList<LPWithParams>();
    }
    	:	(first=singleParameterIndex[context, dynamic] { props.add($first.property); }
    		(',' next=singleParameterIndex[context, dynamic] { props.add($next.property); })*)?
    	;


mappedPropertyOrSimpleExprParam[List<TypedParameter> context] returns [LPWithParams property]
    :   (   toProp=propertyUsage '(' params=singleParameterIndexList[context, true] ')' { if(inIndexParseState()) { $property = self.findIndexProp($toProp.propUsage, $params.props, context); } }
        |   param=singleParameterIndex[context, true] { $property = $param.property; }
        )
;

nonEmptyMappedPropertyOrSimpleExprParamList[List<TypedParameter> context] returns [List<LPWithParams> props]
@init {
	$props = new ArrayList<LPWithParams>();
}
	:	first=mappedPropertyOrSimpleExprParam[context] { $props.add($first.property); }
		(',' next=mappedPropertyOrSimpleExprParam[context] { $props.add($next.property); })*
	;

indexStatement
@init {
	List<TypedParameter> context = new ArrayList<TypedParameter>();
}
@after {
	if (inIndexParseState()) {
		self.addScriptedIndex(context, $list.props);
	}	
}
	:	'INDEX' list=nonEmptyMappedPropertyOrSimpleExprParamList[context] ';'
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
	if (inPropParseState()) {
		self.addScriptedWindow($type.type, $name.name, $name.caption, $opts.options);
	}
}
	:	'WINDOW' name=simpleNameWithCaption type=windowType opts=windowOptions  ';'
	;

windowHideStatement
	:	'HIDE' 'WINDOW' wid=compoundID ';'
		{
			if (inPropParseState()) {
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
			(	addNavigatorElementStatement[parentElement]
			|	newNavigatorElementStatement[parentElement]
			|	setupNavigatorElementStatement[parentElement]
			|	emptyStatement
			)*
		'}'
	| emptyStatement
	;

addNavigatorElementStatement[NavigatorElement parentElement]
	:	'ADD' elem=navigatorElementSelector (caption=stringLiteral)? opts=navigatorElementOptions
		{
			if (inPropParseState()) {
				self.setupNavigatorElement($elem.element, $caption.val, $parentElement, $opts.options, true);
			}
		}
		navigatorElementStatementBody[$elem.element]
	;

newNavigatorElementStatement[NavigatorElement parentElement]
@init {
	NavigatorElement newElement = null;
}
	:	'NEW' id=ID (caption=stringLiteral)? ('ACTION' au=propertyUsage)? opts=navigatorElementOptions
		{
			if (inPropParseState()) {
				newElement = self.createScriptedNavigatorElement($id.text, $caption.val, $parentElement, $opts.options, $au.propUsage);
			}
		}
		navigatorElementStatementBody[newElement]
	;

navigatorElementOptions returns [NavigatorElementOptions options] 
@init {
	$options = new NavigatorElementOptions();
	$options.position = InsertPosition.IN;
}
	:	
	(	'WINDOW' wid=compoundID { $options.windowName = $wid.sid; }
	|	pos=navigatorElementInsertPosition { $options.position = $pos.position; $options.anchor = $pos.anchor; }
	|	'IMAGE' path=stringLiteral { $options.imagePath = $path.val; }	
	)*
	;
	
navigatorElementInsertPosition returns [InsertPosition position, NavigatorElement anchor]
@init {
	$anchor = null;
}
	:	pos=insertRelativePositionLiteral { $position = $pos.val; } elem=navigatorElementSelector { $anchor = $elem.element; }
	|	'FIRST' { $position = InsertPosition.FIRST; }
	;

setupNavigatorElementStatement[NavigatorElement parentElement]
	:	elem=navigatorElementSelector (caption=stringLiteral)? opts=navigatorElementOptions
		{
			if (inPropParseState()) {
				self.setupNavigatorElement($elem.element, $caption.val, $parentElement, $opts.options, false);
			}
		}
		navigatorElementStatementBody[$elem.element]
	;
	
navigatorElementSelector returns [NavigatorElement element]
	:	cid=compoundID
		{
			if (inPropParseState()) {
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
	String caption = null;
}
@after {
	if (inPropParseState()) {
		$view = self.getFormDesign($cid.sid, caption, customDesign);
	}
}
	:	'DESIGN' cid=compoundID (s=stringLiteral { caption = s.val; })? ('CUSTOM' { customDesign = true; })?
	;

componentStatementBody [ComponentView parentComponent]
	:	'{'
		(	setObjectPropertyStatement[parentComponent]
		|	setupComponentStatement
		|	setupGroupObjectStatement
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

setupGroupObjectStatement
@init {
	GroupObjectView groupObject = null;
}
	:	'GROUP'
		'('
			ID
			{
				if (inPropParseState()) {
					groupObject = $designStatement::design.getGroupObject($ID.text, self.getVersion());
				}
			}
		')'
		'{'
			( setObjectPropertyStatement[groupObject]
			| emptyStatement
			)*
		'}'
	;

newComponentStatement[ComponentView parentComponent]
@init {
	ComponentView newComp = null;
}
	:	'NEW' cid=multiCompoundID insPosition=componentInsertPosition
		{
			if (inPropParseState()) {
				newComp = $designStatement::design.createNewComponent($cid.sid, parentComponent, insPosition.position, insPosition.anchor, self.getVersion());
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
			if (inPropParseState()) {
				$designStatement::design.moveComponent(insComp, parentComponent, insPosition.position, insPosition.anchor, self.getVersion());
			}
		}
		componentStatementBody[insComp]
	;
	
componentInsertPosition returns [InsertPosition position, ComponentView anchor]
@init {
	$position = InsertPosition.IN;
	$anchor = null;
}
	:	(	(pos=insertRelativePositionLiteral { $position = $pos.val; } comp=componentSelector { $anchor = $comp.component; })
		|	'FIRST' { $position = InsertPosition.FIRST; }
		)?
	;

removeComponentStatement
	:	'REMOVE' compSelector=componentSelector ';'
		{
			if (inPropParseState()) {
				$designStatement::design.removeComponent($compSelector.component, self.getVersion());
			}
		}
	;

componentSelector returns [ComponentView component]
	:	'PARENT' '(' child=componentSelector ')'
		{
			if (inPropParseState()) {
				$designStatement::design.getParentContainer($child.component, self.getVersion());
			}
		}
	|	'PROPERTY' '(' prop=propertySelector ')' { $component = $prop.propertyView; }
	|	mid=multiCompoundID
		{
			if (inPropParseState()) {
				$component = $designStatement::design.getComponentBySID($mid.sid, self.getVersion());
			}
		}
	;


propertySelector returns [PropertyDrawView propertyView = null]
	:	pname=ID
		{
			if (inPropParseState()) {
				$propertyView = $designStatement::design.getPropertyView($pname.text, self.getVersion());
			}
		}
	|	mappedProp=mappedPropertyDraw	
		{
			if (inPropParseState()) {
				$propertyView = $designStatement::design.getPropertyView($mappedProp.name, $mappedProp.mapping, self.getVersion());
			}
		}
	;

setObjectPropertyStatement[Object propertyReceiver] returns [String id, Object value]
	:	ID '=' componentPropertyValue ';'  { setObjectProperty($propertyReceiver, $ID.text, $componentPropertyValue.value); }
	;

componentPropertyValue returns [Object value]
	:   c=colorLiteral { $value = $c.val; }
	|   s=stringLiteral { $value = $s.val; }
	|   i=intLiteral { $value = $i.val; }
	|   l=longLiteral { $value = $l.val; }
	|   d=doubleLiteral { $value = $d.val; }
	|   dim=dimensionLiteral { $value = $dim.val; }
	|   b=booleanLiteral { $value = $b.val; }
	|   intB=boundsIntLiteral { $value = $intB.val; }
	|   doubleB=boundsDoubleLiteral { $value = $doubleB.val; }
	|   contType=containerTypeLiteral { $value = $contType.val; }
	|   alignment=flexAlignmentLiteral { $value = $alignment.val; }
	|   calcProp=designCalcPropertyObject { $value = $calcProp.property; }
	;
	
designCalcPropertyObject returns [CalcPropertyObjectEntity property = null]
	:	mProperty=formMappedProperty
		{
			if (inPropParseState()) {
				$property = $designStatement::design.addCalcPropertyObject($mProperty.propUsage, $mProperty.mapping);
			}
		}
	;


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// META STATEMENT //////////////////////////////
////////////////////////////////////////////////////////////////////////////////

metaCodeDeclarationStatement
@init {
	String code;
	List<String> tokens;
	int lineNumber = self.getParser().getCurrentParserLineNumber(); 
}
@after {
	if (inInitParseState()) {
		self.addScriptedMetaCodeFragment($id.text, $list.ids, tokens, $text, lineNumber);
	}
}
	
	:	'META' id=ID '(' list=idList ')'  
		{
			tokens = self.grabMetaCode($id.text);
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
	ids = new ArrayList<String>();
}
	:		firstId=metaCodeId { ids.add($firstId.sid); }
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

mappedProperty returns [PropertyUsage propUsage, List<TypedParameter> mapping]
	:	propU=propertyUsage { $propUsage = $propU.propUsage; }
		'('
		list=typedParameterList { $mapping = $list.params; }
		')'
	;

parameter
	:	ID | NUMBERED_PARAM | RECURSIVE_PARAM
	;

typedParameter returns [TypedParameter param]
@after {
	if (inPropParseState()) {
		$param = self.new TypedParameter($cname.sid, $pname.text);
	}
}
	:	(cname=classId)? pname=ID
	;

simpleNameWithCaption returns [String name, String caption] 
	:	simpleName=ID { $name = $simpleName.text; }
		(captionStr=stringLiteral { $caption = $captionStr.val; })?
	;
	
idList returns [List<String> ids] 
@init {
	ids = new ArrayList<String>();	
} 
	:	(neIdList=nonEmptyIdList { ids = $neIdList.ids; })?
	;

classIdList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:	(neList=nonEmptyClassIdList { ids = $neList.ids; })?
	;

nonEmptyClassIdList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:	firstClassName=classId { ids.add($firstClassName.sid); }
		(',' className=classId { ids.add($className.sid); })*
	;

signatureClassList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:	(neList=nonEmptySignatureClassList { ids = $neList.ids; })?
	;

nonEmptySignatureClassList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:	firstClassName=signatureClass { ids.add($firstClassName.sid); }
		(',' className=signatureClass { ids.add($className.sid); })*
	;

typedParameterList returns [List<TypedParameter> params]
@init {
	params = new ArrayList<TypedParameter>();
}
	:	(neList=nonEmptyTypedParameterList { $params = $neList.params; })?
	;

nonEmptyTypedParameterList returns [List<TypedParameter> params]
@init {
	params = new ArrayList<TypedParameter>();
}
	:	firstParam=typedParameter { params.add($firstParam.param); }
		(',' param=typedParameter { params.add($param.param); })*
	;


compoundIdList returns [List<String> ids] 
@init {
	ids = new ArrayList<String>();	
} 
	:	(neIdList=nonEmptyCompoundIdList { ids = $neIdList.ids; })?
	;

nonEmptyIdList returns [List<String> ids]
@init {
	ids = new ArrayList<String>(); 
}
	:	firstId=ID	{ $ids.add($firstId.text); }
		(',' nextId=ID	{ $ids.add($nextId.text); })*
	;

nonEmptyCompoundIdList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:	firstId=compoundID	{ $ids.add($firstId.sid); }
		(',' nextId=compoundID	{ $ids.add($nextId.sid); })*
	;

nonEmptyPropertyUsageList returns [List<PropertyUsage> propUsages]
@init {
	$propUsages = new ArrayList<PropertyUsage>();
}
	:	first=propertyUsage { $propUsages.add($first.propUsage); }
		(',' next=propertyUsage { $propUsages.add($next.propUsage); })* 
	; 

singleParameterList[List<TypedParameter> context, boolean dynamic] returns [List<LPWithParams> props]
@init {
	props = new ArrayList<LPWithParams>();
}
	:	(first=singleParameter[context, dynamic] { props.add($first.property); }
		(',' next=singleParameter[context, dynamic] { props.add($next.property); })*)?
	;

actionPDBList[List<TypedParameter> context, boolean dynamic] returns [List<LPWithParams> props] 
@init {
	$props = new ArrayList<LPWithParams>();
}
	:	(neList=nonEmptyActionPDBList[context, dynamic] { $props = $neList.props; })?
	;

nonEmptyActionPDBList[List<TypedParameter> context, boolean dynamic] returns [List<LPWithParams> props]
@init {
	$props = new ArrayList<LPWithParams>();
}
	:	first=innerActionDefinitionBody[context, dynamic] { $props.add($first.property); }
		(',' next=innerActionDefinitionBody[context, dynamic] { $props.add($next.property); })* 
	; 

propertyExpressionList[List<TypedParameter> context, boolean dynamic] returns [List<LPWithParams> props] 
@init {
	$props = new ArrayList<LPWithParams>();
}
	:	(neList=nonEmptyPropertyExpressionList[context, dynamic] { $props = $neList.props; })?
	;
	

nonEmptyPropertyExpressionList[List<TypedParameter> context, boolean dynamic] returns [List<LPWithParams> props]
@init {
	$props = new ArrayList<LPWithParams>();
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
	if (inPropParseState()) {
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
	|	vstr=stringLiteral	{ $cls = ScriptingLogicsModule.ConstType.STRING; $value = $vstr.val; }  
	|	vbool=booleanLiteral	{ $cls = ScriptingLogicsModule.ConstType.LOGICAL; $value = $vbool.val; }
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

groupObjectID returns [String sid]
    :	(namespacePart=ID '.')? formPart=ID '.' namePart=ID { $sid = ($namespacePart != null ? $namespacePart.text + '.' : "") + $formPart.text + '.' + $namePart.text; }
    ;

multiCompoundID returns [String sid]
	:	id=ID { $sid = $id.text; } ('.' cid=ID { $sid = $sid + '.' + $cid.text; } )*
	;

exclusiveOverrideOption returns [boolean isExclusive]
	:	'OVERRIDE' { $isExclusive = false; }
	|	'EXCLUSIVE'{ $isExclusive = true; } 
	;

abstractExclusiveOverrideOption returns [boolean isExclusive, boolean isLast = true]
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

dateLiteral returns [java.sql.Date val]
	:	date=DATE_LITERAL { $val = self.dateLiteralToDate($date.text); }
	;

dateTimeLiteral returns [java.sql.Timestamp val]
	:	time=DATETIME_LITERAL { $val = self.dateTimeLiteralToTimestamp($time.text); }
	;

timeLiteral returns [java.sql.Time val]
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
	:	s=CODE_LITERAL { $val = self.transformStringLiteral($s.text); }
	;

insertRelativePositionLiteral returns [InsertPosition val]
	:	'BEFORE' { $val = InsertPosition.BEFORE; }
	|	'AFTER' { $val = InsertPosition.AFTER; }
	;

containerTypeLiteral returns [ContainerType val]
	:	'CONTAINERV' { $val = ContainerType.CONTAINERV; }	
	|	'CONTAINERH' { $val = ContainerType.CONTAINERH; }	
	|	'COLUMNS' { $val = ContainerType.COLUMNS; }
	|	'TABBED' { $val = ContainerType.TABBED_PANE; }
	|	'SPLITH' { $val = ContainerType.HORIZONTAL_SPLIT_PANE; }
	|	'SPLITV' { $val = ContainerType.VERTICAL_SPLIT_PANE; }
	;

alignmentLiteral returns [Alignment val]
    :   'LEADING' { $val = Alignment.LEADING; }
    |   'CENTER' { $val = Alignment.CENTER; }
    |   'TRAILING' { $val = Alignment.TRAILING; }
    ;

flexAlignmentLiteral returns [FlexAlignment val]
    :   'LEADING' { $val = FlexAlignment.LEADING; }
    |   'CENTER' { $val = FlexAlignment.CENTER; }
    |   'TRAILING' { $val = FlexAlignment.TRAILING; }
    |   'STRETCH' { $val = FlexAlignment.STRETCH; }
    ;

propertyEditTypeLiteral returns [PropertyEditType val]
	:	'EDITABLE' { $val = PropertyEditType.EDITABLE; }
	|	'READONLY' { $val = PropertyEditType.READONLY; }
	|	'SELECTOR' { $val = PropertyEditType.SELECTOR; }
	;

modalityTypeLiteral returns [ModalityType val]
	:	'DOCKED' { $val = ModalityType.DOCKED; }
	|	'MODAL' { $val = ModalityType.MODAL; }
	|	'DOCKEDMODAL' { $val = ModalityType.DOCKED_MODAL; }
	|	'FULLSCREEN' { $val = ModalityType.FULLSCREEN_MODAL; }
	|	'DIALOG' { $val = ModalityType.DIALOG_MODAL; }
	;
	
formPrintTypeLiteral returns [FormPrintType val]
@init {
	$val = FormPrintType.PRINT;
} 
	:	'PRINT'
		(	'AUTO' { $val = FormPrintType.AUTO; }
		|	'XLS'  { $val = FormPrintType.XLS; }
		|	'XLSX' { $val = FormPrintType.XLSX; }
		|	'PDF' { $val = FormPrintType.PDF; }
		)?
	;
	
formExportTypeLiteral returns [FormExportType val]
	:	'EXPORT'
		(	'DOC'  { $val = FormExportType.DOC; }
		|	'DOCX' { $val = FormExportType.DOCX; }
		|	'PDF'  { $val = FormExportType.PDF; }
		|	'XLS' { $val = FormExportType.XLS; }
		|	'XLSX' { $val = FormExportType.XLSX; }
		)
	;	

formSessionScopeLiteral returns [FormSessionScope val]
	:	'OLDSESSION' { $val = FormSessionScope.OLDSESSION; }
	|	'NEWSESSION' { $val = FormSessionScope.NEWSESSION; }
	|	'NESTEDSESSION' { $val = FormSessionScope.NESTEDSESSION; }
	|	'MANAGESESSION' { $val = FormSessionScope.MANAGESESSION; }
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
fragment STR_LITERAL_CHAR	: '\\\'' | '\\\\' | ~('\r'|'\n'|'\'');	 // overcomplicated due to bug in ANTLR Works
fragment DIGIT		:	'0'..'9';
fragment DIGITS		:	('0'..'9')+;
fragment EDIGITS	:	('0'..'9')*;
fragment HEX_DIGIT	: 	'0'..'9' | 'a'..'f' | 'A'..'F';
fragment FIRST_ID_LETTER	: ('a'..'z'|'A'..'Z');
fragment NEXT_ID_LETTER		: ('a'..'z'|'A'..'Z'|'_'|'0'..'9');
fragment OPEN_CODE_BRACKET	: '<{';
fragment CLOSE_CODE_BRACKET : '}>';

PRIMITIVE_TYPE  :	'INTEGER' | 'DOUBLE' | 'LONG' | 'BOOLEAN' | 'DATE' | 'DATETIME' | 'YEAR' | 'TEXT'  | 'RICHTEXT' | 'TIME' | 'WORDFILE' | 'IMAGEFILE' | 'PDFFILE' | 'CUSTOMFILE' | 'EXCELFILE' | 'STRING[' DIGITS ']' | 'ISTRING[' DIGITS ']'  | 'VARSTRING[' DIGITS ']' | 'VARISTRING[' DIGITS ']' | 'NUMERIC[' DIGITS ',' DIGITS ']' | 'COLOR';
LOGICAL_LITERAL :	'TRUE' | 'FALSE';
NULL_LITERAL	:	'NULL';	
ID          	:	FIRST_ID_LETTER NEXT_ID_LETTER*;
WS		:	(NEWLINE | SPACE) { $channel=HIDDEN; };
STRING_LITERAL	:	'\'' STR_LITERAL_CHAR* '\'';
COLOR_LITERAL 	:	'#' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT;
COMMENTS	:	('//' .* '\n') { $channel=HIDDEN; };
UINT_LITERAL 	:	DIGITS;
ULONG_LITERAL	:	DIGITS('l'|'L');
UDOUBLE_LITERAL	:	DIGITS '.' EDIGITS('d'|'D');
UNUMERIC_LITERAL:	DIGITS '.' EDIGITS;	  
DATE_LITERAL	:	DIGIT DIGIT DIGIT DIGIT '_' DIGIT DIGIT '_' DIGIT DIGIT; 
DATETIME_LITERAL:	DIGIT DIGIT DIGIT DIGIT '_' DIGIT DIGIT '_' DIGIT DIGIT '_' DIGIT DIGIT ':' DIGIT DIGIT;	
TIME_LITERAL	:	DIGIT DIGIT ':' DIGIT DIGIT;
NUMBERED_PARAM	:	'$' DIGITS;
RECURSIVE_PARAM :	'$' FIRST_ID_LETTER NEXT_ID_LETTER*;	
EQ_OPERAND	:	('==') | ('!=');
LESS_OPERAND	: 	('<');
GR_OPERAND	:	('>');
RELEQ_OPERAND	: 	('<=') | ('>=');
MINUS		:	'-';
PLUS		:	'+';
MULT		:	'*';
DIV		:	'/';
ADDOR_OPERAND	:	'(+)' | {ahead("(-)")}?=> '(-)';
CONCAT_OPERAND	:	'##';
CONCAT_CAPITALIZE_OPERAND	:	'###';
CODE_LITERAL    : OPEN_CODE_BRACKET .* CLOSE_CODE_BRACKET;