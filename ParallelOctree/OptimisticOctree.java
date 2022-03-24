package ParallelOctree;

import java.util.concurrent.locks.ReentrantLock;

public class OptimisticOctree extends Octree {
    volatile Octant root = new Octant(null, new double[] { 0, 0, 0 }, 0.5);

    // Initialize octree
    OptimisticOctree(int vertexLimit) {
        this.vertexLimit = vertexLimit;
        name = "Optimistic synchronized Octree";
    }

    // Resize octree to fit new vertex
    @Override
    protected synchronized void resize(Vertex v) {
        root.lock.lock();
        Octant thisRoot = root;
        try {
            // Continually check if vertex is out of bounds and resize until it is contained
            boolean oob = true;
            while (oob) {

                oob = false;
                int oobDir[] = new int[] {
                        v.x < root.center[0] ? -1 : 1,
                        v.y < root.center[1] ? -1 : 1,
                        v.z < root.center[2] ? -1 : 1 };

                if (!root.inBounds(v)) {
                    oob = true;
                }

                // If vertex is out of bounds resize octree in direction of vertex
                if (oob) {

                    // Calculate center of new root octant
                    double newHalfSize = root.halfSize * 2;
                    double newCenter[] = new double[] {
                            root.center[0] + oobDir[0] * root.halfSize,
                            root.center[1] + oobDir[1] * root.halfSize,
                            root.center[2] + oobDir[2] * root.halfSize,
                    };

                    // Initialize new root
                    Octant newRoot = new Octant(null, newCenter, newHalfSize);
                    newRoot.isLeaf = false; // New root is not a leaf
                    root.parent = newRoot; // Set current root's parent to new root

                    /*
                     * Find current roots position inside the new root
                     *
                     * We expand towards the point, so we want the old
                     * root's position to be the farthest octant from
                     * the new vertex.
                     *
                     * 1. Flip direction of out of bounds
                     * 2. Add 1 and divide by 2 to get either 0 or 1
                     * 3. Then multiply to get morton code equivalent position
                     */
                    int rootPos = ((-oobDir[0] + 1) / 2) * 4
                            + ((-oobDir[1] + 1) / 2) * 2
                            + ((-oobDir[2] + 1) / 2);
                    newRoot.children[rootPos] = root;

                    // Initialize other child octants
                    for (int i = 0; i < 2; i++) {
                        for (int j = 0; j < 2; j++) {
                            for (int k = 0; k < 2; k++) {
                                int childCode = i * 4 + j * 2 + k;

                                // Skip current root
                                if (childCode == rootPos)
                                    continue;

                                // Calculate center of and initialize each new octant
                                newRoot.children[childCode] = new Octant(newRoot, new double[] {
                                        newCenter[0] - root.halfSize + i * newHalfSize,
                                        newCenter[1] - root.halfSize + j * newHalfSize,
                                        newCenter[2] - root.halfSize + k * newHalfSize,
                                }, root.halfSize);
                            }
                        }
                    }

                    // Set octree root to new root
                    newRoot.lock.lock();
                    root = newRoot;
                    thisRoot.lock.unlock();
                    thisRoot = root;
                }
            }
        } finally {
            thisRoot.lock.unlock();
        }
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

        // Check if resize is necessary
        // if (!root.inBounds(v)) {
        resize(v);
        // }

        while (true) {
            Octant o = find(v);
            o.lock.lock();
            try {
                // Validate
                if (o.isLeaf) {
                    return o.insert(v);
                }
            } finally {
                o.lock.unlock();
            }
        }
    }

    // Find and remove
    @Override
    public boolean remove(Vertex v) {

        return find(v).remove(v);

    }

    @Override
    public boolean contains(Vertex v) {

        return find(v).contains(v);

    }

    class Octant extends Octree.Octant {
        ReentrantLock lock = new ReentrantLock();

        Octant(Octant parent, double center[], double halfSize) {
            this.parent = parent;
            this.center = center;
            this.halfSize = halfSize;
        }

        @Override
        public boolean insert(Vertex v) {
            if (contains(v))
                return false;

            vertices.add(v);
            // If limit was reached and new vertex is not a duplicate
            if (vertices.size() > vertexLimit) {
                subdivide();
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

        public boolean inBounds(Vertex v) {
            for (int i = 0; i < 3; i++) {
                if (v.xyz[i] < (root.center[i] - root.halfSize) || v.xyz[i] > (root.center[i] + root.halfSize)) {
                    return false;
                }
            }
            return true;
        }
    }
}
