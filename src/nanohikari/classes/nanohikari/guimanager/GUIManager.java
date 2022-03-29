/*
 * Copyright (C) 2020-2021 Alban Lafuente
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

import albanlafuente.physicstools.physics.PhysicsVariables;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Screen;
import nanohikari.executionmanager.ExecutionManager;
import nanohikari.executionmanager.GUIUpdater;
import net.opentsdb.tools.ArgP;

/**
 *
 * @author Alban Lafuente
 */
public class GUIManager extends Application implements GUIUpdater
{
    private BigDecimal m_sampleXSize;
    private BigDecimal m_sampleYSize;
    private double m_frameRate = 500; //represent the time between two key frames, in ms
    private ProgressWindowController m_progressWindow;
    
    @Override
    public void showPicture(Image p_picture, String p_title, String p_position)
    {
        ImageView pictureViewer = new ImageView(p_picture);
        
        Group pictureRoot = new Group(pictureViewer);
        Scene pictureScene = new Scene(pictureRoot);
        
        double shift = Screen.getPrimary().getBounds().getWidth()/4;
        if (p_position.equals("left"))
        {
            shift *= -1;
        }
        
        Stage pictureStage = new Stage();
        pictureStage.setTitle(p_title);
        pictureStage.setScene(pictureScene);
        pictureStage.show();
        pictureStage.setX(pictureStage.getX() + shift);
    }
    
    public static void main(String[] args)
    {
        final ArgP argParser = new ArgP();
        argParser.addOption("--lum", "File containing the luminescence data.");
        argParser.addOption("--QDs", "File containing the quantum dots size and position.");
        argParser.addOption("--help", "The command you just used.");
        
        //parsing the args to get the options passed to the program
        try
	{
	    args = argParser.parse(args);
	}
	catch (IllegalArgumentException e)
	{
	    System.err.println(e.getMessage());
	    System.err.print(argParser.usage());
	    System.exit(1);
	}
        
        if (argParser.has("--help"))
        {
            //just print help message, not continuing execution
            System.out.println(argParser.usage());
        }
        else
        {
            String[] arguments = new String[1];

            arguments[0] = "ressources/configuration/default.conf";
            launch(arguments);
        }
    }
    
    @Override
    public void start (Stage stage)
    {
        stage.setResizable(false);
        
        m_sampleXSize = (new BigDecimal(1)).multiply(PhysicsVariables.UnitsPrefix.MICRO.getMultiplier());
        m_sampleYSize = (new BigDecimal(1)).multiply(PhysicsVariables.UnitsPrefix.MICRO.getMultiplier());
        
        FXMLLoader progressWindowLoader = new FXMLLoader(GUIManager.class.getResource("FXMLProgressWindow.fxml"));
        try
        {
            stage.setScene(new Scene(progressWindowLoader.load()));
            m_progressWindow = progressWindowLoader.getController();
            stage.setTitle("AFMLuminescence - Simulation progress");
            
            m_progressWindow.initialize();
            Properties configuration = new Properties();
            configuration.load(new FileReader(new File(getParameters().getRaw().get(0))));
            (new Thread(new ExecutionManager(this, configuration, m_sampleXSize, m_sampleYSize))).start();
            
            stage.show();
            stage.sizeToScene();
        }
        catch (IOException ex)
        {
            Logger.getLogger(GUIManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void sendMessage (String p_message)
    {
        m_progressWindow.printMessage(p_message);
    }
    
    @Override
    public void setProgressTitle (String p_title)
    {
        m_progressWindow.updateProgressLabel(p_title);
    }
    
    @Override
    public void updateProgress (double p_progress, String p_time, String p_recombinedElectrons)
    {
        m_progressWindow.updateProgress(p_progress, p_time, p_recombinedElectrons);
    }
    
    @Override
    public void stopExecution()
    {
        Platform.exit();
    }
}
