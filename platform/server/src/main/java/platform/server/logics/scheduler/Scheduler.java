package platform.server.logics.scheduler;

import java.util.List;

public class Scheduler implements Runnable {

    private int sleepTime;
    private List<SchedulerTask> tasks;

    public Scheduler(int sleepTime, List<SchedulerTask> tasks) {
        this.sleepTime = sleepTime;
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

        while (true) {

            for (SchedulerTask task : tasks)
                try {
                    task.execute();
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
