package proto_file.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class MainPanelController implements Initializable {

    @FXML
    GridPane registrationPanel;

    @FXML
    GridPane authorizationPanel;

    @FXML
    HBox panelStorage;

    @FXML
    TextField loginFieldRegs;

    @FXML
    PasswordField passwordFieldRegs;

    @FXML
    TextField loginFieldAuth;

    @FXML
    PasswordField passwordFieldAuth;

    @FXML
    VBox clientPanel;

    @FXML
    VBox serverPanel;

    private AuthenticationService authenticationService = AuthenticationService.getInstance();

    private ClientPanelController clientController;
    private ServerPanelController serverController;

    private void setAuthorized(boolean isAuthorized) {
        if (isAuthorized) {
            registrationPanel.setVisible(false);
            registrationPanel.setManaged(false);
            authorizationPanel.setVisible(true);
            authorizationPanel.setManaged(true);
        } else {
            registrationPanel.setVisible(true);
            registrationPanel.setManaged(true);
            authorizationPanel.setVisible(false);
            authorizationPanel.setManaged(false);
            panelStorage.setVisible(false);
            panelStorage.setManaged(false);
        }
    }

    private void setCommunication(boolean isCommunication) {
        if (isCommunication) {
            authorizationPanel.setVisible(false);
            authorizationPanel.setManaged(false);
            panelStorage.setVisible(true);
            panelStorage.setManaged(true);
        } else {
            panelStorage.setVisible(false);
            panelStorage.setManaged(false);
            authorizationPanel.setVisible(true);
            authorizationPanel.setManaged(true);
        }
    }

    private void setRegistrationPanelListener() {
        ClientHandler clientHandler = ProtoClient.getInstance()
                .getCurrentChannel().pipeline().get(ClientHandler.class);
        clientHandler.setRegistrationListener(this::setAuthorization);
    }

    private void setAuthorization(boolean isRegs) {
        if (isRegs) {
            setAuthorized(true);
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Вы успешно зарегистреровались. " +
                        "\nМожете авторизаваться.",
                        ButtonType.OK);
                alert.showAndWait();
            });
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Пользователь с таким логином уже существует",
                        ButtonType.OK);
                alert.showAndWait();
            });
        }
    }

    private void setPanelStorageListener() {
        ClientHandler clientHandler = ProtoClient.getInstance()
                .getCurrentChannel().pipeline().get(ClientHandler.class);
        clientHandler.setAuthenticationListener(this::establishUserConnection);
    }

    private void establishUserConnection(boolean isAuth) {
        if (isAuth) {
            serverController.updateList("getRoot");
            clientController.updateList(ClientCommandService.PATH_FILE_DIRECTORY);
            setCommunication(true);
        } else {
            setCommunication(false);
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Не правильно ввели логин и/или пароль",
                        ButtonType.OK);
                alert.showAndWait();
            });
        }
    }


    public void tryToRegs(ActionEvent actionEvent) {
        if (isOpen()) {
            String login = loginFieldRegs.getText();
            String password = passwordFieldRegs.getText();
            if (!login.equals("") && !password.equals("")) {
                loginFieldRegs.clear();
                passwordFieldRegs.clear();
                authenticationService.registration(login, password);
            } else {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Не заполнены все поля для регистрации!",
                            ButtonType.OK);
                    alert.showAndWait();
                });
            }
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Разорвано соедение с сервером.\n" +
                        "Перезапустите приложение",
                        ButtonType.OK);
                alert.showAndWait();
            });
        }
    }

    public void tryToAuth() {
        if (isOpen()) {
            String login = loginFieldAuth.getText();
            String password = passwordFieldAuth.getText();
            if (!login.equals("") && !password.equals("")) {
                loginFieldAuth.clear();
                passwordFieldAuth.clear();
                authenticationService.login(login, password);
                authenticationService.successLogin(login, password);
            } else {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Не заполнены все поля для авторизации!",
                            ButtonType.OK);
                    alert.showAndWait();
                });
            }
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Разорвано соедение с сервером.\n" +
                        "Перезапустите приложение",
                        ButtonType.OK);
                alert.showAndWait();
            });
        }
    }

    public void setAuthorizedPanel(ActionEvent actionEvent) {
        if (isOpen()) {
            setAuthorized(true);
        }
    }

    public void setRegistrationPanel(ActionEvent actionEvent) {
        if (isOpen()) {
            setAuthorized(false);
        }
    }

    public void sendBtnAction(ActionEvent actionEvent) {
        UserInfo userInfo = authenticationService.getUserInfo();
        if (clientController.getSelectedFilename() == null && serverController.getSelectedFilename() != null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        Path clientPath;
        String serverPath;

        if (clientController.getSelectedFilename() != null) {
            clientPath = Paths.get(clientController.getCurrentPath(), clientController.getSelectedFilename());
            serverPath = Paths.get(userInfo.getLogin().concat(
                    serverController.getCurrentPath())).resolve(clientPath.getFileName()).toString();
            try {
                ClientCommandService.sendFile(clientPath, serverPath, userInfo.getLogin(), userInfo.getPassword(), serverController);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void downloadBtnAction(ActionEvent actionEvent) {
        UserInfo userInfo = authenticationService.getUserInfo();
        if (clientController.getSelectedFilename() != null && serverController.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        String serverPath;
        Path clientPath;
        if (serverController.getSelectedFilename() != null) {
            serverPath = Paths.get(userInfo.getLogin().concat(
                    serverController.getCurrentPath())).resolve(serverController.getSelectedFilename()).toString();
            clientPath = Paths.get(clientController.getCurrentPath()).resolve(serverController.getSelectedFilename());
            ClientHandler.setPath(clientPath.toString());
            ServerCommandService.downloadFile(serverPath, userInfo.getLogin(), userInfo.getPassword());
        }
    }

    public void deleteFile() {
        UserInfo userInfo = authenticationService.getUserInfo();
        if (serverController.getSelectedFilename() == null) {
            if (clientController.getSelectedFilename() != null) {
                ClientCommandService.deleteRequest(clientController);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
                alert.showAndWait();
            }
        } else {
            String serverPath = Paths.get(userInfo.getLogin().concat(
                    serverController.getCurrentPath())).resolve(serverController.getSelectedFilename()).toString();
            ServerCommandService.deleteRequest(serverPath, userInfo.getLogin(), userInfo.getPassword(), serverController);
        }
    }

    public void btnExitAction() {
        setAuthorized(false);
    }

    private boolean isOpen() {
        return ProtoClient.getInstance().getCurrentChannel().isOpen();
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthorized(false);
        setRegistrationPanelListener();
        setPanelStorageListener();
        clientController = (ClientPanelController) clientPanel.getProperties().get("ctrl");
        serverController = (ServerPanelController) serverPanel.getProperties().get("ctr2");
        registrationPanel.setMaxSize(10, 10);
    }
}
