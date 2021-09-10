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
        logScrollPane = new JScrollPane(logField);
        logScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        logScrollPane.setBounds(0, 60, screenSize.width, 250);
        logScrollPane.setBorder(null);

        fileList = new File(folder).listFiles();
        DecimalFormat sizeFormat = new DecimalFormat("#,###");
        DecimalFormat sizeFormatPoint = new DecimalFormat("#,###.##");
        String[][] fileRow = new String[fileList.length][2];
        String[] fileSizeFormat = { " bytes", " KB", " MB", " GB" };

        for (int i = 0; i < fileList.length; i++) {
            fileRow[i][0] = fileList[i].getName();
            int j = 0;
            double fileSize = fileList[i].length();
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
        fileTable.setShowHorizontalLines(false);
        fileTable.setShowVerticalLines(false);
        setJTableColumnsWidth(fileTable, 480, 85, 15);
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        fileTable.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);
        fileTable.setRowHeight(25);
        fileTable.setSurrendersFocusOnKeystroke(true);
        fileTable.setFocusable(false);
        fileTable.setRowSelectionAllowed(true);

        fileScrollPane = new JScrollPane(fileTable);
        fileScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        fileScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        fileScrollPane.setBounds(0, 60, screenSize.width, 250);
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
        private Socket socket;
        private int index;
        private int start;
        private int end;

        MultiThreadUpload(Socket socket, int index, int start, int end) {
            this.socket = socket;
            this.index = index;
            this.start = start;
            this.end = end;
        }

        @Override
        public void run() {
            try {
                InputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(fileList[index].getAbsolutePath()));
                DataOutputStream outputToClient = new DataOutputStream(socket.getOutputStream());
                
                outputToClient.writeInt(start);
                outputToClient.writeInt(end);

                byte[] buffer = new byte[1024 * 1024];
                int read;
                int count = 0;
                boolean check = false;
                bufferedInputStream.skip(start);
                while ((read = bufferedInputStream.read(buffer, 0, buffer.length)) != -1) {
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