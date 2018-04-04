package xyz.coolblog.httpd.json;

import java.io.IOException;
import java.io.StringReader;
import xyz.coolblog.httpd.json.parser.Parser;
import xyz.coolblog.httpd.json.tokenizer.CharReader;
import xyz.coolblog.httpd.json.tokenizer.TokenList;
import xyz.coolblog.httpd.json.tokenizer.Tokenizer;

/**
 * Created by code4wt on 17/9/1.
 */
public class JSONParser {

    private Tokenizer tokenizer = new Tokenizer();

    private Parser parser = new Parser();

    public Object fromJSON(String json) throws IOException {
        CharReader charReader = new CharReader(new StringReader(json));
        TokenList tokens = tokenizer.tokenize(charReader);
        return parser.parse(tokens);
    }
}
