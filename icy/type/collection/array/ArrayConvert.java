/**
 * 
 */
package icy.type.collection.array;

import icy.type.TypeUtil;

/**
 * @author Stephane
 */
public class ArrayConvert
{
    /**
     * Get maximum length in bytes for a copy from in to out with specified offset and step.<br>
     * If specified length != -1 then the value is directly returned (assumed to be in bytes).
     */
    static int getCopyLengthInBytes(Object in, int inOffset, int inStep, Object out, int outOffset, int outStep,
            int length)
    {
        if (length == -1)
            return getCopyLengthInBytes(in, inOffset, inStep, out, outOffset, outStep);

        return length;
    }

    /**
     * Get maximum length in bytes for a copy from in to out with specified offset and step.<br>
     */
    static int getCopyLengthInBytes(Object in, int inOffset, int inStep, Object out, int outOffset, int outStep)
    {
        // 'in' object can't be null !
        final int len = getCopyLengthInBytes(in, inOffset, inStep);

        if (out == null)
            return len;

        return Math.min(len, getCopyLengthInBytes(out, outOffset, outStep));
    }

    /**
     * Get maximum length in bytes for a copy from in to out with specified offset.<br>
     * If specified length != -1 then the value is directly returned (assumed to be in bytes).
     */
    static int getCopyLengthInBytes(Object in, int inOffset, Object out, int outOffset, int length)
    {
        if (length == -1)
            return getCopyLengthInBytes(in, inOffset, out, outOffset);

        return length;
    }

    /**
     * Get maximum length in bytes for a copy from in to out with specified offset.
     */
    public static int getCopyLengthInBytes(Object in, int inOffset, Object out, int outOffset)
    {
        // 'in' object can't be null !
        final int len = getCopyLengthInBytes(in, inOffset);

        if (out == null)
            return len;

        return Math.min(len, getCopyLengthInBytes(out, outOffset));
    }

    /**
     * Get length in bytes for a copy from or to array with specified offset and step.
     */
    public static int getCopyLengthInBytes(Object array, int offset, int step)
    {
        final int result = ArrayUtil.getCopyLength(array, offset) * TypeUtil.sizeOf(TypeUtil.getDataType(array));

        // ex : data.lenght = 10, offset = 2, step = 3
        // data[2], data[5], data[8] can be copied
        // but (10 - 2) / 3 give 2 because we loss floating part...
        if ((offset > 0) && (offset < step))
            return (result / step) + 1;

        return result / step;
    }

