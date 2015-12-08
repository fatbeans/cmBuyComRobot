import entity.Info;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.DateUtils;
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
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xinxin on 2015/12/8.
 */
public class NoticeRobot {

    private final String charset = "utf-8";
    CloseableHttpClient client = HttpClients.createDefault();
    private final String[] dateformat = new String[]{"yyyy-MM-dd"};
    private final String viewUrl = "http://b2b.10086.cn/b2b/main/viewNoticeContent.html?noticeBean.id=";
    private final Pattern pattern = Pattern.compile("\\d+");
    private final HashMap<Integer, Integer> idCache = new HashMap<Integer, Integer>();
    private Connection connection;

    /**
     * @param page
     * @param size
     * @return
     * @throws UnsupportedEncodingException
     */
    public static HttpPost initPost(int page, int size, int type) throws UnsupportedEncodingException {

        HttpPost post = new HttpPost("http://b2b.10086.cn/b2b/main/listVendorNoticeResult.html?noticeBean.noticeType=" + type);


        List<NameValuePair> formparams = new ArrayList<NameValuePair>();

        formparams.add(new BasicNameValuePair("page.currentPage", page + ""));
        formparams.add(new BasicNameValuePair("page.perPageSize", size + ""));
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

    private void initIdCache() throws SQLException, ClassNotFoundException {
        connection = getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("select id from noticeinfo");
        while (resultSet.next()) {
            idCache.put(resultSet.getInt(1), 1);
        }
        resultSet.close();
        statement.close();
        connection.close();
    }


    /**
     * 采购公告  id 2
     *
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public void scanBuyBuyBuy() throws SQLException, ClassNotFoundException, IOException {
//        Date maxDate = 采购公告
        int typeId = 2;
        initIdCache();
        Connection connection = getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("select MAX(DATE) from noticeinfo where noticeType like '%采购公告%'");
        Date minDate = new Date(new java.util.Date().getTime());
        if (resultSet.next()) {
            Date date = resultSet.getDate(1);
            minDate = date == null ? minDate : date;
        }
        resultSet.close();
        statement.close();
        connection.close();

        int size = 100;

        int times = getTotalNum(typeId) / size + 1;
        connection = getConnection();
        connection.setAutoCommit(false);
        PreparedStatement preparedStatement = connection.prepareStatement("insert into noticeinfo (id,noticeType,company,title,date,url) values(?,?,?,?,?,?)");


        for (int i = 0; i < times; i++) {
            Date date = scanItem(i + 1, size, typeId, preparedStatement);
            connection.commit();
//            if(minDate.after(date)) {
//                break;
//            }
        }
        preparedStatement.close();
        connection.close();

    }


    private Date scanItem(int page, int size, int type, PreparedStatement preparedStatement) throws IOException, SQLException, ClassNotFoundException {
        HttpPost post = initPost(page, size, type);
        CloseableHttpResponse response = client.execute(post);
        Document document = Jsoup.parse(EntityUtils.toString(response.getEntity(), charset));
        Elements elementsByTag = document.getElementsByTag("table").get(0).getElementsByTag("tr");
        elementsByTag.remove(0);
        elementsByTag.remove(0);

        Date minDate = new Date(new java.util.Date().getTime());

        for (Element element : elementsByTag) {
            Info info = getInfo(element);
            System.out.println(info);
            if (!idCache.containsKey(info.getId())) {
                preparedStatement.setInt(1, info.getId());
                preparedStatement.setString(2, info.getNoticeType());
                preparedStatement.setString(3, info.getCompany());
                preparedStatement.setString(4, info.getTitle());
                preparedStatement.setDate(5, info.getDate());
                preparedStatement.setString(6, info.getUrl());
                preparedStatement.addBatch();
                idCache.put(info.getId(), 1);
            }
            minDate = minDate.after(info.getDate()) ? info.getDate() : minDate;

        }
        response.close();
        preparedStatement.executeBatch();
        return minDate;
    }

    private Info getInfo(Element element) {
        Info info = new Info();
        String attr = element.attr("onclick");
        Matcher m = pattern.matcher(attr);
        m.find();
        info.setId(NumberUtils.toInt(m.group()));
        info.setCompany(element.child(0).text());
        info.setNoticeType(element.child(1).text());
        info.setTitle(element.child(2).child(0).text());
        info.setDate(new Date(DateUtils.parseDate(element.child(3).text().trim(), dateformat).getTime()));
        info.setUrl(viewUrl + info.getId());
        return info;
    }

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DriverManager.getConnection("jdbc:mysql://192.168.100.201:3306/b2b?useUnicode=true&characterEncoding=UTF8", "root", "123qwe");
        return conn;
    }


    public int getTotalNum(int type) {
        try {
            HttpPost post = initPost(1, 20, type);
            CloseableHttpResponse response = client.execute(post);
            String content = EntityUtils.toString(response.getEntity(), charset);
            response.close();
            Pattern p = Pattern.compile("totalRecordNum.*value=\\\"(.*)\\\"");
            Matcher m = p.matcher(content);
            if (m.find()) {
                int rowNum = NumberUtils.toInt(m.group(1).replaceAll(",", ""));
                return rowNum;
            } else {
                return 0;
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {
//        System.getProperties().list(System.out);


        new NoticeRobot().scanBuyContent();
    }

    private static void DbTest() throws SQLException, ClassNotFoundException {
        NoticeRobot robot = new NoticeRobot();
        Connection connection = robot.getConnection();
        ResultSet resultSet = connection.createStatement().executeQuery("select * from noticeinfo");
        System.out.println("------");
        while (resultSet.next()) {
            System.out.println(resultSet.getString(1));
            System.out.println(resultSet.getString(2));
            System.out.println(resultSet.getString(3));
            System.out.println(resultSet.getString(4));
            System.out.println(resultSet.getString(5));
            System.out.println(resultSet.getString(6));
        }
        System.out.println("------");
        connection.close();

    }


    public  void scanBuyContent() throws IOException {
        HttpPost post = new HttpPost("http://b2b.10086.cn/b2b/main/viewNoticeContent.html?noticeBean.id=234693");
        CloseableHttpResponse response = client.execute(post);
        String content = EntityUtils.toString(response.getEntity(), charset);
        Document document = Jsoup.parse(content);
        Element table = document.getElementById("mobanDiv").child(1);
        System.out.println(table);


    }


}
