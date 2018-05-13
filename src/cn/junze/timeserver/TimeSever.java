package cn.junze.timeserver;

/**
 * Created by Administrator on 2018/5/13 0013.
 */
public class TimeSever {
    public static void main(String[] args) {
        int port = 18080;

        if(args != null && args.length > 0){

            port = Integer.valueOf(args[0]);
        }
        //启动服务端
        MultiplexerTimeServer multiplexerTimeServer = new MultiplexerTimeServer(port);
        new Thread(multiplexerTimeServer,"server01").start();

    }
}
