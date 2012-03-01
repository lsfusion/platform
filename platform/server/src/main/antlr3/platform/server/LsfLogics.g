grammar LsfLogics;

@header {
	package platform.server;

	import platform.base.OrderedMap;
	import platform.interop.ClassViewType;
	import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
	import platform.interop.navigator.FormShowType;
	import platform.server.data.Union;
	import platform.server.data.expr.query.PartitionType;
	import platform.server.form.entity.GroupObjectEntity;
	import platform.server.form.entity.PropertyObjectEntity;
	import platform.server.form.navigator.NavigatorElement;
	import platform.server.form.view.ComponentView;
	import platform.server.form.view.GroupObjectView;
	import platform.server.form.view.PropertyDrawView;
	import platform.server.form.view.panellocation.PanelLocationView;
	import platform.server.logics.linear.LP;
	import platform.server.logics.property.PropertyFollows;
	import platform.server.logics.property.Cycle;
	import platform.server.logics.scripted.*;
	import platform.server.logics.scripted.MappedProperty;
	import platform.server.logics.scripted.ScriptingLogicsModule.WindowType;
	import platform.server.logics.scripted.ScriptingLogicsModule.InsertPosition;
	import platform.server.logics.scripted.ScriptingLogicsModule.GroupingType;
	import platform.server.logics.scripted.ScriptingLogicsModule.LPWithParams;
	import platform.server.mail.EmailActionProperty;
	import platform.server.mail.EmailActionProperty.FormStorageType;
	import platform.server.mail.AttachmentFormat;
	import javax.mail.Message;
	
	import java.util.*;
	import java.awt.*;
	import org.antlr.runtime.BitSet;
	import java.util.List;

	import static java.util.Arrays.asList;
	import static platform.interop.form.layout.SingleSimplexConstraint.*;
	import static platform.server.logics.scripted.ScriptingLogicsModule.WindowType.*;
}

@lexer::header { 
	package platform.server; 
	import platform.server.logics.scripted.ScriptingLogicsModule;
}

@lexer::members {
	public ScriptingLogicsModule self;
	public ScriptingLogicsModule.State parseState;
	
	@Override
	public void emitErrorMessage(String msg) {
		if (parseState == ScriptingLogicsModule.State.GROUP) { 
			self.getErrLog().write(msg + "\n");
		}
	}
	
	@Override
	public String getErrorMessage(RecognitionException e, String[] tokenNames) {
		return self.getErrLog().getErrorMessage(this, super.getErrorMessage(e, tokenNames), e);
	}
	
	@Override
	public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
		self.getErrLog().displayRecognitionError(this, "error", tokenNames, e);
	}
}

