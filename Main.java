import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.BitSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

class Main {
    public static void main(String[] args) throws IOException {
        // Load vertices and indices
        // BufferedReader br = new BufferedReader(new FileReader(args[0]));
        BufferedReader br = new BufferedReader(new FileReader("sphere.gum"));
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
        for(int i = 0; i < 123; i++) {
            octree.insert(vertexData[i]);
        }
        octree.print();
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

    public boolean equals(Vertex v) {
        return x == v.x && y == v.y && z == v.z;
    }
}

class PointerOctree {
    Octant root = new Octant(null, new double[] { 0, 0, 0 }, 0);
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
            int oobDir[] = new int[] { 1, 1, 1 };
            for (int i = 0; i < 3; i++) {
                if (v.xyz[i] < root.center[i] - root.halfSize) {
                    oob = true;
                    oobDir[i] = -1;
                } else if (v.xyz[i] > root.center[i] + root.halfSize) {
                    oob = true;
                }
            }

            // If vertex is out of bounds resize octree in direction of vertex
            if (oob) {
                double newHalfSize = root.halfSize * 2;
                double newCenter[] = new double[] {
                        root.center[0] + oobDir[0] * root.halfSize,
                        root.center[1] + oobDir[1] * root.halfSize,
                        root.center[2] + oobDir[2] * root.halfSize,
                };
                Octant newRoot = new Octant(null, newCenter, newHalfSize);
                root.parent = newRoot;

                // Find current roots position inside the new root
                // 1. Flip direction of out of bounds
                // 2. Add 1 and divide by 2 to get either 0 or 1
                // 3. Then multiply to get morton code equivalent position
                int rootPos = (-oobDir[0] + 1) / 2 * 4
                        + (-oobDir[1] + 1) / 2 * 2
                        + (-oobDir[0] + 1) / 2;
                newRoot.children[rootPos] = root;

                // Initialize other child octants
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        for (int k = 0; k < 2; k++) {
                            int childCode = i * 4 + j * 2 + k;
                            // Skip current root
                            if (childCode == rootPos)
                                continue;

                            newRoot.children[childCode] = new Octant(newRoot, new double[] {
                                    newCenter[0] - newHalfSize + i * root.halfSize,
                                    newCenter[1] - newHalfSize + j * root.halfSize,
                                    newCenter[2] - newHalfSize + k * root.halfSize,
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
            if (v.x > curr.center[0])
                nextOctant += 4;
            if (v.y > curr.center[1])
                nextOctant += 2;
            if (v.z > curr.center[2])
                nextOctant += 1;

            curr = curr.children[nextOctant];
        }
        return curr;
    }

    // Find and insert
    boolean insert(Vertex v) {
        if (firstInsertion) {
            firstInsertion = false;
            // Set halfSize of root to fit
            root.halfSize = Math.abs(v.x) + 0.0001;
            if (Math.abs(v.y) > root.halfSize)
                root.halfSize = Math.abs(v.y) + 0.0001;
            if (Math.abs(v.z) > root.halfSize)
                root.halfSize = Math.abs(v.z) + 0.0001;
        } else {
            // Check if resize is necessary
            resize(v);
        }

        findOctant(v).insert(v);
        return true;
    }

    boolean remove(Vertex v) {
        return true;
    }

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
            if (vertices.size() >= vertexLimit && !isDuplicate(v)) {
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
                if (v.x > center[0])
                    nextOctant += 4;
                if (v.y > center[1])
                    nextOctant += 2;
                if (v.z > center[2])
                    nextOctant += 1;

                children[nextOctant].insert(v);
            }

            // Clear children
            vertices.clear();
        }

        boolean remove(Vertex v) {
            return true;
        }

        boolean isDuplicate(Vertex v) {
            for (var u : vertices)
                if (u.equals(v))
                    return true;

            return false;
        }

        void print(int tabs) {
            if (isLeaf) {
                for (var v : vertices) {
                    for(int i = 0; i < tabs; i++)
                        System.out.print("\t");
                    System.out.println(v);
                }
            } else {
                for (int i = 0; i < children.length; i++) {
                    for(int j = 0; j < tabs; j++)
                        System.out.print("\t");
                    System.out.println("Octant " + i);
                    if (children[i] != null)
                        children[i].print(tabs + 1);
                }
            }
        }
    }
}