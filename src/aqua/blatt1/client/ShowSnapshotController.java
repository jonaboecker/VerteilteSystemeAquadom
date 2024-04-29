package aqua.blatt1.client;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

public class ShowSnapshotController implements ActionListener {
    private final Component parent;
    private final TankModel tankModel;

    public ShowSnapshotController(Component parent, TankModel fishies) {
        this.parent = parent;
        this.tankModel = fishies;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(this.tankModel.globalSnapshot == -1)
            JOptionPane.showMessageDialog(parent, "There is no GlobalSnapshot!");
        else
            JOptionPane.showMessageDialog(parent, "Snapshot = " + this.tankModel.globalSnapshot);
    }
}
