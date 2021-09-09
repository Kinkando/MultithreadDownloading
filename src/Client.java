import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
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

    public Client() {
        this.fileList = new ArrayList<>();
    }

    private void start() {

        frame = new JFrame("Client");
        frame.setSize(700, 105);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        label = new JLabel("Folder from server port " + port);
        label.setFont(THSarabunFont);
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setVerticalAlignment(JLabel.CENTER);

        tagLabel = new JLabel("File : ");
        tagLabel.setFont(THSarabunFont);

        downloadButton = new JButton("Download");
        downloadButton.setFocusable(false);
        // downloadButton.addActionListener(this::downloadButtonAction);
        downloadButton.addActionListener(e -> {
//            try {
                downloadButtonAction(e);
//            } catch (InterruptedException zz) {
//                System.out.println(zz);
//                zz.printStackTrace();
//            }
        });

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
            // frame.getContentPane().add(fileComboBox, BorderLayout.CENTER);
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
//                int latchGroupCount = downloadThread;
//                CountDownLatch latch = new CountDownLatch(latchGroupCount);
                int fileContentLength = fromServer.readInt();
                long starttime = System.currentTimeMillis();
//                long starttime2 = System.currentTimeMillis();
                String filePath = fd.getDirectory() + fd.getFile()
                        + ((fd.getFile().lastIndexOf(fileNameExtension) == fd.getFile().length() - fileNameExtension.length())
                        ? "" : fileNameExtension);
                download = new Download();
                if (fileContentLength > 0) {
                    DownloadProgress downloadProgress = new DownloadProgress(fileNameSelected);
                    new Thread(() -> {
                        while (!download.success) {
                            try {
                                Thread.sleep(10); //Interval 0.01 seconds to calculate (500 = 0.5 seconds)
                                if (download.percent >= fileContentLength) {
                                    download.success = true;
                                    downloadProgress.progressBar.setValue(100);
                                    Thread.sleep(2000);
                                    downloadProgress.frame.setVisible(false);
//                                    System.out.println("Download completed: 100%");
                                } else {
//                                    System.out.println("Download : "+((int) (float) download.percent / (float) fileContentLength * 100) + "%");
                                    downloadProgress.progressBar.setValue((int) ((float) download.percent / (float) fileContentLength * 100));
                                }
                            } catch (InterruptedException ex) {
                                
                            }

                        }
                        OpenFile openf = new OpenFile(fd.getDirectory(), fd.getDirectory() + fd.getFile(), fd.getFile());
                        openf.setVisible(true);
                    }).start();
                    for (int i = 0; i < downloadThread; i++) {
                        new Thread(() -> {
                            try {
                                Socket socket = new Socket("localhost", downloadPort);
                                DataInputStream fromDServer = new DataInputStream(socket.getInputStream());
                                InputStream bufferedInputStream = new BufferedInputStream(fromDServer);
                                int start = fromDServer.readInt();
                                int end = fromDServer.readInt();

                                RandomAccessFile raf = new RandomAccessFile(filePath, "rwd");  //read write synchronized
                                raf.seek(start);

                                byte[] buffer = new byte[end];
                                int read = 0;

                                while ((read = bufferedInputStream.read(buffer, 0, buffer.length)) > -1) {
                                    raf.write(buffer, 0, read);
                                    synchronized (Download.class) {
                                        download.percent = download.percent + read;
                                    }
                                }
//                                System.out.println(Thread.currentThread().getName() + " finished read");
//                                latch.countDown();
                                raf.close();
                                bufferedInputStream.close();
                                fromDServer.close();
                                socket.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }

                        }, "Thread-" + i).start();
                    }
//                    latch.await();
//                    long finish = System.currentTimeMillis();
//                    long timeElapsed = finish - starttime;
//                    System.out.println("all read Timer : " + timeElapsed);
//                    long finish2 = System.currentTimeMillis();
//                    long timeElapsed2 = finish2 - starttime2;
//                    System.out.println("file writed read Timer : " + timeElapsed2);
                    toServer.writeUTF("Download file successful");
                }
            } 
            else {
                toServer.writeUTF("Cancel request file download");
            }
        } catch (IOException ex) {
            try {
//                ex.printStackTrace();
                socket.close();
            } catch (IOException ex1) {
                
            }
        }
    }

    class Download {

        public int percent = 0;
        public boolean success = false;
    }

    class DownloadProgress {

        private JFrame frame;
        private JPanel panel;
        private JProgressBar progressBar;

        public DownloadProgress(String fileName) {
            frame = new JFrame("Download File : " + fileName);
            frame.setResizable(false);
            panel = new JPanel();
            progressBar = new JProgressBar();

            panel.setBackground(new Color(0, 153, 204));

            progressBar.setForeground(new java.awt.Color(255, 51, 255));
            progressBar.setLocation(10, 10);
            progressBar.setSize(280, 40);
            progressBar.setStringPainted(true);

            panel.setLayout(null);
            panel.add(progressBar);

            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.getContentPane().add(panel);
            frame.setSize(310, 90);
            frame.setVisible(true);
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }
}