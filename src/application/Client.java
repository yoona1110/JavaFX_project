package application;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

// 한 명의 Client와 통신할 수 있도록 해주는 Client Class
public class Client {
	
	Socket socket;
	
	public Client(Socket socket) {
		this.socket = socket;
		receive();
	}
	
	// Client로부터 메시지를 전달 받는 메소드
	public void receive() {
		// 하나의 쓰레드를 만들 때, Runnable 많이 사용
		Runnable thread = new Runnable() {
			// Runnable 라이브러리는 내부적으로 반드시 run 함수가 존재해야 함.
			// 하나의 쓰레드가 어떤 모듈로써 동작을 할 건지 run 안에서 정의
			@Override
			public void run() {
				try {
					// 반복적으로 Client에게 메시지를 전달 받음
					while(true) {
						InputStream in = socket.getInputStream();
						byte[] buffer = new byte[512];                           // 한 번에 512바이트만큼 전달 받음
						int length = in.read(buffer);                            // 전달받은 메시지의 크기
						
						while(length == -1) throw new IOException();             // 오류 발생 시 알려줄 것
						
						// 메시지를 전달 받았을 경우 아래 문구 출력
						System.out.println("[메시지 수신 성공]"
								+ socket.getRemoteSocketAddress()                // 현재 접속한 Client의 IP 주소와 같은 주소 정보
								+ ": " + Thread.currentThread().getName());      // 쓰레드의 고유한 정보 (쓰레드의 이름)
						
						String message = new String(buffer, 0, length, "UTF-8"); // 전달 받은 메시지를 한글도 포함 가능하도록 인코딩 처리
						// 전달 받은 메시지를 다른 Client들에게도 보낼 수 있도록
						for(Client client : Main.clients) {
							client.send(message);
						}
					}
					
				} catch (Exception e) {
					try {
						System.out.println("[메시지 수신 오류]"
								+ socket.getRemoteSocketAddress()                // 현재 접속한 Client의 IP 주소와 같은 주소 정보
								+ ": " + Thread.currentThread().getName());      // 쓰레드의 고유한 정보 (쓰레드의 이름)
					
					} catch (Exception e2) {
						e2.printStackTrace();
					}
				}
			}
		};
		
		Main.threadPool.submit(thread);  // 만들어진 쓰레드를 Main의 threadPool에 등록
	}
	
	// Client에게 메시지를 전송하는 메소드
	public void send(String message) {
		Runnable thread = new Runnable() {
			@Override
			public void run() {
				try {
					OutputStream out = socket.getOutputStream();
					byte[] buffer = message.getBytes("UTF-8");
					
					out.write(buffer);                                          // buffer에 담긴 내용을 서버에서 클라이언트로 전송
					out.flush();                                                // 반드시 해줘야 함. 성공적으로 여기까지 전송했다는 것을 알려줄 수 있음.
					
				} catch (Exception e) {
					try {
						System.out.println("[메시지 송신 오류]"
								+ socket.getRemoteSocketAddress()                // 현재 접속한 Client의 IP 주소와 같은 주소 정보
								+ ": " + Thread.currentThread().getName());      // 쓰레드의 고유한 정보 (쓰레드의 이름)
						Main.clients.remove(Client.this);                        // 오류 발생 시 클라이언트 배열에서 해당 클라이언트 제거
						socket.close();                                          // 오류가 발생한 클라이언트의 소켓 닫음
					
					} catch (Exception e2) {
						e2.printStackTrace();
					}
				}
			}
			
		};
		Main.threadPool.submit(thread);
	}
}
