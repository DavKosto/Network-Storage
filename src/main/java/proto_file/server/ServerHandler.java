package proto_file.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ServerHandler extends ChannelInboundHandlerAdapter {
    private enum State {
        IDLE, COMMAND_NAME, ADD, DOWNLOAD, GET_FILES, DELETE, REGISTRATION, AUTHENTICATION, LOGIN_PASSWORD,
        LOGIN_PASSWORD_LENGTH, NAME_LENGTH, FILE_NAME, FILE_LENGTH, FILE, REQUEST_LENGTH, REQUEST
    }

    private final Path PATH_ROOT_DIRECTORY = Paths.get("src/main/java/proto_file/server/serverFiles");

    private State currentState = State.IDLE;
    private State innerState;

    private int nameLength = 0;
    private long fileLength = 0;
    private long receivedFileLength = 0L;
    private byte[] nameBytes;
    private String name;
    private String login;
    private String password;
    private BufferedOutputStream out;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Server connected...");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Server disconnected...");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            chooseState(buf);

            switch (currentState) {
                case REGISTRATION:
                    answerToRegistration(buf, ctx);
                    break;
                case AUTHENTICATION:
                    answerToAuthorization(buf, ctx);
                    break;
                case ADD:
                    fileRecord(buf);
                    break;
                case GET_FILES:
                    sendFileToGetRequest(buf, ctx);
                    break;
                case DOWNLOAD:
                    sendFileToDownloadRequest(ctx, buf);
                    break;
                case DELETE:
                    deleteFileAndDirectory(buf);
                    break;
            }
        }
        release(buf);
    }

    private void release(ByteBuf buf) {
        if (buf.readableBytes() == 0) {
            buf.release();
        }
    }

    private void resetState() {
        name = null;
        login = null;
        nameLength = 0;
        fileLength = 0;
        password = null;
        nameBytes = null;
        innerState = null;
        receivedFileLength = 0L;
        currentState = State.IDLE;
    }

    private void chooseState(ByteBuf buf) {
        if (currentState == State.IDLE) {
            currentState = State.COMMAND_NAME;
            System.out.println("STATE: Team Accepted");
        }

        if (currentState == State.COMMAND_NAME) {
            byte[] bytes = new byte[3];
            buf.readBytes(bytes);
            String commandName = new String(bytes, StandardCharsets.UTF_8);
            System.out.println(commandName);
            switch (commandName) {
                case "Reg":
                    System.out.println("STATE: Team - Registration");
                    currentState = State.REGISTRATION;
                    break;
                case "Aut":
                    System.out.println("STATE: Team - Authentication");
                    currentState = State.AUTHENTICATION;
                    break;
                case "Add":
                    System.out.println("STATE: Team - Add");
                    currentState = State.ADD;
                    break;
                case "Dow":
                    System.out.println("STATE: Team - Download");
                    currentState = State.DOWNLOAD;
                    break;
                case "Del":
                    System.out.println("STATE: Team - Delete");
                    currentState = State.DELETE;
                    break;
                case "Get":
                    System.out.println("STATE: Team - getFiles");
                    currentState = State.GET_FILES;
                    break;
                default:
                    System.out.println("STATE: Invalid Command");
                    break;
            }
        }
    }

    private void answerToRegistration(ByteBuf buf, ChannelHandlerContext ctx) {
        String isRegs = null;
        if (innerState == null) {
            innerState = State.REQUEST_LENGTH;
        }
        System.out.println("STATE: Start file receiving");
        if (innerState == State.REQUEST_LENGTH) {
            if (buf.readableBytes() >= 4) {
                System.out.println("STATE: Get filename length");
                nameLength = buf.readInt();
                innerState = State.REQUEST;
            }
        }

        if (innerState == State.REQUEST) {
            if (buf.readableBytes() >= nameLength) {
                nameBytes = new byte[nameLength];
                buf.readBytes(nameBytes);
                name = new String(nameBytes, StandardCharsets.UTF_8);
                System.out.println("STATE: Filename received - " + name);
                String[] loginAndPass = name.split(" ");
                String login = loginAndPass[1];
                String password = loginAndPass[2];
                Path directory = PATH_ROOT_DIRECTORY.resolve(login);
                try {
                    if (AuthService.loginIsThere(login)) {
                        isRegs = "Regs";
                        System.out.println("Regs");
                    } else {
                        try {
                            Files.createDirectory(directory);
                            AuthService.addNewUser(login, password, login);
                            isRegs = "RegsOk";
                            System.out.println("RegsOk");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

            }
        }
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer(3);
        byteBuf.writeBytes("Reg".getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(byteBuf);

        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(4);
        assert isRegs != null;
        int length = isRegs.getBytes(StandardCharsets.UTF_8).length;
        byteBuf.writeInt(length);
        ctx.writeAndFlush(byteBuf);

        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(length);
        byteBuf.writeBytes(isRegs.getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(byteBuf);

        resetState();
    }

    private void answerToAuthorization(ByteBuf buf, ChannelHandlerContext ctx) {
        String isAuth = null;
        if (innerState == null) {
            innerState = State.REQUEST_LENGTH;
        }
        System.out.println("STATE: Start file receiving");
        if (innerState == State.REQUEST_LENGTH) {
            if (buf.readableBytes() >= 4) {
                System.out.println("STATE: Get filename length");
                nameLength = buf.readInt();
                innerState = State.REQUEST;
            }
        }

        if (innerState == State.REQUEST) {
            if (buf.readableBytes() >= nameLength) {
                nameBytes = new byte[nameLength];
                buf.readBytes(nameBytes);
                name = new String(nameBytes, StandardCharsets.UTF_8);
                System.out.println("STATE: Filename received - " + name);
                String[] loginAndPass = name.split(" ");
                login = loginAndPass[1];
                password = loginAndPass[2];
                String directory = null;
                try {
                    directory = AuthService.getDirectoryByLoginAndPassword(login, password);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                if (directory != null) {
                    isAuth = "AuthOk";
                } else {
                    isAuth = "Auth";
                }
            }
            ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer(3);
            byteBuf.writeBytes("Aut".getBytes(StandardCharsets.UTF_8));
            ctx.writeAndFlush(byteBuf);

            byteBuf = ByteBufAllocator.DEFAULT.directBuffer(4);
            assert isAuth != null;
            int length = isAuth.getBytes(StandardCharsets.UTF_8).length;
            byteBuf.writeInt(length);
            ctx.writeAndFlush(byteBuf);

            byteBuf = ByteBufAllocator.DEFAULT.directBuffer(length);
            byteBuf.writeBytes(isAuth.getBytes(StandardCharsets.UTF_8));
            ctx.writeAndFlush(byteBuf);

            resetState();
        }
    }

    private void fileRecord(ByteBuf buf) throws IOException {
        if (innerState == null) {
            innerState = State.NAME_LENGTH;
        }
        System.out.println("STATE: Start file receiving");
        if (innerState == State.NAME_LENGTH) {
            if (buf.readableBytes() >= 4) {
                System.out.println("STATE: Get filename length");
                nameLength = buf.readInt();
                innerState = State.FILE_NAME;
            }
        }

        if (innerState == State.FILE_NAME) {
            if (buf.readableBytes() >= nameLength) {
                nameBytes = new byte[nameLength];
                buf.readBytes(nameBytes);
                name = new String(nameBytes, StandardCharsets.UTF_8);
                System.out.println("STATE: Filename received - " + name);
                innerState = State.LOGIN_PASSWORD_LENGTH;
            }
        }

        if (innerState == State.LOGIN_PASSWORD_LENGTH) {
            if (buf.readableBytes() >= 4) {
                nameLength = buf.readInt();
                innerState = State.LOGIN_PASSWORD;
            }
        }

        if (innerState == State.LOGIN_PASSWORD) {
            if (buf.readableBytes() >= nameLength) {
                nameBytes = new byte[nameLength];
                buf.readBytes(nameBytes);
                String[] login_Password = new String(nameBytes, StandardCharsets.UTF_8).split(" ");
                login = login_Password[0];
                password = login_Password[1];
                String directory = null;
                try {
                    directory = AuthService.getDirectoryByLoginAndPassword(login, password);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                if (directory != null && name.contains(directory)) {
                    innerState = State.FILE_LENGTH;
                }
            }
        }

        if (innerState == State.FILE_LENGTH) {
            if (buf.readableBytes() >= 8) {
                fileLength = buf.readLong();
                System.out.println("STATE: File length received - " + fileLength);
                innerState = State.FILE;
            }
        }

        if (innerState == State.FILE) {
            out = new BufferedOutputStream(
                    new FileOutputStream(PATH_ROOT_DIRECTORY.resolve(name).toString(), true));
            while (buf.readableBytes() > 0) {
                out.write(buf.readByte());
                receivedFileLength++;
                if (fileLength == receivedFileLength) {
                    System.out.println("File received");
                    resetState();
                    out.close();
                    System.out.println("Файл " + name + " успешно был записан на сервере");
                    break;
                }
            }
            out.close();
        }
    }

    private void sendFileToDownloadRequest(ChannelHandlerContext ctx, ByteBuf buf) throws IOException {
        Path path = null;
        if (innerState == null) {
            innerState = State.NAME_LENGTH;
        }
        if (innerState == State.NAME_LENGTH) {
            if (buf.readableBytes() >= 4) {
                System.out.println("STATE: Get filename length");
                nameLength = buf.readInt();
                innerState = State.FILE_NAME;
            }
        }

        if (innerState == State.FILE_NAME) {
            if (buf.readableBytes() >= nameLength) {
                nameBytes = new byte[nameLength];
                buf.readBytes(nameBytes);
                name = new String(nameBytes, StandardCharsets.UTF_8);
                System.out.println("STATE: Filename received - " + name);
                path = PATH_ROOT_DIRECTORY.resolve(name);
                if (Files.exists(path)) {
                    innerState = State.LOGIN_PASSWORD_LENGTH;
                } else {
                    System.out.println("File with names " + name + " not found");
                    resetState();
                    return;
                }
            }
        }

        if (innerState == State.LOGIN_PASSWORD_LENGTH) {
            if (buf.readableBytes() >= 4) {
                nameLength = buf.readInt();
                innerState = State.LOGIN_PASSWORD;
            }
        }

        if (innerState == State.LOGIN_PASSWORD) {
            if (buf.readableBytes() >= nameLength) {
                nameBytes = new byte[nameLength];
                buf.readBytes(nameBytes);
                String[] login_Password = new String(nameBytes, StandardCharsets.UTF_8).split(" ");
                login = login_Password[0];
                password = login_Password[1];
                String directory = null;
                try {
                    directory = AuthService.getDirectoryByLoginAndPassword(login, password);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                if (directory != null && name.contains(directory)) {
                    ByteBuf bufDown = ByteBufAllocator.DEFAULT.directBuffer(3);
                    bufDown.writeBytes("Dow".getBytes(StandardCharsets.UTF_8));
                    ctx.writeAndFlush(bufDown);

                    bufDown = ByteBufAllocator.DEFAULT.directBuffer(4);
                    int length = name.getBytes(StandardCharsets.UTF_8).length;
                    bufDown.writeInt(length);
                    ctx.writeAndFlush(bufDown);

                    bufDown = ByteBufAllocator.DEFAULT.directBuffer(length);
                    bufDown.writeBytes(name.getBytes(StandardCharsets.UTF_8));
                    ctx.writeAndFlush(bufDown);

                    bufDown = ByteBufAllocator.DEFAULT.directBuffer(8);
                    assert path != null;
                    bufDown.writeLong(Files.size(path));
                    ctx.writeAndFlush(bufDown);

                    FileRegion fileRegion = new DefaultFileRegion(path.toFile(), 0, Files.size(path));
                    ctx.writeAndFlush(fileRegion).addListener(future -> {
                        String n = name;
                        if (future.isSuccess()) {
                            System.out.println("Файл " + n + " с сервера отправен для заиси, пожулйста, ждите...");
                        }else {
                            System.out.println("Файл " + n + " не получилось с сервера отправить для записи");
                        }
                    });
                } else {
                    System.out.println("Вы не являйтесь пользователем " + name + " каталога");
                }
                resetState();
            }
        }
    }

    private void sendFileToGetRequest(ByteBuf buf, ChannelHandlerContext ctx) throws IOException {
        if (innerState == null) {
            innerState = State.FILE_LENGTH;
        }
        if (innerState == State.FILE_LENGTH) {
            if (buf.readableBytes() >= 4) {
                System.out.println("STATE: Get filename length");
                nameLength = buf.readInt();
                innerState = State.FILE_NAME;
            }
        }

        if (innerState == State.FILE_NAME) {
            if (buf.readableBytes() >= nameLength) {
                nameBytes = new byte[nameLength];
                buf.readBytes(nameBytes);
                name = new String(nameBytes, StandardCharsets.UTF_8);
                innerState = State.LOGIN_PASSWORD_LENGTH;
            }
        }
        if (innerState == State.LOGIN_PASSWORD_LENGTH) {
            if (buf.readableBytes() >= 4) {
                nameLength = buf.readInt();
                innerState = State.LOGIN_PASSWORD;
            }
        }

        if (innerState == State.LOGIN_PASSWORD) {
            if (buf.readableBytes() >= nameLength) {
                nameBytes = new byte[nameLength];
                buf.readBytes(nameBytes);
                String[] login_Password = new String(nameBytes, StandardCharsets.UTF_8).split(" ");
                login = login_Password[0];
                password = login_Password[1];
                String directory = null;
                try {
                    directory = AuthService.getDirectoryByLoginAndPassword(login, password);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                if (directory != null) {
                    List<Path> pathFiles;
                    Path path = PATH_ROOT_DIRECTORY.resolve(name);
                    if (name.contains("getRoot")) {
                        pathFiles = getPaths(PATH_ROOT_DIRECTORY.resolve(directory));
                        pathFiles.add(0, PATH_ROOT_DIRECTORY.resolve(directory));
                    } else if (Files.exists(path) && name.contains(directory)) {
                        if (name.equals(directory)) {
                            resetState();
                            return;
                        } else {
                            pathFiles = getPaths(path);
                            pathFiles.add(0, path);
                        }
                    } else {
                        System.out.println("File with names " + name + " not found");
                        resetState();
                        return;
                    }
                    System.out.println("STATE: Filename received - " + name);
                    ByteBuf bufGet = ByteBufAllocator.DEFAULT.directBuffer(3);
                    bufGet.writeBytes("Get".getBytes(StandardCharsets.UTF_8));
                    ctx.writeAndFlush(bufGet);

                    bufGet = ByteBufAllocator.DEFAULT.directBuffer(8);
                    bufGet.writeLong(pathFiles.size());
                    ctx.writeAndFlush(bufGet);

                    for (Path pathFile : pathFiles) {

                        int beginIndex = PATH_ROOT_DIRECTORY.resolve(directory).toString().length();
                        String subPath = pathFile.toString().substring(beginIndex);

                        bufGet = ByteBufAllocator.DEFAULT.directBuffer(8);
                        bufGet.writeLong(Files.size(pathFile));
                        ctx.writeAndFlush(bufGet);

                        bufGet = ByteBufAllocator.DEFAULT.directBuffer(4);
                        int length = subPath.getBytes(StandardCharsets.UTF_8).length;
                        bufGet.writeInt(length);
                        ctx.writeAndFlush(bufGet);

                        bufGet = ByteBufAllocator.DEFAULT.directBuffer(8);
                        bufGet.writeLong(Files.getLastModifiedTime(pathFile).toMillis());
                        ctx.writeAndFlush(bufGet);

                        bufGet = ByteBufAllocator.DEFAULT.directBuffer(length);
                        bufGet.writeBytes(subPath.getBytes(StandardCharsets.UTF_8));
                        ctx.writeAndFlush(bufGet);

                        boolean isDirectory = Files.isDirectory(pathFile);
                        bufGet = ByteBufAllocator.DEFAULT.directBuffer(1);
                        bufGet.writeBoolean(isDirectory);
                        ctx.writeAndFlush(bufGet).addListener(future -> {
                            if (future.isSuccess()) {
                                System.out.println("Файл " + pathFile + " с сервера отправлен, пожулйста, ждите...");
                            }else {
                                System.out.println("Файл " + pathFile + " не получилось отправить с сервера ");
                            }
                        });
                    }
                    resetState();
                }
            }
        }

    }

    private void deleteFileAndDirectory(ByteBuf buf) throws IOException {
        if (innerState == null) {
            innerState = State.FILE_LENGTH;
        }
        if (innerState == State.FILE_LENGTH) {
            if (buf.readableBytes() >= 4) {
                System.out.println("STATE: Get filename length");
                nameLength = buf.readInt();
                innerState = State.FILE_NAME;
            }
        }
        if (innerState == State.FILE_NAME) {
            if (buf.readableBytes() >= nameLength) {
                nameBytes = new byte[nameLength];
                buf.readBytes(nameBytes);
                name = new String(nameBytes, StandardCharsets.UTF_8);
                innerState = State.LOGIN_PASSWORD_LENGTH;
            }
        }
        if (innerState == State.LOGIN_PASSWORD_LENGTH) {
            if (buf.readableBytes() >= 4) {
                nameLength = buf.readInt();
                innerState = State.LOGIN_PASSWORD;
            }
        }
        if (innerState == State.LOGIN_PASSWORD) {
            if (buf.readableBytes() >= nameLength) {
                nameBytes = new byte[nameLength];
                buf.readBytes(nameBytes);
                String[] login_Password = new String(nameBytes, StandardCharsets.UTF_8).split(" ");
                login = login_Password[0];
                password = login_Password[1];
            }
            String directory = null;
            try {
                directory = AuthService.getDirectoryByLoginAndPassword(login, password);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (directory != null && name.contains(directory)) {
                Path path = PATH_ROOT_DIRECTORY.resolve(name);
                if (Files.isDirectory(path)) {
                    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
                Files.deleteIfExists(path);
            } else {
                System.out.println("Вы не являйтесь пользователем " + name + " каталога");
            }
            resetState();
        }
    }


    private List<Path> getPaths(Path path) throws IOException {
        List<Path> pathFiles = new LinkedList<>();
        Files.newDirectoryStream(path)
                .forEach(pathFiles::add);
        return pathFiles;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}

