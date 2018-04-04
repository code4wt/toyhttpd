package xyz.coolblog.httpd;

/**
 * Response
 *
 * @author coolblog.xyz
 * @date 2018-03-27 20:02:52
 */
public class Response {

    private int code;

    private String version;

    private String contentType;

    private String server;

    @Override
    public String toString() {

        return "Response{}";
    }
}
