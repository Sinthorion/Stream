package sinthorion.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Timeout(3)
public class IteratorStreamTest {

  @Test
  public void testMap() {
    Stream<Integer> stream = IteratorStream.from(Arrays.asList(1, 2, 3, 4, 5).iterator());
    Integer[] result = stream.map(x -> x * 2).collect(ArrayList::new, ArrayList::add).toArray(new Integer[0]);
    Assertions.assertArrayEquals(new Integer[] {2, 4, 6, 8, 10}, result);
  }

  @Test
  public void testMapMultipleTakeNext() {
    Stream<Integer> stream = IteratorStream.from(Arrays.asList(1, 2, 3, 4, 5).iterator())
        .map(x -> x);
    Iterator<Integer> iter = stream.iterator();
    Assertions.assertDoesNotThrow(iter::next);
    Assertions.assertDoesNotThrow(iter::next);
    Assertions.assertDoesNotThrow(iter::next);
    Assertions.assertDoesNotThrow(iter::next);
    Assertions.assertDoesNotThrow(iter::next);
  }

  @Test
  public void testFilter() {
    Stream<Integer> stream = IteratorStream.from(Arrays.asList(1, 2, 3, 4, 5).iterator());
    Integer[] result = stream.filter(x -> x % 2 == 0).collect(ArrayList::new, ArrayList::add).toArray(new Integer[0]);
    Assertions.assertArrayEquals(new Integer[] {2, 4}, result);
  }

  @Test
  public void testFilterMultipleTakeNext() {
    Stream<Integer> stream = IteratorStream.from(Arrays.asList(1, 2, 3, 4, 5).iterator())
        .filter(x -> true);
    Iterator<Integer> iter = stream.iterator();
    Assertions.assertDoesNotThrow(iter::next);
    Assertions.assertDoesNotThrow(iter::next);
    Assertions.assertDoesNotThrow(iter::next);
    Assertions.assertDoesNotThrow(iter::next);
    Assertions.assertDoesNotThrow(iter::next);
  }

  @Test
  public void testTakeWhile() {
    Stream<Integer> stream = IteratorStream.from(Arrays.asList(1, 2, 3, 4, 5).iterator());
    Integer[] result = stream.takeWhile(x -> x < 4).collect(ArrayList::new, ArrayList::add).toArray(new Integer[0]);
    Assertions.assertArrayEquals(new Integer[] {1, 2, 3}, result);
  }

  @Test
  public void testTakeWhileMultipleTakeNext() {
    Stream<Integer> stream = IteratorStream.from(Arrays.asList(1, 2, 3, 4, 5).iterator())
        .takeWhile(x -> true);
    Iterator<Integer> iter = stream.iterator();
    Assertions.assertDoesNotThrow(iter::next);
    Assertions.assertDoesNotThrow(iter::next);
    Assertions.assertDoesNotThrow(iter::next);
    Assertions.assertDoesNotThrow(iter::next);
    Assertions.assertDoesNotThrow(iter::next);
  }

  @Test
  public void testDropWhile() {
    Stream<Integer> stream = IteratorStream.from(Arrays.asList(1, 2, 3, 4, 5).iterator());
    Integer[] result = stream.dropWhile(x -> x < 4).collect(ArrayList::new, ArrayList::add).toArray(new Integer[0]);
    Assertions.assertArrayEquals(new Integer[] {4, 5}, result);
  }

  @Test
  public void testDropWhileMultipleTakeNext() {
    Stream<Integer> stream = IteratorStream.from(Arrays.asList(1, 2, 3, 4, 5).iterator())
        .dropWhile(x -> false);
    Iterator<Integer> iter = stream.iterator();
    Assertions.assertDoesNotThrow(iter::next);
    Assertions.assertDoesNotThrow(iter::next);
    Assertions.assertDoesNotThrow(iter::next);
    Assertions.assertDoesNotThrow(iter::next);
    Assertions.assertDoesNotThrow(iter::next);
  }

