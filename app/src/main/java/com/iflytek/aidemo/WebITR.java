package com.iflytek.aidemo;

import android.os.Build;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.iflytek.aidemo.util.FileUtil;
import com.iflytek.aidemo.util.HttpUtil;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 拍照速算识别 WebAPI 接口调用示例
 * 运行前：请先填写Appid、APIKey、APISecret
 * 运行方法：直接运行 main() 即可
 * 结果： 控制台输出结果信息
 *
 * 1.接口文档（必看）：https://www.xfyun.cn/doc/words/photo-calculate-recg/API.html
 * 2.错误码链接：https://www.xfyun.cn/document/error-code （错误码code为5位数字）
 * @author iflytek
 */

public class WebITR {
    // ITR webapi 接口地址
    private static final String WebITR_URL = "https://rest-api.xfyun.cn/v2/itr"; //https url
    // 应用ID（到控制台获取）
    private static final String APPID = "*****";
    // 接口APIKey（到控制台的拍照速算识别页面获取）
    private static final String API_KEY = "*****";
    // 接口APISercet（到控制台的拍照速算识别页面获取）
    private static final String API_SECRET = "*****";
    // 图片地址
    public static String AUDIO_PATH = "app/resource/itr/testitr.jpg"; //test path
    public static byte[] imageByteArray;

    /**
     * ITR WebAPI 调用示例程序
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        imageByteArray = FileUtil.read(AUDIO_PATH);
        call_itr(AUDIO_PATH);
        if(true) return;

        if (APPID.equals("") || API_KEY.equals("") || API_SECRET.equals("")) {
            System.out.println("Appid 或APIKey 或APISecret 为空！请打开demo代码，填写相关信息。");
            return;
        }
        String body = buildHttpBody();
        //System.out.println("【ITR WebAPI body】\n" + body);
        Map<String, String> header = buildHttpHeader(body);
        Map<String, Object> resultMap = HttpUtil.doPost2(WebITR_URL, header, body);
        if (resultMap != null) {
            String resultStr = resultMap.get("body").toString();
            System.out.println("【ITR WebAPI 接口调用结果】\n" + resultStr);
            //以下仅用于调试
            Gson json = new Gson();
            ResponseData resultData = json.fromJson(resultStr, ResponseData.class);
            int code = resultData.getCode();
            if (resultData.getCode() != 0) {
                System.out.println("请前往https://www.xfyun.cn/document/error-code?code=" + code + "查询解决办法");
            }
        } else {
            System.out.println("调用失败！请根据错误信息检查代码，接口文档：https://www.xfyun.cn/doc/words/photo-calculate-recg/API.html");
        }
    }

    public static ResponseData call_itr(String image_path) throws Exception {
        if (APPID.equals("") || API_KEY.equals("") || API_SECRET.equals("")) {
            System.out.println("Appid 或APIKey 或APISecret 为空！请打开demo代码，填写相关信息。");
            return null;
        }
        //AUDIO_PATH = image_path;
        String body = buildHttpBody();
        //System.out.println("【ITR WebAPI body】\n" + body);
        Map<String, String> header = buildHttpHeader(body);
        Map<String, Object> resultMap = HttpUtil.doPost2(WebITR_URL, header, body);
        System.out.println("~~~~~~~~in WebITR.call_itr: image_path = " + image_path);

        if (resultMap != null) {
            String resultStr = resultMap.get("body").toString();
            System.out.println("【ITR WebAPI 接口调用结果】\n" + resultStr);
            //以下仅用于调试
            Gson json = new Gson();
            ResponseData resultData = json.fromJson(resultStr, ResponseData.class);
            int code = resultData.getCode();
            if (resultData.getCode() != 0) {
                System.out.println("请前往https://www.xfyun.cn/document/error-code?code=" + code + "查询解决办法");
            }

            return resultData;
        } else {
            System.out.println("调用失败！请根据错误信息检查代码，接口文档：https://www.xfyun.cn/doc/words/photo-calculate-recg/API.html");
        }
        return null;

    }

    /**
     * 组装http请求头
     */
    public static Map<String, String> buildHttpHeader(String body) throws Exception {
        Map<String, String> header = new HashMap<String, String>();
        URL url = new URL(WebITR_URL);

        //时间戳
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date dateD = new Date();
        String date = format.format(dateD);
        System.out.println("【ITR WebAPI date】\n" + date);

        //对body进行sha256签名,生成digest头部，POST请求必须对body验证
        String digestBase64 = "SHA-256=" + signBody(body);
        System.out.println("【ITR WebAPI digestBase64】\n" + digestBase64);

        //hmacsha256加密原始字符串
        StringBuilder builder = new StringBuilder("host: ").append(url.getHost()).append("\n").//
                append("date: ").append(date).append("\n").//
                append("POST ").append(url.getPath()).append(" HTTP/1.1").append("\n").//
                append("digest: ").append(digestBase64);
        System.out.println("【ITR WebAPI builder】\n" + builder);
        String sha = hmacsign(builder.toString(), API_SECRET);
        System.out.println("【ITR WebAPI sha】\n" + sha);

        //组装authorization
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", API_KEY, "hmac-sha256", "host date request-line digest", sha);
        System.out.println("【ITR WebAPI authorization】\n" + authorization);

        header.put("Authorization", authorization);
        header.put("Content-Type", "application/json");
        header.put("Accept", "application/json,version=1.0");
        header.put("Host", url.getHost());
        header.put("Date", date);
        header.put("Digest", digestBase64);
        System.out.println("【ITR WebAPI header】\n" + header);
        return header;
    }

