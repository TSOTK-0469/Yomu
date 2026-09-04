package cn.yomu.reader.data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class NaturalOrderTest {
    @Test
    public void sortsComicPagesNumerically() {
        List<String> actual = new ArrayList<>(
                Arrays.asList("10.jpg", "2.jpg", "001.jpg", "01.jpg", "cover.jpg"));
        actual.sort(NaturalOrder.INSTANCE);
        assertEquals(
                Arrays.asList("01.jpg", "001.jpg", "2.jpg", "10.jpg", "cover.jpg"),
                actual);
    }

    @Test
    public void ignoresCaseForAlphabeticSections() {
        List<String> actual = new ArrayList<>(
                Arrays.asList("page11.png", "page3.png", "Page2.png"));
        actual.sort(NaturalOrder.INSTANCE);
        assertEquals(Arrays.asList("Page2.png", "page3.png", "page11.png"), actual);
    }
}
