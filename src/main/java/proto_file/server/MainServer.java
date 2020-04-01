package proto_file.server;

public class MainServer {
    public static void main(String[] args) {
        try {
            new ProtoServer().run();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
