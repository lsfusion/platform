CREATE FUNCTION ${fnc.name}(@prm1 ${param.sqltype}, @prm2 ${param.sqltype}) RETURNS ${param.sqltype} AS
BEGIN
    RETURN(SELECT CASE WHEN @prm1 > @prm2 THEN @prm1 ELSE COALESCE(@prm2, @prm1) END)
END
