grammar LsfLogics;

@header {
	package platform.server;

	import platform.base.OrderedMap;
	import platform.interop.ClassViewType;
	import platform.interop.PropertyEditType;
	import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
	import platform.interop.form.layout.ContainerType;
	import platform.interop.form.ServerResponse;
	import platform.interop.FormEventType;
	import platform.interop.ModalityType;
	import platform.server.form.instance.FormSessionScope;
	import platform.server.data.Union;
	import platform.server.data.expr.query.PartitionType;
	import platform.server.form.entity.GroupObjectEntity;
	import platform.server.form.entity.PropertyObjectEntity;
	import platform.server.form.entity.PropertyDrawEntity;
	import platform.server.form.entity.ActionPropertyObjectEntity;
	import platform.server.form.entity.CalcPropertyObjectEntity;
	import platform.server.form.navigator.NavigatorElement;
	import platform.server.form.view.ComponentView;
	import platform.server.form.view.GroupObjectView;
	import platform.server.form.view.PropertyDrawView;
	import platform.server.logics.linear.LP;
	import platform.server.logics.property.PropertyFollows;
	import platform.server.logics.property.Cycle;
	import platform.server.logics.scripted.*;
	import platform.server.logics.scripted.ScriptingLogicsModule.WindowType;
	import platform.server.logics.scripted.ScriptingLogicsModule.InsertPosition;
	import platform.server.logics.scripted.ScriptingLogicsModule.GroupingType;
	import platform.server.logics.scripted.ScriptingLogicsModule.LPWithParams;
	import platform.server.mail.EmailActionProperty.FormStorageType;
	import platform.server.mail.AttachmentFormat;
	import platform.server.logics.property.actions.flow.Inline;
	import platform.server.logics.property.actions.SystemEvent;
	import platform.server.logics.property.Event;
	import javax.mail.Message;
	
	import java.util.*;
	import java.awt.*;
	import org.antlr.runtime.BitSet;
	import java.util.List;
	import java.sql.Date;

	import static java.util.Arrays.asList;
	import static platform.interop.form.layout.SingleSimplexConstraint.*;
	import static platform.server.logics.scripted.ScriptingLogicsModule.WindowType.*;
}

@lexer::header { 
	package platform.server; 
	import platform.server.logics.scripted.ScriptingLogicsModule;
	import platform.server.logics.scripted.ScriptParser;
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

	public List<GroupObjectEntity> getGroupObjectsList(List<String> ids) throws ScriptingErrorLog.SemanticErrorException {
		if (inPropParseState()) {
			return $formStatement::form.getGroupObjectsList(ids);
		}
		return null;
	}

	public MappedProperty getPropertyWithMapping(String name, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
		if (inPropParseState()) {
			return $formStatement::form.getPropertyWithMapping(name, mapping);
		}
		return null;
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
		|   showDepStatement
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
	List<String> instanceNames = new ArrayList<String>();
	List<String> instanceCaptions = new ArrayList<String>();
}
@after {
	if (inClassParseState()) {
		self.addScriptedClass($nameCaption.name, $nameCaption.caption, isAbstract, $classData.names, $classData.captions, $classData.parents);
	}
}
	:	'CLASS' ('ABSTRACT' {isAbstract = true;})?
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
}
@after {
	if (inPropParseState() && initialDeclaration) {
		self.addScriptedForm($formStatement::form);
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
		|	formOrderByList
		|	dialogFormDeclaration
		|	editFormDeclaration
		|	listFormDeclaration
		)*
		';'
	;

dialogFormDeclaration
	:	'DIALOG' cid=classId 'OBJECT' oid=ID
		{
			if (inPropParseState()) {
				$formStatement::form.setAsDialogForm($cid.sid, $oid.text);
			}
		}
	;

editFormDeclaration
	:	'EDIT' cid=classId 'OBJECT' oid=ID
		{
			if (inPropParseState()) {
				$formStatement::form.setAsEditForm($cid.sid, $oid.text);
			}
		}
	;
	
listFormDeclaration
	:	'LIST' cid=classId 'OBJECT' oid=ID
		{
			if (inPropParseState()) {
				$formStatement::form.setAsListForm($cid.sid, $oid.text);
			}
		}
	;

formDeclaration returns [ScriptingFormEntity form]
@init {
	boolean isPrint = false;
	ModalityType modalityType = null;
}
@after {
	if (inPropParseState()) {
		$form = self.createScriptedForm($formNameCaption.name, $formNameCaption.caption, $title.val, $path.val);
		$form.setIsPrintForm(isPrint);
		$form.setModalityType(modalityType);
	}
}
	:	'FORM' 
		formNameCaption=simpleNameWithCaption
		('TITLE' title=stringLiteral)?
		('PRINT' { isPrint = true; })?
		(modality = modalityTypeLiteral { modalityType = $modality.val; })?
		('IMAGE' path=stringLiteral)?
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
		$formStatement::form.addScriptingGroupObjects(groups);
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
	List<List<String>> properties = new ArrayList<List<String>>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptingTreeGroupObject(treeSID, groups, properties);
	}
}
	:	'TREE'
		(id = ID { treeSID = $id.text; })?
		groupElement=formTreeGroupObjectDeclaration { groups.add($groupElement.groupObject); properties.add($groupElement.properties); }
		(',' groupElement=formTreeGroupObjectDeclaration { groups.add($groupElement.groupObject); properties.add($groupElement.properties); })*
	;

formGroupObjectDeclaration returns [ScriptingGroupObject groupObject]
	:	(object = formCommonGroupObject { $groupObject = $object.groupObject; })	
		(path = formGroupObjectReportPath { $groupObject.setReportPathProp($path.propName, $path.mapping); })?
		(viewType = formGroupObjectViewType { $groupObject.setViewType($viewType.type, $viewType.isInitType); } )?
		(pageSize = formGroupObjectPageSize { $groupObject.setPageSize($pageSize.value); })?
	; 

formTreeGroupObjectDeclaration returns [ScriptingGroupObject groupObject, List<String> properties]
	:	(object = formCommonGroupObject { $groupObject = $object.groupObject; })
		(parent = treeGroupParentDeclaration { $properties = $parent.properties; })?
	; 

treeGroupParentDeclaration returns [List<String> properties = new ArrayList<String>()]
	:	'PARENT'
		(	id = compoundID { $properties.add($id.sid); }
		|	'('
				list=nonEmptyCompoundIdList { $properties.addAll($list.ids); }
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

formGroupObjectReportPath returns [String propName, List<String> mapping]
	:	'REPORTFILE' prop=mappedProperty { $propName = $prop.name; $mapping = $prop.mapping; }
	;

formGroupObjectViewType returns [ClassViewType type, boolean isInitType]
	: 	('INIT' {$isInitType = true;} | 'FIXED' {$isInitType = false;})
		viewType=classViewType { $type = $viewType.type; }
	;

classViewType returns [ClassViewType type]
	: 	('PANEL' {$type = ClassViewType.PANEL;} | 'HIDE' {$type = ClassViewType.HIDE;} | 'GRID' {$type = ClassViewType.GRID;})
	;

formGroupObjectPageSize returns [Integer value = null]
	:	'PAGESIZE' size = intLiteral { $value = $size.val; }
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
	List<String> properties = new ArrayList<String>();
	List<String> aliases = new ArrayList<String>();
	List<List<String>> mapping = new ArrayList<List<String>>();
	FormPropertyOptions commonOptions = null;
	List<FormPropertyOptions> options = new ArrayList<FormPropertyOptions>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedPropertyDraws(properties, aliases, mapping, commonOptions, options);
	}
}
	:	'PROPERTIES' '(' objects=idList ')' opts=formPropertyOptionsList list=formPropertiesNamesList
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
		|   'TOOLBAR' { $options.setDrawToToolbar(true); }
		|	'COLUMNS' '(' ids=nonEmptyIdList ')' { $options.setColumns(getGroupObjectsList($ids.ids)); }
		|	'SHOWIF' mappedProp=mappedProperty { $options.setShowIf(getPropertyWithMapping($mappedProp.name, $mappedProp.mapping)); }  // refactor to formPropertyObject? 
		|	'READONLYIF' propObj=formCalcPropertyObject { $options.setReadOnlyIf($propObj.property); }
		|	'BACKGROUND' propObj=formCalcPropertyObject { $options.setBackground($propObj.property); }
		|	'FOREGROUND' propObj=formCalcPropertyObject { $options.setForeground($propObj.property); }
		|	'HEADER' propObj=formCalcPropertyObject { $options.setHeader($propObj.property); }
		|	'FOOTER' propObj=formCalcPropertyObject { $options.setFooter($propObj.property); }
		|	'FORCE' viewType=classViewType { $options.setForceViewType($viewType.type); }
		|	'TODRAW' toDraw=formGroupObjectEntity { $options.setToDraw($toDraw.groupObject); }
		|	'BEFORE' pdraw=formPropertyDraw { $options.setNeighbourPropertyDraw($pdraw.property, $pdraw.text); $options.setNeighbourType(false); }
		|	'AFTER'  pdraw=formPropertyDraw { $options.setNeighbourPropertyDraw($pdraw.property, $pdraw.text); $options.setNeighbourType(true); }
		|	'ON' 'EDIT' prop=formActionPropertyObject { $options.addEditAction(ServerResponse.EDIT_OBJECT, $prop.action); }
		|	'ON' 'CHANGE' prop=formActionPropertyObject { $options.addEditAction(ServerResponse.CHANGE, $prop.action); }
		|	'ON' 'CHANGEWYS' prop=formActionPropertyObject { $options.addEditAction(ServerResponse.CHANGE_WYS, $prop.action); }
		|	'ON' 'SHORTCUT' (c=stringLiteral)? prop=formActionPropertyObject { $options.addContextMenuEditAction($c.val, $prop.action); }
		|	'EVENTID' id=stringLiteral { $options.setEventId($id.val); }
		)*
	;

