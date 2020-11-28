package x.mvmn.lastfmscrobbler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class Server {
	private static final byte[] OK = "OK".getBytes(StandardCharsets.UTF_8);

	private final int port;
	private final Consumer<String> requestProcessor;

	private volatile ServerSocket serverSocket;

	public Server(int port, Consumer<String> requestProcessor) {
		this.port = port;
		this.requestProcessor = requestProcessor;
	}

	public void start() throws IOException {
		if (serverSocket == null) {
			serverSocket = new ServerSocket(port);
			new Thread(() -> {
				while (serverSocket != null && !serverSocket.isClosed()) {
					try {
						Socket socket = serverSocket.accept();
						new Thread(() -> handleConnection(socket)).start();
					} catch (Exception e) {
						if (!e.getMessage().contains("Socket closed")) {
							e.printStackTrace();
						}
					}
				}
			}).start();
		} else {
			throw new IllegalStateException("Server already started");
		}
	}

	public void stop() throws IOException {
		if (serverSocket != null) {
			serverSocket.close();
			serverSocket = null;
		}
	}

	private void handleConnection(Socket socket) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
			String line = "";
			while (!socket.isClosed() && line != null) {
				line = br.readLine();
				if (line != null) {
					requestProcessor.accept(line);
				}
				// println line!=null?line:"<EOF>";
				try (OutputStream os = socket.getOutputStream()) {
					os.write(OK);
				}
			}
			// println "Done reading"
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// println "Connection ended";
			try {
				socket.close();
			} catch (IOException se) {
				if (!se.getMessage().contains("Socket closed")) {
					se.printStackTrace();
				}
			}
		}
	}
}
