package proto_file.client;

public interface Listener<T> {
    void listen(T data);
}
