package proto_file.client;

import javafx.application.Application;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.concurrent.CountDownLatch;

public class MainClient extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception{
        start();
        Parent root = FXMLLoader.load(getClass().getResource("/mainPanel.fxml"));
        primaryStage.setTitle("Data store");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setOnCloseRequest((EventHandler) event -> ProtoClient.getInstance().stop());
        primaryStage.show();
    }

    private void start() {
        CountDownLatch networkStarter = new CountDownLatch(1);
        ProtoClient client = ProtoClient.getInstance();
        ClientHandler clientHandler = new ClientHandler();
        client.addHandler(clientHandler);
        new Thread(() -> client.start(networkStarter)).start();
        try {
            networkStarter.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
