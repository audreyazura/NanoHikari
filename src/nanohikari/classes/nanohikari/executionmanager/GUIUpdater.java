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
package nanohikari.executionmanager;

import javafx.scene.image.Image;

/**
 *
 * @author audreyazura
 */
public interface GUIUpdater
{
    public void sendMessage (String p_message);
    
    public void setProgressTitle (String p_title);
    
    public void showPicture(Image p_picture, String p_title, String p_position);
    
    public void updateProgress (double p_progress, String p_time, String p_recombinedElectrons);
}
