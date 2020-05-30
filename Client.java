package mmn16_1;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.*;
import javax.swing.plaf.ListUI;

public class Client extends JFrame{
	private JTextField msgField;
	private JTextArea chatArea;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private String massage =""; 
	private String chatSrv,userN;
	private Socket client;
	private JButton _login,_logout;
	private boolean conected;
	private JList<String> partiList;
	private PartyVec partyVec;
	private final String [] listTitle = {"List of participants"};
	
	/**
	 * create and initiate chat server frame for the client 
	 * @param host the host of this server
	 */
	public Client(String host) {
		super("Client");
		chatSrv = host;
		this.setLayout(new BorderLayout());
		this.msgField = new JTextField();
		this.msgField.setEditable(false);
		this.msgField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				sendMsg("CLIENT",actionEvent.getActionCommand());
				msgField.setText("");
			}
		});
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent wndwEvent) {
				setVisible(false);
				if(conected) {
					sendMsg(userN, "TERMINATE");
					closeConnection();
				}
				dispose();
				System.exit(0);
			}
		});
		add(msgField,BorderLayout.NORTH);
		
		this.chatArea = new JTextArea();
		chatArea.setEditable(false);
		
		this._login= new JButton("Login");
		this._logout = new JButton("Logout");
		ActionListener actionLis = new ActionLis();
		this._login.addActionListener(actionLis);
		this._logout.addActionListener(actionLis);
		this.partiList = new JList<String>(listTitle);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(_login);
		buttonPanel.add(_logout);
		this.add(new JScrollPane(partiList),BorderLayout.EAST);
		this.add(buttonPanel, BorderLayout.SOUTH);
		add(new JScrollPane(chatArea),BorderLayout.CENTER);
		
		setSize(400, 400);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);	
	}

	
	/**
	 * send message 
	 * @param from the name of the sender.
	 * @param msg - the message
	 */
	private void sendMsg(String from,String msg) {
		try {
			output.writeObject(msg); 
			output.flush();
			//displayMsg("\nCLIENT>>> "+msg);
		}catch(IOException ioExc) {
			//need to notify the client about error in I/O
			ioExc.printStackTrace();
		}
	}
	
	/**
	 * make connection and appropriate stream and handle the process 
	 */
	public void runClient() {
		try {
			connectToServer();
			getStream();
			processConnection();
			closeConnection();
		}catch(EOFException eofExp) {
			displayMsg("Server terminated connection");
		} catch (IOException i_o_e) {
			if(!conected) {
				displayMsg("You are disconnected from the chat");
			}
		} catch (ClassNotFoundException e) {
			displayMsg("Unknown type of massage");
		}finally {
			setTextFieldEditable(false);
			
		}
	}
	
	
	/**
	 * @throws IOException
	 */
	private void connectToServer() throws IOException{
		displayMsg("Attempting to connect server...\n");
		client = new Socket(InetAddress.getByName(chatSrv),7777);
		if(!client.isConnected()){
			return;
		}
		userN=JOptionPane.showInputDialog(this, "\t\tHello,\nEnter user name for the chat : ");
		if(userN == null || userN.trim().isEmpty()) {
			userN= System.getProperty("user.name");
		}
		
		conected=true;
		//displayMsg("Connected to : "+client.getInetAddress().getHostName());
	}
	/**
	 * get I/O stream with the server
	 * @throws IOException if an I/O error occurs while writing/reading stream header
	 * @throws StreamCorruptedException - if the stream header is incorrect
	 * @throws SecurityException - if untrusted subclass illegally overrides security-sensitive methods 
	 * @throws NullPointerException - if in out or client's socket is null
	 */
	private void getStream() throws IOException {
		output = new ObjectOutputStream(client.getOutputStream());
		output.flush();
		input =new ObjectInputStream(client.getInputStream());
		//new DataThread().start();
	}
	/**
	 * process the connection with the server
	 */
	private void processConnection() throws  ClassNotFoundException, IOException{
		String list = (String) input.readObject();
		partyVec.add(list.trim().split(">>>")[1].split("#"));
		partiList.setListData(partyVec);
		sendMsg("",userN);
		setTextFieldEditable(true);
		
		boolean terminate = false;
		do {
			massage = (String) input.readObject();
			massage = massage.trim();
			if(massage.contains("Info>>>")) {
				String name = massage.substring("Info>>>".length(), massage.indexOf("connected", "Info>>>".length()));
				if(name.length() > 3 && name.substring(name.length()-3).compareTo("dis")==0) {
					partyVec.remove(name.substring(0, name.length()-4));
				}else {
					partyVec.add(name.trim());
				}
				partiList.setListData(partyVec);
			}else if(massage.contains(">>>TERMINATE")) {
				terminate = true;
			}else {
				displayMsg(massage);
			}
		}while(!terminate && conected);
	}
	/**
	 * close the connection with the server
	 * @throws NullPointerException If there is no connection 
	 */
	private void closeConnection() {
		displayMsg("Closing connection");
		setTextFieldEditable(false);
		partyVec = new PartyVec(listTitle,0);
		partiList.setListData(partyVec);
		if(client == null || !client.isConnected()) {
			conected=false;
			return;
		}
		try {
			output.close();
			input.close();
			client.close();
		}catch(IOException ioEx) {
		}finally {
			conected = false;
		}
	}
	/**
	 * display the given message to the client
	 * @param msgToDisplay message to display
	 */
	private void displayMsg(final String msgToDisplay) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				chatArea.append("\n"+msgToDisplay);
			}
		});
	}
	/**
	 * @param editable <ul>true <strong>if</strong> <li>the client's text field need to be editable</li> 
	 * 			<strong>else</strong><li> false</li> 
	 */
	private void setTextFieldEditable(final boolean editable) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				msgField.setEditable(editable);
			}
		});
	}
	
	private void setIsConnected(final boolean isConnected) {
		conected = isConnected;
	}
	
	private class ConnectionThread extends Thread{
		public void run() {
			runClient();
		}
	}
	
	private class ActionLis implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			if(!conected && actionEvent.getActionCommand().compareTo("Login")==0) {
				partyVec = new PartyVec();
				new ConnectionThread().start();
			}else if(conected && actionEvent.getActionCommand().compareTo("Logout")==0) {
				setIsConnected(false);
				setTextFieldEditable(false);
				partyVec = new PartyVec(listTitle, 0);
				partiList.setListData(partyVec);
				try {
					input.close();
				} catch (IOException e) {
					displayMsg("Logout Error,please send any massage to logout.");
				}
			}
		}
	}
	
	
	private class DataThread extends Thread {
		private ObjectInputStream dataIn;
		private Socket dataSocket;
		public void run() {
			try {
				dataSocket = new Socket(InetAddress.getByName(chatSrv),6666);
				dataIn = new ObjectInputStream(dataSocket.getInputStream());
				
				while(conected) {
					String[] partyArr = (String[]) dataIn.readObject();
					partiList.setListData(partyArr);
				}
				dataIn.close();
				dataSocket.close();
			} catch (IOException e) {
				if(!conected) {return;}
				displayMsg("Error in data connection !\nTerminating connection" );
			} catch (ClassNotFoundException  notFoundException) {
				if(!conected) {return;}
				displayMsg("Unknown type of data!\nTerminating connection.");
			}
		}
	}
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		Client appC = new Client("localhost");
	}
}
