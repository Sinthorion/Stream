package sinthorion.stream;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class IteratorStream<T> implements Stream<T> {

  private static class IteratorStreamImpl<U> extends sinthorion.stream.IteratorStream<U> {

    private final Iterator<U> iter;

    public IteratorStreamImpl(Iterator<U> iterator) {
      this.iter = iterator;
    }

    @Override
    public Iterator<U> iterator() {
      return new Iterator<U>() {
        @Override
        public boolean hasNext() {
          return iter.hasNext();
        }

        @Override
        public U next() {
          return iter.next();
        }
      };
    }
  }

  private static class MapIterator<U, V> implements Iterator<V> {

    private final Iterator<U> source;
    private final Function<? super U, ? extends V> function;

    MapIterator(Iterator<U> source, Function<? super U, ? extends V> function) {
      this.source = source;
      this.function = function;
    }

    @Override
    public boolean hasNext() {
      return source.hasNext();
    }

    @Override
    public V next() {
      return function.apply(source.next());
    }
  }

  private static class FilterIterator<U> implements Iterator<U> {

    private final Iterator<U> source;
    private final Predicate<? super U> filter;

    /** Next needs to be buffed from the source in order to correctly determine hasNext */
    private U next = null;

    /** Invariant: This is always kept up to date with next, as long as next is non-null. */
    private boolean isNextAccepted;

    public FilterIterator(Iterator<U> source, Predicate<? super U> filter) {
      this.source = source;
      this.filter = filter;
    }

    @Override
    public boolean hasNext() {
      if (next != null) return isNextAccepted;

      while (source.hasNext()) {
        next = source.next();
        isNextAccepted = filter.test(next);
        if (isNextAccepted) return true;
      }
      return false;
    }

    @Override
    public U next() {
      if (next != null && isNextAccepted) {
        U result = next;
        next = null;
        return result;
      }
      U result = source.next();
      while (!filter.test(result)) {
        result = source.next();
      }
      return result;
    }
  }

  private static class TakeIterator<U> implements Iterator<U> {

    private final Iterator<U> source;
    private final Predicate<? super U> condition;

    private U next = null;
    private boolean finished = false;

    public TakeIterator(Iterator<U> source, Predicate<? super U> condition) {
      this.source = source;
      this.condition = condition;
    }

    @Override
    public boolean hasNext() {
      if (!finished && next == null) {
        if (!source.hasNext()) return false;
        next = source.next();
        finished = !condition.test(next);
      }
      return !finished;
    }

    @Override
    public U next() {
      if (next == null) {
        next = source.next();
        finished = !condition.test(next);
      }
      if (!finished) {
        U result = next;
        next = null;
        return result;
      }
      throw new NoSuchElementException();
    }
  }

  private static class DropIterator<U> implements Iterator<U> {
    private final Iterator<U> source;
    private final Predicate<? super U> condition;

    private U next;
    boolean started = false;

    public DropIterator(Iterator<U> source, Predicate<? super U> condition) {
      this.source = source;
      this.condition = condition;
    }

    @Override
    public boolean hasNext() {
      if (!started) {
        boolean hasNext = source.hasNext();
        if (!hasNext) {
          return false;
        }
        do {
          this.next = source.next();
          this.started = !condition.test(next);
          hasNext = source.hasNext();
        } while (!started && hasNext);
        if (!started && !hasNext) return false;
      }
      if (next == null) {
        return source.hasNext();
      }
      return true;
    }

    @Override
    public U next() {
      if (!started) {
        boolean hasNext;
        do {
          this.next = source.next();
          this.started = !condition.test(next);
          hasNext = source.hasNext();
        } while (!started && hasNext);
      }
      if (started) {
        U result;
        if (next != null) {
          result = next;
          next = null;
        } else {
          result = source.next();
        }
        return result;
      }
      throw new NoSuchElementException();
    }
  }

  private static class MultiMapIterator<U, V> implements Iterator<V> {

    private final Iterator<U> source;
    private final BiConsumer<? super U, ? super Consumer<V>> mapper;

    public MultiMapIterator(Iterator<U> source, BiConsumer<? super U, ? super Consumer<V>> mapper) {
      this.source = source;
      this.mapper = mapper;
    }

    @Override
    public boolean hasNext() {
      throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public V next() {
      throw new UnsupportedOperationException(); // TODO
    }
  }

  static <U> IteratorStream<U> from(Iterator<U> iterator) {
    return new IteratorStreamImpl<>(iterator);
  }

  @Override
  public <R> Stream<R> map(Function<? super T, ? extends R> function) {
    return new IteratorStreamImpl<>(new MapIterator<>(this.iterator(), function));
  }

  @Override
  public <R> Stream<R> mapMulti(
      BiConsumer<? super T, ? super Consumer<R>> mapper) {
    return new IteratorStreamImpl<>(new MultiMapIterator<>(this.iterator(), mapper));
  }

  @Override
  public Stream<T> filter(Predicate<? super T> filter) {
    return new IteratorStreamImpl<>(new FilterIterator<>(this.iterator(), filter));
  }

  @Override
  public Stream<T> takeWhile(Predicate<? super T> condition) {
    return new IteratorStreamImpl<>(new TakeIterator<>(this.iterator(), condition));
  }

  @Override
  public Stream<T> dropWhile(Predicate<? super T> condition) {
    return new IteratorStreamImpl<>(new DropIterator<>(this.iterator(), condition));
  }

  @Override
  public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator) {
    Iterator<T> iter = this.iterator();
    R result = supplier.get();
    while (iter.hasNext()) {
      accumulator.accept(result, iter.next());
    }
    return result;
  }
}
