package com.jstomp.provider;


import java.io.Serializable;

/**
 * @company 上海道枢信息科技-->
 * @anthor created by jingzhanwu
 * @date 2018/3/1 0001
 * @change
 * @describe 用户消息类
 **/
public class UserMessageEntry implements Serializable {
    private String id;
    /*内容*/
    private String content;
    /*创建者id*/
    private String createId;
    /*创建者名称*/
    private String createName;
    /*创建时间*/
    private String createTime;
    /*用户头像地址*/
    private String headUrl;
    /*接受消息的微群id*/
    private String microGroupId;
    /*图片、视频、音频文件地址*/
    private String path;
    /*自定义字段 文件本地路劲*/
    private String localPath;

    /*音视频 图片 文件的附加字段*/
    private String obj;
    /*微群名称*/
    private String microGroupName;

    /**
     * 消息类型
     * -1 系统消息
     * 0 文本消息
     * 1 图片
     * 2 音频
     * 3 视频
     * 4 附加文件
     */
    private int type;

    /**
     * 自定义字段
     * 消息的状态 0=发送成功 1==发送失败  -1==重发成功
     */
    private int status;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreateId() {
        return createId;
    }

    public void setCreateId(String createId) {
        this.createId = createId;
    }

    public String getCreateName() {
        return createName;
    }

    public void setCreateName(String createName) {
        this.createName = createName;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getHeadUrl() {
        return headUrl;
    }

    public void setHeadUrl(String headUrl) {
        this.headUrl = headUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMicroGroupId() {
        return microGroupId;
    }

    public void setMicroGroupId(String microGroupId) {
        this.microGroupId = microGroupId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getObj() {
        return obj;
    }

    public void setObj(String obj) {
        this.obj = obj;
    }


    @Override
    public String toString() {
        return "UserMessageEntry{" +
                "id='" + id + '\'' +
                ", content='" + content + '\'' +
                ", createId='" + createId + '\'' +
                ", createName='" + createName + '\'' +
                ", createTime='" + createTime + '\'' +
                ", headUrl='" + headUrl + '\'' +
                ", microGroupId='" + microGroupId + '\'' +
                ", path='" + path + '\'' +
                ", localPath='" + localPath + '\'' +
                ", obj='" + obj + '\'' +
                ", type=" + type +
                ", status=" + status +
                '}';
    }

    public String getMicroGroupName() {
        return microGroupName;
    }

    public void setMicroGroupName(String microGroupName) {
        this.microGroupName = microGroupName;
    }
}
