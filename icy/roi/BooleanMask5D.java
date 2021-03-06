/**
 * 
 */
package icy.roi;

import icy.type.collection.array.DynamicArray;
import icy.type.point.Point5D;
import icy.type.rectangle.Rectangle3D;
import icy.type.rectangle.Rectangle4D;
import icy.type.rectangle.Rectangle5D;

import java.awt.Rectangle;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Class to define a 5D boolean mask and make basic boolean operation between masks.<br>
 * The bounds property of this object define the area of the mask where the mask contains the
 * boolean mask itself.
 * 
 * @author Stephane
 */
public class BooleanMask5D
{
    // Internal use only
    private static BooleanMask4D doUnion4D(BooleanMask4D m1, BooleanMask4D m2)
    {
        if (m1 == null)
        {
            // only use the 3D mask from second mask
            if (m2 != null)
                return (BooleanMask4D) m2.clone();

            return null;
        }
        else if (m2 == null)
            // only use the 3D mask from first mask
            return (BooleanMask4D) m1.clone();

        // process union of 3D mask
        return BooleanMask4D.getUnion(m1, m2);
    }

    // Internal use only
    private static BooleanMask4D doIntersection4D(BooleanMask4D m1, BooleanMask4D m2)
    {
        if ((m1 == null) || (m2 == null))
            return null;

        // process intersection of 3D mask
        return BooleanMask4D.getIntersection(m1, m2);
    }

    // Internal use only
    private static BooleanMask4D doExclusiveUnion4D(BooleanMask4D m1, BooleanMask4D m2)
    {
        if (m1 == null)
        {
            // only use the 3D mask from second mask
            if (m2 != null)
                return (BooleanMask4D) m2.clone();

            return null;
        }
        else if (m2 == null)
            // only use the 3D mask from first mask
            return (BooleanMask4D) m1.clone();

        // process exclusive union of 3D mask
        return BooleanMask4D.getExclusiveUnion(m1, m2);
    }

    // Internal use only
    private static BooleanMask4D doSubtraction4D(BooleanMask4D m1, BooleanMask4D m2)
    {
        if (m1 == null)
            return null;
        // only use the 3D mask from first mask
        if (m2 == null)
            return (BooleanMask4D) m1.clone();

        // process subtraction of 3D mask
        return BooleanMask4D.getSubtraction(m1, m2);
    }

    /**
     * Build resulting mask from union of the mask1 and mask2:
     * 
     * <pre>
     *        mask1          +       mask2        =      result
     *
     *     ################     ################     ################
     *     ##############         ##############     ################
     *     ############             ############     ################
     *     ##########                 ##########     ################
     *     ########                     ########     ################
     *     ######                         ######     ######    ######
     *     ####                             ####     ####        ####
     *     ##                                 ##     ##            ##
     * </pre>
     */
    public static BooleanMask5D getUnion(BooleanMask5D mask1, BooleanMask5D mask2)
    {
        final Rectangle5D.Integer bounds = (Rectangle5D.Integer) mask1.bounds.createUnion(mask2.bounds);

        if (!bounds.isEmpty())
        {
            final BooleanMask4D[] mask;

            // special case of infinite C dimension
            if (bounds.sizeC == Integer.MAX_VALUE)
            {
                // we can allow merge ROI only if they both has infinite C dimension
                if ((mask1.bounds.sizeC != Integer.MAX_VALUE) || (mask2.bounds.sizeC != Integer.MAX_VALUE))
                    throw new UnsupportedOperationException(
                            "Cannot merge an infinite C dimension ROI with  a finite Z dimension ROI");

                mask = new BooleanMask4D[1];

                final BooleanMask4D m2d1 = mask1.mask.firstEntry().getValue();
                final BooleanMask4D m2d2 = mask2.mask.firstEntry().getValue();

                mask[0] = doUnion4D(m2d1, m2d2);
            }
            else
            {
                mask = new BooleanMask4D[bounds.sizeT];

                for (int c = 0; c < bounds.sizeC; c++)
                {
                    final BooleanMask4D m2d1 = mask1.getMask4D(c + bounds.c);
                    final BooleanMask4D m2d2 = mask2.getMask4D(c + bounds.c);

                    mask[c] = doUnion4D(m2d1, m2d2);
                }
            }

            return new BooleanMask5D(bounds, mask);
        }

        return new BooleanMask5D();
    }

