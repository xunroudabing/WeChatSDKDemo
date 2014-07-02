package com.funnysend.jyb;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import com.tencent.mm.sdk.openapi.*;

/**
 * Created with IntelliJ IDEA.
 * User: Chaos
 * Date: 14/04/03
 * Time: 22:34
 */
public class InputActivity extends ActionBarActivity {

    private EditText input;

    private IWXAPI api;

    private boolean isTimeline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_activity);

        api = WXAPIFactory.createWXAPI(this, Constants.APP_ID, false);
        input = (EditText) findViewById(R.id.input);

        //initActionBar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        isTimeline = getSharedPreferences(Constants.SHARE_FILENAME, MODE_PRIVATE).getBoolean(Constants.SHARE_TIMELINE, true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.input_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ab_save:
                String text = input.getText().toString().trim();
                if (text.equals("")) {
                    break;
                }
                WXTextObject textObj = new WXTextObject();
                textObj.text = input.getText().toString();

                // 用WXTextObject对象初始化一个WXMediaMessage对象
                WXMediaMessage msg = new WXMediaMessage();
                msg.mediaObject = textObj;
                // 发送文本类型的消息时，title字段不起作用
                // msg.title = "Will be ignored";
                msg.description = input.getText().toString();

                // 构造一个Req
                SendMessageToWX.Req req = new SendMessageToWX.Req();
                req.transaction = Util.buildTransaction("text"); // transaction字段用于唯一标识一个请求
                req.message = msg;
                req.scene = isTimeline ? SendMessageToWX.Req.WXSceneTimeline : SendMessageToWX.Req.WXSceneSession;

                // 调用api接口发送数据到微信
                api.sendReq(req);
                return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
