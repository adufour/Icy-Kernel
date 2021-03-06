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
package icy.gui.component.button;

import icy.action.IcyAbstractAction;
import icy.resource.icon.IcyIcon;
import icy.util.StringUtil;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;

import org.pushingpixels.flamingo.api.common.JCommandToggleButton;

/**
 * @author Stephane
 */
public class IcyCommandToggleButton extends JCommandToggleButton
{
    /**
     * 
     */
    private static final long serialVersionUID = 6540972110297834178L;

    /**
     * internals
     */
    private boolean internalEnabled;
    private IcyAbstractAction action;
    private final PropertyChangeListener actionPropertyChangeListener;

    public IcyCommandToggleButton(String title, IcyIcon icon)
    {
        super(title, icon);

        action = null;
        internalEnabled = isEnabled();

        actionPropertyChangeListener = new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                if (StringUtil.equals("enabled", evt.getPropertyName()))
                    refreshEnabled();
            }
        };
    }

    /**
     * @deprecated Use {@link #IcyCommandToggleButton(String, IcyIcon)} instead.
     */
    @Deprecated
    public IcyCommandToggleButton(String title, String iconName)
    {
        this(title, new IcyIcon(iconName));
    }

    public IcyCommandToggleButton(IcyIcon icon)
    {
        this(null, icon);
    }

    public IcyCommandToggleButton(String title)
    {
        this(title, (IcyIcon) null);
    }

    public IcyCommandToggleButton(IcyAbstractAction action)
    {
        this(null, (IcyIcon) null);

        setAction(action);
    }

    /**
     * @deprecated User {@link #IcyCommandToggleButton(IcyAbstractAction)} instead.
     */
    @Deprecated
    public IcyCommandToggleButton(icy.common.IcyAbstractAction action)
    {
        this(null, (IcyIcon) null);

        setAction(action);
    }
    
    public IcyCommandToggleButton()
    {
        this(null, (IcyIcon) null);
    }

    /**
     * Return the icon as IcyIcon
     */
    public IcyIcon getIcyIcon()
    {
        final Icon icon = getIcon();

        if (icon instanceof IcyIcon)
            return (IcyIcon) icon;

        return null;
    }

    /**
     * @return the icon name
     */
    public String getIconName()
    {
        final IcyIcon icon = getIcyIcon();

        if (icon != null)
            return icon.getName();

        return null;
    }

    /**
     * @param iconName
     *        the icon name to set
     */
    public void setIconName(String iconName)
    {
        final IcyIcon icon = getIcyIcon();

        if (icon != null)
            icon.setName(iconName);
    }

    @Override
    public void setEnabled(boolean b)
    {
        internalEnabled = b;
        refreshEnabled();
    }

    protected void refreshEnabled()
    {
        super.setEnabled(internalEnabled && ((action == null) || action.isEnabled()));
    }

    /**
     * Sets the {@link IcyAbstractAction} attached to this button.
     */
    public void setAction(IcyAbstractAction value)
    {
        if (action != value)
        {
            // remove listener from previous action
            if (action != null)
            {
                removeActionListener(action);
                action.removePropertyChangeListener(actionPropertyChangeListener);
            }

            action = value;

            setText(action.getName());

            final IcyIcon icon = action.getIcon();

            if (icon != null)
                setIcon(new IcyIcon(icon));
            else
                setIcon(null);

            if (value != null)
            {
                // set tooltip
                setActionRichTooltip(action.getRichToolTip());

                // add listeners
                addActionListener(value);
                value.addPropertyChangeListener(actionPropertyChangeListener);
            }

            refreshEnabled();
        }
    }

    /**
     * Returns the {@link IcyAbstractAction} attached to this button.
     */
    public IcyAbstractAction getAction()
    {
        return action;
    }
}
