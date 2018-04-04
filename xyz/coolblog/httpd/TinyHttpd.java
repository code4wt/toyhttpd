package xyz.coolblog.httpd;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.Instant;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import xyz.coolblog.httpd.exception.InvalidHeaderException;

import static xyz.coolblog.httpd.StatusCodeEnum.BAD_REQUEST;
import static xyz.coolblog.httpd.StatusCodeEnum.FORBIDDEN;
import static xyz.coolblog.httpd.StatusCodeEnum.INTERNAL_SERVER_ERROR;
import static xyz.coolblog.httpd.StatusCodeEnum.NOT_FOUND;
import static xyz.coolblog.httpd.StatusCodeEnum.OK;

/**
 * TinyHttpd
 *
 * @author coolblog.xyz
 * @date 2018-03-26 22:28:44
 */
public class TinyHttpd {

    private static final int DEFAULT_PORT = 8080;

    private static final int DEFAULT_BUFFER_SIZE = 4096;

    private static final String INDEX_PAGE = "index.html";

    private static final String STATIC_RESOURCE_DIR = "static";

    private static final String META_RESOURCE_DIR_PREFIX = "/meta/";

    private static final String KEY_VALUE_SEPARATOR = ":";

    private static final String CRLF = "\r\n";

    private int port;

    public TinyHttpd() {
        this(DEFAULT_PORT);
    }

