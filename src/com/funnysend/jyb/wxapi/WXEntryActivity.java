package com.funnysend.jyb.wxapi;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.funnysend.jyb.*;
import com.tencent.mm.sdk.openapi.*;

public class WXEntryActivity extends ActionBarActivity implements IWXAPIEventHandler, View.OnClickListener {

    private static final String TAG = "WXEntryActivity";

    private static final int TIMELINE_SUPPORTED_VERSION = 0x21020001;

    private static final int REQUEST_CODE_IMG   = 1;
    private static final int REQUEST_CODE_MUSIC = 2;

    private static final int LARGE_SIZE = 1280;
    private static final int MAX_SIZE   = 1024;

    private LinearLayout errorLL;
    private Button       retry;

    private Button sendText;
    private Button sendPic;
    private Button sendMusic;
    private Button sendVideo;
    private Button sendWeb;
    private Button sendApp;

    private CheckBox isTimelineCb;

    private SharedPreferences.Editor editor;

    // IWXAPI 是第三方app和微信通信的openapi接口
    private IWXAPI api;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // 通过WXAPIFactory工厂，获取IWXAPI的实例
        api = WXAPIFactory.createWXAPI(this, Constants.APP_ID, false);

        api.registerApp(Constants.APP_ID);

        findView();

        if (!api.isWXAppInstalled()) {
            errorLL.setVisibility(View.VISIBLE);
        }

        SharedPreferences sharedPreferences = getSharedPreferences(Constants.SHARE_FILENAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        isTimelineCb.setChecked(sharedPreferences.getBoolean(Constants.SHARE_TIMELINE, true));

        api.handleIntent(getIntent(), this);
    }