    /**
     * Get length in bytes for a copy from or to array with specified offset.
     */
    public static int getCopyLengthInBytes(Object array, int offset)
    {
        return ArrayUtil.getCopyLength(array, offset) * TypeUtil.sizeOf(TypeUtil.getDataType(array));
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param inOffset
     *        position where we start read data from
     * @param inStep
     *        input offset increment step (given in output type unit)
     * @param out
     *        output array which is used to receive result (and so define wanted type)
     * @param outOffset
     *        position where we start to write data to
     * @param outStep
     *        output offset increment step
     * @param byteLength
     *        number of bytes to compute (-1 means we will use the maximum possible)
     */
    public static byte[] byteArrayToByteArray(byte[] in, int inOffset, int inStep, byte[] out, int outOffset,
            int outStep, int byteLength)
    {
        final int len = getCopyLengthInBytes(in, inOffset, inStep, out, outOffset, outStep, byteLength);
        final byte[] result = Array1DUtil.allocIfNull(out, outOffset + (len * outStep));

        // simple copy ?
        if ((inStep == 1) && (outStep == 1))
            System.arraycopy(in, inOffset, result, outOffset, len);
        else
        {
            int inOff = inOffset;
            int outOff = outOffset;

            for (int i = 0; i < len; i++)
            {
                result[outOff] = in[inOff];
                inOff += inStep;
                outOff += outStep;
            }
        }

        return result;
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param inOffset
     *        position where we start read data from
     * @param out
     *        output array which is used to receive result (and so define wanted type)
     * @param outOffset
     *        position where we start to write data to
     * @param byteLength
     *        number of bytes to compute (-1 means we will use the maximum possible)
     */
    public static byte[] byteArrayToByteArray(byte[] in, int inOffset, byte[] out, int outOffset, int byteLength)
    {
        return byteArrayToByteArray(in, inOffset, 1, out, outOffset, 1, byteLength);
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param out
     *        output array which is used to receive result (and so define wanted type)
     */
    public static byte[] byteArrayToByteArray(byte[] in, byte[] out)
    {
        return byteArrayToByteArray(in, 0, 1, out, 0, 1, -1);
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     */
    public static byte[] byteArrayToByteArray(byte[] in)
    {
        return byteArrayToByteArray(in, 0, 1, null, 0, 1, -1);
    }

    /**
     * Bit transform and return the 'in' byte array in 'out' short array
     * 
     * @param in
     *        input array
     * @param inOffset
     *        position where we start read data from
     * @param inStep
     *        input offset increment step (given in output type unit)
     * @param out
     *        output array which is used to receive result (and so define wanted type)
     * @param outOffset
     *        position where we start to write data to
     * @param outStep
     *        output offset increment step
     * @param byteLength
     *        number of bytes to compute (-1 means we will use the maximum possible)
     * @param little
     *        little endian order
     */
    public static short[] byteArrayToShortArray(byte[] in, int inOffset, int inStep, short[] out, int outOffset,
            int outStep, int byteLength, boolean little)
    {
        final int adjInStep = inStep * 2;
        final int len = getCopyLengthInBytes(in, inOffset, adjInStep, out, outOffset, outStep, byteLength) / 2;
        final short[] result = Array1DUtil.allocIfNull(out, outOffset + (len * outStep));

        int inOff = inOffset;
        int outOff = outOffset;

        if (little)
        {
            for (int i = 0; i < len; i++)
            {
                result[outOff] = (short) (((in[inOff + 0] & 0xFF) << 0) + ((in[inOff + 1] & 0xFF) << 8));
                inOff += adjInStep;
                outOff += outStep;
            }
        }
        else
        {
            for (int i = 0; i < len; i++)
            {
                result[outOff] = (short) (((in[inOff + 0] & 0xFF) << 8) + ((in[inOff + 1] & 0xFF) << 0));
                inOff += adjInStep;
                outOff += outStep;
            }
        }

        return result;
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param inOffset
     *        position where we start read data from
     * @param out
     *        output array which is used to receive result (and so define wanted type)
     * @param outOffset
     *        position where we start to write data to
     * @param byteLength
     *        number of bytes to compute (-1 means we will use the maximum possible)
     * @param little
     *        little endian order
     */
    public static short[] byteArrayToShortArray(byte[] in, int inOffset, short[] out, int outOffset, int byteLength,
            boolean little)
    {
        return byteArrayToShortArray(in, inOffset, 1, out, outOffset, 1, byteLength, little);
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param out
     *        output array which is used to receive result (and so define wanted type)
     * @param little
     *        little endian order
     */
    public static short[] byteArrayToShortArray(byte[] in, short[] out, boolean little)
    {
        return byteArrayToShortArray(in, 0, 1, out, 0, 1, -1, little);
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param little
     *        little endian order
     */
    public static short[] byteArrayToShortArray(byte[] in, boolean little)
    {
        return byteArrayToShortArray(in, 0, 1, null, 0, 1, -1, little);
    }

    /**
     * Bit transform and return the 'in' byte array in 'out' int array
     * 
     * @param in
     *        input array
     * @param inOffset
     *        position where we start read data from
     * @param inStep
     *        input offset increment step (given in output type unit)
     * @param out
     *        output array which is used to receive result (and so define wanted type)
     * @param outOffset
     *        position where we start to write data to
     * @param outStep
     *        output offset increment step
     * @param byteLength
     *        number of bytes to compute (-1 means we will use the maximum possible)
     * @param little
     *        little endian order
     */
    public static int[] byteArrayToIntArray(byte[] in, int inOffset, int inStep, int[] out, int outOffset, int outStep,
            int byteLength, boolean little)
    {
        final int adjInStep = inStep * 4;
        final int len = getCopyLengthInBytes(in, inOffset, adjInStep, out, outOffset, outStep, byteLength) / 4;
        final int[] result = Array1DUtil.allocIfNull(out, outOffset + (len * outStep));

        int inOff = inOffset;
        int outOff = outOffset;

        if (little)
        {
            for (int i = 0; i < len; i++)
            {
                result[outOff] = ((in[inOff + 0] & 0xFF) << 0) + ((in[inOff + 1] & 0xFF) << 8)
                        + ((in[inOff + 2] & 0xFF) << 16) + ((in[inOff + 3] & 0xFF) << 24);
                inOff += adjInStep;
                outOff += outStep;
            }
        }
        else
        {
            for (int i = 0; i < len; i++)
            {
                result[outOff] = ((in[inOff + 0] & 0xFF) << 24) + ((in[inOff + 1] & 0xFF) << 16)
                        + ((in[inOff + 2] & 0xFF) << 8) + ((in[inOff + 3] & 0xFF) << 0);
                inOff += adjInStep;
                outOff += outStep;
            }
        }

        return result;
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param inOffset
     *        position where we start read data from
     * @param out
     *        output array which is used to receive result (and so define wanted type)
     * @param outOffset
     *        position where we start to write data to
     * @param byteLength
     *        number of bytes to compute (-1 means we will use the maximum possible)
     * @param little
     *        little endian order
     */
    public static int[] byteArrayToIntArray(byte[] in, int inOffset, int[] out, int outOffset, int byteLength,
            boolean little)
    {
        return byteArrayToIntArray(in, inOffset, 1, out, outOffset, 1, byteLength, little);
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param out
     *        output array which is used to receive result (and so define wanted type)
     * @param little
     *        little endian order
     */
    public static int[] byteArrayToIntArray(byte[] in, int[] out, boolean little)
    {
        return byteArrayToIntArray(in, 0, 1, out, 0, 1, -1, little);
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param little
     *        little endian order
     */
    public static int[] byteArrayToIntArray(byte[] in, boolean little)
    {
        return byteArrayToIntArray(in, 0, 1, null, 0, 1, -1, little);
    }

    /**
     * Bit transform and return the 'in' byte array in 'out' float array
     * 
     * @param in
     *        input array
     * @param inOffset
     *        position where we start read data from
     * @param inStep
     *        input offset increment step (given in output type unit)
     * @param out
     *        output array which is used to receive result (and so define wanted type)
     * @param outOffset
     *        position where we start to write data to
     * @param outStep
     *        output offset increment step
     * @param byteLength
     *        number of bytes to compute (-1 means we will use the maximum possible)
     * @param little
     *        little endian order
     */
    public static float[] byteArrayToFloatArray(byte[] in, int inOffset, int inStep, float[] out, int outOffset,
            int outStep, int byteLength, boolean little)
    {
        final int adjInStep = inStep * 4;
        final int len = getCopyLengthInBytes(in, inOffset, adjInStep, out, outOffset, outStep, byteLength) / 4;
        final float[] result = Array1DUtil.allocIfNull(out, outOffset + (len * outStep));

        int inOff = inOffset;
        int outOff = outOffset;

        if (little)
        {
            for (int i = 0; i < len; i++)
            {
                final int value = ((in[inOff + 0] & 0xFF) << 0) + ((in[inOff + 1] & 0xFF) << 8)
                        + ((in[inOff + 2] & 0xFF) << 16) + ((in[inOff + 3] & 0xFF) << 24);
                inOff += adjInStep;
                result[outOff] = Float.intBitsToFloat(value);
                outOff += outStep;
            }
        }
        else
        {
            for (int i = 0; i < len; i++)
            {
                final int value = ((in[inOff + 0] & 0xFF) << 24) + ((in[inOff + 1] & 0xFF) << 16)
                        + ((in[inOff + 2] & 0xFF) << 8) + ((in[inOff + 3] & 0xFF) << 0);
                inOff += adjInStep;
                result[outOff] = Float.intBitsToFloat(value);
                outOff += outStep;
            }
        }

        return result;
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param inOffset
     *        position where we start read data from
     * @param out
     *        output array which is used to receive result (and so define wanted type)
     * @param outOffset
     *        position where we start to write data to
     * @param byteLength
     *        number of bytes to compute (-1 means we will use the maximum possible)
     * @param little
     *        little endian order
     */
    public static float[] byteArrayToFloatArray(byte[] in, int inOffset, float[] out, int outOffset, int byteLength,
            boolean little)
    {
        return byteArrayToFloatArray(in, inOffset, 1, out, outOffset, 1, byteLength, little);
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param out
     *        output array which is used to receive result (and so define wanted type)
     * @param little
     *        little endian order
     */
    public static float[] byteArrayToFloatArray(byte[] in, float[] out, boolean little)
    {
        return byteArrayToFloatArray(in, 0, 1, out, 0, 1, -1, little);
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param little
     *        little endian order
     */
    public static float[] byteArrayToFloatArray(byte[] in, boolean little)
    {
        return byteArrayToFloatArray(in, 0, 1, null, 0, 1, -1, little);
    }

    /**
     * Bit transform and return the 'in' byte array in 'out' double array
     * 
     * @param in
     *        input array
     * @param inOffset
     *        position where we start read data from
     * @param inStep
     *        input offset increment step (given in output type unit)
     * @param out
     *        output array which is used to receive result (and so define wanted type)
     * @param outOffset
     *        position where we start to write data to
     * @param outStep
     *        output offset increment step
     * @param byteLength
     *        number of bytes to compute (-1 means we will use the maximum possible)
     * @param little
     *        little endian order
     */
    public static double[] byteArrayToDoubleArray(byte[] in, int inOffset, int inStep, double[] out, int outOffset,
            int outStep, int byteLength, boolean little)
    {
        final int adjInStep = inStep * 8;
        final int len = getCopyLengthInBytes(in, inOffset, adjInStep, out, outOffset, outStep, byteLength) / 8;
        final double[] result = Array1DUtil.allocIfNull(out, outOffset + (len * outStep));

        int inOff = inOffset;
        int outOff = outOffset;

        if (little)
        {
            for (int i = 0; i < len; i++)
            {
                final int v1 = ((in[inOff + 0] & 0xFF) << 0) + ((in[inOff + 1] & 0xFF) << 8)
                        + ((in[inOff + 2] & 0xFF) << 16) + ((in[inOff + 3] & 0xFF) << 24);
                inOff += 4;
                final int v2 = (in[inOff] & 0xFF) + ((in[inOff + 1] & 0xFF) << 8) + ((in[inOff + 2] & 0xFF) << 16)
                        + ((in[inOff + 3] & 0xFF) << 24);
                inOff += adjInStep - 4;
                result[outOff] = Double.longBitsToDouble(((long) v1 << 0) + ((long) v2 << 32));
                outOff += outStep;
            }
        }
        else
        {
            for (int i = 0; i < len; i++)
            {
                final int v1 = ((in[inOff + 0] & 0xFF) << 24) + ((in[inOff + 1] & 0xFF) << 16)
                        + ((in[inOff + 2] & 0xFF) << 8) + ((in[inOff + 3] & 0xFF) << 0);
                inOff += 4;
                final int v2 = ((in[inOff + 0] & 0xFF) << 24) + ((in[inOff + 1] & 0xFF) << 16)
                        + ((in[inOff + 2] & 0xFF) << 8) + ((in[inOff + 3] & 0xFF) << 0);
                inOff += adjInStep - 4;
                result[outOff] = Double.longBitsToDouble(((long) v1 << 32) + ((long) v2 << 0));
                outOff += outStep;
            }
        }

        return result;
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param inOffset
     *        position where we start read data from
     * @param out
     *        output array which is used to receive result (and so define wanted type)
     * @param outOffset
     *        position where we start to write data to
     * @param byteLength
     *        number of bytes to compute (-1 means we will use the maximum possible)
     * @param little
     *        little endian order
     */
    public static double[] byteArrayToDoubleArray(byte[] in, int inOffset, double[] out, int outOffset, int byteLength,
            boolean little)
    {
        return byteArrayToDoubleArray(in, inOffset, 1, out, outOffset, 1, byteLength, little);
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param out
     *        output array which is used to receive result (and so define wanted type)
     * @param little
     *        little endian order
     */
    public static double[] byteArrayToDoubleArray(byte[] in, double[] out, boolean little)
    {
        return byteArrayToDoubleArray(in, 0, 1, out, 0, 1, -1, little);
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param little
     *        little endian order
     */
    public static double[] byteArrayToDoubleArray(byte[] in, boolean little)
    {
        return byteArrayToDoubleArray(in, 0, 1, null, 0, 1, -1, little);
    }

    /**
     * Bit transform and return the 'in' short array as byte array
     * 
     * @param in
     *        input array
     * @param inOffset
     *        position where we start read data from
     * @param inStep
     *        input offset increment step
     * @param out
     *        output byte array which is used to receive result
     * @param outOffset
     *        position where we start to write data to
     * @param outStep
     *        output offset increment step (given in input type unit)
     * @param byteLength
     *        number of bytes to compute (-1 means we will use the maximum possible)
     * @param little
     *        little endian order
     */
    public static byte[] shortArrayToByteArray(short[] in, int inOffset, int inStep, byte[] out, int outOffset,
            int outStep, int byteLength, boolean little)
    {
        final int adjOutStep = outStep * 2;
        final int len = getCopyLengthInBytes(in, inOffset, inStep, out, outOffset, adjOutStep, byteLength);
        final byte[] result = Array1DUtil.allocIfNull(out, outOffset + (len * adjOutStep));

        int inOff = inOffset;
        int outOff = outOffset;

        if (little)
        {
            for (int i = 0; i < len; i++)
            {
                final short value = in[inOff];
                inOff += inStep;
                result[outOff + 0] = (byte) (value >> 0);
                result[outOff + 1] = (byte) (value >> 8);
                outOff += adjOutStep;
            }
        }
        else
        {
            for (int i = 0; i < len; i++)
            {
                final short value = in[inOff];
                inOff += inStep;
                result[outOff + 0] = (byte) (value >> 8);
                result[outOff + 1] = (byte) (value >> 0);
                outOff += adjOutStep;
            }
        }

        return result;
    }

    /**
     * Bit transform and return the 'in' int array as byte array
     * 
     * @param in
     *        input array
     * @param inOffset
     *        position where we start read data from
     * @param inStep
     *        input offset increment step
     * @param out
     *        output byte array which is used to receive result
     * @param outOffset
     *        position where we start to write data to
     * @param outStep
     *        output offset increment step (given in input type unit)
     * @param byteLength
     *        number of bytes to compute (-1 means we will use the maximum possible)
     * @param little
     *        little endian order
     */
    public static byte[] intArrayToByteArray(int[] in, int inOffset, int inStep, byte[] out, int outOffset,
            int outStep, int byteLength, boolean little)
    {
        final int adjOutStep = outStep * 4;
        final int len = getCopyLengthInBytes(in, inOffset, inStep, out, outOffset, adjOutStep, byteLength);
        final byte[] result = Array1DUtil.allocIfNull(out, outOffset + (len * adjOutStep));

        int inOff = inOffset;
        int outOff = outOffset;

        if (little)
        {
            for (int i = 0; i < len; i++)
            {
                final int value = in[inOff];
                inOff += inStep;
                result[outOff + 0] = (byte) (value >> 0);
                result[outOff + 1] = (byte) (value >> 8);
                result[outOff + 2] = (byte) (value >> 16);
                result[outOff + 3] = (byte) (value >> 24);
                outOff += adjOutStep;
            }
        }
        else
        {
            for (int i = 0; i < len; i++)
            {
                final int value = in[inOff];
                inOff += inStep;
                result[outOff + 0] = (byte) (value >> 24);
                result[outOff + 1] = (byte) (value >> 16);
                result[outOff + 2] = (byte) (value >> 8);
                result[outOff + 3] = (byte) (value >> 0);
                outOff += adjOutStep;
            }
        }

        return result;
    }

    /**
     * Bit transform and return the 'in' float array as byte array
     * 
     * @param in
     *        input array
     * @param inOffset
     *        position where we start read data from
     * @param inStep
     *        input offset increment step
     * @param out
     *        output byte array which is used to receive result
     * @param outOffset
     *        position where we start to write data to
     * @param outStep
     *        output offset increment step (given in input type unit)
     * @param byteLength
     *        number of bytes to compute (-1 means we will use the maximum possible)
     * @param little
     *        little endian order
     */
    public static byte[] floatArrayToByteArray(float[] in, int inOffset, int inStep, byte[] out, int outOffset,
            int outStep, int byteLength, boolean little)
    {
        final int adjOutStep = outStep * 4;
        final int len = getCopyLengthInBytes(in, inOffset, inStep, out, outOffset, adjOutStep, byteLength);
        final byte[] result = Array1DUtil.allocIfNull(out, outOffset + (len * adjOutStep));

        int inOff = inOffset;
        int outOff = outOffset;

        if (little)
        {
            for (int i = 0; i < len; i++)
            {
                final int value = Float.floatToRawIntBits(in[inOff]);
                inOff += inStep;
                result[outOff + 0] = (byte) (value >> 0);
                result[outOff + 1] = (byte) (value >> 8);
                result[outOff + 2] = (byte) (value >> 16);
                result[outOff + 3] = (byte) (value >> 24);
                outOff += adjOutStep;
            }
        }
        else
        {
            for (int i = 0; i < len; i++)
            {
                final int value = Float.floatToRawIntBits(in[inOff]);
                inOff += inStep;
                result[outOff + 0] = (byte) (value >> 24);
                result[outOff + 1] = (byte) (value >> 16);
                result[outOff + 2] = (byte) (value >> 8);
                result[outOff + 3] = (byte) (value >> 0);
                outOff += adjOutStep;
            }
        }

        return result;
    }

    /**
     * Bit transform and return the 'in' double array as byte array
     * 
     * @param in
     *        input array
     * @param inOffset
     *        position where we start read data from
     * @param inStep
     *        input offset increment step
     * @param out
     *        output byte array which is used to receive result
     * @param outOffset
     *        position where we start to write data to
     * @param outStep
     *        output offset increment step (given in input type unit)
     * @param byteLength
     *        number of bytes to compute (-1 means we will use the maximum possible)
     * @param little
     *        little endian order
     */
    public static byte[] doubleArrayToByteArray(double[] in, int inOffset, int inStep, byte[] out, int outOffset,
            int outStep, int byteLength, boolean little)
    {
        final int adjOutStep = outStep * 8;
        final int len = getCopyLengthInBytes(in, inOffset, inStep, out, outOffset, adjOutStep, byteLength);
        final byte[] result = Array1DUtil.allocIfNull(out, outOffset + (len * adjOutStep));

        int inOff = inOffset;
        int outOff = outOffset;

        if (little)
        {
            for (int i = 0; i < len; i++)
            {
                final long value = Double.doubleToRawLongBits(in[inOff]);
                inOff += inStep;
                final int v1 = (int) (value >> 0);
                result[outOff + 0] = (byte) (v1 >> 0);
                result[outOff + 1] = (byte) (v1 >> 8);
                result[outOff + 2] = (byte) (v1 >> 16);
                result[outOff + 3] = (byte) (v1 >> 24);
                outOff += 4;
                final int v2 = (int) (value >> 32);
                result[outOff + 0] = (byte) (v2 >> 0);
                result[outOff + 1] = (byte) (v2 >> 8);
                result[outOff + 2] = (byte) (v2 >> 16);
                result[outOff + 3] = (byte) (v2 >> 24);
                outOff += adjOutStep - 4;
            }
        }
        else
        {
            for (int i = 0; i < len; i++)
            {
                final long value = Double.doubleToRawLongBits(in[inOff]);
                inOff += inStep;
                final int v1 = (int) (value >> 32);
                result[outOff + 0] = (byte) (v1 >> 24);
                result[outOff + 1] = (byte) (v1 >> 16);
                result[outOff + 2] = (byte) (v1 >> 8);
                result[outOff + 3] = (byte) (v1 >> 0);
                outOff += 4;
                final int v2 = (int) (value >> 0);
                result[outOff + 0] = (byte) (v2 >> 24);
                result[outOff + 1] = (byte) (v2 >> 16);
                result[outOff + 2] = (byte) (v2 >> 8);
                result[outOff + 3] = (byte) (v2 >> 0);
                outOff += adjOutStep - 4;
            }
        }

        return result;
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param inOffset
     *        position where we start read data from
     * @param inStep
     *        input offset increment step (given in output type unit)
     * @param out
     *        output array which is used to receive result (and so define wanted type)
     * @param outOffset
     *        position where we start to write data to
     * @param outStep
     *        output offset increment step
     * @param byteLength
     *        number of bytes to compute (-1 means we will use the maximum possible)
     * @param little
     *        little endian order
     */
    public static Object byteArrayTo(byte[] in, int inOffset, int inStep, Object out, int outOffset, int outStep,
            int byteLength, boolean little)
    {
        if (out == null)
            return null;

        switch (TypeUtil.getDataType(out))
        {
            case TypeUtil.TYPE_BYTE:
                return byteArrayToByteArray(in, inOffset, inStep, (byte[]) out, outOffset, outStep, byteLength);

            case TypeUtil.TYPE_SHORT:
                return byteArrayToShortArray(in, inOffset, inStep, (short[]) out, outOffset, outStep, byteLength,
                        little);

            case TypeUtil.TYPE_INT:
                return byteArrayToIntArray(in, inOffset, inStep, (int[]) out, outOffset, outStep, byteLength, little);

            case TypeUtil.TYPE_FLOAT:
                return byteArrayToFloatArray(in, inOffset, inStep, (float[]) out, outOffset, outStep, byteLength,
                        little);

            case TypeUtil.TYPE_DOUBLE:
                return byteArrayToDoubleArray(in, inOffset, inStep, (double[]) out, outOffset, outStep, byteLength,
                        little);
        }

        return out;
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param inOffset
     *        position where we start read data from
     * @param inStep
     *        input offset increment step (given in output type unit)
     * @param out
     *        output array which is used to receive result (and so define wanted type)
     * @param little
     *        little endian order
     */
    public static Object byteArrayTo(byte[] in, int inOffset, int inStep, Object out, boolean little)
    {
        return byteArrayTo(in, inOffset, inStep, out, 0, 1, -1, little);
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param inOffset
     *        position where we start read data from
     * @param out
     *        output array which is used to receive result (and so define wanted type)
     * @param outOffset
     *        position where we start to write data to
     * @param byteLength
     *        number of bytes to compute (-1 means we will use the maximum possible)
     * @param little
     *        little endian order
     */
    public static Object byteArrayTo(byte[] in, int inOffset, Object out, int outOffset, int byteLength, boolean little)
    {
        return byteArrayTo(in, inOffset, 1, out, outOffset, 1, byteLength, little);
    }

    /**
     * Bit transform and return the 'in' byte array in the specified 'out' data type array
     * 
     * @param in
     *        input array
     * @param out
     *        output array which is used to receive result (and so define wanted type)
     * @param little
     *        little endian order
     */
    public static Object byteArrayTo(byte[] in, Object out, boolean little)
    {
        return byteArrayTo(in, 0, 1, out, 0, 1, -1, little);
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param little
     *        little endian order
     */
    public static Object byteArrayTo(byte[] in, boolean little)
    {
        return byteArrayTo(in, 0, 1, null, 0, 1, -1, little);
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param inOffset
     *        position where we start read data from
     * @param inStep
     *        input offset increment step (given in output type unit)
     * @param outDataType
     *        wanted output array data type
     * @param outOffset
     *        position where we start to write data to
     * @param outStep
     *        output offset increment step
     * @param byteLength
     *        number of bytes to compute (-1 means we will use the maximum possible)
     * @param little
     *        little endian order
     */
    public static Object byteArrayTo(byte[] in, int inOffset, int inStep, int outDataType, int outOffset, int outStep,
            int byteLength, boolean little)
    {
        switch (outDataType)
        {
            case TypeUtil.TYPE_BYTE:
                return byteArrayToByteArray(in, inOffset, inStep, null, outOffset, outStep, byteLength);

            case TypeUtil.TYPE_SHORT:
                return byteArrayToShortArray(in, inOffset, inStep, null, outOffset, outStep, byteLength, little);

            case TypeUtil.TYPE_INT:
                return byteArrayToIntArray(in, inOffset, inStep, null, outOffset, outStep, byteLength, little);

            case TypeUtil.TYPE_FLOAT:
                return byteArrayToFloatArray(in, inOffset, inStep, null, outOffset, outStep, byteLength, little);

            case TypeUtil.TYPE_DOUBLE:
                return byteArrayToDoubleArray(in, inOffset, inStep, null, outOffset, outStep, byteLength, little);
        }

        return null;
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param inOffset
     *        position where we start read data from
     * @param outDataType
     *        wanted output array data type
     * @param byteLength
     *        number of bytes to compute (-1 means we will use the maximum possible)
     * @param little
     *        little endian order
     */
    public static Object byteArrayTo(byte[] in, int inOffset, int outDataType, int byteLength, boolean little)
    {
        return byteArrayTo(in, inOffset, 1, outDataType, 0, 1, byteLength, little);
    }

    /**
     * Bit transform and return the 'in' byte array in the specified data type array
     * 
     * @param in
     *        input array
     * @param outDataType
     *        wanted output array data type
     * @param little
     *        little endian order
     */
    public static Object byteArrayTo(byte[] in, int outDataType, boolean little)
    {
        return byteArrayTo(in, 0, 1, outDataType, 0, 1, -1, little);
    }

    /**
     * Bit transform and return the 'in' array as byte array
     * 
     * @param in
     *        input array (define input type)
     * @param inOffset
     *        position where we start read data from
     * @param inStep
     *        input offset increment step
     * @param out
     *        output byte array which is used to receive result
     * @param outOffset
     *        position where we start to write data to
     * @param outStep
     *        output offset increment step (given in input type unit)
     * @param byteLength
     *        number of bytes to compute (-1 means we will use the maximum possible)
     * @param little
     *        little endian order
     */
    public static byte[] toByteArray(Object in, int inOffset, int inStep, byte[] out, int outOffset, int outStep,
            int byteLength, boolean little)
    {
        if (out == null)
            return null;

        switch (TypeUtil.getDataType(in))
        {
            case TypeUtil.TYPE_BYTE:
                return byteArrayToByteArray((byte[]) in, inOffset, inStep, out, outOffset, outStep, byteLength);

            case TypeUtil.TYPE_SHORT:
                return shortArrayToByteArray((short[]) in, inOffset, inStep, out, outOffset, outStep, byteLength,
                        little);

            case TypeUtil.TYPE_INT:
                return intArrayToByteArray((int[]) in, inOffset, inStep, out, outOffset, outStep, byteLength, little);

            case TypeUtil.TYPE_FLOAT:
                return floatArrayToByteArray((float[]) in, inOffset, inStep, out, outOffset, outStep, byteLength,
                        little);

            case TypeUtil.TYPE_DOUBLE:
                return doubleArrayToByteArray((double[]) in, inOffset, inStep, out, outOffset, outStep, byteLength,
                        little);
        }

        return out;
    }

    /**
     * Bit transform and return the 'in' array as byte array
     * 
     * @param in
     *        input array (define input type)
     * @param inOffset
     *        position where we start read data from
     * @param out
     *        output byte array which is used to receive result
     * @param outOffset
     *        position where we start to write data to
     * @param byteLength
     *        number of bytes to compute (-1 means we will use the maximum possible)
     * @param little
     *        little endian order
     */
    public static byte[] toByteArray(Object in, int inOffset, byte[] out, int outOffset, int byteLength, boolean little)
    {
        return toByteArray(in, inOffset, 1, out, outOffset, 1, byteLength, little);
    }

    /**
     * Bit transform and return the 'in' array as byte array
     * 
     * @param in
     *        input array (define input type)
     * @param out
     *        output byte array which is used to receive result
     * @param outOffset
     *        position where we start to write data to
     * @param outStep
     *        output offset increment step (given in input type unit)
     * @param little
     *        little endian order
     */
    public static byte[] toByteArray(Object in, byte[] out, int outOffset, int outStep, boolean little)
    {
        return toByteArray(in, 0, 1, out, outOffset, outStep, -1, little);
    }

    /**
     * Bit transform and return the 'in' array as byte array
     * 
     * @param in
     *        input array (define input type)
     * @param inOffset
     *        position where we start read data from
     * @param byteLength
     *        number of bytes to compute (-1 means we will use the maximum possible)
     * @param little
     *        little endian order
     */
    public static byte[] toByteArray(Object in, int inOffset, int byteLength, boolean little)
    {
        return toByteArray(in, inOffset, 1, null, 0, 1, byteLength, little);
    }

    /**
     * Bit transform and return the 'in' array as byte array
     * 
     * @param in
     *        input array (define input type)
     * @param out
     *        output byte array which is used to receive result
     * @param little
     *        little endian order
     */
    public static byte[] toByteArray(Object in, byte[] out, boolean little)
    {
        return toByteArray(in, 0, 1, out, 0, 1, -1, little);
    }

    /**
     * Bit transform and return the 'in' array as byte array
     * 
     * @param in
     *        input array (define input type)
     * @param little
     *        little endian order
     */
    public static byte[] toByteArray(Object in, boolean little)
    {
        return toByteArray(in, 0, 1, null, 0, 1, -1, little);
    }
}
