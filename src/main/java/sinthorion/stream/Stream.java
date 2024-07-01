package sinthorion.stream;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface Stream<T> extends Iterable<T> {
  <R> Stream<R> map(Function<? super T, ? extends R> function);

  default <R> Stream<R> mapMulti(BiConsumer<? super T, ? super Consumer<R>> mapper) {
    throw new UnsupportedOperationException(); // TODO
  }

  Stream<T> filter(Predicate<? super T> predicate);

  Stream<T> takeWhile(Predicate<? super T> condition);

  Stream<T> dropWhile(Predicate<? super T> condition);

  <R> R collect(Supplier<R> supplier,
      BiConsumer<R, ? super T> accumulator);

}
