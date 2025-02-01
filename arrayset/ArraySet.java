package info.kgeorgiy.ja.kupriyanov.arrayset;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;


public class ArraySet<E> extends AbstractSet<E> implements SortedSet<E> {
    private final List<E> list;
    private Comparator<? super E> comp;


    private ArraySet(Comparator<? super E> comp) {

        this.comp = comp;
        this.list = new ArrayList<>();
    }

    public ArraySet() {
        this.list = new ArrayList<>();
    }

    public ArraySet(Collection<? extends E> collection) {
        this(collection, null);
    }

    public ArraySet(Collection<? extends E> collection, Comparator<? super E> comp) {
        if (collection == null) {
            throw new IllegalArgumentException("Collection cannot be null.");
        }
        if (comp == null) {
            this.comp = null;
            this.list = new ArrayList<>(new TreeSet<>(collection));
        } else {
            this.comp = comp;
            TreeSet<E> sortedSet = new TreeSet<>(comp);
            sortedSet.addAll(collection);
            this.list = new ArrayList<>(sortedSet);
        }
    }


    private ArraySet(List<E> collection, Comparator<? super E> comp) {
        this.list = collection;
        this.comp = comp;
    }


    public int size() {
        return list.size();
    }


    @Override
    public Iterator<E> iterator() {
        return this.list.iterator();
    }

    @Override
    public Comparator<? super E> comparator() {
        return comp;
    }

    public SortedSet<E> headSet(E toElement) {
        int last = Collections.binarySearch(list, toElement, comp);
        if (!list.isEmpty()) {
            return sublist(0, last);
        } else {
            return new ArraySet<>(comp);
        }
    }
    public SortedSet<E> tailSet(E fromElement) {
        int first = Collections.binarySearch(list, fromElement, comp);
        if (!list.isEmpty()) {
            return sublist(first, list.size());
        } else {
            return new ArraySet<>(comp);
        }
    }

    private SortedSet<E> sublist(int first, int last) {
        if (first < 0) {
            first *= -1;
            first--;
        }
        if (last < 0) {
            last *= -1;
            last--;
        }
        if (first < last) {
            return new ArraySet<>(list.subList(first, last), comp);
        } else {
            return new ArraySet<>(comp);
        }
    }

    public SortedSet<E> subSet(E fromElement, E toElement) {//испарвил метод
        if (fromElement == null || toElement == null) {
            throw new NullPointerException("fromElement or toElement is null");
        }
        if (comp != null && comp.compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("fromKey > toKey");
        }
        if (comp == null) {
            if (!(fromElement instanceof Comparable)) {
                throw new IllegalArgumentException("fromElement is not Comparable");
            }
            @SuppressWarnings("unchecked")
            Comparable<? super E> comparableFromElement = (Comparable<? super E>) fromElement;
            if (comparableFromElement.compareTo(toElement) > 0) {
                throw new IllegalArgumentException("fromKey > toKey");
            }
        }
        int start = Collections.binarySearch(list, fromElement, comp);
        int end = Collections.binarySearch(list, toElement, comp);
        return sublist(start, end);
    }


    @Override
    public boolean contains(Object item) { //испарвил предупреждение
        // :NOTE: incorrect contains
        if (item == null || list.isEmpty() || !item.getClass().isAssignableFrom(list.get(0).getClass())) {
            return false;
        }
        @SuppressWarnings("unchecked")
        E castedItem = (E) item;
        return Collections.binarySearch(list, castedItem, comp) >= 0;
    }



    @Override
    public E first() {
        return getElement(0);
    }

    @Override
    public E last() {
        return getElement(list.size() - 1);
    }

    private E getElement(int index) { // исправил копипасту
        if (!list.isEmpty() && index >= 0 && index < list.size()) {
            return list.get(index);
        } else {
            throw new NoSuchElementException();
        }
    }
}



