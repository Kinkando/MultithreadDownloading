import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

public class OpenFile extends JFrame {

    private final String fileName;
    private final String directory;
    private final String filePath;
//    private JProgressBar progressBar;
    private JButton openFolderButton;
    private JButton openFileButton;
    private JLabel label;
   
    public OpenFile(String directory, String filePath, String fileName) {
        this.directory = directory;
        this.filePath = filePath;
        this.fileName = fileName;
        initComponents();
    }
   
    @SuppressWarnings("unchecked")
    private void initComponents() {
        setTitle(fileName);
        openFolderButton = new JButton();
        label = new JLabel();
        openFileButton = new JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setBackground(new Color(73, 161, 248));

        openFolderButton.setText("Open folder");
        openFolderButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                OpenFolderButton(evt);
            }
        });
        label.setFont(new Font("Dialog", 1, 18)); 
        label.setText("Do you want open file location?");

        openFileButton.setText("Open file");
        openFileButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                OpenFileButton(evt);
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(openFolderButton)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(openFileButton, GroupLayout.PREFERRED_SIZE, 90, GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20))
            .addGroup(layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(label, GroupLayout.PREFERRED_SIZE, 309, GroupLayout.PREFERRED_SIZE)
                .addContainerGap(61, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(39, Short.MAX_VALUE)
                .addComponent(label, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(openFolderButton)
                    .addComponent(openFileButton))
                .addGap(16, 16, 16))
        );
        pack();
    }
    private void OpenFolderButton(MouseEvent evt) {
        File folder = new File(directory);
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
            JOptionPane.showMessageDialog(this, "Folder not found", "Error", JOptionPane.ERROR_MESSAGE);
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
//    public static void main(String[] args) {
//        new OpenFile("a","b","c").setVisible(true);
//    }
}