    public TinyHttpd(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.socket().bind(new InetSocketAddress("localhost", port));
        ssc.configureBlocking(false);

        System.out.println(String.format("TinyHttpd 已启动，正在监听 %d 端口...", port));

        Selector selector = Selector.open();
        ssc.register(selector, SelectionKey.OP_ACCEPT);

        while(true) {
            int readyNum = selector.select();
            if (readyNum == 0) {
                continue;
            }

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> it = selectedKeys.iterator();
            while (it.hasNext()) {
                SelectionKey selectionKey = it.next();
                it.remove();

                if (selectionKey.isAcceptable()) {
                    SocketChannel socketChannel = ssc.accept();
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ);
                } else if (selectionKey.isReadable()) {
                    request(selectionKey);
                    selectionKey.interestOps(SelectionKey.OP_WRITE);
                } else if (selectionKey.isWritable()) {
                    response(selectionKey);
                }
            }
        }
    }

    /**
     * 处理请求
     * @param selectionKey 选择键
     * @throws IOException
     */
    private void request(SelectionKey selectionKey) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
        channel.read(buffer);

        buffer.flip();
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        String headerStr = new String(bytes);
        try {
            Headers headers = parseHeader(headerStr);
            selectionKey.attach(Optional.of(headers));
        } catch (InvalidHeaderException e) {
            selectionKey.attach(Optional.empty());
        }
    }

    // 解析消息头
    private Headers parseHeader(String headerStr) {
        if (Objects.isNull(headerStr) || headerStr.isEmpty()) {
            throw new InvalidHeaderException();
        }

        int index = headerStr.indexOf(CRLF);
        if (index == -1) {
            throw new InvalidHeaderException();
        }

        Headers headers = new Headers();
        String firstLine = headerStr.substring(0, index);
        String[] parts = firstLine.split(" ");

        /*
         * 请求头的第一行必须由三部分构成，分别为 METHOD PATH VERSION
         * 比如：
         *     GET /index.html HTTP/1.1
         */
        if (parts.length < 3) {
            throw new InvalidHeaderException();
        }

        headers.setMethod(parts[0]);
        headers.setPath(parts[1]);
        headers.setVersion(parts[2]);

        parts = headerStr.split(CRLF);
        for (String part : parts) {
            index = part.indexOf(KEY_VALUE_SEPARATOR);
            if (index == -1) {
                continue;
            }
            String key = part.substring(0, index);
            if (index == -1 || index + 1 >= part.length()) {
                headers.set(key, "");
                continue;
            }
            String value = part.substring(index + 1);
            headers.set(key, value);
        }

        return headers;
    }

    private void response(SelectionKey selectionKey) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        Optional<Headers> op = (Optional<Headers>) selectionKey.attachment();

        // 处理无效请求，返回 400 错误
        if (!op.isPresent()) {
            handleBadRequest(channel);
            channel.close();
            return;
        }

        String ip = channel.getRemoteAddress().toString().replace("/", "");
        Headers headers = op.get();
        // 处理 403
        if (headers.getPath().startsWith(META_RESOURCE_DIR_PREFIX)) {
            handleForbidden(channel);
            channel.close();
            log(ip, headers, FORBIDDEN.getCode());
            return;
        }

        try {
            handleOK(channel, headers.getPath());
            log(ip, headers, OK.getCode());
        } catch (FileNotFoundException e) {
            handleNotFound(channel);
            log(ip, headers, NOT_FOUND.getCode());
        } catch (Exception e) {
            handleInternalServerError(channel);
            log(ip, headers, INTERNAL_SERVER_ERROR.getCode());
        } finally {
            channel.close();
        }
    }

    private void handleOK(SocketChannel channel, String path) throws IOException {
        ResponseHeaders headers = new ResponseHeaders(OK.getCode());

        ByteBuffer bodyBuffer = readFile(path);
        headers.setContentLength(bodyBuffer.capacity());
        headers.setContentType(ContentTypeUtils.getContentType(getExtension(path)));
        ByteBuffer headerBuffer = ByteBuffer.wrap(headers.toString().getBytes());

        channel.write(new ByteBuffer[]{headerBuffer, bodyBuffer});
    }

    private String getExtension(String path) {
        if (path.endsWith("/")) {
            return "html";
        }

        String finename = path.substring(path.lastIndexOf("/") + 1);
        int index = finename.lastIndexOf(".");
        return index == -1 ? "*" : finename.substring(index + 1);
    }

    private void handleNotFound(SocketChannel channel)  {
        try {
            handleError(channel, NOT_FOUND.getCode());
        } catch (Exception e) {
            handleInternalServerError(channel);
        }
    }

    private void handleBadRequest(SocketChannel channel) {
        try {
            handleError(channel, BAD_REQUEST.getCode());
        } catch (Exception e) {
            handleInternalServerError(channel);
        }
    }

    private void handleForbidden(SocketChannel channel) {
        try {
            handleError(channel, FORBIDDEN.getCode());
        } catch (Exception e) {
            handleInternalServerError(channel);
        }
    }

    private void handleInternalServerError(SocketChannel channel) {
        try {
            handleError(channel, INTERNAL_SERVER_ERROR.getCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleError(SocketChannel channel, int statusCode) throws IOException {
        ResponseHeaders headers = new ResponseHeaders(statusCode);

        ByteBuffer bodyBuffer = readFile(String.format("/%d.html", statusCode));
        headers.setContentLength(bodyBuffer.capacity());
        headers.setContentType(ContentTypeUtils.getContentType("html"));
        ByteBuffer headerBuffer = ByteBuffer.wrap(headers.toString().getBytes());

        channel.write(new ByteBuffer[]{headerBuffer, bodyBuffer});
    }


    private ByteBuffer readFile(String path) throws IOException {
        path = STATIC_RESOURCE_DIR + (path.endsWith("/") ? path + INDEX_PAGE : path);
        RandomAccessFile raf = new RandomAccessFile(path, "r");
        FileChannel channel = raf.getChannel();

        ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
        channel.read(buffer);

        buffer.flip();
        return buffer;
    }

    private void log(String ip, Headers headers, int code) {
        // ip [date] "Method path version" code user-agent
        String dateStr = Date.from(Instant.now()).toString();
        String msg = String.format("%s [%s] \"%s %s %s\" %d %s",
            ip, dateStr, headers.getMethod(), headers.getPath(), headers.getVersion(), code, headers.get("User-Agent"));
        System.out.println(msg);
    }

    public static void main(String[] args) throws IOException {
        new TinyHttpd().start();
    }
}
