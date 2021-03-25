package common;

import naming.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.*;

/** Distributed filesystem paths.

    <p>
    Objects of type <code>Path</code> are used by all filesystem interfaces.
    Path objects are immutable.

    <p>
    The string representation of paths is a forward-slash-delimeted sequence of
    path components. The root directory is represented as a single forward
    slash.

    <p>
    The colon (<code>:</code>) and forward slash (<code>/</code>) characters are
    not permitted within path components. The forward slash is the delimeter,
    and the colon is reserved as a delimeter for application use.
 */
public class Path implements Iterable<String>, Serializable
{

    private String basePath = "/";
    private String path;



    /** Creates a new path which represents the root directory. */
    public Path()
    {
        this.path = Paths.get(basePath).toString();
    }

    /** Creates a new path by appending the given component to an existing path.

        @param path The existing path.
        @param component The new component.
        @throws IllegalArgumentException If <code>component</code> includes the
                                         separator, a colon, or
                                         <code>component</code> is the empty
                                         string.
    */
    public Path(Path path, String component)
    {
        if(component.isEmpty() || component.contains(":") || component.contains("/")) {
            throw new IllegalArgumentException();
        }
        String basePath = path.path.toString();
        // normalised path removes any inconsistency in the path definition
        String normalisedPathString = Paths.get(basePath, component)
                .normalize().toString();
        this.path = normalisedPathString;
    }

    /** Creates a new path from a path string.

        <p>
        The string is a sequence of components delimited with forward slashes.
        Empty components are dropped. The string must begin with a forward
        slash.

        @param path The path string.
        @throws IllegalArgumentException If the path string does not begin with
                                         a forward slash, or if the path
                                         contains a colon character.
     */
    public Path(String path)
    {
        if(path.isEmpty() || path.charAt(0) != '/' || path.contains(":")) {
            throw new IllegalArgumentException();
        }
        this.path = Paths.get(path).normalize().toString();
    }

    /** Returns an iterator over the components of the path.

        <p>
        The iterator cannot be used to modify the path object - the
        <code>remove</code> method is not supported.

        @return The iterator.
     */
    
    @Override
    public Iterator<String> iterator()
    {
        List<String> iteratorArray = new ArrayList<String>();
        // iterator over the empty array.
        if (isRoot()) { return iteratorArray.iterator(); }
        String[] components = path.split("/");
        iteratorArray = Arrays.asList(components);
        // the above array also contains the base path and need to remove the first element
        int startIndex = 1;
        int endIndex = components.length;
        iteratorArray = iteratorArray.subList(startIndex, endIndex);
        Iterator<String> iterator = iteratorArray.iterator(); 
        return iterator;
    }



    public static LinkedList<Path> buildListUtility(File directory, Path prefix)
    {
        File[] listFile = directory.listFiles();
        LinkedList<Path> pathList = new LinkedList<>();
        for(File file : listFile){
            boolean isFile = file.isFile();
            boolean isDir = file.isDirectory();
            if(isFile){ pathList.add(new Path(prefix, file.getName())); }
            if (isDir){
                // if it is a dir then recursively make path and add to the
                LinkedList<Path> list = buildListUtility(file, new Path(prefix, file.getName()));
                pathList.addAll(list);
            }
        }
        return pathList;
    }


    /** Lists the paths of all files in a directory tree on the local
        filesystem.

        @param directory The root directory of the directory tree.
        @return An array of relative paths, one for each file in the directory
                tree.
        @throws FileNotFoundException If the root directory does not exist.
        @throws IllegalArgumentException If <code>directory</code> exists but
                                         does not refer to a directory.
     */


    public static Path[] list(File directory) throws FileNotFoundException
    {
        Path tempPath = new Path();
        int pathListSize = 0;
        LinkedList<Path> pathList = buildListUtility(directory, tempPath);
        pathListSize = pathList.size();
        // allocate the final array same as the size of the linked list of pathList.
        Path[] finalPaths = new Path[pathListSize];
        for (int i = 0; i< pathList.size(); i++){
            finalPaths[i] = pathList.get(i);
        }
        return finalPaths;
    }


    /** Determines whether the path represents the root directory.

        @return <code>true</code> if the path does represent the root directory,
                and <code>false</code> if it does not.
     */
    public boolean isRoot()
    {
        String path =  Paths.get(this.path).normalize().toString();
        String basePath = Paths.get(this.basePath).normalize().toString();
        if (path.equals(basePath)) {
            return true;
        }
        return false;
    }

    /** Returns the path to the parent of this path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no parent.
     */
    public Path parent()
    {
        if (isRoot()) { throw new IllegalArgumentException("The path is a root path"); }
        String parentPathLocation = Paths.get(this.path).normalize().getParent().toString();
        return new Path(parentPathLocation);
    }

    /** Returns the last component in the path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no last
                                         component.
     */
    public String last()
    {
        if (isRoot()) {
            throw new IllegalArgumentException("Path represents the root location path");
        }
        java.nio.file.Path pathLocation = Paths.get(this.path).normalize().getFileName();
        String lastPath = pathLocation.toString();
        return lastPath;
    }

    /** Determines if the given path is a subpath of this path.

        <p>
        The other path is a subpath of this path if is a prefix of this path.
        Note that by this definition, each path is a subpath of itself.

        @param other The path to be tested.
        @return <code>true</code> If and only if the other path is a subpath of
                this path.
     */
    public boolean isSubpath(Path other)
    {
        String originalPath = Paths.get(this.path).normalize().toString();
        if(originalPath.contains(other.toString())) {
            return true;
        }
        return false;
    }

    /** Converts the path to <code>File</code> object.

        @param root The resulting <code>File</code> object is created relative
                    to this directory.
        @return The <code>File</code> object.
     */
    public File toFile(File root)
    {
        return root;
    }

    /** Compares two paths for equality.

        <p>
        Two paths are equal if they share all the same components.

        @param other The other path.
        @return <code>true</code> if and only if the two paths are equal.
     */
    @Override
    public boolean equals(Object other)
    {
        if (other.toString().equalsIgnoreCase(this.path)){ return true; }
        return false;
    }

    /** Returns the hash code of the path. */
    @Override
    public int hashCode()
    {
        return this.path.hashCode();
    }

    /** Converts the path to a string.

        <p>
        The string may later be used as an argument to the
        <code>Path(String)</code> constructor.

        @return The string representation of the path.
     */
    @Override
    public String toString()
    {
        String finalPathString = this.path;
        return finalPathString;
    }

}
