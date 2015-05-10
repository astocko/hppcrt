package com.carrotsearch.hppcrt.generator.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.SyntaxTree;

import com.google.common.collect.Lists;

/** */
public final class Replacement
{
    public final Interval interval;
    public final String source;
    public final String replacement;

    public Replacement(final String src, final Interval interval, final String replacement) {

        //source is only useful for debug purposes
        this.source = src;

        this.interval = interval;
        this.replacement = replacement;
    }

    public Replacement(final String source, final SyntaxTree ctx, final String replacement) {
        this(source, ctx.getSourceInterval(), replacement);
    }

    @Override
    public String toString() {
        return "{'" + this.source + "'(" + this.interval + ") => '" + this.replacement + "'}";
    }

    /**
     * Sort a list of replacements in their natural stream of replacement order.
     * @param replacements
     * @return
     */
    public static ArrayList<Replacement> sort(final Collection<Replacement> replacements) {

        final ArrayList<Replacement> sorted = new ArrayList<Replacement>(replacements);

        sorted.sort(new Comparator<Replacement>() {
            @Override
            public int compare(final Replacement a, final Replacement b) {
                return Integer.compare(a.interval.a, b.interval.b);
            }
        });

        return sorted;
    }

    /**
     * display the list of replacements in their natural replacement order
     * @param replacements
     * @return
     */
    public static String toString(final Collection<Replacement> replacements) {

        final ArrayList<Replacement> sorted = Replacement.sort(replacements);

        return sorted.toString();
    }
}