formPropertyDraw returns [PropertyDrawEntity property]
	:	id=ID 				{ if (inPropParseState()) $property = $formStatement::form.getPropertyDraw($id.text); }
	|	prop=mappedProperty { if (inPropParseState()) $property = $formStatement::form.getPropertyDraw($prop.name, $prop.mapping); }
	;

formMappedPropertiesList returns [List<String> aliases, List<String> properties, List<List<String>> mapping, List<FormPropertyOptions> options]
@init {
	$aliases = new ArrayList<String>();
	$properties = new ArrayList<String>();
	$mapping = new ArrayList<List<String>>();
	$options = new ArrayList<FormPropertyOptions>();
	String alias = null;
}
	:	{ alias = null; }
		(id=ID '=' { alias = $id.text; })?
		mappedProp=formMappedProperty opts=formPropertyOptionsList
		{
			$aliases.add(alias);
			$properties.add($mappedProp.name);
			$mapping.add($mappedProp.mapping);
			$options.add($opts.options);
		}
		(','
			{ alias = null; }
			(id=ID '=' { alias = $id.text; })?
			mappedProp=formMappedProperty opts=formPropertyOptionsList
			{
				$aliases.add(alias);
				$properties.add($mappedProp.name);
				$mapping.add($mappedProp.mapping);
				$options.add($opts.options);
			}
		)*
	;

formCalcPropertyObject returns [CalcPropertyObjectEntity property = null]
	:	mProperty=mappedProperty
		{
			if (inPropParseState()) {
				$property = $formStatement::form.addCalcPropertyObject($mProperty.name, $mProperty.mapping);
			}
		}
	;

formActionPropertyObject returns [ActionPropertyObjectEntity action = null]
	:	mProperty=mappedProperty
		{
			if (inPropParseState()) {
				$action = $formStatement::form.addActionPropertyObject($mProperty.name, $mProperty.mapping);
			}
		}
	;

formGroupObjectEntity returns [GroupObjectEntity groupObject]
	:	id = ID { 
			if (inPropParseState()) {
				$groupObject = $formStatement::form.getGroupObjectEntity($ID.text);
			} 
		}
	;

formMappedProperty returns [String name, List<String> mapping]
	:	pname=formPropertyName { $name = $pname.name; }
		'('
			objects=idList { $mapping = $objects.ids; }
		')'
	;


formPropertiesNamesList returns [List<String> aliases, List<String> properties, List<FormPropertyOptions> options]
@init {
	$aliases = new ArrayList<String>();
	$properties = new ArrayList<String>();
	$options = new ArrayList<FormPropertyOptions>();
	String alias = null;
}
	:	{ alias = null; }
		(id=ID '=' { alias = $id.text; })?
		pname=formPropertyName opts=formPropertyOptionsList
		{
			$aliases.add(alias);
			$properties.add($pname.name);
			$options.add($opts.options);
		}
		(','
			{ alias = null; }
			(id=ID '=' { alias = $id.text; })?
			pname=formPropertyName opts=formPropertyOptionsList
			{
				$aliases.add(alias);
				$properties.add($pname.name);
				$options.add($opts.options);
			}
		)*
	;


formPropertyName returns [String name]
	:	id=compoundID	        { $name = $id.sid; }
	|	cid='OBJVALUE'	        { $name = $cid.text; }
	|	cid='SELECTION'	        { $name = $cid.text; }
	|	cid='ADDOBJ'	        { $name = $cid.text; }
	|	cid='ADDFORM'	        { $name = $cid.text; }
	|	cid='ADDSESSIONFORM'	{ $name = $cid.text; }
	|	cid='EDITFORM'	        { $name = $cid.text; }
	|	cid='EDITSESSIONFORM'	{ $name = $cid.text; }
	;


formFiltersList
@init {
	List<LP> properties = new ArrayList<LP>();
	List<List<String>> propertyMappings = new ArrayList<List<String>>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedFilters(properties, propertyMappings);
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
		$formStatement::form.addScriptedHints(hintNoUpdate, $list.ids);
	}
}
	:	(('HINTNOUPDATE') | ('HINTTABLE' { hintNoUpdate = false; })) 'LIST'
		list=nonEmptyCompoundIdList	
	;

formEventsList
@init {
	List<ActionPropertyObjectEntity> actions = new ArrayList<ActionPropertyObjectEntity>();
	List<FormEventType> types = new ArrayList<FormEventType>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedFormEvents(actions, types);
	}
}
	:	'EVENTS'
		decl=formEventDeclaration { actions.add($decl.action); types.add($decl.type); }
		(',' decl=formEventDeclaration { actions.add($decl.action); types.add($decl.type); })*
	;


formEventDeclaration returns [ActionPropertyObjectEntity action, FormEventType type]
	:	'ON' 
		(	'OK' 	 { $type = FormEventType.OK; }
		|	'APPLY'	 { $type = FormEventType.APPLY; }	
		|	'CLOSE'	 { $type = FormEventType.CLOSE; }
		|	'INIT'	 { $type = FormEventType.INIT; }
		|	'CANCEL' { $type = FormEventType.CANCEL; }
		|	'DROP'	 { $type = FormEventType.DROP; }
		)
		faprop=formActionPropertyObject { $action = $faprop.action; }
	;


filterGroupDeclaration
@init {
	String filterGroupSID = null;
	List<String> captions = new ArrayList<String>();
	List<String> keystrokes = new ArrayList<String>();
	List<LP> properties = new ArrayList<LP>();
	List<List<String>> mappings = new ArrayList<List<String>>();
	List<Boolean> defaults = new ArrayList<Boolean>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedRegularFilterGroup(filterGroupSID, captions, keystrokes, properties, mappings, defaults);
	}
}
	:	'FILTERGROUP' sid=ID { filterGroupSID = $sid.text; }
		(
			'FILTER' caption=stringLiteral keystroke=stringLiteral filter=formFilterDeclaration setDefault=filterSetDefault
			{
				captions.add($caption.val);
				keystrokes.add($keystroke.val);
				properties.add($filter.property);
				mappings.add($filter.mapping);
				defaults.add($setDefault.isDefault);
			}
		)+
	;

	
formFilterDeclaration returns [LP property, List<String> mapping] 
@init {
	List<String> context = null;
	if (inPropParseState()) {
		context = $formStatement::form.getObjectsNames();
	}
}
@after {
	if (inPropParseState()) {
		$mapping = $formStatement::form.getUsedObjectNames(context, $expr.property.usedParams);
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
		$formStatement::form.addScriptedDefaultOrder(properties, orders);
	}
}
	:	'ORDER' 'BY' orderedProp=formPropertyDrawWithOrder { properties.add($orderedProp.property); orders.add($orderedProp.order); }
		(',' orderedProp=formPropertyDrawWithOrder { properties.add($orderedProp.property); orders.add($orderedProp.order); } )*
	;
	
formPropertyDrawWithOrder returns [PropertyDrawEntity property, boolean order = true]
	:	pDraw=formPropertyDraw { $property = $pDraw.property; } ('ASC' | 'DESC' { $order = false; })?
	;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// PROPERTY STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

propertyStatement
@init {
	LP property = null;
	List<String> context = new ArrayList<String>();
	boolean dynamic = true;
	int lineNumber = self.getParser().getCurrentParserLineNumber(); 
}
@after {
	if (inPropParseState()) {
		self.setPropertyScriptInfo(property, $text, lineNumber);
	}
}
	:	declaration=propertyDeclaration { if ($declaration.paramNames != null) { context = $declaration.paramNames; dynamic = false; } }
		'=' 
		(	def=expressionUnfriendlyPD[context, dynamic, false] { property = $def.property; }
		|	expr=propertyExpression[context, dynamic] { if (inPropParseState()) { self.checkNecessaryProperty($expr.property); property = $expr.property.property; } }
		)
		propertyOptions[property, $declaration.name, $declaration.caption, context]
		( {!self.semicolonNeeded()}?=>  | ';')
	;