    /**
     * 组装http请求体
     */
    public static String buildHttpBody() throws Exception {
        JsonObject body = new JsonObject();
        JsonObject business = new JsonObject();
        JsonObject common = new JsonObject();
        JsonObject data = new JsonObject();
        //填充common
        common.addProperty("app_id", APPID);
        //填充business
        business.addProperty("ent", "math-arith");
        business.addProperty("aue", "raw");
        //填充data
        //byte[] imageByteArray = FileUtil.read(AUDIO_PATH);
        // String imageBase64 = new String(android.util.Base64.encodeToString(imageByteArray, android.util.Base64.DEFAULT));
        String imageBase64;
        if (Build.VERSION.SDK_INT >= 26 || Build.VERSION.SDK_INT==0)
            imageBase64 = new String(Base64.getEncoder().encodeToString(imageByteArray));
        else
            imageBase64 = android.util.Base64.encodeToString(imageByteArray, android.util.Base64.DEFAULT);
        data.addProperty("image", imageBase64);
        //填充body
        body.add("common", common);
        body.add("business", business);
        body.add("data", data);

        return body.toString();
    }

    /**
     * 对body进行SHA-256加密
     */
    private static String signBody(String body) throws Exception {
        MessageDigest messageDigest;
        String encodestr = "";
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(body.getBytes("UTF-8"));
            if (Build.VERSION.SDK_INT >= 26 || Build.VERSION.SDK_INT==0)
                encodestr = Base64.getEncoder().encodeToString(messageDigest.digest());
            else
                encodestr = android.util.Base64.encodeToString(messageDigest.digest(), android.util.Base64.DEFAULT);
            // encodestr = Base64.encodeToString(messageDigest.digest(),Base64.DEFAULT);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encodestr;
    }

    /**
     * hmacsha256加密
     */
    private static String hmacsign(String signature, String apiSecret) throws Exception {
        Charset charset = Charset.forName("UTF-8");
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(signature.getBytes(charset));
        if (Build.VERSION.SDK_INT >= 26 || Build.VERSION.SDK_INT==0)
            return Base64.getEncoder().encodeToString(hexDigits);
        else
            return android.util.Base64.encodeToString(hexDigits, android.util.Base64.DEFAULT);
    }

    public static class ResponseData {
        private int code;
        private String message;
        private String sid;
        private Object data;
        public int getCode() {
            return code;
        }
        public String getMessage() {
            return this.message;
        }
        public String getSid() {
            return sid;
        }
        public Object getData() {
            return data;
        }
    }
}

