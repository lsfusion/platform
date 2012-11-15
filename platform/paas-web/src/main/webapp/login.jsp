<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
    <head><title>Simple jsp page</title></head>
    <body>
        <form action="/j_spring_security_check" method="post">
            <div>
                <table align="center">
                    <tr>
                        <td>&nbsp;</td>
                        <td>&nbsp;</td>
                    </tr>
                    <tr>
                        <td>&nbsp;</td>
                        <td>&nbsp;</td>
                    </tr>
                    <tr>
                        <td>&nbsp;</td>
                        <td>&nbsp;</td>
                    </tr>
                    <tr>
                        <td colspan="2" style="font-weight:bold;">Log In</td>
                    </tr>
                    <tr>
                        <td>User name</td>
                        <td><input name="j_username" type="text" value="admin"/></td>
                    </tr>
                    <tr>
                        <td>Password</td>
                        <td><input name="j_password" type="password" value=""/></td>
                    </tr>
                    <tr>
                        <td></td>
                        <td><input type="submit" value="Login"/></td>
                    </tr>
                    <tr>
                        <td>&nbsp;</td>
                        <td>&nbsp;</td>
                    </tr>
                    <tr>
                        <td colspan="2">Forget your password?</td>
                    </tr>
                    <tr>
                        <td colspan="2">Contact your administrator.</td>
                    </tr>
                </table>
            </div>
        </form>
    </body>
</html>