propertyDeclaration returns [String name, String caption, List<String> paramNames]
	:	propNameCaption=simpleNameWithCaption { $name = $propNameCaption.name; $caption = $propNameCaption.caption; }
		('(' paramList=idList ')' { $paramNames = $paramList.ids; })? 
	;


propertyExpression[List<String> context, boolean dynamic] returns [LPWithParams property]
	:	pe=orPE[context, dynamic] { $property = $pe.property; }
	;

orPE[List<String> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedOrProp(props);
	}
} 
	:	firstExpr=andPE[context, dynamic] { props.add($firstExpr.property); }
		('OR' nextExpr=andPE[context, dynamic] { props.add($nextExpr.property); })*
	;

andPE[List<String> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
	List<Boolean> nots = new ArrayList<Boolean>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedAndProp(nots, props);				
	}
}
	:	firstExpr=equalityPE[context, dynamic] { props.add($firstExpr.property); }
		((('AND') | ('IF')) { nots.add(false); }
		('NOT' { nots.set(nots.size()-1, true); })?
		nextExpr=equalityPE[context, dynamic] { props.add($nextExpr.property); })*
	;

equalityPE[List<String> context, boolean dynamic] returns [LPWithParams property]
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


relationalPE[List<String> context, boolean dynamic] returns [LPWithParams property]
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
			(   operand=REL_OPERAND { op = $operand.text; }
			    rhs=additiveORPE[context, dynamic] { rightProp = $rhs.property; }
			)
		|	def=typePropertyDefinition { mainProp = $def.property; }
		)?
	;


additiveORPE[List<String> context, boolean dynamic] returns [LPWithParams property]
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
	
	
additivePE[List<String> context, boolean dynamic] returns [LPWithParams property]
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
		
	
multiplicativePE[List<String> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
	List<String> ops = new ArrayList<String>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedMultiplicativeProp(ops, props);				
	}
}
	:	firstExpr=prefixUnaryPE[context, dynamic] { props.add($firstExpr.property); }
		(operand=MULT_OPERAND { ops.add($operand.text); }
		nextExpr=prefixUnaryPE[context, dynamic] { props.add($nextExpr.property); })*
	;

	
prefixUnaryPE[List<String> context, boolean dynamic] returns [LPWithParams property] 
@init {
	boolean hasPrefix = false;
}
@after {
	if (inPropParseState() && hasPrefix) {
		$property = self.addScriptedUnaryMinusProp($expr.property);
	} 
}
	:	MINUS expr=prefixUnaryPE[context, dynamic]  { hasPrefix = true; }
	|	simpleExpr=postfixUnaryPE[context, dynamic] { $property = $simpleExpr.property; }
	;

		 
postfixUnaryPE[List<String> context, boolean dynamic] returns [LPWithParams property] 
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

		 
simplePE[List<String> context, boolean dynamic] returns [LPWithParams property]
	:	'(' expr=propertyExpression[context, dynamic] ')' { $property = $expr.property; } 
	|	primitive=expressionPrimitive[context, dynamic] { $property = $primitive.property; } 
	;

	
expressionPrimitive[List<String> context, boolean dynamic] returns [LPWithParams property]
	:	param=singleParameter[context, dynamic] { $property = $param.property; }
	|	expr=expressionFriendlyPD[context, dynamic] { $property = $expr.property; }
	;

singleParameter[List<String> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = new LPWithParams(null, Collections.singletonList(self.getParamIndex($paramName.text, $context, $dynamic, insideRecursion)));
	}
}
	:	paramName=parameter
	;
	
expressionFriendlyPD[List<String> context, boolean dynamic] returns [LPWithParams property]
	:	joinDef=joinPropertyDefinition[context, dynamic] { $property = $joinDef.property; } 
	|	unionDef=unionPropertyDefinition[context, dynamic] { $property = $unionDef.property;} 
	|	ifElseDef=ifElsePropertyDefinition[context, dynamic] { $property = $ifElseDef.property; }
	|	caseDef=casePropertyDefinition[context, dynamic] { $property = $caseDef.property; }
	|	partDef=partitionPropertyDefinition[context, dynamic] { $property = $partDef.property; }
	|	recDef=recursivePropertyDefinition[context, dynamic] { $property = $recDef.property; } 
	|	concatDef=structCreationPropertyDefinition[context, dynamic] { $property = $concatDef.property; }
	|	sessionDef=sessionPropertyDefinition[context, dynamic] { $property = $sessionDef.property; }
	|	constDef=literal { $property = new LPWithParams($constDef.property, new ArrayList<Integer>()); }
	;

expressionUnfriendlyPD[List<String> context, boolean dynamic, boolean innerPD] returns [LP property]
	:	ciPD=contextIndependentPD[innerPD] { $property = $ciPD.property; }
	|	actPD=actionPropertyDefinition[context, dynamic] { if (inPropParseState()) $property = $actPD.property.property; }	
	;

contextIndependentPD[boolean innerPD] returns [LP property]
	: 	dataDef=dataPropertyDefinition[innerPD] { $property = $dataDef.property; }
	|	abstractDef=abstractPropertyDefinition { $property = $abstractDef.property; }
	|	abstractActionDef=abstractActionPropertyDefinition { $property = $abstractActionDef.property; }
	|	formulaProp=formulaPropertyDefinition { $property = $formulaProp.property; }
	|	groupDef=groupPropertyDefinition { $property = $groupDef.property; }
	;

joinPropertyDefinition[List<String> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedJProp($mainPropObj.property, $exprList.props);
	}
}
	:	('JOIN')? mainPropObj=propertyObject
		'('
		exprList=propertyExpressionList[context, dynamic]
		')'
	;


groupPropertyDefinition returns [LP property]
@init {
	List<LPWithParams> orderProps = new ArrayList<LPWithParams>();
	List<LPWithParams> groupProps = new ArrayList<LPWithParams>();
	List<String> groupContext = new ArrayList<String>();
	boolean ascending = true;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedGProp($type.type, $mainList.props, groupProps, orderProps, ascending, $whereExpr.property);
	}
}
	:	'GROUP'
		type=groupingType
		mainList=nonEmptyPropertyExpressionList[groupContext, true]
		('BY'
		exprList=nonEmptyPropertyExpressionList[groupContext, true] { groupProps.addAll($exprList.props); })?
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
	|	'EQUAL'	{ $type = GroupingType.EQUAL; }	
	;


partitionPropertyDefinition[List<String> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> paramProps = new ArrayList<LPWithParams>();
	LP ungroupProp = null;
	PartitionType type = null;
	int groupExprCnt;
	boolean strict = false;
	int precision = 0;
	boolean ascending = true;
	boolean useLast = true;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedPartitionProp(type, ungroupProp, strict, precision, ascending, useLast, groupExprCnt, paramProps);
	}
}
	:	'PARTITION' 
		(
			(	'SUM'	{ type = PartitionType.SUM; } 
			|	'PREV'	{ type = PartitionType.PREVIOUS; }
			)
		|	'UNGROUP'
			ungroup=propertyObject { ungroupProp = $ungroup.property; }
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


dataPropertyDefinition[boolean innerPD] returns [LP property]
@init {
	boolean sessionProp = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedDProp($returnClass.sid, $paramClassNames.ids, sessionProp, innerPD);
	}
}
	:	'DATA'
		('SESSION' { sessionProp = true; } )?
		returnClass=classId
		'('
			paramClassNames=classIdList
		')'
	;


abstractPropertyDefinition returns [LP property]
@init {
	boolean isExclusive = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedAbstractProp($returnClass.sid, $paramClassNames.ids, isExclusive);	
	}
}
	:	'ABSTRACT'
		returnClass=classId
		'('
			paramClassNames=classIdList
		')'
		('EXCLUSIVE' { isExclusive = true; })?
	;

abstractActionPropertyDefinition returns [LP property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedAbstractActionProp($params.ids.size());	
	}
}
	:	'ABSTRACT' 'ACTION' 
		'(' 
			params=idList
		')'	
	;
	

unionPropertyDefinition[List<String> context, boolean dynamic] returns [LPWithParams property]
@init {
	Union type = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedUProp(type, $exprList.props, "UNION");
	}
}
	:	'UNION'
		(('MAX' {type = Union.MAX;}) | ('SUM' {type = Union.SUM;}) | ('OVERRIDE' {type = Union.OVERRIDE;}) | ('XOR' { type = Union.XOR;}) | ('EXCLUSIVE' {type = Union.EXCLUSIVE;}) | ('CLASS' {type = Union.CLASS;}))
		exprList=nonEmptyPropertyExpressionList[context, dynamic]
	;


