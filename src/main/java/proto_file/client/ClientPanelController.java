package proto_file.client;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ClientPanelController implements Initializable {
    @FXML
    TableView<FileInfo> filesTableClient;

//    @FXML
//    ComboBox<String> disksBox;

    @FXML
    TextField pathField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setOnLoadPathsListener();

        TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Имя");
        filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        filenameColumn.setPrefWidth(240);

        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Размер");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(
                Files.isDirectory(param.getValue().getPath()) ? 0 : param.getValue().getSize()));
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
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumn.setPrefWidth(120);

        filesTableClient.getColumns().addAll(filenameColumn, fileSizeColumn, fileDateColumn);
        filesTableClient.getSortOrder().add(filenameColumn);

//        disksBox.getItems().clear();
//        for (Path p : FileSystems.getDefault().getRootDirectories()) {
//            disksBox.getItems().add(p.toString());
//        }
//        disksBox.getSelectionModel().select(0);

        filesTableClient.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 &&
                    filesTableClient.getSelectionModel().getSelectedItem() != null) {
                Path path = Paths.get(pathField.getText()).resolve(filesTableClient.getSelectionModel()
                        .getSelectedItem().getFilename());
                if (Files.isDirectory(path)) {
                    updateList(path);
                }
            }
        });
    }

    void updateList(Path path) {
        try {
            pathField.setText(path.normalize().toAbsolutePath().toString());
            filesTableClient.getItems().clear();
            filesTableClient.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            filesTableClient.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "По какой-то причине не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();
        }
    }

    private void setOnLoadPathsListener() {
        ClientHandler clientHandler = ProtoClient.getInstance()
                .getCurrentChannel().pipeline().get(ClientHandler.class);
        clientHandler.setOnDownloadFilePathsListener((voidResult) -> updateList(Paths.get(getCurrentPath())));
    }

    public void btnPathUpAction(ActionEvent actionEvent) {
        Path upperPath = Paths.get(pathField.getText()).getParent();
        if (upperPath != null) {
            updateList(upperPath);
        }
    }

//    public void selectDiskAction(ActionEvent actionEvent) {
//        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
//        updateList(Paths.get(element.getSelectionModel().getSelectedItem()));
//    }

    String getSelectedFilename() {
        if (!filesTableClient.isFocused()) {
            return null;
        }
        return filesTableClient.getSelectionModel().getSelectedItem().getFilename();
    }

    String getCurrentPath() {
        return pathField.getText();
    }
}
