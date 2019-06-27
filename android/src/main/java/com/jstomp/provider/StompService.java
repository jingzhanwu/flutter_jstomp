package com.jstomp.provider;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.Nullable;


/**
 * @company 上海道枢信息科技-->
 * @anthor created by jingzhanwu
 * @date 2018/2/26 0026
 * @change
 * @describe stomp消息服务
 **/
public class StompService extends Service {
    private final static String TAG = "StompService";

    public static final int NOTIFICATION_ID = 0x11;

    private final IBinder mBinder = new LocalBinder();

    /**
     * socket 最大重连尝试时间 15分钟
     */
    private static final int MAX_RE_CONN_TIME = 1000 * 60 * 15;
    /**
     * socket重连执行间隔 单位秒
     */
    private static final int CONN_STEP = 1000 * 10;
    /**
     * 当前已经重连尝试的时间 单位毫秒
     */
    private int mConnTime = 0;
    /**
     * 执行socket重连的定时器
     */
    private Timer timer;
    private static final String CHANNEL_ID = "command_channel";

    private static StompService mInstance;

    public static StompService GET() {
        return mInstance;
    }

    public class LocalBinder extends Binder {
        StompService getService() {
            return StompService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public int onStartCommand(Intent _intent, int flags, int startId) {
        mInstance = this;
        Log.i(TAG, "准备开启 StompService");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, createNotification26());
            startService(new Intent(this, InnerService.class));
        } else {
            //Android7.1 google修复了此漏洞，暂无解决方法（现状：Android7.1以上app启动后通知栏会出现一条"正在运行"的通知消息）
            startForeground(NOTIFICATION_ID, new Notification());
        }
        registerStompConnectionListener();
        return Service.START_STICKY;
    }


    /**
     * 添加stomp链接监听
     */
    public void registerStompConnectionListener() {
        StompProvider.get().connect(new StompProvider.OnStompConnectionListener() {
            @Override
            public void onConnectionOpened() {
                //取消定时器
                cancelTimer();
            }

            @Override
            public void onConnectionError(String error) {
                Log.e(TAG, "Stomp 错误" + error);
            }

            @Override
            public void onConnectionClosed() {
                Log.e(TAG, "Stomp 关闭");
                startConnTimer();
            }
        });
    }


    /**
     * 内部服务，API18 以上 的服务保活机制，
     */
    public class InnerService extends Service {
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onCreate() {
            super.onCreate();
            //发送与StompService中ID相同的Notification，然后将其取消并取消自己的前台显示
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForeground(NOTIFICATION_ID, createNotification26());
            } else {
                //Android7.1 google修复了此漏洞，暂无解决方法（现状：Android7.1以上app启动后通知栏会出现一条"正在运行"的通知消息）
                startForeground(NOTIFICATION_ID, new Notification());
            }

            new Handler().postDelayed(() -> {
                stopForeground(true);
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    manager.deleteNotificationChannel(CHANNEL_ID);
                }
                manager.cancel(NOTIFICATION_ID);
                stopSelf();
            }, 500);
        }
    }

    @SuppressLint("NewApi")
    private Notification createNotification26() {
        // 设置点击通知跳转的Intent ,打开应用
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        // 设置 延迟Intent
        // 最后一个参数可以为PendingIntent.FLAG_CANCEL_CURRENT 或者 PendingIntent.FLAG_UPDATE_CURRENT
        PendingIntent pendingIntent = PendingIntent.getActivity(this, NOTIFICATION_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "通知栏保活", NotificationManager.IMPORTANCE_HIGH);

        channel.enableLights(true); //是否在桌面icon右上角展示小红点
        channel.setLightColor(Color.GREEN); //小红点颜色
        channel.setShowBadge(true); //是否在久按桌面图标时显示此渠道的通知

        channel.enableVibration(false);
        channel.setBypassDnd(true);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);


        Notification.Builder builder = new Notification.Builder(this.getApplicationContext(), CHANNEL_ID)
                .setContentTitle("") // 设置下拉列表里的标题
                .setContentText("点击打开应用") // 设置详细内容
                .setContentIntent(pendingIntent) // 设置点击跳转的界面
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())// 设置该通知发生的时间
                .setPriority(Notification.PRIORITY_HIGH);  //优先级
        return builder.build();
    }

    /**
     * 开启重连定时任务
     */
    public void startConnTimer() {
        //如果已经达到了重连尝试最大时间或者已经连接上，则取消定时器
        if (mConnTime >= MAX_RE_CONN_TIME || StompProvider.get().isConnecting()) {
            cancelTimer();
            return;
        }
        //如果定时器不为null，则证明正在执行定时任务
        if (timer != null) {
            return;
        }
        //初始化一个定时器
        timer = new Timer();
        //开启定时任务，每间隔10s执行一次
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(0);
                //记录已经连接的时间
                mConnTime += CONN_STEP;
            }
        }, 0, CONN_STEP);
    }

    /**
     * 取消重连定时任务
     */
    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        mConnTime = 0;
    }

    /**
     * 处理重连的handler
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            reConnSocket();
        }
    };

    /**
     * socket重连
     */
    private void reConnSocket() {
        try {
            if (StompProvider.get().isConnecting()) {
                cancelTimer();
                return;
            }
            StompProvider.get().stopService = false;
            StompProvider.get().reConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        StompProvider.get().stopService = true;
        // 停止前台服务--参数：表示是否移除之前的通知
        stopForeground(true);
    }
}
