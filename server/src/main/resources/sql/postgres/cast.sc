1CREATE OR REPLACE FUNCTION casttocustomfile(file bytea, ext VARCHAR) RETURNS bytea AS 
$$
BEGIN
	if ((ext = 'doc' or ext = 'xls') and (length(file) > 3) and (get_byte(file, 0) = 80) and (get_byte(file, 1) = 75) and (get_byte(file, 2) = 3) and (get_byte(file, 3) = 4)) then
		ext = ext || 'x';
	end if;

	RETURN chr(octet_length(ext))::bytea || convert_to(ext, 'UTF-8') || file;
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;


CREATE OR REPLACE FUNCTION castfromcustomfile(file bytea) RETURNS bytea AS 
$$
BEGIN
	RETURN substring(file, (get_byte(file, 0) + 2)); -- index in substring is 1-based
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;