@members {
	public ScriptingLogicsModule self;
	public ScriptingLogicsModule.State parseState;
	
	private boolean insideRecursion = false;
	
	public boolean inParseState(ScriptingLogicsModule.State parseState) {
		return this.parseState == parseState;
	}

	public boolean isFirstParseStep() {
		return inGroupParseState(); 
	}

	public boolean inGroupParseState() {
		return inParseState(ScriptingLogicsModule.State.GROUP);
	}

	public boolean inClassParseState() {
		return inParseState(ScriptingLogicsModule.State.CLASS);
	}

	public boolean inPropParseState() {
		return inParseState(ScriptingLogicsModule.State.PROP);
	}

	public boolean inTableParseState() {
		return inParseState(ScriptingLogicsModule.State.TABLE);
	}

	public boolean inIndexParseState() {
		return inParseState(ScriptingLogicsModule.State.INDEX);
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
		if (parseState == ScriptingLogicsModule.State.GROUP) { 
			self.getErrLog().write(msg + "\n");
		}
	}

	@Override
	public String getErrorMessage(RecognitionException e, String[] tokenNames) {
		return self.getErrLog().getErrorMessage(this, super.getErrorMessage(e, tokenNames), e);
	}

	@Override
	public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
		self.getErrLog().displayRecognitionError(this, "error", tokenNames, e);
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
	:	moduleHeader importDirective* statements EOF
	;

statements
	:	statement*
	;

moduleHeader
@after {
	if (isFirstParseStep()) {
		self.setModuleName($name.text);
	}
}
	:	'MODULE' name=ID ';'
	;

importDirective
@after {
	if (isFirstParseStep()) {
		self.addImportedModule($moduleName.text);
	}
}
	:	'IMPORT' moduleName=ID ';'
	;


statement
	:	(	classStatement
		|	groupStatement
		|	propertyStatement
		|	constraintStatement
		|	followsStatement
		|	writeOnChangeStatement
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


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// CLASS STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////


classStatement 
@init {
	List<String> classParents = new ArrayList<String>();
	boolean isAbstract = false;
	boolean isStatic = false;
	List<String> instanceNames = new ArrayList<String>();
	List<String> instanceCaptions = new ArrayList<String>();
}
@after {
	if (inClassParseState()) {
		self.addScriptedClass($nameCaption.name, $nameCaption.caption, isAbstract, isStatic, instanceNames, instanceCaptions, classParents);
	}
}
	:	'CLASS' ('ABSTRACT' {isAbstract = true;} | 'STATIC' {isStatic = true;})?
		nameCaption=simpleNameWithCaption
		(
			'{'
				firstInstData=simpleNameWithCaption { instanceNames.add($firstInstData.name); instanceCaptions.add($firstInstData.caption); }
				(',' nextInstData=simpleNameWithCaption { instanceNames.add($nextInstData.name); instanceCaptions.add($nextInstData.caption); })*
			'}'
			(parents=classParentsList ';' { classParents = $parents.list; })?	
		|	
			(parents=classParentsList { classParents = $parents.list; })? ';'
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
@after {
	if (inPropParseState()) {
		self.addScriptedForm($formStatement::form);
	}
}
	:	declaration=formDeclaration { $formStatement::form = $declaration.form; }
		(	formGroupObjectsList
		|	formTreeGroupObject
		|	formFiltersList
		|	formPropertiesList
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
	FormShowType showType = null;
}
@after {
	if (inPropParseState()) {
		$form = self.createScriptedForm($formNameCaption.name, $formNameCaption.caption);
		$form.setIsPrintForm(isPrint);
		$form.setShowType(showType);
	}
}
	:	'FORM' 
		formNameCaption=simpleNameWithCaption
		('PRINT' { isPrint = true; })?
		(type = formShowTypeSetting { showType = $type.value; })?
	;

formShowTypeSetting returns [FormShowType value = null]
	:	'DOCKING' { $value = FormShowType.DOCKING; }
	|	'MODAL' { $value = FormShowType.MODAL; }
	|	'FULLSCREEN' { $value = FormShowType.MODAL_FULLSCREEN; }
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

formTreeGroupObject
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
		(viewType = formGroupObjectViewType { $groupObject.setViewType($viewType.type, $viewType.isInitType); } )?
		(pageSize = formGroupObjectPageSize { $groupObject.setPageSize($pageSize.value); })?
	; 

formTreeGroupObjectDeclaration returns [ScriptingGroupObject groupObject, List<String> properties]
	:	(object = formCommonGroupObject { $groupObject = $object.groupObject; })
		(parent = treeGroupParentDeclaration { $properties = $parent.properties; })?
	; 

treeGroupParentDeclaration returns [List<String> properties = new ArrayList<String>()]
	:	'PARENT'
		(id = compoundID { $properties.add($id.sid); })+
	;

formCommonGroupObject returns [ScriptingGroupObject groupObject]
	:	sdecl=formSingleGroupObjectDeclaration
		{
			$groupObject = new ScriptingGroupObject(null, asList($sdecl.name), asList($sdecl.className), asList($sdecl.caption));
		}
	|	mdecl=formMultiGroupObjectDeclaration
		{
			$groupObject = new ScriptingGroupObject($mdecl.groupName, $mdecl.objectNames, $mdecl.classNames, $mdecl.captions);
		}
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

formSingleGroupObjectDeclaration returns [String name, String className, String caption] 
	:	foDecl=formObjectDeclaration { $name = $foDecl.name; $className = $foDecl.className; $caption = $foDecl.caption; }
	;

formMultiGroupObjectDeclaration returns [String groupName, List<String> objectNames, List<String> classNames, List<String> captions]
@init {
	$objectNames = new ArrayList<String>();
	$classNames = new ArrayList<String>();
	$captions = new ArrayList<String>();
}
	:	(gname=ID { $groupName = $gname.text; } '=')?
		'('
			objDecl=formObjectDeclaration { $objectNames.add($objDecl.name); $classNames.add($objDecl.className); $captions.add($objDecl.caption); }
			(',' objDecl=formObjectDeclaration { $objectNames.add($objDecl.name); $classNames.add($objDecl.className); $captions.add($objDecl.caption); })+
		')'
	;


formObjectDeclaration returns [String name, String className, String caption] 
	:	(objectName=ID { $name = $objectName.text; } '=')?	
		id=classId { $className = $id.sid; }
		(c=stringLiteral { $caption = $c.val; })?
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
	:	(	'READONLY' { $options.setReadOnly(true); }
		|	'EDITABLE' { $options.setReadOnly(false); }
		|	'COLUMNS' '(' ids=nonEmptyIdList ')' { $options.setColumns(getGroupObjectsList($ids.ids)); }
		|	'SHOWIF' mappedProp=formMappedProperty { $options.setShowIf(getPropertyWithMapping($mappedProp.name, $mappedProp.mapping)); }
		|	'READONLYIF' propObj=formPropertyObject { $options.setReadOnlyIf($propObj.property); }
		|	'HIGHLIGHTIF' propObj=formPropertyObject { $options.setHighlightIf($propObj.property); }
		|	'HEADER' propObj=formPropertyObject { $options.setHeader($propObj.property); }
		|	'FOOTER' propObj=formPropertyObject { $options.setFooter($propObj.property); }
		|	'FORCE' viewType=classViewType { $options.setForceViewType($viewType.type); }
		|	'TODRAW' toDraw=formGroupObjectEntity { $options.setToDraw($toDraw.groupObject); }
		)*
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

formPropertyObject returns [PropertyObjectEntity property = null]
	:	mappedProperty=formMappedProperty
		{
			if (inPropParseState()) {
				$property = $formStatement::form.addPropertyObject(mappedProperty.name, mappedProperty.mapping);
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
	List<String> propertyNames = new ArrayList<String>();
	List<List<String>> propertyMappings = new ArrayList<List<String>>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedFilters(propertyNames, propertyMappings);
	}
}
	:	'FILTERS'
		decl=formFilterDeclaration { propertyNames.add($decl.name); propertyMappings.add($decl.mapping);}
	    (',' decl=formFilterDeclaration { propertyNames.add($decl.name); propertyMappings.add($decl.mapping);})*
	;
	
filterGroupDeclaration
@init {
	String filterGroupSID = null;
	List<String> captions = new ArrayList<String>();
	List<String> keystrokes = new ArrayList<String>();
	List<String> properties = new ArrayList<String>();
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
				properties.add($filter.name);
				mappings.add($filter.mapping);
				defaults.add($setDefault.isDefault);
			}
		)+
	;

	
formFilterDeclaration returns [String name, List<String> mapping] 
	:	'NOT' 'NULL' propDecl=formMappedProperty { $name = $propDecl.name; $mapping = $propDecl.mapping; }
	;
	
filterSetDefault returns [boolean isDefault = false]
	:	('DEFAULT' { $isDefault = true; })?
	;

formOrderByList
@init {
	boolean ascending = true;
	List<String> properties = new ArrayList<String>();
	List<Boolean> orders = new ArrayList<Boolean>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedDefaultOrder(properties, orders);
	}
}
	:	'ORDER' 'BY' orderedProp=formPropertyWithOrder { properties.add($orderedProp.id); orders.add($orderedProp.order); }
		(',' orderedProp=formPropertyWithOrder { properties.add($orderedProp.id); orders.add($orderedProp.order); } )*
	;
	
formPropertyWithOrder returns [String id, boolean order = true]
	:	ID { $id = $ID.text; } ('ASC' | 'DESC' { $order = false; })?
	;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// PROPERTY STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

propertyStatement
@init {
	LP<?> property = null;
	List<String> context = new ArrayList<String>();
	boolean dynamic = true;
}
	:	declaration=propertyDeclaration { if ($declaration.paramNames != null) { context = $declaration.paramNames; dynamic = false; }}
		'=' 
		(	def=expressionUnfriendlyPD[context, dynamic, false] { property = $def.property; }
		|	expr=propertyExpression[context, dynamic] { if (inPropParseState()) {property = $expr.property.property;} }
		)
		settings=commonPropertySettings[property, $declaration.name, $declaration.caption, context]
		';'
	;


propertyDeclaration returns [String name, String caption, List<String> paramNames]
	:	propNameCaption=simpleNameWithCaption { $name = $propNameCaption.name; $caption = $propNameCaption.caption; }
		('(' paramList=idList ')' { $paramNames = $paramList.ids; })? 
	;


propertyExpression[List<String> context, boolean dynamic] returns [LPWithParams property]
	:	pe=andPE[context, dynamic] { $property = $pe.property; }
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
	LP<?> mainProp = null;
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
	:	lhs=additivePE[context, dynamic] { leftProp = $lhs.property; }
		(
			(   operand=REL_OPERAND { op = $operand.text; }
			    rhs=additivePE[context, dynamic] { rightProp = $rhs.property; }
			)
		|	def=typePropertyDefinition { mainProp = $def.property; }
		)?
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
	:	firstExpr=simplePE[context, dynamic] { props.add($firstExpr.property); }
		(operand=MULT_OPERAND { ops.add($operand.text); }
		nextExpr=simplePE[context, dynamic] { props.add($nextExpr.property); })*
	;
	
		 
	 
simplePE[List<String> context, boolean dynamic] returns [LPWithParams property]
	:	'(' expr=propertyExpression[context, dynamic] ')' { $property = $expr.property; } 
	|	primitive=expressionPrimitive[context, dynamic] { $property = $primitive.property; } 
	|	uexpr=unaryMinusPE[context, dynamic] { $property = $uexpr.property; }
	;

	
unaryMinusPE[List<String> context, boolean dynamic] returns [LPWithParams property] 	
@after {
	if (inPropParseState()) {
		$property = self.addScriptedUnaryMinusProp($property);
	}
}
	:	MINUS expr=simplePE[context, dynamic] { $property = $expr.property; }
	;		 
	

expressionPrimitive[List<String> context, boolean dynamic] returns [LPWithParams property]
	:	paramName=parameter
        {
			if (inPropParseState()) {
				$property = new LPWithParams(null, Collections.singletonList(self.getParamIndex($paramName.text, $context, $dynamic, insideRecursion)));
			}
		}
	|	expr=expressionFriendlyPD[context, dynamic] { $property = $expr.property; }
	;

expressionFriendlyPD[List<String> context, boolean dynamic] returns [LPWithParams property]
	:	joinDef=joinPropertyDefinition[context, dynamic] { $property = $joinDef.property; } 
	|	unionDef=unionPropertyDefinition[context, dynamic] { $property = $unionDef.property;} 
	|	ifElseDef=ifElsePropertyDefinition[context, dynamic] { $property = $ifElseDef.property; }
	|	caseDef=casePropertyDefinition[context, dynamic] { $property = $caseDef.property; }
	|	partDef=partitionPropertyDefinition[context, dynamic] { $property = $partDef.property; }
	|	recDef=recursivePropertyDefinition[context, dynamic] { $property = $recDef.property; } 
	|	constDef=literal { $property = new LPWithParams($constDef.property, new ArrayList<Integer>()); }
	;

expressionUnfriendlyPD[List<String> context, boolean dynamic, boolean innerPD] returns [LP property]
	:	ciPD=contextIndependentPD[innerPD] { $property = $ciPD.property; }
	|	actPD=actionPropertyDefinition[context, dynamic] { $property = $actPD.property; }	
	;

contextIndependentPD[boolean innerPD] returns [LP property]
	: 	dataDef=dataPropertyDefinition[innerPD] { $property = $dataDef.property; }
	|	formulaProp=formulaPropertyDefinition { $property = $formulaProp.property; }
	|	groupDef=groupPropertyDefinition { $property = $groupDef.property; }
	|	typeDef=typePropertyDefinition { $property = $typeDef.property; }
	;

joinPropertyDefinition[List<String> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedJProp($mainPropObj.property, $exprList.props);
	}
}
	:	mainPropObj=propertyObject
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
	|	'UNIQUE' { $type = GroupingType.UNIQUE; }
	;


