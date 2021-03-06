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
package plugins.kernel.roi.roi2d;

import icy.canvas.IcyCanvas;
import icy.painter.Anchor2D;
import icy.resource.ResourceUtil;
import icy.type.point.Point5D;
import icy.util.XMLUtil;

import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.w3c.dom.Node;

/**
 * ROI 2D Point class.<br>
 * Define a single point ROI<br>
 * 
 * @author Stephane
 */
public class ROI2DPoint extends ROI2DShape
{
    public class ROI2DPointPainter extends ROI2DShapePainter
    {
        @Override
        protected boolean isSmall(Rectangle2D bounds, Graphics2D g, IcyCanvas canvas)
        {
            if (isSelected())
                return false;

            return super.isSmall(bounds, g, canvas);
        }

        @Override
        protected boolean isTiny(Rectangle2D bounds, Graphics2D g, IcyCanvas canvas)
        {
            if (isSelected())
                return false;

            return super.isTiny(bounds, g, canvas);
        }
    }

    public static final String ID_POSITION = "position";

    private final Anchor2D position;

    /**
     * @deprecated
     */
    @Deprecated
    public ROI2DPoint(Point2D pt, boolean cm)
    {
        this(pt);
    }

    public ROI2DPoint(Point2D position)
    {
        super(new Line2D.Double());

        this.position = createAnchor(position);

        // add to the control point list
        controlPoints.add(this.position);

        this.position.addOverlayListener(this);
        this.position.addPositionListener(this);

        // select the point for "interactive" mode
        this.position.setSelected(true);
        // getOverlay().setMousePos(new Point5D.Double(position.getX(), position.getY(), -1d, -1d,
        // -1d));

        updateShape();

        // set name and icon
        setName("Point2D");
        setIcon(ResourceUtil.ICON_ROI_POINT);
    }

    /**
     * Generic constructor for interactive mode
     */
    public ROI2DPoint(Point5D pt)
    {
        this(pt.toPoint2D());
        // getOverlay().setMousePos(pt);
    }

    public ROI2DPoint(double x, double y)
    {
        this(new Point2D.Double(x, y));
    }

    public ROI2DPoint()
    {
        this(new Point2D.Double());
    }

    @Override
    protected ROI2DShapePainter createPainter()
    {
        return new ROI2DPointPainter();
    }

    /**
     * @deprecated Use {@link #getLine()} instead.
     */
    @Deprecated
    public Rectangle2D getRectangle()
    {
        final Point2D pt = getPoint();
        return new Rectangle2D.Double(pt.getX(), pt.getY(), 0d, 0d);
    }

    public Line2D getLine()
    {
        return (Line2D) shape;
    }

    public Point2D getPoint()
    {
        return position.getPosition();
    }

    @Override
    protected void updateShape()
    {
        final Point2D pt = getPoint();
        final double x = pt.getX();
        final double y = pt.getY();

        getLine().setLine(x, y, x, y);

        // call super method after shape has been updated
        super.updateShape();
    }

    @Override
    public boolean canAddPoint()
    {
        // this ROI doesn't support point add
        return false;
    }

    @Override
    protected boolean removePoint(IcyCanvas canvas, Anchor2D pt)
    {
        // remove point on this ROI remove the ROI
        canvas.getSequence().removeROI(this);
        return true;
    }

    @Override
    public double computeNumberOfContourPoints()
    {
        return 0d;
    }

    @Override
    public double computeNumberOfPoints()
    {
        return 0d;
    }

    @Override
    public boolean loadFromXML(Node node)
    {
        beginUpdate();
        try
        {
            if (!super.loadFromXML(node))
                return false;

            position.loadPositionFromXML(XMLUtil.getElement(node, ID_POSITION));
        }
        finally
        {
            endUpdate();
        }

        return true;
    }

    @Override
    public boolean saveToXML(Node node)
    {
        if (!super.saveToXML(node))
            return false;

        position.savePositionToXML(XMLUtil.setElement(node, ID_POSITION));

        return true;
    }
}
