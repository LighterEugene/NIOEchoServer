import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class EchoServer {//
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private volatile boolean stop;

    EchoServer() throws IOException {
        selector = Selector.open();// открываем селектор
        serverSocketChannel = ServerSocketChannel.open(); // открываем канал
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(40000),1024);// привязываем сокет-сервер к порту
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT); //Регистрируем этот канал  назначенным селектором и возвращаем ключ.
        System.out.println("Сервер прослушивает порт 40000");

        while(!stop){
            selector.select(1000); // Выбираем набор ключей, чьи отвечающие каналы готовы к I/O операциями.
            Set<SelectionKey> selectedKeys = selector.selectedKeys(); // Возвращает сет ключей от селектора
            Iterator<SelectionKey> it = selectedKeys.iterator();
            SelectionKey key = null;
            while(it.hasNext()){
                key = it.next();
                it.remove();;
                handler(key);
            }
        }
    }

    private void handler(SelectionKey key) throws IOException {// обработчик
        if(key.isValid()){// если ключ тот, что надо.
            if(key.isAcceptable()){
                ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                SocketChannel socketChannel = serverSocketChannel.accept();
                socketChannel.configureBlocking(false);
                socketChannel.register(selector,SelectionKey.OP_READ);
            }
            if(key.isReadable()){
                SocketChannel socketChannel = (SocketChannel) key.channel();
                ByteBuffer readBytes = ByteBuffer.allocate(1024);
                int readCount = socketChannel.read(readBytes);
                if(readCount > 0){
                    readBytes.flip();
                    byte[] bytes = new byte[readBytes.remaining()];
                    readBytes.get(bytes);
                    String receiveMsg = new String(bytes,"UTF-8");
                    if("bye".equals(receiveMsg)){
                        stop();
                        return;
                    }
                    String responseString ="Сервер получил:" + receiveMsg;
                    System.out.println(responseString);
                    responseString = "Сервер вернул " + receiveMsg;
                    ByteBuffer responseBuffer = ByteBuffer.allocate(responseString.getBytes().length);// резервируем место
                    responseBuffer.put(responseString.getBytes());// перегоняем строку в набор байтов
                    responseBuffer.flip(); // Метод flip переключает режим буфера с режима записи на режим чтения. Он также устанавливает позицию обратно в 0 и устанавливает предел, в котором позиция была во время записи.
                    socketChannel.write(responseBuffer);
                }
            }//
        }
    }

    public void stop(){
        this.stop = true;
    }

    public static void main(String[] args) {
        try {
            new EchoServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

