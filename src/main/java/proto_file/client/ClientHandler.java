package proto_file.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler extends ChannelInboundHandlerAdapter {
    private enum State {
        IDLE, COMMAND_NAME, DOWNLOAD, GET_FILES, REGISTRATION, AUTHENTICATION,
        NAME_LENGTH, FILE_NAME, FILE_LENGTH, FILE, ANSWER_LENGTH, ANSWER
    }

    private State currentState = State.IDLE;
    private State innerState;

    private Listener<Void> downloadFileListener;
    private Listener<Boolean> registrationListener;
    private Listener<Boolean> authenticationListener;
    private static String path;
    private String name;
    private int nameLength;
    private byte[] nameBytes;
    private long fileSize;

    private List<FileInfo> pathFilesList;
    private Long filePathSize;
    private Integer filePathLength;
    private Long modificationTime;
    private String fileName;
    private Listener<List<FileInfo>> loadFilesPathsListener;
    private long receivedFileLength;
    private BufferedOutputStream out;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Client connected...");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Client disconnected...");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        while (buf.readableBytes() > 0) {
            chooseState(buf);

            switch (currentState) {
                case REGISTRATION:
                    registration(buf);
                    break;
                case AUTHENTICATION:
                    authentication(buf);
                    break;
                case GET_FILES:
                    acceptFile(buf);
                    break;
                case DOWNLOAD:
                    fileRecord(buf);
                    break;
            }
        }
    }

    private void resetState() {
        path = null;
        name = null;
        fileSize = 0L;
        nameLength = 0;
        receivedFileLength = 0L;
        fileName = null;
        nameBytes = null;
        innerState = null;
        filePathSize = null;
        pathFilesList = null;
        filePathLength = null;
        modificationTime = null;
        currentState = State.IDLE;
    }

    private void chooseState(ByteBuf buf) {
        if (currentState == State.IDLE) {
            currentState = State.COMMAND_NAME;
            System.out.println("STATE: Team Accepted");
        }
        if (currentState == State.COMMAND_NAME) {
            byte[] commandBytes = new byte[3];
            buf.readBytes(commandBytes);
            String commandName = new String(commandBytes, StandardCharsets.UTF_8);
            System.out.println(commandName);
            switch (commandName) {
                case "Reg":
                    System.out.println("STATE: answer to Registration");
                    currentState = State.REGISTRATION;
                    break;
                case "Aut":
                    System.out.println("STATE: answer to Authentication");
                    currentState = State.AUTHENTICATION;
                    break;
                case "Dow":
                    System.out.println("STATE: answer to Download");
                    currentState = State.DOWNLOAD;
                    break;
                case "Get":
                    System.out.println("STATE: answer to Get files");
                    currentState = State.GET_FILES;
                    break;
                default:
                    System.out.println("STATE: Invalid Command" + commandName);
                    break;
            }
        }
    }

    static void setPath(String path) {
        ClientHandler.path = path;
    }

    private void registration(ByteBuf buf) {
        if (innerState == null) {
            innerState = State.ANSWER_LENGTH;
        }
        if (innerState == State.ANSWER_LENGTH) {
            if (buf.readableBytes() >= 4) {
                nameLength = buf.readInt();
                nameBytes = new byte[nameLength];
                innerState = State.ANSWER;
            }
        }

        if (innerState == State.ANSWER) {
            if (buf.readableBytes() >= nameBytes.length) {
                buf.readBytes(nameBytes);
                name = new String(nameBytes, StandardCharsets.UTF_8);
                if (name.equals("RegsOk")) {
                    registrationListener.listen(true);
                } else {
                    registrationListener.listen(false);
                }
                resetState();
            }
        }
    }

    private void authentication(ByteBuf buf) {
        if (innerState == null) {
            innerState = State.ANSWER_LENGTH;
        }
        if (innerState == State.ANSWER_LENGTH) {
            if (buf.readableBytes() >= 4) {
                nameLength = buf.readInt();
                nameBytes = new byte[nameLength];
                innerState = State.ANSWER;
            }
        }

        if (innerState == State.ANSWER) {
            if (buf.readableBytes() >= nameBytes.length) {
                buf.readBytes(nameBytes);
                name = new String(nameBytes, StandardCharsets.UTF_8);
                if (name.equals("AuthOk")) {
                    authenticationListener.listen(true);
                } else {
                    authenticationListener.listen(false);
                }
                resetState();
            }
        }
    }

    private void fileRecord(ByteBuf buf) throws IOException {
        if (innerState == null) {
            innerState = State.NAME_LENGTH;
        }
        if (innerState == State.NAME_LENGTH) {
            if (buf.readableBytes() >= 4) {
                nameLength = buf.readInt();
                nameBytes = new byte[nameLength];
                innerState = State.FILE_NAME;
            }
        }

        if (innerState == State.FILE_NAME) {
            if (buf.readableBytes() >= nameBytes.length) {
                buf.readBytes(nameBytes);
                name = new String(nameBytes, StandardCharsets.UTF_8);
                innerState = State.FILE_LENGTH;
            }
        }

        if (innerState == State.FILE_LENGTH) {
            if (buf.readableBytes() >= 8) {
                fileSize = buf.readLong();
                innerState = State.FILE;
            }
        }

        if (innerState == State.FILE) {
            if (buf.readableBytes() > 0) {
                out = new BufferedOutputStream(
                        new FileOutputStream(path, true));
                while (buf.readableBytes() > 0) {
                    out.write(buf.readByte());
                    receivedFileLength++;
                    if (fileSize == receivedFileLength) {
                        System.out.println("File received");
                        System.out.println("Файл " + name + " успешно был записан на клиенте");
                        out.close();
                        downloadFileListener.listen(null);
                        resetState();
                        break;
                    }
                }
                out.close();
            }
        }
    }

    private void acceptFile(ByteBuf buf) {
        if (innerState == null) {
            pathFilesList = new ArrayList<>();
            innerState = State.FILE_LENGTH;
        }

        if (innerState == State.FILE_LENGTH) {
            if (buf.readableBytes() >= 8) {
                fileSize = buf.readLong();
                innerState = State.FILE;
            }
        }

        if (innerState == State.FILE) {
            System.out.println("Прочитанные байты = " + buf.readableBytes());
            System.out.println("Thread id: " + Thread.currentThread().getId());
            if (filePathSize == null && buf.readableBytes() >= 8) {
                filePathSize = buf.readLong();
                System.out.println("filePathSize: " + filePathSize);
            }
            if (filePathLength == null && buf.readableBytes() >= 4) {
                filePathLength = buf.readInt();
                System.out.println("filePathLength: " + filePathLength);
            }
            if (modificationTime == null && buf.readableBytes() >= 8) {
                modificationTime = buf.readLong();
                System.out.println("modificationTime: " + Instant.ofEpochMilli(modificationTime));
            }
            if (modificationTime != null && filePathSize != null &&
                    filePathLength != null && buf.readableBytes() >= filePathLength) {
                byte[] filePath = new byte[filePathLength];
                buf.readBytes(filePath);
                fileName = new String(filePath, StandardCharsets.UTF_8);
            }
            if (modificationTime != null && filePathSize != null &&
                    filePathLength != null && fileName != null && buf.readableBytes() >= 1) {
                boolean isDirectory = buf.readBoolean();
                pathFilesList.add(new FileInfo(Paths.get(fileName), isDirectory,
                        filePathSize,
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(modificationTime), ZoneOffset.systemDefault())));
                filePathSize = null;
                filePathLength = null;
                modificationTime = null;

                System.out.println("Файл " + fileName + " успешно доставлен клиенту");
            }
            if (pathFilesList != null && fileSize == pathFilesList.size()) {
                loadFilesPathsListener.listen(pathFilesList);
                resetState();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public void setOnLoadFilesPathsListener(Listener<List<FileInfo>> listener) {
        this.loadFilesPathsListener = listener;
    }

    public void setOnDownloadFilePathsListener(Listener<Void> listener) {
        this.downloadFileListener = listener;
    }

    public void setRegistrationListener(Listener<Boolean> listener) {
        this.registrationListener = listener;
    }

    public void setAuthenticationListener(Listener<Boolean> listener) {
        this.authenticationListener = listener;
    }
}


