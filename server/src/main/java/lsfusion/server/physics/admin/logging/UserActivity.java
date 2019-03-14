package lsfusion.server.physics.admin.logging;

public class UserActivity {
    public Long computer;
    public Long time;

    public UserActivity(Long computer, Long time) {
        this.computer = computer;
        this.time = time;
    }
}