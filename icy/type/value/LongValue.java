/*
 * Copyright 2010-2013 Institut Pasteur.
 * 
 * This file is part of Icy.
 * 
 * Icy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Icy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Icy. If not, see <http://www.gnu.org/licenses/>.
 */
package icy.type.value;

import icy.util.StringUtil;

/**
 * @author Stephane
 */
public class LongValue extends AbstractValue<Long>
{
    public LongValue(Long value)
    {
        super(value);
    }

    public LongValue(long value)
    {
        this(Long.valueOf(value));
    }

    @Override
    public Long getDefaultValue()
    {
        return Long.valueOf(0L);
    }

    @Override
    public int compareTo(Long l)
    {
        return value.compareTo(l);
    }

    @Override
    public boolean loadFromString(String s)
    {
        // empty string --> default value
        if (StringUtil.isEmpty(s))
        {
            value = getDefaultValue();
            return true;
        }

        try
        {
            value = Long.valueOf(Long.parseLong(s));
            return true;
        }
        catch (NumberFormatException E)
        {
            return false;
        }
    }
}
