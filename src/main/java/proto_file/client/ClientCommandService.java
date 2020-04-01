package proto_file.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

class ClientCommandService {
    static final Path PATH_FILE_DIRECTORY = Paths.get("src/main/java/proto_file/client/clientFiles");
    private static Channel channel = ProtoClient.getInstance().getCurrentChannel();

    static void deleteRequest(ClientPanelController clientController){
        try {
            Path path = Paths.get(clientController.getCurrentPath(), clientController.getSelectedFilename());
            deleteFileAndDirectory(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        clientController.updateList(Paths.get(clientController.getCurrentPath()));
        System.out.println("Файл из клиента был успешно удален");
    }

    private static void deleteFileAndDirectory(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        Files.deleteIfExists(path);
    }

    static void sendFile(Path pathClient, String pathServer, String login, String password, ServerPanelController serverController) throws IOException {
        String login_Password = login + " " + password;
        FileRegion region = new DefaultFileRegion(pathClient.toFile(), 0, Files.size(pathClient));

        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(3);
        buf.writeBytes("Add".getBytes(StandardCharsets.UTF_8));
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        int pathLength = pathServer.getBytes(StandardCharsets.UTF_8).length;
        buf.writeInt(pathLength);
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(pathLength);
        buf.writeBytes(pathServer.getBytes(StandardCharsets.UTF_8));
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        int loginLength = login_Password.getBytes(StandardCharsets.UTF_8).length;
        buf.writeInt(loginLength);
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(loginLength);
        buf.writeBytes(login_Password.getBytes(StandardCharsets.UTF_8));
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(8);
        buf.writeLong(Files.size(pathClient));
        channel.write(buf);

        channel.writeAndFlush(region).addListener((future) -> {
            if (future.isSuccess()) {
                serverController.updateList(serverController.getCurrentPath());
                System.out.println("Файл с клиента был отправет для заиси, пожулйста, ждите...");
            }
        });
    }

}
