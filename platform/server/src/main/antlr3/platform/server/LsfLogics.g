grammar LsfLogics;

@header { 
	package platform.server; 
	import platform.server.logics.ScriptingLogicsModule; 
	import platform.server.logics.ScriptingFormEntity;
	import platform.server.data.Union;
	import platform.server.logics.linear.LP;
	import platform.interop.ClassViewType;
	import java.util.Collections;
	import java.util.Set;
	import java.util.HashSet;
	import java.util.Arrays;
}

@lexer::header { 
	package platform.server; 
}

@members { 
	public ScriptingLogicsModule self;
	public ScriptingLogicsModule.State parseState;
}



script	
	:	importDirective* statement*;


importDirective
@init {
	String name;
}
@after {
        if (parseState == ScriptingLogicsModule.State.GROUP) {
        	self.addImportedModule(name);
        }
}
	:	'IMPORT' moduleName=ID ';' { name = $moduleName.text; };


statement
	:	(classStatement | groupStatement | propertyStatement | tableStatement | indexStatement | formStatement) ';';


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// CLASS STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

classStatement 
@init {
	List<String> classParents;
	String name; 
	String captionStr = null;
}
@after {
	if (parseState == ScriptingLogicsModule.State.CLASS) {
		self.addScriptedClass(name, captionStr, isAbstract, classParents);
	}
}
	:	isAbstract=classDeclarant className=ID 	{ name = $className.text; }
			(caption=STRING_LITERAL { captionStr = $caption.text; })?  
			':'
			parentList=nonEmptyCompoundIdList { classParents = $parentList.ids; };	  


classDeclarant returns [boolean isAbstract]
	:	'CLASS' { $isAbstract = false; } |
		'CLASS' 'ABSTRACT' { $isAbstract = true; }; 


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// GROUP STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

groupStatement
@init {
	String parent = null;
	String name;
	String captionStr = null;
}
@after {
	if (parseState == ScriptingLogicsModule.State.GROUP) {
		self.addScriptedGroup(name, captionStr, parent);
	}
}
	:	'GROUP' groupName=ID { name = $groupName.text; }
			(caption=STRING_LITERAL { captionStr = $caption.text; })?  
			(':' parentName=compoundID { parent = $parentName.text; })?;


////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////// FORM STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

formStatement
@init {
	ScriptingFormEntity form;
}
@after {
	if (parseState == ScriptingLogicsModule.State.NAVIGATOR) {
		self.addScriptedForm(form);
	}
}
	:	declaration=formDeclaration { form = $declaration.form; }
		('OBJECTS' list=formGroupObjectsList[form] |
		'PROPERTIES' list=formPropertiesList[form] |
		'FILTERS' list=formFiltersList[form])*;

	
formDeclaration returns [ScriptingFormEntity form]
@init {
	String name;
	String caption = null;
}
@after {
	if (parseState == ScriptingLogicsModule.State.NAVIGATOR) {
		$form = self.createScriptedForm(name, caption);
	}
}
	:	'FORM' 
		formName=ID { name = $formName.text; }
		(formCaption=STRING_LITERAL { caption = $formCaption.text; })?;


formGroupObjectsList[ScriptingFormEntity form]  // needs refactoring
@init {
	List<List<String>> names = new ArrayList<List<String>>();
	List<List<String>> classNames = new ArrayList<List<String>>(); 
	List<ClassViewType> groupViewType = new ArrayList<ClassViewType>();
	List<Boolean> isInitType = new ArrayList<Boolean>();
}
@after {
	if (parseState == ScriptingLogicsModule.State.NAVIGATOR) {
		$form.addScriptedGroupObjects(names, classNames, groupViewType, isInitType);
	}
}
	:	groupElement=formGroupObjectDeclaration { names.add($groupElement.objectNames); classNames.add($groupElement.classIds); 
							  groupViewType.add($groupElement.type); isInitType.add($groupElement.isInitType);} 
		(',' groupElement=formGroupObjectDeclaration { names.add($groupElement.objectNames); classNames.add($groupElement.classIds); 
								groupViewType.add($groupElement.type); isInitType.add($groupElement.isInitType);})*;


formGroupObjectDeclaration returns [List<String> objectNames, List<String> classIds, ClassViewType type, boolean isInitType]
@init {
	$objectNames = new ArrayList<String>();
	$classIds = new ArrayList<String>();
}
	:	(decl=formSingleGroupObjectDeclaration { $objectNames.add($decl.name); $classIds.add($decl.className); } |
		('(' 
		objDecl=formObjectDeclaration { $objectNames.add($objDecl.name); $classIds.add($objDecl.className); }	
		(',' objDecl=formObjectDeclaration { $objectNames.add($objDecl.name); $classIds.add($objDecl.className); })+	
		')'))
		(viewType=formGroupObjectViewType { $type = $viewType.type; $isInitType = $viewType.isInitType; })?; 


