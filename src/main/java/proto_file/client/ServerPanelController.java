package proto_file.client;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class ServerPanelController implements Initializable {
    @FXML
    TableView<FileInfo> filesTableServer;

//    @FXML
//    ComboBox<String> disksBox;

    @FXML
    TextField pathField;

    private AuthenticationService authenticationService = AuthenticationService.getInstance();


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setOnLoadPathsListener();

        TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Имя");
        filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        filenameColumn.setPrefWidth(240);

        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Размер");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(
                param.getValue().isDirectory() ? 0 : param.getValue().getSize()));
        fileSizeColumn.setCellFactory(column -> new TableCell<FileInfo, Long>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else if (item == 0) {
                    setText("Directory");
                } else {
                    String text = String.format("%,d bytes", item);
                    setText(text);
                }
            }
        });
        fileSizeColumn.setPrefWidth(120);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Дата изменения");
        fileDateColumn.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getLastModified() == null ? null :
                        param.getValue().getLastModified().format(dtf)));
        fileDateColumn.setPrefWidth(120);

        filesTableServer.getColumns().addAll(filenameColumn, fileSizeColumn, fileDateColumn);
        filesTableServer.getSortOrder().add(filenameColumn);

//        disksBox.getItems().clear();
//        for (Path p : FileSystems.getDefault().getRootDirectories()) {
//            disksBox.getItems().add(p.toString());
//        }
//        disksBox.getSelectionModel().select(0);

        filesTableServer.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2 &&
                        filesTableServer.getSelectionModel().getSelectedItem() != null) {
                    if (filesTableServer.getSelectionModel()
                            .getSelectedItem().isDirectory()) {
                        String path = (Paths.get(pathField.getText())
                                        .resolve(filesTableServer.getSelectionModel()
                                                .getSelectedItem().getFilename()).toString());
                        updateList(path);
                    }
                }
            }
        });
    }

    void updateList(String path) {
        UserInfo userInfo = authenticationService.getUserInfo();
        String fullPath = userInfo.getLogin().concat(path);
        try {
            ServerCommandService.loadPathFilesList(fullPath, userInfo.getLogin(), userInfo.getPassword());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setOnLoadPathsListener() {
        ClientHandler clientHandler = ProtoClient.getInstance()
                .getCurrentChannel().pipeline().get(ClientHandler.class);
        clientHandler.setOnLoadFilesPathsListener(this::renderServerSide);
    }

    private void renderServerSide(List<FileInfo> serverFiles) {
        pathField.setText(Paths.get("/").resolve(serverFiles.get(0).getPath()).toString());
        filesTableServer.getItems().clear();
        for (int i = 1; i < serverFiles.size(); i++) {
            filesTableServer.getItems().add(serverFiles.get(i));
        }
        filesTableServer.sort();
    }

    public void btnPathUpAction(ActionEvent actionEvent) {
        String upperPath = pathField.getText().substring(0, pathField.getText()
                        .lastIndexOf(Paths.get("/").toString()) + 1);
        updateList(upperPath);
    }

//    public void selectDiskAction(ActionEvent actionEvent) {
//        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
//        updateList(Paths.get(element.getSelectionModel().getSelectedItem()));
//    }

    String getSelectedFilename() {
        if (!filesTableServer.isFocused()) {
            return null;
        }
        return filesTableServer.getSelectionModel().getSelectedItem().getFilename();
    }

    String getCurrentPath() {
        return pathField.getText();
    }
}
