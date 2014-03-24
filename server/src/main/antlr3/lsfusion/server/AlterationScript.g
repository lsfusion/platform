grammar AlterationScript;

@header {
	package lsfusion.server;
	import lsfusion.server.logics.DBManager;
	import java.util.*;
	import org.antlr.runtime.BitSet;
}

@lexer::header { 
	package lsfusion.server;
}

@members {
	public DBManager self;
}



script
scope {
	String version;
}
    :	(	v=changeVersion { $script::version = $v.version; }
    		'{'
    			statement*
    		'}'
    	)*
    	EOF
    ;

statement	
	:	propertyRename
 	|	classRename
	|	tableRename
	|	objectRename
	;
    
propertyRename
@init {
	List<String> newCls = null;
}
	:	'PROPERTY' 
		(	r=sidRename { self.addPropertySIDChange($script::version, $r.from, $r.to); }
		|	oldName=compoundID '[' oldClasses=classList ']' '->' newName=compoundID ('[' newClasses=classList ']' { newCls = $newClasses.classes; })?
			{ self.addPropertySIDChange($script::version, $oldName.sid, $oldClasses.classes, $newName.sid, newCls); }
		) 
	;
	
classRename
	:	'CLASS' r=sidRename { self.addClassSIDChange($script::version, $r.from, $r.to); }
	;
	
tableRename
	:	'TABLE' r=sidRename { self.addTableSIDChange($script::version, $r.from, $r.to); }
	;
	
objectRename
	:	'OBJECT' r=objectSidRename { self.addObjectSIDChange($script::version, $r.from, $r.to); }	
	;
	
sidRename returns [String from, String to]
	:	old=compoundID '->' newID=compoundID { $from = $old.sid; $to = $newID.sid; }
	;	

objectSidRename returns [String from, String to]
	:	old=staticObjectID '->' newID=staticObjectID	{ $from = $old.text; $to = $newID.text; }
	;

changeVersion returns [String version] 
	:	v=VERSION { $version = $v.text.substring(1); }
	;
	
className returns [String classID]
	:	id=compoundID { $classID = $id.sid; }
	|	type=PRIMITIVE_TYPE { $classID = $type.text; }
	;	

compoundID returns [String sid]
	:	{ $sid = ""; }
		(firstPart=ID '.' { $sid = $firstPart.text + '.'; })? secondPart=ID { $sid = $sid + $secondPart.text; }
	;
	
staticObjectID returns [String sid]
	:	(namespacePart=ID '.')? classPart=ID '.' namePart=ID { $sid = ($namespacePart != null ? $namespacePart.text + '.' : "") + $classPart.text + '.' + $namePart.text; }
	;
	
classList returns [List<String> classes]
@init {
	classes = new ArrayList<String>();
}
	:	(firstName=className	{ $classes.add($firstName.classID); }
		(',' nextName=className	{ $classes.add($nextName.classID); })*)?
	;
	
	
/////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////// LEXER //////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////
	
fragment NEWLINE	:	'\r'?'\n'; 
fragment SPACE		:	(' '|'\t');
fragment FIRST_ID_LETTER: 	('a'..'z'|'A'..'Z');
fragment NEXT_ID_LETTER	: 	('a'..'z'|'A'..'Z'|'_'|'0'..'9');
fragment DIGIT		:	'0'..'9';
fragment DIGITS		:	('0'..'9')+;


VERSION			:	'V' DIGIT+ ('.' DIGIT+)*;
ID          		:	FIRST_ID_LETTER NEXT_ID_LETTER*;
WS			:	(NEWLINE | SPACE) { $channel=HIDDEN; };
COMMENTS		:	('//' .* '\n') { $channel=HIDDEN; };
PRIMITIVE_TYPE  	:	'INTEGER' | 'DOUBLE' | 'LONG' | 'BOOLEAN' | 'DATE' | 'DATETIME' | 'YEAR' | 'TEXT' | 'TIME' | 'WORDFILE' | 'IMAGEFILE' | 'PDFFILE' | 'CUSTOMFILE' | 'EXCELFILE'
			| 	'STRING[' DIGITS ']' | 'ISTRING[' DIGITS ']'  | 'VARSTRING[' DIGITS ']' | 'VARISTRING[' DIGITS ']' | 'NUMERIC[' DIGITS ',' DIGITS ']' | 'COLOR'
			;
