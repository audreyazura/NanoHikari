/*
 * Copyright (C) 2021 Alban Lafuente
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

import albanlafuente.physicstools.physics.PhysicsVariables;
import nanohikari.luminescencegenerator.Electron;
import nanohikari.luminescencegenerator.ImageBuffer;
import nanohikari.luminescencegenerator.QuantumDot;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 *
 * @author Alban Lafuente
 */
public class ResultInterpretator implements ImageBuffer
{
    private final GUIUpdater m_gui;
    
    public ResultInterpretator (GUIUpdater p_gui)
    {
        m_gui = p_gui;
    }
    
    /**
     * Format the data to send them to the GUI
     * @param p_electrons the list of electrons
     * @param p_neededRecombinations the number of recombination needed to finish the simulation
     * @param p_qds the list of QDs
     * @param p_time the time passed in the simulation, in nanoseconds
     */
    @Override
    public void logObjects(List<Electron> p_electrons, int p_neededRecombinations, List<QuantumDot> p_qds, BigDecimal p_time)
    {
        int numberRecombinedElectron = 0;
        
        for (Electron electron: p_electrons)
        {
            if (electron.isRecombined())
            {
                numberRecombinedElectron += 1;
            }
        }
        
        BigDecimal timens = (p_time.divide(PhysicsVariables.UnitsPrefix.NANO.getMultiplier(), MathContext.DECIMAL128)).setScale(3, RoundingMode.HALF_UP);
        String timeUnit = timens.toPlainString() + " ns";
        String recombinedRatio = numberRecombinedElectron + "/" + p_neededRecombinations;
        
        m_gui.updateProgress((double) numberRecombinedElectron / p_neededRecombinations, timeUnit, recombinedRatio);
    }
}
