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
        assertEquals(10.2103f, Math3D.VectorLength(vec), 0.0001f);
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

    @Test
    public void checkDotProductWithSomeVectors(){
        float[] x = {1.0f,0.0f,0.0f};
        float[] y = {1.0f,0.0f,0.0f};
        assertEquals(1.0f,Math3D.DotProduct(x,y), 0.0001f);
    }

    @Test
    public void checkDotProductWithPerpendicularSomeVectors(){
        float[] x = {1.0f,0.0f,0.0f};
        float[] y = {0.0f,1.0f,0.0f};
        assertEquals(0.0f,Math3D.DotProduct(x,y), 0.0001f);
    }

    @Test
    public void createVectorsAndCheckThereEqualOrNot(){
        float[] vec1 = {1.0f,2.0f,3.0f};
        float[] vec2 = {1.0f,2.0f,3.0f};
        assertTrue(Math3D.VectorEquals(vec1,vec2));
    }

    @Test
    public void createVectorsAndCheckThereEqualOrNotButThereActuallyNotEqual(){
        float[] vec1 = {1.0f,2.0f,3.0f};
        float[] vec2 = {1.0f,2.0f,5.0f};
        assertFalse(Math3D.VectorEquals(vec1,vec2));
    }

    @Test
    public void createVectorsAndAddALittleDiffrenceBetweenThemAncCheckEqual(){
        float[] vec1 = {1.0f,2.0f,3.0f};
        float[] vec2 = {1.0001f,2.0f,3.0f};
        assertFalse(Math3D.VectorEquals(vec1,vec2));
    }

}