    /**
     * Build resulting mask from intersection of the mask1 and mask2:
     * 
     * <pre>
     *        mask1     intersect     mask2      =        result
     *
     *     ################     ################     ################
     *     ##############         ##############       ############
     *     ############             ############         ########
     *     ##########                 ##########           ####
     *     ########                     ########
     *     ######                         ######
     *     ####                             ####
     *     ##                                 ##
     * </pre>
     */
    public static BooleanMask5D getIntersection(BooleanMask5D mask1, BooleanMask5D mask2)
    {
        final Rectangle5D.Integer bounds = (Rectangle5D.Integer) mask1.bounds.createIntersection(mask2.bounds);

        if (!bounds.isEmpty())
        {
            final BooleanMask4D[] mask;

            // special case of infinite C dimension
            if (bounds.sizeC == Integer.MAX_VALUE)
            {
                // we can allow merge ROI only if they both has infinite C dimension
                if ((mask1.bounds.sizeC != Integer.MAX_VALUE) || (mask2.bounds.sizeC != Integer.MAX_VALUE))
                    throw new UnsupportedOperationException(
                            "Cannot merge an infinite C dimension ROI with  a finite Z dimension ROI");

                mask = new BooleanMask4D[1];

                final BooleanMask4D m2d1 = mask1.mask.firstEntry().getValue();
                final BooleanMask4D m2d2 = mask2.mask.firstEntry().getValue();

                mask[0] = doIntersection4D(m2d1, m2d2);
            }
            else
            {
                mask = new BooleanMask4D[bounds.sizeT];

                for (int c = 0; c < bounds.sizeC; c++)
                {
                    final BooleanMask4D m2d1 = mask1.getMask4D(c + bounds.c);
                    final BooleanMask4D m2d2 = mask2.getMask4D(c + bounds.c);

                    mask[c] = doIntersection4D(m2d1, m2d2);
                }
            }

            return new BooleanMask5D(bounds, mask);
        }

        return new BooleanMask5D();
    }

    /**
     * Build resulting mask from exclusive union of the mask1 and mask2:
     * 
     * <pre>
     *          mask1       xor      mask2        =       result
     *
     *     ################     ################
     *     ##############         ##############     ##            ##
     *     ############             ############     ####        ####
     *     ##########                 ##########     ######    ######
     *     ########                     ########     ################
     *     ######                         ######     ######    ######
     *     ####                             ####     ####        ####
     *     ##                                 ##     ##            ##
     * </pre>
     */
    public static BooleanMask5D getExclusiveUnion(BooleanMask5D mask1, BooleanMask5D mask2)
    {
        final Rectangle5D.Integer bounds = (Rectangle5D.Integer) mask1.bounds.createUnion(mask2.bounds);

        if (!bounds.isEmpty())
        {
            final BooleanMask4D[] mask;

            // special case of infinite C dimension
            if (bounds.sizeC == Integer.MAX_VALUE)
            {
                // we can allow merge ROI only if they both has infinite C dimension
                if ((mask1.bounds.sizeC != Integer.MAX_VALUE) || (mask2.bounds.sizeC != Integer.MAX_VALUE))
                    throw new UnsupportedOperationException(
                            "Cannot merge an infinite C dimension ROI with  a finite Z dimension ROI");

                mask = new BooleanMask4D[1];

                final BooleanMask4D m2d1 = mask1.mask.firstEntry().getValue();
                final BooleanMask4D m2d2 = mask2.mask.firstEntry().getValue();

                mask[0] = doExclusiveUnion4D(m2d1, m2d2);
            }
            else
            {
                mask = new BooleanMask4D[bounds.sizeT];

                for (int c = 0; c < bounds.sizeC; c++)
                {
                    final BooleanMask4D m2d1 = mask1.getMask4D(c + bounds.c);
                    final BooleanMask4D m2d2 = mask2.getMask4D(c + bounds.c);

                    mask[c] = doExclusiveUnion4D(m2d1, m2d2);
                }
            }

            return new BooleanMask5D(bounds, mask);
        }

        return new BooleanMask5D();
    }

