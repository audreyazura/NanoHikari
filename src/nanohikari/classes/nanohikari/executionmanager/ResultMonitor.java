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

import nanohikari.luminescencegenerator.Electron;
import nanohikari.luminescencegenerator.GeneratorManager;
import albanlafuente.physicstools.physics.PhysicsVariables;
import com.sun.jdi.AbsentInformationException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Alban Lafuente
 */
public class ResultMonitor implements Runnable
{
    private final boolean m_convertEnergy;
    private final ExecutionManager m_manager;
    private GeneratorManager m_simulator;
    private Thread m_monitoredThread;
    
    public ResultMonitor ()
    {
        m_convertEnergy = false;
        m_manager = null;
        m_simulator = null;
        m_monitoredThread = null;
    }
    
    public ResultMonitor (boolean p_convertEnergy, ExecutionManager p_manager, GeneratorManager p_simulator, Thread p_toMonitor)
    {
        m_convertEnergy = p_convertEnergy;
        m_manager = p_manager;
        m_simulator = p_simulator;
        m_monitoredThread = p_toMonitor;
    }
    
    @Override
    public void run()
    {
        if (m_simulator != null && m_monitoredThread != null)
        {
            try
            {
                m_monitoredThread.join();
            }
            catch (InterruptedException ex)
            {
                Logger.getLogger(ResultMonitor.class.getName()).log(Level.SEVERE, null, ex);
            }

            HashSet<Electron> results = m_simulator.getFinalElectronList();
            if (results.size() > 0)
            {
                List<BigDecimal> recombinationTimes = new ArrayList<>();
                List<BigDecimal> recombinationEnergy = new ArrayList<>();

                for (Electron el: results)
                {
                    try
                    {
                        if (m_convertEnergy)
                        {
                            BigDecimal wavelength = PhysicsVariables.h.multiply(PhysicsVariables.c).divide(el.getRecombinationEnergy(), MathContext.DECIMAL128);
                            recombinationEnergy.add(new BigDecimal(wavelength.toString()));
                        }
                        else
                        {
                            BigDecimal energy = el.getRecombinationEnergy();
                            recombinationEnergy.add(new BigDecimal(energy.toString()));
                        }

                        recombinationTimes.add(new BigDecimal(el.getRecombinationTime().toString()));
                    }
                    catch (AbsentInformationException ex)
                    {
                        Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                m_manager.computeResults(recombinationEnergy, recombinationTimes);
            }
        }
    }
    
    public void initializeTrackedGenerator (GeneratorManager p_simulator, Thread p_toMonitor)
    {
        m_simulator = p_simulator;
        m_monitoredThread = p_toMonitor;
    }
}