partitionPropertyDefinition[List<String> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> paramProps = new ArrayList<LPWithParams>();
	PartitionType type = null;
	int groupExprCnt;
	boolean ascending = true;
	boolean useLast = true;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedOProp(type, ascending, useLast, groupExprCnt, paramProps);
	}
}
	:	'PARTITION' ('SUM' {type = PartitionType.SUM;} | 'PREV' {type = PartitionType.PREVIOUS;})
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
	:	('SESSION' { sessionProp = true; } )?
		'DATA'
		returnClass=classId
		'('
			paramClassNames=classIdList
		')'
	;


unionPropertyDefinition[List<String> context, boolean dynamic] returns [LPWithParams property]
@init {
	Union type = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedUProp(type, $exprList.props);
	}
}
	:	'UNION'
		(('MAX' {type = Union.MAX;}) | ('SUM' {type = Union.SUM;}) | ('OVERRIDE' {type = Union.OVERRIDE;}) | ('XOR' { type = Union.XOR;}) | ('EXCLUSIVE' {type = Union.EXCLUSIVE;}))
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
		('CYCLES' { cycleType = Cycle.YES; } ('IMPOSSIBLE' { cycleType = Cycle.IMPOSSIBLE; })? )?
	;


formulaPropertyDefinition returns [LP property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedSFProp($className.sid, $formulaText.text);
	}
}
	:	'FORMULA' className=classId formulaText=STRING_LITERAL
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
	|	'['	(	expr=propertyExpression[newContext, true] { if (inPropParseState()) { $property = $expr.property.property; } }
			|	def=expressionUnfriendlyPD[newContext, true, true] { $property = $def.property; }
			)
		']'
	;


