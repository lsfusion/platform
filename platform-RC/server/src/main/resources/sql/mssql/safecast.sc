CREATE FUNCTION ${function.name}(@prm1 numeric(38, 10)) RETURNS ${param.type} 
AS
BEGIN
    RETURN(SELECT CASE WHEN @prm1 BETWEEN ${param.minvalue} AND ${param.maxvalue} THEN CAST(@prm1 AS ${param.type}) ELSE NULL END);
END

