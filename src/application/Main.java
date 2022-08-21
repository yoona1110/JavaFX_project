package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
	
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 하나의 서버 프로그램은 하나의 서버 모듈을 구동시키도록 구현
public class Main extends Application {
	
	// ExecutorService : 여러 개의 쓰레드를 효율적으로 관리하기 위한 라이브러리
	// threadPool로 쓰레드를 처리하면 기본적인 쓰레드 숫자에 제한을 두므로,
	// 갑작스럽게 Client 수가 폭증하더라도 서버의 성능 저하를 방지
	// => 한정된 자원을 이용해서 안정적으로 서버를 운영하기 위함
	public static ExecutorService threadPool;
	public static Vector<Client> clients = new Vector<Client>();
	
	ServerSocket serverSocket;
	
	// 서버를 구동시켜서 클라이언트의 연결을 기다리는 메소드
	public void startServer(String IP, int port) {
		try {
			serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress(IP, port));
		} catch (Exception e) {
			e.printStackTrace();
			if(!serverSocket.isClosed()) { // 서버 소켓이 닫혀있는 상태가 아니라면
				stopServer(); // 서버 닫아줌
			}
			return;
		}
		
		// 클라이언트가 접속할 때까지 계속 기다리는 쓰레드
		Runnable thread = new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						// 클라이언크가 접속했다면 클라이언트 배열에 새롭게 접속한 클라이언트를 추가
						Socket socket = serverSocket.accept();
						clients.add(new Client(socket));
						System.out.println("[클라이언트 접속]"
								+ socket.getRemoteSocketAddress()
								+ ": " + Thread.currentThread().getName());
					} catch (Exception e) {
						if(!serverSocket.isClosed()) {
							stopServer();
						}
						break;
					}
				}
			}
		};
		threadPool = Executors.newCachedThreadPool();  // threadPool 초기화
		threadPool.submit(thread);                     // 클라이언트의 접속을 기다리는 쓰레드를 첫번째 쓰레드로 넣음
	}
	
	// 서버의 작동을 중지시키는 메소드 - 서버 작동 종료 이후에 자원을 할당 해지
	public void stopServer() {
		try {
			// 현재 작동 중인 모든 소켓 닫기
			Iterator<Client> iterator = clients.iterator();
			while(iterator.hasNext()) {               // client 하나하나 접근
				Client client = iterator.next();
				client.socket.close();                // 소켓 닫기
				iterator.remove();
			}
			
			// 서버 소켓 객체 닫기
			if(serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}
			// 스레드 풀 종료
			if(threadPool != null && !threadPool.isShutdown()) {
				threadPool.shutdown();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// UI를 생성하고, 실질적으로 프로그램을 동작시키는 메소드
	@Override
	public void start(Stage primaryStage) {
		BorderPane root = new BorderPane(); // 레이아웃. 다른 디자인 요소들을 담는 것
		root.setPadding(new Insets(5));     // 패딩 5
		
		TextArea textArea = new TextArea();
		textArea.setEditable(false);        // 수정이 불가능하게 만든다
		textArea.setFont(new Font("나눔바른고딕", 15));
		root.setCenter(textArea);
		
		Button toggleButton = new Button("시작하기");
		toggleButton.setMaxWidth(Double.MAX_VALUE);
		BorderPane.setMargin(toggleButton, new Insets(1, 0, 0, 0));
		root.setBottom(toggleButton);
		
		String IP = "127.0.0.1";
		int port = 9876;
		
		toggleButton.setOnAction(event -> {
			if(toggleButton.getText().equals("시작하기")) { // 시작하기
				startServer(IP, port);
				// runLater 같은 걸 이용해서 GUI 출력하도록 구성해야 함
				Platform.runLater(() -> {
					String message = String.format("[서버 시작]\n", IP, port);
					textArea.appendText(message);
					toggleButton.setText("종료하기");  // 버튼의 내용을 종료하기로 변경
				});
			} else { // 종료하기
				stopServer();
				// runLater 같은 걸 이용해서 GUI 출력하도록 구성해야 함
				Platform.runLater(() -> {
					String message = String.format("[서버 종료]\n", IP, port);
					textArea.appendText(message);
					toggleButton.setText("시작하기");  // 버튼의 내용을 시작하기로 변경
				});
			}
		});
		
		Scene scene = new Scene(root, 400, 400);
		primaryStage.setTitle("[채팅 서버]");
		
		// 프로그램 자체를 종료했다면 stopServer()를 호출하여 서버도 종료한 후에 프로그램을 종료하도록
		primaryStage.setOnCloseRequest(event -> stopServer());
		
		// scene 정보를 정상적으로 출력할 수 있도록
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	// 프로그램의 진입점
	public static void main(String[] args) {
		launch(args);
	}
}
