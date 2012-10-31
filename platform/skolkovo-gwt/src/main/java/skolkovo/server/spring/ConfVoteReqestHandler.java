package skolkovo.server.spring;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;
import platform.base.ExceptionUtils;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.interop.exceptions.RemoteInternalException;
import skolkovo.api.remote.SkolkovoRemoteInterface;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component("confVoteHandler")
public class ConfVoteReqestHandler implements HttpRequestHandler {
    protected final static Logger logger = Logger.getLogger(ConfVoteReqestHandler.class);

    private static final String CONFHASH_PARAM = "conf";
    private static final String VOTE_RES_PARAM = "result";

    @Autowired
    private BusinessLogicsProvider<SkolkovoRemoteInterface> blProvider;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("Handling jnlp request");

        String conferenceHash = request.getParameter(CONFHASH_PARAM);
        String sVoteResult = request.getParameter(VOTE_RES_PARAM);
        if (conferenceHash == null || (!"yes".equalsIgnoreCase(sVoteResult) && !"no".equalsIgnoreCase(sVoteResult))) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Wrong request!!!");
            return;
        }

        try {
            boolean voteResult = "yes".equalsIgnoreCase(sVoteResult);
            blProvider.getLogics().setConfResult(conferenceHash, voteResult);
            response.getOutputStream().println(voteResult?"You have confirmed your participation":"You have refused from participation");
        } catch (RemoteInternalException e) {
            response.getOutputStream().println(ExceptionUtils.getInitialCause(e).getMessage());
        } catch (Exception e) {
            blProvider.invalidate();
            logger.debug("Error handling request: ", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing request, plz try again later.");
        }
    }
}
