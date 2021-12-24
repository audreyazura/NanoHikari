/*
 * Copyright (C) 2021 audreyazura
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nanohikari.guimanager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 *
 * @author audreyazura
 */
public class ProgressWindowController
{
    @FXML private Label progressTitle;
    @FXML private Label timeLabel;
    @FXML private Label electronTrackerLabel;
    @FXML private ProgressBar mainPbar;
    @FXML private TextArea consoleWindow;
    @FXML private VBox pbarBox;
    
    void initialize()
    {
        mainPbar.prefWidthProperty().bind(pbarBox.widthProperty());
    }
    
    public void updateProgressLabel(String p_title)
    {
        Platform.runLater(() -> progressTitle.setText(p_title));
    }
    
    public void updateProgress(double p_progress, String p_passedTime, String p_recombinedElectrons)
    {
        Platform.runLater(() -> 
        {
            timeLabel.setText("Simulated time: " + p_passedTime);
            electronTrackerLabel.setText("Recombined electrons: " + p_recombinedElectrons);
        });
        mainPbar.setProgress(p_progress);
    }
    
    public void printMessage(String p_message)
    {
        Platform.runLater(() -> consoleWindow.appendText(p_message + "\n"));
    }
}