formGroupObjectViewType returns [ClassViewType type, boolean isInitType]
	: ('INIT' {$isInitType = true;} | 'FIXED' {$isInitType = false;})
	  ('PANEL' {$type = ClassViewType.PANEL;} | 'HIDE' {$type = ClassViewType.HIDE;} | 'GRID' {$type = ClassViewType.GRID;});


formSingleGroupObjectDeclaration returns [String name, String className] 
	:	foDecl=formObjectDeclaration { $name = $foDecl.name; $className = $foDecl.className; };


formObjectDeclaration returns [String name, String className] 
	:	(objectName=ID { $name = $objectName.text; } '=')?	
		id=classId { $className = $id.text; }; 
	
	
formPropertiesList[ScriptingFormEntity form] 
@init {
	List<String> properties = new ArrayList<String>();
	List<List<String>> mapping = new ArrayList<List<String>>();
}
@after {
	if (parseState == ScriptingLogicsModule.State.NAVIGATOR) {
		$form.addScriptedPropertyDraws(properties, mapping);
	}
}
	:	decl=formPropertyDeclaration { properties.add($decl.name); mapping.add($decl.mapping); }
		(',' decl=formPropertyDeclaration { properties.add($decl.name); mapping.add($decl.mapping); })*;


formPropertyDeclaration returns [String name, List<String> mapping]
	:	id=compoundID { $name = $id.text; }
		'(' 
		objects=idList { $mapping = $objects.ids; } 
		')';


formFiltersList[ScriptingFormEntity form] 
@init {
	List<String> propertyNames = new ArrayList<String>();
	List<List<String>> propertyMappings = new ArrayList<List<String>>();
}
@after {
	if (parseState == ScriptingLogicsModule.State.NAVIGATOR) {
		$form.addScriptedFilters(propertyNames, propertyMappings);
	}
}
	: decl=formFilterDeclaration { propertyNames.add($decl.name); propertyMappings.add($decl.mapping);}
	  (',' decl=formFilterDeclaration { propertyNames.add($decl.name); propertyMappings.add($decl.mapping);})*;

	
formFilterDeclaration returns [String name, List<String> mapping] 
	: 'NOT' 'NULL' propDecl=formPropertyDeclaration { $name = $propDecl.name; $mapping = $propDecl.mapping; };	

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// PROPERTY STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

propertyStatement
	:	declaration=propertyDeclaration 
		'=' 
		expr=propertyExpression[declaration.name, declaration.paramNames, declaration.paramNames]
		settings=commonPropertySettings[$expr.property]; 

	
propertyDeclaration returns [String name, List<String> paramNames] 
	:	propName=ID { $name = $propName.text; }
		('(' paramList=idList ')' { $paramNames = $paramList.ids; })? ;

	
propertyExpression[String name, List<String> namedParams, List<String> context] returns [LP property, List<Integer> usedParams]
	:	
		propertyExpr=contextDependentPE[name, namedParams, context] { $property = $propertyExpr.property; $usedParams = $propertyExpr.usedParams; } |
		propertyExprI=contextIndependentPE[name, namedParams] { $property = $propertyExprI.property; $usedParams = new ArrayList<Integer>(); }			
	;

contextDependentPE[String name, List<String> namedParams, List<String> context] returns [LP property, List<Integer> usedParams]
	:	joinDef=joinPropertyDefinition[name, namedParams, context] { $property = $joinDef.property; $usedParams = $joinDef.usedParams; } | 
		unionDef=unionPropertyDefinition[name, namedParams, context] { $property = $unionDef.property; $usedParams = $unionDef.usedParams; } |
		constDef=literal[name] { $property = $constDef.property; $usedParams = new ArrayList<Integer>(); } 	
	;
	
contextIndependentPE[String name, List<String> namedParams] returns [LP property]
	: 	dataDef=dataPropertyDefinition[name, namedParams] { $property = $dataDef.property; } | 
		groupDef=groupPropertyDefinition[name, namedParams] { $property = $groupDef.property; }  
//		typedef=typeExpression[name, namedParams, context]
	;	

