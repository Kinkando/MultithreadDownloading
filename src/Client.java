import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

public class Client {

    private Download download;
    private Socket socket;
    private DataOutputStream toServer = null;
    private DataInputStream fromServer = null;
    private final ArrayList<String> fileList;

    private final Font THSarabunFont = new Font("TH Sarabun New", Font.PLAIN, 26);
    private final String hostName = "localhost";
    private final int port = 3300;
    private final int downloadPort = 3301;

    private int downloadThread = 1;

    private JFrame frame;
    private JLabel label, tagLabel;
    private JButton downloadButton;
    private JComboBox fileComboBox;
    private JComboBox threadComboBox;
    
    private final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    public Client() {
        this.fileList = new ArrayList<>();
    }

    private void start() {

        frame = new JFrame("Client");
        frame.setSize(700, 105);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocation(screenSize.width/2-frame.getWidth()/2, screenSize.height/2-400);

        label = new JLabel("Folder from server port " + port);
        label.setFont(THSarabunFont);
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setVerticalAlignment(JLabel.CENTER);

        tagLabel = new JLabel("File : ");
        tagLabel.setFont(THSarabunFont);

        downloadButton = new JButton("Download");
        downloadButton.setFocusable(false);
        downloadButton.addActionListener(e -> {downloadButtonAction(e); });

        String threadNum[] = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
        threadComboBox = new JComboBox(threadNum);
        threadComboBox.setFont(THSarabunFont);
        threadComboBox.setSelectedIndex(0);
        threadComboBox.setSize(55, 55);
        threadComboBox.setFocusable(false);

        try {

            socket = new Socket(hostName, port);
            fromServer = new DataInputStream(socket.getInputStream());
            toServer = new DataOutputStream(socket.getOutputStream());

            BufferedReader reader = new BufferedReader(new InputStreamReader(fromServer));

            int fileNo = fromServer.readInt();

            for (int i = 0; i < fileNo; i++)
                fileList.add(reader.readLine());
            fileComboBox = new JComboBox(fileList.toArray());
            fileComboBox.setFont(THSarabunFont);
            fileComboBox.setSelectedIndex(0);
            fileComboBox.setSize(150, 55);
            fileComboBox.setFocusable(false);
            frame.getContentPane().add(threadComboBox);
            frame.getContentPane().add(label, BorderLayout.NORTH);
            frame.getContentPane().add(tagLabel, BorderLayout.WEST);
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, fileComboBox, threadComboBox);
            frame.getContentPane().add(splitPane, BorderLayout.CENTER);
            frame.getContentPane().add(downloadButton, BorderLayout.EAST);
            frame.setVisible(true);
        } catch (IOException ex) {
            System.exit(0);
        }
    }

    void downloadButtonAction(ActionEvent e) {
        String fileNameSelected = fileComboBox.getSelectedItem().toString();
        String fileNameExtension = "." + fileNameSelected.substring(fileNameSelected.lastIndexOf(".") + 1);
        downloadThread = Integer.parseInt(threadComboBox.getSelectedItem().toString());
        try {
            FileDialog fd = new FileDialog(frame, "Save File", FileDialog.SAVE);
            fd.setFile(fileNameSelected);
            fd.setVisible(true);
            if (fd.getFile() != null) {
                toServer.writeUTF(fileNameSelected);
                toServer.writeInt(downloadThread);
                int fileContentLength = fromServer.readInt();
                long starttime = System.currentTimeMillis();
                String filePath = fd.getDirectory() + fd.getFile()
                        + ((fd.getFile().lastIndexOf(fileNameExtension) == fd.getFile().length() - fileNameExtension.length())
                        ? "" : fileNameExtension);
                download = new Download();
                if (fileContentLength > 0) {
                    DownloadFrame downloadFrame = new DownloadFrame(fd.getDirectory(), fd.getDirectory() + fd.getFile(), fd.getFile());
                    new Thread(() -> {
                        while (!download.success) {
                            try {
                                Thread.sleep(10); //Interval 0.01 seconds to calculate (500 = 0.5 seconds)
                                if (download.percent >= fileContentLength) {
                                    download.success = true;
                                    downloadFrame.progressBar.setValue(100);
                                    downloadFrame.progressBar.setString("Download Successful");
                                    downloadFrame.setTitle("Download file complete");
                                    downloadFrame.setSize(downloadFrame.getWidth(), downloadFrame.getHeight()+40);
                                    
                                    long finish = System.currentTimeMillis();
                                    long timeElapsed = finish - starttime;
                                    toServer.writeUTF("Download file successful");
                                    toServer.writeUTF(""+TimeUnit.MILLISECONDS.toSeconds(timeElapsed));
                                } else {
                                    downloadFrame.progressBar.setValue((int) ((float) download.percent / (float) fileContentLength * 100));
                                }
                            } catch (InterruptedException ex) {
                                
                            } catch (IOException ex) {
                                
                            }

                        }
                    }).start();
                    for (int i = 0; i < downloadThread; i++) {
                        new Thread(() -> {
                            try {
                                Socket downloadSocket = new Socket("localhost", downloadPort);
                                DataInputStream fromDServer = new DataInputStream(downloadSocket.getInputStream());
                                InputStream bufferedInputStream = new BufferedInputStream(fromDServer);
                                int start = fromDServer.readInt();
                                int end = fromDServer.readInt();

                                RandomAccessFile raf = new RandomAccessFile(filePath, "rwd");  //read write synchronized
                                raf.seek(start);

                                byte[] buffer = new byte[1024*1024];    //can be to 1024 but it slower than true size
                                int read = 0;

                                while ((read = bufferedInputStream.read(buffer)) > -1) {        //(buffer, 0, buffer.length)
                                    raf.write(buffer, 0, read);
                                    synchronized (Download.class) {
                                        download.percent = download.percent + read;
                                    }
                                }
                                raf.close();
                                bufferedInputStream.close();
                                fromDServer.close();
                                downloadSocket.close();
                            } catch (IOException e1) {
//                                e1.printStackTrace();
                            }

                        }, "Thread-" + i).start();
                    }
                }
            } 
            else {
                toServer.writeUTF("Cancel request file download");
            }
        } catch (IOException ex) {
            try {
//                ex.printStackTrace();
                socket.close();
                System.exit(0);
            } catch (IOException ex1) {
                
            }
        }
    }

    class Download {
        public int percent = 0;
        public boolean success = false;
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
        Client client = new Client();
        client.start();
    }
}
