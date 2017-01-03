package org.janusgraph.graphdb.query.vertex;

import com.google.common.base.Preconditions;
import org.janusgraph.core.JanusRelation;
import org.janusgraph.diskstorage.keycolumnvalue.SliceQuery;
import org.janusgraph.graphdb.internal.InternalVertex;
import org.janusgraph.graphdb.internal.OrderList;
import org.janusgraph.graphdb.query.BackendQueryHolder;
import org.janusgraph.graphdb.query.ElementQuery;
import org.janusgraph.graphdb.query.condition.Condition;
import org.janusgraph.graphdb.relations.RelationComparator;
import org.apache.tinkerpop.gremlin.structure.Direction;

import java.util.Comparator;
import java.util.List;

/**
 * A vertex-centric query which implements {@link ElementQuery} so that it can be executed by
 * {@link org.janusgraph.graphdb.query.QueryProcessor}. Most of the query definition
 * is in the extended {@link BaseVertexCentricQuery} - this class only adds the base vertex to the mix.
 *
 * @author Matthias Broecheler (me@matthiasb.com)
 */
public class VertexCentricQuery extends BaseVertexCentricQuery implements ElementQuery<JanusRelation, SliceQuery> {

    private final InternalVertex vertex;

    public VertexCentricQuery(InternalVertex vertex, Condition<JanusRelation> condition,
                              Direction direction,
                              List<BackendQueryHolder<SliceQuery>> queries,
                              OrderList orders,
                              int limit) {
        super(condition, direction, queries, orders, limit);
        Preconditions.checkNotNull(vertex);
        this.vertex = vertex;
    }

    public VertexCentricQuery(InternalVertex vertex, BaseVertexCentricQuery base) {
        super(base);
        Preconditions.checkNotNull(vertex);
        this.vertex = vertex;
    }

    /**
     * Constructs an empty query
     * @param vertex
     */
    protected VertexCentricQuery(InternalVertex vertex) {
        super();
        Preconditions.checkNotNull(vertex);
        this.vertex = vertex;
    }

    public static VertexCentricQuery emptyQuery(InternalVertex vertex) {
        return new VertexCentricQuery(vertex);
    }

    public InternalVertex getVertex() {
        return vertex;
    }

    @Override
    public boolean isSorted() {
        return true;
    }

    @Override
    public Comparator getSortOrder() {
        return new RelationComparator(vertex,getOrders());
    }

    @Override
    public boolean hasDuplicateResults() {
        return false; //We wanna count self-loops twice
    }

    @Override
    public String toString() {
        return vertex+super.toString();
    }

}
