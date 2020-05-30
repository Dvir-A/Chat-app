package mmn16_1;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

/**
 * @author dvir0
 *
 */
public class ChatServer extends JFrame{
	private JTextField _msgField;
	private JTextArea _chatArea;
	private ServerSocket _server,_dataBase;
	private Socket _connection;
	private ArrayList<ChatThread> _clientList;
	private PartyVec _clientsNameList;
	private JList<String> _usersList;
	
	public ChatServer() {
		super("Server");
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent wndwEvent) {
				setVisible(false);
				sendToAll("SERVER", "TERMINATE", false);
				try {
					ChatServer.this.finalize();
				} catch (Throwable t) {
					t.printStackTrace();
				}
				dispose();
				System.exit(0);
			}
		});
		_clientList= new ArrayList<ChatThread>();
		_clientsNameList =new PartyVec();
		_clientsNameList.add("List of participants");
		_clientsNameList.add("Server");
		this._usersList = new JList<String>(_clientsNameList);
		this.add(new JScrollPane(_usersList),BorderLayout.EAST);
		this._msgField = new JTextField();
		this._msgField.setEditable(false);
		this._msgField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				sendToAll("SERVER",actionEvent.getActionCommand(),true);
				_msgField.setText("");
			}
		});
		add(_msgField,BorderLayout.NORTH);
		
		this._chatArea = new JTextArea(60,30);
		_chatArea.setEditable(false);
		add(new JScrollPane(_chatArea),BorderLayout.CENTER);
		setSize(400, 400);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setVisible(true);
	}
	

	/**
	 * Wait for connection with client
	 * 
	 * @throws IOException  - if an I/O error occurs when waiting for a connection.
	 * @throws SecurityException - if a security manager exists and its checkAccept method doesn't allow the operation.
	 * @throws	SocketTimeoutException - if a timeout was previously set with setSoTimeout andthe timeout has been reached.
	 * @throws	IllegalBlockingModeException - if this socket has an associated channel, 
	 * 							the channel is in non-blocking mode, and there is no connection ready to beaccepted
	 */
	private void waitForConnection() throws IOException {
		//displayMsg("waiting for connection");
		_connection = _server.accept();
	}
	
	@Override
	protected void finalize() throws Throwable {
		for (ChatThread chatThread : _clientList) {
			try {
				chatThread.finalize();
			}catch(Throwable t) {}
		}
		_server.close();
		super.finalize();
	}
	
	/**
	 * send message to all the client
	 * @param from the sender name
	 * @param msg the message to send
	 * @param print indicate if this message will send to the server
	 */
	protected void sendToAll(String from,String msg,boolean print) {
		for (ChatThread chatThread : _clientList) {
			chatThread.sendMsg(from,msg,print);
			if(print) {
				print=false;
			}
		}
	}
	
	/**
	 * run the server by establish a connection with clients and make a new chat thread to handle the rest 
	 */
	public void runServer() {
		try {
			_server = new ServerSocket(7777);
			//_dataBase = new ServerSocket(6666);
			while(true) {
				try {
					waitForConnection();
					ChatThread clientT = new ChatThread(_connection);
					_clientList.add(clientT);
					clientT.start();
				}catch(EOFException eofExp) {
					displayMsg("Server terminated connection");
				}
			}
		}catch(IOException ioExc) {
			displayMsg("Error in runServer()");
		}
	}
	
	private class ChatThread extends Thread{
		private Socket _cnct;
		private ObjectOutputStream _output;
		private ObjectInputStream _input;
		private String myName=null;
		private boolean _isConnected=false;
		/**
		 * Construct a new thread 
		 * @param conection - the socket how's connect to a specific client
		 */
		public ChatThread(Socket conection) {
			super("ChatThread"+_clientList.size());
			this._cnct=conection;
		}
		@Override
		protected void finalize() throws Throwable {
			_input.close();
			_output.close();
			_cnct.close();
			super.finalize();
		}
		/**
		 * get I/O stream with the client
		 * @throws IOException if an I/O error occurs while writing/reading stream header
		 * @throws StreamCorruptedException - if the stream header is incorrect
		 * @throws SecurityException - if untrusted subclass illegally overrides security-sensitive methods 
		 * @throws NullPointerException - if in is null
		 */
		private void getStream() throws IOException {
			_output = new ObjectOutputStream(_cnct.getOutputStream());
			_output.flush();
			_input = new ObjectInputStream(_cnct.getInputStream());
		}
		/**
		 * process the connection with the client
		 */
		private void processConnection() {
			sendMsg("", _clientsNameList.toString(), false);
			try {
				myName = (String) _input.readObject();
				_clientsNameList.add(myName);
			} catch (ClassNotFoundException | IOException exc) {
				return;
			}
			String massage = myName+" connected successfuly";
			sendToAll("Info",massage,true);
			updateList();
			
			setTextFieldEditable(true);
			boolean terminate = false;
			do {
				try {
					massage = (String) _input.readObject();
					if(massage.compareTo("TERMINATE")==0) {
						terminate = true;
					}else {
						sendToAll(myName,massage,true);
					}
				}catch(ClassNotFoundException classNotFound) {
					displayMsg("Unkown type of massage recived");
				} catch (IOException e) {
					return;
				}
			}while(!terminate);
		}
		/**
		 * close the connection with the client
		 * @throws IOException If an I/O error has occurred.
		 */
		private void closeConnection() throws Throwable{
			_isConnected=false;
			displayMsg("Terminating Connection with "+myName);
			_clientList.remove(this);
			_clientsNameList.remove(myName);
			if(_clientList.size()==0) {
				setTextFieldEditable(false);
			}
			updateList();
			sendToAll("Info",myName+" disconnected",false);
			this.finalize();
		}
		/**send message to all the client
		 * @param from the sender name
		 * @param msg the message to send
		 * @param print indicate if this message will send to the server
		 */
		private void sendMsg(String from,String msg,boolean srvPrint) {
			try {
				_output.writeObject(from+">>>"+msg);
				_output.flush();
				if(!srvPrint) {
					return;
				}
				displayMsg(from+">>> "+msg);
			}catch(IOException ioExc) {
				displayMsg("Error in sendingMsg !");
			}
		}
		@Override
		public void run() {
			try {
				_isConnected=true;
				//new ParticipateThread().start();
				getStream();
				processConnection();
				closeConnection();
			} catch (IOException e) {
				displayMsg("Connection with "+myName+" destroyed");
				_clientList.remove(this);
				_clientsNameList.remove(myName);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		
		/**
		 * ParticipateThread handle the transformation of the chat's participate list , between the server and the client<br>
		 * this method of transformation can take a lot of time and effort . <br>
		 * 
		 * @author dvir	
		 *
		 */
		private class ParticipateThread extends Thread{
			
			private ObjectOutputStream outData;
			private Socket dataSocket;
			
			public ParticipateThread() {
				super("ServerPartiThread"+_clientList.size());
			}
			
			public void run() {
				 try {
					dataSocket = _dataBase.accept();
					displayMsg("new data connection recived");
					outData = new ObjectOutputStream(dataSocket.getOutputStream());
					outData.flush();
				} catch (IOException eIoException) {
					eIoException.printStackTrace();
				}
				 while(_isConnected) {
						try {
							String[] clientsNameArr = new String[_clientList.size()+1] ;
							outData.writeObject(_clientsNameList.toArray(clientsNameArr));
							outData.flush();
						}catch (IOException exception) {
							_isConnected=false;
							//displayMsg(myName+" data's connection is off!");
						}
					}
				 try {
					outData.close();
					dataSocket.close();
				} catch (IOException e) {}
			}
		} 
	}
	private void updateList() {
		//new NotifyThread().start();
		this._usersList.setListData(_clientsNameList);
	}
	
	private class NotifyThread extends Thread{
		public void run() {
			for(Iterator<ChatThread> iterator = _clientList.iterator();iterator.hasNext();) {
			//for (ChatThread chatThread : _clientList) {
				//if(chatThread._isConnected) {
				ChatThread chatT = iterator.next();
				synchronized(chatT) {
					chatT.interrupt();
				}
				//}
			}
		}
	}
	
	
	/**
	 * display the given message to the server
	 * @param msgToDisplay message to display
	 */
	private void displayMsg(final String msgToDisplay) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				_chatArea.append("\n"+msgToDisplay);
			}
		});
	}
	
	/**
	 * @param editable <ul>true <strong>if</strong> <li>the server's text field need to be editable</li> 
	 * 			<strong>else</strong><li> false</li> 
	 */
	private void setTextFieldEditable(final boolean editable) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				_msgField.setEditable(editable);
			}
		});
	}
	
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		ChatServer srv = new ChatServer();
		srv.runServer();
	}
}
