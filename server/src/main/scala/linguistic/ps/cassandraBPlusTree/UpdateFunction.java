package linguistic.ps.cassandraBPlusTree;

import java.util.function.BiFunction;

import com.google.common.base.Function;
/**
 * An interface defining a function to be applied to both the object we are replacing in a BTree and
 * the object that is intended to replace it, returning the object to actually replace it.
 */
public interface UpdateFunction<K, V> extends Function<K, V>
{
  /**
   * @param replacing the value in the original tree we have matched
   * @param update the value in the updating collection that matched
   * @return the value to insert into the new tree
   */
  V apply(V replacing, K update);

  /**
   * @return true if we should fail the update
   */
  boolean abortEarly();

  /**
   * @param heapSize extra heap space allocated (over previous tree)
   */
  void allocated(long heapSize);

  public static final class Simple<V> implements UpdateFunction<V, V>
  {
    private final BiFunction<V, V, V> wrapped;
    public Simple(BiFunction<V, V, V> wrapped)
    {
      this.wrapped = wrapped;
    }

    public V apply(V v) { return v; }
    public V apply(V replacing, V update) { return wrapped.apply(replacing, update); }
    public boolean abortEarly() { return false; }
    public void allocated(long heapSize) { }

    public static <V> Simple<V> of(BiFunction<V, V, V> f)
    {
      return new Simple<>(f);
    }
  }

  static final Simple<Object> noOp = Simple.of((a, b) -> a);

  public static <K> UpdateFunction<K, K> noOp()
  {
    return (UpdateFunction<K, K>) noOp;
  }
}