    /**
     * Build resulting mask from the subtraction of mask2 from mask1:
     * 
     * <pre>
     *        mask1          -        mask2       =  result
     *
     *     ################     ################
     *     ##############         ##############     ##
     *     ############             ############     ####
     *     ##########                 ##########     ######
     *     ########                     ########     ########
     *     ######                         ######     ######
     *     ####                             ####     ####
     *     ##                                 ##     ##
     * </pre>
     */
    public static BooleanMask5D getSubtraction(BooleanMask5D mask1, BooleanMask5D mask2)
    {
        final Rectangle5D.Integer bounds = (Rectangle5D.Integer) mask1.bounds.createIntersection(mask2.bounds);

        // need to subtract something ?
        if (!bounds.isEmpty())
        {
            final BooleanMask4D[] mask;

            // special case of infinite C dimension
            if (bounds.sizeC == Integer.MAX_VALUE)
            {
                // we can allow merge ROI only if they both has infinite C dimension
                if ((mask1.bounds.sizeC != Integer.MAX_VALUE) || (mask2.bounds.sizeC != Integer.MAX_VALUE))
                    throw new UnsupportedOperationException(
                            "Cannot merge an infinite C dimension ROI with  a finite Z dimension ROI");

                mask = new BooleanMask4D[1];

                final BooleanMask4D m2d1 = mask1.mask.firstEntry().getValue();
                final BooleanMask4D m2d2 = mask2.mask.firstEntry().getValue();

                mask[0] = doSubtraction4D(m2d1, m2d2);
            }
            else
            {
                mask = new BooleanMask4D[bounds.sizeC];

                for (int c = 0; c < bounds.sizeC; c++)
                {
                    final BooleanMask4D m2d1 = mask1.getMask4D(c + bounds.c);
                    final BooleanMask4D m2d2 = mask2.getMask4D(c + bounds.c);

                    mask[c] = doSubtraction4D(m2d1, m2d2);
                }
            }

            return new BooleanMask5D(bounds, mask);
        }

        return new BooleanMask5D();
    }

    /**
     * Region represented by the mask.
     */
    public Rectangle5D.Integer bounds;
    /**
     * Boolean mask 4D array.
     */
    public final TreeMap<Integer, BooleanMask4D> mask;

    /**
     * Build a new 4D boolean mask with specified bounds and 4D mask array.<br>
     * The 4D mask array length should be >= to <code>bounds.getSizeT()</code>.
     */
    public BooleanMask5D(Rectangle5D.Integer bounds, BooleanMask4D[] mask)
    {
        super();

        this.bounds = bounds;
        this.mask = new TreeMap<Integer, BooleanMask4D>();

        // special case of infinite C dim
        if (bounds.sizeC == Integer.MAX_VALUE)
            this.mask.put(Integer.valueOf(Integer.MIN_VALUE), mask[0]);
        else
        {
            for (int c = 0; c < bounds.sizeC; c++)
                if (mask[c] != null)
                    this.mask.put(Integer.valueOf(bounds.c + c), mask[c]);
        }
    }

