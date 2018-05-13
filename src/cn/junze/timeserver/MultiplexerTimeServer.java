package cn.junze.timeserver;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Administrator on 2018/5/13 0013.
 */
public class MultiplexerTimeServer implements Runnable {

    private Selector selector;

    private ServerSocketChannel serverSocketChannel;

    private volatile boolean stop;

    public MultiplexerTimeServer(int port) {
        try {
            //创建Selector
            selector = Selector.open();
            //创建ServerSocketChannel
            serverSocketChannel = ServerSocketChannel.open();
            //配置ServerSocketChannel为非阻塞模式
            serverSocketChannel.configureBlocking(false);
            //绑定端口号
            //argument is the requested maximum number of pending connections on the socket.
            //设置最大连接数
            serverSocketChannel.socket().bind(new InetSocketAddress(port), 1024);
            //向selector注册接收请求事件
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("服务的端口号：" + port);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    public void stop() {

        this.stop = stop;
    }

    @Override
    public void run() {

        while (!stop) {
            try {
                //设置超时时间，超时了重新获取事件
                selector.select(1000);
                //获取事件set
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();

                SelectionKey key = null;

                while (iterator.hasNext()) {

                    key = iterator.next();
                    //从iterator中删除这个key
                    iterator.remove();

                    try {
                        //处理请求事件
                        handleInput(key);

                    } catch (Exception e) {
                        //直接关闭
                        if (key != null) {
                            if (key.channel() != null) {
                                key.channel().close();
                            }
                            key.cancel();
                        }
                    }

                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleInput(SelectionKey key) throws IOException {
        //判断key是否合法
        if (key.isValid()) {

            //处理新请求
            if (key.isAcceptable()) {
                //从key取的ServerSocketChannel
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                //通过ServerSocketChannel接收客户端请求并创建SocketChannel
                //相当与完成TCP的三次握手，TCP的物理链路正式建立
                SocketChannel sc = ssc.accept();
                //配置非阻塞模式
                sc.configureBlocking(false);
                //向selector注册可读事件
                sc.register(selector, SelectionKey.OP_READ);


                byte[] bytes = "aa".getBytes("utf-8");

                ByteBuffer allocate = ByteBuffer.allocate(bytes.length);
                allocate.put(bytes);
                allocate.flip();
                sc.write(allocate);

                System.out.println("连接完成");
            }
            //处理可读请求事件
            if (key.isReadable()) {

                System.out.println("接收请求");
                //从key中获取SocketChannel
                SocketChannel sc = (SocketChannel) key.channel();
                //创建大小为1024字节的byteBuffer
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                //将客户端channel中的数据写入byteBuffer中，Query Time Order不会超过1024字节
                int read = sc.read(byteBuffer);
                System.out.println(read);
                if (read > 0) {
                    //转换为可读模式
                    byteBuffer.flip();

                    byte[] bytes = new byte[byteBuffer.remaining()];
                    //读取byteBuffer数据
                    byteBuffer.get(bytes);
                    //编码
                    String body = new String(bytes, "utf-8");

                    System.out.println("服务接收到：" + body);
                    //判断客户端请求如果是Query Time Order，获取当前是时间
                    String curTime = "Query Time Order".equals(body) ? LocalDateTime.now().toString() : "Bad";
                    System.out.println(curTime);
                    //向客户端响应当前时间
                    //这里的两次写入客户端有可能收到一次（合并一起的），也可能收到两次（两个分开的）
                    doWrite(sc, curTime);

                    doWrite(sc, "aaa");
                } else if (read < 0) {
                    //关闭端链路
                    key.cancel();
                    sc.close();
                }

            }
        }


    }

    //向客户端channel发送数据的方法
    private void doWrite(SocketChannel sc, String response) throws IOException {

        if (response != null && response.trim().length() > 0) {
            byte[] bytes = response.getBytes("utf-8");
            //通过ByteBuffer创建缓冲区
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            //将字节数组存入
            writeBuffer.put(bytes);
            //转换为可读模式
            writeBuffer.flip();
            //向客户端channel写writeBuffer中的数据
            sc.write(writeBuffer);
        }
    }
}