commonPropertySettings[LP property, String propertyName, String caption, List<String> namedParams]
@init {
	String groupName = null;
	boolean isPersistent = false;
}
@after {
	if (inPropParseState()) {
		self.addSettingsToProperty(property, propertyName, caption, namedParams, groupName, isPersistent);
	}
}
	: 	(	'IN' name=compoundID { groupName = $name.sid; }
		|	'PERSISTENT' { isPersistent = true; }
		|	panelLocationSetting [property]
		|	fixedCharWidthSetting [property]
		|	minCharWidthSetting [property]
		|	maxCharWidthSetting [property]
		|	prefCharWidthSetting [property]
		|	imageSetting [property]
		|	editKeySetting [property]
		|	autosetSetting [property]
		|	confirmSetting [property]
		|	regexpSetting [property]
		|	loggableSetting [property]
		|	echoSymbolsSetting [property]
		|	indexSetting [propertyName]
		)*
	;


panelLocationSetting [LP property]
@init {
	boolean toolbar = false;
	String sid = null;
	boolean defaultProperty = false;
}
@after {
	if (inPropParseState()) {
		self.setPanelLocation($property, toolbar, sid, defaultProperty);
	}
}
	:	'TOOLBAR' { toolbar = true; }
	|	'SHORTCUT' { toolbar = false; } (name = compoundID { sid = $name.sid; })? ('DEFAULT' { defaultProperty = true; })?
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

