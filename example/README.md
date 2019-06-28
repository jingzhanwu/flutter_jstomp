# jstomp_example

  目前只支持Android，下面看一下具体使用的步骤
  1、初始化
  2、打开连接
  3、订阅通道
  4、添加监听
  5、发送消息

## Example

```
      ///初始化并连接stomp
      Future _initStomp() async {
        if (stomp == null) {
          stomp = JStomp.instance;
        }
        String userId = "104435390701569";
        String url = "ws://192.168.1.223:9990/message/websocket?personId=" + userId;
        bool b = await stomp.init(url: url, sendUrl: "/groupMessage/sendMessage");

        _initStateChanged(b ? "初始化成功" : "初始化失败");

        if (b) {
          ///打开连接
          await stomp.connection((open) {
            print("连接打开了...$open");
            _connectionStateChanged("Stomp连接打开了...");
          }, onError: (error) {
            print("连接打开错误了...$error");
            _connectionStateChanged("Stomp连接出错了：$error");
          }, onClosed: (closed) {
            print("连接打开错误了...$closed");
            _connectionStateChanged("Stomp连接关闭了...");
          });
        }

        ///订阅点对点通道
        final String p2p = "/groupMessage/channel/" + userId;
        await stomp.subscribP2P([p2p]);

        ///订阅广播通道
        await stomp.subscribBroadcast(["groupBroadcast/message"]);

        setState(() {
          _subscriberState = "通道订阅完成：" + p2p;
        });

        ///添加消息回调
        await stomp.onMessageCallback((message) {
          print("收到p2p新消息：" + message.toString());
          _messageStateChanged("收到p2p新消息：" + message.toString());
        }, onBroadCast: (cast) {
          print("收到新广播消息：" + cast.toString());
          _messageStateChanged("收到广播新消息：" + cast.toString());
        });

        ///添加发送回调
        await stomp.onSendCallback((status, sendMsg) {
          print("消息发送完毕：$status :msg=" + sendMsg.toString());
          _sendStateChanged("发送了一条消息：$status :msg=" + sendMsg.toString());
        });
      }

      ///
      /// 发送消息
      ///
      Future<String> _sendMsg() async {
        Map<String, dynamic> msg = {
          "content": "flutter发送的消息",
          "createId": "1143077861691756546",
          "createName": "陈晨",
          "createTime": "2019-06-24 17:03:51",
          "id": "1046324312976343042",
          "microGroupId": "1143049991384731649",
          "microGroupName": "flutter讨论群",
          "type": 0
        };

        Map<String, dynamic> head = {
          "userId": "p123456",
          "token": "MgjkjkdIdkkDkkjkfdjfdkjfk",
        };
        return await stomp.sendMessage(json.encode(msg), header: head);
      }

      /// 断开连接，销毁资源
      Future<bool> _destroyStomp() async {
        if (stomp == null) {
          return true;
        }
        bool b = await stomp.destroy();
        stomp = null;
        return b;
      }

```

以上就是整个Stomp库使用的基本步骤。





