    /**
     * Build a new 4D boolean mask from the specified array of {@link Point5D}.<br>
     */
    public BooleanMask5D(Point5D.Integer[] points)
    {
        super();

        mask = new TreeMap<Integer, BooleanMask4D>();

        if ((points == null) || (points.length == 0))
            bounds = new Rectangle5D.Integer();
        else
        {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int minT = Integer.MAX_VALUE;
            int minC = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            int maxT = Integer.MIN_VALUE;
            int maxC = Integer.MIN_VALUE;

            for (Point5D.Integer pt : points)
            {
                final int x = pt.x;
                final int y = pt.y;
                final int z = pt.z;
                final int t = pt.t;
                final int c = pt.c;

                if (x < minX)
                    minX = x;
                if (x > maxX)
                    maxX = x;
                if (y < minY)
                    minY = y;
                if (y > maxY)
                    maxY = y;
                if (z < minZ)
                    minZ = z;
                if (z > maxZ)
                    maxZ = z;
                if (t < minT)
                    minT = t;
                if (t > maxT)
                    maxT = t;
                if (c < minC)
                    minC = c;
                if (c > maxC)
                    maxC = c;
            }

            // define bounds
            bounds = new Rectangle5D.Integer(minX, minY, minZ, minT, minC, (maxX - minX) + 1, (maxY - minY) + 1,
                    (maxZ - minZ) + 1, (maxT - minT) + 1, (maxC - minC) + 1);

            // set mask
            for (Point5D.Integer pt : points)
            {
                BooleanMask4D m4d = mask.get(Integer.valueOf(pt.c));

                // allocate 4D boolean mask if needed
                if (m4d == null)
                {
                    m4d = new BooleanMask4D(new Rectangle4D.Integer(minX, minY, minZ, minT, bounds.sizeX, bounds.sizeY,
                            bounds.sizeZ, bounds.sizeT), new BooleanMask3D[bounds.sizeT]);
                    // set 4D mask for position C
                    mask.put(Integer.valueOf(pt.c), m4d);
                }

                BooleanMask3D m3d = m4d.getMask3D(pt.t);

                // allocate 3D boolean mask if needed
                if (m3d == null)
                {
                    m3d = new BooleanMask3D(new Rectangle3D.Integer(minX, minY, minZ, bounds.sizeX, bounds.sizeY,
                            bounds.sizeZ), new BooleanMask2D[bounds.sizeZ]);
                    // set 3D mask for position T
                    m4d.mask.put(Integer.valueOf(pt.t), m3d);
                }

                BooleanMask2D m2d = m3d.getMask2D(pt.z);

                // allocate 2D boolean mask if needed
                if (m2d == null)
                {
                    m2d = new BooleanMask2D(new Rectangle(minX, minY, bounds.sizeX, bounds.sizeY),
                            new boolean[bounds.sizeX * bounds.sizeY]);
                    // set 2D mask for position Z
                    m3d.mask.put(Integer.valueOf(pt.z), m2d);
                }

                // set mask point
                m2d.mask[((pt.y - minY) * bounds.sizeX) + (pt.x - minX)] = true;
            }

            // optimize mask 4D bounds
            for (BooleanMask4D m : mask.values())
                m.optimizeBounds();
        }
    }

    /**
     * Build a new boolean mask from the specified array of {@link Point5D}.<br>
     */
    public BooleanMask5D(Point5D[] points)
    {
        super();

        mask = new TreeMap<Integer, BooleanMask4D>();

        if ((points == null) || (points.length == 0))
            bounds = new Rectangle5D.Integer();
        else
        {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int minT = Integer.MAX_VALUE;
            int minC = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            int maxT = Integer.MIN_VALUE;
            int maxC = Integer.MIN_VALUE;

            for (Point5D pt : points)
            {
                final int x = (int) pt.getX();
                final int y = (int) pt.getY();
                final int z = (int) pt.getZ();
                final int t = (int) pt.getT();
                final int c = (int) pt.getC();

                if (x < minX)
                    minX = x;
                if (x > maxX)
                    maxX = x;
                if (y < minY)
                    minY = y;
                if (y > maxY)
                    maxY = y;
                if (z < minZ)
                    minZ = z;
                if (z > maxZ)
                    maxZ = z;
                if (t < minT)
                    minT = t;
                if (t > maxT)
                    maxT = t;
                if (c < minC)
                    minC = c;
                if (c > maxC)
                    maxC = c;
            }

            // define bounds
            bounds = new Rectangle5D.Integer(minX, minY, minZ, minT, minC, (maxX - minX) + 1, (maxY - minY) + 1,
                    (maxZ - minZ) + 1, (maxT - minT) + 1, (maxC - minC) + 1);

            // set mask
            for (Point5D pt : points)
            {
                BooleanMask4D m4d = mask.get(Integer.valueOf((int) pt.getC()));

                // allocate 4D boolean mask if needed
                if (m4d == null)
                {
                    m4d = new BooleanMask4D(new Rectangle4D.Integer(minX, minY, minZ, minT, bounds.sizeX, bounds.sizeY,
                            bounds.sizeZ, bounds.sizeT), new BooleanMask3D[bounds.sizeT]);
                    // set 4D mask for position C
                    mask.put(Integer.valueOf((int) pt.getC()), m4d);
                }

                BooleanMask3D m3d = m4d.getMask3D((int) pt.getT());

                // allocate 3D boolean mask if needed
                if (m3d == null)
                {
                    m3d = new BooleanMask3D(new Rectangle3D.Integer(minX, minY, minZ, bounds.sizeX, bounds.sizeY,
                            bounds.sizeZ), new BooleanMask2D[bounds.sizeZ]);
                    // set 3D mask for position T
                    m4d.mask.put(Integer.valueOf((int) pt.getT()), m3d);
                }

                BooleanMask2D m2d = m3d.getMask2D((int) pt.getZ());

                // allocate 2D boolean mask if needed
                if (m2d == null)
                {
                    m2d = new BooleanMask2D(new Rectangle(minX, minY, bounds.sizeX, bounds.sizeY),
                            new boolean[bounds.sizeX * bounds.sizeY]);
                    // set 2D mask for position Z
                    m3d.mask.put(Integer.valueOf((int) pt.getZ()), m2d);
                }

                // set mask point
                m2d.mask[(((int) pt.getY() - minY) * bounds.sizeX) + ((int) pt.getX() - minX)] = true;
            }

            // optimize mask 4D bounds
            for (BooleanMask4D m : mask.values())
                m.optimizeBounds();
        }
    }

