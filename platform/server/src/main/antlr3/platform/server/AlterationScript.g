grammar AlterationScript;

@header {
	package platform.server;
	import platform.server.logics.DBManager;
	import java.util.*;
	import org.antlr.runtime.BitSet;
}

@lexer::header { 
	package platform.server; 
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
	;
    
propertyRename
	:	'PROPERTY' r=sidRename { self.addPropertySIDChange($script::version, $r.from, $r.to); }
	;
	
classRename
	:	'CLASS' r=sidRename { self.addClassSIDChange($script::version, $r.from, $r.to); }
	;
	
tableRename
	:	'TABLE' r=sidRename { self.addTableSIDChange($script::version, $r.from, $r.to); }
	;
	
sidRename returns [String from, String to]
	:	old=ID '->' newID=ID	{ $from = $old.text; $to = $newID.text; }
	;	

changeVersion returns [String version] 
	:	v=VERSION { $version = $v.text.substring(1); }
	;
	
/////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////// LEXER //////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////
	
fragment NEWLINE	:	'\r'?'\n'; 
fragment SPACE		:	(' '|'\t');
fragment FIRST_ID_LETTER	: ('a'..'z'|'A'..'Z');
fragment NEXT_ID_LETTER		: ('a'..'z'|'A'..'Z'|'_'|'0'..'9');
fragment DIGIT		:	'0'..'9';

ID          	:	FIRST_ID_LETTER NEXT_ID_LETTER*;
WS				:	(NEWLINE | SPACE) { $channel=HIDDEN; };
COMMENTS		:	('//' .* '\n') { $channel=HIDDEN; };
VERSION			:	'V' DIGIT+ ('.' DIGIT+)*; 			    