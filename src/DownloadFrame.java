import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

public class DownloadFrame extends JFrame {

    private final String fileName;
    private final String directory;
    private final String filePath;
    public JButton openFolderButton;
    public JButton openFileButton;
    public JProgressBar progressBar;
    public JLabel label;
    private final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
   
    public DownloadFrame(String directory, String filePath, String fileName) {
        this.directory = directory;
        this.filePath = filePath;
        this.fileName = fileName;
        initComponents();
    }
    
    private void initComponents() {
        setTitle("Download Manager");
        setResizable(false);
        openFolderButton = new JButton();
        label = new JLabel();
        openFileButton = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setBackground(new Color(73, 161, 248));

        openFolderButton.setText("Open folder");
        openFolderButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                OpenFolderButton(evt);
            }
        });
        openFolderButton.setFocusPainted(false);
        
        label.setFont(new Font("TH Sarabun New", Font.PLAIN, 24)); 
        label.setText(fileName);

        openFileButton.setText("Open file");
        openFileButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                OpenFileButton(evt);
            }
        });
        openFileButton.setFocusPainted(false);
        
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                    .addComponent(label, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 228, Short.MAX_VALUE)
                        .addComponent(openFolderButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(openFileButton))
                    .addComponent(progressBar, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(label, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(progressBar, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(openFileButton)
                    .addComponent(openFolderButton))
                .addContainerGap())
        );
        pack();
        setSize(getWidth(), getHeight()-40);
        setLocation(screenSize.width-600, screenSize.height/2 - 420);
        setVisible(true);
    }
    private void OpenFolderButton(MouseEvent evt) {
        if(openFolderButton.getText().equalsIgnoreCase("Open Folder")) {
            File folder = new File(directory);
            Desktop desktop = null;
            if (Desktop.isDesktopSupported()) { //Check Desktop class support on current platform
                desktop = Desktop.getDesktop(); 
            } //Return the Desktop instance of the current context to allow open file directory on file explorer
            try {
                desktop.open(folder);
            } 
            catch (IOException e) {
    //            System.err.println(e);
            }
            catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Folder not found", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    private void OpenFileButton(MouseEvent evt) {
        File folder = new File(filePath);
        Desktop desktop = null;
        if (Desktop.isDesktopSupported()) {
            desktop = Desktop.getDesktop();
        }
        try {
            desktop.open(folder);
        } 
        catch (IOException e) {
//            System.err.println(e);
        }
        catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "File \""+fileName+"\" not found", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
