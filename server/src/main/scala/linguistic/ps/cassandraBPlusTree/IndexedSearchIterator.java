package linguistic.ps.cassandraBPlusTree;

public interface IndexedSearchIterator<K, V> //extends SearchIterator<K, V>
{
  /**
   * @return true if iterator has any elements left, false otherwise
   */
  public boolean hasNext();

  /**
   * @return the value just recently returned by next()
   * @throws java.util.NoSuchElementException if next() returned null
   */
  public V current();

  /**
   * @return the index of the value returned by current(), and just returned by next()
   * @throws java.util.NoSuchElementException if next() returned null
   */
  public int indexOfCurrent();

  public V next(K key);
}