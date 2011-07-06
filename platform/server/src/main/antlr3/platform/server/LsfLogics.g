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
	:	importStatement* statement*;


importStatement
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
	List<String> classParents = new ArrayList<String>();
	String name; 
	String captionStr = null;
}
@after {
	if (parseState == ScriptingLogicsModule.State.CLASS) {
		System.out.print("addScriptedClass(" + name + ", " + (captionStr==null ? "" : captionStr) + ", " + isAbstract + ", " + classParents.toString() + ", " + importModules.toString() + ");\n");
		self.addScriptedClass(name, captionStr, isAbstract, classParents, importModules);
	}
}
	:	isAbstract=classDeclarant className=ID 	{ name = $className.text; }
			(caption=STRING_LITERAL { captionStr = $caption.text; })?  
			':'  
			firstParentName=identificator 	{ classParents.add($firstParentName.text); }
			(',' parentName=identificator 	{ classParents.add($parentName.text); })*;

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
		System.out.print("addScriptedGroup(" + name + ", " + (captionStr==null ? "" : captionStr) + ", " + (parent == null ? "null" : parent) + ", " + importModules.toString() + ");\n");
		self.addScriptedGroup(name, captionStr, parent, importModules);
	}
}
	:	'group' groupName=ID { name = $groupName.text; }
			(caption=STRING_LITERAL { captionStr = $caption.text; })?  
			(':' parentName=identificator { parent = $parentName.text; })?;


propertyStatement
	:	dataPropertyStatement | joinPropertyStatement;
	
	

joinPropertyStatement 
	:	'j';
	
	
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
		System.out.print("addScriptedDProp(" + name + ", " + returnClass + ", " + paramClassNames + ", " + (groupName == null ? "" : groupName) + ", " + isPersistent + ", " + importModules + ");\n");
		self.addScriptedDProp(name, "", paramClassNames, returnClass, isPersistent, groupName, importModules);
	}
}
	:	propertyName=ID { name = $propertyName.text; }
		'='
		retClass=classId { returnClass = $retClass.text; }
		'(' 
			(firstClassName=classId { paramClassNames.add($firstClassName.text); })
			(',' className=classId { paramClassNames.add($className.text); })*	
		')' 
		settings=propertyCommonSettings { groupName = $settings.group; isPersistent = $settings.isPersistent; }; 

propertyCommonSettings returns [String group, boolean isPersistent] 
	: 	('in' groupName=identificator { $group = $groupName.text; })?
		('persistent' { $isPersistent = true; })?;

tableStatement 
	:	't';


indexStatement
	:	'z';
	
positiveIntLiteral 
	:	DIGITS;
	
intLiteral
	:	'-'? positiveIntLiteral;		

insensitiveStringClassId 
	:	'InsensitiveString[' positiveIntLiteral ']';

stringClassId 
	:	'String[' positiveIntLiteral ']';

classId 
	:	identificator | PRIMITIVE_TYPE | stringClassId | insensitiveStringClassId;

identificator
	:	(ID '.')? ID;
	
fragment NEWLINE     :   '\r'?'\n';
fragment SPACE       :   (' '|'\t');
fragment STR_LITERAL_CHAR	: '\\\'' | ~('\r'|'\n'|'\'');	 
	 
PRIMITIVE_TYPE  :	'Integer' | 'Double' | 'Long' | 'Boolean' | 'Date';		
ID          	:	('a'..'z'|'A'..'Z')('a'..'z'|'A'..'Z'|'_'|'0'..'9')*;
WS		:	(NEWLINE | SPACE) { $channel=HIDDEN; }; 	
STRING_LITERAL	:	'\'' STR_LITERAL_CHAR* '\'';
COMMENTS	:	('//' .* '\n') { $channel=HIDDEN; };
DIGITS		:	('0'..'9')+;
