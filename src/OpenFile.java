import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

public class OpenFile extends JFrame {

    private final String fileName;
    private final String directory;
    private final String filePath;
    
    private JButton jButton1;
    private JButton jButton2;
    private JLabel jLabel1;
   
    public OpenFile(String directory, String filePath, String fileName) {
        this.directory = directory;
        this.filePath = filePath;
        this.fileName = fileName;
        initComponents();
    }
   
    @SuppressWarnings("unchecked")
    private void initComponents() {
        setTitle(fileName);
        jButton1 = new JButton();
        jLabel1 = new JLabel();
        jButton2 = new JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setBackground(new Color(73, 161, 248));

        jButton1.setText("Open folder");
        jButton1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                jButton1MouseClicked(evt);
            }
        });
        jLabel1.setFont(new Font("Dialog", 1, 18)); 
        jLabel1.setText("Do you want open file location?");

        jButton2.setText("Open file");
        jButton2.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                jButton2MouseClicked(evt);
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton1)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton2, GroupLayout.PREFERRED_SIZE, 90, GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20))
            .addGroup(layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(jLabel1, GroupLayout.PREFERRED_SIZE, 309, GroupLayout.PREFERRED_SIZE)
                .addContainerGap(61, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(39, Short.MAX_VALUE)
                .addComponent(jLabel1, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addGap(16, 16, 16))
        );
        pack();
    }
    private void jButton1MouseClicked(java.awt.event.MouseEvent evt) {
        File folder = new File(directory); // path to the directory to be opened
        Desktop desktop = null;
        if (Desktop.isDesktopSupported()) {
            desktop = Desktop.getDesktop();
        }
        try {
            desktop.open(folder);
//            setVisible(false);
        } 
        catch (IOException e) { }
    }
    private void jButton2MouseClicked(java.awt.event.MouseEvent evt) {
        File folder = new File(filePath); // path to the directory to be opened
        Desktop desktop = null;
        if (Desktop.isDesktopSupported()) {
            desktop = Desktop.getDesktop();
        }
        try {
            desktop.open(folder);
//            setVisible(false);
        } 
        catch (IOException e) { }
    }
}
