package ser;

import com.cedarsoftware.util.DeepEquals;
import junit.framework.Assert;
import org.junit.Test;
import org.nustaq.offheap.bytez.malloc.MallocBytez;
import org.nustaq.offheap.bytez.malloc.MallocBytezAllocator;
import org.nustaq.serialization.*;
import org.nustaq.serialization.simpleapi.OffHeapCoder;
import org.nustaq.serialization.simpleapi.OnHeapCoder;

import java.io.IOException;
import java.io.Serializable;

import static junit.framework.TestCase.assertTrue;

/**
 * Created by ruedi on 09.11.14.
 */
public class RawMemTest extends BasicFSTTest {

    @Override
    public void setUp() throws Exception {
        FSTConfiguration conf = FSTConfiguration.createFastBinaryConfiguration();
        out = new FSTObjectOutput(conf);
        in = new FSTObjectInput(conf);
    }

    static Object original = new Object[] {
        new BasicFSTTest.Primitives(),
        new BasicFSTTest.Primitives(),
        new BasicFSTTest.PrimitiveArray(),
        new BasicFSTTest.AscStrings(),
        new BasicFSTTest.Strings(),
        new BasicFSTTest.Bl(),
        new boolean[]{true, false, true},
        new int[]{1, 2, 3,1, 2, 31, 2, 31, 2, 31, 2, 31, 2, 31, 2, 31, 2, 31, 2, 31, 2, 31, 2, 31, 2, 31, 2, 31, 2, 31, },
        new BasicFSTTest.Primitives(),
        new BasicFSTTest.Primitives(),
        new BasicFSTTest.PrimitiveArray(),
        new BasicFSTTest.Strings(),
        new BasicFSTTest.Bl(),
        new boolean[]{true, false, true},
    };

    @Test
    public void test() {
        FSTConfiguration conf = FSTConfiguration.createFastBinaryConfiguration();
        Object deser = null;
        byte[] ser = null;

        System.out.println("binary");
        deser = smallBench(conf, original, deser);
        deser = smallBench(conf, original, deser);
        deser = smallBench(conf, original, deser);
        deser = smallBench(conf, original, deser);
        deser = smallBench(conf, original, deser);
        assertTrue(DeepEquals.deepEquals(original, deser));

        System.out.println("default");
        conf = FSTConfiguration.createDefaultConfiguration();
        deser = smallBench(conf, original, deser);
        deser = smallBench(conf, original, deser);
        deser = smallBench(conf, original, deser);
        deser = smallBench(conf, original, deser);
        deser = smallBench(conf, original, deser);
        assertTrue(DeepEquals.deepEquals(original, deser));

        System.out.println("Default LEN:"+FSTConfiguration.createDefaultConfiguration().asByteArray(original).length);
    }

    protected Object smallBench(FSTConfiguration conf, Object original, Object deser) {
        byte[] ser = null;
        int count = 0;
        long tim = System.currentTimeMillis();
        int len[] = { 0 };
        while ( System.currentTimeMillis() - tim < 2000 ) {
            count++;
            ser = conf.asByteArray(original);
            deser = conf.asObject(ser);
        }
        System.out.println("BIN COUNT:"+count);
        return deser;
    }

    enum Model {
        A,B,C
    }

    static class Engine implements Serializable {
        short capacity;
        byte cylinders;
        short maxRpm;
        String manufactureCode = "POK";
        String fuel = "Petrol";
    }

    static class SimpleTest implements Serializable {
        int serialNumber;
        short modelYear;
        boolean available;
        Model code;
        int someNumbers[] = new int[] {1,2,3,4,5};  // 5
        String vehicleCode = "123456"; // 6
        byte optionalExtras;
        Engine engine = new Engine();
        short fuelSpeed[] = new short[] {60,45};
        float fuelMpg[] = new float[] {7.1f, 3.4f};

        String make = "MAKE";
        String model = "MODEL";
    }

    static Object smallClazz = new SimpleTest();

    @Test
    public void testOffHeapCoder() throws Exception {
        OffHeapCoder coder = new OffHeapCoder(SimpleTest.class,Engine.class,Model.class);

        FSTConfiguration conf = FSTConfiguration.createFastBinaryConfiguration();
        conf.registerClass(SimpleTest.class,Engine.class,Model.class);
        byte b[] = conf.asByteArray(smallClazz);

        MallocBytezAllocator alloc = new MallocBytezAllocator();
        MallocBytez bytez = (MallocBytez) alloc.alloc(1000 * 1000);

        ohbench(original, coder, bytez);
        ohbench(original, coder, bytez);
        ohbench(original, coder, bytez);
        ohbench(original, coder, bytez);
        Object deser = ohbench(original, coder, bytez);
        assertTrue(DeepEquals.deepEquals(original, deser));

        System.out.println("-----");
        ohbench(smallClazz, coder, bytez);
        ohbench(smallClazz, coder, bytez);
        ohbench(smallClazz, coder, bytez);
        ohbench(smallClazz, coder, bytez);
        deser = ohbench(smallClazz, coder, bytez);
        assertTrue(DeepEquals.deepEquals(smallClazz, deser));

        boolean lenEx = false;
        try {
            coder.writeObject(original,bytez.getBaseAdress(),10);
        } catch (Exception e) {
            lenEx = true;
        }

        Assert.assertTrue(lenEx);

        alloc.freeAll();
    }

    protected Object ohbench(Object toSer, OffHeapCoder coder, MallocBytez bytez) throws Exception {
        long tim = System.currentTimeMillis();
        int count = 0;
        Object deser = null;
        while ( System.currentTimeMillis() - tim < 2000 ) {
            count++;
            coder.writeObject(toSer, bytez.getBaseAdress(), (int) bytez.length());
            deser = coder.readObject(bytez.getBaseAdress(),(int)bytez.length());
        }
        System.out.println("offheap enc COUNT:"+count);
        return deser;
    }


    @Test
    public void testOnHeapCoder() throws Exception {
        OnHeapCoder coder = new OnHeapCoder(SimpleTest.class,Engine.class,Model.class);

        FSTConfiguration conf = FSTConfiguration.createFastBinaryConfiguration();
        conf.registerClass(SimpleTest.class,Engine.class,Model.class);
        byte b[] = conf.asByteArray(smallClazz);

        byte arr[] = new byte[1000000];
        onhbench(original, coder, arr, 0);
        Object deser = onhbench(original, coder, arr, 0);
        assertTrue(DeepEquals.deepEquals(original, deser));

        System.out.println("-----");
        deser = onhbench(smallClazz, coder, arr, 0);
        assertTrue(DeepEquals.deepEquals(smallClazz, deser));

        boolean lenEx = false;
        try {
            coder.writeObject(original,arr,0,10);
        } catch (Exception e) {
            lenEx = true;
        }

        Assert.assertTrue(lenEx);
    }

    protected Object onhbench(Object toSer, OnHeapCoder coder, byte[] bytez, int off) throws Exception {
        long tim = System.currentTimeMillis();
        int count = 0;
        Object deser = null;
        while ( System.currentTimeMillis() - tim < 2000 ) {
            count++;
            coder.writeObject(toSer, bytez, off, (int) bytez.length);
            deser = coder.readObject(bytez, off, (int)bytez.length);
        }
        System.out.println("onheap enc COUNT:"+count);
        return deser;
    }

}
