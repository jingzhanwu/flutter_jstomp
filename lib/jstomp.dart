import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

///定义消息回调函数,并制定参数类型
typedef OnMessageCallback = Function(dynamic jsonMsg);

///定义消息发送回调函数
typedef OnSendMessageCallback = Function(SendStatus status, dynamic jsonMsg);

class JStomp {
  JStomp._();

  static JStomp _instance;

  factory JStomp() => _getInstance();

  static JStomp get instance => _getInstance();

  ///channel实例
  MethodChannel _channel;

  ///接受消息stream
  StreamController<_OnMessageData> _messageController;

  ///连接回到stream
  StreamController<_OnConnectionData> _connectionController;

  ///发送消息数据stream
  // ignore: close_sinks
  StreamController<_OnSendMessageData> _sendController;

  JStomp._init() {
    ///初始化
    _channel = const MethodChannel('jstomp');
    _connectionController = new StreamController.broadcast();
    _messageController = new StreamController.broadcast();
    _sendController = new StreamController.broadcast();
  }

  ///
  /// 返回实例对象
  ///
  static JStomp _getInstance() {
    if (_instance == null) {
      _instance = JStomp._init();
    }
    return _instance;
  }

  ///
  /// stomp初始化
  ///
  Future<bool> init({@required String url, @required String sendUrl}) async {
    ///添加native方法调用处理方法
    _channel.setMethodCallHandler(_nativeHandle);

    Map<String, String> params = {
      "url": url,
      "sendUrl": sendUrl,
    };
    bool result = await _channel.invokeMethod(_NativeMethod.INIT, params);
    return result;
  }

  ///
  /// 打开stomp连接
  ///
  Future<bool> connection(ValueChanged onOpen,
      {ValueChanged onError, ValueChanged onClosed}) async {
    ///先注册连接状态监听器
    _onConnectionCallback(onOpen, onError, onClosed);
    return await _channel.invokeMethod(_NativeMethod.CONNECTION);
  }

  ///
  /// stomp连接监听器
  ///
  void _onConnectionCallback(
      ValueChanged onOpen, ValueChanged onError, ValueChanged onClosed) {
    _connectionController.stream.listen((callback) {
      switch (callback.call) {
        case _Connection.OPEN:
          if (onOpen != null) {
            onOpen(callback.state);
          }
          break;
        case _Connection.ERROR:
          if (onError != null) {
            onError(callback.state);
          }
          break;
        case _Connection.CLOSED:
          if (onClosed != null) {
            onClosed(callback.state);
          }
          break;
      }
    });
  }

  ///
  /// 销毁
  /// 断开stomp连接，并且销毁一切资源，包括client，监听器，Rxjava Observer等
  /// 停止service
  ///
  Future<bool> destroy() async {
    bool b = await _channel.invokeMethod(_NativeMethod.DESTROY);
    await _closedStreamControllers();
    return b;
  }

  ///
  /// 订阅p2p通道
  /// [urls] 要订阅的点对点通道地址，可以是多个
  ///
  Future<bool> subscribP2P(List<String> urls) async {
    assert(urls != null);
    String urlStr = urls.join(",");
    return _channel.invokeMethod(_NativeMethod.SUBSCRIBER_P2P, urlStr);
  }

  ///
  /// 订阅广播通道
  /// [urls] 要订阅的广播通道地址，可以是多个
  ///
  Future<bool> subscribBroadcast(List<String> urls) async {
    assert(urls != null);
    String urlStr = urls.join(",");
    return await _channel.invokeMethod(
        _NativeMethod.SUBSCRIBER_BROADCAST, urlStr);
  }

  ///
  /// 接受消息监听
  /// [onMessage] 点对点消息回到函数
  /// [onBroadCast] 广播消息回调函数
  ///
  Future<bool> onMessageCallback(OnMessageCallback onMessage,
      {OnMessageCallback onBroadCast}) async {
    ///监听消息流
    _messageController.stream.listen((message) {
      ///根据具体的消息类型回调flutter
      switch (message.type) {
        case _MessageType.P2P: //点对点消息
          onMessage(message.message);
          break;
        case _MessageType.BROADCAST: //广播消息
          if (onBroadCast != null) {
            onBroadCast(message.message);
          }
          break;
        default:
          break;
      }
    });

    ///调用native方法注册消息callback
    return _channel.invokeMethod(_NativeMethod.MESSAGE_CALLBACK);
  }

