import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

public class Server {
    private File[] fileList;  
    private int clientNo = 0; 
    private final String folder = "C:/Users/User/Downloads/Document/Operating Systems/Server Folder";  
    private final int port = 3300;            
    private final int downloadPort = 3301;  
    private LocalDateTime date;
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();      
    private final Font THSarabunFont = new Font("TH Sarabun New", Font.PLAIN, 26);

    private JFrame frame;
    private JTable fileTable;
    private JTextArea logField;
    private JScrollPane logScrollPane, fileScrollPane;
    private JTabbedPane tab;

    private void start() {
        frame = new JFrame("Server Port " + port);  
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); 
        logField = new JTextArea();
        logField.setEditable(false);    
        logScrollPane = new JScrollPane(logField); 
        logScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);   
        logScrollPane.setBorder(null); 

        fileList = new File(folder).listFiles();        //สมมติที่อ่านมาเป็น Folder ไม่ใช่ File จะทำไง  
        DecimalFormat sizeFormat = new DecimalFormat("#,###");   
        DecimalFormat sizeFormatPoint = new DecimalFormat("#,###.##"); 
        String[][] fileRow = new String[fileList.length][2];           
        String[] fileSizeFormat = { " bytes", " KB", " MB", " GB" };   

        for (int i = 0; i < fileList.length; i++) {   
            fileRow[i][0] = fileList[i].getName();     
            int j = 0;                                
            long fileSize = fileList[i].length();     
            for (j = 0; j < 3 && fileSize >= 1024; j++)
                fileSize /= 1024.0;
            fileRow[i][1] = (j > 1 ? sizeFormatPoint.format(fileSize) : sizeFormat.format(fileSize))  
                    + fileSizeFormat[j];
        }

        new Thread(() -> {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(port);    

                date = LocalDateTime.now();
                logField.setFont(THSarabunFont); 
                logField.append(date.format(dateFormat) + " Server started time"); 
                logField.setEditable(false);          
                
                ServerSocket uploadServer = new ServerSocket(downloadPort);   
                while (true) {
                    Socket socket = serverSocket.accept();      
                    date = LocalDateTime.now();
                    logField.append("\n" + date.format(dateFormat) + " Starting Client " + (clientNo + 1)
                            + " IP Address is " + socket.getInetAddress().getHostAddress());
                    new Thread(new ThreadClient(socket, clientNo++, uploadServer)).start();   
                }
            } catch (IOException ex) {
//                ex.printStackTrace();
            }
        }).start();
        String column[] = { "Name", "Size" };      
        fileTable = new JTable(fileRow, column) {
            @Override
            public boolean isCellEditable(int row, int column) {    
                return false; 
            }
        };
        fileTable.setFont(THSarabunFont);
        fileTable.setShowGrid(false);                  
        setJTableColumnsWidth(fileTable, 480, 85, 15); 
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        fileTable.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);    
        fileTable.setRowHeight(25);      
        fileTable.setFocusable(false);

        fileScrollPane = new JScrollPane(fileTable);    
        fileScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        fileScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        fileScrollPane.setBorder(null);

        tab = new JTabbedPane();
        tab.setFont(THSarabunFont);
        tab.add("Log", logScrollPane);
        tab.add("File", fileScrollPane);
        tab.setFocusable(false);

        frame.getContentPane().add(tab);      
        frame.setBounds(screenSize.width / 2 - 400, screenSize.height / 2 - 250, 800, 500);
        frame.setMinimumSize(new Dimension(600, 300)); 
        frame.setVisible(true); 
    }

    class MultiThreadUpload implements Runnable {
        private final Socket socket;
        private final int index;
        private final long start;
        private final long size;

        MultiThreadUpload(Socket socket, int index, long start, long end) {
            this.socket = socket;
            this.index = index;
            this.start = start;
            this.size = end;
        }

        @Override
        public void run() {
            try {
                //FileInputStream  -> Convert file to byte stream for read byte data from file
                //FileOutputStream -> Convert byte stream to file for write byte data to file
                //BufferedStream   -> Read/Write a lot of byte data at once time and keep byte data in buffer before send
                
                InputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(fileList[index].getAbsolutePath())); 
                DataOutputStream outputToClient = new DataOutputStream(socket.getOutputStream());
                
                outputToClient.writeLong(start); 

                byte[] buffer = new byte[1024 * 1024];   
                int read;                                 
                int count = 0;                              
                long allRound = (long) Math.ceil(((double)size/buffer.length));       
                
                bufferedInputStream.skip(start);            
                while ((read = bufferedInputStream.read(buffer)) != -1 && count<allRound) {  
                    outputToClient.write(buffer, 0, read);  
                    count++;                               
                }
                bufferedInputStream.close();  
                outputToClient.close();
                socket.close();

            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException e1) {
                    // e1.printStackTrace();
                }
                // e.printStackTrace();
            }
        }
    }

    class ThreadClient implements Runnable {
        private final Socket socket;
        private final int no;
        private int uploadThread;
        private ServerSocket uploadServer = null;

        public ThreadClient(Socket socket, int no, ServerSocket uploadServer) {
            this.socket = socket;
            this.no = no;
            this.uploadServer = uploadServer;
        }

        @Override
        public void run() {
            try {
                DataInputStream inputFromClient = new DataInputStream(socket.getInputStream());  
                DataOutputStream outputToClient = new DataOutputStream(socket.getOutputStream()); 

                outputToClient.writeInt(fileList.length); 

                PrintWriter writer = new PrintWriter(outputToClient, true); 
                for(File f : fileList)
                    writer.println(f.getName());
                writer.close();

                while (true) {
                    try {
                        date = LocalDateTime.now();

                        String fileName = inputFromClient.readUTF();
                        if (fileName.contains("\"successful\"")) {
                            logField.append("\n" + date.format(dateFormat) + " Client " + (no + 1));
                            logField.append(" Download status : " + fileName.substring(fileName.lastIndexOf(" ")+1));
                            logField.append("\n" + date.format(dateFormat) + " Client " + (no + 1));
                            logField.append(" Take time to download : " + inputFromClient.readUTF()+" seconds ("+uploadThread+" thread"+(uploadThread==1?")":"s)"));
                        }
                        else {
                            boolean found = false;
                            for (int i = 0; i < fileList.length; i++) {
                                if (fileList[i].getName().equalsIgnoreCase(fileName)) {
                                    found = fileList[i].isFile();
                                    outputToClient.writeBoolean(found);
                                    if(found) {
                                        uploadThread = inputFromClient.readInt();
                                        outputToClient.writeLong(fileList[i].length());
                                        long fileLength = (fileList[i].length() / uploadThread);
                                        for (int j = 0; j < uploadThread; j++) {
                                            Socket uploadSocket = uploadServer.accept();
                                            new Thread(new MultiThreadUpload(uploadSocket, i, j * fileLength,
                                                    j == uploadThread - 1
                                                            ? fileList[i].length() - (j * fileLength)     
                                                            : fileLength)).start();
                                        }  
                                    }
                                    break;
                                }
                            }
                            if (found) {
                                logField.append("\n" + date.format(dateFormat) + " Client " + (no + 1));
                                logField.append(" Request to download file : " + fileName);
                            }
                        }
                    } catch (IOException ex) {
                        date = LocalDateTime.now();
                        logField.append("\n" + date.format(dateFormat) + " Client " + (no + 1) + " is disconnected");
                        inputFromClient.close();
                        outputToClient.close();
                        socket.close();
                        break;
                    }
                }
            } catch (IOException ex) {
//                ex.printStackTrace();
            }
        }
    }

    public static void setJTableColumnsWidth(JTable table, int tablePreferredWidth, double... percentages) {
        double total = 0;
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++)
            total += percentages[i];
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setPreferredWidth((int) (tablePreferredWidth * (percentages[i] / total)));
        }
    }

    public static void main(String[] args) {
        try {             
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {         }
        Server server = new Server();
        server.start();
    }
}