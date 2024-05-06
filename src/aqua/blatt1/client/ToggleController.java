package aqua.blatt1.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ToggleController implements ActionListener {
    private TankModel tankModel;
    private String fishId;
    public ToggleController(TankModel tankModel, String fishId) {
        this.tankModel = tankModel;
        this.fishId = fishId;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        tankModel.locateFishGlobally(fishId);
    }
}
