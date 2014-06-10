package edu.stanford.nlp.ling.tokensregex.matcher;

import edu.stanford.nlp.util.Generics;
import edu.stanford.nlp.util.StringUtils;

import java.util.*;

/**
 * Map that takes a iterable as key, and maps it to an value.
 *
 * This implementation is not particularly memory efficient, but will have relatively
 *   fast lookup times for sequences where there are many possible keys (e.g. sequences over Strings)
 * Can be used for fairly efficient look up of sequence by prefix
 *
 * @author Angel Chang
 *
 * @param <K, V>
 */
public class TrieMap<K, V> extends AbstractMap<Iterable<K>, V> {
  /**
   * Child tries
   */
  protected Map<K, TrieMap<K, V>> children;

  /**
   * Value at a leaf node (leaf node is indicated by non-null value)
   */
  protected V value;
  // Should we have explicit marking if this element is a leaf node without requiring value?

  public TrieMap() {
  }

  public TrieMap(int initialCapacity) {
    // TODO: initial capacity implementation
  }

  // Trie specific functions
  public TrieMap<K,V> getChildTrie(K key) {
    return (children != null)? children.get(key):null;
  }

  public TrieMap<K,V> getChildTrie(Iterable<K> key) {
    TrieMap<K, V> curTrie = this;
    // go through each element
    for(Object element : key){
      curTrie = (curTrie.children != null)? curTrie.children.get(element):null;
      if(curTrie == null){
        return null;
      }
    }
    return curTrie;
  }

  public V getValue() {
    return value;
  }

  public boolean isLeaf() {
    return value != null;
  }


  public String toFormattedString(){
    List<String> strings = new LinkedList<String>();
    updateTrieStrings(strings, "");
    return StringUtils.join(strings, "\n");
  }

  protected void updateTrieStrings(List<String> strings, String prefix) {
    if (children != null) {
      for (K key:children.keySet()) {
        children.get(key).updateTrieStrings(strings, prefix + " - " + key);
      }
    }
    if (isLeaf()) {
      strings.add( prefix + " -> " + value);
    }
  }

  // Functions to support map interface to lookup using sequence

  @Override
  public int size() {
    int s = 0;
    if (children != null) {
      for (K key:children.keySet()) {
        s += children.get(key).size();
      }
    }
    if (isLeaf()) s++;
    return s;
  }

  @Override
  public boolean isEmpty() {
    return (children == null && !isLeaf());
  }

  @Override
  public boolean containsKey(Object key) {
    return get(key) != null;
  }

  @Override
  public boolean containsValue(Object value) {
    return values().contains(value);
  }

  @Override
  public V get(Object key) {
    if (key instanceof Iterable) {
      return get( (Iterable) key);
    } else if (key instanceof Object[]) {
      return get( Arrays.asList( (Object[]) key) );
    }
    return null;
  }

  public V get(Iterable key) {
    TrieMap<K, V> curTrie = getChildTrie(key);
    return (curTrie != null)? curTrie.value:null;
  }

  public V get(K[] key) {
    return get(Arrays.asList(key));
  }

  @Override
  public V put(Iterable<K> key, V value) {
    if (value == null) throw new IllegalArgumentException("Value cannot be null");
    TrieMap<K, V> curTrie = this;
    // go through each element
    for(K element:key){
      if (curTrie.children == null) {
        curTrie.children = Generics.newConcurrentHashMap();
      }
      TrieMap<K, V> parent = curTrie;
      curTrie = curTrie.children.get(element);
      if(curTrie == null){
        parent.children.put(element, curTrie = new TrieMap<K,V>());
      }
    }
    V oldValue = curTrie.value;
    curTrie.value = value;
    return oldValue;
  }

  public V put(K[] key, V value) {
    return put(Arrays.asList(key), value);
  }

  @Override
  public V remove(Object key) {
    if (key instanceof Iterable) {
      return remove( (Iterable) key );
    }
    return null;
  }

  public V remove(Iterable key) {
    TrieMap<K, V> parent = null;
    TrieMap<K, V> curTrie = this;
    Object lastKey = null;
    // go through each element
    for(Object element : key){
      if (curTrie.children == null) return null;
      lastKey = element;
      parent = curTrie;
      curTrie = curTrie.children.get(element);
      if (curTrie == null){
        return null;
      }
    }
    V v = curTrie.value;
    if (parent != null) {
      parent.children.remove(lastKey);
    } else {
      value = null;
    }
    return v;
  }

  public V remove(K[] key) {
    return remove(Arrays.asList(key));
  }

  @Override
  public void putAll(Map<? extends Iterable<K>, ? extends V> m) {
    for (Iterable<K> k:m.keySet()) {
      put(k, m.get(m));
    }
  }

  @Override
  public void clear() {
    value = null;
    children = null;
  }

  @Override
  public Set<Iterable<K>> keySet() {
    Set<Iterable<K>> keys = new LinkedHashSet<Iterable<K>>();
    updateKeys(keys, new ArrayList<K>());
    return keys;
  }

  protected void updateKeys(Set<Iterable<K>> keys, List<K> prefix) {
    if (children != null) {
      for (K key:children.keySet()) {
        List<K> p = new ArrayList<K>(prefix.size() + 1);
        p.addAll(prefix);
        p.add(key);
        children.get(key).updateKeys(keys, p);
      }
    }
    if (value != null) {
      keys.add(prefix);
    }
  }

  @Override
  public Collection<V> values() {
    List<V> values = new ArrayList<V>();
    updateValues(values);
    return values;
  }

  protected void updateValues(List<V> values) {
    if (children != null) {
      for (K key:children.keySet()) {
        children.get(key).updateValues(values);
      }
    }
    if (value != null) {
      values.add(value);
    }
  }

  @Override
  public Set<Entry<Iterable<K>, V>> entrySet() {
    Set<Entry<Iterable<K>, V>> entries = new LinkedHashSet<Entry<Iterable<K>,V>>();
    updateEntries(entries, new ArrayList<K>());
    return entries;
  }

  protected void updateEntries(Set<Entry<Iterable<K>,V>> entries, final List<K> prefix) {
    if (children != null) {
      for (K key:children.keySet()) {
        List<K> p = new ArrayList<K>(prefix.size() + 1);
        p.addAll(prefix);
        p.add(key);
        children.get(key).updateEntries(entries, p);
      }
    }
    if (value != null) {
      entries.add(new Map.Entry() {
        @Override
        public Object getKey() {
          return prefix;
        }

        @Override
        public Object getValue() {
          return value;
        }

        @Override
        public Object setValue(Object value) {
          throw new UnsupportedOperationException();
        }
      });
    }
  }
}