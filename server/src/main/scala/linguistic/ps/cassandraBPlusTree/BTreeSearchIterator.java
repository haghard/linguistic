package linguistic.ps.cassandraBPlusTree;

import java.util.Iterator;
import java.util.Comparator;
import java.util.NoSuchElementException;

import static linguistic.ps.cassandraBPlusTree.BTree.size;

public class BTreeSearchIterator<K, V> extends TreeCursor<K> implements IndexedSearchIterator<K, V>, Iterator<V>
{
  private final boolean forwards;
  private int index;
  private byte state;
  private final int lowerBound, upperBound; // inclusive

  private static final int MIDDLE = 0; // only "exists" as an absence of other states
  private static final int ON_ITEM = 1; // may only co-exist with LAST (or MIDDLE, which is 0)
  private static final int BEFORE_FIRST = 2; // may not coexist with any other state
  private static final int LAST = 4; // may co-exist with ON_ITEM, in which case we are also at END
  private static final int END = 5; // equal to LAST | ON_ITEM

  public BTreeSearchIterator(Object[] btree, Comparator<? super K> comparator, BTree.Dir dir)
  {
    this(btree, comparator, dir, 0, size(btree)-1);
  }

  BTreeSearchIterator(Object[] btree, Comparator<? super K> comparator, BTree.Dir dir, int lowerBound, int upperBound)
  {
    super(comparator, btree);
    this.forwards = dir == BTree.Dir.ASC;
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
    rewind();
  }

  /**
   * @return 0 if we are on the last item, 1 if we are past the last item, and -1 if we are before it
   */
  private int compareToLast(int idx)
  {
    return forwards ? idx - upperBound : lowerBound - idx;
  }

  private int compareToFirst(int idx)
  {
    return forwards ? idx - lowerBound : upperBound - idx;
  }

  public boolean hasNext()
  {
    return state != END;
  }

  public V next()
  {
    switch (state)
    {
      case ON_ITEM:
        if (compareToLast(index = moveOne(forwards)) >= 0)
          state = END;
        break;
      case BEFORE_FIRST:
        seekTo(index = forwards ? lowerBound : upperBound);
        state = (byte) (upperBound == lowerBound ? LAST : MIDDLE);
      case LAST:
      case MIDDLE:
        state |= ON_ITEM;
        break;
      default:
        throw new NoSuchElementException();
    }

    return current();
  }

  public V next(K target)
  {
    if (!hasNext())
      return null;

    int state = this.state;
    boolean found = seekTo(target, forwards, (state & (ON_ITEM | BEFORE_FIRST)) != 0);
    int index = cur.globalIndex();

    V next = null;
    if (state == BEFORE_FIRST && compareToFirst(index) < 0)
      return null;

    int compareToLast = compareToLast(index);
    if ((compareToLast <= 0))
    {
      state = compareToLast < 0 ? MIDDLE : LAST;
      if (found)
      {
        state |= ON_ITEM;
        next = (V) currentValue();
      }
    }
    else state = END;

    this.state = (byte) state;
    this.index = index;
    return next;
  }

  /**
   * Reset this Iterator to its starting position
   */
  public void rewind()
  {
    if (upperBound < lowerBound)
    {
      state = (byte) END;
    }
    else
    {
      // we don't move into the tree until the first request is made, so we know where to go
      reset(forwards);
      state = (byte) BEFORE_FIRST;
    }
  }

  private void checkOnItem()
  {
    if ((state & ON_ITEM) != ON_ITEM)
      throw new NoSuchElementException();
  }

  public V current()
  {
    checkOnItem();
    return (V) currentValue();
  }

  public int indexOfCurrent()
  {
    checkOnItem();
    return compareToFirst(index);
  }
}