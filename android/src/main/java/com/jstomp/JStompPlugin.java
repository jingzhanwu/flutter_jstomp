package com.jstomp;

import android.app.Activity;
import android.util.Log;

import com.jstomp.provider.StompConfig;
import com.jstomp.provider.StompProvider;
import com.jstomp.provider.UserMessageEntry;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * JstompPlugin
 *
 * @anthor created by jingzhanwu
 * @date 2019-06-24
 * @change
 * @describe Stomp Android插件
 **/
public class JStompPlugin implements MethodCallHandler {
    private Activity activity;

    private MethodChannel channel;

    public JStompPlugin(Activity act, MethodChannel channel) {
        this.activity = act;
        this.channel = channel;
        handlerRxError();
    }

    /**
     * rxjava错误处理
     */
    private void handlerRxError() {
        //统一处理Rxjava的error
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Log.e("JStompPlugin--", throwable.getMessage());
            }
        });
    }


    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "jstomp");
        channel.setMethodCallHandler(new JStompPlugin(registrar.activity(), channel));
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        String method = call.method;
        try {
            switch (method) {
                case FlutterCall.INIT://初始化
                    String url = call.argument("url");
                    String sendUrl = call.argument("sendUrl");
                    boolean b = init(url, sendUrl);
                    result.success(b);
                    break;
                case FlutterCall.DESTROY: //销毁，断开
                    boolean d = destroy();
                    result.success(d);
                    break;
                case FlutterCall.CONNECTION://连接
                    boolean c = connection();
                    result.success(c);
                    break;
                case FlutterCall.SEND_MESSAGE: //发送消息
                    Map<String, String> header = null;
                    if (call.hasArgument("header")) {
                        header = (Map<String, String>) call.argument("header");
                    }
                    String str = sendMessage(call.argument("msg"), header);
                    result.success(str);
                    break;
                case FlutterCall.SUBSCRIBER_P2P://订阅p2p
                    String[] urls = call.arguments.toString().split(",");
                    boolean s = subscriberP2P(urls);
                    result.success(s);
                    break;
                case FlutterCall.SUBSCRIBER_BROADCAST: //订阅广播
                    String[] burls = call.arguments.toString().split(",");
                    boolean sb = subscriberBroadcast(burls);
                    result.success(sb);
                    break;
                case FlutterCall.MESSAGE_CALLBACK: //设置消息回调
                    boolean sm = setMessageCallback();
                    result.success(sm);
                    break;
                case FlutterCall.SEND_CALLBACK: //设置发送回调
                    boolean ss = setSendCallback();
                    result.success(ss);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            result.success(Boolean.FALSE);
        }
    }


    /**
     * stomp初始化
     *
     * @param url
     * @param sendUrl
     */
    private boolean init(String url, String sendUrl) {
        try {
            return StompProvider.get().init(activity.getApplicationContext(), new StompConfig(url, sendUrl));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 资源销毁
     *
     * @return
     */
    private boolean destroy() {
        try {
            StompProvider.get().destroy();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 打开连接
     */
    private boolean connection() {
        try {
            StompProvider.get().openConnection(new StompProvider.OnStompConnectionListener() {
                @Override
                public void onConnectionOpened() {
                    //连接打开，通知flutter
                    channel.invokeMethod(CallFlutter.ON_CONNECTION_OPENED, Boolean.TRUE);
                }

                @Override
                public void onConnectionError(String error) {
                    //连接错误，通知flutter
                    channel.invokeMethod(CallFlutter.ON_CONNECTION_ERROR, error);
                }

                @Override
                public void onConnectionClosed() {
                    //连接关闭，通知flutter
                    channel.invokeMethod(CallFlutter.ON_CONNECTION_CLOSED, Boolean.FALSE);
                }
            });
            return true;
        } catch (Exception e) {
            //连接错误，通知flutter
            channel.invokeMethod(CallFlutter.ON_CONNECTION_ERROR, e.getMessage());
            return false;
        }
    }

    /**
     * 订阅p2p
     *
     * @param url
     */
    private boolean subscriberP2P(String[] url) {
        try {
            StompProvider.get().subscriber(url);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 订阅广播
     *
     * @param url
     */
    private boolean subscriberBroadcast(String[] url) {
        try {
            StompProvider.get().subscriberBroadcast(url);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 发送消息
     *
     * @param message 必须是json字串
     * @return
     */
    private String sendMessage(String message, Map<String, String> header) {
        try {
            StompProvider.get().sendMessage(message, header);
            return message;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 消息监听器
     *
     * @return
     */
    private boolean setMessageCallback() {
        try {
            StompProvider.get().setOnMessageListener(new StompProvider.OnMessageListener() {
                @Override
                public void onBroadcastMessage(String stompMsg, String topicUrl) {
                    channel.invokeMethod(CallFlutter.ON_BROAD_CAST, stompMsg);
                }

                @Override
                public void onP2PMessage(String stompMsg, String topicUrl) {
                    channel.invokeMethod(CallFlutter.ON_MESSAGE, stompMsg);
                }
            });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 添加发送成功，失败监听器
     *
     * @return
     */
    private boolean setSendCallback() {
        try {
            StompProvider.get().setOnMessageSendListener(new StompProvider.OnMessageSendListener() {
                @Override
                public void onSendMessage(int status, String userMsg, String tipsMsg) {
                    Map<String, Object> map = new HashMap();
                    map.put("msg", userMsg);
                    map.put("status", status);
                    channel.invokeMethod(CallFlutter.ON_SEND, map);
                }
            });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * 定义flutter调用native的方法
     */
    class FlutterCall {
        static final String INIT = "init";
        static final String CONNECTION = "connection";
        static final String SUBSCRIBER_P2P = "subscriberP2P";
        static final String SUBSCRIBER_BROADCAST = "subscriberBroadcast";
        static final String DESTROY = "destroy";
        static final String SEND_MESSAGE = "sendMessage";

        static final String MESSAGE_CALLBACK = "setMessageCallback";
        static final String SEND_CALLBACK = "setSendCallback";
    }

    /**
     * 定义native调用flutter的方法
     */
    class CallFlutter {
        static final String ON_CONNECTION_OPENED = "onConnectionOpen";
        static final String ON_CONNECTION_ERROR = "onConnectionError";
        static final String ON_CONNECTION_CLOSED = "onConnectionClosed";

        static final String ON_MESSAGE = "onMessage";
        static final String ON_BROAD_CAST = "onBroadcastMessage";
        static final String ON_SEND = "onSend";
    }

    /**
     * 将stonp消息转换成map
     *
     * @param userMsg
     * @return
     */
    private Map<String, Object> parserMsg(UserMessageEntry userMsg) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", userMsg.getId());
        map.put("content", userMsg.getContent());
        map.put("createId", userMsg.getCreateId());
        map.put("microGroupName", userMsg.getMicroGroupName());
        map.put("createName", userMsg.getCreateName());
        map.put("createTime", userMsg.getCreateTime());
        map.put("headUrl", userMsg.getHeadUrl());
        map.put("microGroupId", userMsg.getMicroGroupId());
        map.put("path", userMsg.getPath());
        map.put("localPath", userMsg.getLocalPath());
        map.put("obj", userMsg.getObj());
        map.put("type", userMsg.getType());

        map.put("sendState", userMsg.getStatus());//发送状态
        map.put("direct", 1);//接受
        map.put("status", 1);//状态，成功
        map.put("isCrowd", 0);
        return map;
    }
}
