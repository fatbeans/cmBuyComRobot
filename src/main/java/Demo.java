import org.apache.commons.lang.math.NumberUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xinxin on 2015/12/7.
 */
public class Demo {
    public static void main(String[] args) throws IOException, URISyntaxException, SQLException {

//        http://b2b.10086.cn/b2b/main/listVendorNoticeResult.html?noticeBean.noticeType=2


        PatternTest();


    }

    private static HttpPost initPost() throws UnsupportedEncodingException {
        HttpPost post = new HttpPost("http://b2b.10086.cn/b2b/main/listVendorNoticeResult.html?noticeBean.noticeType=2");


        List<NameValuePair> formparams = new ArrayList<NameValuePair>();

        formparams.add(new BasicNameValuePair("page.currentPage", "1"));
        formparams.add(new BasicNameValuePair("page.perPageSize", "50"));
        formparams.add(new BasicNameValuePair("page.noticeBean.sourceCH", ""));
        formparams.add(new BasicNameValuePair("noticeBean.source", ""));
        formparams.add(new BasicNameValuePair("noticeBean.title", ""));
        formparams.add(new BasicNameValuePair("noticeBean.startDate", ""));
        formparams.add(new BasicNameValuePair("noticeBean.endDate", ""));


        UrlEncodedFormEntity uefEntity;
        uefEntity = new UrlEncodedFormEntity(formparams, "UTF-8");
        post.setEntity(uefEntity);
        return post;
    }


    public static void PatternTest() {
        Matcher m = Pattern.compile("\\d\\d.\\d\\d").matcher("12aa32");
        System.out.println(m.find());
    }


}
