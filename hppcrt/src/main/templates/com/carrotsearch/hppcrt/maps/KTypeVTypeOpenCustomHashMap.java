package com.carrotsearch.hppcrt.maps;

import com.carrotsearch.hppcrt.*;
import com.carrotsearch.hppcrt.cursors.*;
import com.carrotsearch.hppcrt.predicates.*;
import com.carrotsearch.hppcrt.procedures.*;
import com.carrotsearch.hppcrt.strategies.*;
import com.carrotsearch.hppcrt.hash.*;

/*! #import("com/carrotsearch/hppcrt/Intrinsics.java") !*/
/*! ${TemplateOptions.doNotGenerateKType("BOOLEAN")} !*/
/*! #set( $ROBIN_HOOD_FOR_ALL = true) !*/
/*! #set( $DEBUG = false) !*/
//If RH is defined, RobinHood Hashing is in effect :
/*! #set( $RH = $ROBIN_HOOD_FOR_ALL) !*/

/**
 * A hash map of <code>KType</code> to <code>VType</code>, implemented using open
 * addressing with linear probing for collision resolution.
 * <p>
 * The difference with {@link KTypeVTypeOpenHashMap} is that it uses a
 * {@link KTypeHashingStrategy} to compare objects externally instead of using
 * the built-in hashCode() /  equals(). In particular, the management of <code>null</code>
 * keys is up to the {@link KTypeHashingStrategy} implementation.
 * The internal buffers of this implementation ({@link #keys}, {@link #values})
 * are always allocated to the nearest size that is a power of two. When
 * the capacity exceeds the given load factor, the buffer size is doubled.
 * </p>
 * 
 * <p><b>Important note.</b> The implementation uses power-of-two tables and linear
 * probing, which may cause poor performance (many collisions) if hash values are
 * not properly distributed. Therefore, it is up to the {@link KTypeHashingStrategy} to
 * assure good performance.</p>
 * 
#if ($TemplateOptions.KTypeGeneric)
 * <p>This implementation supports <code>null</code> keys. In addition, objects passed to the {@link KTypeHashingStrategy} are guaranteed to be not-<code>null</code>. </p>
#end
#if ($TemplateOptions.VTypeGeneric)
 * <p>This implementation supports <code>null</code> values.</p>
#end
 * 
 * @author This code is inspired by the original <a
 *         href="http://labs.carrotsearch.com/hppc.html">HPPC</a> and also by the <a
 *         href="http://fastutil.di.unimi.it/">fastutil</a> project.
 *
#if ($RH)
 *   <p> Robin-Hood hashing algorithm is also used to minimize variance
 *  in insertion and search-related operations, for an all-around smother operation at the cost
 *  of smaller peak performance:</p>
 *  <p> - Pedro Celis (1986) for the original Robin-Hood hashing paper, </p>
 *  <p> - <a href="cliff@leaninto.it">MoonPolySoft/Cliff Moon</a> for the initial Robin-hood on HPPC implementation,</p>
 *  <p> - <a href="vsonnier@gmail.com" >Vincent Sonnier</a> for the present implementation using cached hashes.</p>
#end
 *
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeVTypeOpenCustomHashMap<KType, VType>
implements KTypeVTypeMap<KType, VType>, Cloneable
{
    protected VType defaultValue = Intrinsics.<VType> defaultVTypeValue();

    /**
     * Hash-indexed array holding all keys.
    #if ($TemplateOptions.KTypeGeneric)
     * <p><strong>Important!</strong>
     * The actual value in this field is always an instance of <code>Object[]</code>.
     * Be warned that <code>javac</code> emits additional casts when <code>keys</code>
     * are directly accessed; <strong>these casts
     * may result in exceptions at runtime</strong>. A workaround is to cast directly to
     * <code>Object[]</code> before accessing the buffer's elements (although it is highly
     * recommended to use a {@link #iterator()} instead.
     * </pre>
    #end
     * <p>
     * Direct map iteration: iterate  {keys[i], values[i]} for i in [0; keys.length[ where keys[i] != 0/null, then also
     * {0/null, {@link #allocatedDefaultKeyValue} } is in the map if {@link #allocatedDefaultKey} = true.
     * </p>
     */
    public KType[] keys;

    /**
     * Hash-indexed array holding all values associated to the keys.
     * stored in {@link #keys}.
    #if ($TemplateOptions.KTypeGeneric)
     * <p><strong>Important!</strong>
     * The actual value in this field is always an instance of <code>Object[]</code>.
     * Be warned that <code>javac</code> emits additional casts when <code>values</code>
     * are directly accessed; <strong>these casts
     * may result in exceptions at runtime</strong>. A workaround is to cast directly to
     * <code>Object[]</code> before accessing the buffer's elements (although it is highly
     * recommended to use a {@link #iterator()} instead.
     * </pre>
    #end
     * 
     */
    public VType[] values;

    /*! #if ($RH) !*/
    /**
     * #if ($RH)
     * Caches the hash value = hash(strategy, keys[i]) & mask, if keys[i] != 0/null,
     * for every index i.
     * #end
     * @see #assigned
     */
    /*! #end !*/
    /*! #if ($RH) !*/
    protected int[] hash_cache;
    /*! #end !*/

    /**
     *True if key = 0/null is in the map.
     */
    public boolean allocatedDefaultKey = false;

    /**
     * if allocatedDefaultKey = true, contains the associated V to the key = 0/null
     */
    public VType allocatedDefaultKeyValue;

    /**
     * Cached number of assigned slots in {@link #keys}.
     */
    protected int assigned;

    /**
     * The load factor for this map (fraction of allocated slots
     * before the buffers must be rehashed or reallocated).
     */
    protected final double loadFactor;

    /**
     * Resize buffers when {@link #keys} hits this value.
     */
    protected int resizeAt;

    /**
     * Per-instance, per-allocation size perturbation
     * introduced in rehashing to create a unique key distribution.
     */
    private final int perturbation = HashContainers.computeUniqueIdentifier(this);

    /**
     * Custom hashing strategy :
     * comparisons and hash codes of keys will be computed
     * with the strategy methods instead of the native Object equals() and hashCode() methods.
     */
    protected final KTypeHashingStrategy<? super KType> hashStrategy;

    /**
     * Creates a hash map with the default capacity of {@value #DEFAULT_CAPACITY},
     * load factor of {@value #DEFAULT_LOAD_FACTOR}, using the hashStrategy as {@link KTypeHashingStrategy}
     * 
     * <p>See class notes about hash distribution importance.</p>
     */
    public KTypeVTypeOpenCustomHashMap(final KTypeHashingStrategy<? super KType> hashStrategy)
    {
        this(Containers.DEFAULT_EXPECTED_ELEMENTS, hashStrategy);
    }

    /**
     * Creates a hash map with the given initial capacity, default load factor of
     * {@value #DEFAULT_LOAD_FACTOR}, using the hashStrategy as {@link KTypeHashingStrategy}
     * 
     * @param initialCapacity Initial capacity (greater than zero and automatically
     *            rounded to the next power of two).
     */
    public KTypeVTypeOpenCustomHashMap(final int initialCapacity, final KTypeHashingStrategy<? super KType> hashStrategy)
    {
        this(initialCapacity, HashContainers.DEFAULT_LOAD_FACTOR, hashStrategy);
    }

    /**
     * Creates a hash map with the given initial capacity,
     * load factor, using the hashStrategy as {@link KTypeHashingStrategy}
     *
     * @param loadFactor The load factor (greater than zero and smaller than 1).
     * 
     * 
     */
    public KTypeVTypeOpenCustomHashMap(final int initialCapacity, final double loadFactor, final KTypeHashingStrategy<? super KType> hashStrategy)
    {
        //only accept not-null strategies.
        if (hashStrategy != null)
        {
            this.hashStrategy = hashStrategy;
        }
        else {

            throw new IllegalArgumentException("KTypeVTypeOpenCustomHashMap() cannot have a null hashStrategy !");
        }

        this.loadFactor = loadFactor;
        //take into account of the load factor to guarantee no reallocations before reaching  initialCapacity.
        allocateBuffers(HashContainers.minBufferSize(initialCapacity, loadFactor));
    }

    /**
     * Creates a hash map from all key-value pairs of another container.
     */
    public KTypeVTypeOpenCustomHashMap(final KTypeVTypeAssociativeContainer<KType, VType> container, final KTypeHashingStrategy<? super KType> hashStrategy)
    {
        this(container.size(), hashStrategy);
        putAll(container);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VType put(KType key, VType value)
    {
        if (Intrinsics.isEmptyKey(key)) {

            if (this.allocatedDefaultKey) {

                final VType previousValue = this.allocatedDefaultKeyValue;
                this.allocatedDefaultKeyValue = value;

                return previousValue;
            }

            this.allocatedDefaultKeyValue = value;
            this.allocatedDefaultKey = true;

            return this.defaultValue;
        }

        final int mask = this.keys.length - 1;

        final KTypeHashingStrategy<? super KType> strategy = this.hashStrategy;

        final KType[] keys = this.keys;
        final VType[] values = this.values;

        //copied straight from  fastutil "fast-path"
        int slot;
        KType curr;

        //1.1 The rehashed key slot is occupied...
        if (!Intrinsics.isEmptyKey((curr = keys[slot = (REHASH(strategy, key) & mask)]))) {

            //1.2 the occupied place is indeed key, so only updates the value and nothing else.
            if (strategy.equals(curr, key)) {

                final VType oldValue = this.values[slot];
                values[slot] = value;

                return oldValue;
            }

            //1.3 key is colliding, manage below :
        }
        else if (this.assigned < this.resizeAt) {

            //1.4 key is not colliding, without resize, so insert, return defaultValue.
            keys[slot] = key;
            values[slot] = value;
            this.assigned++;

            /*! #if ($RH) !*/
            this.hash_cache[slot] = slot;
            /*! #end !*/

            return this.defaultValue;
        }

        /*! #if ($RH) !*/
        final int[] cached = this.hash_cache;

        KType tmpKey;
        VType tmpValue;
        int tmpAllocated;
        int initial_slot = slot;
        int dist = 0;
        int existing_distance = 0;
        /*! #end !*/

        //2. Slow path, find a place somewhere down there...
        while (is_allocated(slot, keys))
        {
            if (strategy.equals(key, keys[slot]))
            {
                final VType oldValue = values[slot];
                values[slot] = value;

                return oldValue;
            }

            /*! #if ($RH) !*/
            //re-shuffle keys to minimize variance
            existing_distance = probe_distance(slot, cached);

            if (dist > existing_distance)
            {
                //swap current (key, value, initial_slot) with slot places
                tmpKey = keys[slot];
                keys[slot] = key;
                key = tmpKey;

                tmpAllocated = cached[slot];
                cached[slot] = initial_slot;
                initial_slot = tmpAllocated;

                tmpValue = values[slot];
                values[slot] = value;
                value = tmpValue;

                /*! #if($DEBUG) !*/
                //Check invariants

                assert cached[slot] == (REHASH(strategy, keys[slot]) & mask);
                assert initial_slot == (REHASH(strategy, key) & mask);
                /*! #end !*/

                dist = existing_distance;
            }
            /*! #end !*/

            slot = (slot + 1) & mask;

            /*! #if ($RH) !*/
            dist++;
            /*! #end !*/
        }

        // Check if we need to grow. If so, reallocate new data, fill in the last element
        // and rehash.
        if (this.assigned == this.resizeAt)
        {
            expandAndPut(key, value, slot);
        }
        else
        {
            this.assigned++;
            /*! #if ($RH) !*/
            cached[slot] = initial_slot;
            /*! #end !*/

            keys[slot] = key;
            values[slot] = value;

            /*! #if ($RH) !*/
            /*! #if($DEBUG) !*/
            //Check invariants
            assert cached[slot] == (REHASH(strategy, keys[slot]) & mask);

            /*! #end !*/
            /*! #end !*/
        }
        return this.defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int putAll(final KTypeVTypeAssociativeContainer<? extends KType, ? extends VType> container)
    {
        return putAll((Iterable<? extends KTypeVTypeCursor<? extends KType, ? extends VType>>) container);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int putAll(final Iterable<? extends KTypeVTypeCursor<? extends KType, ? extends VType>> iterable)
    {
        final int count = this.size();

        for (final KTypeVTypeCursor<? extends KType, ? extends VType> c : iterable)
        {
            put(c.key, c.value);
        }
        return this.size() - count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean putIfAbsent(final KType key, final VType value)
    {
        if (!containsKey(key))
        {
            put(key, value);
            return true;
        }
        return false;
    }

    /*! #if ($TemplateOptions.VTypeNumeric) !*/
    /**
     * <a href="http://trove4j.sourceforge.net">Trove</a>-inspired API method. An equivalent
     * of the following code:
     * <pre>
     *  if (containsKey(key))
     *  {
     *      VType v = (VType) (lget() + additionValue);
     *      lset(v);
     *      return v;
     *  }
     *  else
     *  {
     *     put(key, putValue);
     *     return putValue;
     *  }
     * </pre>
     * 
     * @param key The key of the value to adjust.
     * @param putValue The value to put if <code>key</code> does not exist.
     * @param additionValue The value to add to the existing value if <code>key</code> exists.
     * @return Returns the current value associated with <code>key</code> (after changes).
     */
    /*! #end !*/
    /*! #if ($TemplateOptions.VTypeNumeric)
    @Override
    public VType putOrAdd(KType key, VType putValue, VType additionValue)
    {
        if (Intrinsics.isEmptyKey(key)) {

            if (this.allocatedDefaultKey) {

                this.allocatedDefaultKeyValue += additionValue;

                return this.allocatedDefaultKeyValue;
            }

            this.allocatedDefaultKeyValue = putValue;

            this.allocatedDefaultKey = true;

            return putValue;
        }

         final int mask = this.keys.length - 1;

        final KType[] keys = this.keys;
        final VType[] values = this.values;

        final KTypeHashingStrategy<? super KType> strategy = this.hashStrategy;

         VType value =  putValue;

         //copied straight from  fastutil "fast-path"
        int slot;
        KType curr;

        //1.1 The rehashed key slot is occupied...
        if (!Intrinsics.isEmptyKey(curr = keys[slot = REHASH(strategy, key) & mask])) {

            //1.2 the occupied place is indeed key, so only increments the value and nothing else.
            if (strategy.equals(curr, key)) {

                values[slot] += additionValue;
                return values[slot];
            }

            //1.3 key is colliding, manage below :
        }
        else if (this.assigned < this.resizeAt) {

            //1.4 key is not colliding, without resize, so insert, return defaultValue.
            keys[slot] = key;
            values[slot] = value;
            this.assigned++;

            #if ($RH)
            this.hash_cache[slot] = slot;
            #end

            return putValue;
        }

        #if ($RH)
        final int[] cached = this.hash_cache;
        KType tmpKey;
        VType tmpValue;
        int tmpAllocated;
        int initial_slot = slot;
        int dist = 0;
        int existing_distance = 0;
        #end


        while (is_allocated(slot, keys))
        {
            #if ($RH)
            existing_distance = probe_distance(slot, cached);
            #end

            if (strategy.equals(key, keys[slot]))
            {
                values[slot] += additionValue;
                return values[slot];
            }
            #if ($RH)
            else if (dist > existing_distance)
            {
                //swap current (key, value, initial_slot) with slot places
                tmpKey = keys[slot];
                keys[slot] = key;
                key =  tmpKey;

                tmpValue = values[slot];
                values[slot] = value;
                value =  tmpValue;

    			tmpAllocated = cached[slot];
    			cached[slot] = initial_slot;
                initial_slot = tmpAllocated;

                dist = existing_distance;
            }
            #end

            slot = (slot + 1) & mask;

            #if ($RH)
            dist++;
            #end
        }

        if (assigned == resizeAt) {
            expandAndPut(key, value, slot);
        } else {

            assigned++;
            #if ($RH)
            cached[slot] = initial_slot;
            #end

            keys[slot] = key;
            values[slot] = value;
        }
        return putValue;
    }
    #end !*/

    /*! #if ($TemplateOptions.VTypeNumeric) !*/
    /**
     * An equivalent of calling
     * <pre>
     *  if (containsKey(key))
     *  {
     *      VType v = (VType) (lget() + additionValue);
     *      lset(v);
     *      return v;
     *  }
     *  else
     *  {
     *     put(key, additionValue);
     *     return additionValue;
     *  }
     * </pre>
     * 
     * @param key The key of the value to adjust.
     * @param additionValue The value to put or add to the existing value if <code>key</code> exists.
     * @return Returns the current value associated with <code>key</code> (after changes).
     */
    /*! #end !*/
    /*! #if ($TemplateOptions.VTypeNumeric)
    @Override
    public VType addTo(KType key, VType additionValue)
    {
        return putOrAdd(key, additionValue, additionValue);
    }
    #end !*/

    /**
     * Expand the internal storage buffers (capacity) and rehash.
     */
    private void expandAndPut(final KType pendingKey, final VType pendingValue, final int freeSlot)
    {
        assert this.assigned == this.resizeAt;

        //default sentinel value is never in the keys[] array, so never trigger reallocs
        assert !Intrinsics.isEmptyKey(pendingKey);

        // Try to allocate new buffers first. If we OOM, it'll be now without
        // leaving the data structure in an inconsistent state.
        final KType[] oldKeys = this.keys;
        final VType[] oldValues = this.values;

        allocateBuffers(HashContainers.nextBufferSize(this.keys.length, this.assigned, this.loadFactor));

        // We have succeeded at allocating new data so insert the pending key/value at
        // the free slot in the old arrays before rehashing.

        this.assigned++;

        oldKeys[freeSlot] = pendingKey;
        oldValues[freeSlot] = pendingValue;

        //for inserts
        final int mask = this.keys.length - 1;

        final KTypeHashingStrategy<? super KType> strategy = this.hashStrategy;

        KType key = Intrinsics.<KType> defaultKTypeValue();
        VType value = Intrinsics.<VType> defaultVTypeValue();

        int slot = -1;

        final KType[] keys = this.keys;
        final VType[] values = this.values;

        /*! #if ($RH) !*/
        final int[] cached = this.hash_cache;
        /*! #end !*/

        /*! #if ($RH) !*/
        KType tmpKey = Intrinsics.<KType> defaultKTypeValue();
        VType tmpValue = Intrinsics.<VType> defaultVTypeValue();
        int tmpAllocated = -1;
        int initial_slot = -1;
        int dist = -1;
        int existing_distance = -1;
        /*! #end !*/

        //iterate all the old arrays to add in the newly allocated buffers
        //It is important to iterate backwards to minimize the conflict chain length !
        for (int i = oldKeys.length; --i >= 0;)
        {
            if (is_allocated(i, oldKeys))
            {
                key = oldKeys[i];
                value = oldValues[i];

                slot = REHASH(strategy, key) & mask;

                /*! #if ($RH) !*/
                initial_slot = slot;
                dist = 0;
                /*! #end !*/

                while (is_allocated(slot, keys))
                {
                    /*! #if ($RH) !*/
                    //re-shuffle keys to minimize variance
                    existing_distance = probe_distance(slot, cached);

                    if (dist > existing_distance)
                    {
                        //swap current (key, value, initial_slot) with slot places
                        tmpKey = keys[slot];
                        keys[slot] = key;
                        key = tmpKey;

                        tmpAllocated = cached[slot];
                        cached[slot] = initial_slot;
                        initial_slot = tmpAllocated;

                        tmpValue = values[slot];
                        values[slot] = value;
                        value = tmpValue;

                        /*! #if($DEBUG) !*/
                        //Check invariants
                        assert cached[slot] == (REHASH(strategy, keys[slot]) & mask);
                        assert initial_slot == (REHASH(strategy, key) & mask);
                        /*! #end !*/

                        dist = existing_distance;
                    }
                    /*! #end !*/

                    slot = (slot + 1) & mask;

                    /*! #if ($RH) !*/
                    dist++;
                    /*! #end !*/
                } //end while

                /*! #if ($RH) !*/
                cached[slot] = initial_slot;
                /*! #end !*/

                keys[slot] = key;
                values[slot] = value;

                /*! #if ($RH) !*/
                /*! #if($DEBUG) !*/
                //Check invariants
                assert cached[slot] == (REHASH(strategy, keys[slot]) & mask);
                /*! #end !*/
                /*! #end !*/
            }
        }
    }

    /**
     * Allocate internal buffers for a given capacity.
     * 
     * @param capacity New capacity (must be a power of two).
     */
    private void allocateBuffers(final int capacity)
    {
        try {

            final KType[] keys = Intrinsics.newKTypeArray(capacity);
            final VType[] values = Intrinsics.newVTypeArray(capacity);

            /*! #if ($RH) !*/
            final int[] cached = new int[capacity];
            /*!  #end !*/

            this.keys = keys;
            this.values = values;

            /*! #if ($RH) !*/
            this.hash_cache = cached;
            /*! #end !*/

            //allocate so that there is at least one slot that remains allocated = false
            //this is compulsory to guarantee proper stop in searching loops
            this.resizeAt = Math.max(3, (int) (capacity * this.loadFactor)) - 2;
        }
        catch (final OutOfMemoryError e) {

            throw new BufferAllocationException(
                    "Not enough memory to allocate buffers to grow  %d -> %d",
                    e,
                    this.keys.length,
                    capacity);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VType remove(final KType key)
    {
        if (Intrinsics.isEmptyKey(key)) {

            if (this.allocatedDefaultKey) {

                final VType previousValue = this.allocatedDefaultKeyValue;

                /*! #if ($TemplateOptions.VTypeGeneric) !*/
                //help the GC
                this.allocatedDefaultKeyValue = Intrinsics.defaultVTypeValue();
                /*! #end !*/

                this.allocatedDefaultKey = false;
                return previousValue;
            }

            return this.defaultValue;
        }

        final int mask = this.keys.length - 1;

        final KType[] keys = this.keys;
        final VType[] values = this.values;

        final KTypeHashingStrategy<? super KType> strategy = this.hashStrategy;

        //copied straight from  fastutil "fast-path"
        int slot;
        KType curr;

        //1.1 The rehashed slot is free, nothing to remove, return default value
        if (Intrinsics.isEmptyKey(curr = keys[slot = REHASH(strategy, key) & mask])) {

            return this.defaultValue;
        }

        //1.2) The rehashed entry is occupied by the key, remove it, return value
        if (strategy.equals(curr, key)) {

            final VType value = values[slot];

            this.assigned--;
            shiftConflictingKeys(slot);

            return value;
        }

        //2. Hash collision, search for the key along the path
        slot = (slot + 1) & mask;

        /*! #if ($RH) !*/
        int dist = 0;
        final int[] cached = this.hash_cache;
        /*!  #end !*/

        //2. Slow path here
        while (is_allocated(slot, keys)
                /*! #if ($RH) !*/&& dist <= probe_distance(slot, cached) /*! #end !*/)
        {
            if (strategy.equals(key, keys[slot]))
            {
                final VType value = values[slot];

                this.assigned--;
                shiftConflictingKeys(slot);

                return value;
            }
            slot = (slot + 1) & mask;

            /*! #if ($RH) !*/
            dist++;
            /*! #end !*/
        } //end while true

        return this.defaultValue;
    }

    /**
     * Shift all the slot-conflicting keys allocated to (and including) <code>slot</code>.
     */
    private void shiftConflictingKeys(int gapSlot)
    {
        final int mask = this.keys.length - 1;

        final KTypeHashingStrategy<? super KType> strategy = this.hashStrategy;

        final KType[] keys = this.keys;
        final VType[] values = this.values;

        /*! #if ($RH) !*/
        final int[] cached = this.hash_cache;
        /*!  #end !*/

        // Perform shifts of conflicting keys to fill in the gap.
        int distance = 0;
        while (true) {

            final int slot = (gapSlot + (++distance)) & mask;

            final KType existing = keys[slot];
            final VType existingValue = values[slot];

            if (Intrinsics.isEmptyKey(existing)) {
                break;
            }

            /*! #if ($RH) !*/
            //use the cached value, no need to recompute
            final int idealSlotModMask = cached[slot];
            /*! #if($DEBUG) !*/
            //Check invariants
            assert idealSlotModMask == (REHASH(strategy, existing) & mask);
            /*! #end !*/
            /*! #else
            final int idealSlotModMask = REHASH(strategy, existing) & mask;
            #end !*/

            //original HPPC code: shift = (slot - idealSlot) & mask;
            //equivalent to shift = (slot & mask - idealSlot & mask) & mask;
            //since slot and idealSlotModMask are already folded, we have :
            final int shift = (slot - idealSlotModMask) & mask;

            if (shift >= distance) {
                // Entry at this position was originally at or before the gap slot.
                // Move the conflict-shifted entry to the gap's position and repeat the procedure
                // for any entries to the right of the current position, treating it
                // as the new gap.
                keys[gapSlot] = existing;
                values[gapSlot] = existingValue;

                /*! #if ($RH) !*/
                cached[gapSlot] = idealSlotModMask;
                /*! #end !*/

                gapSlot = slot;
                distance = 0;
            }
        } //end while

        // Mark the last found gap slot without a conflict as empty.
        keys[gapSlot] = Intrinsics.<KType> defaultKTypeValue();

        /* #if ($TemplateOptions.VTypeGeneric) */
        values[gapSlot] = Intrinsics.<VType> defaultVTypeValue();
        /* #end */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAll(final KTypeContainer<? super KType> container)
    {
        final int before = this.size();

        if (this.allocatedDefaultKey) {

            if (container.contains(Intrinsics.<KType> defaultKTypeValue()))
            {
                this.allocatedDefaultKey = false;

                /*! #if ($TemplateOptions.VTypeGeneric) !*/
                //help the GC
                this.allocatedDefaultKeyValue = Intrinsics.defaultVTypeValue();
                /*! #end !*/
            }
        }

        final KType[] keys = this.keys;

        for (int i = 0; i < keys.length;)
        {
            if (is_allocated(i, keys) && container.contains(keys[i]))
            {
                this.assigned--;
                shiftConflictingKeys(i);
                // Shift, do not increment slot.
            }
            else {
                i++;
            }
        }

        return before - this.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAll(final KTypePredicate<? super KType> predicate)
    {
        final int before = this.size();

        if (this.allocatedDefaultKey) {

            if (predicate.apply(Intrinsics.<KType> defaultKTypeValue()))
            {
                this.allocatedDefaultKey = false;

                /*! #if ($TemplateOptions.VTypeGeneric) !*/
                //help the GC
                this.allocatedDefaultKeyValue = Intrinsics.defaultVTypeValue();
                /*! #end !*/
            }
        }

        final KType[] keys = this.keys;

        for (int i = 0; i < keys.length;)
        {
            if (is_allocated(i, keys) && predicate.apply(keys[i]))
            {
                this.assigned--;
                shiftConflictingKeys(i);
                // Shift, do not increment slot.
            }
            else {
                i++;
            }
        }

        return before - this.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAll(final KTypeVTypePredicate<? super KType, ? super VType> predicate) {

        final int before = this.size();

        if (this.allocatedDefaultKey) {

            if (predicate.apply(Intrinsics.<KType> defaultKTypeValue(), this.allocatedDefaultKeyValue))
            {
                this.allocatedDefaultKey = false;

                /*! #if ($TemplateOptions.VTypeGeneric) !*/
                //help the GC
                this.allocatedDefaultKeyValue = Intrinsics.defaultVTypeValue();
                /*! #end !*/
            }
        }

        final KType[] keys = this.keys;
        final VType[] values = this.values;

        for (int i = 0; i < keys.length;)
        {
            if (is_allocated(i, keys) && predicate.apply(keys[i], values[i]))
            {
                this.assigned--;
                shiftConflictingKeys(i);
                // Shift, do not increment slot.
            }
            else {
                i++;
            }
        }

        return before - this.size();
    }

    /**
     * {@inheritDoc}
     * 
     * <p> Use the following snippet of code to check for key existence
     * first and then retrieve the value if it exists.</p>
     * <pre>
     * if (map.containsKey(key))
     *   value = map.lget();
     * </pre>
     */
    @Override
    public VType get(final KType key)
    {
        if (Intrinsics.isEmptyKey(key)) {

            if (this.allocatedDefaultKey) {

                return this.allocatedDefaultKeyValue;
            }

            return this.defaultValue;
        }

        final int mask = this.keys.length - 1;

        final KTypeHashingStrategy<? super KType> strategy = this.hashStrategy;

        final KType[] keys = this.keys;
        final VType[] values = this.values;

        //copied straight from  fastutil "fast-path"
        int slot;
        KType curr;

        //1.1 The rehashed slot is free, nothing to get, return default value
        if (Intrinsics.isEmptyKey(curr = keys[slot = REHASH(strategy, key) & mask])) {

            return this.defaultValue;
        }

        //1.2) The rehashed entry is occupied by the key, return value
        if (strategy.equals(curr, key)) {

            return values[slot];
        }

        //2. Hash collision, search for the key along the path
        slot = (slot + 1) & mask;

        /*! #if ($RH) !*/
        int dist = 0;
        final int[] cached = this.hash_cache;
        /*! #end !*/

        while (is_allocated(slot, keys)
                /*! #if ($RH) !*/&& dist <= probe_distance(slot, cached) /*! #end !*/)
        {
            if (strategy.equals(key, keys[slot]))
            {
                return values[slot];
            }
            slot = (slot + 1) & mask;

            /*! #if ($RH) !*/
            dist++;
            /*! #end !*/
        } //end while true

        return this.defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(final KType key)
    {
        if (Intrinsics.isEmptyKey(key)) {

            return this.allocatedDefaultKey;
        }

        final int mask = this.keys.length - 1;

        final KTypeHashingStrategy<? super KType> strategy = this.hashStrategy;

        final KType[] keys = this.keys;

        //copied straight from  fastutil "fast-path"
        int slot;
        KType curr;

        //1.1 The rehashed slot is free, return false
        if (Intrinsics.isEmptyKey(curr = keys[slot = REHASH(strategy, key) & mask])) {

            return false;
        }

        //1.2) The rehashed entry is occupied by the key, return true
        if (strategy.equals(curr, key)) {

            return true;
        }

        //2. Hash collision, search for the key along the path
        slot = (slot + 1) & mask;

        /*! #if ($RH) !*/
        int dist = 0;
        final int[] cached = this.hash_cache;
        /*! #end !*/

        while (is_allocated(slot, keys)
                /*! #if ($RH) !*/&& dist <= probe_distance(slot, cached) /*! #end !*/)
        {
            if (strategy.equals(key, keys[slot]))
            {
                return true;
            }
            slot = (slot + 1) & mask;

            /*! #if ($RH) !*/
            dist++;
            /*! #end !*/
        } //end while true

        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Does not release internal buffers.</p>
     */
    @Override
    public void clear()
    {
        this.assigned = 0;

        // States are always cleared.
        this.allocatedDefaultKey = false;

        /*! #if ($TemplateOptions.VTypeGeneric) !*/
        //help the GC
        this.allocatedDefaultKeyValue = Intrinsics.defaultVTypeValue();
        /*! #end !*/

        //Faster than Arrays.fill(keys, null); // Help the GC.
        KTypeArrays.blankArray(this.keys, 0, this.keys.length);

        /*! #if ($TemplateOptions.VTypeGeneric) !*/
        //Faster than Arrays.fill(values, null); // Help the GC.
        VTypeArrays.<VType> blankArray(this.values, 0, this.values.length);
        /*! #end !*/
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        return this.assigned + (this.allocatedDefaultKey ? 1 : 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int capacity() {

        return this.resizeAt - 1;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Note that an empty container may still contain many deleted keys (that occupy buffer
     * space). Adding even a single element to such a container may cause rehashing.</p>
     */
    @Override
    public boolean isEmpty()
    {
        return size() == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final KTypeHashingStrategy<? super KType> strategy = this.hashStrategy;

        int h = 0;

        if (this.allocatedDefaultKey) {
            h += 0 + Internals.rehash(this.allocatedDefaultKeyValue);
        }

        final KType[] keys = this.keys;
        final VType[] values = this.values;

        for (int i = keys.length; --i >= 0;)
        {
            if (is_allocated(i, keys))
            {
                h += PhiMix.hash(strategy.computeHashCode(keys[i])) + Internals.rehash(values[i]);
            }
        }

        return h;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj)
    {
        if (obj != null)
        {
            if (obj == this)
            {
                return true;
            }

            //must be of the same class, subclasses are not comparable
            if (obj.getClass() != this.getClass())
            {
                return false;
            }

            if (!this.hashStrategy.equals(((KTypeVTypeOpenCustomHashMap<KType, VType>) obj).hashStrategy))
            {
                return false;
            }

            @SuppressWarnings("unchecked")
            final KTypeVTypeOpenCustomHashMap<KType, VType> other = (KTypeVTypeOpenCustomHashMap<KType, VType>) obj;

            if (other.size() == this.size())
            {
                final EntryIterator it = this.iterator();

                while (it.hasNext())
                {
                    final KTypeVTypeCursor<KType, VType> c = it.next();

                    if (other.containsKey(c.key))
                    {
                        final VType v = other.get(c.key);
                        if (Intrinsics.equalsVType(c.value, v))
                        {
                            continue;
                        }
                    }
                    //recycle
                    it.release();
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * An iterator implementation for {@link #iterator}.
     */
    public final class EntryIterator extends AbstractIterator<KTypeVTypeCursor<KType, VType>>
    {
        public final KTypeVTypeCursor<KType, VType> cursor;

        public EntryIterator()
        {
            this.cursor = new KTypeVTypeCursor<KType, VType>();
            this.cursor.index = -2;
        }

        /**
         * Iterate backwards w.r.t the buffer, to
         * minimize collision chains when filling another hash container (ex. with putAll())
         */
        @Override
        protected KTypeVTypeCursor<KType, VType> fetch()
        {
            if (this.cursor.index == KTypeVTypeOpenCustomHashMap.this.keys.length + 1) {

                if (KTypeVTypeOpenCustomHashMap.this.allocatedDefaultKey) {

                    this.cursor.index = KTypeVTypeOpenCustomHashMap.this.keys.length;
                    this.cursor.key = Intrinsics.defaultKTypeValue();
                    this.cursor.value = KTypeVTypeOpenCustomHashMap.this.allocatedDefaultKeyValue;

                    return this.cursor;

                }
                //no value associated with the default key, continue iteration...
                this.cursor.index = KTypeVTypeOpenCustomHashMap.this.keys.length;
            }

            int i = this.cursor.index - 1;

            while (i >= 0 &&
                    !is_allocated(i, KTypeVTypeOpenCustomHashMap.this.keys))
            {
                i--;
            }

            if (i == -1)
            {
                return done();
            }

            this.cursor.index = i;
            this.cursor.key = KTypeVTypeOpenCustomHashMap.this.keys[i];
            this.cursor.value = KTypeVTypeOpenCustomHashMap.this.values[i];

            return this.cursor;
        }
    }

    /**
     * internal pool of EntryIterator
     */
    protected final IteratorPool<KTypeVTypeCursor<KType, VType>, EntryIterator> entryIteratorPool = new IteratorPool<KTypeVTypeCursor<KType, VType>, EntryIterator>(
            new ObjectFactory<EntryIterator>() {

                @Override
                public EntryIterator create()
                {
                    return new EntryIterator();
                }

                @Override
                public void initialize(final EntryIterator obj)
                {
                    obj.cursor.index = KTypeVTypeOpenCustomHashMap.this.keys.length + 1;
                }

                @Override
                public void reset(final EntryIterator obj) {
                    // nothing
                }
            });

    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    public EntryIterator iterator()
    {
        //return new EntryIterator();
        return this.entryIteratorPool.borrow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypeVTypeProcedure<? super KType, ? super VType>> T forEach(final T procedure)
    {
        if (this.allocatedDefaultKey) {

            procedure.apply(Intrinsics.<KType> defaultKTypeValue(), this.allocatedDefaultKeyValue);
        }

        final KType[] keys = this.keys;
        final VType[] values = this.values;

        //Iterate in reverse for side-stepping the longest conflict chain
        //in another hash, in case apply() is actually used to fill another hash container.
        for (int i = keys.length - 1; i >= 0; i--)
        {
            if (is_allocated(i, keys))
            {
                procedure.apply(keys[i], values[i]);
            }
        }

        return procedure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypeVTypePredicate<? super KType, ? super VType>> T forEach(final T predicate)
    {
        if (this.allocatedDefaultKey) {

            if (!predicate.apply(Intrinsics.<KType> defaultKTypeValue(), this.allocatedDefaultKeyValue)) {

                return predicate;
            }
        }

        final KType[] keys = this.keys;
        final VType[] values = this.values;

        //Iterate in reverse for side-stepping the longest conflict chain
        //in another hash, in case apply() is actually used to fill another hash container.
        for (int i = keys.length - 1; i >= 0; i--)
        {
            if (is_allocated(i, keys))
            {
                if (!predicate.apply(keys[i], values[i]))
                {
                    break;
                }
            }
        } //end for

        return predicate;
    }

    /**
     *  @return a new KeysContainer view of the keys of this associated container.
     * This view then reflects all changes from the map.
     */
    @Override
    public KeysContainer keys()
    {
        return new KeysContainer();
    }

    /**
     * A view of the keys inside this hash map.
     */
    public final class KeysContainer
    extends AbstractKTypeCollection<KType> implements KTypeLookupContainer<KType>
    {
        private final KTypeVTypeOpenCustomHashMap<KType, VType> owner =
                KTypeVTypeOpenCustomHashMap.this;

        @Override
        public boolean contains(final KType e)
        {
            return containsKey(e);
        }

        @Override
        public <T extends KTypeProcedure<? super KType>> T forEach(final T procedure)
        {

            if (this.owner.allocatedDefaultKey) {

                procedure.apply(Intrinsics.<KType> defaultKTypeValue());
            }

            final KType[] keys = this.owner.keys;

            //Iterate in reverse for side-stepping the longest conflict chain
            //in another hash, in case apply() is actually used to fill another hash container.
            for (int i = keys.length - 1; i >= 0; i--)
            {
                if (is_allocated(i, keys))
                {
                    procedure.apply(keys[i]);
                }
            }

            return procedure;
        }

        @Override
        public <T extends KTypePredicate<? super KType>> T forEach(final T predicate)
        {
            if (this.owner.allocatedDefaultKey) {

                if (!predicate.apply(Intrinsics.<KType> defaultKTypeValue())) {

                    return predicate;
                }
            }

            final KType[] keys = this.owner.keys;

            //Iterate in reverse for side-stepping the longest conflict chain
            //in another hash, in case apply() is actually used to fill another hash container.
            for (int i = keys.length - 1; i >= 0; i--)
            {
                if (is_allocated(i, keys))
                {
                    if (!predicate.apply(keys[i]))
                    {
                        break;
                    }
                }
            }

            return predicate;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public KeysIterator iterator()
        {
            //return new KeysIterator();
            return this.keyIteratorPool.borrow();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int size()
        {
            return this.owner.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int capacity() {

            return this.owner.capacity();
        }

        @Override
        public void clear()
        {
            this.owner.clear();
        }

        @Override
        public int removeAll(final KTypePredicate<? super KType> predicate)
        {
            return this.owner.removeAll(predicate);
        }

        @Override
        public int removeAll(final KType e)
        {
            final boolean hasKey = this.owner.containsKey(e);
            int result = 0;
            if (hasKey)
            {
                this.owner.remove(e);
                result = 1;
            }
            return result;
        }

        /**
         * internal pool of KeysIterator
         */
        protected final IteratorPool<KTypeCursor<KType>, KeysIterator> keyIteratorPool = new IteratorPool<KTypeCursor<KType>, KeysIterator>(
                new ObjectFactory<KeysIterator>() {

                    @Override
                    public KeysIterator create()
                    {
                        return new KeysIterator();
                    }

                    @Override
                    public void initialize(final KeysIterator obj)
                    {
                        obj.cursor.index = KTypeVTypeOpenCustomHashMap.this.keys.length + 1;
                    }

                    @Override
                    public void reset(final KeysIterator obj) {
                        // nothing
                    }
                });

        @Override
        public KType[] toArray(final KType[] target)
        {
            int count = 0;

            if (this.owner.allocatedDefaultKey) {

                target[count++] = Intrinsics.defaultKTypeValue();
            }

            final KType[] keys = this.owner.keys;

            for (int i = 0; i < keys.length; i++)
            {
                if (is_allocated(i, keys))
                {
                    target[count++] = keys[i];
                }
            }

            assert count == this.owner.size();
            return target;
        }
    };

    /**
     * An iterator over the set of keys.
     */
    public final class KeysIterator extends AbstractIterator<KTypeCursor<KType>>
    {
        public final KTypeCursor<KType> cursor;

        public KeysIterator()
        {
            this.cursor = new KTypeCursor<KType>();
            this.cursor.index = -2;
        }

        /**
         * Iterate backwards w.r.t the buffer, to
         * minimize collision chains when filling another hash container (ex. with putAll())
         */
        @Override
        protected KTypeCursor<KType> fetch()
        {
            if (this.cursor.index == KTypeVTypeOpenCustomHashMap.this.keys.length + 1) {

                if (KTypeVTypeOpenCustomHashMap.this.allocatedDefaultKey) {

                    this.cursor.index = KTypeVTypeOpenCustomHashMap.this.keys.length;
                    this.cursor.value = Intrinsics.defaultKTypeValue();

                    return this.cursor;

                }
                //no value associated with the default key, continue iteration...
                this.cursor.index = KTypeVTypeOpenCustomHashMap.this.keys.length;
            }

            int i = this.cursor.index - 1;

            while (i >= 0 && !is_allocated(i, KTypeVTypeOpenCustomHashMap.this.keys))
            {
                i--;
            }

            if (i == -1)
            {
                return done();
            }

            this.cursor.index = i;
            this.cursor.value = KTypeVTypeOpenCustomHashMap.this.keys[i];

            return this.cursor;
        }
    }

    /**
     *  @return a new ValuesContainer, view of the values of this map.
     * This view then reflects all changes from the map.
     */
    @Override
    public ValuesContainer values()
    {
        return new ValuesContainer();
    }

    /**
     * A view over the set of values of this map.
     */
    public final class ValuesContainer extends AbstractKTypeCollection<VType>
    {
        private final KTypeVTypeOpenCustomHashMap<KType, VType> owner =
                KTypeVTypeOpenCustomHashMap.this;

        /**
         * {@inheritDoc}
         */
        @Override
        public int size()
        {
            return this.owner.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int capacity() {

            return this.owner.capacity();
        }

        @Override
        public boolean contains(final VType value)
        {
            if (this.owner.allocatedDefaultKey && Intrinsics.equalsVType(value, this.owner.allocatedDefaultKeyValue)) {

                return true;
            }

            // This is a linear scan over the values, but it's in the contract, so be it.

            final KType[] keys = this.owner.keys;
            final VType[] values = this.owner.values;

            for (int slot = 0; slot < keys.length; slot++)
            {
                if (is_allocated(slot, keys)
                        && Intrinsics.equalsVType(value, values[slot]))
                {
                    return true;
                }
            }
            return false;
        }

        @Override
        public <T extends KTypeProcedure<? super VType>> T forEach(final T procedure)
        {
            if (this.owner.allocatedDefaultKey) {

                procedure.apply(this.owner.allocatedDefaultKeyValue);
            }

            final KType[] keys = this.owner.keys;
            final VType[] values = this.owner.values;

            for (int slot = 0; slot < keys.length; slot++)
            {
                if (is_allocated(slot, keys)) {
                    procedure.apply(values[slot]);
                }
            }

            return procedure;
        }

        @Override
        public <T extends KTypePredicate<? super VType>> T forEach(final T predicate)
        {
            if (this.owner.allocatedDefaultKey) {

                if (!predicate.apply(this.owner.allocatedDefaultKeyValue))
                {
                    return predicate;
                }
            }

            final KType[] keys = this.owner.keys;
            final VType[] values = this.owner.values;

            for (int slot = 0; slot < keys.length; slot++)
            {
                if (is_allocated(slot, keys))
                {
                    if (!predicate.apply(values[slot]))
                    {
                        break;
                    }
                }
            }

            return predicate;
        }

        @Override
        public ValuesIterator iterator()
        {
            // return new ValuesIterator();
            return this.valuesIteratorPool.borrow();
        }

        /**
         * {@inheritDoc}
         * Indeed removes all the (key,value) pairs matching
         * (key ? ,  e) with the  same  e,  from  the map.
         */
        @Override
        public int removeAll(final VType e)
        {
            final int before = this.owner.size();

            if (this.owner.allocatedDefaultKey) {

                if (Intrinsics.equalsVType(e, this.owner.allocatedDefaultKeyValue)) {

                    this.owner.allocatedDefaultKey = false;

                    /*! #if ($TemplateOptions.VTypeGeneric) !*/
                    //help the GC
                    this.owner.allocatedDefaultKeyValue = Intrinsics.defaultVTypeValue();
                    /*! #end !*/
                }
            }

            final KType[] keys = this.owner.keys;
            final VType[] values = this.owner.values;

            for (int slot = 0; slot < keys.length;)
            {
                if (is_allocated(slot, keys) && Intrinsics.equalsVType(e, values[slot]))
                {
                    this.owner.assigned--;
                    shiftConflictingKeys(slot);
                    // Shift, do not increment slot.
                }
                else {
                    slot++;
                }
            }
            return before - this.owner.size();
        }

        /**
         * {@inheritDoc}
         * Indeed removes all the (key,value) pairs matching
         * the predicate for the values, from  the map.
         */
        @Override
        public int removeAll(final KTypePredicate<? super VType> predicate)
        {
            final int before = this.owner.size();

            if (this.owner.allocatedDefaultKey) {

                if (predicate.apply(this.owner.allocatedDefaultKeyValue)) {

                    this.owner.allocatedDefaultKey = false;

                    /*! #if ($TemplateOptions.VTypeGeneric) !*/
                    //help the GC
                    this.owner.allocatedDefaultKeyValue = Intrinsics.defaultVTypeValue();
                    /*! #end !*/
                }
            }

            final KType[] keys = this.owner.keys;
            final VType[] values = this.owner.values;

            for (int slot = 0; slot < keys.length;)
            {
                if (is_allocated(slot, keys) && predicate.apply(values[slot]))
                {
                    this.owner.assigned--;
                    shiftConflictingKeys(slot);
                    // Shift, do not increment slot.
                }
                else {
                    slot++;
                }
            }

            return before - this.owner.size();
        }

        /**
         * {@inheritDoc}
         *  Alias for clear() the whole map.
         */
        @Override
        public void clear()
        {
            this.owner.clear();
        }

        /**
         * internal pool of ValuesIterator
         */
        protected final IteratorPool<KTypeCursor<VType>, ValuesIterator> valuesIteratorPool = new IteratorPool<KTypeCursor<VType>, ValuesIterator>(
                new ObjectFactory<ValuesIterator>() {

                    @Override
                    public ValuesIterator create()
                    {

                        return new ValuesIterator();
                    }

                    @Override
                    public void initialize(final ValuesIterator obj)
                    {
                        obj.cursor.index = KTypeVTypeOpenCustomHashMap.this.keys.length + 1;
                    }

                    @Override
                    public void reset(final ValuesIterator obj) {
                        // nothing

                    }
                });

        @Override
        public VType[] toArray(final VType[] target)
        {
            int count = 0;

            if (this.owner.allocatedDefaultKey) {

                target[count++] = this.owner.allocatedDefaultKeyValue;
            }

            final KType[] keys = this.owner.keys;
            final VType[] values = this.owner.values;

            for (int i = 0; i < values.length; i++)
            {
                if (is_allocated(i, keys))
                {
                    target[count++] = values[i];
                }
            }

            assert count == this.owner.size();
            return target;
        }
    }

    /**
     * An iterator over the set of  values.
     */
    public final class ValuesIterator extends AbstractIterator<KTypeCursor<VType>>
    {
        public final KTypeCursor<VType> cursor;

        public ValuesIterator()
        {
            this.cursor = new KTypeCursor<VType>();
            this.cursor.index = -2;
        }

        /**
         * Iterate backwards w.r.t the buffer, to
         * minimize collision chains when filling another hash container (ex. with putAll())
         */
        @Override
        protected KTypeCursor<VType> fetch()
        {

            if (this.cursor.index == KTypeVTypeOpenCustomHashMap.this.values.length + 1) {

                if (KTypeVTypeOpenCustomHashMap.this.allocatedDefaultKey) {

                    this.cursor.index = KTypeVTypeOpenCustomHashMap.this.values.length;
                    this.cursor.value = KTypeVTypeOpenCustomHashMap.this.allocatedDefaultKeyValue;

                    return this.cursor;

                }
                //no value associated with the default key, continue iteration...
                this.cursor.index = KTypeVTypeOpenCustomHashMap.this.keys.length;
            }

            int i = this.cursor.index - 1;

            while (i >= 0 &&
                    !is_allocated(i, KTypeVTypeOpenCustomHashMap.this.keys))
            {
                i--;
            }

            if (i == -1)
            {
                return done();
            }

            this.cursor.index = i;
            this.cursor.value = KTypeVTypeOpenCustomHashMap.this.values[i];

            return this.cursor;
        }
    }

    /**
     * Clone this object.
     * #if ($TemplateOptions.AnyGeneric)
     * The returned clone will use the same HashingStrategy strategy.
     * #end
     */
    @Override
    public KTypeVTypeOpenCustomHashMap<KType, VType> clone()
    {
        final KTypeVTypeOpenCustomHashMap<KType, VType> cloned =
                new KTypeVTypeOpenCustomHashMap<KType, VType>(capacity(), this.loadFactor, this.hashStrategy);

        //We must NOT clone because of independent perturbations seeds
        cloned.putAll(this);

        cloned.allocatedDefaultKeyValue = this.allocatedDefaultKeyValue;
        cloned.allocatedDefaultKey = this.allocatedDefaultKey;
        cloned.defaultValue = this.defaultValue;

        return cloned;

    }

    /**
     * Convert the contents of this map to a human-friendly string.
     */
    @Override
    public String toString()
    {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("[");

        boolean first = true;
        for (final KTypeVTypeCursor<KType, VType> cursor : this)
        {
            if (!first)
            {
                buffer.append(", ");
            }
            buffer.append(cursor.key);
            buffer.append("=>");
            buffer.append(cursor.value);
            first = false;
        }
        buffer.append("]");
        return buffer.toString();
    }

    /**
     * Creates a hash map from two index-aligned arrays of key-value pairs.
     */
    public static <KType, VType> KTypeVTypeOpenCustomHashMap<KType, VType> from(final KType[] keys, final VType[] values, final KTypeHashingStrategy<? super KType> hashStrategy)
    {
        if (keys.length != values.length)
        {
            throw new IllegalArgumentException("Arrays of keys and values must have an identical length.");
        }

        final KTypeVTypeOpenCustomHashMap<KType, VType> map = new KTypeVTypeOpenCustomHashMap<KType, VType>(keys.length, hashStrategy);

        for (int i = 0; i < keys.length; i++)
        {
            map.put(keys[i], values[i]);
        }
        return map;
    }

    /**
     * Create a hash map from another associative container.
     */
    public static <KType, VType> KTypeVTypeOpenCustomHashMap<KType, VType> from(final KTypeVTypeAssociativeContainer<KType, VType> container, final KTypeHashingStrategy<? super KType> hashStrategy)
    {
        return new KTypeVTypeOpenCustomHashMap<KType, VType>(container, hashStrategy);
    }

    /**
     * Create a new hash map without providing the full generic signature (constructor
     * shortcut).
     */
    public static <KType, VType> KTypeVTypeOpenCustomHashMap<KType, VType> newInstance(final KTypeHashingStrategy<? super KType> hashStrategy)
    {
        return new KTypeVTypeOpenCustomHashMap<KType, VType>(hashStrategy);
    }

    /**
     * Create a new hash map with initial capacity and load factor control. (constructor
     * shortcut).
     */
    public static <KType, VType> KTypeVTypeOpenCustomHashMap<KType, VType> newInstance(final int initialCapacity, final float loadFactor, final KTypeHashingStrategy<? super KType> hashStrategy)
    {
        return new KTypeVTypeOpenCustomHashMap<KType, VType>(initialCapacity, loadFactor, hashStrategy);
    }

    /**
     * Return the current {@link KTypeHashingStrategy} in use.
     */
    public KTypeHashingStrategy<? super KType> strategy()
    {
        return this.hashStrategy;
    }

    /**
     * Returns the "default value" value used
     * in containers methods returning "default value"
     * @return
     */
    public VType getDefaultValue()
    {
        return this.defaultValue;
    }

    /**
     * Set the "default value" value to be used
     * in containers methods returning "default value"
     * @return
     */
    public void setDefaultValue(final VType defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    //Test for existence in template
    /*! #if ($TemplateOptions.inlineKType("is_allocated",
    "(slot, keys)",
    "!Intrinsics.isEmptyKey(keys[slot])")) !*/
    /**
     *  template version
     * (actual method is inlined in generated code)
     */
    private boolean is_allocated(final int slot, final KType[] keys) {

        return !Intrinsics.isEmptyKey(keys[slot]);
    }

    /*! #end !*/

    /*! #if ($TemplateOptions.inlineKType("probe_distance",
    "(slot, cache)",
    "slot < cache[slot] ? slot + cache.length - cache[slot] : slot - cache[slot]")) !*/
    /**
     * (actual method is inlined in generated code)
     */
    private int probe_distance(final int slot, final int[] cache) {

        final int rh = cache[slot];

        /*! #if($DEBUG) !*/
        //Check : cached hashed slot is == computed value
        final int mask = cache.length - 1;
        assert rh == (REHASH(this.hashStrategy, this.keys[slot]) & mask);
        /*! #end !*/

        if (slot < rh) {
            //wrap around
            return slot + cache.length - rh;
        }

        return slot - rh;
    }

    /*! #end !*/

    /*! #if ($TemplateOptions.inlineKType("REHASH",
    "(strategy, value)",
    "MurmurHash3.hash(strategy.computeHashCode(value) ^ this.perturbation )")) !*/
    /**
     * (actual method is inlined in generated code)
     */
    private int REHASH(final KTypeHashingStrategy<? super KType> strategy, final KType value) {

        return MurmurHash3.hash(strategy.computeHashCode(value) ^ this.perturbation);
    }
    /*! #end !*/

}
