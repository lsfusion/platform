CREATE FUNCTION ${fnc.name}(@prm1 ${param.sqltype}) RETURNS ${param.sqltype} AS
BEGIN
    RETURN(SELECT CASE WHEN @prm1 > -0.0005 AND @prm1 < 0.0005 THEN NULL ELSE @prm1 END)
END