loggableSetting [LP property]
@after {
	if (inPropParseState()) {
		self.makeLoggable(property);
	}
}
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
		self.addScriptedIndices(Arrays.asList(propName));
	}
}
	:	'INDEXED'
	;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// ACTION PROPERTIES ///////////////////////////
////////////////////////////////////////////////////////////////////////////////

actionPropertyDefinition[List<String> context, boolean dynamic] returns [LP property]
@init {
	List<String> localContext = context;
	boolean localDynamic = dynamic;
	boolean ownContext = false;
}
@after {
	if (inPropParseState()) {
		self.checkActionAllParamsUsed(context, $property, ownContext);
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
		pdb=actionPropertyDefinitionBody[localContext, localDynamic] { if (inPropParseState()) $property = $pdb.property.property; }
	;
	
actionPropertyDefinitionBody[List<String> context, boolean dynamic] returns [LPWithParams property]
	:	extPDB=extendContextActionPDB[context, dynamic] { $property = $extPDB.property; }
	|	keepPDB=keepContextActionPDB[context, dynamic] { $property = $keepPDB.property; }
	|	trivPDB=customActionPDB[context, dynamic] { $property = $trivPDB.property; }
	;

extendContextActionPDB[List<String> context, boolean dynamic] returns [LPWithParams property]
@init {
	if (inPropParseState() && dynamic) {
		self.getErrLog().emitExtendActionContextError(self.getParser());
	}
}
	:	setPDB=setActionPropertyDefinitionBody[context] { $property = $setPDB.property; }
	|	forPDB=forActionPropertyDefinitionBody[context] { $property = $forPDB.property; }
	;
	
keepContextActionPDB[List<String> context, boolean dynamic] returns [LPWithParams property]
	:	listPDB=listActionPropertyDefinitionBody[context, dynamic] { $property = $listPDB.property; }
	|	execPDB=execActionPropertyDefinitionBody[context, dynamic] { $property = $execPDB.property; }	
	|	ifPDB=ifActionPropertyDefinitionBody[context, dynamic] { $property = $ifPDB.property; }
	;

customActionPDB[List<String> context, boolean dynamic] returns [LPWithParams property]
@init {
	$property = new LPWithParams(null, new ArrayList<Integer>());
}
	:	formPDB=formActionPropertyDefinitionBody { $property.property = $formPDB.property; }
	|	addPDB=addObjectActionPropertyDefinitionBody { $property.property = $addPDB.property; }
	|	actPDB=customActionPropertyDefinitionBody { $property.property = $actPDB.property; }
	|   msgPDB=messageActionPropertyDefinitionBody[context, dynamic] { $property = $msgPDB.property; }
	|   mailPDB=emailActionPropertyDefinitionBody[context, dynamic] { $property = $mailPDB.property; }
	;

			
formActionPropertyDefinitionBody returns [LP property]
@init {
	boolean newSession = false;
	boolean isModal = false;
	boolean checkOnOk = false;
	List<String> objects = new ArrayList<String>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedFAProp($formName.sid, objects, $exprList.props, $className.sid, newSession, isModal, checkOnOk);	
	}
}
	:	'FORM' formName=compoundID 
		('OBJECTS' list=nonEmptyIdList { objects = $list.ids; })? 
		('SET' exprList=nonEmptyPropertyExpressionList[objects, false])? 
		('CLASS' className=classId)?
		('NEWSESSION' { newSession = true; })? 
		('MODAL' { isModal = true; })?
		('CHECK' { checkOnOk = true; })?
	;

customActionPropertyDefinitionBody returns [LP property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedCustomActionProp($classN.val);	
	}
}
	:	'CUSTOM' classN = stringLiteral 
	;



addObjectActionPropertyDefinitionBody returns [LP property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedAddObjProp($cid.sid);
	}
}
	:	'ADDOBJ' cid=classId
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

listActionPropertyDefinitionBody[List<String> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
	boolean newSession = false;
	boolean doApply = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedListAProp(newSession, doApply, props);
	}
}
	:	('NEWSESSION' { newSession = true; } ('AUTOAPPLY' {doApply = true; } )? )?
		'{'
			(	PDB=actionPropertyDefinitionBody[context, dynamic] ';' { props.add($PDB.property); }
		    |   emptyStatement
		    )*
		'}'
	;

