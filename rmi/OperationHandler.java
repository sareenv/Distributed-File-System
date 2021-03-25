package rmi;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;

class OperationHandler<T> extends Thread implements Serializable {

    private Socket socket;
    private final T remoteInterfaceImplementation;
    private final Class<T> remoteInterface;
    private ObjectOutputStream objectOutputStream = null;
    private ObjectInputStream objectInputStream;

    OperationHandler(Socket socket, T remoteInterfaceImplementation, Class<T> remoteInterface) throws IOException {
        if(socket == null) { throw new Error("Socket cannot be null"); }
        this.socket = socket;
        this.remoteInterface = remoteInterface;
        // implementation class of the specified interface of the skeleton.
        this.remoteInterfaceImplementation = remoteInterfaceImplementation;
        setupStreams();
    }

     public synchronized void setupStreams() throws IOException {
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectOutputStream.flush();
        objectInputStream = new ObjectInputStream(socket.getInputStream());
    }

    public synchronized void communication() throws InvocationTargetException, IOException {
        try {
            InternalRemoteMethod interRemoteMethod = (InternalRemoteMethod) objectInputStream.readObject();
            String remoteMethodName = interRemoteMethod.getMethodName();
            Class[] argsType = interRemoteMethod.getArgsType();
            Object[] args = interRemoteMethod.getArgs();
            Method methodToImplement = remoteInterface.getMethod(remoteMethodName, argsType);
            Object methodCall;
            methodCall = methodToImplement.invoke(remoteInterfaceImplementation, args);
            objectOutputStream.writeObject(methodCall);
            this.socket.close();
        } catch (InvocationTargetException e ){
            System.out.println("Error: " + e);
            if (objectOutputStream != null) {
                objectOutputStream.writeObject(e.getTargetException());
                socket.close();
            }
        } catch (NoSuchMethodException e) {
            System.out.println("Error: " + e);
        } catch (IOException e) {
            System.out.println("Error: " + e);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            System.out.println("Error: " + e);
            e.printStackTrace();
        } catch (ClassNotFoundException classNotFoundException) {
            classNotFoundException.printStackTrace();
        } finally {
            objectInputStream.close();
            objectOutputStream.close();
            this.notifyAll();
        }

    }


    @Override
    public void run() {
        super.run();
        try {
            communication();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}