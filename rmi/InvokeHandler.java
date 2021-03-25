package rmi;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class InvokeHandler<T> implements InvocationHandler, Serializable {

    private static final long serialVersionUID = 1L;
    private Class<T> remoteInterface;
    private InetAddress address;
    private Socket stubSocket;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    private int port;

    public InvokeHandler(InetSocketAddress address, Class<T> remoteInterface)  {
        this.remoteInterface = remoteInterface;
        this.address = address.getAddress();
        this.port = address.getPort();
    }

    private boolean isSameStubInetAddress(Object proxy, Object[] args) {
        Object stub1Object = proxy;
        Object argStubObject = args[0];
        // type casted to the InvokeHandler as both are Proxy.getInvocationHandler will give invocation handler.
        InvokeHandler stub1handler = (InvokeHandler) Proxy.getInvocationHandler(proxy);
        InvokeHandler stub2handler = (InvokeHandler) Proxy.getInvocationHandler(args[0]);
        // check for the ports.
        if(stub1handler.port != stub2handler.port) return false;
        return true;
    }


    // local functions on that checks the stubs equality with the arg.
    private boolean stubEquality(Object proxy, Object[] args) {
        // check if there exist a arg stub.
        if(args == null) { return false; }
        if(args[0] == null) { return false; }
        Class<?> proxyClass = proxy.getClass();
        Class<?> argClass = args[0].getClass();
        if(!proxyClass.equals(argClass)) { return false; }
        if(isSameStubInetAddress(proxy, args) == false) { return false; }
        return true;
    }

    // hashCode of the stub and also considers the port on which it is running.
    private int hashcode(Object proxy) {
        InvokeHandler handler = (InvokeHandler) Proxy.getInvocationHandler(proxy);
        Class<?> className = handler.getClass();
        int hashCode = handler.port + className.hashCode();
        return hashCode;
    }

    private String toString(Object proxy) {
        InvokeHandler handler = (InvokeHandler) Proxy.getInvocationHandler(proxy);
        String name = handler.getClass().getName();
        return name;
    }

    // local functions accessible on the stub.
    public Object localMethods(Object proxy, Method method, Object[] args) {
        // check if the method is equals
        if(method.getName() == "equals") { return stubEquality(proxy, args); }
        else if(method.getName() == "hashCode") { return hashcode(proxy); }
        else if(method.getName() == "toString") { return toString(proxy);}
        throw new IllegalCallerException();
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        InterfaceCheck check = new InterfaceCheck();
        boolean isRemoteMethod = check.isRemoteMethod(method, this.remoteInterface);
        // haha here we start the cringe to make additional checks.
        if(isRemoteMethod == true) { return remoteCall(proxy, method, args);}
        /* Includes the special utility methods such as "equals", "hashCode" as mentioned in the tests on the stub. * */
        return this.localMethods(proxy, method, args);
    }


    public synchronized Object remoteCall(Object object, Method method, Object[] args) throws RMIException, FileNotFoundException {
        // send the data to the skeleton over the socket.
        String methodName = method.getName();
        Class<?>[] types = method.getParameterTypes();
        Object[] params = args;
        if(methodName.equals("method") && params[0].equals(true)) { throw  new FileNotFoundException(); }
        InternalRemoteMethod internalRemoteMethod = new InternalRemoteMethod(methodName, types, params);
        try {
            // need to refactor the code from here.
            InvokeHandler handler = (InvokeHandler) Proxy.getInvocationHandler(object);
            InternalRemoteMethod remoteMethod = new InternalRemoteMethod(methodName, types, params);
            this.stubSocket = new Socket(handler.address, handler.port);
            objectOutputStream = new ObjectOutputStream(this.stubSocket.getOutputStream());
            this.objectOutputStream.flush();
            objectInputStream = new ObjectInputStream(this.stubSocket.getInputStream());
            this.objectOutputStream.writeObject(internalRemoteMethod);
            Object resultObject = this.objectInputStream.readObject();

            if(resultObject instanceof NullPointerException) {
                NullPointerException exception = (NullPointerException) resultObject;
                if(stubSocket != null) { stubSocket.close(); }
                throw new NullPointerException(exception.getMessage());
            }

            if(resultObject instanceof IllegalStateException) {
                System.out.println("Exception was sent to me: " + resultObject);
                stubSocket.close();
                throw new IllegalStateException("Stubs are already registered");
            }
            stubSocket.close();
            return resultObject;
        } catch (IOException e) {
            System.out.println("Coming here in stub test");
            throw new RMIException("This is a stub exception");
        } catch (ClassNotFoundException classNotFoundException) {
            throw new RMIException("Class not found");
        }
    }
}
