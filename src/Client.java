import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

public class Client {

//    private Download download;
    private Socket socket;
    private DataOutputStream toServer = null;
    private DataInputStream fromServer = null;
    private final ArrayList<String> fileList;

    private final Font THSarabunFont = new Font("TH Sarabun New", Font.PLAIN, 26);
    private final String hostName = "localhost";
    private final int port = 3300;
    private final int downloadPort = 3301;
    private int downloadThread = 1;
    private long total;

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

            BufferedReader reader = new BufferedReader(new InputStreamReader(fromServer)); //Read character input stream only
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
            splitPane.setEnabled(false);
            frame.getContentPane().add(splitPane, BorderLayout.CENTER);
            frame.getContentPane().add(downloadButton, BorderLayout.EAST);
            frame.setVisible(true);
        } catch (IOException ex) {
            System.exit(0);
        }
    }

    void downloadButtonAction(ActionEvent e) {
        String fileNameSelected = fileComboBox.getSelectedItem().toString();        //file name in combo box that selected
        String fileNameExtension = "." + fileNameSelected.substring(fileNameSelected.lastIndexOf(".") + 1);  //file name extension
        downloadThread = Integer.parseInt(threadComboBox.getSelectedItem().toString());
        try {
            FileDialog fd = new FileDialog(frame, "Save File", FileDialog.SAVE);
            fd.setFile(fileNameSelected);
            fd.setVisible(true);
            if (fd.getFile() != null) {
                toServer.writeUTF(fileNameSelected);
                boolean haveFile = fromServer.readBoolean();
                if(haveFile) {
                    toServer.writeInt(downloadThread);
                    long fileContentLength = fromServer.readLong();
                    long starttime = System.currentTimeMillis();
                    String filePath = fd.getDirectory() + fd.getFile()
                            + ((fd.getFile().lastIndexOf(fileNameExtension) == fd.getFile().length() - fileNameExtension.length())
                            ? "" : fileNameExtension);
                    File f = new File(filePath);
//                    download = new Download();      //use for detect percentage of file downloading
                    DownloadFrame downloadFrame = new DownloadFrame(fd.getDirectory(), f.getAbsolutePath(), f.getName());
                    downloadButton.setEnabled(false);   //Can't press download button when file download unfinish
                    new Thread(() -> {
                        boolean success = false;
                        while (!success) {
                            try {
                                Thread.sleep(100); //Interval 0.1 seconds to calculate (1000 milliseconds = 1 seconds)
                                if (total >= fileContentLength) {
                                    success = true;
                                    downloadFrame.progressBar.setValue(100);
                                    downloadFrame.progressBar.setString("Download Successful");
                                    downloadFrame.setSize(downloadFrame.getWidth(), downloadFrame.getHeight()+40);

                                    long finish = System.currentTimeMillis();
                                    long timeElapsed = finish - starttime;
                                    toServer.writeUTF("\"successful\"");
                                    toServer.writeUTF(""+TimeUnit.MILLISECONDS.toSeconds(timeElapsed));
                                    downloadButton.setEnabled(true);
                                    total = 0;
                                } else {
                                    DecimalFormat decimalFormat = new DecimalFormat("0.00");
                                    double percentage = ((double)total / (double)fileContentLength) * 100;
                                    String withoutExponential = decimalFormat.format(percentage);
                                    downloadFrame.progressBar.setValue((int)Double.parseDouble(withoutExponential));
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
                                InputStream bufferedInputStream = new BufferedInputStream(fromDServer); //Write Data in buffer of Stream before
                                long start = fromDServer.readLong();

                                RandomAccessFile raf = new RandomAccessFile(filePath, "rwd");  //read write synchronized
                                raf.seek(start);

                                byte[] buffer = new byte[1024*1024];   
                                int read = 0;

                                while ((read = bufferedInputStream.read(buffer)) > -1) { 
                                    raf.write(buffer, 0, read);
                                    synchronized (Client.class) {
                                        executePercentage(read);
//                                        download.percent = download.percent + read;
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
                else {
                    JOptionPane.showMessageDialog(frame, "File \""+fileNameSelected+"\" not found", "Error", JOptionPane.ERROR_MESSAGE);
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
    
    private synchronized void executePercentage(int read) {
        total += read;
    }
    
    public static void main(String[] args) {
        try {             
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {         }
        Client client = new Client();
        client.start();
    }
}