ifElsePropertyDefinition[List<String> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedIfElseUProp($ifExpr.property, $thenExpr.property, $elseExpr.property);
	}
}
	:	'IF' ifExpr=propertyExpression[context, dynamic]
		'THEN' thenExpr=propertyExpression[context, dynamic]
		'ELSE' elseExpr=propertyExpression[context, dynamic]
	;


casePropertyDefinition[List<String> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> whenProps = new ArrayList<LPWithParams>();
	List<LPWithParams> thenProps = new ArrayList<LPWithParams>();
	LPWithParams defaultProp = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedCaseUProp(whenProps, thenProps, defaultProp);
	}
}
	:	'CASE'
			( branch=caseBranchBody[context, dynamic] { whenProps.add($branch.whenProperty); thenProps.add($branch.thenProperty); } )+
			'DEFAULT' defaultExpr=propertyExpression[context, dynamic] { defaultProp = $defaultExpr.property; }
		'END'
	;
	
	
caseBranchBody[List<String> context, boolean dynamic] returns [LPWithParams whenProperty, LPWithParams thenProperty]
	:	'WHEN' whenExpr=propertyExpression[context, dynamic] { $whenProperty = $whenExpr.property; }
		'THEN' thenExpr=propertyExpression[context, dynamic] { $thenProperty = $thenExpr.property; }
	;

recursivePropertyDefinition[List<String> context, boolean dynamic] returns [LPWithParams property]
@init {
	Cycle cycleType = Cycle.NO;
	List<String> recursiveContext = null;
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
		  	recursiveContext = new ArrayList<String>(context);
		}
		nextStep=propertyExpression[recursiveContext, dynamic]
		('CYCLES' 
			(	'YES' { cycleType = Cycle.YES; }
			|	'NO' { cycleType = Cycle.NO; } 
			|	'IMPOSSIBLE' { cycleType = Cycle.IMPOSSIBLE; }
			)
		)?
	;

structCreationPropertyDefinition[List<String> context, boolean dynamic] returns [LPWithParams property] 
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

sessionPropertyDefinition[List<String> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedSpecialProp($name.text, $expr.property);
	}
}
	:	name=specialPropertyName 
		'(' 
			expr=propertyExpression[context, dynamic] 
		')'
	;

specialPropertyName
	:	('PREV') 
	| 	('CHANGED')
	|   ('SETCHANGED')
	|   ('CHANGEDSET')
	| 	('ASSIGNED')
	| 	('DROPPED')
    | 	('CLASS')
	;


formulaPropertyDefinition returns [LP property]
@init {
	String className = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedSFProp(className, $formulaText.val);
	}
}
	:	'FORMULA' 
		(clsName=classId { className = $clsName.sid; })? 
		formulaText=stringLiteral
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


propertyObject returns [LP property]
@init {
	List<String> newContext = new ArrayList<String>(); 
}
	:	name=compoundID
		{
			if (inPropParseState()) {
				$property = self.findLPByCompoundName($name.sid);
			}
		}
	|	'['	(	expr=propertyExpression[newContext, true] { if (inPropParseState()) { self.checkNecessaryProperty($expr.property); $property = $expr.property.property; } }
			|	def=expressionUnfriendlyPD[newContext, true, true] { $property = $def.property; }
			)
		']'
	;


propertyOptions[LP property, String propertyName, String caption, List<String> namedParams]
@init {
	String groupName = null;
	String table = null;
	boolean isPersistent = false;
	Boolean isLoggable = null;
	Boolean notNullResolve = null;
	Event notNullEvent = null;
}
@after {
	if (inPropParseState()) {
		self.addSettingsToProperty(property, propertyName, caption, namedParams, groupName, isPersistent, table, notNullResolve, notNullEvent);
		self.makeLoggable(property, isLoggable);
	}
}
	: 	(	'IN' name=compoundID { groupName = $name.sid; }
		|	'PERSISTENT' { isPersistent = true; }
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
		|	indexSetting [propertyName]
		|	aggPropSetting [property]
		|	s=notNullSetting { notNullResolve = $s.toResolve; notNullEvent = $s.event; }
		|	onEditEventSetting [property, namedParams]
		|	eventIdSetting [property]
		)*
	;


shortcutSetting [LP property, String caption]
@init {
	String sid = null;
}
@after {
	if (inPropParseState()) {
		self.addToContextMenuFor($property, caption, sid);
	}
}
	:	'SHORTCUT' name = compoundID { sid = $name.sid; }
	;

asEditActionSetting [LP property]
@init {
	String mainPropertySID = null;
	String editActionSID = null;
}
@after {
	if (inPropParseState()) {
		self.setAsEditActionFor($property, editActionSID, mainPropertySID);
	}
}
	:	(   'ASONCHANGE' { editActionSID = ServerResponse.CHANGE; }
	    |   'ASONCHANGEWYS' { editActionSID = ServerResponse.CHANGE_WYS; }
        |   'ASONEDIT' { editActionSID = ServerResponse.EDIT_OBJECT; }
        )
        name = compoundID { mainPropertySID = $name.sid; }
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

indexSetting [String propName]
@after {
	if (inIndexParseState()) {
		self.addScriptedIndex(Arrays.asList(propName));
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

notNullSetting returns [boolean toResolve = false, Event event]
	:	'NOT' 'NULL' ('DELETE' { $toResolve = true; })? et=baseEvent { $event = $et.event; }
	;

onEditEventSetting [LP property, List<String> context]
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
		action=actionPropertyDefinitionBody[context, false]
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

actionPropertyDefinition[List<String> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<String> localContext = context;
	boolean localDynamic = dynamic;
	boolean ownContext = false;
}
@after {
	if (inPropParseState()) {
		self.checkActionAllParamsUsed(localContext, $property.property, ownContext);
	}
}
	:	'ACTION'
		( '(' list=idList ')' 
			{ 
				localContext = $list.ids; localDynamic = false; ownContext = true; 
				
				if (inPropParseState() && !dynamic)	{
					self.checkActionLocalContext(context, localContext);
				}
			} 
		)?
		pdb=actionPropertyDefinitionBody[localContext, localDynamic] { if (inPropParseState()) $property = $pdb.property; }
	;

actionPropertyDefinitionBody[List<String> context, boolean dynamic] returns [LPWithParams property]
	:	extPDB=extendContextActionPDB[context, dynamic] { $property = $extPDB.property; }
	|	keepPDB=keepContextActionPDB[context, dynamic] 	{ $property = $keepPDB.property; }
	|	trivPDB=customActionPDB[context, dynamic] 		{ $property = $trivPDB.property; }
	;

extendContextActionPDB[List<String> context, boolean dynamic] returns [LPWithParams property]
@init { 
	if (inPropParseState() && dynamic) {
		self.getErrLog().emitExtendActionContextError(self.getParser());
	}
}
	:	setPDB=setActionPropertyDefinitionBody[context] { $property = $setPDB.property; }
	|	forPDB=forActionPropertyDefinitionBody[context] { $property = $forPDB.property; }
	|	classPDB=changeClassActionPropertyDefinitionBody[context] { $property = $classPDB.property; }
	|	addPDB=addObjectActionPropertyDefinitionBody[context] { $property = $addPDB.property; }
	;
	
keepContextActionPDB[List<String> context, boolean dynamic] returns [LPWithParams property]
	:	listPDB=listActionPropertyDefinitionBody[context, dynamic] { $property = $listPDB.property; }
	|	requestInputPDB=requestInputActionPropertyDefinitionBody[context, dynamic] { $property = $requestInputPDB.property; }
	|	execPDB=execActionPropertyDefinitionBody[context, dynamic] { $property = $execPDB.property; }	
	|	ifPDB=ifActionPropertyDefinitionBody[context, dynamic] { $property = $ifPDB.property; }
	|	termPDB=terminalFlowActionPropertyDefinitionBody { $property = $termPDB.property; }
	;
	
customActionPDB[List<String> context, boolean dynamic] returns [LPWithParams property]
@init {
	$property = new LPWithParams(null, new ArrayList<Integer>());
}
	:	formPDB=formActionPropertyDefinitionBody[context, dynamic] { $property = $formPDB.property; }
	|	addformPDB=addFormActionPropertyDefinitionBody { $property.property = $addformPDB.property; }
	|	editformPDB=editFormActionPropertyDefinitionBody { $property.property = $editformPDB.property; }
	|	actPDB=customActionPropertyDefinitionBody { $property.property = $actPDB.property; }
	|   msgPDB=messageActionPropertyDefinitionBody[context, dynamic] { $property = $msgPDB.property; }
	|   asyncPDB=asyncUpdateActionPropertyDefinitionBody[context, dynamic] { $property = $asyncPDB.property; }
	|   confirmPDB=confirmActionPropertyDefinitionBody[context, dynamic] { $property = $confirmPDB.property; }
	|   mailPDB=emailActionPropertyDefinitionBody[context, dynamic] { $property = $mailPDB.property; }
	|	filePDB=fileActionPropertyDefinitionBody[context, dynamic] { $property = $filePDB.property; }
	|	evalPDB=evalActionPropertyDefinitionBody[context, dynamic] { $property = $evalPDB.property; }
	;


formActionPropertyDefinitionBody[List<String> context, boolean dynamic] returns [LPWithParams property]
@init {
	FormSessionScope sessionScope = FormSessionScope.OLDSESSION;
	ModalityType modalityType = ModalityType.DOCKED;
	boolean checkOnOk = false;
	boolean showDrop = false;
	List<String> objects = new ArrayList<String>();
	List<LPWithParams> mapping = new ArrayList<LPWithParams>();
	}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedFAProp($formName.sid, objects, mapping, modalityType, sessionScope, checkOnOk, showDrop);
	}
}
	:	'FORM' formName=compoundID 
		('OBJECTS' list=formActionObjectList[context, dynamic] { objects = $list.objects; mapping = $list.exprs; })? 
		(sessScope = formSessionScopeLiteral { sessionScope = $sessScope.val; })?
		(modality = modalityTypeLiteral { modalityType = $modality.val; })?
		('CHECK' { checkOnOk = true; })?
		('SHOWDROP' { showDrop = true; })?
	;

