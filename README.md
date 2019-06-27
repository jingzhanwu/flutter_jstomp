# jstomp

pub地址：https://pub.dev/packages/jstomp

    之前项目上有使用到Stomp协议封装的websocket，端上使用订阅通道的形式，支持ws与http，支持订阅多个通道，
JStomp是我基于当前项目中的使用和总结开发的一个FLutter 插件，一般中小型的项目中有消息推送，IM等业务场景
增加进入，下面看JStomp提供的能力。
    
1、支持ws方式连接
        
2、支持http方式连接
        
3、支持连接时的自定义验证参数，如token等
        
4、支持同时订阅多个点对点通道
        
5、支持同时订阅多个广播通道
        
6、提供连接、消息、发送等回调监听
        
7、支持发送消息时的自定义消息头
        
8、JStomp为单例设计，避免多次初始化和订阅
        
9、可不手动断开连接，程序重新进入重新初始化处理，不会多次订阅
        
10、轻量级接入，使用简单
        
11、自动管理心跳，无需使用者自己发送心跳维持长连接
        
12、支持AndroidX
        
13、连接失败或者断开连接后默认重试15分钟，重试间隔10秒
        
        
    如何使用
    
        1、flutter项目的pubspec.yaml文件中引入：
        
        dependencies:
            jstomp: ^0.0.1
            
        2、初始化stomp
             JStomp stomp = JStomp.instance;
             
             //初始化连接的url地址
             String url="ws://192.168.3.25:9000/stompMsg.../...";
             
             //发送消息的url地址
             String sendUrl="sendMessage/android/...";
             
             //初始化stomp,成功返回true
             bool b =await stomp.init(url: url, sendUrl: sendUrl);
             
        3、打开连接
             if (b) {
                   await stomp.connection((open) {
                   
                     print("连接打开了...$open");
                     
                   }, onError: (error) {
                   
                     print("连接打开错误了...$error");
                     
                   }, onClosed: (closed) {
                   
                     print("连接打开错误了...$closed");
                     
                   });
             }
             
             参数open：是带有一个bool类型的回调函数，true代表连接正常打开了，false代表失败；
             参数error：是带有一个String类型参数的回调函数，代表连接出错了，error为错误信息；
             参数closed：是带有一个bool类型的回调函数，代表连接关闭，返回值为false；
             
        4、订阅消息通道，支持点对点和广播，支持同时订阅多个通道
            //点对点通道地址，我这里订阅指定userid的通道
             final String p2p = "/microGroupMessage/" + userId;
             
             //开始订阅
             await stomp.subscribP2P([p2p,"地址2..."]);
             
             //订阅广播通道
             await stomp.subscribBroadcast(["广播通道1...","广播通道2..."]);
             
        5、设置消息监听器，当有新消息到达时回调，可同时设置点对点和广播回调；回调返回的消息为一个json格式的String，
           可根据自己的需求对json字串解析。
           
           //添加消息监听器
          await stomp.onMessageCallback((message) {//点对点回调，必选参数
           
                   print("收到p2p新消息：" + message.toString());
                   
                 }, onBroadCast: (message) {     //广播回调，可选参数
                 
                   print("收到新广播消息：" + message.toString());
                   
                 });
           
           参数message：是一个json字串，代表本次接受到的消息内容。
                 
        6、设置发送消息回调监听器,当发送一条stomp消息时，不管此条消息发送成功还是失败，此回调都会将此条消息内容
           回调回来，除此之外还有发送状态.
           
           await stomp.onSendCallback((status, sendMsg) {
            
                 print("消息发送完毕：$status :msg=" + sendMsg.toString());
                 
               });
            
            参数status：是一个枚举类型，enum SendStatus { FAIL, SUCCESS }
            参数sendMsg：是一个json字串，代表本次发送的消息内容
            
        7、发送消息
            //使用map构造一个要发送的数据,以下数据是我项目上的消息数据格式，消息字段大家根据自己需求自定义
           Map<String, dynamic> msg = {
                 "content": "flutter发送的消息",
                 "createId": "161691756546",
                 "createName": "陈晨",
                 "createTime": "2019-06-24 17:03:51",
                 "id": "1046324312976343042",
                 "microGroupId": "1143049991384731649",
                 "microGroupName": "flutter专属群",
                 "type": 0
               };
           
               
           第一种：默认发送方法，直接传入消息内容。
           //开始发送消息,这一步一定记得要转成json类型的字串，负责将出现格式错误导致发送失败，
           //底层stomp只接受json格式的数据
           
           await stomp.sendMessage(json.encode(msg)); 
           
           第二种：自定义stomp消息头
           
           //定义自定义的stomp头，必须是Map类型，value只支持基本数据类型
               Map<String, dynamic> head = {
                 "userId": "p123456",
                 "token": "MgjkjkdIdkkDkkjkfdjfdkjfk",
               };
           
           //发送消息，将自定义头传入方法参数header
           await stomp.sendMessage(json.encode(msg), header: head);
           
        8、断开连接并销毁资源
            
           await stomp.destroy();




