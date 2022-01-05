grammar MigrationScript;

@header {
	package lsfusion.server.language;
	import lsfusion.server.physics.exec.db.controller.manager.MigrationManager;
	import java.util.*;
	import org.antlr.runtime.BitSet;
}

@lexer::header { 
	package lsfusion.server.language;
}

@members {
	public MigrationManager self;
}



script
scope {
	String version;
}
    :	(	v=changeVersion { $script::version = $v.version; self.addMigrationVersion($v.version); }
    		'{'
    			statement*
    		'}'
    	)*
    	EOF
    ;

statement	
	:	propertyRename
	|	actionRename
 	|	classRename
	|	tableRename
	|	objectRename
	|	propertyDrawRename
	|	navigatorElementRename 
	;

propertyRename
@init {
	boolean stored = false;
	String newClasses = null;
}
@after {
	self.addPropertyCNChange($script::version, $oldName.sid, $oldSignature.result, $newName.sid, newClasses, stored);
}
	:	('STORED' { stored = true; })? 'PROPERTY'
		oldName=compoundID oldSignature=signature '->' newName=compoundID (newSignature=signature { newClasses = $newSignature.result; })?
	;

actionRename 
@init {
	String newClasses = null;
}
@after {
	self.addActionCNChange($script::version, $oldName.sid, $oldSignature.result, $newName.sid, newClasses);
}
	:	'ACTION' oldName=compoundID oldSignature=signature '->' newName=compoundID (newSignature=signature { newClasses = $newSignature.result; })?
	;
	
classRename
	:	'CLASS' r=sidRename { self.addClassSIDChange($script::version, $r.from, $r.to); }
	;
	
tableRename
	:	'TABLE' r=sidRename { self.addTableSIDChange($script::version, $r.from, $r.to); }
	;

navigatorElementRename
	:	'NAVIGATOR' r=sidRename { self.addNavigatorElementCNChange($script::version, $r.from, $r.to); }
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

propertyDrawRename
	:	'FORM' 'PROPERTY' oldName=formPropertyID '->' newName=formPropertyID { self.addPropertyDrawSIDChange($script::version, $oldName.sid, $newName.sid); }
	;

changeVersion returns [String version] 
	:	v=VERSION { $version = $v.text.substring(1); }
	;

signature returns [String result] 
@after {
	$result = $text;
}
	:	'[' (signatureItem (',' signatureItem)*)? ']'
	;
	
signatureItem 
	:	cs=resolveClassSet 
	|	'?'
	;	
	
resolveClassSet
	:	concatenateClassSet
	|	orObjectClassSet
	|	upClassSet
	|	singleClass
	;
	
concatenateClassSet
	:	'CONCAT(' resolveClassSetList ')'
	;

resolveClassSetList
	:	(resolveClassSet (',' resolveClassSet)*)?
	;

orObjectClassSet
	:	'{' (upClassSet | customClass) (',' customClassList)? '}'
	;

customClassList
	:	(customClass (',' customClass)*)?
	;

upClassSet
	:	'(' customClassList ')'
	;

singleClass
	:	customClass
	|	PRIMITIVE_TYPE
	;

	
customClass
	:	compoundID
	;

compoundID returns [String sid]
	:	namespacePart=ID '.' namePart=ID { $sid = $namespacePart.text + '.' + $namePart.text; }
	;
	
formPropertyID returns [String sid]
	:	namespacePart=ID '.' namePart=ID '.' propertyName=ID { $sid = $namespacePart.text + '.' + $namePart.text + '.' + $propertyName.text; }
		('(' ids=idList ')' { $sid = $sid + '(' + $ids.result + ')'; })?
	;	
	
staticObjectID returns [String sid]
	:	namespacePart=ID '.' classPart=ID '.' namePart=ID { $sid = $namespacePart.text + '.' + $classPart.text + '.' + $namePart.text; }
	;
	
idList returns [String result]
	:	{ $result = ""; }
		(first=ID { $result = $result + $first.text; } (',' next=ID { $result = $result + ',' + $next.text; })*)?
	;
	
/////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////// LEXER //////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////
	
fragment NEWLINE	:	'\r'?'\n'; 
fragment SPACE		:	(' '|'\t');
fragment FIRST_ID_LETTER: 	('a'..'z'|'A'..'Z'|'_');
fragment NEXT_ID_LETTER	: 	('a'..'z'|'A'..'Z'|'_'|'0'..'9');
fragment DIGIT		:	'0'..'9';
fragment DIGITS		:	('0'..'9')+;

PRIMITIVE_TYPE      :   'INTEGER' | 'DOUBLE' | 'LONG' | 'BOOLEAN' | 'DATETIME' | 'DATE' | 'YEAR' | 'TIME'
                    |   'WORDFILE' | 'IMAGEFILE' | 'PDFFILE' | 'DBFFILE' | 'RAWFILE' | 'FILE' | 'EXCELFILE' | 'TEXTFILE' | 'CSVFILE' | 'HTMLFILE' | 'JSONFILE' | 'XMLFILE' | 'TABLEFILE' | 'NAMEDFILE'
                    |   'WORDLINK' | 'IMAGELINK' | 'PDFLINK' | 'DBFLINK' | 'RAWLINK' | 'LINK' | 'EXCELLINK' | 'TEXTLINK' | 'CSVLINK' | 'HTMLLINK' | 'JSONLINK' | 'XMLLINK' | 'TABLELINK'
                    |   'STRING' | 'NUMERIC' | 'COLOR'
                    ;

VERSION     :	'V' DIGIT+ ('.' DIGIT+)*;
ID          :	FIRST_ID_LETTER NEXT_ID_LETTER*;
WS          :	(NEWLINE | SPACE) { $channel=HIDDEN; };
COMMENTS    :	('//' .* '\n') { $channel=HIDDEN; };
