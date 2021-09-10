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
    private File[] fileList;    //keep file storage in array
    private int clientNo = 0;   //keep number of clients
    private final String folder = "C:/Users/User/Downloads/Document/Operating Systems/Server Folder";   //File path of folder
    private final int port = 3300;              //Port to connect server socket (Communication between server and client)
    private final int downloadPort = 3301;      //Port to connect upload file server socket (Send and receive file)
    private LocalDateTime date;
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();       //keep width and height of your PC screen size
    private final Font THSarabunFont = new Font("TH Sarabun New", Font.PLAIN, 26);

    private JFrame frame;
    private JTable fileTable;
    private JTextArea logField;
    private JScrollPane logScrollPane, fileScrollPane;
    private JTabbedPane tab;

    private void start() {
        frame = new JFrame("Server Port " + port);      //Create object of JFrame and Set Title
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);      //Define instruction of (X) symbol to close the program
        logField = new JTextArea();
        logField.setEditable(false);    //Can't type anything in log of server (text field)
        logScrollPane = new JScrollPane(logField);  //Scroll tab of logField
        logScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); //Scollbar width
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);     //Scollbar height
        logScrollPane.setBorder(null);      //No border of scrollbar

        fileList = new File(folder).listFiles();    //Push all files from server folder path to array of File
        DecimalFormat sizeFormat = new DecimalFormat("#,###");          //Only decimal number without decimal point use in kb and bytes size
        DecimalFormat sizeFormatPoint = new DecimalFormat("#,###.##");  //Decimal number with 2 decimal point use in GB and MB size
        String[][] fileRow = new String[fileList.length][2];            //Data in JTable
        String[] fileSizeFormat = { " bytes", " KB", " MB", " GB" };    //Size of file

        for (int i = 0; i < fileList.length; i++) {     //Loop in all files in server folder
            fileRow[i][0] = fileList[i].getName();      //Keep file name to array of fileRow in order to display with table (Column 0)
            int j = 0;                                  //Create to reference size of file with fileSizeFormat variable
            double fileSize = fileList[i].length();     //Keep length of file with bytes size in fileSize variable
            for (j = 0; j < 3 && fileSize >= 1024; j++)
                fileSize /= 1024.0;
            fileRow[i][1] = (j > 1 ? sizeFormatPoint.format(fileSize) : sizeFormat.format(fileSize))    //Keep length and size of file in array to display on table
                    + fileSizeFormat[j];
        }

        new Thread(() -> {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(port);      //Create Server with 3300 port for communation with Client

                date = LocalDateTime.now();                 //Keep time of server boot
                logField.setFont(THSarabunFont); 
                logField.append(date.format(dateFormat) + " Server started time");  //Print started server time
                logField.setEditable(false);            //Can't type anything on log server
                
                ServerSocket uploadServer = new ServerSocket(downloadPort);     //Create Server with 3301 port for upload-download file
                while (true) {
                    Socket socket = serverSocket.accept();      //Accept serverSocket when Client connect to server port 3300
                    date = LocalDateTime.now();
                    logField.append("\n" + date.format(dateFormat) + " Starting Client " + (clientNo + 1)
                            + " IP Address is " + socket.getInetAddress().getHostAddress());
                    new Thread(new ThreadClient(socket, clientNo++, uploadServer)).start();     //new Thread to handle with that Client Thread
                }
            } catch (IOException ex) {
//                ex.printStackTrace();
            }
        }).start();
        String column[] = { "Name", "Size" };       //Column name of folder server table
        fileTable = new JTable(fileRow, column) {
            @Override
            public boolean isCellEditable(int row, int column) {    
                return false;       //Can't change anything on file in table of server folder
            }
        };
        fileTable.setFont(THSarabunFont);
        fileTable.setShowGrid(false);                   //Set no cell border in vertical and horizontal line on table
        setJTableColumnsWidth(fileTable, 480, 85, 15);  //Set each column width (85% width of Filename column, 15% width of file length size)
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        fileTable.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);     //Set right alignment in file size column on table
        fileTable.setRowHeight(25);         //set height of each row in table
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

        frame.getContentPane().add(tab);        //Add tab bar to JFrame
        frame.setBounds(screenSize.width / 2 - 400, screenSize.height / 2 - 250, 800, 500); //set Location and Size
        frame.setMinimumSize(new Dimension(600, 300));  //Set minimum size of frame
        frame.setVisible(true); //display frame
    }

    class MultiThreadUpload implements Runnable {
        private final Socket socket;
        private final int index;
        private final int start;
        private final int end;

        MultiThreadUpload(Socket socket, int index, int start, int end) {
            this.socket = socket;
            this.index = index;
            this.start = start;
            this.end = end;
        }

        @Override
        public void run() {
            try {
                //FileInputStream  -> Convert file to byte stream for read byte data from file
                //FileOutputStream -> Convert byte stream to file for write byte data to file
                
                InputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(fileList[index].getAbsolutePath()));
                DataOutputStream outputToClient = new DataOutputStream(socket.getOutputStream());
                
                outputToClient.writeInt(start);
                outputToClient.writeInt(end);

                byte[] buffer = new byte[1024 * 1024];      //read byte data 1024 * 1024 KB each time
                int read;                                   //Last location or length of byte data that readed
                int count = 0;
                boolean check = false;
                bufferedInputStream.skip(start);
                while ((read = bufferedInputStream.read(buffer)) != -1) {
                    if (count + read > end) {
                        read = end - count;
                        count += read;
                        check = true;
                    }
                    outputToClient.write(buffer, 0, read);
                    if (check)
                        break;
                    count += read;
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

                while (true) {
                    try {
                        date = LocalDateTime.now();

                        String fileName = inputFromClient.readUTF();
                        if (fileName.contains("successful")) {
                            logField.append("\n" + date.format(dateFormat) + " Client " + (no + 1));
                            logField.append(" Download status : " + fileName.substring(fileName.lastIndexOf(" ")+1));
                            logField.append("\n" + date.format(dateFormat) + " Client " + (no + 1));
                            logField.append(" Take time to download : " + inputFromClient.readUTF()+" seconds ("+uploadThread+" threads)");
                        } else {
                            boolean found = false;
                            for (int i = 0; i < fileList.length; i++) {
                                if (fileList[i].getName().equalsIgnoreCase(fileName)) {
                                    uploadThread = inputFromClient.readInt();

                                    outputToClient.writeInt((int) fileList[i].length());

                                    int fileLength = (int) (fileList[i].length() / uploadThread);
                                    
                                    for (int j = 0; j < uploadThread; j++) {
                                        Socket uploadSocket = uploadServer.accept();
                                        new Thread(new MultiThreadUpload(uploadSocket, i, j * fileLength,
                                                j == uploadThread - 1
                                                        ? (int) fileList[i].length() - (j * fileLength)
                                                        : fileLength)).start();
                                    }
                                    found = true;
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
                        socket.close();
                        break;
                    }
                }
            } catch (IOException ex) {
//                ex.printStackTrace();
            }
        }
    }

    public static String getFileExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        return i > 0 ? fileName.substring(i + 1) : "No extension found";
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
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            
        } catch (InstantiationException ex) {
            
        } catch (IllegalAccessException ex) {
            
        } catch (UnsupportedLookAndFeelException ex) {
            
        }
        Server server = new Server();
        server.start();
    }
}