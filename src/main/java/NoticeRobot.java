import entity.NoticeInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
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

    public static final Pattern timePattern = Pattern.compile("截止时间.*?(\\d{4}.\\d{1,2}.\\d{1,2}.\\d{1,2}.\\d{1,2}.)");
    public static final String nbspText = Jsoup.parse("&nbsp;").text();
    private final String charset = "utf-8";
    CloseableHttpClient client = HttpClients.createDefault();
    private final String[] dateformat = new String[]{"yyyy-MM-dd"};
    private final String viewUrl = "http://b2b.10086.cn/b2b/main/viewNoticeContent.html?noticeBean.id=";
    private final Pattern pattern = Pattern.compile("\\d+");
    private final HashMap<Integer, Integer> idCache = new HashMap<Integer, Integer>();
    Pattern buyTimePattern = Pattern.compile("售卖时间.+?(\\d{4}.\\d{1,2}.\\d{1,2}.((\\d{1,2}.\\d{1,2})|).*?\\d{4}.\\d{1,2}.\\d{1,2}.\\d{1,2}.\\d{1,2}.)");
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
        PreparedStatement preparedStatement = connection.prepareStatement("insert into noticeinfo " +
                "(id,noticeType,company,title,date,url, company_buy, file_buy_time, file_send_time, contact,file_send_address) " +
                "values(?,?,?,?,?,?, ?, ?, ?, ?,?)");


        for (int i = 0; i < times; i++) {
            Date date = scanItem(i + 1, size, typeId, preparedStatement);
            connection.commit();
            System.out.println("i = " + i);
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
            try {
                NoticeInfo info = getInfo(element);

                if (!idCache.containsKey(info.getId())) {
                    try {
                        preparedStatement.setInt(1, info.getId());
                        preparedStatement.setString(2, info.getNoticeType());
                        preparedStatement.setString(3, info.getCompany());
                        preparedStatement.setString(4, info.getTitle());
                        preparedStatement.setDate(5, info.getDate());
                        preparedStatement.setString(6, info.getUrl());
                        preparedStatement.setString(7, info.getCompany_buy());
                        preparedStatement.setString(8, info.getFile_buy_time());
                        preparedStatement.setString(9, info.getFile_send_time());
                        preparedStatement.setString(10, info.getContact());
                        preparedStatement.setString(11, info.getFile_send_address());
                        preparedStatement.addBatch();
                        idCache.put(info.getId(), 1);

                        minDate = minDate.after(info.getDate()) ? info.getDate() : minDate;
                    } catch (SQLException e) {
                        System.out.println(info);
                        e.printStackTrace();

                    }

                }
            } catch (Exception e1) {
//                e.printStackTrace();
                System.out.println("失败公告：" + element.attr("onclick"));
            }
        }
        response.close();
        preparedStatement.executeBatch();
        return minDate;
    }

    private NoticeInfo getInfo(Element element) throws IOException {
        NoticeInfo info = new NoticeInfo();
        String attr = element.attr("onclick");
        Matcher m = pattern.matcher(attr);
        m.find();
        info.setId(NumberUtils.toInt(m.group()));
        info.setCompany(element.child(0).text());
        info.setNoticeType(element.child(1).text());
        info.setTitle(element.child(2).child(0).text());
        info.setDate(new Date(DateUtils.parseDate(element.child(3).text().trim(), dateformat).getTime()));
        info.setUrl(viewUrl + info.getId());
        scanBuyContent(info);
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

//
//        NoticeInfo noticeInfo = new NoticeInfo();
//        noticeInfo.setId(229167);
//        new NoticeRobot().scanBuyContent(noticeInfo);
//        System.out.println(noticeInfo);


        new NoticeRobot().scanBuyBuyBuy();
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


    public void scanBuyContent(NoticeInfo info) throws IOException {

        HttpPost post = new HttpPost("http://b2b.10086.cn/b2b/main/viewNoticeContent.html?noticeBean.id=" + info.getId());
        CloseableHttpResponse response = client.execute(post);
        String content = EntityUtils.toString(response.getEntity(), charset);
        Document document = Jsoup.parse(content);

        Element mobanDiv = document.getElementById("mobanDiv");

        Elements tr = mobanDiv.getElementsByTag("table").get(0).child(0).tagName().equals("tbody") ?
                mobanDiv.getElementsByTag("table").get(0).child(0).children() :
                mobanDiv.getElementsByTag("table").get(0).children();


        for (Element element : tr) {
            String s = element.text().replaceAll(nbspText, "");
            if (s.contains("免责声明") || s.contains("附加项")||s.contains("项目概况")) {
                continue;
            }

            Matcher buy_time_m = buyTimePattern.matcher(s);
            if (buy_time_m.find()) {
                info.setFile_buy_time(buy_time_m.group(1));
            }

            Matcher send_time_m = timePattern.matcher(s);
            if (send_time_m.find()) {
                info.setFile_send_time(send_time_m.group(1));
            }

            Matcher address_m = Pattern.compile("截止时间.*?地点(:|：)?(\\S+)；|;").matcher(s);
            if (address_m.find()) {
                info.setFile_send_address(address_m.group(2));
            }
            if (s.contains("联系方式")) {
                String contact = s.replaceAll("七、", "").replaceAll("联系方式", "").replaceAll(nbspText, "").trim();
                if (contact.length() > 512) {
                    contact = contact.substring(0, 512);
                }
                info.setContact(contact);
            }


        }
        Matcher company_buy_m = Pattern.compile("招标人.*?：(.+)\\d{4}").matcher(tr.last().text());
        if (company_buy_m.find()) {
            info.setCompany_buy(company_buy_m.group(1).trim());
        }


    }


}
