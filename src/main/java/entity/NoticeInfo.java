package entity;

/**
 * Created by xinxin on 2015/12/9.
 */
public class NoticeInfo extends Info {
    String company_buy, file_buy_time, file_send_time, contact,file_send_address;

    public String getCompany_buy() {
        return company_buy;
    }

    public void setCompany_buy(String company_buy) {
        this.company_buy = company_buy;
    }

    public String getFile_buy_time() {
        return file_buy_time;
    }

    public void setFile_buy_time(String file_buy_time) {
        this.file_buy_time = file_buy_time;
    }

    public String getFile_send_time() {
        return file_send_time;
    }

    public void setFile_send_time(String file_send_time) {
        this.file_send_time = file_send_time;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getFile_send_address() {
        return file_send_address;
    }

    public void setFile_send_address(String file_send_address) {
        this.file_send_address = file_send_address;
    }

    @Override
    public String toString() {
        return "NoticeInfo{" +
                "company_buy='" + company_buy + '\'' +
                ", file_buy_time='" + file_buy_time + '\'' +
                ", file_send_time='" + file_send_time + '\'' +
                ", contact='" + contact + '\'' +
                ", file_send_address='" + file_send_address + '\'' +
                "} " + super.toString();
    }
}
