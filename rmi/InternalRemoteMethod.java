package rmi;


import java.io.Serializable;

public class InternalRemoteMethod<T> implements Serializable {
    private String methodName;
    private Class<T>[] argsType;
    private Object[] args;

    public String getMethodName() {
        return methodName;
    }

    public Class<T>[] getArgsType() {
        return argsType;
    }

    public Object[] getArgs() { return args; }

    InternalRemoteMethod(String methodName, Class<T> [] argsType, Object[] args) {
        this.methodName = methodName;
        this.argsType = argsType;
        this.args = args;
    }
}