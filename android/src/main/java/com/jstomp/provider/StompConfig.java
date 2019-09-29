package com.jstomp.provider;

import android.annotation.SuppressLint;
import android.util.ArraySet;

import java.util.Arrays;
import java.util.Set;

/**
 * @company 上海道枢信息科技-->
 * @anthor created by jingzhanwu
 * @date 2018/1/23
 * @change
 * @describe describe
 * Stomp配置信息
 **/
@SuppressLint("NewApi")
public class StompConfig {
    /*操作成功*/
    public static final int STOMP_SEND_SUCCESS = 1;
    /*操作失败*/
    public static final int STOMP_SEND_FAIL = 0;
    /*初始化端点url*/
    private String url;
    /*发送消息url*/
    private String sendUrl;
    /*订阅广播的地址*/
    private Set<String> topicBroadCast = new ArraySet<>();
    /*订阅点对点的地址*/
    private Set<String> topic = new ArraySet<>();
    /*登陆用户名*/
    private String login;
    /*登陆密码*/
    private String passcode;


    public StompConfig(String url, String sendURL, String login, String passcode) {
        this.url = url;
        this.sendUrl = sendURL;
        this.login = login;
        this.passcode = passcode;
    }


    /**
     * p2p订阅地址，多个
     *
     * @param topicUrl
     * @return
     */

    public StompConfig topicUrl(String... topicUrl) {
        this.topic.addAll(Arrays.asList(topicUrl));
        return this;
    }

    /**
     * 广播订阅地址 多个
     *
     * @param broadcastUrl
     * @return
     */
    public StompConfig broadcastUrl(String... broadcastUrl) {
        this.topicBroadCast.addAll(Arrays.asList(broadcastUrl));
        return this;
    }

    /**
     * 返回发送url
     *
     * @return
     */
    public String getSendUrl() {
        return this.sendUrl;
    }

    /**
     * 返回点对点 订阅的地址
     *
     * @return
     */
    public Set<String> getTopicUrl() {
        return topic;
    }

    /**
     * 返回广播订阅的地址
     *
     * @return
     */
    public Set<String> getTopicBroadcastUrl() {
        return topicBroadCast;
    }


    /**
     * 返回最终初始化用的url
     *
     * @return
     */
    public String connectionUrl() {
        return url;
    }

    public String getLogin() {
        return login;
    }

    public String getPasscode() {
        return passcode;
    }
}