    public BooleanMask5D()
    {
        this(new Rectangle5D.Integer(), new BooleanMask4D[0]);
    }

    /**
     * Returns the 4D boolean mask for the specified C position
     */
    public BooleanMask4D getMask4D(int c)
    {
        // special case of infinite C dimension
        if (bounds.sizeC == Integer.MAX_VALUE)
            return mask.firstEntry().getValue();

        return mask.get(Integer.valueOf(c));
    }

    /**
     * Returns the 3D boolean mask for the specified T, C position
     */
    public BooleanMask3D getMask3D(int t, int c)
    {
        final BooleanMask4D m = getMask4D(c);

        if (m != null)
            return m.getMask3D(t);

        return null;
    }

    /**
     * Returns the 2D boolean mask for the specified Z, T, C position
     */
    public BooleanMask2D getMask2D(int z, int t, int c)
    {
        final BooleanMask3D m = getMask3D(t, c);

        if (m != null)
            return m.getMask2D(z);

        return null;
    }

    /**
     * Return <code>true</code> if boolean mask is empty
     */
    public boolean isEmpty()
    {
        return bounds.isEmpty();
    }

    /**
     * Return true if mask contains the specified point
     */
    public boolean contains(int x, int y, int z, int t, int c)
    {
        if (bounds.contains(x, y, z, t, c))
        {
            final BooleanMask4D m4d = getMask4D(c);

            if (m4d != null)
                return m4d.contains(x, y, z, t);
        }

        return false;
    }

    /**
     * Return true if mask contains the specified 2D mask at position Z, T, C
     */
    public boolean contains(BooleanMask2D booleanMask, int z, int t, int c)
    {
        if (isEmpty())
            return false;

        final BooleanMask2D mask2d = getMask2D(z, t, c);

        if (mask2d != null)
            return mask2d.contains(booleanMask);

        return false;
    }

    /**
     * Return true if mask contains the specified 3D mask at position T, C
     */
    public boolean contains(BooleanMask3D booleanMask, int t, int c)
    {
        if (isEmpty())
            return false;

        final BooleanMask3D mask3d = getMask3D(t, c);

        if (mask3d != null)
            return mask3d.contains(booleanMask);

        return false;
    }

    /**
     * Return true if mask contains the specified 4D mask at position C
     */
    public boolean contains(BooleanMask4D booleanMask, int c)
    {
        if (isEmpty())
            return false;

        final BooleanMask4D mask4d = getMask4D(c);

        if (mask4d != null)
            return mask4d.contains(booleanMask);

        return false;
    }

