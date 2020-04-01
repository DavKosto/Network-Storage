package proto_file.client;

public class AuthenticationService {
    private static final AuthenticationService instance = new AuthenticationService();
    private final UserInfo userInfo = new UserInfo();

    private AuthenticationService(){

    }

    public static AuthenticationService getInstance(){
        return instance;
    }

    public void successLogin(String login, String password){
        userInfo.setLogin(login);
        userInfo.setPassword(password);
    }

    public void registration(String login, String password){
        ServerCommandService.connect("Reg " + login + " " +
                password);
    }

    public void login(String login, String password){
        ServerCommandService.connect("Aut " + login + " " +
                password);
    }

    public UserInfo getUserInfo() {
        UserInfo copyUserInfo = new UserInfo();
        copyUserInfo.setLogin(userInfo.getLogin());
        copyUserInfo.setPassword(userInfo.getPassword());
        return copyUserInfo;
    }
}