formActionObjectList[List<String> context, boolean dynamic] returns [List<String> objects = new ArrayList<String>(), List<LPWithParams> exprs = new ArrayList<LPWithParams>()]
	:	objName=ID { $objects.add($objName.text); } '=' expr=propertyExpression[context, dynamic] { $exprs.add($expr.property); } 
		(',' objName=ID { $objects.add($objName.text); } '=' expr=propertyExpression[context, dynamic] { $exprs.add($expr.property); })*
	;
	
customActionPropertyDefinitionBody returns [LP property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedCustomActionProp($classN.val);	
	}
}
	:	'CUSTOM' classN = stringLiteral 
	;


addFormActionPropertyDefinitionBody returns [LP property]
@init {
	boolean session = false;	
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedAddFormAction($cls.sid, session);	
	}
}
	:	'ADDFORM' ('SESSION' { session = true; })? cls=classId
	;

editFormActionPropertyDefinitionBody returns [LP property]
@init {
	boolean session = false;	
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedEditFormAction($cls.sid, session);	
	}
}
	:	'EDITFORM' ('SESSION' { session = true; })? cls=classId
	;

addObjectActionPropertyDefinitionBody[List<String> context] returns [LPWithParams property]
@init {
	List<String> newContext = new ArrayList<String>(context);
	LPWithParams condition = null;
	String toPropName = null;
	List<LPWithParams> toPropMapping = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedAddObjProp(context, $cid.sid, toPropName, toPropMapping, condition);
	}
}
	:	'ADDOBJ' cid=classId
		('WHERE' pe=propertyExpression[newContext, true] { condition = $pe.property; })?
		('TO' propName=compoundID '(' params=singleParameterList[newContext, false] ')' { toPropName = $propName.sid; toPropMapping = $params.props; } )?
	;

emailActionPropertyDefinitionBody[List<String> context, boolean dynamic] returns [LPWithParams property]
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
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedEmailProp(fromProp, subjProp, recipTypes, recipProps, forms, formTypes, mapObjects, attachNames, attachFormats);
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
		)*
	;
	
emailActionFormObjects[List<String> context, boolean dynamic] returns [OrderedMap<String, LPWithParams> mapObjects]
@init {
	$mapObjects = new OrderedMap<String, LPWithParams>();
}

	:	(	'OBJECTS'
			obj=ID '=' objValueExpr=propertyExpression[context, dynamic] { $mapObjects.put($obj.text, $objValueExpr.property); }
			(',' obj=ID '=' objValueExpr=propertyExpression[context, dynamic] { $mapObjects.put($obj.text, $objValueExpr.property); })*
		)?
	;

confirmActionPropertyDefinitionBody[List<String> context, boolean dynamic] returns [LPWithParams property]
@init {
	int length = 2000;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedConfirmProp(length, $pe.property);
	}
}
	:	'CONFIRM' pe=propertyExpression[context, dynamic] ('LENGTH' len=uintLiteral { length = $len.val; } )? 
	;
		
messageActionPropertyDefinitionBody[List<String> context, boolean dynamic] returns [LPWithParams property]
@init {
	int length = 2000;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedMessageProp(length, $pe.property);
	}
}
	:	'MESSAGE' pe=propertyExpression[context, dynamic] ('LENGTH' len=uintLiteral { length = $len.val; } )?
	;

asyncUpdateActionPropertyDefinitionBody[List<String> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedAsyncUpdateProp($pe.property);
	}
}
	:	'ASYNCUPDATE' pe=propertyExpression[context, dynamic]
	;

fileActionPropertyDefinitionBody[List<String> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean loadFile = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedFileAProp(loadFile, $pe.property);
	}
}
	:	('LOADFILE' { loadFile = true; } | 'OPENFILE' { loadFile = false; }) 
		pe=propertyExpression[context, dynamic]	
	;

changeClassActionPropertyDefinitionBody[List<String> context] returns [LPWithParams property]
@init {
	List<String> newContext = new ArrayList<String>(context);
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

evalActionPropertyDefinitionBody[List<String> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedEvalActionProp($expr.property);
	}
}
	:	'EVAL' expr=propertyExpression[context, dynamic]
	;

requestInputActionPropertyDefinitionBody[List<String> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedRequestUserInputAProp($tid.sid, $objID.text, $PDB.property);
	}
}
	:	'REQUEST' tid=typeId
		(	'INPUT'
		|	(objID=ID)? PDB=actionPropertyDefinitionBody[context, dynamic]
		)
	;

listActionPropertyDefinitionBody[List<String> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
	List<String> localPropNames = new ArrayList<String>();
	boolean newSession = false;
	boolean doApply = false;
	boolean singleApply = false;
	
	Set<String> upLocalNames = null;
	if (inPropParseState())
		upLocalNames = self.copyCurrentLocalProperties();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedListAProp(newSession, doApply, singleApply, upLocalNames, null, props, localPropNames);
	}
}
	:	('NEWSESSION' { newSession = true; } ('AUTOAPPLY' {doApply = true; } )?
					('SINGLE' { singleApply = true; })? )?
		'{'
			(	(PDB=actionPropertyDefinitionBody[context, dynamic] { props.add($PDB.property); }
				( {!self.semicolonNeeded()}?=>  | ';'))
			|	def=localDataPropertyDefinition ';' { localPropNames.add($def.name); }
			|	emptyStatement
			)*
		'}'
	;
	
localDataPropertyDefinition returns [String name]
@after {
	$name = $propName.text;
	if (inPropParseState()) {
		self.addLocalDataProperty($propName.text, $returnClass.sid, $paramClasses.ids);
	}
}
	:	'LOCAL' propName=ID 
		'=' returnClass=classId
		'('
			paramClasses=classIdList
		')'
	;

execActionPropertyDefinitionBody[List<String> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedJoinAProp($prop.property, $exprList.props);
	}
}
	:	('EXEC')?
		prop=propertyObject
		{ if (inPropParseState()) self.checkActionProperty($prop.property); }
		'('
		exprList=propertyExpressionList[context, dynamic]
		')'
	;

setActionPropertyDefinitionBody[List<String> context] returns [LPWithParams property]
@init {
	List<String> newContext = new ArrayList<String>(context); 
	LPWithParams condition = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedSetPropertyAProp(context, $propName.sid, $params.props, $expr.property, condition);
	}
}
	:	('SET')?
		propName=compoundID
		'(' params=singleParameterList[newContext, true] ')'
		'<-'
		expr=propertyExpression[newContext, false] //no need to use dynamic context, because params should be either on global context or used in the left side
		('WHERE'
		whereExpr=propertyExpression[newContext, false] { condition = $whereExpr.property; })?
	;

ifActionPropertyDefinitionBody[List<String> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedIfAProp($expr.property, $thenPDB.property, $elsePDB.property);
	}
}
	:	'IF' expr=propertyExpression[context, dynamic] 
		'THEN' thenPDB=actionPropertyDefinitionBody[context, dynamic]
		('ELSE' elsePDB=actionPropertyDefinitionBody[context, dynamic])?
	;

forAddObjClause[List<String> context] returns [Integer paramCnt, String className]
@init {
	String varName = "added";
}
@after {
	if (inPropParseState()) {
		$paramCnt = self.getParamIndex(varName, context, true, insideRecursion);
	}
}
	:	'ADDOBJ'
		(varID=ID '=' {varName = $varID.text;})?
		addClass=classId { $className = $addClass.sid; }
	;

