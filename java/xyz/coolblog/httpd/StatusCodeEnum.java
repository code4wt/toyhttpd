package xyz.coolblog.httpd;

/**
 * StatusCodeEnum
 *
 * @author coolblog.xyz
 * @date 2018-03-27 12:29:43
 */
public enum StatusCodeEnum {
    OK(200, "OK"),
    BAD_REQUEST(400, "Bad Request"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error");

    private int code;

    private String phrase;

    StatusCodeEnum(int code, String phrase) {
        this.code = code;
        this.phrase = phrase;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getPhrase() {
        return phrase;
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }

    public static String queryPhrase(int code) {
        for (StatusCodeEnum statusCodeEnum : StatusCodeEnum.values()) {
            if (statusCodeEnum.getCode() == code) {
                return statusCodeEnum.getPhrase();
            }
        }

        return null;
    }
}
