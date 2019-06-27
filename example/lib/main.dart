import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:jstomp/jstomp.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Stomp Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: MyHomePage(title: 'Stomp Example'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key key, this.title}) : super(key: key);

  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  JStomp stomp;

  String _initState = "";
  String _connectionState = "";
  String _subscriberState = "";
  String _content = "";
  String _sendContent = "";

  @override
  void initState() {
    super.initState();
    stomp = JStomp.instance;
//    _initStomp();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Center(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: <Widget>[
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  "初始化状态：",
                  style: TextStyle(color: Colors.blue),
                ),
                Expanded(
                  child: Text(
                    _initState ?? "",
                  ),
                ),
              ],
            ),
            Divider(height: 1.0),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  "连接状态：",
                  style: TextStyle(color: Colors.blue),
                ),
                Expanded(
                  child: Text(
                    _connectionState ?? "",
                  ),
                ),
              ],
            ),
            Divider(height: 1.0),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  "订阅状态：",
                  style: TextStyle(color: Colors.blue),
                ),
                Expanded(
                  child: Text(
                    _subscriberState ?? "",
                  ),
                ),
              ],
            ),
            Divider(height: 1.0),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  "新消息：",
                  style: TextStyle(color: Colors.blue),
                ),
                Expanded(
                  child: Text(
                    _content ?? "",
                  ),
                ),
              ],
            ),
            Divider(height: 1.0),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  "发送的消息：",
                  style: TextStyle(color: Colors.blue),
                ),
                Expanded(
                  child: Text(
                    _sendContent ?? "",
                  ),
                ),
              ],
            ),
            Divider(height: 1.0),
            RaisedButton(
              onPressed: () {
                _initStomp();
              },
              textColor: Colors.white,
              highlightColor: Colors.blueAccent,
              splashColor: Colors.blue,
              color: Colors.blue,
              child: Text("初始化并打开连接"),
            ),
            RaisedButton(
              onPressed: () {
                _sendMsg();
              },
              textColor: Colors.white,
              highlightColor: Colors.blueAccent,
              splashColor: Colors.blue,
              color: Colors.blue,
              child: Text("发送消息"),
            ),
            RaisedButton(
              onPressed: () {
                _destroyStomp();
              },
              textColor: Colors.white,
              highlightColor: Colors.blueAccent,
              splashColor: Colors.blue,
              color: Colors.blue,
              child: Text("断开并销毁资源"),
            ),
          ],
        ),
      ),
    );
  }

  void _initStateChanged(String str) {
    setState(() {
      _initState = str;
    });
  }

  void _connectionStateChanged(String state) {
    setState(() {
      _connectionState = state;
    });
  }

  void _messageStateChanged(String msg) {
    setState(() {
      _content = msg;
    });
  }

  void _sendStateChanged(String send) {
    setState(() {
      _sendContent = send;
    });
  }

  ///
  ///初始化并连接stomp
  ///
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

  ///
  /// 断开连接，销毁资源
  ///
  Future<bool> _destroyStomp() async {
    if (stomp == null) {
      return true;
    }
    bool b = await stomp.destroy();
    stomp = null;
    return b;
  }
}
