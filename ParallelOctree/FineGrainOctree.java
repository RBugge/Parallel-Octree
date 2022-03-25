package ParallelOctree;

import java.util.concurrent.locks.ReentrantLock;

public class FineGrainOctree extends Octree {
    Octant root = new Octant(null, new double[] { 0, 0, 0 }, 0.5);

    // Initialize octree
    FineGrainOctree(int vertexLimit) {
        this.vertexLimit = vertexLimit;
        name = "Fine-grain synchronized Octree";
    }

    protected boolean outOfBounds(Octant o, Vertex v) {
        for (int i = 0; i < 3; i++) {
            if (v.xyz[i] < (o.center[i] - o.halfSize)
                    || v.xyz[i] > (o.center[i] + o.halfSize)) {
                return true;
            }
        }
        return false;
    }

    // Resize octree to fit new vertex
    @Override
    protected synchronized void resize(Vertex v) {

        while (!root.lock.tryLock()) {
        }

        Octant saveRoot = root;

        try {
            Octant currRoot = root;
            // Continually check if vertex is out of bounds and resize until it is contained
            while (outOfBounds(currRoot, v)) {

                int oobDir[] = new int[] {
                        v.x < currRoot.center[0] ? -1 : 1,
                        v.y < currRoot.center[1] ? -1 : 1,
                        v.z < currRoot.center[2] ? -1 : 1 };

                // Calculate center of new root octant
                double newHalfSize = currRoot.halfSize * 2;
                double newCenter[] = new double[] {
                        currRoot.center[0] + oobDir[0] * currRoot.halfSize,
                        currRoot.center[1] + oobDir[1] * currRoot.halfSize,
                        currRoot.center[2] + oobDir[2] * currRoot.halfSize,
                };

                // Initialize new root
                Octant newRoot = new Octant(null, newCenter, newHalfSize);
                newRoot.isLeaf = false; // New root is not a leaf
                currRoot.parent = newRoot; // Set current root's parent to new root

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
                newRoot.children[rootPos] = currRoot;

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
                                    newCenter[0] - currRoot.halfSize + i * newHalfSize,
                                    newCenter[1] - currRoot.halfSize + j * newHalfSize,
                                    newCenter[2] - currRoot.halfSize + k * newHalfSize,
                            }, currRoot.halfSize);
                        }
                    }
                }

                currRoot = newRoot;
            }

            // Set octree root to new root
            root = currRoot;
        } finally {
            saveRoot.lock.unlock();
        }
    }

    // Find octant which contains vertex v
    // Implemented inside insert/remove functions
    @Override
    protected Octant find(Vertex v) {
        return null;
    }

    // Find and insert
    @Override
    public boolean insert(Vertex v) {

        // Check if resize is necessary
        resize(v);

        while (!root.lock.tryLock()) {
        }
        Octant curr = root;
        Octant pred;
        try {
            while (!curr.isLeaf) {
                // Based on morton code, calculates correct octant based on position from center
                int nextOctant = 0;
                if (v.x >= curr.center[0])
                    nextOctant += 4;
                if (v.y >= curr.center[1])
                    nextOctant += 2;
                if (v.z >= curr.center[2])
                    nextOctant += 1;

                pred = curr;
                curr = (Octant) curr.children[nextOctant];

                curr.lock.lock();
                pred.lock.unlock();
            }

            return curr.insert(v);

        } finally {
            curr.lock.unlock();
        }

    }

    // Find and remove
    @Override
    public boolean remove(Vertex v) {
        root.lock.lock();
        Octant curr = root;
        Octant next;
        try {
            while (!curr.isLeaf) {

                // Based on morton code, calculates correct octant based on position from center
                int nextOctant = 0;
                if (v.x >= curr.center[0])
                    nextOctant += 4;
                if (v.y >= curr.center[1])
                    nextOctant += 2;
                if (v.z >= curr.center[2])
                    nextOctant += 1;

                next = (Octant) curr.children[nextOctant];
                next.lock.lock();
                curr.lock.unlock();
                curr = next;
            }

            return curr.remove(v);

        } finally {
            curr.lock.unlock();
        }
    }

    @Override
    public boolean contains(Vertex v) {

        root.lock.lock();
        Octant curr = root;
        Octant next;
        try {
            while (!curr.isLeaf) {

                // Based on morton code, calculates correct octant based on position from center
                int nextOctant = 0;
                if (v.x >= curr.center[0])
                    nextOctant += 4;
                if (v.y >= curr.center[1])
                    nextOctant += 2;
                if (v.z >= curr.center[2])
                    nextOctant += 1;

                next = (Octant) curr.children[nextOctant];
                next.lock.lock();
                curr.lock.unlock();
                curr = next;
            }

            return curr.contains(v);

        } finally {
            curr.lock.unlock();
        }

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

            double childHalfSize = halfSize / 2;

            Octant tempChildren[] = new Octant[8];

            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    for (int k = 0; k < 2; k++) {
                        tempChildren[i * 4 + j * 2 + k] = new Octant(this, new double[] {
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

                tempChildren[nextOctant].insert(v);
            }

            children = tempChildren;

            // Clear children
            vertices.clear();

            // linearization point?
            isLeaf = false;
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
