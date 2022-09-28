CREATE OR REPLACE FUNCTION cast_static_file_to_dynamic_file(file bytea, ext VARCHAR) RETURNS bytea AS
$$
BEGIN
    ext = COALESCE(ext, 'dat');
	if ((ext = 'doc' or ext = 'xls') and (length(file) > 3) and (get_byte(file, 0) = 80) and (get_byte(file, 1) = 75) and (get_byte(file, 2) = 3) and (get_byte(file, 3) = 4)) then
		ext = ext || 'x';
	end if;

	if ((ext = 'jpg') and (length(file) > 1)) then
		if (get_byte(file, 0) = 137 and get_byte(file, 1) = 80) then
		    ext = 'png';
        end if;
        if (get_byte(file, 0) = 66 and get_byte(file, 1) = 77) then
            ext = 'bmp';
        end if;
    end if;

	RETURN chr(octet_length(ext))::bytea || convert_to(ext, 'UTF-8') || file;
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;

CREATE OR REPLACE FUNCTION cast_dynamic_file_to_static_file(file bytea) RETURNS bytea AS
$$
BEGIN
	RETURN substring(file, (get_byte(file, 0) + 2)); -- index in substring is 1-based
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;

CREATE OR REPLACE FUNCTION cast_json_to_dynamic_file(json jsonb) RETURNS bytea AS
$$
BEGIN
	RETURN chr(octet_length('json'))::bytea || convert_to('json', 'UTF-8') || convert_to(json::text,'UTF-8');
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;

CREATE OR REPLACE FUNCTION cast_dynamic_file_to_json(file bytea) RETURNS jsonb AS
$$
BEGIN
	RETURN convert_from(substring(file, (get_byte(file, 0) + 2)),'UTF-8')::jsonb; -- index in substring is 1-based
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;

CREATE OR REPLACE FUNCTION cast_json_to_static_file(json jsonb) RETURNS bytea AS
$$
BEGIN
	RETURN convert_to(json::text,'UTF-8');
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;

CREATE OR REPLACE FUNCTION cast_static_file_to_json(file bytea) RETURNS jsonb AS
$$
BEGIN
	RETURN convert_from(file,'UTF-8')::jsonb;
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;

CREATE OR REPLACE FUNCTION cast_file_to_string(file bytea) RETURNS VARCHAR AS
$$
BEGIN
	RETURN convert_from(file, 'UTF-8');
EXCEPTION
  WHEN OTHERS THEN
    RETURN NULL;
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;

CREATE OR REPLACE FUNCTION cast_string_to_file(string VARCHAR) RETURNS bytea AS 
$$
BEGIN
	RETURN convert_to(string, 'UTF-8');
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;

CREATE OR REPLACE FUNCTION get_extension(file bytea) RETURNS VARCHAR AS 
$$
BEGIN
	RETURN convert_from(substring(file, 2, get_byte(file, 0)), 'UTF-8');  -- index in substring is 1-based
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;

CREATE OR REPLACE FUNCTION get_named_file_name(file bytea) RETURNS VARCHAR AS
$$
BEGIN
RETURN convert_from(substring(file, 2, get_byte(file, 0)), 'UTF-8');
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;

CREATE OR REPLACE FUNCTION get_named_file_extension(file bytea) RETURNS VARCHAR AS
$$
DECLARE
extLengthPosition INTEGER;
BEGIN
extLengthPosition = get_byte(file, 0) + 2;
RETURN convert_from(substring(file, extLengthPosition + 1, get_byte(file, extLengthPosition - 1)), 'UTF-8');
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;

CREATE OR REPLACE FUNCTION cast_named_file_to_dynamic_file(file bytea) RETURNS bytea AS
$$
BEGIN
RETURN substring(file, (get_byte(file, 0) + 2));
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;

CREATE OR REPLACE FUNCTION cast_dynamic_file_to_named_file(file bytea, name VARCHAR) RETURNS bytea AS
$$
BEGIN
name = COALESCE(name, 'file');
RETURN chr(length(name::bytea))::bytea || name::bytea || file;
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;

CREATE OR REPLACE FUNCTION cast_named_file_to_static_file(file bytea) RETURNS bytea AS
$$
DECLARE
extLengthPosition INTEGER;
BEGIN
extLengthPosition = get_byte(file, 0) + 2;
RETURN substring(file, extLengthPosition + get_byte(file, extLengthPosition - 1) + 1);
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;

CREATE OR REPLACE FUNCTION cast_static_file_to_named_file(file bytea, name varchar, ext varchar) RETURNS bytea AS
$$
BEGIN
name = COALESCE(name, 'file');
ext = COALESCE(ext, 'dat');
RETURN chr(length(name::bytea))::bytea || name::bytea || chr(length(ext::bytea))::bytea || ext::bytea || file;
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;