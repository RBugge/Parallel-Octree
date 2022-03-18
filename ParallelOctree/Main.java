package ParallelOctree;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

class Main {
    public static void main(String[] args) throws IOException {
        // Load vertices and indices
        // BufferedReader br = new BufferedReader(new FileReader(args[0]));
        BufferedReader br = new BufferedReader(new FileReader("Test models/sphere.gum"));
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

        // Testing remove
        octree.remove(new Vertex(0, 0.980785, -0.19509));

        // Should return false on vertex already removed ^
        for (var v : vertexData) {
            boolean result = octree.remove(v);
            if (!result) {
                System.out.println("Remove " + v + ": " + result);
            }
        }
    }
}