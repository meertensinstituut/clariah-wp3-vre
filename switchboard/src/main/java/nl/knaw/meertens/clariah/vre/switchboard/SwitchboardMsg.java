package nl.knaw.meertens.clariah.vre.switchboard;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus;

public class SwitchboardMsg {

    public String msg;

    public String workDir;

    public String stackTrace;

    public DeploymentStatus status;

    public SwitchboardMsg(String msg) {
        this.msg = msg;
    }

    public SwitchboardMsg(String message, String stackTrace) {
        this(message);
        this.stackTrace = stackTrace;
    }
}
