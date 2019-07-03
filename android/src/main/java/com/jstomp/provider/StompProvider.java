package com.jstomp.provider;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.reactivex.CompletableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompCommand;
import ua.naiksoftware.stomp.dto.StompHeader;
import ua.naiksoftware.stomp.dto.StompMessage;

/**
 * @company 上海道枢信息科技-->
 * @anthor created by jingzhanwu
 * @date 2018/1/23
 * @change
 * @describe describe
 * Stomp消息处理
 **/
@SuppressLint("NewApi")
public class StompProvider {

    private static final String TAG = "StompProvider";
    private StompClient mStompClient;
    private static StompProvider instance;
    private Context mContext;
    /*统一的消息监听接口*/

    private OnMessageListener messageListener;
    /*全局发送监听*/
    private OnMessageSendListener globalSendStatusListener;
    /*连接监听*/
    private OnStompConnectionListener connectionListener;

    /*stomp连接信息配置类*/
    private StompConfig mConfig;
    /*标记服务是否已经启动*/
    public boolean stopService = false;

    public StompClient getStompClient() {
        return mStompClient;
    }

    private CompositeDisposable compositeDisposable;

    /**
     * stomp 的连接 关闭监听接口
     */
    public interface OnStompConnectionListener {
        void onConnectionOpened();//链接打开

        void onConnectionError(String error);//链接错误

        void onConnectionClosed();//链接关闭
    }

    /**
     * 总的消息监听接口
     */
    public interface OnMessageListener {
        void onBroadcastMessage(String stompMsg, String topicUrl);

        void onP2PMessage(String stompMsg, String topicUrl);
    }

    /**
     * stomp发送接口
     */
    public interface OnMessageSendListener {
        void onSendMessage(int status, String userMsg, String tipsMsg);
    }


    private StompProvider() {
    }

    public static StompProvider get() {
        if (instance == null) {
            synchronized (StompProvider.class) {
                if (instance == null) {
                    instance = new StompProvider();
                }
            }
        }
        return instance;
    }