forActionPropertyDefinitionBody[List<String> context] returns [LPWithParams property]
@init {
	boolean recursive = false;
	boolean descending = false;
	List<String> newContext = new ArrayList<String>(context);
	List<LPWithParams> orders = new ArrayList<LPWithParams>();
	Inline inline = null;
	
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedForAProp(context, $expr.property, orders, $actPDB.property, $elsePDB.property, $addObj.paramCnt, $addObj.className, recursive, descending, inline);
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
		(addObj=forAddObjClause[newContext])?
		('INLINE' { inline = Inline.FORCE; } | 'NOINLINE' { inline = Inline.NO; } )?
		'DO' actPDB=actionPropertyDefinitionBody[newContext, false]
		( {!recursive}?=> 'ELSE' elsePDB=actionPropertyDefinitionBody[context, false])?
	;

terminalFlowActionPropertyDefinitionBody returns [LPWithParams property]
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
	List<String> context = new ArrayList<String>();
	boolean dynamic = true;
	LPWithParams property = null;
	LPWithParams when = null;
}
@after {
	if (inPropParseState()) {
		self.addImplementationToAbstract($propName.sid, $list.ids, property, when);
	}
}
	:	propName=compoundID
		'(' list=idList ')' { context = $list.ids; dynamic = false; }
		'+='
		(	expr=propertyExpression[context, dynamic] { property = $expr.property; }
		|	action=actionPropertyDefinition[context, dynamic] { property = $action.property; }
		)
		('WHEN' where=propertyExpression[context, dynamic] {when = $where.property; } ) ?
		( {!self.semicolonNeeded()}?=>  | ';')
	;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// CONSTRAINT STATEMENT //////////////////////////
////////////////////////////////////////////////////////////////////////////////

constraintStatement 
@init {
	boolean checked = false;
	List<String> propNames = null;
}
@after {
	if (inPropParseState()) {
		self.addScriptedConstraint($expr.property.property, $et.event, checked, propNames, $message.val);
	}
}
	:	'CONSTRAINT'
	    et=baseEvent
		expr=propertyExpression[new ArrayList<String>(), true] { if (inPropParseState()) self.checkNecessaryProperty($expr.property); }
		('CHECKED' { checked = true; } 
			('BY' list=nonEmptyCompoundIdList { propNames = $list.ids; })? 
		)?
		'MESSAGE' message=stringLiteral
		';'
	;


////////////////////////////////////////////////////////////////////////////////
///////////////////////////////// FOLLOWS STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

followsStatement
@init {
	List<String> context;
	String mainProp;
	List<LPWithParams> props = new ArrayList<LPWithParams>();
	List<Integer> options = new ArrayList<Integer>();
	List<Event> events = new ArrayList<Event>();
	Event event = Event.APPLY;
}
@after {
	if (inPropParseState()) {
		self.addScriptedFollows(mainProp, context, options, props, events);
	}
}
	:	prop=mappedProperty { mainProp = $prop.name; context = $prop.mapping; }
		'=>'
		firstExpr=propertyExpression[context, false] ('RESOLVE' type=followsResolveType et=baseEvent { event = $et.event; })?
		{
			props.add($firstExpr.property); 
			options.add(type == null ? PropertyFollows.RESOLVE_ALL : $type.type);
			events.add(event);
		}
		(','
			{	event = Event.APPLY;	}
			nextExpr=propertyExpression[context, false] ('RESOLVE' type=followsResolveType et=baseEvent { event = $et.event; })?
			{
		     	props.add($nextExpr.property); 
		     	options.add(type == null ? PropertyFollows.RESOLVE_ALL : $type.type);
				events.add(event);
			}
		)*
		';'
	;

followsResolveType returns [Integer type]
	:	lit=LOGICAL_LITERAL	{ $type = $lit.text.equals("TRUE") ? PropertyFollows.RESOLVE_TRUE : PropertyFollows.RESOLVE_FALSE; }
	|	'ALL'			{ $type = PropertyFollows.RESOLVE_ALL; }
	|	'NOTHING'		{ $type = PropertyFollows.RESOLVE_NOTHING; }
	;


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// WRITE STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

writeWhenStatement
@after {
	if (inPropParseState()) {
		self.addScriptedWriteWhen($mainProp.name, $mainProp.mapping, $valueExpr.property, $whenExpr.property);
	}
}
	:	mainProp=mappedProperty 
		'<-'
		valueExpr=propertyExpression[$mainProp.mapping, false] 
		'WHEN'
		whenExpr=propertyExpression[$mainProp.mapping, false]
		';'
	;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// EVENT STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

eventStatement
@init {
	List<String> context = new ArrayList<String>();
	List<LPWithParams> orderProps = new ArrayList<LPWithParams>();
	boolean descending = false;
}
@after {
	if (inPropParseState()) {
		self.addScriptedEvent($whenExpr.property, $action.property, orderProps, descending, $et.event);
	} 
}
	:	'WHEN'
		et=baseEvent
		whenExpr=propertyExpression[context, true]
		'DO'
		action=actionPropertyDefinitionBody[context, false]
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
		self.addScriptedGlobalEvent($action.property, $et.event, single, $property.text, $prevStart.ids);
	}
}
	:	'ON' 
		et=baseEvent
		('SINGLE' { single = true; })?
		('PREVSTART' prevStart=nonEmptyIdList)?
		('SHOWDEP' property=ID)?
		action=actionPropertyDefinitionBody[new ArrayList<String>(), false]
		( {!self.semicolonNeeded()}?=>  | ';')
	;

baseEvent returns [Event event]
@init {
	SystemEvent baseEvent = SystemEvent.APPLY;
	List<String> ids = null;
}
@after {
	if (inPropParseState()) {
		$event = self.createScriptedEvent(baseEvent, ids);
	}
}
	:	('APPLY' { baseEvent = SystemEvent.APPLY; } | 'SESSION'	{ baseEvent = SystemEvent.SESSION; })?
	    ('FORMS' (neIdList=nonEmptyCompoundIdList { ids = $neIdList.ids; }) )?
	;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// SHOWDEP STATEMENT //////////////////////////////
////////////////////////////////////////////////////////////////////////////////

showDepStatement
@after {
    if (inPropParseState()) {
        self.addScriptedShowDep($property.text, $propFrom.text);
    }
}
    :	'SHOWDEP'
        property=ID
        'FROM'
        propFrom=ID
        ';'
    ;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// ASPECT STATEMENT //////////////////////////////
////////////////////////////////////////////////////////////////////////////////

aspectStatement
@init {
	List<String> context = new ArrayList<String>();
	boolean before = true;
}
@after {
	if (inPropParseState()) {
		self.addScriptedAspect($mainProp.name, $mainProp.mapping, $action.property, before);
	}
}
	:	(	'BEFORE' 
		| 	'AFTER' { before = false; }
		)
		mainProp=mappedProperty 'DO' action=actionPropertyDefinitionBody[$mainProp.mapping, false]
		( {!self.semicolonNeeded()}?=>  | ';')
	;


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// TABLE STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

tableStatement 
@after {
	if (inTableParseState()) {
		self.addScriptedTable($name.text, $list.ids);
	}
}
	:	'TABLE' name=ID '(' list=nonEmptyClassIdList ')' ';';

////////////////////////////////////////////////////////////////////////////////
/////////////////////////////// LOGGABLE STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

loggableStatement
@after {
	if (inPropParseState()) {
		self.addScriptedLoggable($list.ids);
	}	
}
	:	'LOGGABLE' list=nonEmptyCompoundIdList ';'
	;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// INDEX STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

indexStatement
@after {
	if (inIndexParseState()) {
		self.addScriptedIndex($list.ids);
	}	
}
	:	'INDEX' list=nonEmptyCompoundIdList ';'
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
		self.addScriptedWindow($type.type, $id.text, $caption.val, $opts.options);
	}
}
	:	'WINDOW' type=windowType id=ID caption=stringLiteral opts=windowOptions ';'
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
		|	'HALIGN' '(' ha=horizontalAlignment ')' { $options.setHAlign($ha.val); }
		|	'VALIGN' '(' va=verticalAlignment ')' { $options.setVAlign($va.val); }
		|	'TEXTHALIGN' '(' tha=horizontalAlignment ')' { $options.setTextHAlign($tha.val); }
		|	'TEXTVALIGN' '(' tva=verticalAlignment ')' { $options.setTextVAlign($tva.val); }
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

verticalAlignment returns [VAlign val]
	:	'TOP'		{ $val = VAlign.TOP; }
	|	'CENTER'	{ $val = VAlign.CENTER; }
	|	'BOTTOM'	{ $val = VAlign.BOTTOM; }
	;

