grammar LsfLogics;

@header { 
	package platform.server; 
	import platform.server.logics.ScriptingLogicsModule; 
	import java.util.Set;
	import java.util.HashSet;
	import java.lang.System;
}

@lexer::header { 
	package platform.server; 
}

@members { 
	private Set<String> importModules = new HashSet<String>();
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
		System.out.print("import " + name + ";\n");
	}
}
	:	'import' moduleName=ID ';' { name = $moduleName.text; importModules.add(name); };


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
		System.out.print("addScriptedClass(" + name + ", " + (captionStr==null ? "" : captionStr) + ", " + isAbstract + ", " + classParents + ", " + importModules + ");\n");
		self.addScriptedClass(name, captionStr, isAbstract, classParents, importModules);
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
		System.out.print("addScriptedGroup(" + name + ", " + (captionStr==null ? "" : captionStr) + ", " + (parent == null ? "null" : parent) + ", " + importModules + ");\n");
		self.addScriptedGroup(name, captionStr, parent, importModules);
	}
}
	:	'group' groupName=ID { name = $groupName.text; }
			(caption=STRING_LITERAL { captionStr = $caption.text; })?  
			(':' parentName=compoundID { parent = $parentName.text; })?;


propertyStatement
	:	dataPropertyStatement | joinPropertyStatement;
	
	

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
		System.out.print("addScriptedJProp(" + name + ", " + (groupName == null ? "" : groupName) + ", " + mainProp + ", " + isPersistent + ", " + namedParams + ", " + paramIds + ", " + propParams + ", " + importModules + ");\n");
		self.addScriptedJProp(name, "", groupName, mainProp, isPersistent, namedParams, paramIds, propParams, importModules);
	}
}
	:	propertyName=ID { name = $propertyName.text; }
		'('
			paramList=idList { namedParams = $paramList.ids; }
		')' 
		'='
		mainPropName=compoundID { mainProp = $mainPropName.text; }
		'(' 
			(firstParam=jpropertyParam { paramIds.add($firstParam.paramID); propParams.add($firstParam.paramNames); }
			(',' nextParam=jpropertyParam { paramIds.add($nextParam.paramID); propParams.add($nextParam.paramNames);})* )?	
		')' 
		settings=propertyCommonSettings { groupName = $settings.group; isPersistent = $settings.isPersistent; }; 
	

jpropertyParam returns [String paramID, List<String> paramNames]
	:	singleParam=ID { $paramID = $singleParam.text; } | 	
		(pID=compoundID { $paramID = $pID.text; }
		'('
		paramList=compoundIdList { $paramNames = $paramList.ids; }		
		')');


dataPropertyStatement 
@init {
	List<String> paramClassNames = new ArrayList<String>();
	String returnClass = null;
	String groupName = null;
	boolean isPersistent = false;
	String name;
}
@after {
	if (parseState == ScriptingLogicsModule.State.PROP) {
		System.out.print("addScriptedDProp(" + name + ", " + (groupName == null ? "" : groupName) + ", " + returnClass + ", " + paramClassNames + ", " + isPersistent + ", " + importModules + ");\n");
		self.addScriptedDProp(name, "", groupName, returnClass, paramClassNames, isPersistent, importModules);
	}
}
	:	propertyName=ID { name = $propertyName.text; }
		'='
		retClass=classId { returnClass = $retClass.text; }
		'(' 
			((firstClassName=classId { paramClassNames.add($firstClassName.text); })
			(',' className=classId { paramClassNames.add($className.text); })*)?
		')' 
		settings=propertyCommonSettings { groupName = $settings.group; isPersistent = $settings.isPersistent; };

propertyCommonSettings returns [String group, boolean isPersistent] 
	: 	('in' groupName=compoundID { $group = $groupName.text; })?
		('persistent' { $isPersistent = true; })?;

tableStatement 
	:	't';


indexStatement
	:	'z';



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
