package aqua.blatt1.client;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

public class ShowSnapshotController implements ActionListener {
    private final Component parent;
    private final int fishies;

    public ShowSnapshotController(Component parent, int fishies) {
        this.parent = parent;
        this.fishies = fishies;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(this.fishies == -1)
            JOptionPane.showMessageDialog(parent, "There is no GlobalSnapshot!");
        else
            JOptionPane.showMessageDialog(parent, "Snapshot = " + this.fishies);
    }
}
