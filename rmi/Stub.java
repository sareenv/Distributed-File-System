package rmi;

import java.lang.reflect.Proxy;
import java.net.*;

/** RMI stub factory.

 <p>
 RMI stubs hide network communication with the remote server and provide a
 simple object-like interface to their users. This class provides methods for
 creating stub objects dynamically, when given pre-defined interfaces.

 <p>
 The network address of the remote server is set when a stub is created, and
 may not be modified afterwards. Two stubs are equal if they implement the
 same interface and carry the same remote server address - and would
 therefore connect to the same skeleton. Stubs are serializable.
 */
public abstract class Stub
{

    private static InterfaceCheck check;

    /** Creates a stub, given a skeleton with an assigned adress.

     <p>
     The stub is assigned the address of the skeleton. The skeleton must
     either have been created with a fixed address, or else it must have
     already been started.

     <p>
     This method should be used when the stub is created together with the
     skeleton. The stub may then be transmitted over the network to enable
     communication with the skeleton.

     @param c A <code>Class</code> object representing the interface
     implemented by the remote object.
     @param skeleton The skeleton whose network address is to be used.
     @return The stub created.
     @throws IllegalStateException If the skeleton has not been assigned an
     address by the user and has not yet been
     started.
     @throws UnknownHostException When the skeleton address is a wildcard and
     a port is assigned, but no address can be
     found for the local host.
     @throws NullPointerException If any argument is <code>null</code>.
     @throws Error If <code>c</code> does not represent a remote interface
     - an interface in which each method is marked as throwing
     <code>RMIException</code>, or if an object implementing
     this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, Skeleton<T> skeleton)
            throws UnknownHostException
    {
        if(c.equals(null) || skeleton.equals(null)) { throw new NullPointerException("values cannot be null ");}
        InetSocketAddress address = skeleton.socketAddress();
        if(address == null) { throw new IllegalStateException("Invalid socket address"); }
        if(c.isInterface() == false) { throw new Error("Invalid Interface"); }
        if(check == null) { check = new InterfaceCheck();}
        if(check.isValidRemoteInterface(c) == false) { new RMIException("Invalid remote Interface");}
        if(address.equals(null)) { throw new IllegalStateException(); }
        return create(c, address);
    }


    /** Creates a stub, given a skeleton with an assigned address and a hostname
     which overrides the skeleton's hostname.

     <p>
     The stub is assigned the port of the skeleton and the given hostname.
     The skeleton must either have been started with a fixed port, or else
     it must have been started to receive a system-assigned port, for this
     method to succeed.

     <p>
     This method should be used when the stub is created together with the
     skeleton, but firewalls or private networks prevent the system from
     automatically assigning a valid externally-routable address to the
     skeleton. In this case, the creator of the stub has the option of
     obtaining an externally-routable address by other means, and specifying
     this hostname to this method.

     @param c A <code>Class</code> object representing the interface
     implemented by the remote object.
     @param skeleton The skeleton whose port is to be used.
     @param hostname The hostname with which the stub will be created.
     @return The stub created.
     @throws IllegalStateException If the skeleton has not been assigned a
     port.
     @throws NullPointerException If any argument is <code>null</code>.
     @throws Error If <code>c</code> does not represent a remote interface
     - an interface in which each method is marked as throwing
     <code>RMIException</code>, or if an object implementing
     this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, Skeleton<T> skeleton,
                               String hostname)
    {
        if (c == null || hostname == null || skeleton == null) { throw new NullPointerException("One of the parameter is null"); }
        if(check == null) { check = new InterfaceCheck();}
        if(check.isValidRemoteInterface(c) == false) { new RMIException("Invalid remote Interface");}
        if(skeleton.socketAddress() == null) { throw new IllegalStateException("address and port has not been assigned"); }
        int port = skeleton.socketAddress().getPort();
        // InetSocketAddress(host, port);
        InetSocketAddress address = new InetSocketAddress(hostname, port);
        return create(c, address);
    }

    /** Creates a stub, given the address of a remote server.

     <p>
     This method should be used primarily when bootstrapping RMI. In this
     case, the server is already running on a remote host but there is
     not necessarily a direct way to obtain an associated stub.

     @param c A <code>Class</code> object representing the interface
     implemented by the remote object.
     @param address The network address of the remote skeleton.
     @return The stub created.
     @throws NullPointerException If any argument is <code>null</code>.
     @throws Error If <code>c</code> does not represent a remote interface
     - an interface in which each method is marked as throwing
     <code>RMIException</code>, or if an object implementing
     this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, InetSocketAddress address)
    {
        if(check == null) { check = new InterfaceCheck();}
        if(c.isInterface() == false) { throw new Error("RMI Exception"); }
        if(check.isValidRemoteInterface(c) == false) { throw new Error("RMI Exception: No remote interface found");}
        if(c == null || address == null) { throw new NullPointerException("interface or address is null ! "); }
        // invoke the remote object over the socket request.
        // Reference1 : Tip from lab slide to use Proxy class and Invocation Handler to invoke functions. -
        // Reference2 : https://dzone.com/articles/java-dynamic-proxy - Explains the concept of using the invocation handler and Proxy.
        InvokeHandler handler = new InvokeHandler(address, c);
        return (T) Proxy.newProxyInstance(c.getClassLoader(), new Class<?>[] {c}, handler);
    }
}
