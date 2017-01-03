package org.janusgraph.graphdb.tinkerpop.optimize;

import com.google.common.collect.Iterables;
import org.janusgraph.core.BaseVertexQuery;
import org.janusgraph.core.JanusElement;
import org.janusgraph.core.JanusMultiVertexQuery;
import org.janusgraph.core.JanusVertex;
import org.janusgraph.core.JanusVertexQuery;
import org.janusgraph.graphdb.query.BaseQuery;
import org.janusgraph.graphdb.query.Query;
import org.janusgraph.graphdb.query.JanusPredicate;
import org.janusgraph.graphdb.query.profile.QueryProfiler;
import org.janusgraph.graphdb.query.vertex.BasicVertexCentricQueryBuilder;
import org.janusgraph.graphdb.tinkerpop.profile.TP3ProfileWrapper;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.step.Profiling;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.VertexStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.HasContainer;
import org.apache.tinkerpop.gremlin.process.traversal.util.FastNoSuchElementException;
import org.apache.tinkerpop.gremlin.process.traversal.util.MutableMetrics;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Matthias Broecheler (me@matthiasb.com)
 */
public class JanusVertexStep<E extends Element> extends VertexStep<E> implements HasStepFolder<Vertex, E>, Profiling, MultiQueriable<Vertex,E> {

    public JanusVertexStep(VertexStep<E> originalStep) {
        super(originalStep.getTraversal(), originalStep.getReturnClass(), originalStep.getDirection(), originalStep.getEdgeLabels());
        originalStep.getLabels().forEach(this::addLabel);
        this.hasContainers = new ArrayList<>();
        this.limit = Query.NO_LIMIT;
    }

    private boolean initialized = false;
    private boolean useMultiQuery = false;
    private Map<JanusVertex, Iterable<? extends JanusElement>> multiQueryResults = null;
    private QueryProfiler queryProfiler = QueryProfiler.NO_OP;

    @Override
    public void setUseMultiQuery(boolean useMultiQuery) {
        this.useMultiQuery = useMultiQuery;
    }

    public <Q extends BaseVertexQuery> Q makeQuery(Q query) {
        query.labels(getEdgeLabels());
        query.direction(getDirection());
        for (HasContainer condition : hasContainers) {
            query.has(condition.getKey(), JanusPredicate.Converter.convert(condition.getBiPredicate()), condition.getValue());
        }
        for (OrderEntry order : orders) query.orderBy(order.key, order.order);
        if (limit != BaseQuery.NO_LIMIT) query.limit(limit);
        ((BasicVertexCentricQueryBuilder) query).profiler(queryProfiler);
        return query;
    }

    @SuppressWarnings("deprecation")
    private void initialize() {
        assert !initialized;
        initialized = true;
        if (useMultiQuery) {
            if (!starts.hasNext()) throw FastNoSuchElementException.instance();
            JanusMultiVertexQuery mquery = JanusTraversalUtil.getTx(traversal).multiQuery();
            List<Traverser.Admin<Vertex>> vertices = new ArrayList<>();
            starts.forEachRemaining(v -> {
                vertices.add(v);
                mquery.addVertex(v.get());
            });
            starts.add(vertices.iterator());
            assert vertices.size() > 0;
            makeQuery(mquery);

            multiQueryResults = (Vertex.class.isAssignableFrom(getReturnClass())) ? mquery.vertices() : mquery.edges();
        }
    }

    @Override
    protected Traverser<E> processNextStart() {
        if (!initialized) initialize();
        return super.processNextStart();
    }

    @Override
    protected Iterator<E> flatMap(final Traverser.Admin<Vertex> traverser) {
        if (useMultiQuery) {
            assert multiQueryResults != null;
            return (Iterator<E>) multiQueryResults.get(traverser.get()).iterator();
        } else {
            JanusVertexQuery query = makeQuery((JanusTraversalUtil.getJanusVertex(traverser)).query());
            return (Vertex.class.isAssignableFrom(getReturnClass())) ? query.vertices().iterator() : query.edges().iterator();
        }
    }

    @Override
    public void reset() {
        super.reset();
        this.initialized = false;
    }

    @Override
    public JanusVertexStep<E> clone() {
        final JanusVertexStep<E> clone = (JanusVertexStep<E>) super.clone();
        clone.initialized = false;
        return clone;
    }

    /*
    ===== HOLDER =====
     */

    private final List<HasContainer> hasContainers;
    private int limit = BaseQuery.NO_LIMIT;
    private List<OrderEntry> orders = new ArrayList<>();


    @Override
    public void addAll(Iterable<HasContainer> has) {
        Iterables.addAll(hasContainers, has);
    }

    @Override
    public void orderBy(String key, Order order) {
        orders.add(new OrderEntry(key, order));
    }

    @Override
    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public int getLimit() {
        return this.limit;
    }

    @Override
    public String toString() {
        return this.hasContainers.isEmpty() ? super.toString() : StringFactory.stepString(this, this.hasContainers);
    }

    @Override
    public void setMetrics(MutableMetrics metrics) {
        queryProfiler = new TP3ProfileWrapper(metrics);
    }
}