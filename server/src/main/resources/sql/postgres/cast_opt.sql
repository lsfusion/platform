-- These 2 functions moved to separate _opt file in 5.2 because they are renamed in 6.0.
-- This is need for backward compatibility to start base both with 5.2 and 6.0.

CREATE OR REPLACE FUNCTION cast_json_to_static_file(json jsonb) RETURNS bytea AS
$$
BEGIN
	RETURN convert_to(json::text,'UTF-8');
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;

CREATE OR REPLACE FUNCTION cast_json_text_to_static_file(json json) RETURNS bytea AS
$$
BEGIN
RETURN convert_to(json::text,'UTF-8');
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;