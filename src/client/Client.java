package client;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class Client extends JFrame implements ActionListener {

	private static final int PORT_DEFAULT = 1412;
	private static final String HOST_DEFAULT = "localhost";
	private static final String PATHSTORE = "/media/pc/FA5AD83A5AD7F17D/tong_hop/hoctap/Ki8/LapTrinhMang/Code/DALTM_SendReceiveFile/client";
	JButton btnSend;
	JButton btnSelectFile;
	JButton btnConnect;
	JFileChooser fileChooser;
	JTextField tfUserSender;
	JTextField tfPort;
	JTextField tfHost;
	JTextField tfUserReceiver;
	JTextField tfFilePath;

	Socket socket;
	DataInputStream dataInputStream;
	DataOutputStream dataOutputStream;
	String userSender;
	String userReceiver;

	public Client(String title) {
		super(title);
		JPanel pnMain = new JPanel();
		pnMain.setLayout(null);

		tfHost = new JTextField();
		createPanelConnection(pnMain, new JLabel("Host:"), 15, 20, 50, 30, tfHost, 70, 20, 130, 30);
		tfHost.setText(HOST_DEFAULT);

		tfPort = new JTextField();
		createPanelConnection(pnMain, new JLabel("Port:"), 220, 20, 50, 30, tfPort, 275, 20, 70, 30);
		tfPort.setText(String.valueOf(PORT_DEFAULT));

		tfUserSender = new JTextField();
		createPanelConnection(pnMain, new JLabel("From:"), 15, 70, 50, 30, tfUserSender, 70, 70, 130, 30);
		tfUserSender.setText("Tuyen");

		tfUserReceiver = new JTextField();
		createPanelConnection(pnMain, new JLabel("To:"), 220, 70, 50, 30, tfUserReceiver, 275, 70, 185, 30);
		tfUserReceiver.setText("Nhi");

		btnConnect = createButton(pnMain, "Connect", 360, 20, 100, 30);
		btnConnect.addActionListener(this);

		JPanel pnSelectFile = new JPanel();
		pnSelectFile.setLayout(null);
		pnSelectFile.setBounds(0, 140, 500, 260);
		pnSelectFile.setBackground(Color.DARK_GRAY);

		JLabel labelTitle = new JLabel("SELECT FILE TO SEND:");
		labelTitle.setBounds(5, 5, 200, 30);
		labelTitle.setFont(new Font("Consolas", Font.BOLD, 15));
		labelTitle.setForeground(Color.white);
		pnSelectFile.add(labelTitle);

		fileChooser = new JFileChooser();
		btnSelectFile = createIconButton("/image/open.png", 40, 50, 40, 40);
		btnSelectFile.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
		btnSelectFile.addActionListener(this);
		pnSelectFile.add(btnSelectFile);

		tfFilePath = new JTextField();
		tfFilePath.setEditable(false);
		tfFilePath.setFont(new Font("Consolas", Font.BOLD, 13));
		tfFilePath.setBounds(100, 50, 380, 40);
		pnSelectFile.add(tfFilePath);

		btnSend = createButton(pnSelectFile, "Send", 140, 120, 100, 30);
		btnSend.addActionListener(this);
		JButton btnCancel = createButton(pnSelectFile, "Cancel", 270, 120, 100, 30);
		btnCancel.addActionListener(this);
		pnMain.add(pnSelectFile);
		this.setSize(500, 400);
		this.setLocationRelativeTo(null);
		this.setResizable(false);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.add(pnMain);
		this.setVisible(true);
	}

	public static void main(String[] args) {
		new Client("Client");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnSelectFile) {
			int select = fileChooser.showOpenDialog(Client.this);
			if (select == JFileChooser.APPROVE_OPTION) {
				String path = fileChooser.getSelectedFile().getPath();
//				String[] tmp = path.split("/");
				tfFilePath.setText(path); //view name file select
			}
		} else if (e.getSource() == btnSend) {
			try {
				if (socket.isConnected()) {
					String path = tfFilePath.getText();
					String endOfFile = getEndOfFile(path);
					dataOutputStream.writeUTF("sendfile:" + userSender + ":" + userReceiver + ":" + endOfFile);
					sendFile(socket, path);
					System.out.println("Sendfile thành công!");
				} else {
					JOptionPane.showMessageDialog(null, "Bạn phải connect tới server!");
				}
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(null, "Truyền file thất bại!");
				e1.printStackTrace();
			}
		} else if (e.getSource() == btnConnect) {
			userSender = tfUserSender.getText();
			userReceiver = tfUserReceiver.getText();
			try {
				socket = new Socket("localhost", 1412);
				dataInputStream = new DataInputStream(socket.getInputStream());
				dataOutputStream = new DataOutputStream(socket.getOutputStream());
				dataOutputStream.writeUTF("connect:" + userSender + ":" + userReceiver);
				listenThread();
			} catch (IOException ex) {
				tfUserSender.setEditable(true);
			}
			btnConnect.setText("GetFile");
		} else {
			System.exit(0);
		}
	}

	public void listenThread() {
		Thread incomingReader = new Thread(new Incoming());
		incomingReader.start();
	}

	private String getEndOfFile(String path) {
		int size = path.length();
		return path.substring(size - 4, size);
	}

	public void sendFile(Socket clientSock, String file) throws IOException {
		File f = new File(file);
		DataOutputStream dos = new DataOutputStream(clientSock.getOutputStream());
		FileInputStream fis = new FileInputStream(file);
		dos.writeLong(f.length());
		System.out.println(f.length());
		byte[] buffer = new byte[8192];
		while (fis.read(buffer) > 0) {
			dos.write(buffer);
		}
		fis.close();
	}

	private void createStore(String folderName) {
		File dataDirectory = new File(PATHSTORE + "/" + folderName);
		if (!dataDirectory.exists()) {
			dataDirectory.mkdir();
		}
	}

	private JButton createIconButton(String path, int x, int y, int width, int height) {
		JButton button = new JButton();
		button.setBounds(x, y, width, height);
		button.setIcon(new ImageIcon(getClass().getResource(path)));
		return button;
	}

	private JButton createButton(JPanel panel, String title, int x, int y, int width, int height) {
		JButton button = new JButton(title);
		button.setBounds(x, y, width, height);
		button.setBackground(Color.blue);
		button.setForeground(Color.white);
		panel.add(button);
		return button;
	}

	private void createPanelConnection(JPanel panel, JLabel label, int x1, int y1, int width1, int height1,
			JTextField textField, int x2, int y2, int width2, int height2) {
		label.setBounds(x1, y1, width1, height1);
		panel.add(label);
		textField.setBounds(x2, y2, width2, height2);
		panel.add(textField);
	}

	public class Incoming implements Runnable {
		@Override
		public void run() {
			String[] data;
			String stream;
			try {
				while ((stream = dataInputStream.readUTF()) != null) {
					data = stream.split(":");
					if (data[0].equals("sendfile")) {
						if (tfUserSender.getText().equals(data[1])) {
							File f = new File(PATHSTORE + "/" + data[1]);
							if (f.exists() && f.isDirectory()) {
								int count = f.listFiles().length;
								System.out.println(count);
								saveFile(socket, PATHSTORE + "/" + data[1] + "/" + data[1] + count + data[2]);
							} else {
								createStore(data[1]);
								saveFile(socket, PATHSTORE + "/" + data[1] + "/" + data[1] + data[2]);
								JOptionPane.showMessageDialog(null, stream);
							}
							dataOutputStream.writeUTF("delete:" + data[1]);
							System.out.println("Đã nhận được file");
						}
					}
					dataInputStream = new DataInputStream(socket.getInputStream());
					System.out.println("Đang chờ file gửi đến...");
				}
			} catch (Exception ex) {
			}
		}

		private void saveFile(Socket clientSock, String filename) throws IOException {
			DataInputStream dis = new DataInputStream(clientSock.getInputStream());
			FileOutputStream fos = new FileOutputStream(filename);
			int count = 1;
			long filesize = dis.readLong();
			byte[] buffer = new byte[8192]; // 8KB
			int read = 0;
			int totalRead = 0;
			long remaining = filesize;
			while ((read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) > 0) {
				totalRead += read;
				remaining -= read;
				System.out.println(count++ + ": read " + totalRead + " bytes.");
				fos.write(buffer, 0, read);
			}
			fos.close();
		}
	}

}