  @Test
  public void testCollect() {
    Stream<Integer> stream = IteratorStream.from(Arrays.asList(1, 2, 3, 4, 5).iterator());
    AtomicInteger result = stream.collect(AtomicInteger::new, AtomicInteger::addAndGet);
    Assertions.assertEquals(15, result.get());
  }

  @Test
  public void testComplex() {
    Stream<Integer> stream = IteratorStream.from(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).iterator());
    AtomicInteger result = stream
        .filter(x -> x % 2 == 0)
        .takeWhile(x -> x != 8)
        .map(x -> x + 1)
        .dropWhile(x -> x < 5)
        .collect(AtomicInteger::new, AtomicInteger::addAndGet);
    Assertions.assertEquals(12, result.get());
  }

  @Test
  public void testEmpty() {
    Stream<Integer> stream = IteratorStream.from(Collections.emptyListIterator());
    Integer result = stream.collect(() -> 0, (a, b) -> a += b);
    Assertions.assertEquals(0, result);
  }

  @Test
  public void testInfinite() {
    Iterator<Integer> infiniteCounter = new Iterator<>() {
      private int i = 0;
      @Override
      public boolean hasNext() {
        return true;
      }

      @Override
      public Integer next() {
        return i++;
      }
    };
    Stream<Integer> stream = IteratorStream.from(infiniteCounter)
        .map(x -> x + 10)
        .filter(x -> x % 2 == 0)
        .dropWhile(x -> x < 5)
        .takeWhile(x -> x < 20);
    List<Integer> result = stream.takeWhile(x -> x < 20).collect(ArrayList::new, ArrayList::add);
    Assertions.assertArrayEquals(new Integer[] {10, 12, 14, 16, 18}, result.toArray(new Integer[0]));
  }

  @ParameterizedTest
  @MethodSource("iteratorStreamProvider")
  public void testMultipleTakeNext(Stream<Integer> stream, int size) {
    Iterator<Integer> iter = stream.iterator();
    for (int i = 0; i < size; i++) {
      Assertions.assertDoesNotThrow(iter::next);
    }
    Assertions.assertThrows(java.util.NoSuchElementException.class, iter::next);
  }

  @ParameterizedTest
  @MethodSource("iteratorStreamProvider")
  public void testMultipleHasNext(Stream<Integer> stream, int size) {
    Iterator<Integer> iter = stream.iterator();
    if (size == 0) {
      Assertions.assertFalse(iter.hasNext());
    } else {
      // hasNext is indempotent, so the exact number doesn't matter
      for (int i = 0; i < 10; i++) {
        Assertions.assertTrue(iter.hasNext());
      }
    }
  }

  public static java.util.stream.Stream<Arguments> iteratorStreamProvider() {
    List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
    return java.util.stream.Stream.of(
        argumentBuilder("Basic (NOOP)", IteratorStream.from(list.iterator()), list.size()),
        argumentBuilder("Map (NOOP)", IteratorStream.from(list.iterator()).map(x -> x), list.size()),
        argumentBuilder("Filter (accept all)", IteratorStream.from(list.iterator()).filter(x -> true), list.size()),
        argumentBuilder("Filter (reject all)", IteratorStream.from(list.iterator()).filter(x -> false), 0),
        argumentBuilder("TakeWhile (all)", IteratorStream.from(list.iterator()).takeWhile(x -> true), list.size()),
        argumentBuilder("TakeWhile (none)", IteratorStream.from(list.iterator()).takeWhile(x -> false), 0),
        argumentBuilder("DropWhile (none)", IteratorStream.from(list.iterator()).dropWhile(x -> false), list.size()),
        argumentBuilder("DropWhile (all)", IteratorStream.from(list.iterator()).dropWhile(x -> true), 0));

  }

  private static Arguments argumentBuilder(String name, Stream<?> stream, int size) {
    return Arguments.of(Named.of(name, stream), Named.of("(" + size + " elements)", size));
  }
}
