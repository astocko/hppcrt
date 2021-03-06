package com.carrotsearch.hppcrt.lists;

import org.junit.After;
import org.junit.Assert;

import com.carrotsearch.hppcrt.Intrinsics;
import com.carrotsearch.hppcrt.KTypeContainer;
import com.carrotsearch.hppcrt.KTypeDeque;
import com.carrotsearch.hppcrt.TestUtils;

/*! #import("com/carrotsearch/hppcrt/Intrinsics.java") !*/
/**
 * Unit tests for {@link KTypeArrayDeque as KTypeDeque}.
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeArrayDequeAsDequeTest<KType> extends AbstractKTypeDequeTest<KType>
{
    @Override
    protected KTypeDeque<KType> createNewInstance(final int initialCapacity) {

        return new KTypeArrayDeque<KType>(initialCapacity);
    }

    @Override
    protected void addFromArray(final KTypeDeque<KType> testList, final KType... keys) {
        final KTypeArrayDeque<KType> concreteClass = (KTypeArrayDeque<KType>) (testList);
        concreteClass.addLast(keys);

    }

    @Override
    protected void addFromContainer(final KTypeDeque<KType> testList, final KTypeContainer<KType> container) {
        final KTypeArrayDeque<KType> concreteClass = (KTypeArrayDeque<KType>) (testList);
        concreteClass.addLast(container);

    }

    @Override
    protected KType[] getBuffer(final KTypeDeque<KType> testList) {
        final KTypeArrayDeque<KType> concreteClass = (KTypeArrayDeque<KType>) (testList);
        return Intrinsics.<KType[]> cast(concreteClass.buffer);
    }

    @Override
    protected int getDescendingValuePoolSize(final KTypeDeque<KType> testList) {
        final KTypeArrayDeque<KType> concreteClass = (KTypeArrayDeque<KType>) (testList);
        return concreteClass.descendingValueIteratorPool.size();
    }

    @Override
    protected int getDescendingValuePoolCapacity(final KTypeDeque<KType> testList) {
        final KTypeArrayDeque<KType> concreteClass = (KTypeArrayDeque<KType>) (testList);
        return concreteClass.descendingValueIteratorPool.capacity();
    }

    /**
     * Move one index to the right, wrapping around buffer of size modulus
     */
    private int oneRight(final int index, final int modulus)
    {
        return (index + 1 == modulus) ? 0 : index + 1;
    }

    @After
    public void checkConsistency()
    {
        final KTypeArrayDeque<KType> arrayDeque = (KTypeArrayDeque<KType>) this.deque;

        if (arrayDeque != null)
        {
            int count = 0;
            //check access by get()
            for (/*! #if ($TemplateOptions.KTypeGeneric) !*/final Object
                    /*! #else
            final KType
            #end !*/
                    val : arrayDeque.toArray()) {

                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                TestUtils.assertEquals2(val, (Object) arrayDeque.get(count));
                /*! #else
                TestUtils.assertEquals2(val, arrayDeque.get(count));
                #end !*/
                count++;
            }

            Assert.assertEquals(count, this.deque.size());

            for (int i = arrayDeque.tail; i < arrayDeque.head; i = oneRight(i, arrayDeque.buffer.length))
            {
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                Assert.assertTrue(Intrinsics.<KType> empty() == arrayDeque.buffer[i]);
                /*! #end !*/
            }
        }
    }
}
