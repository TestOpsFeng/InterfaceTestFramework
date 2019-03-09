package testcase;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import utils.ExtentTestNGIReporterListener;
import utils.NetUtils;

import java.util.HashMap;

@Listeners(ExtentTestNGIReporterListener.class)
public class BaiduTest{
    @Test
    public void test(){
        HashMap<String, String> header = new HashMap<>();
        header.put("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
        HashMap<String, String> body = new HashMap<>();
        body.put("username","qwertyu");
        CloseableHttpResponse response = NetUtils.post("https://passport.yhd.com/passport/agree.do", header, body);
        Assert.assertEquals(NetUtils.getResponseStatusCode(response),200);
        Assert.assertEquals(NetUtils.getResponseString(response),"{\"show\":true}");
    }

}