    /**
     * Return true if mask contains the specified 5D mask.
     */
    public boolean contains(BooleanMask5D booleanMask)
    {
        if (isEmpty())
            return false;

        final int sizeC = booleanMask.bounds.sizeC;

        // check for special MAX_INTEGER case (infinite C dim)
        if (sizeC == Integer.MAX_VALUE)
        {
            // we cannot contains it if we are not on infinite C dim too
            if (bounds.sizeC != Integer.MAX_VALUE)
                return false;

            return booleanMask.mask.firstEntry().getValue().contains(mask.firstEntry().getValue());
        }

        final int offC = booleanMask.bounds.c;

        for (int c = offC; c < offC + sizeC; c++)
            if (!contains(booleanMask.getMask4D(c), c))
                return false;

        return true;
    }

    /**
     * Return true if mask intersects (contains at least one point) the specified 2D mask at
     * position Z, T, C
     */
    public boolean intersects(BooleanMask2D booleanMask, int z, int t, int c)
    {
        if (isEmpty())
            return false;

        final BooleanMask2D mask2d = getMask2D(z, t, c);

        if (mask2d != null)
            return mask2d.intersects(booleanMask);

        return false;
    }

    /**
     * Return true if mask intersects (contains at least one point) the specified 3D mask at
     * position T, C
     */
    public boolean intersects(BooleanMask3D booleanMask, int t, int c)
    {
        if (isEmpty())
            return false;

        final BooleanMask3D mask3d = getMask3D(t, c);

        if (mask3d != null)
            return mask3d.intersects(booleanMask);

        return false;
    }

    /**
     * Return true if mask intersects (contains at least one point) the specified 4D mask at
     * position C
     */
    public boolean intersects(BooleanMask4D booleanMask, int c)
    {
        if (isEmpty())
            return false;

        final BooleanMask4D mask4d = getMask4D(c);

        if (mask4d != null)
            return mask4d.intersects(booleanMask);

        return false;
    }

    /**
     * Return true if mask intersects (contains at least one point) the specified 5D mask region
     */
    public boolean intersects(BooleanMask5D booleanMask)
    {
        if (isEmpty())
            return false;

        final int sizeC = booleanMask.bounds.sizeC;

        // check for special MAX_INTEGER case (infinite C dim)
        if (sizeC == Integer.MAX_VALUE)
        {
            // get the single T slice
            final BooleanMask4D mask4d = booleanMask.mask.firstEntry().getValue();

            // test with every slice
            for (BooleanMask4D m : mask.values())
                if (m.intersects(mask4d))
                    return true;

            return false;
        }

        // check for special MAX_INTEGER case (infinite C dim)
        if (bounds.sizeC == Integer.MAX_VALUE)
        {
            // get the single T slice
            final BooleanMask4D mask4d = mask.firstEntry().getValue();

            // test with every slice
            for (BooleanMask4D m : booleanMask.mask.values())
                if (m.intersects(mask4d))
                    return true;

            return false;
        }

        final int offC = booleanMask.bounds.c;

        for (int c = offC; c < offC + sizeC; c++)
            if (intersects(booleanMask.getMask4D(c), c))
                return true;

        return false;
    }

    /**
     * Optimize mask bounds so it fits mask content.
     */
    public Rectangle5D.Integer getOptimizedBounds(boolean compute4DBounds)
    {
        final Rectangle5D.Integer result = new Rectangle5D.Integer();

        if (mask.isEmpty())
            return result;

        Rectangle4D.Integer bounds4D = null;

        for (BooleanMask4D m4d : mask.values())
        {
            // get optimized 4D bounds for each C
            final Rectangle4D.Integer optB4d;

            if (compute4DBounds)
                optB4d = m4d.getOptimizedBounds();
            else
                optB4d = new Rectangle4D.Integer(m4d.bounds);

            // only add non empty bounds
            if (!optB4d.isEmpty())
            {
                if (bounds4D == null)
                    bounds4D = optB4d;
                else
                    bounds4D.add(optB4d);
            }
        }

        // empty ?
        if ((bounds4D == null) || bounds4D.isEmpty())
            return result;

        int minC = mask.firstKey().intValue();
        int maxC = mask.lastKey().intValue();

        // set 4D bounds to start with
        result.setX(bounds4D.x);
        result.setY(bounds4D.y);
        result.setZ(bounds4D.z);
        result.setT(bounds4D.t);
        result.setSizeX(bounds4D.sizeX);
        result.setSizeY(bounds4D.sizeY);
        result.setSizeZ(bounds4D.sizeZ);
        result.setSizeT(bounds4D.sizeT);

        // single C --> check for special MAX_INTEGER case
        if ((minC == maxC) && (bounds.sizeC == Integer.MAX_VALUE))
        {
            result.setC(Integer.MIN_VALUE);
            result.setSizeC(Integer.MAX_VALUE);
        }
        else
        {
            result.setC(minC);
            result.setSizeC((maxC - minC) + 1);
        }

        return result;
    }

