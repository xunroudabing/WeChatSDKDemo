package com.funnysend.jyb;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import com.tencent.mm.sdk.openapi.*;

/**
 * Created with IntelliJ IDEA.
 * User: Chaos
 * Date: 14/04/04
 * Time: 11:05
 */
public class MediaActivity extends ActionBarActivity {

    private static final String TAG = "MediaActivity";

    private EditText  urlText;
    private EditText  titleText;
    private EditText  contentText;
    private ImageView image;

    private Bitmap thumb;

    private int type;

    private IWXAPI api;

    private boolean isTimeline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media);

        api = WXAPIFactory.createWXAPI(this, Constants.APP_ID, false);

        urlText = (EditText) findViewById(R.id.url);
        titleText = (EditText) findViewById(R.id.title);
        contentText = (EditText) findViewById(R.id.content);
        image = (ImageView) findViewById(R.id.thumb_img);

        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent();
                /* 开启Pictures画面Type设定为image */
                intent1.setType("image/*");
                /* 使用Intent.ACTION_GET_CONTENT这个Action */
                intent1.setAction(Intent.ACTION_GET_CONTENT);
                /* 取得相片后返回本画面 */
                startActivityForResult(intent1, 1);
            }
        });

        thumb = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        image.setImageBitmap(thumb);

        isTimeline = getSharedPreferences(Constants.SHARE_FILENAME, MODE_PRIVATE).getBoolean(Constants.SHARE_TIMELINE, true);
        type = getIntent().getIntExtra(Constants.INTENT_TYPE, -1);

        //initActionBar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        switch (type) {
            case -1:
                Toast.makeText(this, "应用异常……", Toast.LENGTH_LONG).show();
                finish();
                break;
            case Constants.TYPE_MUSIC:
                actionBar.setTitle("分享音乐");
                break;
            case Constants.TYPE_VIDEO:
                actionBar.setTitle("分享视频");
                break;
            case Constants.TYPE_WEB:
                actionBar.setTitle("分享网页");
                break;
        }
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
                WXMediaMessage.IMediaObject object;
                String url = urlText.getText().toString().trim();
                if (url.equals("")) {
                    Toast.makeText(this, "URL不可为空", Toast.LENGTH_SHORT).show();
                    break;
                }

                WXMediaMessage msg = new WXMediaMessage();

                SendMessageToWX.Req req = new SendMessageToWX.Req();
                switch (type) {
                    case Constants.TYPE_MUSIC:
                        object = new WXMusicObject();
                        ((WXMusicObject) object).musicUrl = url;
                        req.transaction = Util.buildTransaction("music");
                        break;
                    case Constants.TYPE_VIDEO:
                        object = new WXVideoObject();
                        ((WXVideoObject) object).videoUrl = url;
                        req.transaction = Util.buildTransaction("video");
                        break;
                    default:
                        object = new WXWebpageObject();
                        ((WXWebpageObject) object).webpageUrl = url;
                        req.transaction = Util.buildTransaction("webpage");
                        break;
                }

                msg.mediaObject = object;
                msg.thumbData = Util.bmpToByteArray(thumb, false);
                msg.title = titleText.getText().toString();
                msg.description = contentText.getText().toString();

                req.transaction = Util.buildTransaction("text");
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 1:
                    Uri uri = data.getData();
                    String path = Util.getPath(this, uri);

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;

                    BitmapFactory.decodeFile(path, options);

                    options.inSampleSize = Util.calculateInSampleSize(options, 150, 150);
                    options.inJustDecodeBounds = false;

                    thumb = BitmapFactory.decodeFile(path, options);

                    image.setImageBitmap(thumb);
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}