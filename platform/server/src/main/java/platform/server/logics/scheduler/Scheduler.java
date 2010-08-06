package platform.server.logics.scheduler;

import java.util.Calendar;
import java.util.List;

public class Scheduler implements Runnable {

    private int sleepTime;
    private int backupHour;
    private List<SchedulerTask> tasks;

    public Scheduler(int sleepTime, int backupHour, List<SchedulerTask> tasks) {
        this.sleepTime = sleepTime;
        this.backupHour = backupHour;
        this.tasks = tasks;
    }

    public void setTasks(List<SchedulerTask> tasks) {
        this.tasks = tasks;
    }

    public void start() {
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    public void run() {
        boolean dayBackup = false;

        while (true) {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);

            if (dayBackup && hour != backupHour)
                dayBackup = false;

            for (SchedulerTask task : tasks)
                try {
                    if (!task.getID().equals("dump")) {
                        task.execute();
                    } else if(hour == backupHour && !dayBackup) {
                        task.execute();
                        dayBackup = true;
                    }
                } catch (Exception e) {
                    System.out.println("Ошибка выполнении задания " + task.getID() + " : ");
                    e.printStackTrace();
                }

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                System.out.println("Такого вообще-то не бывает");
            }
        }

    }
}
