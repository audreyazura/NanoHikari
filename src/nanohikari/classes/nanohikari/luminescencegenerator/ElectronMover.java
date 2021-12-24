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
package nanohikari.luminescencegenerator;

import com.github.kilianB.pcg.fast.PcgRSFast;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Alban Lafuente
 */
public class ElectronMover implements Runnable
{
    private final BigDecimal m_sampleXSize;
    private final BigDecimal m_sampleYSize;
    private final BigDecimal m_timeStep;
    private final BigDecimal m_vth;
    private final HashMap<BigInteger, Set<QuantumDot>> m_QDMap;
    private final List<Electron> m_electronList;
    private final PcgRSFast m_randomGenerator;
    
    public ElectronMover (BigDecimal p_sampleXMax, BigDecimal p_sampleYMax, BigDecimal p_timeStep, BigDecimal p_vth, List<Electron> p_electronToTreat, HashMap<BigInteger, Set<QuantumDot>> p_map)
    {
        m_sampleXSize = p_sampleXMax;
        m_sampleYSize = p_sampleYMax;
        m_timeStep = p_timeStep;
        m_vth = p_vth;
        m_electronList = new ArrayList(p_electronToTreat);
        m_randomGenerator = new PcgRSFast();
        
        m_QDMap = p_map;
    }
    
    public boolean allRecombined()
    {
        boolean finished = true;
        
        for (Electron currentElectron: m_electronList)
        {
            finished &= currentElectron.isRecombined();
        }
        
        return finished;
    }
    
    public ArrayList<Electron> getElectronList()
    {
        return new ArrayList(m_electronList);
    }
    
    @Override
    public void run()
    {
        for (Electron curentElectron: m_electronList)
        {
            curentElectron.move(m_timeStep, m_sampleXSize, m_sampleYSize, m_vth, m_QDMap, m_randomGenerator);
        }
    }
    
    public void addElectron(Electron p_newElectron)
    {
        m_electronList.add(p_newElectron);
    }
}
