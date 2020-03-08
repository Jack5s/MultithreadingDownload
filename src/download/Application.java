package download;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Application {
	private static int coreCount = Runtime.getRuntime().availableProcessors();
	private static ExecutorService threadPool;
	private static int threadCount = 10;
	private static int runningThreadCount = 5;

	public static void main(String[] args) throws Exception {
//		newSingleThreadExecutor()����һ�����̻߳����̳߳أ�
//		��ֻ����Ψһ�Ĺ����߳���ִ�����񣬱�֤����������ָ��˳��(FIFO, LIFO, ���ȼ�)ִ��
//		threadPool = Executors.newSingleThreadExecutor();

//		newCacheThreadPool()�ɻ����̳߳أ��Ȳ鿴������û����ǰ�������̣߳�
//		����У���ֱ��ʹ�á����û�У��ͽ�һ���µ��̼߳�����У������ͳ���ͨ������ִ��һЩ�����ں̵ܶ��첽������
//		threadPool = Executors.newCachedThreadPool();

//		newFixedThreadPool(int n)����һ�������ù̶��������̳߳أ��Թ�����޽���з�ʽ��������Щ�̡߳�
		threadPool = Executors.newFixedThreadPool(runningThreadCount);

//		newScheduledThreadPool(int n)����һ�������̳߳أ�֧�ֶ�ʱ������������ִ��
//		threadPool = Executors.newScheduledThreadPool(runningThreadCount);

//		BlockingQueue<Runnable> bq = new ArrayBlockingQueue<Runnable>(threadCount);
		// ThreadPoolExecutor:�����Զ����̳߳أ����б�����߳���Ϊ3�����������߳���Ϊ6
//		threadPool = new ThreadPoolExecutor(coreCount, threadCount, 24, TimeUnit.HOURS, bq);

//		String downloadURL = "https://ak.hypergryph.com/downloads/a61183d56a7a6d6c26d3c286c5f12ea8/arknights-hg-0872.apk";
		String downloadURL = "https://dldir1.qq.com/qqfile/qq/TIM2.3.2/21173/TIM2.3.2.21173.exe";
		String fileFullName = "F:\\a.exe";
		downloadFile(downloadURL, fileFullName,threadCount);
	}

	public static void downloadFile(String downloadURL, String filePath) {
		try {
//			TrustAllHttpsCertificates();
			URLConnection urlConnection = new URL(downloadURL).openConnection();
			InputStream inputStream = urlConnection.getInputStream();
			byte[] data = readInputStream(inputStream);
			FileOutputStream fileOutputStream = new FileOutputStream(filePath, false);
			fileOutputStream.write(data);
			fileOutputStream.close();
			System.out.println("ok");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static byte[] readInputStream(InputStream inputStream) throws IOException {
		int bufferSize = 1000;
		byte[] buffer = new byte[bufferSize];
		int len = 0;
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		while ((len = inputStream.read(buffer)) != -1) {
			byteArrayOutputStream.write(buffer, 0, len);
		}
		byteArrayOutputStream.close();
		return byteArrayOutputStream.toByteArray();
	}



	public static void downloadFile(String downloadURL, String filePath, int threadCount) {
		try {
			Calendar startCalendar = Calendar.getInstance();
			URLConnection urlConnection = new URL(downloadURL).openConnection();
			urlConnection.setConnectTimeout(100000);
			urlConnection.setRequestProperty("Accept-Encoding", "identity");
			long contentLength = GetRemoteFileSize(downloadURL);

			RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "rw");
			randomAccessFile.setLength(contentLength);
			randomAccessFile.close();

			MonitorThread monitorThread = new MonitorThread(0, threadCount, startCalendar);
			threadPool.submit(monitorThread);
			long blockSize = contentLength / threadCount;
			for (int i = 0; i < threadCount; i++) {
				long startIndex = blockSize * i;
				long endIndex = 0;
				if (i == threadCount) {
					endIndex = contentLength;
				} else {
					endIndex = blockSize * i + blockSize - 1;
				}
				DownloadThread downloadThread = new DownloadThread(downloadURL, filePath, startIndex, endIndex, i,
						monitorThread);
				threadPool.submit(downloadThread);
			}
			threadPool.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static long GetRemoteFileSize(String remoteFileUrl) throws Exception {
		TrustAllHttpsCertificates();
		HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(remoteFileUrl).openConnection();
		httpsURLConnection.setRequestMethod("HEAD");
		long fileSize = -1;
		Map<String, List<String>> fields = httpsURLConnection.getHeaderFields();
		for (String key : fields.keySet()) {
			if (key != null) {
				if (key.equals("Content-Length") == true) {
					fileSize = Long.parseLong(httpsURLConnection.getHeaderField(key));
					break;
				}
			}
		}
		return fileSize;
	}

	private static void TrustAllHttpsCertificates() throws Exception {
		HostnameVerifier hostnameVerifier = new HostnameVerifier() {
			public boolean verify(String urlHostName, SSLSession session) {
				System.out.println("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
				return true;
			}
		};
		HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);

		TrustManager[] trustAllCerts = new TrustManager[1];
		TrustManager trustManager = new MyTrustManager();
		trustAllCerts[0] = trustManager;
		SSLContext sslContext = SSLContext.getInstance("SSL");
		sslContext.init(null, trustAllCerts, null);
		HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
	}

	static class MyTrustManager implements TrustManager, X509TrustManager {
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public boolean isServerTrusted(java.security.cert.X509Certificate[] certs) {
			return true;
		}

		public boolean isClientTrusted(java.security.cert.X509Certificate[] certs) {
			return true;
		}

		public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
				throws CertificateException {
			return;
		}

		public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)
				throws CertificateException {
			return;
		}
	}
}
