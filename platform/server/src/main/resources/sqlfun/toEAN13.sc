CREATE OR REPLACE FUNCTION toEAN13(char(12))
  RETURNS char(13) AS
$BODY$
	DECLARE
		evenSum INTEGER := 0;
		oddSum INTEGER := 0;
		checkDigit INTEGER := 0;
	BEGIN
		FOR i IN 1..12 LOOP
			IF mod(i, 2) = 0 THEN
				evenSum = evenSum + CAST(substr($1, i, 1) as INTEGER);
			ELSE
				oddSum = oddSum + CAST(substr($1, i, 1) as INTEGER);
			END IF;
		END LOOP;
		IF mod(evenSum * 3 + oddSum, 10) != 0 THEN
			checkDigit = 10 - mod(evenSum * 3 + oddSum, 10);
		END IF;
		RETURN $1 || checkDigit;
	END;
$BODY$
  LANGUAGE plpgsql VOLATILE STRICT;