package download;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

class DownloadThread extends Thread {
	private String downloadURL;
	private String filePath;
	private long startIndex;
	private long endIndex;
	private int threadIndex;
	private int bufferSize;
	private MonitorThread monitorThread;

	public DownloadThread(String downloadURL, String filePath, long startIndex, long endIndex, int threadIndex,
			MonitorThread monitorThread) {
		this.downloadURL = downloadURL;
		this.filePath = filePath;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.threadIndex = threadIndex;
		this.bufferSize = 1000;
		this.monitorThread = monitorThread;
	}

	public byte[] readInputStream(InputStream inputStream) throws Exception {
		byte[] buffer = new byte[this.bufferSize];
		int len = 0;
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		while ((len = inputStream.read(buffer)) != -1) {
			byteArrayOutputStream.write(buffer, 0, len);
		}
		byteArrayOutputStream.close();
		return byteArrayOutputStream.toByteArray();
	}

	@Override
	public void run() {
		try {
			URL urlPath = new URL(downloadURL);
			HttpURLConnection connection = (HttpURLConnection) urlPath.openConnection();
			connection.setConnectTimeout(0);
			connection.setRequestProperty("Range", "bytes=" + startIndex + "-" + endIndex);
			connection.setRequestMethod("GET");
			connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
			int status = connection.getResponseCode();
			if (status == 206 || status == 200) {
				System.out.println("Part " + threadIndex + " start: " + startIndex + " - " + endIndex);
				InputStream inputStream = connection.getInputStream();
				byte[] data = readInputStream(inputStream);
				RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "rwd");
				randomAccessFile.seek(startIndex);
				randomAccessFile.write(data);
				randomAccessFile.close();
				System.out.println("Part " + threadIndex + " finished");
				monitorThread.finishedCount++;
			} else {
				throw new Exception();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