  ///
  /// 发送消息
  /// [message] 消息体，一般为json
  /// [header] stomp消息头,默认可不传
  ///
  Future<String> sendMessage(String message, {Map<String, dynamic> header}) {
    ///將stomp头的value 转换为String类型，
    Map<String, String> headMap = new Map();
    if (header != null) {
      headMap = header.map((String key, value) {
        return new MapEntry(key, value.toString());
      });
    }
    Map<String, dynamic> params = {"msg": message, "header": headMap};
    return _channel.invokeMethod(_NativeMethod.SEND_MESSAGE, params);
  }

  ///
  /// 发送消息监听
  /// [callback] 发送消息回调函数，参数为消息体
  ///
  Future<bool> onSendCallback(OnSendMessageCallback callback) async {
    _sendController.stream.listen((message) {
      if (message.status == 1) {
        callback(SendStatus.SUCCESS, message.message);
      } else {
        callback(SendStatus.FAIL, message.message);
      }
    });
    return _channel.invokeMethod(_NativeMethod.SEND_CALLBACK);
  }

  ///
  /// native调用flutter的方法处理
  ///
  Future<dynamic> _nativeHandle(MethodCall call) async {
    String method = call.method;

    switch (method) {
      case _NativeMethod.ON_SEND:

        ///发送消息回调
        Map<String, dynamic> params = Map.from(call.arguments);
        _sendController
            .add(_OnSendMessageData(params["status"], params["msg"]));
        break;
      case _NativeMethod.ON_MESSAGE:

        ///收到新消息
        _messageController
            .add(new _OnMessageData(_MessageType.P2P, call.arguments));
        break;
      case _NativeMethod.ON_BROAD_CAST:

        ///接到到新广播消息回调
        _messageController
            .add(new _OnMessageData(_MessageType.BROADCAST, call.arguments));
        break;
      case _NativeMethod.ON_CONNECTION_OPENED:

        ///连接打开回调
        _connectionController
            .add(_OnConnectionData(_Connection.OPEN, call.arguments));
        break;
      case _NativeMethod.ON_CONNECTION_ERROR:

        ///连接错误回调
        _connectionController
            .add(_OnConnectionData(_Connection.ERROR, call.arguments));
        break;
      case _NativeMethod.ON_CONNECTION_CLOSED:

        ///连接断开回调
        _connectionController
            .add(_OnConnectionData(_Connection.CLOSED, call.arguments));
        break;
    }
    return Future.value("");
  }

  ///
  ///关闭streamcontroller 对象
  ///
  Future _closedStreamControllers() async {
    if (_connectionController != null) {
      _connectionController.close();
      _connectionController = null;
    }
    if (_messageController != null) {
      _messageController.close();
      _messageController = null;
    }
    if (_sendController != null) {
      _sendController.close();
      _sendController = null;
    }
  }
}

///
/// 连接方式
///
enum Schema { WS, HTTP }

///
/// 连接回调方法
///
enum _Connection { OPEN, ERROR, CLOSED }

///
/// 接受的消息类型
///
enum _MessageType { P2P, BROADCAST }

///
/// 消息发送给状态，成功1 失败0
///
enum SendStatus { FAIL, SUCCESS }

///
/// 连接回调数据
///
class _OnConnectionData {
  _Connection call;
  dynamic state;

  _OnConnectionData(this.call, this.state);
}

///
/// 消息回调数据
///
class _OnMessageData {
  _MessageType type;
  dynamic message;

  _OnMessageData(this.type, this.message);
}

///
/// 发送消息回调数据
///
class _OnSendMessageData {
  int status;
  dynamic message;

  _OnSendMessageData(this.status, this.message);
}

///
/// 定义调用的方法,包括原生和flutter
///
class _NativeMethod {
  ///flutter调用native
  static const String INIT = "init";
  static const String CONNECTION = "connection";
  static const String SUBSCRIBER_P2P = "subscriberP2P";
  static const String SUBSCRIBER_BROADCAST = "subscriberBroadcast";
  static const String MESSAGE_CALLBACK = "setMessageCallback";
  static const String SEND_CALLBACK = "setSendCallback";
  static const String DESTROY = "destroy";
  static const String SEND_MESSAGE = "sendMessage";

  ///native 反调flutter
  static const String ON_CONNECTION_OPENED = "onConnectionOpen";
  static const String ON_CONNECTION_ERROR = "onConnectionError";
  static const String ON_CONNECTION_CLOSED = "onConnectionClosed";

  static const String ON_MESSAGE = "onMessage";
  static const String ON_BROAD_CAST = "onBroadcastMessage";
  static const String ON_SEND = "onSend";
}
