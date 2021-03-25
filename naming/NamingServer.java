package naming;

import java.io.*;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

import rmi.*;
import common.*;
import storage.*;

/** Naming server.

    <p>
    Each instance of the filesystem is centered on a single naming server. The
    naming server maintains the filesystem directory tree. It does not store any
    file data - this is done by separate storage servers. The primary purpose of
    the naming server is to map each file name (path) to the storage server
    which hosts the file's contents.

    <p>
    The naming server provides two interfaces, <code>Service</code> and
    <code>Registration</code>, which are accessible through RMI. Storage servers
    use the <code>Registration</code> interface to inform the naming server of
    their existence. Clients use the <code>Service</code> interface to perform
    most filesystem operations. The documentation accompanying these interfaces
    provides details on the methods supported.

    <p>
    Stubs for accessing the naming server must typically be created by directly
    specifying the remote network address. To make this possible, the client and
    registration interfaces are available at well-known ports defined in
    <code>NamingStubs</code>.
 */


public class NamingServer implements Service, Registration
{
    private boolean isRunning = false;
    private boolean isStopped = false;
    private Skeleton<Registration> registrationSkeleton;
    private Skeleton<Service>  serviceSkeleton;
    private ServerStubs stub = null;
    private ArrayList<ServerStubs> stubs;
    private ArrayList<Path> registeredPaths;
    private PathNode node;

    public NamingServer()
    {
        this.registrationSkeleton= new Skeleton<Registration>(Registration.class, this, new InetSocketAddress(NamingStubs.REGISTRATION_PORT));
        this.serviceSkeleton = new Skeleton<Service>(Service.class, this, new InetSocketAddress(NamingStubs.SERVICE_PORT));
        this.stubs = new ArrayList<>();
        this.registeredPaths = new ArrayList<>();
    }

    /** Starts the naming server.

        <p>
        After this method is called, it is possible to access the client and
        registration interfaces of the naming server remotely.

        @throws RMIException If either of the two skeletons, for the client or
                             registration server interfaces, could not be
                             started. The user should not attempt to start the
                             server again if an exception occurs.
     */
    public synchronized void start() throws RMIException
    {
        if(isStopped || isRunning) { throw new RMIException("Either naming server is stopped or already started! "); }
        try {
            registrationSkeleton.start();
            serviceSkeleton.start();
            isRunning = true;
        } catch (RMIException e) {
            System.out.println(e);
            throw new RMIException(e);
        }
    }



    /** Stops the naming server.

        <p>
        This method waits for both the client and registration interface
        skeletons to stop. It attempts to interrupt as many of the threads that
        are executing naming server code as possible. After this method is
        called, the naming server is no longer accessible remotely. The naming
        server should not be restarted.
     */
    public void stop()
    {
        changeVoidStatus();
        try {
            registrationSkeleton.stop();
            serviceSkeleton.stop();
            stopped(null);
        }catch (Throwable t){
            stopped(t);
        }

    }

    /** Indicates that the server has completely shut down.

        <p>
        This method should be overridden for error reporting and application
        exit purposes. The default implementation does nothing.

        @param cause The cause for the shutdown, or <code>null</code> if the
                     shutdown was by explicit user request.
     */
    protected void stopped(Throwable cause)
    {
    }


    @Override
    public boolean isDirectory(Path path) throws FileNotFoundException
    {

        if(path == null) { throw new NullPointerException(); }
        // it is a file
        if(this.registeredPaths.contains(path.toString())) { return false; }
        ArrayList<String> dirs = new ArrayList<>();
        for(Path regPath: registeredPaths) {
            String regPathFileString = regPath.last();
            String regPathDirString = regPath.toString().replace(regPathFileString, "");
            if(dirs.contains(regPathDirString) == false) { dirs.add(regPathDirString); }
        }
        String requestedPath = path.toString() + "/";
        if(path.isRoot()) { return true; }
        if(dirs.contains(requestedPath) == false) { throw new FileNotFoundException(); }
        return true; // change this
    }

    @Override
    public String[] list(Path directory) throws FileNotFoundException
    {
        System.out.println(directory);
        if(directory == null) { throw new NullPointerException("path cannot be null"); }

        boolean isExistingDir = sameFolderNameExists(directory.toString());
        return new String[0];
    }

    @Override
    public boolean createFile(Path file)
        throws RMIException, FileNotFoundException
    {
        try {
            return this.stub.commandStub.create(file);
        }catch (Exception e) {
            throw new RMIException("Exception");
        }
    }

    @Override
    public boolean createDirectory(Path directory) throws FileNotFoundException, RMIException {
        try {
            return this.stub.commandStub.create(directory);
        }catch (Exception e) {
            throw new RMIException("Exception creating the directory");
        }
    }

    @Override
    public boolean delete(Path path) throws FileNotFoundException, RMIException {
        throw new UnsupportedOperationException("");
    }

    @Override
    public Storage getStorage(Path file) throws FileNotFoundException
    {
        return this.stub.storageStub;
    }

    public synchronized void changeVoidStatus() {
        synchronized (this) {
            this.isStopped = true;
        }
    }



    public boolean sameFolderNameExists(String path) {
        // /hello/hello.txt
        for(Path p1: this.registeredPaths) {
            String registeredParent = Paths.get(p1.toString()).getParent().toString();
            if (registeredParent.contains(path)) {
                return true;
            }
        }
        return false;
    }

    public Path[] duplicatePaths(Path[] files) {
        ArrayList<Path> duplicatedPath = new ArrayList<Path>();
        for(Path path: files) {
            if(path.isRoot() == false) {
                String fileName = Paths.get(path.toString()).getFileName().toString();
                boolean sameFolder = sameFolderNameExists(fileName);
                if(sameFolder == true) {
                    duplicatedPath.add(path);
                }

                if(this.registeredPaths.contains(path) == true) {
                    duplicatedPath.add(path);
                } else {
                    this.registeredPaths.add(path);
                }
            }
        }
        Path[] duplicatedList = new Path[duplicatedPath.size()];
        for(int i = 0; i< duplicatedPath.size() ; i++) {
            duplicatedList[i] = duplicatedPath.get(i);
        }
        return duplicatedList;
    }



    // The method register is documented in Registration.java.
    @Override
    public Path[] register(Storage client_stub, Command command_stub,
                           Path[] files) {
        if(client_stub == null || command_stub == null || files == null) { throw new NullPointerException("Cannot be null"); }
        ServerStubs stub = new ServerStubs(client_stub, command_stub);
        boolean containsStub = stubs.contains(stub) ? true: false;
        if(containsStub == true) { throw new IllegalStateException("Stubs are already registered"); }
        this.stub = stub;
        this.stubs.add(stub);
        // Remove the duplicate paths.
        Path[] duplicatedPathsArray = duplicatePaths(files);
        return duplicatedPathsArray;
    }
}
