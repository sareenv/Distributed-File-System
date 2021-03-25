package rmi;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ConcurrencyHandler<T> extends Thread implements Serializable {
    /*
    private attributes for the concurrency class.
    * */

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private ServerSocket serverSocket;
    private Socket socket;
    private final Skeleton<?> lock;
    private InetSocketAddress address;
    private Thread operationThread;
    private T remoteInterfaceImplementation;
    private Class<T> remoteInterface;

    /*Constructor method for the Concurrency Handler*/
    public ConcurrencyHandler(Class<T> remoteInterface, T remoteInterfaceImplementation,
                              Skeleton<?> lock, InetSocketAddress address) {
        this.lock = lock;
        this.address = address;
        this.remoteInterfaceImplementation = remoteInterfaceImplementation;
        this.remoteInterface = remoteInterface;
    }
    public synchronized InetSocketAddress getAddress() {
        return this.address;
    }
    public synchronized void settingSocketServer() throws IOException {
        ServerSocket server;
        if (this.address != null) {
            server = new ServerSocket();
            server.setReuseAddress(true);
            this.serverSocket = server;
        }
        else {
            server = new ServerSocket(0);
            this.serverSocket = server;
            this.address = (InetSocketAddress) server.getLocalSocketAddress();
        }
    }
    // thread entry point for executing code
    @Override
    public void run() {
        super.run();

        synchronized (lock) {
            try {
                settingSocketServer();
                lock.notifyAll();
                if(this.address != null) { this.serverSocket.bind(address); }
            } catch (NullPointerException | IOException e) {
                System.out.print(e.getMessage() + "\n");
            }
        }
        while(true) {
            try {
                this.socket = this.serverSocket.accept();
				/* The operation thread receives method name and parameters then
				    invokes the method and send the results over the socket. */
                operationThread = new OperationHandler(socket, remoteInterfaceImplementation, remoteInterface);
                operationThread.start();
            } catch (IOException e) { }
            // breaks the while loop if the condition is not true.
            synchronized(lock) { if(lock.isRunning != true) { return; } }
        }
    }
    public synchronized void stopThread() {
        // if the thread is already stop return;
        if(lock.isRunning == false) { return; }
        lock.isRunning = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            lock.isRunning = true;
        }
    }
}