execActionPropertyDefinitionBody[List<String> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedJoinAProp($prop.property, $exprList.props);
	}
}
	:	'EXEC'
		prop=propertyObject
		{ if (inPropParseState()) self.checkActionProperty($prop.property); }		
		'('
		exprList=propertyExpressionList[context, dynamic]
		')'
	;

setActionPropertyDefinitionBody[List<String> context] returns [LPWithParams property]
@init {
	List<String> newContext = new ArrayList<String>(context); 
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedSetPropertyAProp(context, $p1.property, $p2.property);
	}
}
	:	'SET'
		p1=propertyExpression[newContext, true]
		'<-'
		p2=propertyExpression[newContext, false] //no need to use dynamic context, because params should be either on global context or used in the left expression
	;

ifActionPropertyDefinitionBody[List<String> context, boolean dynamic] returns [LPWithParams property]
@init {
	LPWithParams elseProp = null;	
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedIfAProp($expr.property, $thenPDB.property, elseProp);
	}
}
	:	'IF' expr=propertyExpression[context, dynamic] 
		'THEN' thenPDB=actionPropertyDefinitionBody[context, dynamic]
		('ELSE' elsePDB=actionPropertyDefinitionBody[context, dynamic] { elseProp = $elsePDB.property; })?
	;

forActionPropertyDefinitionBody[List<String> context] returns [LPWithParams property]
@init {
	boolean recursive = false;
	boolean descending = false;
	List<String> newContext = new ArrayList<String>(context);
	List<LPWithParams> orders = new ArrayList<LPWithParams>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedForAProp(context, $expr.property, orders, $actPDB.property, recursive, descending);
	}	
}
	:	(	'FOR' 
		| 	'WHILE' { recursive = true; }
		)
		expr=propertyExpression[newContext, true]
		('ORDER' 
			('DESC' { descending = true; } )? 
			ordExprs=nonEmptyPropertyExpressionList[newContext, false] { orders = $ordExprs.props; }
		)?	
		'DO' actPDB=actionPropertyDefinitionBody[newContext, false]
	;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// CONSTRAINT STATEMENT //////////////////////////
////////////////////////////////////////////////////////////////////////////////

