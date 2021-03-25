package storage;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;

import common.*;
import rmi.*;
import naming.*;

/** Storage server.

    <p>
    Storage servers respond to client file access requests. The files accessible
    through a storage server are those accessible under a given directory of the
    local filesystem.
 */
public class StorageServer implements Storage, Command
{

    private File root;
    private boolean isCommandServerRunning = false;
    private boolean isStorageServerRunning = false;
    private Skeleton<Storage> storageSkeleton;
    private Skeleton<Command> commandSkeleton;
    private Integer serverPort;

    /** Creates a storage server, given a directory on the local filesystem.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
    */
    public StorageServer(File root)
    {
        if(root.equals(null)) { throw new NullPointerException("root is provided as null"); }
        this.root = root;
    }

    public synchronized void registerStubs(String host, Registration namingServer, Path[] paths) throws UnknownHostException {
        try {
            Storage storageStub = Stub.create(Storage.class, storageSkeleton, host);
            Command command = Stub.create(Command.class, commandSkeleton, host);
            namingServer.register(storageStub, command, paths);
        } catch (Exception e) {
            throw new UnknownHostException("Stubs cannot be connected to the");
        }
    }


    /** Starts the storage server and registers it with the given naming
        server.

        @param hostname The externally-routable hostname of the local host on
                        which the storage server is running. This is used to
                        ensure that the stub which is provided to the naming
                        server by the <code>start</code> method carries the
                        externally visible hostname or address of this storage
                        server.
        @param naming_server Remote interface for the naming server with which
                             the storage server is to register.
        @throws UnknownHostException If a stub cannot be created for the storage
                                     server because a valid address has not been
                                     assigned.
        @throws FileNotFoundException If the directory with which the server was
                                      created does not exist or is in fact a
                                      file.
        @throws RMIException If the storage server cannot be started, or if it
                             cannot be registered.
     */
    public synchronized void start(String hostname, Registration naming_server)
        throws RMIException, UnknownHostException, FileNotFoundException
    {

        try {
            // if the host name contains the port also.
            URI host = new URI(hostname);
            this.serverPort = host.getPort();

            InetSocketAddress address = new InetSocketAddress(this.serverPort);
            this.storageSkeleton = new Skeleton<Storage>(Storage.class, this, address);
            this.commandSkeleton = new Skeleton<Command>(Command.class, this, address);
        } catch (URISyntaxException e) {
            // only mentions the hostname and no port given when creating the skeleton.
            this.storageSkeleton = new Skeleton<Storage>(Storage.class, this);
            this.commandSkeleton = new Skeleton<Command>(Command.class, this);
        }
        try {
            this.storageSkeleton.start();
            this.commandSkeleton.start();
            isStorageServerRunning = true;
            isCommandServerRunning = true;
        } catch (Exception e) {
            throw new RMIException("Error Starting the servers/skeleton");
        }
        try {
            Path [] pathLocations = Path.list(root);
            registerStubs(hostname, naming_server, pathLocations);
        } catch (Exception e) {
            throw new UnknownHostException();
        }

    }

    /** Stops the storage server.

        <p>
        The server should not be restarted.
     */
    public void stop()
    {
        this.storageSkeleton.stop();
        this.commandSkeleton.stop();
        this.isCommandServerRunning = false;
        this.isStorageServerRunning = false;
        stopped(null);
    }

    /** Called when the storage server has shut down.

        @param cause The cause for the shutdown, if any, or <code>null</code> if
                     the server was shut down by the user's request.
     */
    protected void stopped(Throwable cause)
    {
    }


    public synchronized boolean fileExistingChecks(File file1) {
        if (file1.isFile() == false || file1.exists() == false) { return false; }
        return true;
    }


    // The following methods are documented in Storage.java.
    @Override
    public synchronized long size(Path file) throws FileNotFoundException {
        // Reference: journaldev.com/839/java-get-file-size - getting the file size in java in bytes.
        File file1 = file.toFile(root);
        if (!fileExistingChecks(file1)) { throw new FileNotFoundException(); }
        long fileSize = 0;
        try {
            FileChannel channel = FileChannel.open(file1.toPath());
            fileSize = channel.size();
            channel.close();
        } catch (IOException e) { }

        return fileSize;
    }


    @Override
    public synchronized byte[] read(Path file, long offset, int length)
        throws FileNotFoundException, IOException
    {
        byte[] bytes = new byte[length];
        File file1 = file.toFile(root);
        if (!fileExistingChecks(file1)) { throw new FileNotFoundException(); }
        if(offset < 0) { throw new IndexOutOfBoundsException("Offset cannot be negative"); }
        boolean canReadFile = file1.canRead();
        if(canReadFile == false) { throw new IOException("File cannot be read"); }
        String filePathString = file1.toPath().toString();
        FileInputStream stream = new FileInputStream(filePathString);
        Integer offsetIntValue = (int) offset;
        stream.read(bytes, offsetIntValue, length);
        stream.close();
        return bytes;
    }

    // need to implement this method now.
    @Override
    public synchronized void write(Path file, long offset, byte[] data)
        throws FileNotFoundException, IOException
    {
        File file1 = file.toFile(root);
        if (!fileExistingChecks(file1)) { throw new FileNotFoundException(); }
        if(offset < 0) { throw new IndexOutOfBoundsException("Offset cannot be negative"); }
        boolean canWriteFile = file1.canWrite();
        if(canWriteFile == false) { throw new IOException("Cannot write the data"); }
        FileOutputStream stream = new FileOutputStream(file1.toPath().toString());
        Integer offsetIntValue = (int) offset;
        stream.write(data, offsetIntValue, data.length);
        return;
    }

    // The following methods are documented in Command.java.
    @Override
    public synchronized boolean create(Path file)
    {
        // check if the path is the root - This path may not be the root directory.
        boolean isParentPath = false;
        isParentPath = file.isRoot();
        if(isParentPath == true) { throw new Error("This path cannot be a root dir path"); }
        File file1 = file.toFile(root);
        File parent = file1.getParentFile();
        // Reference: https://www.geeksforgeeks.org/file-mkdir-method-in-java-with-examples/
        // checks if the directory can be made here.
        if (parent.mkdirs()) {
            try {
                file1.createNewFile();
                return true;
            } catch (IOException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public synchronized boolean delete(Path path)
    {
        throw new UnsupportedOperationException("not implemented");
    }
}