    private boolean timelineSupport() {
        int wxSdkVersion = api.getWXAppSupportAPI();
        if (wxSdkVersion >= TIMELINE_SUPPORTED_VERSION) {
            Toast.makeText(WXEntryActivity.this, "当前版本微信不支持朋友圈，若想使用朋友圈功能，请更新微信", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void findView() {
        errorLL = (LinearLayout) findViewById(R.id.error_ll);
        retry = (Button) findViewById(R.id.retry);

        sendText = (Button) findViewById(R.id.send_text);
        sendPic = (Button) findViewById(R.id.send_pic);
        sendMusic = (Button) findViewById(R.id.send_music);
        sendVideo = (Button) findViewById(R.id.send_video);
        sendApp = (Button) findViewById(R.id.send_app);
        sendWeb = (Button) findViewById(R.id.send_web);
        isTimelineCb = (CheckBox) findViewById(R.id.timeline_cb);

        sendText.setOnClickListener(this);
        sendPic.setOnClickListener(this);
        sendMusic.setOnClickListener(this);
        sendVideo.setOnClickListener(this);
        sendApp.setOnClickListener(this);
        sendWeb.setOnClickListener(this);
        isTimelineCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !timelineSupport()) {
                    buttonView.setChecked(false);
                }
                editor.putBoolean(Constants.SHARE_TIMELINE, isChecked);
                editor.commit();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        api.handleIntent(intent, this);
    }

    // 微信发送请求到第三方应用时，会回调到该方法
    @Override
    public void onReq(BaseReq req) {

    }

    // 第三方应用发送到微信的请求处理后的响应结果，会回调到该方法
    @Override
    public void onResp(BaseResp resp) {
        int result;

        switch (resp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                result = R.string.errcode_success;
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                result = R.string.errcode_cancel;
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                result = R.string.errcode_deny;
                break;
            default:
                result = R.string.errcode_unknown;
                break;
        }

        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.send_text:
                intent = new Intent(WXEntryActivity.this, InputActivity.class);
                startActivity(intent);
                break;
            case R.id.send_pic:
                MMAlert.showAlert(this, getString(R.string.title_pic), this.getResources().getStringArray(R.array.arr_pic), null, new MMAlert.OnAlertSelectId() {
                    @Override
                    public void onClick(int whichButton) {
                        switch (whichButton) {
                            case 0:
                                Intent intent = new Intent();
                                intent.setType("image/*");
                                intent.setAction(Intent.ACTION_GET_CONTENT);
                                startActivityForResult(intent, REQUEST_CODE_IMG);
                                break;
                        }
                    }
                });
                break;
            case R.id.send_music:
                intent = new Intent(WXEntryActivity.this, MediaActivity.class);
                intent.putExtra(Constants.INTENT_TYPE, Constants.TYPE_MUSIC);
                startActivity(intent);
                break;
            case R.id.send_video:
                intent = new Intent(WXEntryActivity.this, MediaActivity.class);
                intent.putExtra(Constants.INTENT_TYPE, Constants.TYPE_VIDEO);
                startActivity(intent);
                break;
            case R.id.send_app:
                Toast.makeText(this, "还没弄", Toast.LENGTH_SHORT).show();
                break;
            case R.id.send_web:
                intent = new Intent(WXEntryActivity.this, MediaActivity.class);
                intent.putExtra(Constants.INTENT_TYPE, Constants.TYPE_WEB);
                startActivity(intent);
                break;
            case R.id.retry:
                if (api.isWXAppInstalled()) {
                    errorLL.setVisibility(View.GONE);
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_IMG:
                    Uri uri = data.getData();
                    String path = Util.getPath(this, uri);

                    Log.e(TAG, path);
//                    String path = "/storage/emulated/0/Music/恋と選挙とチョコレート - シグナルグラフ／Annabel/Jacket/IMG_0009.png";

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;

                    BitmapFactory.decodeFile(path, options);
                    int imageHeight = options.outHeight;
                    int imageWidth = options.outWidth;
                    int reqHeight = imageHeight;
                    int reqWidth = imageWidth;
                    float scale;

                    if (imageHeight > imageWidth) {
                        if (imageHeight > MAX_SIZE) {
                            reqHeight = MAX_SIZE;
                            scale = (float) reqHeight / imageHeight;
                            reqWidth = (int) (scale * imageWidth);
                        }
                    } else {
                        if (imageWidth > MAX_SIZE) {
                            reqWidth = MAX_SIZE;
                            scale = (float) reqWidth / imageWidth;
                            reqHeight = (int) (scale * imageHeight);
                        }
                    }

                    options.inSampleSize = Util.calculateInSampleSize(options, reqWidth, reqHeight);
                    options.inJustDecodeBounds = false;

                    if (imageHeight >= LARGE_SIZE || imageWidth >= LARGE_SIZE) {
                        Toast.makeText(this, "请稍候，图片过大时需要点时间", Toast.LENGTH_SHORT).show();
                    }


                    WXImageObject imgObj = new WXImageObject();
                    imgObj.setImagePath(path);

                    WXMediaMessage msg = new WXMediaMessage();
                    msg.mediaObject = imgObj;

                    Bitmap bmp = BitmapFactory.decodeFile(path, options);

                    if (bmp == null) {
                        Toast.makeText(this, "别折腾了，选个正确的格式吧……", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int w = bmp.getWidth();
                    int h = bmp.getHeight();
                    int thumbWidth = 200;
                    int thumbHeight = 150;
                    if (h > w) {
                        if (h < 150) {
                            thumbHeight = h;
                        }
                        scale = (float) thumbHeight / h;
                        thumbWidth = (int) (w * scale);
                    } else {
                        if (w < 200) {
                            thumbWidth = w;
                        }
                        scale = (float) thumbWidth / w;
                        thumbHeight = (int) (h * scale);
                    }

//                    Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, thumbWidth, thumbHeight, true);
                    Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, thumbWidth, thumbHeight, true);
                    if (thumbBmp != bmp) {
                        bmp.recycle();
                    }
                    msg.thumbData = Util.bmpToByteArray(thumbBmp, true);

                    Log.e(TAG, thumbBmp.getWidth() + " " + thumbBmp.getHeight() + " " + msg.thumbData.length);

                    SendMessageToWX.Req req = new SendMessageToWX.Req();
                    req.transaction = Util.buildTransaction("img");
                    req.message = msg;
                    req.scene = isTimelineCb.isChecked() ? SendMessageToWX.Req.WXSceneTimeline : SendMessageToWX.Req.WXSceneSession;
                    api.sendReq(req);
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


}