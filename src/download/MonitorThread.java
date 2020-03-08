package download;

import java.util.Calendar;

class MonitorThread extends Thread {
	int finishedCount;
	private int threadCount;
	private Calendar startCalendar;

	public MonitorThread(int finishedCount, int threadCount, Calendar startCalendar) {
		this.finishedCount = finishedCount;
		this.threadCount = threadCount;
		this.startCalendar = startCalendar;
	}

	@Override
	public void run() {
		try {
			while (finishedCount < threadCount) {
				Thread.sleep(1000);
			}
			System.out.println("ok");
			Calendar endCalendar = Calendar.getInstance();
			System.out.println((endCalendar.getTimeInMillis() - startCalendar.getTimeInMillis()) / 1000.0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}