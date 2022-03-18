package ParallelOctree;

public class Vertex {
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