joinPropertyDefinition[String name, List<String> namedParams, List<String> context] returns [LP property, List<Integer> usedParams] 
@init {
	List<LP<?>> paramProps = new ArrayList<LP<?>>();
	List<List<Integer>> usedSubParams = new ArrayList<List<Integer>>();
	LP mainProp = null;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) {
		ScriptingLogicsModule.LPWithParams result = self.addScriptedJProp(name, "", mainProp, namedParams, paramProps, usedSubParams);
		$property = result.property;
		$usedParams = result.usedParams;
	}
}
	:	mainPropObj=propertyObject { mainProp = $mainPropObj.property; }
		'(' 
			(firstParam=propertyParam[context] { paramProps.add($firstParam.property); usedSubParams.add($firstParam.usedParams); }
			(',' nextParam=propertyParam[context] { paramProps.add($nextParam.property); usedSubParams.add($nextParam.usedParams);})* )?	
		')'; 




groupPropertyDefinition[String name, List<String> namedParams] returns [LP property, List<Integer> usedParams]
@init {
	List<LP<?>> paramProps = new ArrayList<LP<?>>();
	List<List<Integer>> usedParams = new ArrayList<List<Integer>>();
	LP<?> groupProp = null;
	String groupPropName;
	boolean isSGProp = true; 
	List<String> groupContext = null;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) {
		$property = self.addScriptedGProp(name, "", groupProp, isSGProp, namedParams, paramProps, usedParams);
	}
}
	:	'GROUP' (('SUM') { isSGProp = true; } | ('MAX') { isSGProp = false; }) 
		prop=propertyObject { groupProp = $prop.property; groupPropName = $prop.propName; }
		'BY'
		{ 
			if (groupPropName != null && parseState == ScriptingLogicsModule.State.PROP) 
				groupContext = self.getNamedParamsList(groupPropName); 
		}
		(firstParam=propertyParam[groupContext] { paramProps.add($firstParam.property); usedParams.add($firstParam.usedParams); }
		(',' nextParam=propertyParam[groupContext] { paramProps.add($nextParam.property); usedParams.add($nextParam.usedParams);})* )	
		;
		
		
dataPropertyDefinition[String name, List<String> namedParams] returns [LP property]
@init {
	List<String> paramClassNames;
	String returnClass = null;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) {
		$property = self.addScriptedDProp(name, "", returnClass, paramClassNames, namedParams);
	}
}
	:	'DATA'
		retClass=classId { returnClass = $retClass.text; }
		'(' 
			classIds=classIdList { paramClassNames = $classIds.ids; }
		')'; 




unionPropertyDefinition[String name, List<String> namedParams, List<String> context] returns [LP property, List<Integer> usedParams]
@init {
	List<LP<?>> paramProps = new ArrayList<LP<?>>();
	List<List<Integer>> usedParams = new ArrayList<List<Integer>>();
	Union type = null;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) { 
		$property = self.addScriptedUProp(name, "", type, namedParams, paramProps, usedParams);	
	}
}
	:	'UNION'
		(('MAX' {type = Union.MAX;}) | ('SUM' {type = Union.SUM;}) | ('OVERRIDE' {type = Union.OVERRIDE;}) | ('XOR' { type = Union.XOR;}) | ('EXCLUSIVE' {type = Union.EXCLUSIVE;}))
		'('
		firstParam=contextDependentPE[null, null, context] { paramProps.add($firstParam.property); usedParams.add($firstParam.usedParams); }
		(',' nextParam=contextDependentPE[null, null, context] { paramProps.add($nextParam.property); usedParams.add($nextParam.usedParams);})* 
		')';	



//propertyCompositionParam returns [Object param, List<String> paramNames]
//	:	singleParam=parameter { $param = $singleParam.text; } | 	
//		mappedProperty=propertyWithMapping { $param = $mappedProperty.property; $paramNames = $mappedProperty.paramNames; };


//propertyWithMapping returns [Object property, List<String> paramNames] : 
//		(propertyObj=propertyObject { $property = $propertyObj.property; }
//		'('
//		paramList=parameterList { $paramNames = $paramList.ids; }		
//		')') |
//		constant=literal { $property = $constant.property; $paramNames = new ArrayList<String>(); } |
//		expr=typeExpression { $property = $expr.property; $paramNames = Arrays.asList($expr.param); };


propertyParam[List<String> context] returns [LP property, List<Integer> usedParams]
	:	(name=parameter { $usedParams = Collections.singletonList(self.getParamIndex($name.text, $context)); }) | 
		(expr=contextDependentPE[null, null, context] { $property = $expr.property; $usedParams = $expr.usedParams; }) //|
//		('(' expr=contextIndependentPE[null, null] ')' '(' ')')
	;


