import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

class Main {
    public static void main(String[] args) throws IOException {
        // Load vertices and indices
        // BufferedReader br = new BufferedReader(new FileReader(args[0]));
        BufferedReader br = new BufferedReader(new FileReader("../Test models/sphere.gum"));
        br.readLine(); // Ignore name

        // Read/split line containing vertex/index count, third tokens are the numbers
        int numVerts = Integer.parseInt(br.readLine().split(" ")[2]);
        int numIndices = Integer.parseInt(br.readLine().split(" ")[2]);

        br.readLine(); // Ignore color count

        String vertexDataStr[] = br.readLine().split(" ");
        String indexDataStr[] = br.readLine().split(" ");

        br.close();

        Vertex vertexData[] = new Vertex[numVerts];
        for (int i = 0; i < numVerts; i++) {
            vertexData[i] = new Vertex(
                    Double.parseDouble(vertexDataStr[(i) * 3 + 1]),
                    Double.parseDouble(vertexDataStr[(i) * 3 + 2]),
                    Double.parseDouble(vertexDataStr[(i) * 3 + 3]));
        }

        int indexData[] = new int[numIndices];
        for (int i = 0; i < numIndices; i++) {
            indexData[i] = Integer.parseInt(indexDataStr[i + 1]);
        }

        PointerOctree octree = new PointerOctree(10, 10);
        for (var v : vertexData) {
            octree.insert(v);
        }

        octree.remove(new Vertex(0.0, 1.96157, -0.39018));
        for(var v : vertexData) {
            boolean result = octree.remove(v);
            if(!result) {
                System.out.println("Remove " + v + ": " + result);
            }
        }
    }
}

class Vertex {
    double x;
    double y;
    double z;
    double xyz[];

    Vertex(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        xyz = new double[] { x, y, z };
    }

    Vertex(double xyz[]) {
        this.xyz = xyz;
        x = xyz[0];
        y = xyz[1];
        z = xyz[2];
    }

    @Override
    public String toString() {
        return "[" + x + " " + y + " " + z + "]";
    }

    // public boolean equals(Vertex v) {
    // return x == v.x && y == v.y && z == v.z;
    // }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj.getClass() != this.getClass()) {
            return false;
        }

        final Vertex other = (Vertex) obj;
        return this.x == other.x && this.y == other.y && this.z == other.z;
    }
}

class PointerOctree {
    Octant root = new Octant(null, new double[] { 0, 0, 0 }, 0.5);
    boolean firstInsertion = true;
    int vertexLimit;
    int maxDepth;

    // Initialize octree
    PointerOctree(int maxDepth, int vertexLimit) {
        this.maxDepth = maxDepth;
        this.vertexLimit = vertexLimit;
    }

    // Resize octree to fit new vertex
    void resize(Vertex v) {
        // Continually check if vertex is out of bounds and resize until it is contained
        boolean oob = true;
        while (oob) {
            oob = false;
            int oobDir[] = new int[] {
                    v.x < root.center[0] ? -1 : 1,
                    v.y < root.center[1] ? -1 : 1,
                    v.z < root.center[2] ? -1 : 1 };

            // Get direction of vertex for axes that it's out of bounds
            for (int i = 0; i < 3; i++) {
                if (v.xyz[i] < (root.center[i] - root.halfSize) || v.xyz[i] > (root.center[i] + root.halfSize)) {
                    oob = true;
                }
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
                root = newRoot;
            }
        }
    }

    // Find octant which contains vertex v
    Octant findOctant(Vertex v) {
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

            curr = curr.children[nextOctant];
        }
        return curr;
    }

    // Find and insert
    boolean insert(Vertex v) {
        resize(v);

        findOctant(v).insert(v);

        // print();
        return true;
    }

    // TODO: Implement remove
    boolean remove(Vertex v) {
        return findOctant(v).remove(v);
    }

    // Print octree
    void print() {
        System.out.println("Center: [" + root.center[0] + " " + root.center[1] + " " + root.center[2] + "]");
        System.out.println("HalfSize: " + root.halfSize);
        System.out.println("Octree");
        root.print(1);
    }

    class Octant {
        Octant parent;
        Octant children[] = new Octant[8];
        double center[];
        double halfSize;
        boolean isLeaf = true;
        Vector<Vertex> vertices = new Vector<Vertex>(); // Java vectors are synchronized, change?

        Octant(Octant parent, double center[], double halfSize) {
            this.parent = parent;
            this.center = center;
            this.halfSize = halfSize;
        }

        boolean insert(Vertex v) {
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
        void subdivide() {
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

        // TODO: Implement remove
        boolean remove(Vertex v) {
            return vertices.remove(v);
        }

        // Check if vertex is a duplicate in the octant's vertices
        boolean contains(Vertex v) {
            for (var u : vertices)
                if (u.equals(v))
                    return true;

            return false;
        }

        // Recursive print of octants
        void print(int tabs) {
            if (isLeaf) {
                for (var v : vertices) {
                    for (int i = 0; i < tabs; i++)
                        System.out.print("\t");
                    System.out.println(v);
                }
            } else {
                for (int i = 0; i < children.length; i++) {
                    for (int j = 0; j < tabs; j++)
                        System.out.print("\t");
                    System.out.println("Octant " + i);
                    if (children[i] != null)
                        children[i].print(tabs + 1);
                }
            }
        }
    }
}