horizontalAlignment returns [HAlign val]
	:	'LEFT'		{ $val = HAlign.LEFT; }
	|	'CENTER'	{ $val = HAlign.CENTER; }
	|	'RIGHT'		{ $val = HAlign.RIGHT; }
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
			|	setupNavigatorElementStatement
			|	emptyStatement
			)*
		'}'
	| emptyStatement
	;

addNavigatorElementStatement[NavigatorElement parentElement]
	:	'ADD' elem=navigatorElementSelector (caption=stringLiteral)? posSelector=navigatorElementInsertPositionSelector[parentElement] ('TO' wid=compoundID)?
		{
			if (inPropParseState()) {
				self.setupNavigatorElement($elem.element, $caption.val, $posSelector.parent, $posSelector.position, $posSelector.anchor, $wid.sid);
			}
		}
		navigatorElementStatementBody[$elem.element]
	;

newNavigatorElementStatement[NavigatorElement parentElement]
@init {
	NavigatorElement newElement = null;
}
	:	'NEW' id=ID ('ACTION' aid=compoundID)? caption=stringLiteral posSelector=navigatorElementInsertPositionSelector[parentElement] ('TO' wid=compoundID)? ('IMAGE' path=stringLiteral)?
		{
			if (inPropParseState()) {
				newElement = self.createScriptedNavigatorElement($id.text, $caption.val, $posSelector.parent, $posSelector.position, $posSelector.anchor, $wid.sid, $aid.sid, $path.val);
			}
		}
		navigatorElementStatementBody[newElement]
	;
	
navigatorElementInsertPositionSelector[NavigatorElement parentElement] returns [NavigatorElement parent, InsertPosition position, NavigatorElement anchor]
@init {
	$parent = parentElement;
	$position = InsertPosition.IN;
	$anchor = null;
}
	:	(	'IN' { $position = InsertPosition.IN; }
			elem=navigatorElementSelector { $parent = $elem.element; }
		)?
		(
			(pos=insertRelativePositionLiteral { $position = $pos.val; }
			elem=navigatorElementSelector { $anchor = $elem.element; })
		|	'FIRST' { $position = InsertPosition.FIRST; }
		)?
	;

setupNavigatorElementStatement
	:	elem=navigatorElementSelector (caption=stringLiteral)? ('TO' wid=compoundID)?
		{
			if (inPropParseState()) {
				self.setupNavigatorElement($elem.element, $caption.val, null, null, null, $wid.sid);
			}
		}
		navigatorElementStatementBody[$elem.element]
	;
	
navigatorElementSelector returns [NavigatorElement element]
	:	cid=compoundID
		{
			if (inPropParseState()) {
				$element = self.findNavigatorElementByName($cid.sid);
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
	:	(	decl=designDeclaration 			{ $designStatement::design = formView = $decl.view; }
		|	edecl=extendDesignDeclaration 	{ $designStatement::design = formView = $edecl.view; }	
		)
		componentStatementBody[formView == null ? null : formView.getView(), formView == null ? null : formView.getMainContainer()]
	;

designDeclaration returns [ScriptingFormView view]
@init {
	boolean applyDefault = false;
}
@after {
	if (inPropParseState()) {
		$view = self.createScriptedFormView($cid.sid, $caption.val, applyDefault);
	}
}
	:	'DESIGN' cid=compoundID (caption=stringLiteral)? ('FROM' 'DEFAULT' { applyDefault = true; })?
	;

extendDesignDeclaration returns [ScriptingFormView view]
@after {
	if (inPropParseState()) {
		$view = self.getDesignForExtending($cid.sid);
	}
}
	:	'EXTEND' 'DESIGN' cid=compoundID 
	;	

componentStatementBody [Object propertyReceiver, ComponentView parentComponent]
	:	'{'
			(	setObjectPropertyStatement[propertyReceiver]
			|	positionComponentsStatement[parentComponent]
			|	setupComponentStatement
			|	setupGroupObjectStatement
			|	newComponentStatement[parentComponent]
			|	addComponentStatement[parentComponent]
			|	removeComponentStatement
			|	emptyStatement
			)*
		'}'
	|	emptyStatement
	;

setupComponentStatement
	:	comp=componentSelector componentStatementBody[$comp.component, $comp.component]
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
					groupObject = $designStatement::design.getGroupObject($ID.text);
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
	:	'NEW' cid=multiCompoundID insPosition=componentInsertPositionSelector[parentComponent]
		{
			if (inPropParseState()) {
				newComp = $designStatement::design.createNewComponent($cid.sid, insPosition.parent, insPosition.position, insPosition.anchor);
			}
		}
		componentStatementBody[newComp, newComp]
	;
	
addComponentStatement[ComponentView parentComponent]
@init {
	ComponentView insComp = null;
}
	:	'ADD' insSelector=componentSelector { insComp = $insSelector.component; } insPosition=componentInsertPositionSelector[parentComponent]
		{
			if (inPropParseState()) {
				$designStatement::design.moveComponent(insComp, insPosition.parent, insPosition.position, insPosition.anchor);
			}
		}
		componentStatementBody[insComp, insComp]
	;
	
componentInsertPositionSelector[ComponentView parentComponent] returns [ComponentView parent, InsertPosition position, ComponentView anchor]
@init {
	$parent = parentComponent;
	$position = InsertPosition.IN;
	$anchor = null;
}
	:	(	'IN' { $position = InsertPosition.IN; }
			comp=componentSelector { $parent = $comp.component; }
		)?
		(
			(pos=insertRelativePositionLiteral { $position = $pos.val; }
			comp=componentSelector { $anchor = $comp.component; })
		|	'FIRST' { $position = InsertPosition.FIRST; }
		)?
	;

removeComponentStatement
@init {
	boolean cascade = false;
}
	:	'REMOVE' compSelector=componentSelector ('CASCADE' { cascade = true; } )? ';'
		{
			if (inPropParseState()) {
				$designStatement::design.removeComponent($compSelector.component, cascade);
			}
		}
	;

componentSelector returns [ComponentView component]
	:	'PARENT' '(' child=componentSelector ')'
		{
			if (inPropParseState()) {
				$designStatement::design.getParentContainer($child.component);
			}
		}
	|	'PROPERTY' '(' prop=propertySelector ')' { $component = $prop.propertyView; }
	|	mid=multiCompoundID
		{
			if (inPropParseState()) {
				$component = $designStatement::design.getComponentBySID($mid.sid);
			}
		}
	;


propertySelector returns [PropertyDrawView propertyView = null]
	:	pname=compoundID
		{
			if (inPropParseState()) {
				$propertyView = $designStatement::design.getPropertyView($pname.sid);
			}
		}
	|	mappedProp=mappedProperty
		{
			if (inPropParseState()) {
				$propertyView = $designStatement::design.getPropertyView($mappedProp.name, $mappedProp.mapping);
			}
		}
	;

positionComponentsStatement[ComponentView parentComponent]
@init {
	boolean hasSecondComponent = false;
}
	:	'POSITION' compSelector1=componentSelector constraint=simplexConstraintLiteral ( compSelector2=componentSelector  { hasSecondComponent = true; } )? ';'
		{
			if (inPropParseState()) {
				$designStatement::design.addIntersection($compSelector1.component,
															$constraint.val,
															hasSecondComponent ? $compSelector2.component : parentComponent);
			}
		}
	;

setObjectPropertyStatement[Object propertyReceiver] returns [String id, Object value]
	:	ID '=' componentPropertyValue ';'  { setObjectProperty($propertyReceiver, $ID.text, $componentPropertyValue.value); }
	;

componentPropertyValue returns [Object value]
	:	c=colorLiteral { $value = $c.val; }
	|	s=stringLiteral { $value = $s.val; }
	|	i=intLiteral { $value = $i.val; }
	|	d=doubleLiteral { $value = $d.val; }
	|	dim=dimensionLiteral { $value = $dim.val; }
	|	b=booleanLiteral { $value = $b.val; }
	|	cons=simplexConstraintLiteral { $value = $cons.val; }
	|	ins=insetsLiteral { $value = $ins.val; }
	|   contType=containerTypeLiteral { $value = $contType.val; }
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
}
@after {
	self.runMetaCode($id.sid, $list.ids, lineNumber);
}
	:	'@' id=compoundID '(' list=metaCodeIdList ')' ';'	
	;


metaCodeIdList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:		firstId=metaCodeId { ids.add($firstId.sid); }
			( ',' nextId=metaCodeId { ids.add($nextId.sid); })* 
	;


metaCodeId returns [String sid]
	:	id=compoundID 			{ $sid = $id.sid; }
	|	ptype=PRIMITIVE_TYPE	{ $sid = $ptype.text; } 
	|	lit=metaCodeLiteral 	{ $sid = $lit.text; }
	|							{ $sid = ""; }
	;

metaCodeLiteral
	:	STRING_LITERAL 
	| 	UINT_LITERAL
	|	POSITIVE_DOUBLE_LITERAL
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

mappedProperty returns [String name, List<String> mapping]
	:	propName=compoundID { $name = $propName.sid; }
		'('
		list=idList { $mapping = $list.ids; }
		')'
	;

parameter
	:	ID | NUMBERED_PARAM | RECURSIVE_PARAM
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
	:		firstClassName=classId { ids.add($firstClassName.sid); }
			(',' className=classId { ids.add($className.sid); })*
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

singleParameterList[List<String> context, boolean dynamic] returns [List<LPWithParams> props]
@init {
	props = new ArrayList<LPWithParams>();
}
	:	(first=singleParameter[context, dynamic] { props.add($first.property); }
		(',' next=singleParameter[context, dynamic] { props.add($next.property); })*)?
	;

propertyExpressionList[List<String> context, boolean dynamic] returns [List<LPWithParams> props] 
@init {
	$props = new ArrayList<LPWithParams>();
}
	:	(neList=nonEmptyPropertyExpressionList[context, dynamic] { $props = $neList.props; })?
	;
	

nonEmptyPropertyExpressionList[List<String> context, boolean dynamic] returns [List<LPWithParams> props]
@init {
	$props = new ArrayList<LPWithParams>();
}
	:	first=propertyExpression[context, dynamic] { $props.add($first.property); }
		(',' next=propertyExpression[context, dynamic] { $props.add($next.property); })* 
	; 

literal returns [LP property]
@init {
	ScriptingLogicsModule.ConstType cls = null;
	Object value = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addConstantProp(cls, value);	
	}
}
	: 	vint=uintLiteral	{ cls = ScriptingLogicsModule.ConstType.INT; value = $vint.val; }
	|	vlong=ulongLiteral	{ cls = ScriptingLogicsModule.ConstType.LONG; value = $vlong.val; }
	|	vreal=POSITIVE_DOUBLE_LITERAL	{ cls = ScriptingLogicsModule.ConstType.REAL; value = $vreal.text; }
	|	vstr=stringLiteral	{ cls = ScriptingLogicsModule.ConstType.STRING; value = $vstr.val; }  
	|	vbool=booleanLiteral	{ cls = ScriptingLogicsModule.ConstType.LOGICAL; value = $vbool.val; }
	|	vdate=dateLiteral	{ cls = ScriptingLogicsModule.ConstType.DATE; value = $vdate.val; }
	|	vdatetime=dateTimeLiteral { cls = ScriptingLogicsModule.ConstType.DATETIME; value = $vdatetime.val; }
	|	vtime=timeLiteral 	{ cls = ScriptingLogicsModule.ConstType.TIME; value = $vtime.val; }
	|	vsobj=staticObjectID { cls = ScriptingLogicsModule.ConstType.STATIC; value = $vsobj.sid; }
	|	vnull=NULL_LITERAL 	{ cls = ScriptingLogicsModule.ConstType.NULL; }
	|	vcolor=colorLiteral { cls = ScriptingLogicsModule.ConstType.COLOR; value = $vcolor.val; }		
	;

