-- прибавляет к дате n рабочих дней

--1 - country, 2 - date, 3 - days to jump

CREATE OR REPLACE FUNCTION jumpworkdays(integer, date, integer)
  RETURNS date AS
$BODY$
	DECLARE
		quantity INTEGER := @$3;
		bufferSize INTEGER := CASE WHEN quantity > 1 THEN quantity / 2 ELSE 1 END;
		counter INTEGER := 0;
		date date := $2;
		daysOff date[];
		add INTEGER;
		lastOff date := date;
		forward BOOLEAN := $3 > 0;
	BEGIN
		LOOP
			SELECT CASE WHEN forward THEN array_agg(key1 ORDER BY key1 ASC) ELSE array_agg(key1 ORDER BY key1 DESC) END INTO daysOff FROM Country_countrydate
				WHERE key0 = $1 AND Country_isdayoffcountrydate_country_date = 1 AND CASE WHEN forward THEN key1 > lastOff ELSE key1 < lastOff END LIMIT bufferSize;
			IF daysOff IS NULL THEN	--if there's no days off for the specified country OR date > last day off in the table
				RETURN CASE WHEN $3 = 0 THEN date ELSE NULL END;
			END IF;
			FOR i IN 1..array_upper(daysOff, 1) LOOP
				add = CASE WHEN forward THEN daysOff[i] - lastOff - 1 ELSE lastOff - daysOff[i] - 1 END;
				add = CASE WHEN quantity - counter >= add THEN add ELSE quantity - counter END;
				date = CASE WHEN forward THEN lastOff + add ELSE lastOff - add END;
				counter = counter + add;
				IF counter = quantity THEN
					RETURN date;
				END IF;
				lastOff = daysOff[i];
			END LOOP;
		END LOOP;
		RETURN date;
	END;
$BODY$
  LANGUAGE plpgsql VOLATILE STRICT;
    