    /**
     * Optimize mask bounds so it fits mask content.
     */
    public Rectangle5D.Integer getOptimizedBounds()
    {
        return getOptimizedBounds(true);
    }

    /**
     * Optimize mask bounds so it fits mask content.
     */
    public void optimizeBounds()
    {
        // start by optimizing 4D bounds
        for (BooleanMask4D m : mask.values())
            m.optimizeBounds();

        moveBounds(getOptimizedBounds(false));
    }

    /**
     * Change the bounds of BooleanMask.<br>
     * Keep mask data intersecting from old bounds.
     */
    public void moveBounds(Rectangle5D.Integer value)
    {
        // bounds changed ?
        if (!bounds.equals(value))
        {
            // changed to empty mask
            if (value.isEmpty())
            {
                // clear bounds and mask
                bounds = new Rectangle5D.Integer();
                mask.clear();
                return;
            }

            final Rectangle4D.Integer bounds4D = new Rectangle4D.Integer(value.x, value.y, value.z, value.t,
                    value.sizeX, value.sizeY, value.sizeZ, value.sizeT);

            // it was infinite C dim ?
            if (bounds.sizeC == Integer.MAX_VALUE)
            {
                // get the single 4D mask
                final BooleanMask4D m4d = mask.firstEntry().getValue();

                // adjust 4D bounds if needed to the single 4D mask
                m4d.moveBounds(bounds4D);

                // we passed from infinite C to defined C range
                if (value.sizeC != Integer.MAX_VALUE)
                {
                    // assign the same 4D mask for all C position
                    mask.clear();
                    for (int c = 0; c <= value.sizeC; c++)
                        mask.put(Integer.valueOf(c + value.c), (BooleanMask4D) m4d.clone());
                }
            }
            // we pass to infinite C dim
            else if (value.sizeT == Integer.MAX_VALUE)
            {
                // try to use the 4D mask at C position
                BooleanMask4D mask4D = getMask4D(value.c);

                // otherwise we use the first found 2D mask
                if ((mask4D == null) && !mask.isEmpty())
                    mask4D = mask.firstEntry().getValue();

                // set new mask
                mask.clear();
                if (mask4D != null)
                    mask.put(Integer.valueOf(Integer.MIN_VALUE), mask4D);
            }
            else
            {
                // create new mask array
                final BooleanMask4D[] newMask = new BooleanMask4D[value.sizeC];

                for (int c = 0; c < value.sizeC; c++)
                {
                    final BooleanMask4D mask4D = getMask4D(value.c + c);

                    if (mask4D != null)
                        // adjust 4D bounds
                        mask4D.moveBounds(bounds4D);

                    newMask[c] = mask4D;
                }

                // set new mask
                mask.clear();
                for (int c = 0; c < value.sizeC; c++)
                    mask.put(Integer.valueOf(value.c + c), newMask[c]);
            }

            bounds = value;
        }
    }

    int[] toInt5D(int[] source4D, int c)
    {
        final int[] result = new int[(source4D.length * 5) / 4];

        int pt = 0;
        for (int i = 0; i < source4D.length; i += 4)
        {
            result[pt++] = source4D[i + 0];
            result[pt++] = source4D[i + 1];
            result[pt++] = source4D[i + 2];
            result[pt++] = source4D[i + 3];
            result[pt++] = c;
        }

        return result;
    }

    /**
     * Return an array of {@link icy.type.point.Point5D.Integer} containing the contour/surface
     * points
     * of the 5D mask.<br>
     * Points are returned in ascending XYZTC order. <br>
     * <br>
     * WARNING: The basic implementation is not totally accurate.<br>
     * It returns all points from the first and the last C slices + contour points for intermediate
     * C
     * slices.
     * 
     * @see #getContourPointsAsIntArray()
     */
    public Point5D.Integer[] getContourPoints()
    {
        return Point5D.Integer.toPoint5D(getContourPointsAsIntArray());
    }

