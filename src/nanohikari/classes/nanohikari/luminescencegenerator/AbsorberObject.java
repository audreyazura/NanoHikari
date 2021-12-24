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

import java.math.BigDecimal;
import org.nevec.rjm.BigDecimalMath;

/**
 *
 * @author Alban Lafuente
 */
public class AbsorberObject
{
    BigDecimal m_positionX;
    BigDecimal m_positionY;
    
    public BigDecimal getDistance (BigDecimal p_positionX, BigDecimal p_positionY)
    {
        BigDecimal squaredDistance = ((m_positionX.subtract(p_positionX)).pow(2)).add(((m_positionY.subtract(p_positionY)).pow(2)));
        BigDecimal distance;
        
        if (squaredDistance.compareTo(BigDecimal.ZERO) == 0)
        {
            distance = BigDecimal.ZERO;
        }
        else
        {
            distance = BigDecimalMath.sqrt(squaredDistance);
        }
        
        return distance;
    }
    
    public BigDecimal getX()
    {
        return m_positionX;
    }
    
    public BigDecimal getY()
    {
        return m_positionY;
    }
}
