package jake2.qcommon.util;

import com.sun.source.tree.AssertTree;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class LibTest {

    @Test
    public void testDecodeNewLine() {
        Assert.assertEquals("hello\nworld", Lib.decodeBackslash("hello\\nworld"));
    }

    @Test
    public void testDecode2Backslash() {
        Assert.assertEquals("hello\\world", Lib.decodeBackslash("hello\\\\world"));
    }

    @Test
    public void testDecodeOther() {
        Assert.assertEquals("hello\\world", Lib.decodeBackslash("hello\\tworld"));
    }

    @Test
    public void testDecodeNormal() {
        Assert.assertEquals("hello, world", Lib.decodeBackslash("hello, world"));
    }

    @Test
    public void checkARandomNumInSomeRange(){
        boolean result = false;
        short roll = (short) (Lib.rand() % 8);
        for (int i = 1; i <= 8; i++){
            if (roll == i){
                result = true;
            }
        }
        assertTrue(result);
    }
}
