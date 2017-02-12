package com.tulingdemo;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.baidu.tts.auth.AuthInfo;
import com.baidu.tts.client.SpeechError;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.tulingdemo.ListData.RECEIVER;

public class MainActivity extends Activity implements HttpGetDataListener,
        OnClickListener {

    private HttpData httpData;
    private List<ListData> lists;
    private ListView lv;
    private ImageView iv_send;
    private TextAdapter adapter;
    private String[] welcome_array;
    private ListData listData;

    private String res;//语音识别的结果
    private boolean isMessage=false;//判断是否是短信内容的标志
    private String msg_number;//发短信时的联系人电话
    private String msg_name;//发短信时的联系人名字


    //下面是百度语音用到的
    private SpeechSynthesizer mSpeechSynthesizer;
    private String mSampleDirPath;
    private static final String SAMPLE_DIR_NAME = "baiduTTS";
    private static final String SPEECH_FEMALE_MODEL_NAME = "bd_etts_speech_female.dat";
    private static final String SPEECH_MALE_MODEL_NAME = "bd_etts_speech_male.dat";
    private static final String TEXT_MODEL_NAME = "bd_etts_text.dat";
    private static final String LICENSE_FILE_NAME = "temp_license";
    private static final String ENGLISH_SPEECH_FEMALE_MODEL_NAME = "bd_etts_speech_female_en.dat";
    private static final String ENGLISH_SPEECH_MALE_MODEL_NAME = "bd_etts_speech_male_en.dat";
    private static final String ENGLISH_TEXT_MODEL_NAME = "bd_etts_text_en.dat";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //如果没有网络连接，无法使用
        if (!isNetworkConnect(this)){
            setContentView(R.layout.activity_no_network);
            return;
        }
        setContentView(R.layout.activity_main);

        initView();

        initialEnv();

        initialTts();

    }

    //判断网络是否连接
    private boolean isNetworkConnect(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        Log.d("tag",networkInfo+"");

        boolean isNetworkAvailable;
        if (networkInfo==null){
            isNetworkAvailable=false;
        }else {
            isNetworkAvailable=true;
        }
        Log.d("tag",isNetworkAvailable+"");
        return isNetworkAvailable;
    }

    private void initView() {
        lv = (ListView) findViewById(R.id.lv);
        iv_send = (ImageView) findViewById(R.id.iv_send);
        lists = new ArrayList<ListData>();
        iv_send.setOnClickListener(this);
        adapter = new TextAdapter(lists, this);
        lv.setAdapter(adapter);

        //刷新界面
        refresh(getRandomWelcomeTips(), RECEIVER);
    }


     //用户第一次进入，随机获取欢迎语
    private String getRandomWelcomeTips() {
        String welcome_tip = null;
        welcome_array = this.getResources()
                .getStringArray(R.array.welcome_tips);
        int index = (int) (Math.random() * (welcome_array.length - 1));
        welcome_tip = welcome_array[index];
        return welcome_tip;
    }

    //获取图灵机器人返回的数据
    @Override
    public void getDataUrl(String data) {
        parseText(data);
    }


    //解析数据
    public void parseText(String str) {
        try {
            JSONObject jb = new JSONObject(str);

            refresh(jb.getString("text"), RECEIVER);

            //语音合成返回的结果
            speak(listData.getContent());

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        //开启语音识别
        Intent intent = new Intent("com.baidu.action.RECOGNIZE_SPEECH");
        intent.putExtra("grammar", "asset:///baidu_speech_grammardemo.bsg"); // 设置离线的授权文件(离线模块需要授权), 该语法可以用自定义语义工具生成, 链接http://yuyin.baidu.com/asr#m5
        startActivityForResult(intent, 1);

    }

    //语音识别返回的结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Bundle results = data.getExtras();
            ArrayList<String> results_recognition = results.getStringArrayList("results_recognition");

            //将数组形式的识别结果变为正常的String类型，例：[给张三打电话]变成给张三打电话
            String str = results_recognition + "";
            res = str.substring(str.indexOf("[") + 1, str.indexOf("]"));

            refresh(res, ListData.SEND);

            //如果是发送的短信内容，必须写在最前面，防止短信内容里面出现关键词
            if (isMessage){
                sendMessage(res);
                isMessage=false;
                return;
            }

            //关键词"打开"
            if (res.contains("打开")){
                String appName= res.substring(res.indexOf("开")+1);
                Log.d("tag app name",appName);

                openApp(appName);
                return;
            }

            //关键词"打电话"
            if (res.contains("打电话")){
                call();
                return;
            }

            //关键词"发短信"
            if (res.contains("发短信")){
                getSendMsgContactInfo();
                return;
            }

            //关键词"查找"
            if (res.contains("查找")){
                String searchContent=res.substring(res.indexOf("找")+1);

                surfTheInternet(searchContent);
                return;
            }

            //如果语音识别的结果没有以上关键词，那么执行聊天机器人的功能
            chat();
        }
    }

    //网上查找
    private void surfTheInternet(String searchContent) {
        refresh("已经为您上网查找"+"\""+searchContent+"\"",RECEIVER);
        speak(listData.getContent());

        // 指定intent的action是ACTION_WEB_SEARCH就能调用浏览器
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        // 指定搜索关键字是选中的文本
        intent.putExtra(SearchManager.QUERY, searchContent);
        startActivity(intent);
    }

    //发送短信的内容
    private void sendMessage(String content) {
        if (msg_number==null){
            return;
        }

        SmsManager manager = SmsManager.getDefault();
        ArrayList<String> list = manager.divideMessage(content);  //因为一条短信有字数限制，因此要将长短信拆分
        for(String text:list){
            manager.sendTextMessage(msg_number, null, text, null, null);
        }

        refresh("已经发送"+"\""+content+"\""+"给"+msg_name,RECEIVER);
        speak(listData.getContent());
    }

    //发送短信的联系人信息
    private void getSendMsgContactInfo() {
        List<ContactInfo> contactLists = getContactLists(this);
        if (contactLists.isEmpty()){
            refresh("通讯录为空",RECEIVER);
            speak(listData.getContent());
            return;
        }
        for (ContactInfo contactInfo:contactLists){
            if (res.contains(contactInfo.getName())){
                msg_name=contactInfo.getName();
                msg_number=contactInfo.getNumber();
                refresh("请问您要发送什么给"+msg_name,RECEIVER);
                speak(listData.getContent());
                isMessage=true;
                return;
            }
        }

        refresh("通讯录中没有此人",RECEIVER);
        speak(listData.getContent());
    }

    //打电话
    private void call() {
        List<ContactInfo> contactLists = getContactLists(this);
        if (contactLists.isEmpty()){
            refresh("通讯录为空",RECEIVER);
            speak(listData.getContent());
            return;
        }
        for (ContactInfo contactInfo:contactLists){
            if (res.contains(contactInfo.getName())){
                refresh("已经为您拨通"+contactInfo.getName()+"的电话",RECEIVER);
                speak(listData.getContent());

                String number = contactInfo.getNumber();
                Intent intent = new Intent();
                intent.setAction("android.intent.action.CALL");
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("tel:"+number));
                startActivity(intent);
                return;
            }
        }

        refresh("通讯录中没有此人",RECEIVER);
        speak(listData.getContent());
    }

    //聊天机器人
    private void chat() {
        // 去掉空格
        String dropk = res.replace(" ", "");
        // 去掉回车
        String droph = dropk.replace("\n", "");
        //api_key请换成自己图灵机器人的api_key
        httpData = (HttpData) new HttpData(
                "http://www.tuling123.com/openapi/api?key=e014cd9ee10f49abb8e54fdd05e778b2&info="
                        + droph, this).execute();
    }

    //获取通信录中所有的联系人
    private List<ContactInfo> getContactLists(Context context) {
        List<ContactInfo> lists = new ArrayList<ContactInfo>();
        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);
        //moveToNext方法返回的是一个boolean类型的数据
        while (cursor.moveToNext()) {
            //读取通讯录的姓名
            String name = cursor.getString(cursor
                    .getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            //读取通讯录的号码
            String number = cursor.getString(cursor
                    .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            ContactInfo contactInfo = new ContactInfo(name, number);
            lists.add(contactInfo);
        }
        return lists;
    }

    //打开应用
    private void openApp(String appName) {
        PackageManager packageManager = MainActivity.this.getPackageManager();
        // 获取手机里的应用列表
        List<PackageInfo> pInfo = packageManager.getInstalledPackages(0);
        for (int i = 0; i < pInfo.size(); i++) {
            PackageInfo p = pInfo.get(i);
            // 获取相关包的<application>中的label信息，也就是-->应用程序的名字
            String label = packageManager.getApplicationLabel(p.applicationInfo).toString();
            Log.d("tag", label);
            if (label.contains(appName)) { //比较label
                refresh(appName + "已经为您打开", RECEIVER);

                speak(listData.getContent());

                String pName = p.packageName; //获取包名
                //获取intent
                Intent intent = packageManager.getLaunchIntentForPackage(pName);
                startActivity(intent);
                return;
            }
        }
        refresh("您没有安装该应用",RECEIVER);
        speak(listData.getContent());
    }

    //刷新页面
    private void refresh(String content,int flag) {
        listData = new ListData(content, flag);
        lists.add(listData);
        //如果item数量大于30，清空数据
        if (lists.size() > 30) {
            for (int i = 0; i < lists.size(); i++) {
                // 移除数据
                lists.remove(i);
            }
        }
        adapter.notifyDataSetChanged();
    }

    //语音合成
    private void speak(String text) {
//        String text = this.mInput.getText().toString();
        //需要合成的文本text的长度不能超过1024个GBK字节。
//        if (TextUtils.isEmpty(mInput.getText())) {
//            text = "欢迎使用百度语音合成SDK,百度语音为你提供支持。";
//            mInput.setText(text);
//        }
        int result = this.mSpeechSynthesizer.speak(text);
        if (result < 0) {
            Toast.makeText(this, "error,please look up error code in doc or URL:http://yuyin.baidu.com/docs/tts/122 ", Toast.LENGTH_LONG).show();
        }
    }

    //下面都是百度语音合成的初始化设置，直接从demo里拷贝的
    private void initialEnv() {
        if (mSampleDirPath == null) {
            String sdcardPath = Environment.getExternalStorageDirectory().toString();
            mSampleDirPath = sdcardPath + "/" + SAMPLE_DIR_NAME;
        }
        makeDir(mSampleDirPath);
        copyFromAssetsToSdcard(false, SPEECH_FEMALE_MODEL_NAME, mSampleDirPath + "/" + SPEECH_FEMALE_MODEL_NAME);
        copyFromAssetsToSdcard(false, SPEECH_MALE_MODEL_NAME, mSampleDirPath + "/" + SPEECH_MALE_MODEL_NAME);
        copyFromAssetsToSdcard(false, TEXT_MODEL_NAME, mSampleDirPath + "/" + TEXT_MODEL_NAME);
        copyFromAssetsToSdcard(false, LICENSE_FILE_NAME, mSampleDirPath + "/" + LICENSE_FILE_NAME);
        copyFromAssetsToSdcard(false, "english/" + ENGLISH_SPEECH_FEMALE_MODEL_NAME, mSampleDirPath + "/"
                + ENGLISH_SPEECH_FEMALE_MODEL_NAME);
        copyFromAssetsToSdcard(false, "english/" + ENGLISH_SPEECH_MALE_MODEL_NAME, mSampleDirPath + "/"
                + ENGLISH_SPEECH_MALE_MODEL_NAME);
        copyFromAssetsToSdcard(false, "english/" + ENGLISH_TEXT_MODEL_NAME, mSampleDirPath + "/"
                + ENGLISH_TEXT_MODEL_NAME);
    }

    private void makeDir(String dirPath) {
        File file = new File(dirPath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    private void initialTts() {
        this.mSpeechSynthesizer = SpeechSynthesizer.getInstance();
        this.mSpeechSynthesizer.setContext(this);
        this.mSpeechSynthesizer.setSpeechSynthesizerListener(new SpeechSynthesizerListener() {
            @Override
            public void onSynthesizeStart(String s) {

            }

            @Override
            public void onSynthesizeDataArrived(String s, byte[] bytes, int i) {

            }

            @Override
            public void onSynthesizeFinish(String s) {

            }

            @Override
            public void onSpeechStart(String s) {

            }

            @Override
            public void onSpeechProgressChanged(String s, int i) {

            }

            @Override
            public void onSpeechFinish(String s) {

            }

            @Override
            public void onError(String s, SpeechError speechError) {

            }
        });
        // 文本模型文件路径 (离线引擎使用)
        this.mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, mSampleDirPath + "/"
                + TEXT_MODEL_NAME);
        // 声学模型文件路径 (离线引擎使用)
        this.mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE, mSampleDirPath + "/"
                + SPEECH_FEMALE_MODEL_NAME);
        // 本地授权文件路径,如未设置将使用默认路径.设置临时授权文件路径，LICENCE_FILE_NAME请替换成临时授权文件的实际路径，仅在使用临时license文件时需要进行设置，如果在[应用管理]中开通了正式离线授权，不需要设置该参数，建议将该行代码删除（离线引擎）
        // 如果合成结果出现临时授权文件将要到期的提示，说明使用了临时授权文件，请删除临时授权即可。
        this.mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_LICENCE_FILE, mSampleDirPath + "/"
                + LICENSE_FILE_NAME);
        // 请替换为语音开发者平台上注册应用得到的App ID (离线授权)
        this.mSpeechSynthesizer.setAppId("9262249"/*这里只是为了让Demo运行使用的APPID,请替换成自己的id。*/);
        // 请替换为语音开发者平台注册应用得到的apikey和secretkey (在线授权)
        this.mSpeechSynthesizer.setApiKey("gMtyBG2jx1H6vGsVjMQoehko",
                "e7b6cafdd5fb1e460c843cb7df4b773a"/*这里只是为了让Demo正常运行使用APIKey,请替换成自己的APIKey*/);
        // 发音人（在线引擎），可用参数为0,1,2,3。。。（服务器端会动态增加，各值含义参考文档，以文档说明为准。0--普通女声，1--普通男声，2--特别男声，3--情感男声。。。）
        this.mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "0");
        // 设置Mix模式的合成策略
        this.mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_DEFAULT);
        // 授权检测接口(只是通过AuthInfo进行检验授权是否成功。)
        // AuthInfo接口用于测试开发者是否成功申请了在线或者离线授权，如果测试授权成功了，可以删除AuthInfo部分的代码（该接口首次验证时比较耗时），不会影响正常使用（合成使用时SDK内部会自动验证授权）
        AuthInfo authInfo = this.mSpeechSynthesizer.auth(TtsMode.MIX);

        if (authInfo.isSuccess()) {
            Toast.makeText(this, "语音合成授权成功", Toast.LENGTH_LONG).show();
        } else {
            String errorMsg = authInfo.getTtsError().getDetailMessage();
            Toast.makeText(this, "语音合成授权失败，错误码:" + errorMsg, Toast.LENGTH_LONG).show();
        }

        // 初始化tts
        mSpeechSynthesizer.initTts(TtsMode.MIX);
        // 加载离线英文资源（提供离线英文合成功能）
//        int result =
//                mSpeechSynthesizer.loadEnglishModel(mSampleDirPath + "/" + ENGLISH_TEXT_MODEL_NAME, mSampleDirPath
//                        + "/" + ENGLISH_SPEECH_FEMALE_MODEL_NAME);
        //Toast.makeText(this,"loadEnglishModel result=" + result,Toast.LENGTH_LONG).show();

        //打印引擎信息和model基本信息
        //printEngineInfo();
    }

    /**
     * 将sample工程需要的资源文件拷贝到SD卡中使用（授权文件为临时授权文件，请注册正式授权）
     *
     * @param isCover 是否覆盖已存在的目标文件
     * @param source
     * @param dest
     */
    private void copyFromAssetsToSdcard(boolean isCover, String source, String dest) {
        File file = new File(dest);
        if (isCover || (!isCover && !file.exists())) {
            InputStream is = null;
            FileOutputStream fos = null;
            try {
                is = getResources().getAssets().open(source);
                String path = dest;
                fos = new FileOutputStream(path);
                byte[] buffer = new byte[1024];
                int size = 0;
                while ((size = is.read(buffer, 0, 1024)) >= 0) {
                    fos.write(buffer, 0, size);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
