package org.janusgraph.graphdb.query.condition;

import org.janusgraph.core.JanusEdge;
import org.janusgraph.core.JanusVertexProperty;
import org.janusgraph.core.JanusRelation;
import org.janusgraph.core.JanusVertex;
import org.janusgraph.graphdb.relations.CacheEdge;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * @author Matthias Broecheler (me@matthiasb.com)
 */

public class DirectionCondition<E extends JanusRelation> extends Literal<E> {

    private final JanusVertex baseVertex;
    private final Direction direction;

    public DirectionCondition(JanusVertex vertex, Direction dir) {
        assert vertex != null && dir != null;
        this.baseVertex = vertex;
        this.direction = dir;
    }

    @Override
    public boolean evaluate(E element) {
        if (direction==Direction.BOTH) return true;
        if (element instanceof CacheEdge) {
            return direction==((CacheEdge)element).getVertexCentricDirection();
        } else if (element instanceof JanusEdge) {
            return ((JanusEdge)element).vertex(direction).equals(baseVertex);
        } else if (element instanceof JanusVertexProperty) {
            return direction==Direction.OUT;
        }
        return false;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getType()).append(direction).append(baseVertex).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (other == null || !getClass().isInstance(other))
            return false;

        DirectionCondition oth = (DirectionCondition)other;
        return direction == oth.direction && baseVertex.equals(oth.baseVertex);
    }

    @Override
    public String toString() {
        return "dir["+getDirection()+"]";
    }
}
