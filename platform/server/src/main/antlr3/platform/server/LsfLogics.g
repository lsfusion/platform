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
}

script	
	:	importStatement* statement*;

importStatement
@init {
	String name;
}
@after {
	System.out.print("import " + name + ";\n");
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
	System.out.print("addScriptedClass(" + name + ", " + (captionStr==null ? "" : captionStr) + ", " + isAbstract + ", " + classParents.toString() + ", " + importModules.toString() + ");\n");
	self.addScriptedClass(name, captionStr, isAbstract, classParents, importModules);
}
	:	isAbstract=classDeclarant className=ID 	{ name = $className.text; }
			(caption=STRING_LITERAL { captionStr = $caption.text; })?  
			':'  
			firstParentName=identificator 		{ classParents.add($firstParentName.text); }
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
	System.out.print("addScriptedGroup(" + name + ", " + (captionStr==null ? "" : captionStr) + ", " + (parent == null ? "null" : parent) + ", " + importModules.toString() + ");\n");
	self.addScriptedGroup(name, captionStr, parent, importModules);
}
	:	'group' groupName=ID { name = $groupName.text; }
			(caption=STRING_LITERAL { captionStr = $caption.text; })?  
			(':' parentName=identificator { parent = $parentName.text; })?;


propertyStatement
	:	'p';

	
tableStatement 
	:	't';


indexStatement
	:	'i';

identificator
	:	(ID '.')? ID;
	
fragment NEWLINE     :   '\r'?'\n';
fragment SPACE       :   (' '|'\t');
fragment STR_LITERAL_CHAR	: ~('\r'|'\n'|'\'');	 
fragment ESCAPE 	: '\\\'';
	 
ID          	:	('a'..'z'|'A'..'Z')('a'..'z'|'A'..'Z'|'_'|'0'..'9')*;
WS		:	NEWLINE | SPACE { $channel=HIDDEN; }; 	
STRING_LITERAL	:	'\'' (ESCAPE | STR_LITERAL_CHAR)* '\'';

