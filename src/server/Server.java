package server;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class Server extends JFrame implements ActionListener {
	private static final String PATH_STORE = "/media/pc/FA5AD83A5AD7F17D/tong_hop/hoctap/Ki8/LapTrinhMang/Code/DALTM_SendReceiveFile/server";
	private static final int PORT_DEFAULT = 1412;
	private JTextArea taInformation;
	private JButton btnStart;
	private JButton btnStop;
	private JButton btnChangePort;
	private JTextField tfServerPort;

	private ArrayList<DataOutputStream> clientOutputStreams;
	private ArrayList<String> userUploads;
	private ArrayList<String> userDownloads;
	private ServerSocket serverSocket;
	private Socket socket;
	private String endOfFile = "";

	public Server(String title) {
		super(title);
		initUI();
	}

	private void initUI() {
		JPanel pnMain = new JPanel();
		pnMain.setLayout(null);
		pnMain.setBackground(Color.darkGray);

		JLabel lbPort = createLabel("Port:", 100, 10, 50, 30);
		pnMain.add(lbPort);

		tfServerPort = createTextField(160, 10, 100, 30);
		tfServerPort.setText("1412");
		pnMain.add(tfServerPort);

		btnChangePort = createButton("Change", 270, 10, 100, 30);
		pnMain.add(btnChangePort);

		taInformation = new JTextArea();
		taInformation.setLineWrap(true);
		taInformation.setWrapStyleWord(true);
		taInformation.setEditable(false);

		JScrollPane js = new JScrollPane(taInformation);
		js.getViewport().setView(taInformation);
		js.setBounds(10, 60, 480, 250);
		pnMain.add(js);

		JPanel pnButton = new JPanel();
		pnButton.setLayout(new FlowLayout());
		pnButton.setBounds(0, 320, 500, 40);
		pnButton.setBackground(Color.darkGray);
		btnStart = createButton("Start", 40, 320, 100, 30);
		pnButton.add(btnStart);
		btnStop = createButton("Stop", 170, 320, 100, 30);
		pnButton.add(btnStop);

		pnMain.add(pnButton);
		btnChangePort.addActionListener(this);
		btnStart.addActionListener(this);
		btnStop.addActionListener(this);
		
		this.add(pnMain);
		this.setSize(500, 400);
		this.setResizable(false);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);
	}

	public static void main(String[] args) {
		String ip = "localhost";
		try {
		    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		    while (interfaces.hasMoreElements()) {
		        NetworkInterface iface = interfaces.nextElement();
		        // filters out 127.0.0.1 and inactive interfaces
		        if (iface.isLoopback() || !iface.isUp())
		            continue;
		        Enumeration<InetAddress> addresses = iface.getInetAddresses();
		        while(addresses.hasMoreElements()) {
		            InetAddress addr = addresses.nextElement();
		            if (addr instanceof Inet6Address) continue;
		            ip = addr.getHostAddress();
		        }
		    }
		} catch (SocketException e) {
		    throw new RuntimeException(e);
		}
		new Server("Server - "+ip);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnStart) {
			Thread starter = new Thread(new ServerStart());
			starter.start();
		} else if (e.getSource() == btnChangePort) {
			tfServerPort.setEditable(true);
		} else if (e.getSource() == btnStop) {
			try {
				if (serverSocket != null) {
					if (socket != null)
						socket.close();
					serverSocket.close();
					taInformation.append("Server đã ngừng hoạt động. \n");
				}
				System.exit(0);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	public class ServerStart implements Runnable {
		@SuppressWarnings("resource")
		@Override
		public void run() {
			clientOutputStreams = new ArrayList<>();
			userUploads = new ArrayList<>();
			userDownloads = new ArrayList<>();
			int currentPort = PORT_DEFAULT;
			try {
				currentPort = Integer.parseInt(String.valueOf(tfServerPort.getText()));
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(null,
						"Port không hợp lệ hoặc đã được sử dụng!\n " + "Server sẽ chạy với port mặc định.");
			}
			try {
				ServerSocket serverSocket = new ServerSocket(currentPort);
				taInformation.append("Server đã khởi động với PORT = " + currentPort + ".\n");
				boolean check = true;
				while (check) {
					Socket clientSocket = serverSocket.accept();
					DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
					clientOutputStreams.add(dos);
					Thread listener = new Thread(new ClientHandler(clientSocket, dos));
					listener.start();
				}
			} catch (Exception ex) {
				taInformation.append("Error making a connection. \n");
			}
		}
	}

	public class ClientHandler implements Runnable {

		DataInputStream din;
		Socket socket;
		DataOutputStream dos;

		public ClientHandler(Socket clientSocket, DataOutputStream dos) {
			this.dos = dos;
			try {
				socket = clientSocket;
				din = new DataInputStream(socket.getInputStream());
			} catch (Exception ex) {
				taInformation.append("Unexpected error... \n");
			}

		}

		@Override
		public void run() {
			String message;
			String[] data;
			try {
				while ((message = din.readUTF()) != null) {
					data = message.split(":");
					System.out.println("Request: " + message + "\n");
					if (data[0].equals("connect")) {
						userUploads.add(data[1]);
						userDownloads.add(data[2]);
						taInformation.append(data[1] + " đã kết nối!\n");
						int size = userDownloads.size();
						for (int i = 0; i < size; i++) {
							if (data[1].equals(userDownloads.get(i))) {
								String nameSend = data[1];
								File f = new File(PATH_STORE + "/" + nameSend + "/" + nameSend + endOfFile);
								if (f.exists() && f.isFile()) {
									dos.writeUTF("sendfile:" + data[1] + ":" + endOfFile);
									sendFile(socket, PATH_STORE + "/" + nameSend + "/" + nameSend + endOfFile);
									System.out.println("Đã gởi file tro lai cho client " + data[1]);
								}
							}
						}

					} else if (data[0].equals("sendfile")) {
						String nameStore = data[2];
						endOfFile = data[3];
						createStore(nameStore);
						saveFile(socket, PATH_STORE + "/" + nameStore + "/" + nameStore + endOfFile);
						taInformation.append(data[1] + " đã truyền file!\n");
						int size = userDownloads.size();
						for (int i = 0; i < size; i++) {
							if (data[2].equals(userDownloads.get(i))) {
								dos.writeUTF("sendfile:" + data[2]);
								String nameSend = data[2];
								dos.writeUTF("sendfile:" + nameSend + ":" + endOfFile);
								sendFile(socket, PATH_STORE + "/" + nameSend + "/" + nameSend + endOfFile);
							}
						}
					} else if (data[0].equals("delete")) {
						String fileName = data[1];
						File f = new File(PATH_STORE + "/" + fileName + "/" + fileName + endOfFile);
						if (f.exists() && f.isFile()) {
							f.delete();
						}
					}
				}
			} catch (IOException ex) {
				taInformation.append("Có 1 Client đã ngắt kết nối. \n");
				ex.printStackTrace();
				clientOutputStreams.remove(dos);
			}
		}
	}

	@SuppressWarnings("unused")
	private String getEndOfFile(String path) {
		int size = path.length();
		return path.substring(size - 4, size);
	}

	public void sendFile(Socket clientSock, String file) throws IOException {
		File f = new File(file);
		if (f.exists() && !f.isDirectory()) {
			DataOutputStream dos = new DataOutputStream(clientSock.getOutputStream());
			FileInputStream fis = new FileInputStream(file);
			dos.writeLong(f.length());
			System.out.println("method:sendFile-Kích thước file cần truyền: " + f.length());
			byte[] buffer = new byte[8192];
			while (fis.read(buffer) > 0) {
				dos.write(buffer);
			}
			fis.close();
		}
	}

	private void saveFile(Socket clientSock, String filename) throws IOException {
		DataInputStream dis = new DataInputStream(clientSock.getInputStream());
		FileOutputStream fos = new FileOutputStream(filename);
		int count = 1;
		long filesize = dis.readLong();
		System.out.println("method:saveFile-Kích thước file nhận được: " + filesize);
		byte[] buffer = new byte[8192]; // 8KB
		int read = 0;
		int totalRead = 0;
		long remaining = filesize;
		while ((read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) > 0) {
			totalRead += read;
			remaining -= read;
			System.out.println(count++ + ": Đọc " + totalRead + " bytes.");
			fos.write(buffer, 0, read);
		}
		fos.close();
	}

	private void createStore(String folderName) {
		File dataDirectory = new File(PATH_STORE + "/" + folderName);
		if (!dataDirectory.exists()) {
			dataDirectory.mkdir();
		}
	}

	private JButton createButton(String title, int x, int y, int width, int height) {
		JButton button = new JButton(title);
		button.setBounds(x, y, width, height);
		button.setBackground(Color.BLUE);
		button.setForeground(Color.white);
		return button;
	}

	private JTextField createTextField(int x, int y, int width, int height) {
		JTextField textField = new JTextField();
		textField.setBounds(x, y, width, height);
		textField.setEditable(false);
		return textField;
	}

	private JLabel createLabel(String title, int x, int y, int width, int height) {
		JLabel label = new JLabel(title);
		label.setBounds(x, y, width, height);
		label.setForeground(Color.white);
		return label;
	}
}