    /**
     * Return an array of integer containing the contour/surface points of the 5D mask.<br>
     * <code>result.length</code> = number of point * 4<br>
     * <code>result[(pt * 4) + 0]</code> = X coordinate for point <i>pt</i>.<br>
     * <code>result[(pt * 4) + 1]</code> = Y coordinate for point <i>pt</i>.<br>
     * <code>result[(pt * 4) + 2]</code> = Z coordinate for point <i>pt</i>.<br>
     * <code>result[(pt * 4) + 3]</code> = T coordinate for point <i>pt</i>.<br>
     * <code>result[(pt * 5) + 4]</code> = C coordinate for point <i>pt</i>.<br>
     * Points are returned in ascending XYZTC order.<br>
     * <br>
     * WARNING: The basic implementation is not totally accurate.<br>
     * It returns all points from the first and the last C slices + contour points for intermediate
     * C
     * slices.
     * 
     * @see #getContourPoints()
     */
    public int[] getContourPointsAsIntArray()
    {
        final DynamicArray.Int result = new DynamicArray.Int(8);

        // perimeter = first slice volume + inter slices perimeter + last slice volume
        // TODO: fix this method and use real 5D contour point
        if (mask.size() <= 2)
        {
            for (Entry<Integer, BooleanMask4D> entry : mask.entrySet())
                result.add(toInt5D(entry.getValue().getPointsAsIntArray(), entry.getKey().intValue()));
        }
        else
        {
            final Entry<Integer, BooleanMask4D> firstEntry = mask.firstEntry();
            final Entry<Integer, BooleanMask4D> lastEntry = mask.lastEntry();
            final Integer firstKey = firstEntry.getKey();
            final Integer lastKey = lastEntry.getKey();

            result.add(toInt5D(firstEntry.getValue().getPointsAsIntArray(), firstKey.intValue()));

            for (Entry<Integer, BooleanMask4D> entry : mask.subMap(firstKey, false, lastKey, false).entrySet())
                result.add(toInt5D(entry.getValue().getContourPointsAsIntArray(), entry.getKey().intValue()));

            result.add(toInt5D(lastEntry.getValue().getPointsAsIntArray(), lastKey.intValue()));
        }

        return result.asArray();
    }

    /**
     * Return an array of {@link icy.type.point.Point5D.Integer} representing all points of the
     * current 5D mask.<br>
     * Points are returned in ascending XYZTC order.
     */
    public Point5D.Integer[] getPoints()
    {
        return Point5D.Integer.toPoint5D(getPointsAsIntArray());
    }

    /**
     * Return an array of integer representing all points of the current 5D mask.<br>
     * <code>result.length</code> = number of point * 5<br>
     * <code>result[(pt * 5) + 0]</code> = X coordinate for point <i>pt</i>.<br>
     * <code>result[(pt * 5) + 1]</code> = Y coordinate for point <i>pt</i>.<br>
     * <code>result[(pt * 5) + 2]</code> = Z coordinate for point <i>pt</i>.<br>
     * <code>result[(pt * 5) + 3]</code> = T coordinate for point <i>pt</i>.<br>
     * <code>result[(pt * 5) + 4]</code> = C coordinate for point <i>pt</i>.<br>
     * Points are returned in ascending XYZTC order.
     */
    public int[] getPointsAsIntArray()
    {
        final DynamicArray.Int result = new DynamicArray.Int(8);

        for (Entry<Integer, BooleanMask4D> entry : mask.entrySet())
            result.add(toInt5D(entry.getValue().getPointsAsIntArray(), entry.getKey().intValue()));

        return result.asArray();
    }

    @Override
    public Object clone()
    {
        final BooleanMask5D result = new BooleanMask5D();

        result.bounds = new Rectangle5D.Integer(bounds);
        for (Entry<Integer, BooleanMask4D> entry : mask.entrySet())
            result.mask.put(entry.getKey(), (BooleanMask4D) entry.getValue().clone());

        return result;
    }
}
