package ParallelOctreeV2;

public class PointerOctree extends Octree {
    Octant root;

    // Initialize octree
    PointerOctree(int vertexLimit, double halfSize) {
        this.vertexLimit = vertexLimit;
        name = "Pointer based Octree";

        root = new Octant(null, new double[] { 0, 0, 0 }, halfSize);
    }

    // Find octant which contains vertex v
    @Override
    protected Octant find(Vertex v) {
        Octant curr = root;
        while (!curr.isLeaf) {
            // Based on morton code, calculates correct octant based on position from center
            int nextOctant = 0;
            if (v.x >= curr.center[0])
                nextOctant += 4;
            if (v.y >= curr.center[1])
                nextOctant += 2;
            if (v.z >= curr.center[2])
                nextOctant += 1;

            curr = (Octant) curr.children[nextOctant];
        }
        return curr;
    }

    // Find and insert
    @Override
    public boolean insert(Vertex v) {
        find(v).insert(v);

        // print();
        return true;
    }

    @Override
    public boolean remove(Vertex v) {
        return find(v).remove(v);
    }

    @Override
    public boolean contains(Vertex v) {
        return find(v).contains(v);
    }

    class Octant extends Octree.Octant {

        Octant(Octant parent, double center[], double halfSize) {
            this.parent = parent;
            this.center = center;
            this.halfSize = halfSize;
        }

        @Override
        public boolean insert(Vertex v) {
            // If limit was reached and new vertex is not a duplicate
            if (vertices.size() >= vertexLimit && !contains(v)) {
                vertices.add(v);
                subdivide();
            } else {
                vertices.add(v);
            }
            return true;
        }

        // Complete subdivision of octant into eight new octants
        @Override
        protected void subdivide() {
            isLeaf = false;
            double childHalfSize = halfSize / 2;

            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    for (int k = 0; k < 2; k++) {
                        children[i * 4 + j * 2 + k] = new Octant(this, new double[] {
                                center[0] - childHalfSize + i * halfSize,
                                center[1] - childHalfSize + j * halfSize,
                                center[2] - childHalfSize + k * halfSize,
                        }, childHalfSize);
                    }
                }
            }

            // Reinsert vertices to children
            for (var v : vertices) {
                int nextOctant = 0;
                if (v.x >= center[0])
                    nextOctant += 4;
                if (v.y >= center[1])
                    nextOctant += 2;
                if (v.z >= center[2])
                    nextOctant += 1;

                children[nextOctant].insert(v);
            }

            // Clear children
            vertices.clear();
        }

        @Override
        public boolean remove(Vertex v) {
            return vertices.remove(v);
        }

        // Check if vertex is a duplicate in the octant's vertices
        @Override
        public boolean contains(Vertex v) {
            for (var u : vertices)
                if (u.equals(v))
                    return true;

            return false;
        }
    }
}
