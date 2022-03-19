package ParallelOctree;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

class Main {
    public static void main(String[] args) throws IOException {
        // Load vertices and indices
        // BufferedReader br = new BufferedReader(new FileReader(args[0]));
        String model = "stresstest";
        BufferedReader br = new BufferedReader(new FileReader("Test models/" + model + ".gum"));
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

        testPointerOctree(model, vertexData);
    }

    static void testPointerOctree(String model, Vertex vertexData[]) {
        long start = System.nanoTime();

        PointerOctree octree = new PointerOctree(10, 10);
        for (var v : vertexData) {
            octree.insert(v);
        }

        double execTime = (double) (System.nanoTime() - start) / Math.pow(10, 9);

        printResults("Pointer Octree", model, 1, vertexData.length, execTime);


        // Testing remove with different object
        octree.remove(new Vertex(vertexData[0].x, vertexData[0].y, vertexData[0].z));

        // Should return false on vertex already removed ^
        for (var v : vertexData) {
            boolean result = octree.remove(v);
            if (!result) {
                System.out.println("Remove " + v + ": " + result);
            }
        }
    }

    static void testOctree(Octree octree, String model, int numThreads, Vertex vertexData[]) {
        long start = System.nanoTime();



        double execTime = (double) (System.nanoTime() - start) / Math.pow(10, 9);
        printResults("Pointer Octree", model, 1, vertexData.length, execTime);
    }

    static void printResults(String name, String model, int numThreads, int numVertices, double runtime) {
        System.out.println(name + "\n" +
                "\t# Threads: 1\n" +
                "\tModel: " + model + "\n" +
                "\t# Vertices: " + numVertices + "\n" +
                "\tRuntime: " + runtime + "s\n");
    }
}