package andrei.dynamic.client;

import andrei.dynamic.common.Address;
import andrei.dynamic.common.DirectoryManager;
import andrei.dynamic.common.MessageFactory;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.CopyOption;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 *
 * @author Andrei
 */
public class ServerImage {

    private final Address controlAddress;
    private final Address dataAddress;
    private final ClientConnection parent;
    private final DirectoryManager dir;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private State state;
    private boolean keepWorking;

    public ServerImage(final ClientConnection parent,
	    final Address controlAddress, final Address dataAddress,
	    final DirectoryManager dir, final String authToken) {
	this.parent = parent;
	this.controlAddress = controlAddress;
	this.dataAddress = dataAddress;
	this.dir = dir;
	keepWorking = true;
	state = State.INIT;
    }

    public void initConnection() throws UnknownHostException, IOException {

	socket = new Socket(controlAddress.getHost(), controlAddress.getPort());

	try {
	    if (!socket.isBound() || !socket.isConnected()) {
		throw new SocketException(
			"control socket not bound in server image");
	    }

	    try {
		socket.setTcpNoDelay(true);
	    } catch (Exception ex) {
		System.err.println("could not disable nagle algorithm");
	    }
	    try {
		socket.setSoTimeout(5000);
	    } catch (Exception e) {
		System.err.println("could not enable socket reading timeout");
	    }

	    out = new DataOutputStream(socket.getOutputStream());
	    in = new DataInputStream(socket.getInputStream());

	    while (keepWorking) {

		state = State.WAITING_UPDATE;
		MessageFromServer message = null;

		try {

		    message = getMessage();
		    state = State.UPDATING;

		    switch (message.getType()) {
			case CREATE_FILE_MESSAGE:
		    System.out.println("created " + new String(message.getContent()));
			    state = State.STARTING_DOWNLOAD;
			    downloadFile(trim(new String(message.getContent())));
			    break;
			case MODIFY_FILE_MESSAGE:
		    System.out.println("modified " + new String(message.getContent()));
			    state = State.STARTING_DOWNLOAD;
			    downloadFile(trim(new String(message.getContent())));
			    break;
			case DELETE_FILE_MESSAGE:
		    System.out.println("deleted " + new String(message.getContent()));
			    deleteFile(trim(new String(message.getContent())));
			    break;
			case TEST_MESSAGE:
			    sendTestResponse();
			    break;
		    }

		} catch (EOFException ex) {
		    stop();
		    System.out.println("reached EOF");
		} catch (SocketException ex) {
		    stop();
		    System.err.println("socket exception: " + ex.getMessage());
		    ex.printStackTrace(System.err);
		} catch (Exception ex) {
		    if (message != null) {
			System.err.println("failed processing " + message.
				getType() + ": " + ex.getMessage());
		    } else {
			System.err.println("failed processing: " + ex.
				getMessage());
		    }
		    ex.printStackTrace(System.err);
		}

	    }
	} catch (Exception ex) {
	    throw ex;
	} finally {
	    if (socket != null) {
		try {
		    socket.close();
		} catch (Exception ex) {
		    System.err.println("failed to close tcp socket");
		}
	    }

	    socket = null;
	    in = null;
	    out = null;

	}
    }

    private void sendTestResponse() throws Exception {
	out.write(MessageFactory.newTestResponseMessage());
    }

    public void stop() {
	keepWorking = false;
    }

    public void disconnect() throws IOException {
	final StringBuilder exceptionMessage = new StringBuilder();
	try {
	    out.close();
	} catch (Exception ex) {
	    exceptionMessage.append(ex.getMessage()).append(" ; ");
	    throw ex;
	} finally {
	    try {
		in.close();
	    } catch (Exception ex) {
		exceptionMessage.append(ex.getMessage()).append(" ; ");
		throw ex;
	    } finally {
		try {
		    socket.close();
		} catch (Exception ex) {
		    exceptionMessage.append(ex.getMessage()).append(" ; ");
		    throw ex;
		}
	    }
	    //TODO fa ceva cu mesajele de exceptie
	}
    }

    private MessageFromServer getMessage() throws Exception {

	Byte type = null;

	while (type == null) {
	    if (!keepWorking) {
		throw new TerminatedException("received shutdown signal");
	    }

	    try {
		type = in.readByte();
	    } catch (SocketTimeoutException ex) {
		//nimic
	    }
	}

	int counter = 0;
	byte[] buff = new byte[130];
	while (counter < 128) {
	    try {
		counter += in.read(buff, counter, 128 - counter);
	    } catch (SocketTimeoutException ex) {
		//nimic
	    }

	}

	return new MessageFromServer(type, buff);

    }

    private void downloadFile(final String relative) throws Exception {

	Path original = FileSystems.getDefault().getPath(dir.getAbsolutePath(),
		relative);
	Path temp = dir.getTempFilePath(relative);
	Socket dataSocket = null;

	while (keepWorking) {
	    try {
		dataSocket = new Socket(dataAddress.getHost(),
			dataAddress.getPort());
		break;
	    } catch (Exception ex) {
		//nimic
		System.err.println("failed connecting to data channel");
	    }
	}

	if (dataSocket == null || !dataSocket.isConnected()) {
	    throw new SocketException("data socket not bound in server image");
	}

	DataOutputStream writer = new DataOutputStream(new FileOutputStream(
		temp.toString()));

	state = State.DOWNLOADING;

	BufferedInputStream dataStream = new BufferedInputStream(dataSocket.
		getInputStream());

	final byte[] buff = new byte[1024 * 4];
	try {
	    while (true) {
		try {
		    int read = dataStream.read(buff);
		    if (read == -1) {
			break;
		    }
		    writer.write(buff, 0, read);
		} catch (EOFException ex) {
		    break;
		}
	    }

	    writer.close();
	    if (!Files.exists(original.getParent())) {
		Files.createDirectories(original.getParent());
	    }
	    Files.deleteIfExists(original);
	    //(new File(temp.toString())).renameTo(new File(original.toString()));
	    Files.move(temp, original, REPLACE_EXISTING);
	} finally {
	    try {
		dataStream.close();
	    } catch (Exception ex1) {
		//nimic
	    }

	    try {
		dataSocket.close();
	    } catch (Exception ex1) {
		//nimic
	    }
	}
    }

    private boolean deleteFile(final String relative) throws Exception {
	Path absolute = FileSystems.getDefault().getPath(dir.getAbsolutePath(),
		relative.trim());

	return Files.deleteIfExists(absolute);
    }

    private String trim(final String input) {
	final String temp = input.trim();
	if (temp.charAt(0) == '/' || temp.charAt(0) == '\\') {
	    return temp.substring(1).trim();
	}

	return temp;
    }

    private static enum State {
	INIT,
	WAITING_UPDATE,
	DOWNLOADING,
	CLOSING,
	UPDATING,
	STARTING_DOWNLOAD
    }
}