//typeExpression[String name, List<String> namedParams, List<String> context] returns [LP property, Integer param] 
//@init {
//	String clsId = null;
//	boolean bIs = false;
//}
//@after {
//	if (parseState == ScriptingLogicsModule.State.PROP) { 
//		$property = self.addScriptedTypeProp(name, clsId, bIs, namedParams);
//	}
//}
//	:	paramName=parameter { $param = $paramName.text; }
//		('IS' { bIs = true; } | 'IF')  
//		id=classId { clsId = $id.text; };


propertyObject returns [LP property, String propName]
	:	name=compoundID	{ $property = self.getLPByName($name.text); $propName = $name.text; } |
		'(' expr=propertyExpression[null, null, null] ')' { $property = $expr.property; };	


commonPropertySettings[LP property] 
@init {
	String groupName = null;
	boolean isPersistent = false;	
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) { 
		self.addSettingsToProperty(property, groupName, isPersistent);	
	}
} 
	: 	('IN' name=compoundID { groupName = $name.text; })?
		('PERSISTENT' { isPersistent = true; })?;



////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// TABLE STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

tableStatement 
	:	't';


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// INDEX STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

indexStatement
	:	'z';


////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////// COMMON /////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

parameter 
	:	ID | NUMBERED_PARAM;

	
idList returns [List<String> ids] 
@init {
	ids = new ArrayList<String>();	
} 
	: (neIdList=nonEmptyIdList { ids = $neIdList.ids; })?;

classIdList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:	((firstClassName=classId { ids.add($firstClassName.text); })
		(',' className=classId { ids.add($className.text); })*)?;

compoundIdList returns [List<String> ids] 
@init {
	ids = new ArrayList<String>();	
} 
	: (neIdList=nonEmptyCompoundIdList { ids = $neIdList.ids; })?;

nonEmptyIdList returns [List<String> ids]
@init {
	ids = new ArrayList<String>(); 
}
	:	firstId=ID	{ $ids.add($firstId.text); }
		(',' nextId=ID	{ $ids.add($nextId.text); })*;

nonEmptyCompoundIdList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:	firstId=compoundID	{ $ids.add($firstId.text); }
		(',' nextId=compoundID	{ $ids.add($nextId.text); })*;

parameterList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:	(firstParam=parameter	 { $ids.add($firstParam.text); }
		(',' nextParam=parameter { $ids.add($nextParam.text); })* )?;


literal[String name] returns [LP property]
@init {
	ScriptingLogicsModule.ConstType cls = null;
	String text = null;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) { 
		$property = self.addConstantProp(name, cls, text);	
	}
}
	: 	strInt=intLiteral	{ cls = ScriptingLogicsModule.ConstType.INT; text = $strInt.text; } | 
		strReal=doubleLiteral	{ cls = ScriptingLogicsModule.ConstType.REAL; text = $strReal.text; } |
		str=STRING_LITERAL	{ cls = ScriptingLogicsModule.ConstType.STRING; text = $str.text; } | 
		str=LOGICAL_LITERAL	{ cls = ScriptingLogicsModule.ConstType.LOGICAL; text = $str.text; };
	
classId 
	:	compoundID | PRIMITIVE_TYPE;

compoundID
	:	(ID '.')? ID;
	
doubleLiteral 
	:	'-'? POSITIVE_DOUBLE_LITERAL; 
		

intLiteral
	:	'-'? UINT_LITERAL;		



/////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////// LEXER //////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////
	
fragment NEWLINE	:   '\r'?'\n'; 
fragment SPACE		:   (' '|'\t');
fragment STR_LITERAL_CHAR	: '\\\'' | ~('\r'|'\n'|'\'');	 // overcomplicated due to bug in ANTLR Works
fragment DIGITS		:	('0'..'9')+;
	 
PRIMITIVE_TYPE  :	'INTEGER' | 'DOUBLE' | 'LONG' | 'BOOLEAN' | 'DATE' | 'STRING[' DIGITS ']' | 'ISTRING[' DIGITS ']';		
LOGICAL_LITERAL :	'TRUE' | 'FALSE';		
ID          	:	('a'..'z'|'A'..'Z')('a'..'z'|'A'..'Z'|'_'|'0'..'9')*;
WS		:	(NEWLINE | SPACE) { $channel=HIDDEN; }; 	
STRING_LITERAL	:	'\'' STR_LITERAL_CHAR* '\'';
COMMENTS	:	('//' .* '\n') { $channel=HIDDEN; };
UINT_LITERAL 	:	DIGITS;
POSITIVE_DOUBLE_LITERAL	: 	DIGITS '.' DIGITS;	  
NUMBERED_PARAM	:	'$' DIGITS;
