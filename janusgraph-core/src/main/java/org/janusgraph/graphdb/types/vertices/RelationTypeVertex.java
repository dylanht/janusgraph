package org.janusgraph.graphdb.types.vertices;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.janusgraph.core.schema.ConsistencyModifier;
import org.janusgraph.core.Multiplicity;
import org.janusgraph.graphdb.internal.Order;
import org.janusgraph.graphdb.internal.InternalRelationType;
import org.janusgraph.graphdb.transaction.StandardJanusTx;
import org.janusgraph.graphdb.types.IndexType;
import org.janusgraph.graphdb.types.SchemaSource;
import org.janusgraph.graphdb.types.TypeDefinitionCategory;
import org.janusgraph.graphdb.types.TypeUtil;
import org.apache.tinkerpop.gremlin.structure.Direction;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Matthias Broecheler (me@matthiasb.com)
 */
public abstract class RelationTypeVertex extends JanusSchemaVertex implements InternalRelationType {

    public RelationTypeVertex(StandardJanusTx tx, long id, byte lifecycle) {
        super(tx, id, lifecycle);
    }

    @Override
    public long[] getSortKey() {
        return getDefinition().getValue(TypeDefinitionCategory.SORT_KEY, long[].class);
    }

    @Override
    public Order getSortOrder() {
        return getDefinition().getValue(TypeDefinitionCategory.SORT_ORDER, Order.class);
    }

    @Override
    public long[] getSignature() {
        return getDefinition().getValue(TypeDefinitionCategory.SIGNATURE, long[].class);
    }

    @Override
    public boolean isInvisibleType() {
        return getDefinition().getValue(TypeDefinitionCategory.INVISIBLE, Boolean.class);
    }

    @Override
    public Multiplicity multiplicity() {
        return getDefinition().getValue(TypeDefinitionCategory.MULTIPLICITY, Multiplicity.class);
    }

    private ConsistencyModifier consistency = null;

    public ConsistencyModifier getConsistencyModifier() {
        if (consistency==null) {
            consistency = TypeUtil.getConsistencyModifier(this);
        }
        return consistency;
    }

    private Integer ttl = null;

    @Override
    public Integer getTTL() {
        if (null == ttl) {
            ttl = TypeUtil.getTTL(this);
        }
        return ttl;
    }

    public InternalRelationType getBaseType() {
        Entry entry = Iterables.getOnlyElement(getRelated(TypeDefinitionCategory.RELATIONTYPE_INDEX,Direction.IN),null);
        if (entry==null) return null;
        assert entry.getSchemaType() instanceof InternalRelationType;
        return (InternalRelationType)entry.getSchemaType();
    }

    public Iterable<InternalRelationType> getRelationIndexes() {
        return Iterables.concat(ImmutableList.of(this),Iterables.transform(getRelated(TypeDefinitionCategory.RELATIONTYPE_INDEX,Direction.OUT),new Function<Entry, InternalRelationType>() {
            @Nullable
            @Override
            public InternalRelationType apply(@Nullable Entry entry) {
                assert entry.getSchemaType() instanceof InternalRelationType;
                return (InternalRelationType)entry.getSchemaType();
            }
        }));
    }

    private List<IndexType> indexes = null;

    public Iterable<IndexType> getKeyIndexes() {
        List<IndexType> result = indexes;
        if (result==null) {
            ImmutableList.Builder<IndexType> b = ImmutableList.builder();
            for (Entry entry : getRelated(TypeDefinitionCategory.INDEX_FIELD,Direction.IN)) {
                SchemaSource index = entry.getSchemaType();
                b.add(index.asIndexType());
            }
            result = b.build();
            indexes=result;
        }
        assert result!=null;
        return result;
    }

    public void resetCache() {
        super.resetCache();
        indexes=null;
    }
}