    /**
     * 重置、断开订阅者
     */
    private void resetSubscriptions() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
        compositeDisposable = new CompositeDisposable();
    }

    /**
     * 获取当前配置
     *
     * @return
     */
    public StompConfig getConfig() {
        return mConfig;
    }

    /**
     * 初始化操作,
     *
     * @param config 自定义配置信息
     */
    public boolean init(Context context, StompConfig config) {
        try {
            resetSubscriptions();
            this.mContext = context;
            this.mConfig = config;
            String url = config.connectionUrl();
            mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, url);
            Log.d(TAG, "Stomp 初始化--url:" + url);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 启动stomp 消息服务
     */
    @TargetApi(Build.VERSION_CODES.O)
    public StompProvider openConnection(OnStompConnectionListener listener) {
        if (mContext == null) {
            return this;
        }
        try {
            connectionListener = listener;
            //如果StompService 已经启动了并且service没有销毁则不用重新启动服务，
            //只需要重新注册Stomp监听即可
            if (!stopService && StompService.GET() != null) {
                StompService.GET().registerStompConnectionListener();
                return this;
            }

            Intent intent = new Intent(mContext, StompService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //android8.0以上通过startForegroundService启动service
                mContext.startForegroundService(intent);
            } else {
                mContext.startService(intent);
            }
            stopService = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * 重新连接stomp
     *
     * @return
     */
    public void reConnection() {
        if (mContext == null || mConfig == null) {
            return;
        }
        //先断开连接
        // disconnect();
        //重新初始化
        boolean b = init(mContext, mConfig);
        if (b) {
            Log.i(TAG, "正在进行stomp重连");
            openConnection(connectionListener);
        }
    }

    /**
     * 订阅p2p,没有指定url时从配置信息中获取
     *
     * @return
     */
    public StompProvider subscriber() {
        if (mConfig != null && mConfig.getTopicUrl() != null) {
            String[] urls = (String[]) mConfig.getTopicUrl().toArray();
            return subscriber(urls);
        }
        return this;
    }

    /**
     * 订阅p2p
     *
     * @param topicUrl
     * @return
     */
    public StompProvider subscriber(String... topicUrl) {
        if (null == topicUrl || topicUrl.length == 0) {
            Log.i(TAG, "p2p订阅地址为空--");
            return this;
        }
        mConfig.topicUrl(topicUrl);
        for (String url : topicUrl) {
            Log.i(TAG, "P2P订阅:" + url);
            Disposable dispCast = mStompClient.topic(url)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(topicMessage -> {
                        Log.i(TAG, topicMessage.getPayload());
                        if (null != messageListener) {
                            messageListener.onP2PMessage(topicMessage.getPayload(), url);
                        }
                    });
            compositeDisposable.add(dispCast);
        }
        return this;
    }

    public StompProvider subscriberBroadcast() {
        if (mConfig != null && mConfig.getTopicBroadcastUrl() != null) {
            String[] urls = (String[]) mConfig.getTopicBroadcastUrl().toArray();
            return subscriberBroadcast(urls);
        }
        return this;
    }

    /**
     * 订阅广播
     *
     * @param broadCast
     * @return
     */
    public StompProvider subscriberBroadcast(String... broadCast) {
        if (null == broadCast || broadCast.length == 0) {
            Log.i(TAG, "广播订阅地址为空--");
            return this;
        }
        mConfig.broadcastUrl(broadCast);
        for (String url : broadCast) {
            Disposable dispTopic = mStompClient.topic(url)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(broadcastTopicMessage -> {
                        Log.d(TAG, "broadcastMessage: " + broadcastTopicMessage.getPayload());
                        if (messageListener != null) {
                            messageListener.onBroadcastMessage(broadcastTopicMessage.getPayload(), url);
                        }
                    });
            compositeDisposable.add(dispTopic);
        }
        return this;
    }

    /**
     * 停止stomp的服务
     */
    private void stopStompService() {
        if (mContext == null) {
            return;
        }
        try {
            Intent intent = new Intent(mContext, StompService.class);
            mContext.stopService(intent);
            stopService = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 连接stomp服务
     *
     * @param connectionListener
     */
    protected void connect(OnStompConnectionListener connectionListener) {
        connect(connectionListener, null);
    }

    /**
     * 打开链接
     */
    protected void connect(OnStompConnectionListener callback, List<StompHeader> headers) {
        if (mStompClient == null) {
            return;
        }
        if (headers != null && headers.size() > 0) {
            mStompClient.connect(headers);
        } else {
            mStompClient.connect();
        }

        if (mStompClient.isConnected()) {
            Log.i(TAG, "Stomp 链接已经打开，无需重连");
            return;
        }
        try {
            Disposable dispLifecycle = mStompClient.lifecycle()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(lifecycleEvent -> {
                        switch (lifecycleEvent.getType()) {
                            case OPENED:
                                Log.i(TAG, "Stomp 链接打开");
                                callback.onConnectionOpened();
                                if (connectionListener != null) {
                                    connectionListener.onConnectionOpened();
                                }
                                break;
                            case ERROR:
                                Log.e(TAG, "Stomp 连接错误" + lifecycleEvent.getException());
                                String error = "Stomp 错误 " + (lifecycleEvent.getException() == null ? "" : lifecycleEvent.getException().toString());
                                callback.onConnectionError(error);
                                if (connectionListener != null) {
                                    connectionListener.onConnectionError(error);
                                }
                                break;
                            case CLOSED:
                                Log.e(TAG, "Stomp 连接关闭");
                                callback.onConnectionClosed();
                                if (connectionListener != null) {
                                    connectionListener.onConnectionClosed();
                                }
                                break;
                        }
                    });

            compositeDisposable.add(dispLifecycle);

        } catch (Exception e) {
            e.printStackTrace();
            callback.onConnectionError(e.getMessage());
            if (connectionListener != null) {
                connectionListener.onConnectionError(e.getMessage());
            }
        }
    }


    /**
     * 断开链接
     */
    private void disconnect() {
        if (mStompClient != null) {
            mStompClient.disconnect();
            stopStompService();
            mStompClient = null;
        }
    }

    public boolean isConnecting() {
        if (mStompClient == null) {
            return false;
        }
        return mStompClient.isConnected();
    }

    /**
     * 销毁相关资源
     */
    public void destroy() {
        disconnect();
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
            compositeDisposable = null;
        }
        messageListener = null;
        globalSendStatusListener = null;
        connectionListener = null;
        mConfig = null;
    }

    /**
     * 统一的消息监听注册入口
     *
     * @param listener
     */
    public StompProvider setOnMessageListener(OnMessageListener listener) {
        if (listener == null) {
            Log.d(TAG, "OnMessageListener is null");
            return this;
        }
        messageListener = listener;
        return this;
    }

    /**
     * 注册全局发送监听
     *
     * @param listener
     */
    public StompProvider setOnMessageSendListener(OnMessageSendListener listener) {
        if (listener == null) {
            Log.d(TAG, "registerStompGlobalSenderListener: listener is null");
            return this;
        }
        globalSendStatusListener = listener;
        return this;
    }


    /**
     * 发送消息
     *
     * @param jsonMsg 消息文本
     */
    public void sendMessage(String jsonMsg) {
        sendMessage(jsonMsg, null);
    }

    /**
     * 发送消息，带自定义头
     *
     * @param jsonMsg
     * @param header
     */
    public void sendMessage(String jsonMsg, Map<String, String> header) {
        List<StompHeader> stompHeaders = new ArrayList<>();
        StompHeader defaultHeader = new StompHeader(StompHeader.DESTINATION, mConfig.getSendUrl());
        stompHeaders.add(defaultHeader);

        //如果有自定义头，则一一添加进去
        if (header != null && header.size() > 0) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                stompHeaders.add(new StompHeader(entry.getKey(), entry.getValue()));
            }
        }
        //构造stomp消息体
        StompMessage message = new StompMessage(StompCommand.SEND, stompHeaders, jsonMsg);

        sendMessage(message);
    }

    /**
     * 发送消息并回调监听
     *
     * @param sender
     */
    private void sendMessage(StompMessage sender) {
        compositeDisposable.add(mStompClient.send(sender)
                .compose(applySchedulers())
                .subscribe(() -> {
                    Log.d(TAG, "Stomp消息发送成功" + sender.getPayload());
                    handleSendResultMessage(StompConfig.STOMP_SEND_SUCCESS, sender);
                }, throwable -> {
                    Log.e(TAG, "Stomp消息发送失败", throwable);
                    handleSendResultMessage(StompConfig.STOMP_SEND_FAIL, sender);
                }));
    }


    /**
     * 处理发送消息回调结果
     *
     * @param status 发送消息状态
     * @param sender 发送的消息
     */
    private void handleSendResultMessage(int status, StompMessage sender) {
        //全局点对点发送监听
        if (null != globalSendStatusListener) {
            globalSendStatusListener.onSendMessage(status, sender.getPayload(), status == StompConfig.STOMP_SEND_SUCCESS ? "发送成功" : "发送失败");
        }
    }

    /**
     * 从stompMessage 中获取 消息的内容
     *
     * @param message
     * @return
     */
    public UserMessageEntry parseStompMessage(StompMessage message) {
        if (message == null) {
            return null;
        }

        String content = message.getPayload();
        if (TextUtils.isEmpty(content)) {
            return null;
        }
        UserMessageEntry msg = new Gson().fromJson(content, UserMessageEntry.class);
        String createTime = msg.getCreateTime();
        if (createTime != null) {
            //判断是否是纯数字
            boolean isNumber = createTime.matches("^\\d+$");
            if (isNumber) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                msg.setCreateTime(sdf.format(new Date(Long.parseLong(createTime))));
            }
        }

        return msg;
    }

    private CompletableTransformer applySchedulers() {
        return upstream -> upstream
                .unsubscribeOn(Schedulers.newThread())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

}
