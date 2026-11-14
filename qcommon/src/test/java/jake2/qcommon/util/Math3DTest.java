package jake2.qcommon.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class Math3DTest {

    @Test
    public void createVectorWithSomeNumsAndCreateEmptyVectorThenCopyFromFirstToSecondVectorThanCompareHimToEqual(){
        float[] vec1 = {0.1f,20.0f,5.2f};
        float[] vec2 = new float[3];
        Math3D.VectorCopy(vec1, vec2);
        assertArrayEquals(vec1,vec2, 0.0001f);
    }

    @Test
    public void createVectorWithSomeNumsAndCheckHisVectorLength(){
        float[] vec = {0.5f,2.0f,10.0f};
        assertEquals(10.21f, Math3D.VectorLength(vec), 0.01f);
    }

    @Test
    public void createVectorWithSomeNumsAndCheckHisVectorLengthAttempt2(){
        float[] vec = {3.0f,4.0f,0.0f};
        assertEquals(5.0f,Math3D.VectorLength(vec), 0.0001f);
    }

    @Test
    public void createVectorWithZeroNumsAndCheckHisVectorResultNeedZero(){
        float[] vec = {0.0f,0.0f,0.0f};
        assertEquals(0.0f,Math3D.VectorLength(vec), 0.0001f);
    }

}