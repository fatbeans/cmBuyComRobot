import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xinxin on 2015/12/7.
 */
public class Demo {
    public static void main(String[] args) throws IOException, URISyntaxException {

//        http://b2b.10086.cn/b2b/main/listVendorNoticeResult.html?noticeBean.noticeType=2



        HttpPost post = new HttpPost("http://b2b.10086.cn/b2b/main/listVendorNoticeResult.html?noticeBean.noticeType=2");


        List<NameValuePair> formparams = new ArrayList<>();

        formparams.add(new BasicNameValuePair("page.currentPage", "1"));
        formparams.add(new BasicNameValuePair("page.perPageSize", "20"));
        formparams.add(new BasicNameValuePair("page.noticeBean.sourceCH", ""));
        formparams.add(new BasicNameValuePair("noticeBean.source", ""));
        formparams.add(new BasicNameValuePair("noticeBean.title", ""));
        formparams.add(new BasicNameValuePair("noticeBean.startDate", ""));
        formparams.add(new BasicNameValuePair("noticeBean.endDate", ""));


        UrlEncodedFormEntity uefEntity;
        uefEntity = new UrlEncodedFormEntity(formparams, "UTF-8");
        post.setEntity(uefEntity);


        CloseableHttpClient client = HttpClients.createDefault();


        CloseableHttpResponse response = client.execute(post);

        Document document = Jsoup.parse(EntityUtils.toString(response.getEntity(), "utf-8"));

        document.getElementsByTag("table").get(0).getElementsByTag("tr").stream()
                .skip(2)
                .forEach(x->System.out.println(x+"\r\n"));



    }
}
