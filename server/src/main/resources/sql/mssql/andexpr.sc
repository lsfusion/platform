CREATE FUNCTION ${fnc.name}(@prm1 bit, @prm2 ${param.sqltype}) RETURNS ${param.sqltype} AS
BEGIN
    RETURN(SELECT CASE WHEN @prm1 = 1 THEN @prm2 ELSE NULL END)
END
