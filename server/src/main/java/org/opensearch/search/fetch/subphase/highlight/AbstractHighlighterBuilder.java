/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.search.fetch.subphase.highlight;

import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.opensearch.common.ParseField;
import org.opensearch.common.ParsingException;
import org.opensearch.common.Strings;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.common.xcontent.ObjectParser;
import org.opensearch.common.xcontent.ToXContentObject;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.Rewriteable;
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder.BoundaryScannerType;
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder.Order;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import static org.opensearch.common.xcontent.ObjectParser.fromList;
import static org.opensearch.index.query.AbstractQueryBuilder.parseInnerQueryBuilder;

/**
 * This abstract class holds parameters shared by {@link HighlightBuilder} and {@link HighlightBuilder.Field}
 * and provides the common setters, equality, hashCode calculation and common serialization
 *
 * @opensearch.internal
 */
public abstract class AbstractHighlighterBuilder<HB extends AbstractHighlighterBuilder<?>>
    implements
        Writeable,
        Rewriteable<HB>,
        ToXContentObject {
    public static final ParseField PRE_TAGS_FIELD = new ParseField("pre_tags");
    public static final ParseField POST_TAGS_FIELD = new ParseField("post_tags");
    public static final ParseField FIELDS_FIELD = new ParseField("fields");
    public static final ParseField ORDER_FIELD = new ParseField("order");
    public static final ParseField HIGHLIGHT_FILTER_FIELD = new ParseField("highlight_filter");
    public static final ParseField FRAGMENT_SIZE_FIELD = new ParseField("fragment_size");
    public static final ParseField FRAGMENT_OFFSET_FIELD = new ParseField("fragment_offset");
    public static final ParseField NUMBER_OF_FRAGMENTS_FIELD = new ParseField("number_of_fragments");
    public static final ParseField ENCODER_FIELD = new ParseField("encoder");
    public static final ParseField REQUIRE_FIELD_MATCH_FIELD = new ParseField("require_field_match");
    public static final ParseField BOUNDARY_SCANNER_FIELD = new ParseField("boundary_scanner");
    public static final ParseField BOUNDARY_MAX_SCAN_FIELD = new ParseField("boundary_max_scan");
    public static final ParseField BOUNDARY_CHARS_FIELD = new ParseField("boundary_chars");
    public static final ParseField BOUNDARY_SCANNER_LOCALE_FIELD = new ParseField("boundary_scanner_locale");
    public static final ParseField TYPE_FIELD = new ParseField("type");
    public static final ParseField FRAGMENTER_FIELD = new ParseField("fragmenter");
    public static final ParseField NO_MATCH_SIZE_FIELD = new ParseField("no_match_size");
    public static final ParseField FORCE_SOURCE_FIELD = new ParseField("force_source");
    public static final ParseField PHRASE_LIMIT_FIELD = new ParseField("phrase_limit");
    public static final ParseField OPTIONS_FIELD = new ParseField("options");
    public static final ParseField HIGHLIGHT_QUERY_FIELD = new ParseField("highlight_query");
    public static final ParseField MATCHED_FIELDS_FIELD = new ParseField("matched_fields");

    protected String[] preTags;

    protected String[] postTags;

    protected Integer fragmentSize;

    protected Integer numOfFragments;

    protected String highlighterType;

    protected String fragmenter;

    protected QueryBuilder highlightQuery;

    protected Order order;

    protected Boolean highlightFilter;

    protected Boolean forceSource;

    protected BoundaryScannerType boundaryScannerType;

    protected Integer boundaryMaxScan;

    protected char[] boundaryChars;

    protected Locale boundaryScannerLocale;

    protected Integer noMatchSize;

    protected Integer phraseLimit;

    protected Map<String, Object> options;

    protected Boolean requireFieldMatch;

    public AbstractHighlighterBuilder() {}

    protected AbstractHighlighterBuilder(AbstractHighlighterBuilder<?> template, QueryBuilder queryBuilder) {
        preTags = template.preTags;
        postTags = template.postTags;
        fragmentSize = template.fragmentSize;
        numOfFragments = template.numOfFragments;
        highlighterType = template.highlighterType;
        fragmenter = template.fragmenter;
        highlightQuery = queryBuilder;
        order = template.order;
        highlightFilter = template.highlightFilter;
        forceSource = template.forceSource;
        boundaryScannerType = template.boundaryScannerType;
        boundaryMaxScan = template.boundaryMaxScan;
        boundaryChars = template.boundaryChars;
        boundaryScannerLocale = template.boundaryScannerLocale;
        noMatchSize = template.noMatchSize;
        phraseLimit = template.phraseLimit;
        options = template.options;
        requireFieldMatch = template.requireFieldMatch;
    }

    /**
     * Read from a stream.
     */
    protected AbstractHighlighterBuilder(StreamInput in) throws IOException {
        preTags(in.readOptionalStringArray());
        postTags(in.readOptionalStringArray());
        fragmentSize(in.readOptionalVInt());
        numOfFragments(in.readOptionalVInt());
        highlighterType(in.readOptionalString());
        fragmenter(in.readOptionalString());
        if (in.readBoolean()) {
            highlightQuery(in.readNamedWriteable(QueryBuilder.class));
        }
        order(in.readOptionalWriteable(Order::readFromStream));
        highlightFilter(in.readOptionalBoolean());
        forceSource(in.readOptionalBoolean());
        boundaryScannerType(in.readOptionalWriteable(BoundaryScannerType::readFromStream));
        boundaryMaxScan(in.readOptionalVInt());
        if (in.readBoolean()) {
            boundaryChars(in.readString().toCharArray());
        }
        if (in.readBoolean()) {
            boundaryScannerLocale(in.readString());
        }
        noMatchSize(in.readOptionalVInt());
        phraseLimit(in.readOptionalVInt());
        if (in.readBoolean()) {
            options(in.readMap());
        }
        requireFieldMatch(in.readOptionalBoolean());
    }

    /**
     * write common parameters to {@link StreamOutput}
     */
    @Override
    public final void writeTo(StreamOutput out) throws IOException {
        out.writeOptionalStringArray(preTags);
        out.writeOptionalStringArray(postTags);
        out.writeOptionalVInt(fragmentSize);
        out.writeOptionalVInt(numOfFragments);
        out.writeOptionalString(highlighterType);
        out.writeOptionalString(fragmenter);
        boolean hasQuery = highlightQuery != null;
        out.writeBoolean(hasQuery);
        if (hasQuery) {
            out.writeNamedWriteable(highlightQuery);
        }
        out.writeOptionalWriteable(order);
        out.writeOptionalBoolean(highlightFilter);
        out.writeOptionalBoolean(forceSource);
        out.writeOptionalWriteable(boundaryScannerType);
        out.writeOptionalVInt(boundaryMaxScan);
        boolean hasBounaryChars = boundaryChars != null;
        out.writeBoolean(hasBounaryChars);
        if (hasBounaryChars) {
            out.writeString(String.valueOf(boundaryChars));
        }
        boolean hasBoundaryScannerLocale = boundaryScannerLocale != null;
        out.writeBoolean(hasBoundaryScannerLocale);
        if (hasBoundaryScannerLocale) {
            out.writeString(boundaryScannerLocale.toLanguageTag());
        }
        out.writeOptionalVInt(noMatchSize);
        out.writeOptionalVInt(phraseLimit);
        boolean hasOptions = options != null;
        out.writeBoolean(hasOptions);
        if (hasOptions) {
            out.writeMap(options);
        }
        out.writeOptionalBoolean(requireFieldMatch);
        doWriteTo(out);
    }

    protected abstract void doWriteTo(StreamOutput out) throws IOException;

    /**
     * Set the pre tags that will be used for highlighting.
     */
    @SuppressWarnings("unchecked")
    public HB preTags(String... preTags) {
        this.preTags = preTags;
        return (HB) this;
    }

    /**
     * @return the value set by {@link #preTags(String...)}
     */
    public String[] preTags() {
        return this.preTags;
    }

    /**
     * Set the post tags that will be used for highlighting.
     */
    @SuppressWarnings("unchecked")
    public HB postTags(String... postTags) {
        this.postTags = postTags;
        return (HB) this;
    }

    /**
     * @return the value set by {@link #postTags(String...)}
     */
    public String[] postTags() {
        return this.postTags;
    }

    /**
     * Set the fragment size in characters, defaults to {@link HighlightBuilder#DEFAULT_FRAGMENT_CHAR_SIZE}
     */
    @SuppressWarnings("unchecked")
    public HB fragmentSize(Integer fragmentSize) {
        this.fragmentSize = fragmentSize;
        return (HB) this;
    }

    /**
     * @return the value set by {@link #fragmentSize(Integer)}
     */
    public Integer fragmentSize() {
        return this.fragmentSize;
    }

    /**
     * Set the number of fragments, defaults to {@link HighlightBuilder#DEFAULT_NUMBER_OF_FRAGMENTS}
     */
    @SuppressWarnings("unchecked")
    public HB numOfFragments(Integer numOfFragments) {
        this.numOfFragments = numOfFragments;
        return (HB) this;
    }

    /**
     * @return the value set by {@link #numOfFragments(Integer)}
     */
    public Integer numOfFragments() {
        return this.numOfFragments;
    }

    /**
     * Set type of highlighter to use. Out of the box supported types
     * are {@code unified}, {@code plain} and {@code fvh}.
     * Defaults to {@code unified}.
     * Details of the different highlighter types are covered in the reference guide.
     */
    @SuppressWarnings("unchecked")
    public HB highlighterType(String highlighterType) {
        this.highlighterType = highlighterType;
        return (HB) this;
    }

    /**
     * @return the value set by {@link #highlighterType(String)}
     */
    public String highlighterType() {
        return this.highlighterType;
    }

    /**
     * Sets what fragmenter to use to break up text that is eligible for highlighting.
     * This option is only applicable when using the plain highlighterType {@code highlighter}.
     * Permitted values are "simple" or "span" relating to {@link SimpleFragmenter} and
     * {@link SimpleSpanFragmenter} implementations respectively with the default being "span"
     */
    @SuppressWarnings("unchecked")
    public HB fragmenter(String fragmenter) {
        this.fragmenter = fragmenter;
        return (HB) this;
    }

    /**
     * @return the value set by {@link #fragmenter(String)}
     */
    public String fragmenter() {
        return this.fragmenter;
    }

    /**
     * Sets a query to be used for highlighting instead of the search query.
     */
    @SuppressWarnings("unchecked")
    public HB highlightQuery(QueryBuilder highlightQuery) {
        this.highlightQuery = highlightQuery;
        return (HB) this;
    }

    /**
     * @return the value set by {@link #highlightQuery(QueryBuilder)}
     */
    public QueryBuilder highlightQuery() {
        return this.highlightQuery;
    }

    /**
     * The order of fragments per field. By default, ordered by the order in the
     * highlighted text. Can be {@code score}, which then it will be ordered
     * by score of the fragments, or {@code none}.
     */
    public HB order(String order) {
        return order(Order.fromString(order));
    }

    /**
     * By default, fragments of a field are ordered by the order in the highlighted text.
     * If set to {@link Order#SCORE}, this changes order to score of the fragments.
     */
    @SuppressWarnings("unchecked")
    public HB order(Order scoreOrdered) {
        this.order = scoreOrdered;
        return (HB) this;
    }

    /**
     * @return the value set by {@link #order(Order)}
     */
    public Order order() {
        return this.order;
    }

    /**
     * Set this to true when using the highlighterType {@code fvh}
     * and you want to provide highlighting on filter clauses in your
     * query. Default is {@code false}.
     */
    @SuppressWarnings("unchecked")
    public HB highlightFilter(Boolean highlightFilter) {
        this.highlightFilter = highlightFilter;
        return (HB) this;
    }

    /**
     * @return the value set by {@link #highlightFilter(Boolean)}
     */
    public Boolean highlightFilter() {
        return this.highlightFilter;
    }

    /**
     * When using the highlighterType {@code fvh} this setting
     * controls which scanner to use for fragment boundaries, and defaults to "simple".
     */
    @SuppressWarnings("unchecked")
    public HB boundaryScannerType(String boundaryScannerType) {
        this.boundaryScannerType = BoundaryScannerType.fromString(boundaryScannerType);
        return (HB) this;
    }

    /**
     * When using the highlighterType {@code fvh} this setting
     * controls which scanner to use for fragment boundaries, and defaults to "simple".
     */
    @SuppressWarnings("unchecked")
    public HB boundaryScannerType(BoundaryScannerType boundaryScannerType) {
        this.boundaryScannerType = boundaryScannerType;
        return (HB) this;
    }

    /**
     * @return the value set by {@link #boundaryScannerType(String)}
     */
    public BoundaryScannerType boundaryScannerType() {
        return this.boundaryScannerType;
    }

    /**
     * When using the highlighterType {@code fvh} this setting
     * controls how far to look for boundary characters, and defaults to 20.
     */
    @SuppressWarnings("unchecked")
    public HB boundaryMaxScan(Integer boundaryMaxScan) {
        this.boundaryMaxScan = boundaryMaxScan;
        return (HB) this;
    }

    /**
     * @return the value set by {@link #boundaryMaxScan(Integer)}
     */
    public Integer boundaryMaxScan() {
        return this.boundaryMaxScan;
    }

    /**
     * When using the highlighterType {@code fvh} this setting
     * defines what constitutes a boundary for highlighting. It’s a single string with
     * each boundary character defined in it. It defaults to .,!? \t\n
     */
    @SuppressWarnings("unchecked")
    public HB boundaryChars(char[] boundaryChars) {
        this.boundaryChars = boundaryChars;
        return (HB) this;
    }

    /**
     * @return the value set by {@link #boundaryChars(char[])}
     */
    public char[] boundaryChars() {
        return this.boundaryChars;
    }

    /**
     * When using the highlighterType {@code fvh} and boundaryScannerType {@code break_iterator}, this setting
     * controls the locale to use by the BreakIterator, defaults to "root".
     */
    @SuppressWarnings("unchecked")
    public HB boundaryScannerLocale(String boundaryScannerLocale) {
        if (boundaryScannerLocale != null) {
            this.boundaryScannerLocale = Locale.forLanguageTag(boundaryScannerLocale);
        }
        return (HB) this;
    }

    /**
     * @return the value set by {@link #boundaryScannerLocale(String)}
     */
    public Locale boundaryScannerLocale() {
        return this.boundaryScannerLocale;
    }

    /**
     * Allows to set custom options for custom highlighters.
     */
    @SuppressWarnings("unchecked")
    public HB options(Map<String, Object> options) {
        this.options = options;
        return (HB) this;
    }

    /**
     * @return the value set by {@link #options(Map)}
     */
    public Map<String, Object> options() {
        return this.options;
    }

    /**
     * Set to true to cause a field to be highlighted only if a query matches that field.
     * Default is false meaning that terms are highlighted on all requested fields regardless
     * if the query matches specifically on them.
     */
    @SuppressWarnings("unchecked")
    public HB requireFieldMatch(Boolean requireFieldMatch) {
        this.requireFieldMatch = requireFieldMatch;
        return (HB) this;
    }

    /**
     * @return the value set by {@link #requireFieldMatch(Boolean)}
     */
    public Boolean requireFieldMatch() {
        return this.requireFieldMatch;
    }

    /**
     * Sets the size of the fragment to return from the beginning of the field if there are no matches to
     * highlight and the field doesn't also define noMatchSize.
     * @param noMatchSize integer to set or null to leave out of request.  default is null.
     * @return this for chaining
     */
    @SuppressWarnings("unchecked")
    public HB noMatchSize(Integer noMatchSize) {
        this.noMatchSize = noMatchSize;
        return (HB) this;
    }

    /**
     * @return the value set by {@link #noMatchSize(Integer)}
     */
    public Integer noMatchSize() {
        return this.noMatchSize;
    }

    /**
     * Sets the maximum number of phrases the fvh will consider if the field doesn't also define phraseLimit.
     * @param phraseLimit maximum number of phrases the fvh will consider
     * @return this for chaining
     */
    @SuppressWarnings("unchecked")
    public HB phraseLimit(Integer phraseLimit) {
        this.phraseLimit = phraseLimit;
        return (HB) this;
    }

    /**
     * @return the value set by {@link #phraseLimit(Integer)}
     */
    public Integer phraseLimit() {
        return this.phraseLimit;
    }

    /**
     * Forces the highlighting to highlight fields based on the source even if fields are stored separately.
     */
    @SuppressWarnings("unchecked")
    public HB forceSource(Boolean forceSource) {
        this.forceSource = forceSource;
        return (HB) this;
    }

    /**
     * @return the value set by {@link #forceSource(Boolean)}
     */
    public Boolean forceSource() {
        return this.forceSource;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        innerXContent(builder);
        builder.endObject();
        return builder;
    }

    protected abstract void innerXContent(XContentBuilder builder) throws IOException;

    void commonOptionsToXContent(XContentBuilder builder) throws IOException {
        if (preTags != null) {
            builder.array(PRE_TAGS_FIELD.getPreferredName(), preTags);
        }
        if (postTags != null) {
            builder.array(POST_TAGS_FIELD.getPreferredName(), postTags);
        }
        if (fragmentSize != null) {
            builder.field(FRAGMENT_SIZE_FIELD.getPreferredName(), fragmentSize);
        }
        if (numOfFragments != null) {
            builder.field(NUMBER_OF_FRAGMENTS_FIELD.getPreferredName(), numOfFragments);
        }
        if (highlighterType != null) {
            builder.field(TYPE_FIELD.getPreferredName(), highlighterType);
        }
        if (fragmenter != null) {
            builder.field(FRAGMENTER_FIELD.getPreferredName(), fragmenter);
        }
        if (highlightQuery != null) {
            builder.field(HIGHLIGHT_QUERY_FIELD.getPreferredName(), highlightQuery);
        }
        if (order != null) {
            builder.field(ORDER_FIELD.getPreferredName(), order.toString());
        }
        if (highlightFilter != null) {
            builder.field(HIGHLIGHT_FILTER_FIELD.getPreferredName(), highlightFilter);
        }
        if (boundaryScannerType != null) {
            builder.field(BOUNDARY_SCANNER_FIELD.getPreferredName(), boundaryScannerType.name());
        }
        if (boundaryMaxScan != null) {
            builder.field(BOUNDARY_MAX_SCAN_FIELD.getPreferredName(), boundaryMaxScan);
        }
        if (boundaryChars != null) {
            builder.field(BOUNDARY_CHARS_FIELD.getPreferredName(), new String(boundaryChars));
        }
        if (boundaryScannerLocale != null) {
            builder.field(BOUNDARY_SCANNER_LOCALE_FIELD.getPreferredName(), boundaryScannerLocale.toLanguageTag());
        }
        if (options != null && options.size() > 0) {
            builder.field(OPTIONS_FIELD.getPreferredName(), options);
        }
        if (forceSource != null) {
            builder.field(FORCE_SOURCE_FIELD.getPreferredName(), forceSource);
        }
        if (requireFieldMatch != null) {
            builder.field(REQUIRE_FIELD_MATCH_FIELD.getPreferredName(), requireFieldMatch);
        }
        if (noMatchSize != null) {
            builder.field(NO_MATCH_SIZE_FIELD.getPreferredName(), noMatchSize);
        }
        if (phraseLimit != null) {
            builder.field(PHRASE_LIMIT_FIELD.getPreferredName(), phraseLimit);
        }
    }

    static <HB extends AbstractHighlighterBuilder<HB>> BiFunction<XContentParser, HB, HB> setupParser(ObjectParser<HB, Void> parser) {
        parser.declareStringArray(fromList(String.class, HB::preTags), PRE_TAGS_FIELD);
        parser.declareStringArray(fromList(String.class, HB::postTags), POST_TAGS_FIELD);
        parser.declareString(HB::order, ORDER_FIELD);
        parser.declareBoolean(HB::highlightFilter, HIGHLIGHT_FILTER_FIELD);
        parser.declareInt(HB::fragmentSize, FRAGMENT_SIZE_FIELD);
        parser.declareInt(HB::numOfFragments, NUMBER_OF_FRAGMENTS_FIELD);
        parser.declareBoolean(HB::requireFieldMatch, REQUIRE_FIELD_MATCH_FIELD);
        parser.declareString(HB::boundaryScannerType, BOUNDARY_SCANNER_FIELD);
        parser.declareInt(HB::boundaryMaxScan, BOUNDARY_MAX_SCAN_FIELD);
        parser.declareString((HB hb, String bc) -> hb.boundaryChars(bc.toCharArray()), BOUNDARY_CHARS_FIELD);
        parser.declareString(HB::boundaryScannerLocale, BOUNDARY_SCANNER_LOCALE_FIELD);
        parser.declareString(HB::highlighterType, TYPE_FIELD);
        parser.declareString(HB::fragmenter, FRAGMENTER_FIELD);
        parser.declareInt(HB::noMatchSize, NO_MATCH_SIZE_FIELD);
        parser.declareBoolean(HB::forceSource, FORCE_SOURCE_FIELD);
        parser.declareInt(HB::phraseLimit, PHRASE_LIMIT_FIELD);
        parser.declareObject(HB::options, (XContentParser p, Void c) -> {
            try {
                return p.map();
            } catch (IOException e) {
                throw new RuntimeException("Error parsing options", e);
            }
        }, OPTIONS_FIELD);
        parser.declareObject(HB::highlightQuery, (XContentParser p, Void c) -> {
            try {
                return parseInnerQueryBuilder(p);
            } catch (IOException e) {
                throw new RuntimeException("Error parsing query", e);
            }
        }, HIGHLIGHT_QUERY_FIELD);
        return (XContentParser p, HB hb) -> {
            try {
                parser.parse(p, hb, null);
                if (hb.preTags() != null && hb.postTags() == null) {
                    throw new ParsingException(p.getTokenLocation(), "pre_tags are set but post_tags are not set");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return hb;
        };
    }

    @Override
    public final int hashCode() {
        return Objects.hash(
            getClass(),
            Arrays.hashCode(preTags),
            Arrays.hashCode(postTags),
            fragmentSize,
            numOfFragments,
            highlighterType,
            fragmenter,
            highlightQuery,
            order,
            highlightFilter,
            forceSource,
            boundaryScannerType,
            boundaryMaxScan,
            Arrays.hashCode(boundaryChars),
            boundaryScannerLocale,
            noMatchSize,
            phraseLimit,
            options,
            requireFieldMatch,
            doHashCode()
        );
    }

    /**
     * fields only present in subclass should contribute to hashCode in the implementation
     */
    protected abstract int doHashCode();

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        HB other = (HB) obj;
        return Arrays.equals(preTags, other.preTags)
            && Arrays.equals(postTags, other.postTags)
            && Objects.equals(fragmentSize, other.fragmentSize)
            && Objects.equals(numOfFragments, other.numOfFragments)
            && Objects.equals(highlighterType, other.highlighterType)
            && Objects.equals(fragmenter, other.fragmenter)
            && Objects.equals(highlightQuery, other.highlightQuery)
            && Objects.equals(order, other.order)
            && Objects.equals(highlightFilter, other.highlightFilter)
            && Objects.equals(forceSource, other.forceSource)
            && Objects.equals(boundaryScannerType, other.boundaryScannerType)
            && Objects.equals(boundaryMaxScan, other.boundaryMaxScan)
            && Arrays.equals(boundaryChars, other.boundaryChars)
            && Objects.equals(boundaryScannerLocale, other.boundaryScannerLocale)
            && Objects.equals(noMatchSize, other.noMatchSize)
            && Objects.equals(phraseLimit, other.phraseLimit)
            && Objects.equals(options, other.options)
            && Objects.equals(requireFieldMatch, other.requireFieldMatch)
            && doEquals(other);
    }

    /**
     * fields only present in subclass should be checked for equality in the implementation
     */
    protected abstract boolean doEquals(HB other);

    @Override
    public String toString() {
        return Strings.toString(this, true, true);
    }
}
