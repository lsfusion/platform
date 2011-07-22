grammar LsfLogics;

@header { 
	package platform.server; 
	import platform.server.logics.ScriptingLogicsModule; 
	import platform.server.data.Union;
	import platform.server.logics.linear.LP;
	import java.util.Set;
	import java.util.HashSet;
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
	:	(classStatement | groupStatement | propertyStatement | tableStatement | indexStatement) ';';


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
//////////////////////////////// PROPERTY STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

propertyStatement
	:	declaration=propertyDeclaration 
		'=' 
		propertyDefinition[declaration.name, declaration.paramNames];

	
propertyDeclaration returns [String name, List<String> paramNames] 
	:	propertyName=ID { $name = $propertyName.text; }
		('(' paramList=idList ')' { $paramNames = $paramList.ids; })? ;
	

propertyDefinition[String name, List<String> namedParams] returns [LP property]
	:	def=dataPropertyDefinition[name, namedParams] { $property = $def.property; } | 
		def=joinPropertyDefinition[name, namedParams] { $property = $def.property; } | 
		def=groupPropertyDefinition[name, namedParams] { $property = $def.property; }| 
		def=unionPropertyDefinition[name, namedParams] { $property = $def.property; };
	

joinPropertyDefinition[String name, List<String> namedParams] returns [LP property]
@init {
	List<Object> params = new ArrayList<Object>();
	List<List<String>> mappings = new ArrayList<List<String>>();
	Object mainProp = null;
	String groupName = null;
	boolean isPersistent = false;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) {
		property = self.addScriptedJProp(name, "", groupName, mainProp, isPersistent, namedParams, params, mappings);
	}
}
	:	mainPropObj=propertyObject { mainProp = $mainPropObj.property; }
		'(' 
			(firstParam=propertyCompositionParam { params.add($firstParam.param); mappings.add($firstParam.paramNames); }
			(',' nextParam=propertyCompositionParam { params.add($nextParam.param); mappings.add($nextParam.paramNames);})* )?	
		')' 
		settings=commonPropertySettings { groupName = $settings.group; isPersistent = $settings.isPersistent; }; 
	

dataPropertyDefinition[String name, List<String> namedParams] returns [LP property]
@init {
	List<String> paramClassNames = new ArrayList<String>();
	String returnClass = null;
	String groupName = null;
	boolean isPersistent = false;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) {
		property = self.addScriptedDProp(name, "", groupName, returnClass, paramClassNames, isPersistent, namedParams);
	}
}
	:	'DATA'
		retClass=classId { returnClass = $retClass.text; }
		'(' 
			((firstClassName=classId { paramClassNames.add($firstClassName.text); })
			(',' className=classId { paramClassNames.add($className.text); })*)?
		')' 
		settings=commonPropertySettings { groupName = $settings.group; isPersistent = $settings.isPersistent; };


groupPropertyDefinition[String name, List<String> namedParams] returns [LP property]
@init {
	List<Object> params = new ArrayList<Object>();
	List<List<String>> mappings = new ArrayList<List<String>>();
	Object groupProp = null;
	String groupName = null;
	boolean isPersistent = false;
	boolean isSGProp = true; 
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) {
		property = self.addScriptedGProp(name, "", groupName, groupProp, isPersistent, isSGProp, namedParams, params, mappings);
	}
}
	:	(('SGROUP') { isSGProp = true; } | ('MGROUP') { isSGProp = false;}) 
		groupPropName=propertyObject { groupProp = $groupPropName.text; }
		'BY'
		(firstParam=propertyCompositionParam { params.add($firstParam.param); mappings.add($firstParam.paramNames); }
		(',' nextParam=propertyCompositionParam { params.add($nextParam.param); mappings.add($nextParam.paramNames);})* )?	
		settings=commonPropertySettings { groupName = $settings.group; isPersistent = $settings.isPersistent; }; 


unionPropertyDefinition[String name, List<String> namedParams] returns [LP property]
@init {
	List<Object> params = new ArrayList<Object>();
	List<List<String>> mappings = new ArrayList<List<String>>();
	String groupName = null;
	boolean isPersistent = false;
	Union type = null;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) { 
		property = self.addScriptedUProp(name, "", groupName, isPersistent, type, namedParams, params, mappings);	
	}
}
	:	'UNION'
		(('MAX' {type = Union.MAX;}) | ('SUM' {type = Union.SUM;}) | ('OVERRIDE' {type = Union.OVERRIDE;}) | ('XOR' { type = Union.XOR;}) | ('EXCLUSIVE' {type = Union.EXCLUSIVE;}))
		firstParam=propertyWithMapping { params.add($firstParam.property); mappings.add($firstParam.paramNames); }
		(',' nextParam=propertyWithMapping { params.add($nextParam.property); mappings.add($nextParam.paramNames);})* 	
		settings=commonPropertySettings { groupName = $settings.group; isPersistent = $settings.isPersistent; }; 



propertyCompositionParam returns [Object param, List<String> paramNames]
	:	singleParam=parameter { $param = $singleParam.text; } | 	
		mappedProperty=propertyWithMapping { $param = $mappedProperty.property; $paramNames = $mappedProperty.paramNames; };


propertyWithMapping returns [Object property, List<String> paramNames] : 
		(propertyObj=propertyObject { $property = $propertyObj.property; }
		'('
		paramList=parameterList { $paramNames = $paramList.ids; }		
		')');


propertyObject returns [Object property]
	:	name=compoundID { $property = $name.text; } | 
		expr=propertyExpression { $property = $expr.property; };

propertyExpression returns [LP property]
	:	'(' def=propertyDefinition[null, new ArrayList<String>()] ')' { $property = $def.property; };


commonPropertySettings returns [String group, boolean isPersistent] 
	: 	('IN' groupName=compoundID { $group = $groupName.text; })?
		('PERSISTENT' { $isPersistent = true; })?;



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

	
classId 
	:	compoundID | PRIMITIVE_TYPE;

compoundID
	:	(ID '.')? ID;

intLiteral
	:	'-'? UINT_LITERAL;		



/////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////// LEXER //////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////
	
fragment NEWLINE     :   '\r'?'\n'; 
fragment SPACE       :   (' '|'\t');
fragment STR_LITERAL_CHAR	: '\\\'' | ~('\r'|'\n'|'\'');	 // overcomplicated due to bug in ANTLR Works
fragment DIGITS		:	('0'..'9')+;
	 
PRIMITIVE_TYPE  :	'Integer' | 'Double' | 'Long' | 'Boolean' | 'Date' | 'String[' DIGITS ']' | 'InsensitiveString[' DIGITS ']';		
ID          	:	('a'..'z'|'A'..'Z')('a'..'z'|'A'..'Z'|'_'|'0'..'9')*;
WS		:	(NEWLINE | SPACE) { $channel=HIDDEN; }; 	
STRING_LITERAL	:	'\'' STR_LITERAL_CHAR* '\'';
COMMENTS	:	('//' .* '\n') { $channel=HIDDEN; };
UINT_LITERAL 	:	DIGITS;	 
NUMBERED_PARAM	:	'$' DIGITS;