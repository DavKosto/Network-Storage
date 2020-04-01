package proto_file.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

class ServerCommandService {
    private static Channel channel = ProtoClient.getInstance().getCurrentChannel();

    static void connect(String Command_Login_Pass){
        String[] command = Command_Login_Pass.split(" ");

        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(3);
        buf.writeBytes(command[0].getBytes(StandardCharsets.UTF_8));
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        int command_login_password_Length = Command_Login_Pass.getBytes(StandardCharsets.UTF_8).length;
        buf.writeInt(command_login_password_Length);
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(command_login_password_Length);
        buf.writeBytes(Command_Login_Pass.getBytes(StandardCharsets.UTF_8));
        channel.writeAndFlush(buf);
    }



    static void deleteRequest(String serverPath, String login, String password, ServerPanelController serverController){
        String login_Password = login + " " + password;

        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(3);
        buf.writeBytes("Del".getBytes(StandardCharsets.UTF_8));
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        int pathLength = serverPath.getBytes(StandardCharsets.UTF_8).length;
        buf.writeInt(pathLength);
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(pathLength);
        buf.writeBytes(serverPath.getBytes(StandardCharsets.UTF_8));
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        int login_password_Length = login_Password.getBytes(StandardCharsets.UTF_8).length;
        buf.writeInt(login_password_Length);
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(login_password_Length);
        buf.writeBytes(login_Password.getBytes(StandardCharsets.UTF_8));
        channel.writeAndFlush(buf).addListener((future) -> {
            if (future.isSuccess()) {
                serverController.updateList(serverController.getCurrentPath());
                System.out.println("Файл из сервера был успешно удален");
            }
        });
    }

    static void loadPathFilesList(String path, String login, String password) throws InterruptedException {
        String login_Password = login + " " + password;

        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(3);
        buf.writeBytes("Get".getBytes(StandardCharsets.UTF_8));
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        int pathLength = path.getBytes(StandardCharsets.UTF_8).length;
        buf.writeInt(pathLength);
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(pathLength);
        buf.writeBytes(path.getBytes(StandardCharsets.UTF_8));
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        int login_password_Length = login_Password.getBytes(StandardCharsets.UTF_8).length;
        buf.writeInt(login_password_Length);
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(login_password_Length);
        buf.writeBytes(login_Password.getBytes(StandardCharsets.UTF_8));
        channel.writeAndFlush(buf);
    }

    static void downloadFile(String pathServer, String login, String password) {
        String login_Password = login + " " + password;

        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(3);
        buf.writeBytes("Dow".getBytes(StandardCharsets.UTF_8));
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        int pathLength = pathServer.getBytes(StandardCharsets.UTF_8).length;
        buf.writeInt(pathLength);
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(pathLength);
        buf.writeBytes(pathServer.getBytes(StandardCharsets.UTF_8));
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        int login_password_Length = login_Password.getBytes(StandardCharsets.UTF_8).length;
        buf.writeInt(login_password_Length);
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(login_password_Length);
        buf.writeBytes(login_Password.getBytes(StandardCharsets.UTF_8));
        channel.writeAndFlush(buf).addListener((future) -> {
            if (future.isSuccess()) {
                System.out.println("Файл с сервера был отправет для заиси, пожулйста, ждите...");
            }
        });
    }

}