constraintStatement 
@init {
	boolean checked = false;
}
@after {
	if (inPropParseState()) {
		self.addScriptedConstraint($expr.property.property, checked, $message.text);
	}
}
	:	'CONSTRAINT' ('CHECKED' { checked = true; })?
		expr=propertyExpression[new ArrayList<String>(), true]
		'MESSAGE' message=STRING_LITERAL
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
}
@after {
	if (inPropParseState()) {
		self.addScriptedFollows(mainProp, context, options, props);
	}
}
	:	prop=propertyWithNamedParams { mainProp = $prop.name; context = $prop.params; }
		'=>'
		firstExpr=propertyExpression[context, false] ('RESOLVE' type=followsResolveType)?
		{
			props.add($firstExpr.property); 
			options.add(type == null ? PropertyFollows.RESOLVE_ALL : $type.type);
		}
		(',' nextExpr=propertyExpression[context, false] ('RESOLVE' type=followsResolveType)?
			{
		     	props.add($nextExpr.property); 
		     	options.add(type == null ? PropertyFollows.RESOLVE_ALL : $type.type);
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
////////////////////////////////// CHANGE STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

writeOnChangeStatement
@init {
	boolean old = false;
	boolean anyChange = true;
	List<String> context;
}
@after {
	if (inPropParseState()) {
		self.addScriptedWriteOnChange($mainProp.name, context, old, anyChange, $valueExpr.property, $changeExpr.property);
	}
}
	:	mainProp=propertyWithNamedParams { context = $mainProp.params; }
		'<-'
		('OLD' { old = true; })?
		valueExpr=propertyExpression[context, false]
		'ON' ('CHANGE' | 'ASSIGN' { anyChange = false; })
		changeExpr=propertyExpression[context, false]
		';'
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
////////////////////////////////// INDEX STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

indexStatement
@after {
	if (inIndexParseState()) {
		self.addScriptedIndices($list.ids);
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
	:	'NAVIGATOR' navigatorElementStatementBody[self.baseLM.baseElement]
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
@init {
	boolean hasPosition = false;
}
	:	'ADD' elem=navigatorElementSelector (caption=stringLiteral)? posSelector=navigatorElementInsertPositionSelector[parentElement] ('TO' wid=compoundID)?
		{
			if (inPropParseState()) {
				self.setupNavigatorElement($elem.element, $caption.val, $posSelector.position, $posSelector.anchor, $wid.sid);
			}
		}
		navigatorElementStatementBody[$elem.element]
	;

newNavigatorElementStatement[NavigatorElement parentElement]
@init {
	NavigatorElement newElement = null;
}
	:	'NEW' id=ID caption=stringLiteral posSelector=navigatorElementInsertPositionSelector[parentElement] ('TO' wid=compoundID)?
		{
			if (inPropParseState()) {
				newElement = self.createScriptedNavigatorElement($id.text, $caption.val, $posSelector.position, $posSelector.anchor, $wid.sid);
			}
		}
		navigatorElementStatementBody[newElement]
	;
	
navigatorElementInsertPositionSelector[NavigatorElement parentElement] returns [InsertPosition position, NavigatorElement anchor]
@init {
	$position = InsertPosition.IN;
	$anchor = parentElement;
}
	:	(	pos=insertPositionLiteral { $position = $pos.val; }
			elem=navigatorElementSelector { $anchor = $elem.element; }
		)?
	;

setupNavigatorElementStatement
	:	elem=navigatorElementSelector (caption=stringLiteral)? ('TO' wid=compoundID)?
		{
			if (inPropParseState()) {
				self.setupNavigatorElement($elem.element, $caption.val, null, null, $wid.sid);
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
	:	'DESIGN' cid=compoundID (caption=stringLiteral)? ('FROM' 'DEFAULT' { applyDefault = true; })?
		{
			if (inPropParseState()) {
				$designStatement::design = formView = self.createScriptedFormView($cid.sid, $caption.val, applyDefault);
			}
		}
		componentStatementBody[formView, formView == null ? null : formView.mainContainer]
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
	boolean hasPosition = false;
	ComponentView newComp = null;
}
	:	'NEW' cid=multiCompoundID (insPosition=insertPositionLiteral posSelector=componentSelector { hasPosition = true; })?
		{
			if (inPropParseState()) {
				newComp = $designStatement::design.createNewComponent($cid.sid,
																		hasPosition ? $insPosition.val : InsertPosition.IN,
																		hasPosition ? $posSelector.component : $parentComponent);
			}
		}
		componentStatementBody[newComp, newComp]
	;
	
addComponentStatement[ComponentView parentComponent]
@init {
	boolean hasPosition = false;
	ComponentView insComp = null;
}
	:	'ADD' insSelector=componentSelector { insComp = $insSelector.component; }
		( insPosition=insertPositionLiteral posSelector=componentSelector { hasPosition = true; } )?
		{
			if (inPropParseState()) {
				$designStatement::design.moveComponent(insComp,
														hasPosition ? $insPosition.val : InsertPosition.IN,
														hasPosition ? $posSelector.component : $parentComponent);
			}
		}
		componentStatementBody[insComp, insComp]
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
	:	pname=formPropertyName
		{
			if (inPropParseState()) {
				$propertyView = $designStatement::design.getPropertyView($pname.name);
			}
		}
	|	mappedProp=formMappedProperty
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
	|	panLoc=panelLocationLiteral { $value = $panLoc.val; }
	;



////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// META STATEMENT //////////////////////////////
////////////////////////////////////////////////////////////////////////////////

metaCodeDeclarationStatement
@init {
	String code;
	List<String> tokens;
}
@after {
	if (isFirstParseStep()) {
		self.addScriptedMetaCodeFragment($id.text, $list.ids, tokens);
	}
}
	
	:	'META' id=ID '(' list=idList ')'  
		{
			tokens = self.grabMetaCode($id.text);
		}
		'END'
	;


metaCodeStatement
@after {
	self.runMetaCode($id.sid, $list.ids);
}
	:	'@' id=compoundID '(' list=metaCodeIdList ')' ';'	
	;


metaCodeIdList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:		( firstId=metaCodeId { ids.add($firstId.sid); }
			( ',' nextId=metaCodeId { ids.add($nextId.sid); })* )?
	;


metaCodeId returns [String sid]
	:	id=compoundID 			{ $sid = $id.sid; }
	|	ptype=PRIMITIVE_TYPE	{ $sid = $ptype.text; } 
	|	str=STRING_LITERAL 		{ $sid = $str.text; }
	;

////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////// COMMON /////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

emptyStatement
	:	';'
	;

propertyWithNamedParams returns [String name, List<String> params]
	:	propName=compoundID { $name = $propName.sid; }
		'('
		list=idList { $params = $list.ids; }
		')'
	;

parameter
	:	ID | NUMBERED_PARAM | RECURSIVE_PARAM
	;


simpleNameWithCaption returns [String name, String caption] 
	:	simpleName=ID { $name = $simpleName.text; }
		(captionStr=STRING_LITERAL { $caption = $captionStr.text; })?
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

parameterList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:	(firstParam=parameter	 { $ids.add($firstParam.text); }
		(',' nextParam=parameter { $ids.add($nextParam.text); })* )?
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
	String text = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addConstantProp(cls, text);	
	}
}
	: 	strInt=uintLiteral	{ cls = ScriptingLogicsModule.ConstType.INT; text = $strInt.text; }
	|	strReal=udoubleLiteral	{ cls = ScriptingLogicsModule.ConstType.REAL; text = $strReal.text; }
	|	str=STRING_LITERAL	{ cls = ScriptingLogicsModule.ConstType.STRING; text = $str.text; }  
	|	str=LOGICAL_LITERAL	{ cls = ScriptingLogicsModule.ConstType.LOGICAL; text = $str.text; }
	|	strEnum=strictCompoundID { cls = ScriptingLogicsModule.ConstType.ENUM; text = $strEnum.sid; } 
	|	strNull=NULL_LITERAL { cls = ScriptingLogicsModule.ConstType.NULL; }
	;
	
classId returns [String sid]
	:	id=compoundID { $sid = $id.sid; }
	|	pid=PRIMITIVE_TYPE { $sid = $pid.text; }
	;

compoundID returns [String sid]
	:	firstPart=ID { $sid = $firstPart.text; } ('.' secondPart=ID { $sid = $sid + '.' + $secondPart.text; })?
	;

strictCompoundID returns [String sid]
	:	firstPart=ID '.' secondPart=ID { $sid = $firstPart.text + '.' + $secondPart.text; }
	;
	
multiCompoundID returns [String sid]
	:	id=ID { $sid = $id.text; } ('.' cid=ID { $sid = $sid + "." + $cid.text; } )*
	;

colorLiteral returns [Color val]
	:	c=COLOR_LITERAL { $val = Color.decode($c.text); }
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

doubleLiteral returns [double val]
@init {
	boolean isMinus = false;
}
	:	(MINUS {isMinus=true;})?
		ud=udoubleLiteral { $val = isMinus ? -$ud.val : $ud.val; }
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

insertPositionLiteral returns [InsertPosition val]
	:	'IN' { $val = InsertPosition.IN; }
	|	'BEFORE' { $val = InsertPosition.BEFORE; }
	|	'AFTER' { $val = InsertPosition.AFTER; }
	;

panelLocationLiteral returns [PanelLocationView val]
@init {
	boolean toolbar = false;
	PropertyDrawView property = null;
	boolean defaultProperty = false;
} 
@after {
	if (inPropParseState()) {
		$val = $designStatement::design.createPanelLocation(toolbar, property, defaultProperty);
	}
}
	:	'TOOLBAR' { toolbar = true; }
	|	'SHORTCUT' { toolbar = false; } (onlyProp=propertySelector { property = $onlyProp.propertyView; })? ('DEFAULT' { defaultProperty = true; })?
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
	:	u=UINT_LITERAL { $val = Integer.parseInt($u.text); }
	;		


/////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////// LEXER //////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////
	
fragment NEWLINE	:	'\r'?'\n'; 
fragment SPACE		:	(' '|'\t');
fragment STR_LITERAL_CHAR	: '\\\'' | ~('\r'|'\n'|'\'');	 // overcomplicated due to bug in ANTLR Works
fragment DIGITS		:	('0'..'9')+;
fragment HEX_DIGIT	: 	'0'..'9' | 'a'..'f' | 'A'..'F';
fragment FIRST_ID_LETTER	: ('a'..'z'|'A'..'Z');
fragment NEXT_ID_LETTER		: ('a'..'z'|'A'..'Z'|'_'|'0'..'9');

PRIMITIVE_TYPE  :	'INTEGER' | 'DOUBLE' | 'LONG' | 'BOOLEAN' | 'DATE' | 'DATETIME' | 'TEXT' | 'TIME' | 'STRING[' DIGITS ']' | 'ISTRING[' DIGITS ']';
LOGICAL_LITERAL :	'TRUE' | 'FALSE';	
NULL_LITERAL	:	'NULL';	
ID          	:	FIRST_ID_LETTER NEXT_ID_LETTER*;
WS				:	(NEWLINE | SPACE) { $channel=HIDDEN; };
STRING_LITERAL	:	'\'' STR_LITERAL_CHAR* '\'';
COLOR_LITERAL 	:	'#' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT;
COMMENTS		:	('//' .* '\n') { $channel=HIDDEN; };
UINT_LITERAL 	:	DIGITS;
POSITIVE_DOUBLE_LITERAL	: 	DIGITS '.' DIGITS;	  
NUMBERED_PARAM	:	'$' DIGITS;
RECURSIVE_PARAM :	'$' FIRST_ID_LETTER NEXT_ID_LETTER*;	
EQ_OPERAND		:	('==') | ('!=');
REL_OPERAND		: 	('<') | ('>') | ('<=') | ('>=');
MINUS			:	'-';
PLUS			:	'+';
MULT_OPERAND	:	('*') | ('/');
CONCAT_OPERAND	:	'##';
CONCAT_CAPITALIZE_OPERAND	:	'###';	