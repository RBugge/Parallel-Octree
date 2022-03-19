package ParallelOctree;

import java.util.Vector;

public abstract class Octree {
    Octant root;
    boolean firstInsertion = true;
    int vertexLimit;
    String name;

    abstract protected void resize(Vertex v);

    abstract protected Octant find(Vertex v);

    abstract public boolean insert(Vertex v);

    abstract public boolean remove(Vertex v);

    abstract public boolean contains(Vertex v);

    // Print octree
    void print() {
        System.out.println("Center: [" + root.center[0] + " " + root.center[1] + " " + root.center[2] + "]");
        System.out.println("HalfSize: " + root.halfSize);
        System.out.println("Octree");
        root.print(1);
    }

    abstract protected class Octant {
        Octant parent;
        Octant children[] = new Octant[8];
        double center[];
        double halfSize;
        boolean isLeaf = true;
        Vector<Vertex> vertices = new Vector<Vertex>(); // Java vectors are synchronized, change?

        Octant() {
        }

        Octant(Octant parent, double center[], double halfSize) {
            this.parent = parent;
            this.center = center;
            this.halfSize = halfSize;
        }

        abstract public boolean insert(Vertex v);

        abstract public boolean remove(Vertex v);

        abstract public boolean contains(Vertex v);

        abstract protected void subdivide();

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
