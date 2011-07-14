grammar LsfLogics;

@header { 
	package platform.server; 
	import platform.server.logics.ScriptingLogicsModule; 
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
	:	'import' moduleName=ID ';' { name = $moduleName.text; };


statement
	:	(classStatement | groupStatement | propertyStatement | tableStatement | indexStatement) ';';


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
	:	'class' { $isAbstract = false; } |
		'class' 'abstract' { $isAbstract = true; }; 



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
	:	'group' groupName=ID { name = $groupName.text; }
			(caption=STRING_LITERAL { captionStr = $caption.text; })?  
			(':' parentName=compoundID { parent = $parentName.text; })?;


propertyStatement
	:	dataPropertyStatement | joinPropertyStatement | groupPropertyStatement;
	

propertyDeclaration returns [String name, List<String> paramNames] 
	:	propertyName=ID { $name = $propertyName.text; }
		('(' paramList=idList ')' { $paramNames = $paramList.ids; })? ;
	

joinPropertyStatement 
@init {
	List<String> namedParams = new ArrayList<String>();
	List<String> paramIds = new ArrayList<String>();
	List<List<String>> propParams = new ArrayList<List<String>>();
	String mainProp = null;
	String groupName = null;
	boolean isPersistent = false;
	String name;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) {
		self.addScriptedJProp(name, "", groupName, mainProp, isPersistent, namedParams, paramIds, propParams);
	}
}
	:	declaration=propertyDeclaration { name = $declaration.name; namedParams = $declaration.paramNames; }
		'=' 
		mainPropName=compoundID { mainProp = $mainPropName.text; }
		'(' 
			(firstParam=propertyParam { paramIds.add($firstParam.paramID); propParams.add($firstParam.paramNames); }
			(',' nextParam=propertyParam { paramIds.add($nextParam.paramID); propParams.add($nextParam.paramNames);})* )?	
		')' 
		settings=propertyCommonSettings { groupName = $settings.group; isPersistent = $settings.isPersistent; }; 
	

propertyParam returns [String paramID, List<String> paramNames]
	:	singleParam=parameter { $paramID = $singleParam.text; } | 	
		(pID=compoundID { $paramID = $pID.text; }
		'('
		paramList=parameterList { $paramNames = $paramList.ids; }		
		')');


dataPropertyStatement 
@init {
	List<String> paramClassNames = new ArrayList<String>();
	List<String> namedParams;
	String returnClass = null;
	String groupName = null;
	boolean isPersistent = false;
	String name;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) {
		self.addScriptedDProp(name, "", groupName, returnClass, paramClassNames, isPersistent, namedParams);
	}
}
	:	declaration=propertyDeclaration { name = $declaration.name; namedParams = $declaration.paramNames; }
		'=' 'data'
		retClass=classId { returnClass = $retClass.text; }
		'(' 
			((firstClassName=classId { paramClassNames.add($firstClassName.text); })
			(',' className=classId { paramClassNames.add($className.text); })*)?
		')' 
		settings=propertyCommonSettings { groupName = $settings.group; isPersistent = $settings.isPersistent; };


groupPropertyStatement 
@init {
	List<String> namedParams = new ArrayList<String>();
	List<String> paramIds = new ArrayList<String>();
	List<List<String>> propParams = new ArrayList<List<String>>();
	String groupProp = null;
	String groupName = null;
	boolean isPersistent = false;
	boolean isSGProp = true; 
	String name;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) {
		self.addScriptedGProp(name, "", groupName, groupProp, isPersistent, isSGProp, namedParams, paramIds, propParams);
	}
}
	:	declaration=propertyDeclaration { name = $declaration.name; namedParams = $declaration.paramNames; }
		'=' 
		groupPropName=compoundID { groupProp = $groupPropName.text; }
		(('sgroup') { isSGProp = true; } | 
		 ('mgroup') { isSGProp = false;}) 
		'by'
		(firstParam=propertyParam { paramIds.add($firstParam.paramID); propParams.add($firstParam.paramNames); }
		(',' nextParam=propertyParam { paramIds.add($nextParam.paramID); propParams.add($nextParam.paramNames);})* )?	
		settings=propertyCommonSettings { groupName = $settings.group; isPersistent = $settings.isPersistent; }; 


propertyCommonSettings returns [String group, boolean isPersistent] 
	: 	('in' groupName=compoundID { $group = $groupName.text; })?
		('persistent' { $isPersistent = true; })?;

tableStatement 
	:	't';


indexStatement
	:	'z';


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