classId returns [String sid]
	:	id=compoundID { $sid = $id.sid; }
	|	pid=PRIMITIVE_TYPE { $sid = $pid.text; }
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
	
multiCompoundID returns [String sid]
	:	id=ID { $sid = $id.text; } ('.' cid=ID { $sid = $sid + '.' + $cid.text; } )*
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
		ud=udoubleLiteral { $val = isMinus ? -$ud.val : $ud.val; }
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
	
insetsLiteral returns [Insets val]
	:	'(' top=intLiteral ',' left=intLiteral ',' bottom=intLiteral ',' right=intLiteral ')' { $val = new Insets($top.val, $left.val, $bottom.val, $right.val); }
	;
	
simplexConstraintLiteral returns [DoNotIntersectSimplexConstraint val]
	:	'TO' 'THE' 'LEFT' { $val = TOTHE_LEFT; }
	|	'TO' 'THE' 'RIGHT' { $val = TOTHE_RIGHT; }
	|	'TO' 'THE' 'BOTTOM' { $val = TOTHE_BOTTOM; }
	|	'TO' 'THE' 'RIGHTBOTTOM' { $val = TOTHE_RIGHTBOTTOM; }
	|	'TO' 'NOT' 'INTERSECT' { $val = DO_NOT_INTERSECT; }
	;

insertRelativePositionLiteral returns [InsertPosition val]
	:	'BEFORE' { $val = InsertPosition.BEFORE; }
	|	'AFTER' { $val = InsertPosition.AFTER; }
	;

containerTypeLiteral returns [byte val]
	:	'CONTAINERV' { $val = ContainerType.CONTAINERV; }	
	|	'CONTAINERH' { $val = ContainerType.CONTAINERH; }	
	|	'CONTAINERVH' { $val = ContainerType.CONTAINERVH; }	
	|	'TABBED' { $val = ContainerType.TABBED_PANE; }
	|	'SPLITH' { $val = ContainerType.SPLIT_PANE_HORIZONTAL; }
	|	'SPLITV' { $val = ContainerType.SPLIT_PANE_VERTICAL; }
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
	;

formSessionScopeLiteral returns [FormSessionScope val]
	:	'OLDSESSION' { $val = FormSessionScope.OLDSESSION; }
	|	'NEWSESSION' { $val = FormSessionScope.NEWSESSION; }
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
	;

udoubleLiteral returns [double val]
	:	d=POSITIVE_DOUBLE_LITERAL  { $val = Double.parseDouble($d.text); }
	; 
		
uintLiteral returns [int val]
	:	u=UINT_LITERAL { $val = self.createScriptedInteger($u.text); }
	;		

ulongLiteral returns [long val]
	:	u=ULONG_LITERAL { $val = self.createScriptedLong($u.text.substring(0, $u.text.length() - 1)); }
	;
	
/////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////// LEXER //////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////
	
fragment NEWLINE	:	'\r'?'\n'; 
fragment SPACE		:	(' '|'\t');
fragment STR_LITERAL_CHAR	: '\\\'' | ~('\r'|'\n'|'\'');	 // overcomplicated due to bug in ANTLR Works
fragment DIGIT		:	'0'..'9';
fragment DIGITS		:	('0'..'9')+;
fragment HEX_DIGIT	: 	'0'..'9' | 'a'..'f' | 'A'..'F';
fragment FIRST_ID_LETTER	: ('a'..'z'|'A'..'Z');
fragment NEXT_ID_LETTER		: ('a'..'z'|'A'..'Z'|'_'|'0'..'9');

PRIMITIVE_TYPE  :	'INTEGER' | 'DOUBLE' | 'LONG' | 'BOOLEAN' | 'DATE' | 'DATETIME' | 'TEXT' | 'TIME' | 'WORDFILE' | 'IMAGEFILE' | 'PDFFILE' | 'CUSTOMFILE' | 'EXCELFILE' | 'STRING[' DIGITS ']' | 'ISTRING[' DIGITS ']' | 'NUMERIC[' DIGITS ',' DIGITS ']' | 'COLOR';
LOGICAL_LITERAL :	'TRUE' | 'FALSE';
NULL_LITERAL	:	'NULL';	
ID          	:	FIRST_ID_LETTER NEXT_ID_LETTER*;
WS				:	(NEWLINE | SPACE) { $channel=HIDDEN; };
STRING_LITERAL	:	'\'' STR_LITERAL_CHAR* '\'';
COLOR_LITERAL 	:	'#' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT;
COMMENTS		:	('//' .* '\n') { $channel=HIDDEN; };
UINT_LITERAL 	:	DIGITS;
ULONG_LITERAL	:	DIGITS('l'|'L');
POSITIVE_DOUBLE_LITERAL	: 	DIGITS '.' DIGITS;	  
DATE_LITERAL	:	DIGIT DIGIT DIGIT DIGIT '_' DIGIT DIGIT '_' DIGIT DIGIT; 
DATETIME_LITERAL:	DIGIT DIGIT DIGIT DIGIT '_' DIGIT DIGIT '_' DIGIT DIGIT '_' DIGIT DIGIT ':' DIGIT DIGIT;	
TIME_LITERAL	:	DIGIT DIGIT ':' DIGIT DIGIT;
NUMBERED_PARAM	:	'$' DIGITS;
RECURSIVE_PARAM :	'$' FIRST_ID_LETTER NEXT_ID_LETTER*;	
EQ_OPERAND		:	('==') | ('!=');
REL_OPERAND		: 	('<') | ('>') | ('<=') | ('>=');
MINUS			:	'-';
PLUS			:	'+';
MULT_OPERAND	:	('*') | ('/');
ADDOR_OPERAND	:	'(+)' | '(-)';
CONCAT_OPERAND	:	'##';
CONCAT_CAPITALIZE_OPERAND	:	'###';	