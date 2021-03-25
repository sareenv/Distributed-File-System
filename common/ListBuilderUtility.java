
package common;

import java.io.File;
import java.util.LinkedList;

class ListBuilderUtility {
    // default constructor.
    public ListBuilderUtility() { }

    public LinkedList<Path> buildFileLinkedList(Path basePath, File dir)
    {

        LinkedList<Path> pathList = new LinkedList<>();
        File[] files = dir.listFiles();
        for(File file : files){
            if(file.isFile()){
                pathList.add(new Path(basePath, file.getName()));
            }
            else if (file.isDirectory()){
                pathList.addAll(buildFileLinkedList(new Path(basePath, file.getName()), file));
            }
        }
        return pathList;
    }
}