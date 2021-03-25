package rmi;


import java.lang.reflect.Method;

public class InterfaceCheck<T> {

    private Class<T> remoteInterface;

    // empty constructor.
    public InterfaceCheck() {}

    public InterfaceCheck(Class<T> remoteInterface) {
        this.remoteInterface = remoteInterface;
    }

    public boolean isRemoteMethod(Method method, Class<T> remoteInterface) {
        if(this.remoteInterface == null) {
            this.remoteInterface = remoteInterface;
        }
        Method[] remoteMethods = remoteInterface.getMethods();
        for(Method remoteMethod: remoteMethods) {
            if(method.equals(remoteMethod)) { return true; }
        }
        return false;
    }

    public boolean isValidRemoteInterface(Class<T> remoteInterface) {
        boolean isValidInterface = false;
        Method[] methods = remoteInterface.getMethods();
        for(Method method: methods) {
            String methodString = method.toString();
            if (methodString.contains("RMIException") == false) {
                return false;
            }
            isValidInterface = true;
        }
        return isValidInterface;
    }
}
