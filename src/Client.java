
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

public class Client {

    private Download download = new Download();
    
    DataOutputStream toServer = null;
    DataInputStream fromServer = null;
    ArrayList<String> fileList = new ArrayList<>();

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
            try {
                downloadButtonAction(e);
            } catch (InterruptedException zz) {
                System.out.println(zz);
                zz.printStackTrace();
            }
        });

        String threadNum[] = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" };
        threadComboBox = new JComboBox(threadNum);
        threadComboBox.setFont(THSarabunFont);
        threadComboBox.setSelectedIndex(0);
        threadComboBox.setSize(55, 55);
        threadComboBox.setFocusable(false);

        try {

            Socket socket = new Socket(hostName, port);

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

    void downloadButtonAction(ActionEvent e) throws InterruptedException {
        String fileNameSelected = fileComboBox.getSelectedItem().toString();
        String fileNameExtension = "."+fileNameSelected.substring(fileNameSelected.lastIndexOf(".")+1);

        downloadThread = Integer.parseInt(threadComboBox.getSelectedItem().toString());
//        System.out.println(downloadThread);

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
                
//                System.out.println(fileContentLength);
                long starttime = System.currentTimeMillis();
//                long starttime2 = System.currentTimeMillis();
                
                String filePath = fd.getDirectory()+fd.getFile()+
                        ((fd.getFile().lastIndexOf(fileNameExtension)==fd.getFile().length()-fileNameExtension.length())
                        ? "" : fileNameExtension);
                download = new Download();
                if (fileContentLength > 0) {
                    for (int i = 0; i < downloadThread; i++) {
                        new Thread(() -> {
                            try {
                                Socket socket = new Socket("localhost", downloadPort);
                                DataInputStream fromDServer = new DataInputStream(socket.getInputStream());
                                InputStream bufferedInputStream = new BufferedInputStream(fromDServer);
//                                Download partial = new Download();
                                int start = fromDServer.readInt();
                                int end = fromDServer.readInt();
                                
                                RandomAccessFile raf = new RandomAccessFile(filePath, "rwd");  //read write synchronized
                                raf.seek(start);
                                
                                byte[] buffer = new byte[end];
                                int read = 0;
                                
                                while((read = bufferedInputStream.read(buffer, 0, buffer.length)) > -1) {
                                    raf.write(buffer, 0, read);
                                    synchronized(Download.class) {
                                        download.percent = download.percent+read;
                                    }
                                }
                                
                                raf.close();
                                bufferedInputStream.close();
                                
//                                System.out.println(Thread.currentThread().getName() + " finished read");

//                                latch.countDown();
                                socket.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }, "Thread-" + i).start();
                    }
                    DownloadProgress downloadProgress = new DownloadProgress(fileNameSelected);
                    Thread process = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (!download.success) {
                                try {
                                    Thread.sleep(250); //Interval 0.5 seconds to calculate
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                if (download.percent >= fileContentLength) {
                                    download.success = true;
                                    downloadProgress.percent.setText("100%");
                                    downloadProgress.progressBar.setValue(100);
                                    downloadProgress.frame.setVisible(false);
//                                    System.out.println("Download completed: 100%");
                                } 
                                else {
//                                    System.out.println("Download : "+((int) (float) download.percent / (float) fileContentLength * 100) + "%");
                                    downloadProgress.percent.setText((int)((float)download.percent/(float)fileContentLength * 100)+"%");
                                    downloadProgress.progressBar.setValue((int)((float)download.percent/(float)fileContentLength * 100));
                                }
                            }
                            OpenFile openf = new OpenFile(fd.getDirectory(), fd.getDirectory() + fd.getFile());
                            openf.setVisible(true);
                        }
                        
                    });
                    process.start();
                    
//                    latch.await();
                    
//                    long finish = System.currentTimeMillis();
//                    long timeElapsed = finish - starttime;
//                    System.out.println("all read Timer : " + timeElapsed);

//                    long finish2 = System.currentTimeMillis();
//                    long timeElapsed2 = finish2 - starttime2;
//                    System.out.println("file writed read Timer : " + timeElapsed2);

                    toServer.writeUTF("Download file successful");
                }

            } else
                toServer.writeUTF("Cancel request file download");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    class Download {
        public int percent = 0;
        public boolean success = false;
    }
    
    class DownloadProgress {
    private JFrame frame;
    private JPanel panel;
//    private JLabel loading;
    private JLabel percent;
    private JProgressBar progressBar;
    public DownloadProgress(String fileName) {
        frame = new JFrame("Download File : "+fileName);
        frame.setResizable(false);
        panel = new JPanel();
//        loading = new JLabel();
        percent = new JLabel();
        progressBar = new JProgressBar();
        
        panel.setBackground(new Color(0, 153, 204));
        
        progressBar.setForeground(new java.awt.Color(255, 51, 255));
        progressBar.setLocation(10, 10);
        progressBar.setSize(280, 40);

//        loading.setFont(new Font("Dialog", 3, 18)); // NOI18N
//        loading.setForeground(new java.awt.Color(255, 0, 51));
//        loading.setText("Loading...");

        percent.setFont(new Font("Angsana New", Font.PLAIN, 24)); // NOI18N
        percent.setText("0%");
        percent.setLocation(140,3);
        percent.setSize(50,50);

        panel.setLayout(null);
//        panel.add(loading);
        panel.add(percent);
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